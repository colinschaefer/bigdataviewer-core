// renderslice renders a maximum projection of the current slice on the GPU

package bdv.cl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.display.screenimage.awt.UnsignedByteAWTScreenImage;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import bdv.AbstractViewerImgLoader;
import bdv.cl.BlockTexture.Block;
import bdv.cl.BlockTexture.BlockKey;
import bdv.cl.FindRequiredBlocks.RequiredBlocks;
import bdv.img.cache.CachedCellImg;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.ViewerState;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEvent.ProfilingCommand;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.ChannelOrder;
import com.jogamp.opencl.CLImageFormat.ChannelType;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Map;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;

public class RenderSlice {
	private final AbstractViewerImgLoader<UnsignedShortType, VolatileUnsignedShortType> imgLoader;

	private CLPlatform platform = CLPlatform.getDefault();

	private CLContext context;

	private CLCommandQueue queue;

	private BlockTexture blockTexture;

	private CLKernel slice;

	private CLBuffer<FloatBuffer> transformMatrix;

	private CLBuffer<IntBuffer> sizes;

	private final int[] blockSize = new int[] { 32, 32, 8 };

	private final int[] paddedBlockSize = new int[] { 33, 33, 9 };

	private byte[] data;

	protected AffineTransform3D newAffineTransform = new AffineTransform3D();

	private double[] newTransformMatrix = new double[12];

	private double[] oldTransformMatrix = new double[] { 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0 };

	private boolean resized = false;

	private int oldwidth = 0;

	private int oldheight = 0;

	private boolean rezet = false;

	private float oldZ = 0;

	private boolean retimed = false;

	private float oldT = 0;

