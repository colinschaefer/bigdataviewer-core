package bdv.tools.zdim;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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

	private int value = 1;

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

		// setting up the panel for the texfields and labels
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout(3, 2));

		// creating the textfields, setting them right aligned thext and setting
		// the default value
		final JTextField inputField = new JTextField(5);
		final JTextField maxField = new JTextField(5);
		final JTextField minField = new JTextField(5);

		inputField.setHorizontalAlignment(SwingConstants.RIGHT);
		minField.setHorizontalAlignment(SwingConstants.RIGHT);
		maxField.setHorizontalAlignment(SwingConstants.RIGHT);

		inputField.setText(String.valueOf(microns.getValue()));
		minField.setText(String.valueOf(min));
		maxField.setText(String.valueOf(max));

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

		// slider change listener, that also changes the textfield to the chosen
		// value
		microns.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				value = microns.getValue();
				inputField.setText(Integer.toString(value));
			}
		});

		// listener for the input field, that changes the slider accordingly and
		// adjusts the minimum and maximum value if needed
		inputField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				value = Integer.valueOf(inputField.getText());
				int tmpValue = value;
				if (value < 0) {
					throw new IllegalArgumentException(
							"Please enter an Integer value over 0");
				}

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
				minField.setText(String.valueOf(min));
				maxField.setText(String.valueOf(max));

				microns.setValue(tmpValue);

				value = tmpValue;
			}
		});

		// listener for the minimum field, that adjusts the mininum value of the
		// slider according to the input and adjusts the maximum value if needed
		minField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				min = Integer.valueOf(minField.getText());
				if (min >= max) {
					max = min + 50;
					maxField.setText(String.valueOf(max));
				}
				microns.setMinimum(min);
				microns.setMaximum(max);
				microns.setLabelTable(null);
				microns.setMajorTickSpacing((max - min) / 10);
			}

		});

		// listener for the maximum field, that adjusts the maximum value of the
		// slider according to the input and adjusts the mininum value if needed
		maxField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				max = Integer.valueOf(maxField.getText());
				if (max < min && max > 50) {
					min = max - 50;
					minField.setText(String.valueOf(min));
				} else {
					min = 0;
					minField.setText(String.valueOf(min));
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
