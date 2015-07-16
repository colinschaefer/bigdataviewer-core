package bdv.cl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

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
	// rendering instance
	RenderSlice render;

	// variables with initialization
	float currentdimZ = 20;
	float minBright = 0;
	float maxBright = 0;

	// attributes, which need to be available everywhere
	ViewerPanel renderViewer;
	SetupAssignments renderSetup;
	ZdimDialog renderZdim;
	BrightnessDialog renderBrightness;

	private AffineTransform3D newTransform = new AffineTransform3D();

	boolean createListenersOnFirstRendering = true;

	final ActionMap actionMap = new ActionMap();

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

				private boolean changed = false;
				private boolean pendingAlignTransform = false;

				private AffineTransform3D oldTransform = new AffineTransform3D();

				public void transformChanged(final AffineTransform3D transform) {

					// check, if maximum projection option is
					// switched on
					if (viewer.getMaxproj() == true) {

						// did the transformation change?
						renderViewer.getState()
								.getViewerTransform(newTransform);

						changed = newTransform.get(0, 0) != oldTransform.get(0,
								0)
								|| newTransform.get(0, 1) != oldTransform.get(
										0, 1)
								|| newTransform.get(0, 2) != oldTransform.get(
										0, 2)
								|| newTransform.get(0, 3) != oldTransform.get(
										0, 3)
								|| newTransform.get(1, 0) != oldTransform.get(
										1, 0)
								|| newTransform.get(1, 1) != oldTransform.get(
										1, 1)
								|| newTransform.get(1, 2) != oldTransform.get(
										1, 2)
								|| newTransform.get(1, 3) != oldTransform.get(
										1, 3)
								|| newTransform.get(2, 0) != oldTransform.get(
										2, 0)
								|| newTransform.get(2, 1) != oldTransform.get(
										2, 1)
								|| newTransform.get(2, 2) != oldTransform.get(
										2, 2)
								|| newTransform.get(2, 3) != oldTransform.get(
										2, 3);

						pendingAlignTransform = renderViewer
								.getPendingAlignTransform();

						// start rendering if the transformation has
						// changed
						if (changed && !pendingAlignTransform) {
							render();
							System.out.println("render: transform");
							oldTransform = newTransform;
							newTransform = new AffineTransform3D();

							// else if the transformation has changed and there
							// is a pending align transform repaint and in the
							// next step it will be rerendered
						} else if (changed) {
							renderViewer.paint();
							pendingAlignTransform = false;
						}
					}
				}
			};

			ChangeListener zdimListener = new ChangeListener() {

				private float oldDimZ = 20;
				private float newDimZ = 20;

				@Override
				public void stateChanged(ChangeEvent e) {
					// if maximum projection is switched on: render
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
					// if maximum projection is switched on: render
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
					// if maximum projection is switched on: render
					if (renderViewer.getMaxproj() == true) {
						render();
						System.out.println("render: timepoint "
								+ viewer.getState().getCurrentTimepoint());
					}
				}
			};

			ChangeListener brightnessListener = new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					// if maximum projection is switched on: render
					if (renderViewer.getMaxproj() == true) {
						render();
						System.out.println("render: brightness");
					}
				}
			};

			ActionListener setupIdListener = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// if maximum projection is switched on: render
					if (renderViewer.getMaxproj() == true) {
						render();
						System.out.println("render: setupId");
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

				} else {
					renderViewer.showMessage("maximum projection ON");

					// render every time
					render();

					// only create Listeners during the first rendering
					if (createListenersOnFirstRendering) {
						// add all Listeners
						// renderViewer.addRenderTransformListener(transformListener);
						renderViewer.addTransformListener(transformListener);
						renderZdim.addChangeListener(zdimListener);
						renderZdim.addActionListener(setupIdListener);
						renderViewer.addComponentListener(resizeListener);
						renderViewer.addTimeListener(timeListener);
						renderBrightness.addChangeListener(brightnessListener);

						// no more listeners will be created
						createListenersOnFirstRendering = false;
					}
				}
			}

		});

		// add the local keymappings to the global maps
		bindings.addActionMap("volume", actionMap);
		bindings.addInputMap("volume", inputMap);
	}

	private void render() {

		final int setupId = renderZdim.getCurrentSetupId();

		currentdimZ = renderZdim.getDimZ();

		minBright = renderSetup.getMinMaxGroups().get(setupId)
				.getMinBoundedValue().getCurrentValue();
		maxBright = renderSetup.getMinMaxGroups().get(setupId)
				.getMaxBoundedValue().getCurrentValue();
		ARGBType color = renderSetup.getConverterSetups().get(setupId)
				.getColor();

		final int optimalMipMapLevel = renderViewer.getState()
				.getBestMipMapLevel(newTransform, setupId);

		render.renderSlice(renderViewer, currentdimZ, minBright, maxBright,
				color, renderZdim.getMaxProjKeepColor(), setupId,
				optimalMipMapLevel);
	}

	protected void initialRender() {
		actionMap.get("continuous").actionPerformed(
				new ActionEvent(actionMap, 0, "continuous"));
	}

}