	// the constructor initializes the OpenCL Kernel and Context
	public RenderSlice(
			final AbstractViewerImgLoader<UnsignedShortType, VolatileUnsignedShortType> imgLoader) {
		this.imgLoader = imgLoader;

		// try to set the OpenCL Kernel and Context
		try {
			// select the Device with the maximum flops, ideally this should
			// select the GPU
			context = CLContext.create(platform.getMaxFlopsDevice());

			// create the command queue
			queue = context.getDevices()[0]
					.createCommandQueue(Mode.PROFILING_MODE);

			// create the program and build the kernel
			final CLProgram program = context.createProgram(this.getClass()
					.getResourceAsStream("slice3.cl"));
			program.build();
			slice = program.createCLKernel("slice");

			// initialize the buffers
			transformMatrix = context.createFloatBuffer(12, Mem.READ_ONLY,
					Mem.ALLOCATE_BUFFER);
			sizes = context.createIntBuffer(8, Mem.READ_ONLY,
					Mem.ALLOCATE_BUFFER);
			final int[] gridSize = BlockTexture.findSuitableGridSize(
					paddedBlockSize, 300);
			blockTexture = new BlockTexture(gridSize, paddedBlockSize, queue);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	// after switching off the rendering the GPU can be cleaned up and memory
	// can be released
	public void cleanUp() {
		if (queue != null && !queue.isReleased())
			queue.release();

		if (blockTexture != null && !blockTexture.isReleased())
			blockTexture.release();

		if (transformMatrix != null && !transformMatrix.isReleased())
			transformMatrix.release();

		if (sizes != null && !sizes.isReleased())
			sizes.release();

		if (context != null && !context.isReleased())
			context.release();
	}

	// this method actually renders the maximum projection slice
	public void renderSlice(final ViewerState viewerState, final int width,
			final int height, final ViewerPanel viewer, float dimZ,
			float minBright, float maxBright) {

		// get the current transformation of the dataset
		viewer.getState().getViewerTransform(newAffineTransform);
		newAffineTransform.toArray(newTransformMatrix);

		// compare the old window size to the current one
		resized = ((oldwidth * oldheight) != (width * height));

		// compare the old projected z dimension to the current one
		rezet = (oldZ != dimZ);

		// compare the last timepoint to the current one
		final int timepoint = viewerState.getCurrentTimepoint();
		retimed = (oldT != timepoint);

		// if the current transformation, window size or z dimensional rendering
		// is different to the old one, start rendering
		if (!Arrays.equals(oldTransformMatrix, newTransformMatrix) || resized
				|| rezet || retimed) {

			System.out.println();

			// variable declaration and initialization
			final Source<?> source = viewerState.getSources().get(0)
					.getSpimSource(); // TODO
			final int timepointId = timepoint; // TODO
			final int setupId = 0; // TODO
			final int mipmapIndex = 0; // TODO

			// getting the current 3D transformation
			final AffineTransform3D sourceToScreen = new AffineTransform3D();
			viewerState.getViewerTransform(sourceToScreen);
			final AffineTransform3D sourceTransform = new AffineTransform3D();
			source.getSourceTransform(timepoint, mipmapIndex, sourceTransform);
			sourceToScreen.concatenate(sourceTransform);

			// loading the required blocks from disk
			long t = System.currentTimeMillis();
			final RequiredBlocks requiredBlocks = getRequiredBlocks(
					sourceToScreen, width, height, (int) dimZ, new ViewId(
							timepointId, setupId));
			t = System.currentTimeMillis() - t;
			System.out.println("getRequiredBlocks: " + t + " ms");

			// initialization of the image with the loaded blocks
			t = System.currentTimeMillis();
			final RandomAccessible<UnsignedShortType> img = Views
					.extendZero(imgLoader.getImage(new ViewId(timepointId,
							setupId), 0)); // TODO
			final short[] blockData = new short[paddedBlockSize[0]
					* paddedBlockSize[1] * paddedBlockSize[2]];
			int nnn = 0;
			for (final int[] cellPos : requiredBlocks.cellPositions) {
				final BlockKey key = new BlockKey(cellPos);
				if (!blockTexture.contains(key)) {
					blockTexture
							.put(key, getBlockData(cellPos, img, blockData));
					nnn++;
				}
			}
			t = System.currentTimeMillis() - t;
			System.out.println("upload " + nnn + " blocks: " + t + " ms");

			// initializing buffers for writing to the GPU
			final int[] lookupDims = new int[3];
			final int[] maxCell = requiredBlocks.maxCell;
			final int[] minCell = requiredBlocks.minCell;
			for (int d = 0; d < 3; ++d)
				lookupDims[d] = maxCell[d] - minCell[d] + 1;
			System.out.println("need "
					+ (4 * (int) Intervals.numElements(lookupDims))
					+ " shorts for lookup table");

			final CLBuffer<ShortBuffer> blockLookup = context
					.createShortBuffer(
							4 * (int) Intervals.numElements(lookupDims) + 16,
							Mem.READ_ONLY, Mem.ALLOCATE_BUFFER);
			final ByteBuffer bytes = queue.putMapBuffer(blockLookup, Map.WRITE,
					true);
			final ShortBuffer shorts = bytes.asShortBuffer();
			for (final int[] cellPos : requiredBlocks.cellPositions) {
				final BlockKey key = new BlockKey(cellPos);
				final Block block = blockTexture.get(key);
				final int[] blockPos;
				if (block != null)
					blockPos = block.getBlockPos();
				else
					blockPos = new int[] { 0, 0, 0 };
				final int i = 4 * IntervalIndexer.positionWithOffsetToIndex(
						cellPos, lookupDims, minCell);
				for (int d = 0; d < 3; ++d)
					shorts.put(i + d,
							(short) (blockPos[d] * paddedBlockSize[d]));
				shorts.put(i + 3, (short) 0);
			}
			for (int i = 4 * (int) Intervals.numElements(lookupDims); i < 4 * (int) Intervals
					.numElements(lookupDims) + 16; ++i)
				shorts.put(i, (short) 0);
			queue.putUnmapMemory(blockLookup, bytes);
			queue.finish();

			final CLImage2d<ByteBuffer> renderTarget = (CLImage2d<ByteBuffer>) context
					.createImage2d(Buffers.newDirectByteBuffer(width * height),
							width, height, new CLImageFormat(ChannelOrder.R,
									ChannelType.UNSIGNED_INT8), Mem.READ_WRITE);

			final AffineTransform3D screenToShiftedSource = new AffineTransform3D();
			screenToShiftedSource.set(1, 0, 0, -minCell[0] * blockSize[0], 0,
					1, 0, -minCell[1] * blockSize[1], 0, 0, 1, -minCell[2]
							* blockSize[2]);
			screenToShiftedSource.concatenate(sourceToScreen.inverse());
			final AffineTransform3D shiftedSourceToBlock = new AffineTransform3D();
			shiftedSourceToBlock.set(1.0 / blockSize[0], 0, 0, 0, 0,
					1.0 / blockSize[1], 0, 0, 0, 0, 1.0 / blockSize[2], 0);
			screenToShiftedSource.preConcatenate(shiftedSourceToBlock);

			for (int r = 0; r < 3; ++r)
				for (int c = 0; c < 4; ++c)
					transformMatrix.getBuffer().put(
							(float) screenToShiftedSource.get(r, c));
			transformMatrix.getBuffer().rewind();
			queue.putWriteBuffer(transformMatrix, true);

			sizes.getBuffer().put(blockSize);
			sizes.getBuffer().put(1);
			sizes.getBuffer().put(lookupDims);
			sizes.getBuffer().put(1);
			sizes.getBuffer().rewind();
			queue.putWriteBuffer(sizes, true);

			// variable declaration for the kernel
			final long globalWorkOffsetX = 0;
			final long globalWorkOffsetY = 0;
			final long globalWorkSizeX = width;
			final long globalWorkSizeY = height;
			final long localWorkSizeX = 0;
			final long localWorkSizeY = 0;

			// kernel execution
			for (int i = 0; i < 1; ++i) {
				final CLEventList eventList = new CLEventList(1);
				slice.rewind().putArg(transformMatrix).putArg(sizes)
						.putArg(dimZ).putArg(minBright).putArg(maxBright)
						.putArg(blockLookup).putArg(blockTexture.get())
						.putArg(renderTarget);
				queue.put2DRangeKernel(slice, globalWorkOffsetX,
						globalWorkOffsetY, globalWorkSizeX, globalWorkSizeY,
						localWorkSizeX, localWorkSizeY, eventList);
				queue.putReadImage(renderTarget, true).finish();

				final CLEvent event = eventList.getEvent(0);
				final long start = event
						.getProfilingInfo(ProfilingCommand.START);
				final long end = event.getProfilingInfo(ProfilingCommand.END);
				System.out.println("event t = " + ((end - start) / 1000000.0)
						+ " ms");
			}

			// writing the data from the kernel output buffer into a byte array
			if (data == null || data.length != width * height)
				data = new byte[width * height];
			renderTarget.getBuffer().get(data);

			for (int i = 0; i < data.length; i++) {
				if (data[i] == -1) {
					data[i] = 0;
				}
			}
			// start the representation in the viewerpanel
			show(data, width, height, viewer);

			// releasing Memory
			renderTarget.release();
			blockLookup.release();

			// if the current transformation, window size and z dimensional
			// rendering stayed the same just show the same as before
		} else {
			show(data, width, height, viewer);
		}
		// copy the current transformation, width, height and z dimension
		// settings for comparison in the next loop
		oldTransformMatrix = Arrays.copyOf(newTransformMatrix, 12);
		oldwidth = width;
		oldheight = height;
		oldZ = dimZ;
		oldT = timepoint;
	}

	// the show method paints the maximum projection which was rendered on the
	// GPU to the Interactive Canvas
	private void show(final byte[] data, final int width, final int height,
			ViewerPanel viewer) {

		// Converting the byte buffer back in image data
		final UnsignedByteAWTScreenImage screenImage = new UnsignedByteAWTScreenImage(
				ArrayImgs.unsignedBytes(data, width, height));

		// Converting the Image to a buffered image
		final BufferedImage bufferedImage = screenImage.image();

		// painting the canvas with the buffered image
		viewer.paint(bufferedImage);

	}

	private short[] getBlockData(final int[] blockPos,
			final RandomAccessible<UnsignedShortType> img,
			final short[] useThisData) {
		final int n = 3;
		final long[] min = new long[n];
		final long[] max = new long[n];
		for (int d = 0; d < n; ++d) {
			min[d] = blockPos[d] * blockSize[d];
			max[d] = min[d] + paddedBlockSize[d] - 1;
		}

		final short[] data = useThisData == null ? new short[paddedBlockSize[0]
				* paddedBlockSize[1] * paddedBlockSize[2]] : useThisData;
		final Cursor<UnsignedShortType> in = Views.flatIterable(
				Views.interval(img, min, max)).cursor();
		for (int i = 0; i < data.length; ++i)
			data[i] = (short) (in.next().get() & 0xffff);

		return data;
	}

	private RequiredBlocks getRequiredBlocks(
			final AffineTransform3D sourceToScreen, final int w, final int h,
			final int dd, final ViewId view) {
		final CachedCellImg<?, ?> cellImg = (bdv.img.cache.CachedCellImg<?, ?>) imgLoader
				.getImage(view, 0);
		final long[] imgDimensions = new long[3];
		cellImg.dimensions(imgDimensions);
		return FindRequiredBlocks.getRequiredBlocks(sourceToScreen, w, h, dd,
				blockSize, imgDimensions);
	}
}
