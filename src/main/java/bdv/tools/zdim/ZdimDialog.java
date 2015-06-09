package bdv.tools.zdim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.type.numeric.ARGBType;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.brightness.SliderPanel;
//import bdv.tools.brightness.BrightnessDialog.ColorIcon;
import bdv.tools.brightness.BrightnessDialog.ColorsPanel;
import bdv.tools.brightness.BrightnessDialog.MinMaxPanel;
import bdv.tools.brightness.BrightnessDialog.MinMaxPanels;

public class ZdimDialog extends JDialog
{
	public ZdimDialog( final Frame owner, final SetupAssignments setupAssignments )
	{
		super( owner, "Z - dimension of max-projection", false );

		final Container content = getContentPane();

		final MinMaxPanels minMaxPanels = new MinMaxPanels( setupAssignments, this, true );
		final ColorsPanel colorsPanel = new ColorsPanel( setupAssignments );
		content.add( minMaxPanels, BorderLayout.NORTH );
		content.add( colorsPanel, BorderLayout.SOUTH );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}

			private static final long serialVersionUID = 3904286091931838921L;
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		setupAssignments.setUpdateListener( new SetupAssignments.UpdateListener()
		{
			@Override
			public void update()
			{
				colorsPanel.recreateContent();
				minMaxPanels.recreateContent();
			}
		} );

		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	

	private static final long serialVersionUID = 7963632306732311403L;
}
