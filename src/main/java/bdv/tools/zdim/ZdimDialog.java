package bdv.tools.zdim;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
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
	private boolean maxProjKeepColor = false;
	private int value = 20;

	int max = 100;
	int min = 0;

	JSlider microns;

	public ZdimDialog(final Frame owner, final SetupAssignments setupAssignments) {
		super(owner, "Z - dimension of max-projection", false);

		SpinnerModel modelin = new SpinnerNumberModel(value, 0, 2000, 1);
		SpinnerModel modelmin = new SpinnerNumberModel(min, 0, 2000, 1);
		SpinnerModel modelmax = new SpinnerNumberModel(max, 0, 2000, 1);

		// setup the dialog
		setSize(500, 140);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
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
		final JSpinner inputField = new JSpinner(modelin);
		final JSpinner maxField = new JSpinner(modelmax);
		final JSpinner minField = new JSpinner(modelmin);

		// adding the textfields and labels to the panel
		rightPanel.add(minField);
		rightPanel.add(new JLabel("min Value"));
		rightPanel.add(inputField);
		rightPanel.add(new JLabel("\u00B5" + "m"));
		rightPanel.add(maxField);
		rightPanel.add(new JLabel("max Value"));

		// creating and setting the checkbox
		JCheckBox keepColor = new JCheckBox();
		JPanel downPanel = new JPanel();
		downPanel.setLayout(new BorderLayout(10, 10));
		downPanel.add(keepColor, BorderLayout.LINE_START);
		downPanel.add(new JLabel(
				"keep the Color upon rendering (might decrease performance)"),
				BorderLayout.CENTER);

		// adding the slider and the panel to the dialog
		add(microns, BorderLayout.CENTER);
		add(rightPanel, BorderLayout.EAST);
		add(downPanel, BorderLayout.SOUTH);

		// slider change listener, that also changes the spinner to the chosen
		// value
		microns.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				value = microns.getValue();
				inputField.setValue(value);
			}
		});

		microns.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				boolean max = false;
				if (Integer.valueOf(microns.getValue()) == microns.getMaximum()) {
					int posX = MouseInfo.getPointerInfo().getLocation().x;
					int posY = MouseInfo.getPointerInfo().getLocation().y;
					int startPosX = microns.getX();
					int startPosY = microns.getY();
					int diffX = posX - startPosX;
					int diffY = posY - startPosY;
					if ((diffX * diffY) / 2 > 150 || !max) {
						maxField.setValue(2000);
						inputField.setValue(2000);
						max = true;
					}
				}
			}
		});

		// listener for the input field, that changes the slider accordingly and
		// adjusts the minimum and maximum value if needed

		inputField.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				value = (Integer) inputField.getValue();
				System.out.println("value" + String.valueOf(value));

				microns.setValue(value);

				System.out.println(String.valueOf(value));
			}
		});

		// listener for the minimum field, that adjusts the mininum value of the
		// slider according to the input and adjusts the maximum value if needed
		minField.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				min = (Integer) minField.getValue();
				System.out.println("min" + String.valueOf(min));
				microns.setMinimum(min);
				microns.setLabelTable(null);
				microns.setMajorTickSpacing((max - min) / 10);
			}

		});

		// listener for the maximum field, that adjusts the maximum value of the
		// slider according to the input and adjusts the mininum value if needed
		maxField.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				max = (Integer) maxField.getValue();
				microns.setMaximum(max);
				microns.setLabelTable(null);
				microns.setMajorTickSpacing((max - min) / 10);

				System.out.println("max" + String.valueOf(max));

			}
		});

		keepColor.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				maxProjKeepColor = !maxProjKeepColor;
			}

		});

	}

	private static final long serialVersionUID = 6538962298579455010L;

	public boolean getMaxProjKeepColor() {
		return maxProjKeepColor;
	}

	public float getDimZ() {
		return value;
	}

}
