package bdv.tools.zdim;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import bdv.tools.brightness.SetupAssignments;

/**
 * Adjust brightness and colors for individual (or groups of)
 * {@link BasicViewSetup setups}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */

public class ZdimDialog extends JDialog {

	private int value = 20;

	int max = 100;
	int min = 0;

	JSlider microns;

	public ZdimDialog(final Frame owner,
			final SetupAssignments setupAssignments, final int width) {
		super(owner, "Z - dimension of max-projection", false);

		// setup the dialog
		setSize(500, 120);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setLocation(width + 20, 25);
		setLayout(new BorderLayout(10, 10));

		// setup the new slider and its appearance
		microns = new JSlider(min, max, 20);
		microns.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		microns.setMajorTickSpacing(10);
		microns.setPaintLabels(true);
		microns.setPaintTicks(true);

		// setting up the panel for the spinners and labels
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout(3, 2));

		// creating the spinners, setting them right aligned thext and setting
		// the default value
		final JSpinner inputField = new JSpinner();
		final JSpinner maxField = new JSpinner();
		final JSpinner minField = new JSpinner();

		inputField.setValue(value);
		minField.setValue(min);
		maxField.setValue(max);

		// adding the textfields and labels to the panel
		rightPanel.add(minField);
		rightPanel.add(new JLabel("min Value"));
		rightPanel.add(inputField);
		rightPanel.add(new JLabel("\u00B5" + "m"));
		rightPanel.add(maxField);
		rightPanel.add(new JLabel("max Value"));

		// adding the slider and the panel to the dialog
		add(microns, BorderLayout.CENTER);
		add(rightPanel, BorderLayout.EAST);

		// slider change listener, that also changes the spinner to the chosen
		// value
		microns.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				value = microns.getValue();
				inputField.setValue(value);
			}
		});

		// listener for the input field, that changes the slider accordingly and
		// adjusts the minimum and maximum value if needed
		inputField.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				value = Integer.valueOf(inputField.getValue().toString());
				int tmpValue = value;
				if (tmpValue < min && tmpValue >= 20) {
					min = tmpValue - 20;
				} else if (tmpValue < min && tmpValue < 20) {
					min = 0;
				} else if (tmpValue > max) {
					max = tmpValue + 20;
				}

				microns.setMinimum(min);
				microns.setMaximum(max);
				microns.setLabelTable(null);
				microns.setMajorTickSpacing((max - min) / 10);
				minField.setValue(String.valueOf(min));
				maxField.setValue(String.valueOf(max));

				microns.setValue(tmpValue);

				value = tmpValue;
			}
		});

		// listener for the minimum field, that adjusts the mininum value of the
		// slider according to the input and adjusts the maximum value if needed
		minField.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				min = Integer.valueOf(minField.getValue().toString());
				if (min < 0) {
					min = 0;
					minField.setValue(String.valueOf(min));
				}

				if (min >= max) {
					max = min + 50;
					maxField.setValue(String.valueOf(max));
				}
				microns.setMinimum(min);
				microns.setMaximum(max);
				microns.setLabelTable(null);
				microns.setMajorTickSpacing((max - min) / 10);
			}

		});

		// listener for the maximum field, that adjusts the maximum value of the
		// slider according to the input and adjusts the mininum value if needed
		maxField.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				max = Integer.valueOf(maxField.getValue().toString());
				if (max < 0) {
					max = 100;
					min = 0;
					minField.setValue(String.valueOf(min));
					maxField.setValue(String.valueOf(max));
					microns.setValue(20);
				}

				if (max < min && max > 50) {
					min = max - 50;
					minField.setValue(String.valueOf(min));
				} else {
					min = 0;
					minField.setValue(String.valueOf(min));
				}

				microns.setMinimum(min);
				microns.setMaximum(max);
				microns.setLabelTable(null);
				microns.setMajorTickSpacing((max - min) / 10);
			}
		});

	}

	private static final long serialVersionUID = 6538962298579455010L;

	public float getDimZ() {
		return value;
	}

}
