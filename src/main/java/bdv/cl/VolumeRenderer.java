package bdv.cl;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.ui.TransformListener;
import bdv.AbstractViewerImgLoader;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.zdim.ZdimDialog;
import bdv.viewer.InputActionBindings;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;

public class VolumeRenderer {
	RenderSlice render;
	float currentdimZ = 20;
	float minBright = 0;
	float maxBright = 0;
	ViewerPanel renderViewer;
	SetupAssignments renderSetup;
	ZdimDialog renderZdim;

	public VolumeRenderer(final AbstractSpimData<?> spimData,
			final ViewerPanel viewer, final ZdimDialog zdimDialog,
			final SetupAssignments setupAssignments, ViewerFrame viewerFrame) {
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
		viewer.setMaxproj(false);

		actionMap.put(RENDER_CONTINUOUS, new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			// rendering of the maximum projection after pressing the hotkey
			@Override
			public void actionPerformed(final ActionEvent e) {
				viewer.inverseMaxproj();
				if (viewer.getMaxproj() == false) {
					viewer.requestRepaint();
					viewer.showMessage("maximum projection OFF");
				} else {
					viewer.showMessage("maximum projection ON");
				}
				// rendering new after manual transformation
				viewer.addRenderTransformListener(new TransformListener<AffineTransform3D>() {

					boolean changed = false;

					private double[] newTransformMatrix = new double[12];

					private double[] oldTransformMatrix = new double[] { 0, 0,
							0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

					public void transformChanged(
							final AffineTransform3D transform) {

						System.out.println("bla");

						// check, if maximum projection option is
						// switched on
						if (viewer.getMaxproj() == true) {

							// did the transformation change?
							transform.toArray(newTransformMatrix);
							changed = !Arrays.equals(oldTransformMatrix,
									newTransformMatrix);

							// start rendering if the transformation has changed
							if (changed) {
								render();
								oldTransformMatrix = Arrays.copyOf(
										newTransformMatrix, 12);
							}

						}
					}
				});
			}
		});

		// add the local keymappings to the global maps
		final InputActionBindings bindings = viewerFrame.getKeybindings();
		bindings.addActionMap("volume", actionMap);
		bindings.addInputMap("volume", inputMap);
	}

	private void render() {
		final int width = renderViewer.getDisplay().getWidth();
		final int height = renderViewer.getDisplay().getHeight();
		currentdimZ = renderZdim.getDimZ();

		minBright = renderSetup.getMinMaxGroups().get(0).getMinBoundedValue()
				.getCurrentValue();
		maxBright = renderSetup.getMinMaxGroups().get(0).getMaxBoundedValue()
				.getCurrentValue();
		ARGBType color = renderSetup.getConverterSetups().get(0).getColor();

		render.renderSlice(renderViewer.getState(), width, height,
				renderViewer, currentdimZ, minBright, maxBright, color,
				renderZdim.getMaxProjKeepColor());

		System.out.println("render");
	}
}