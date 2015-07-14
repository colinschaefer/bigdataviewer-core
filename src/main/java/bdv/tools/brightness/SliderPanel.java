package bdv.tools.brightness;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.util.BoundedValue;

/**
 * A {@link JSlider} with a {@link JSpinner} next to it, both modifying the same
 * {@link BoundedValue value}.
 */
public class SliderPanel extends JPanel implements BoundedValue.UpdateListener {
	private static final long serialVersionUID = 6444334522127424416L;

	private final JSlider slider;

	private final JSpinner spinner;

	private final BoundedValue model;

	protected final CopyOnWriteArrayList<ChangeListener> changeListeners;

	/**
	 * Create a {@link SliderPanel} to modify a given {@link BoundedValue value}
	 * .
	 *
	 * @param name
	 *            label to show next to the slider.
	 * @param model
	 *            the value that is modified.
	 */
	public SliderPanel(final String name, final BoundedValue model,
			final int spinnerStepSize) {
		super();
		setLayout(new BorderLayout(10, 10));

		changeListeners = new CopyOnWriteArrayList<ChangeListener>();

		slider = new JSlider(SwingConstants.HORIZONTAL, model.getRangeMin(),
				model.getRangeMax(), model.getCurrentValue());
		spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(model.getCurrentValue(), model
				.getRangeMin(), model.getRangeMax(), spinnerStepSize));

		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final int value = slider.getValue();
				model.setCurrentValue(value);
				for (final ChangeListener listener : changeListeners) {
					listener.stateChanged(new ChangeEvent(spinner));
				}
			}
		});

		spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final int value = ((Integer) spinner.getValue()).intValue();
				model.setCurrentValue(value);
				for (final ChangeListener listener : changeListeners) {
					listener.stateChanged(new ChangeEvent(spinner));
				}
			}
		});

		final JLabel label = new JLabel(name, SwingConstants.CENTER);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);

		add(label, BorderLayout.WEST);
		add(slider, BorderLayout.CENTER);
		add(spinner, BorderLayout.EAST);

		this.model = model;
		model.setUpdateListener(this);
	}

	public void addChangeListener(ChangeListener listener) {
		changeListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		changeListeners.remove(listener);
	}

	@Override
	public void update() {
		final int value = model.getCurrentValue();
		final int min = model.getRangeMin();
		final int max = model.getRangeMax();
		if (slider.getMaximum() != max || slider.getMinimum() != min) {
			slider.setMinimum(min);
			slider.setMaximum(max);
			final SpinnerNumberModel spinnerModel = (SpinnerNumberModel) spinner
					.getModel();
			spinnerModel.setMinimum(min);
			spinnerModel.setMaximum(max);
		}
		slider.setValue(value);
		spinner.setValue(value);
	}

}
