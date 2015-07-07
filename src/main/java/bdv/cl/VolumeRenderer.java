package bdv.cl;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.ui.TransformListener;
import bdv.AbstractViewerImgLoader;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.zdim.ZdimDialog;
import bdv.viewer.InputActionBindings;
import bdv.viewer.ViewerPanel;

public class VolumeRenderer {
	RenderSlice render;
	float currentdimZ = 20;
	float minBright = 0;
	float maxBright = 0;
	ViewerPanel renderViewer;
	SetupAssignments renderSetup;
	ZdimDialog renderZdim;
	BrightnessDialog renderBrightness;
	boolean retimed = false;

	public VolumeRenderer(final AbstractSpimData<?> spimData,
			final ViewerPanel viewer, final ZdimDialog zdimDialog,
			final SetupAssignments setupAssignments,
			final InputActionBindings bindings,
			BrightnessDialog brightnessDialog) {
		@SuppressWarnings("unchecked")
		final AbstractViewerImgLoader<UnsignedShortType, VolatileUnsignedShortType> imgLoader = (AbstractViewerImgLoader<UnsignedShortType, VolatileUnsignedShortType>) spimData
				.getSequenceDescription().getImgLoader();
		render = new RenderSlice(imgLoader);
		final String RENDER_CONTINUOUS = "continuous";
		final InputMap inputMap = new InputMap();
		inputMap.put(KeyStroke.getKeyStroke("V"), RENDER_CONTINUOUS);
		final ActionMap actionMap = new ActionMap();
		renderViewer = viewer;
		renderSetup = setupAssignments;
		renderZdim = zdimDialog;
		renderBrightness = brightnessDialog;
		renderViewer.setMaxproj(false);

		actionMap.put(RENDER_CONTINUOUS, new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			TransformListener<AffineTransform3D> transformListener = new TransformListener<AffineTransform3D>() {

				boolean changed = false;
				private AffineTransform3D newTransform = new AffineTransform3D();
				private double[] newTransformMatrix = new double[12];
				private double[] oldTransformMatrix = new double[] { 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0, 0 };

				public void transformChanged(final AffineTransform3D transform) {

					// check, if maximum projection option is
					// switched on
					if (viewer.getMaxproj() == true) {

						// did the transformation change?
						renderViewer.getState()
								.getViewerTransform(newTransform);

						newTransform.toArray(newTransformMatrix);
						changed = !Arrays.equals(oldTransformMatrix,
								newTransformMatrix);

						// start rendering if the transformation has
						// changed
						if (changed) {
							render();
							System.out.println("render: transform");
							oldTransformMatrix = Arrays.copyOf(
									newTransformMatrix, 12);
						}

					}
				}
			};

			ChangeListener zdimListener = new ChangeListener() {

				private float oldDimZ = 20;
				private float newDimZ = 20;

				@Override
				public void stateChanged(ChangeEvent e) {
					if (renderViewer.getMaxproj() == true) {

						newDimZ = zdimDialog.getDimZ();

						if (oldDimZ != newDimZ) {
							render();
							System.out.println("render: dimZ");
							oldDimZ = newDimZ;
						}
					}
				}
			};

			ComponentListener resizeListener = new ComponentListener() {

				@Override
				public void componentShown(ComponentEvent e) {
				}

				@Override
				public void componentResized(ComponentEvent e) {
					if (renderViewer.getMaxproj() == true) {
						render();
						System.out.println("render: resize");
					}
				}

				@Override
				public void componentMoved(ComponentEvent e) {
				}

				@Override
				public void componentHidden(ComponentEvent e) {
				}
			};

			ChangeListener timeListener = new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (renderViewer.getMaxproj() == true) {
						retimed = true;
						render();
						System.out.println("render: timepoint");
					}
				}
			};

			ChangeListener brightnessListener = new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (renderViewer.getMaxproj() == true) {
						render();
						System.out.println("render: brightness");
					}
				}
			};

			// rendering of the maximum projection after pressing the hotkey
			@Override
			public void actionPerformed(final ActionEvent e) {

				renderViewer.inverseMaxproj();
				if (renderViewer.getMaxproj() == false) {
					renderViewer.requestRepaint();
					renderViewer.showMessage("maximum projection OFF");

					// remove all Listeners
					renderViewer
							.removeRenderTransformListener(transformListener);
					renderZdim.removeChangeListener(zdimListener);
					renderViewer.removeComponentListener(resizeListener);
					renderViewer.removeTimeListener(timeListener);
					renderBrightness.removeChangeListener(brightnessListener);
				} else {
					renderViewer.showMessage("maximum projection ON");

					// initial rendering
					render();

					// add all Listeners
					renderViewer.addRenderTransformListener(transformListener);
					renderZdim.addChangeListener(zdimListener);
					renderViewer.addComponentListener(resizeListener);
					renderViewer.addTimeListener(timeListener);
					renderBrightness.addChangeListener(brightnessListener);
				}
			}
		});

		// add the local keymappings to the global maps
		bindings.addActionMap("volume", actionMap);
		bindings.addInputMap("volume", inputMap);
	}

	private void render() {

		currentdimZ = renderZdim.getDimZ();

		minBright = renderSetup.getMinMaxGroups().get(0).getMinBoundedValue()
				.getCurrentValue();
		maxBright = renderSetup.getMinMaxGroups().get(0).getMaxBoundedValue()
				.getCurrentValue();
		ARGBType color = renderSetup.getConverterSetups().get(0).getColor();

		render.renderSlice(renderViewer, currentdimZ, minBright, maxBright,
				color, renderZdim.getMaxProjKeepColor(), retimed);
		retimed = false;
	}
}