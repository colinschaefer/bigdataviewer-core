package bdv.cl;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.ui.TransformListener;
import bdv.AbstractViewerImgLoader;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.zdim.ZdimDialog;
import bdv.viewer.InputActionBindings;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.ViewerState;

public class VolumeRenderer {

	public VolumeRenderer(final AbstractSpimData<?> spimData,
			final ViewerPanel viewer, final ZdimDialog zdimDialog,
			final SetupAssignments setupAssignments, ViewerFrame viewerFrame) {
		@SuppressWarnings("unchecked")
		final AbstractViewerImgLoader<UnsignedShortType, VolatileUnsignedShortType> imgLoader = (AbstractViewerImgLoader<UnsignedShortType, VolatileUnsignedShortType>) spimData
				.getSequenceDescription().getImgLoader();
		final RenderSlice render = new RenderSlice(imgLoader);
		final String RENDER_CONTINUOUS = "continuous";
		final InputMap inputMap = new InputMap();
		inputMap.put(KeyStroke.getKeyStroke("V"), RENDER_CONTINUOUS);
		final ActionMap actionMap = new ActionMap();
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
					@Override
					public void transformChanged(
							final AffineTransform3D transform) {

						// check, if maximum projection option is
						// switched
						// on
						if (viewer.getMaxproj() == true) {

							float currentdimZ = 20;
							float minBright = 0;
							float maxBright = 0;

							// initialize variables
							final ViewerState state = viewer.getState();
							final int width = viewer.getDisplay().getWidth();
							final int height = viewer.getDisplay().getHeight();
							currentdimZ = zdimDialog.getDimZ();
							minBright = setupAssignments.getMinMaxGroups()
									.get(0).getMinBoundedValue()
									.getCurrentValue();
							maxBright = setupAssignments.getMinMaxGroups()
									.get(0).getMaxBoundedValue()
									.getCurrentValue();
							render.renderSlice(state, width, height, viewer,
									currentdimZ, minBright, maxBright);
						}
					}
				});
			}
		});
		final InputActionBindings bindings = viewerFrame.getKeybindings();
		bindings.addActionMap("volume", actionMap);
		bindings.addInputMap("volume", inputMap);
	}
}