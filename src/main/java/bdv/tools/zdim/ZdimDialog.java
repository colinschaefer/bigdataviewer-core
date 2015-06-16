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

	public ZdimDialog(final Frame owner,
			final SetupAssignments setupAssignments, final int width) {
		super(owner, "Z - dimension of max-projection", false);
		setSize(500, 120);
		final int max = 100;
		final int min = 0;
		final JSlider microns = new JSlider(min, max, 20);
		microns.setPaintLabels(true);
		microns.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		microns.setMajorTickSpacing(10);
		microns.setMinorTickSpacing(1);
		microns.setPaintLabels(true);
		microns.setPaintTicks(true);

		setLayout(new BorderLayout(10, 10));

		JPanel input = new JPanel();
		final JTextField inputField = new JTextField(5);
		JLabel inputLabel = new JLabel("\u00B5" + "m");
		// input.setLayout(new BorderLayout());
		input.setLayout(new GridLayout(3, 2));
		inputField.setHorizontalAlignment(SwingConstants.RIGHT);
		input.add(new JLabel());
		input.add(new JLabel());
		input.add(inputField);
		input.add(inputLabel);
		input.add(new JLabel());
		input.add(new JLabel());

		microns.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				value = microns.getValue();
				inputField.setText(Integer.toString(value));
			}
		});

		inputField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				value = Integer.valueOf(inputField.getText());
				if (value <= max && value >= 0) {
					microns.setValue(value);
				} else {
					throw new IllegalArgumentException(
							"Please enter an Integer value between " + min
									+ " and " + max);
				}

			}
		});

		add(microns, BorderLayout.CENTER);
		add(input, BorderLayout.EAST);
		inputField.setText(String.valueOf(microns.getValue()));
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setLocation(width + 20, 25);
	}

	private static final long serialVersionUID = 6538962298579455010L;

	public float getDimZ() {
		// TODO Auto-generated method stub
		return value;
	}

}
