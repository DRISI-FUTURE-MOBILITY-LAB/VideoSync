/*
 * ****************************************************************
 * File: 			GraphOptions.java
 * Date Created:  	June 28, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To control all aspects of the graphing options
 *                  pertaining to time width and graph offset
 *
 * Modified			August 23, 2016
 * Programmer		Danny Hale
 *                  Added an update to the offset textfield so that
 *                  the numbers would be updated when the graph moved
 *                  with the mouse pointer. Fixed offset bug that kept
 *                  negative numbers from being consistent with the
 *                  graph movement.
 * ****************************************************************
 */
package VideoSync.views.tabbed_panels.graphs;

import VideoSync.commands.windows.graph.CommandGraphWidth;
import VideoSync.models.DataModelProxy;
import VideoSync.views.textfilters.IntFilter;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.prefs.Preferences;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GraphOptions extends JPanel implements Observer, KeyListener, ActionListener
{
    private static final long serialVersionUID = 1L;

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graph Options Variable Declarations

    /**
     * Used for notifying the DataModel of any changes that need to be reflected application wide
     */
    private final DataModelProxy dmp;

    /**
     * Used for entering in the timing offset for the data
     */
    private final JTextField txtOffset;

    /**
     *
     */
    private final JTextField regionThickness;

    /**
     * Used for selecting how much data is to be displayed
     */
    private final JComboBox graphWidthComboBox;

    /**
     * Used for displaying the resolution of the graph tick marks.
     */
    private final JLabel fileNameLabel;
    private final JLabel rightWindowTime;
    private final JLabel leftWindowTime;

    /**
     *
     */
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graph Options Construction

    /**
     * Creates the Graph Options panel with the DataModel that is to receive the updates
     *
     * @param dmp - DataModelProxy to receive any updates
     */
    public GraphOptions(DataModelProxy dmp)
    {
        // Set the DataModel in the graph options to the one passed in the constructor
        this.dmp = dmp;

        // Panel for containing the graph resolution.
        // Sets the layout of the panel to null, indicating we are using the absolute positioning layout
        //JPanel resolutionPanel = new JPanel();
        //resolutionPanel.setBounds(2, 2, 240, 55);
        //resolutionPanel.setLayout(null);

        // Create a label for the Resolution
        JLabel lblScale = new JLabel("Resolution:");
        lblScale.setBounds(6, 9, 73, 16);
        lblScale.setHorizontalAlignment(SwingConstants.LEFT);

        // Set the border, Layout, and Size of the Graph OptionsKeyListener
        this.setBorder(new EtchedBorder(EtchedBorder.RAISED, Color.BLACK, Color.WHITE));
        setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(724, 80));

        // Create a label for the Graph Width
        JLabel lblGraphWidth = new JLabel("Graph Width:");
        lblGraphWidth.setBounds(386, 3, 156, 16);
        add(lblGraphWidth);
        lblGraphWidth.setHorizontalAlignment(SwingConstants.LEFT);

        // Create a combo box for the values in the graph width
        graphWidthComboBox = new JComboBox();
        graphWidthComboBox.setBounds(380, 20, 85, 27);
        add(graphWidthComboBox);
        graphWidthComboBox.setName("seconds");
        graphWidthComboBox.setModel(new DefaultComboBoxModel(new String[]{".100", ".25", ".5", "1", "2", "4", "8", "16", "32", "64", "120"}));

        // Create a label for indicating the width time scale
        JLabel lblSeconds = new JLabel("seconds");
        lblSeconds.setBounds(470, 25, 60, 16);
        add(lblSeconds);

        JLabel lblGraphOffset = new JLabel("Graph Offset:");
        lblGraphOffset.setBounds(561, 3, 164, 16);
        add(lblGraphOffset);
        lblGraphOffset.setHorizontalAlignment(SwingConstants.LEFT);

        txtOffset = new JTextField();
        txtOffset.setBounds(555, 20, 101, 28);
        add(txtOffset);
        txtOffset.setColumns(10);
        // Add a key listener to the text field to listen to any changes
        txtOffset.addKeyListener(this);

        JLabel lblRegionThickness = new JLabel("Region Thickness:");
        lblRegionThickness.setBounds(736, 3, 164, 16);
        add(lblRegionThickness);
        lblRegionThickness.setHorizontalAlignment(SwingConstants.LEFT);

        regionThickness = new JTextField();
        IntFilter intFilter = new IntFilter();
        ((PlainDocument) regionThickness.getDocument()).setDocumentFilter(intFilter);

        ConfigDocumentListener thicknessTFListener = new ConfigDocumentListener() {
            @Override
            protected void updateValues(DocumentEvent e)
            {
                if(!regionThickness.getText().equals(""))
                    setRegionThickness(regionThickness.getText());
            }
        };

        regionThickness.setBounds(730, 20, 101, 28);
        regionThickness.setText(this.prefs.get("regionThickness","2"));
        regionThickness.getDocument().addDocumentListener(thicknessTFListener);
        add(regionThickness);

        JLabel lblFileName = new JLabel("Device File Names:");
        lblFileName.setBounds(6, 3, 163, 16);
        add(lblFileName);

        fileNameLabel = new JLabel("New label");
        fileNameLabel.setBounds(6, 21, 350, 16);
        add(fileNameLabel);

        leftWindowTime = new JLabel();
        leftWindowTime.setHorizontalAlignment(SwingConstants.LEFT);
        leftWindowTime.setFont(new Font("Lucida Grande", Font.BOLD, 16));
        leftWindowTime.setBounds(235, 32, 58, 25);
        add(leftWindowTime);

        rightWindowTime = new JLabel();
        rightWindowTime.setHorizontalAlignment(SwingConstants.RIGHT);
        rightWindowTime.setFont(new Font("Lucida Grande", Font.BOLD, 16));
        rightWindowTime.setBounds(800, 31, 58, 25);
        add(rightWindowTime);

        graphWidthComboBox.setSelectedIndex(4);
        // FIXME: getSelectedItem() might be null
        setWidth((String) graphWidthComboBox.getSelectedItem());
    }

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Java Event Listeners & Action Commands
    // -- NOTE: None of the following methods have been currently implemented in this version

    /**
     * Sets the graph's width (in seconds) and allows any changes that are made to the combo box to
     * fire off an action command to the CommandGraphWidth
     *
     * @param cgw command object for graph width
     */
    public void setWidthActionCommand(CommandGraphWidth cgw)
    {
        graphWidthComboBox.setAction(cgw);
        graphWidthComboBox.addActionListener(this);
    }

    /**
     * Fires when a key typed event has been detected
     */
    public void keyReleased(KeyEvent arg0)
    {
        String input = txtOffset.getText();
        int start = input.indexOf('-');
        int sep = input.indexOf('.');

        if((input.length() > 0 && start == -1) || (input.length() > 1 && start == 0))
        {
            try
            {
                int seconds = 0;
//				int millis = 0;
                double millis = 0;

                if(sep < 0)
                {    //True when the '.' doesn't exist
                    if(start == 0)
                    {    //Check if offset is negative
                        seconds = Integer.parseInt(input.substring(start + 1));
                    }
                    else
                    {
                        seconds = Integer.parseInt(input);
                    }

                }
                else
                {
                    String secStr;
                    if(start == 0)
                    {//Check if offset is negative
                        secStr = input.substring(1, sep);
                    }
//						seconds = Integer.parseInt(input.substring(start + 1, sep));
                    else
                    {
                        secStr = input.substring(0, sep);
//						seconds = Integer.parseInt(input.substring(0, sep));
                    }

                    //= input.substring(0, sep);
                    if(secStr.length() > 0)
                    {
                        seconds = Integer.parseInt(input.substring(start + 1, sep));
                    }

                    String milliStr = input.substring(sep + 1);
//					if (milliStr.length() > 0)
//						millis = Integer.parseInt(input.substring(sep + 1));
//					s
                    if(milliStr.length() > 0)
                    {
                        millis = Double.parseDouble(input.substring(sep));
                    }
                    System.out.println(start + " " + secStr + " " + seconds);
                    System.out.println("milliStr: " + milliStr + " -- millis: " + millis);
                }

                int offset;
//				long offset = 0;

                if(start == -1)
                {
                    offset = seconds * 1000 + (int) (millis * 1000);
                }
                else
                {
                    offset = -(seconds * 1000 + (int) (millis * 1000));
                }

                System.out.println("offset: " + offset);


                // Set the graph offset based on the Integer value from the txtOffset field.
                // If we have a number format exception with that value, we don't update the data model
                dmp.setGraphOffset(offset);
            }
            catch(NumberFormatException nfe)
            {
                System.out.println("NFE Reached: " + nfe.getMessage());
                nfe.printStackTrace();
            }
        }
        else
        {
            dmp.setGraphOffset(0);
        }
    }

    // -- Java Event Listeners - UNUSED
    // -- NOTE: None of the following methods have been currently implemented in this version

    @Override
    public void update(Observable o, Object arg)
    {
        if(arg instanceof DataModelProxy)
        {
            Vector<File> dataFiles = ((DataModelProxy) arg).getDataFiles();

            //Build up display string from names of files in vector.
            StringBuilder nameBuilder = new StringBuilder();
            boolean addSeparator = false;
            for(File dataFile : dataFiles)
            {
                //If multiple files have been loaded, show them separated by comas.
                if(addSeparator)
                {
                    nameBuilder.append(", ");
                }
                else
                {
                    addSeparator = true;
                }

                nameBuilder.append(dataFile.getName());
            }

            fileNameLabel.setText(nameBuilder.toString());

        }

        if(arg instanceof String)
        {
            String temp = (String) arg;

            if(temp.equals("Mouse") || temp.equals("Jump"))
            {
                double ms = dmp.getGraphOffset() / 1000.0;
                System.out.println("Graph offset set to " + ms + " ms.");
                txtOffset.setText(Double.toString(ms));
            }
            else if(temp.equals("GraphWidth"))
            {
                //FIXME: Find way to get the width index without using the DataModelProxy.
                graphWidthComboBox.setSelectedIndex(dmp.getGraphWidthIndex());
                // FIXME: getSelectedItem() might be null
                setWidth((String) graphWidthComboBox.getSelectedItem());
            }
        }
    }


    @Override
    public void keyPressed(KeyEvent arg0)
    {
    }

    @Override
    public void keyTyped(KeyEvent arg0)
    {
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        if(arg0.getSource() == graphWidthComboBox)
        {
            //FIXME: Find way to get the width index without using the DataModelProxy.
            dmp.setGraphWidthIndex(graphWidthComboBox.getSelectedIndex());
        }
    }

    public void setWidth(String val)
    {
        if(val.contains("."))
        {
            float fVal = Float.parseFloat(val) / 2;

            leftWindowTime.setText("- " + fVal);
            rightWindowTime.setText("+ " + fVal);
        }
        else
        {
            int iVal = Integer.parseInt(val) / 2;

            leftWindowTime.setText("- " + iVal);
            rightWindowTime.setText("+ " + iVal);

        }
    }

    private void setRegionThickness(String rt)
    {
        this.prefs.put("regionThickness", rt);
    }

    private static class ConfigDocumentListener implements DocumentListener
    {
        @Override
        public void changedUpdate(DocumentEvent e)
        {
            updateValues(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e)
        {
            updateValues(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
            updateValues(e);
        }

        protected void updateValues(DocumentEvent e) { }
    }
}
