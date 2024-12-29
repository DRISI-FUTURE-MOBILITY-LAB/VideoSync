/*
 * ****************************************************************
 * File: 			InputMapping.java
 * Date Created:  	July 24, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To allow a user to change the default name values
 *                  for the channels that they are currently using.
 *                  It will automatically save the mapping file if
 *                  changes are made and the view exits.
 * Modified:		August 24, 2016
 * Programmer:		Danny Hale
 *                  Fixed problem with editing already existing
 *                  entries. Updated the output file to include the
 *                  numbers of elements written to the file and
 *                  adjusted the table widths to fit the data better.
 * Modified			Circa Spring/Summer 2019
 * Programmer:		Jenzel Arevalo
 *                  Implemented additional fields in table and added
 *                  Detector Config combo box in edit pane to allow
 *                  users to specify the detection configuration of
 *                  a particular channel. This addition will be
 *                  relevant with the use of Event Logger and
 *                  the Auto-Analysis tool.
 * ****************************************************************
 */
package VideoSync.views.modals;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.EDeviceType;
import VideoSync.objects.InputMappingFile;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

// FIXME: This needs to be changed to detect what kind of detector station it is (Freeway, Ramp, Intersection) and display the appropriate values and fields

@SuppressWarnings({"rawtypes", "unchecked"})
public class InputMapping extends JFrame implements ActionListener, ItemListener, ListSelectionListener, KeyListener, Observer, WindowListener
{
    private static final long serialVersionUID = -5651018822338534325L;

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Video Player Variable Declarations

    /**
     * Constant string for the suffix to the mapping file. It is prefaced by the device name.
     */
    private static final String MAPPING_NAME = "mapping.mpf";

    /**
     * Constant string array containing all of the column names that will be displayed in the table.
     */
    private final String[] columnNames = {"Chip", "Pin", "Channel #", "Name", "Lane #", "Type", "Direction", "Detector Type", "Detector Configuration"};

    /**
     * Used for storing the table column data generated from the column names.
     */
    private final Vector<String> tableColumnData;

    /**
     * Used for presenting all of the table rows with the data
     */
    private Vector<Vector> tableRowData;

    /**
     * Used for storing the resulting input map data
     */
    private Vector<DeviceInputMap> inputMapData;

    /**
     * Input File to read/write the input data from/to.
     */
    //private File inputFile;
    private InputMappingFile inputMappingFile;
    /**
     * Reference to notify the Data Model that there are changes and to have the views immediately reflect those changes.
     */
    private DataModelProxy dmp;

    /**
     * Used in indicating that we want to write the updates to a file.
     */
    private boolean writeUpdatesToFile = false;

    /**
     * Used for indicating that changes were made to an input element.
     */
    private boolean changesMade = false;

    /**
     * Used for indicating that changes are currently being made.
     */
    private boolean isEditing = false;

    /**
     * Used for keeping track of which array element is being updated.
     */
    private int index;

    /**
     * Used for presenting all of the available channels to the user.
     */
    private JTable table_Channels;

    /**
     * Used for containing the JTable and allowing it to scroll if there is enough data to warrant it.
     */
    private final JScrollPane scrollPane;

    /**
     * Used for indicating what bit the data represents. This number is not changeable by the user.
     * as it is the reference to all events within the program.
     */
    private final JLabel label_BitNumber;

    /**
     * Used for allowing the user to indicate which lane number the detector is in
     */
    private final JTextField textfield_LaneNumber;

    /**
     * Used for allowing the user to give the detector a more specific name.
     */
    private final JTextField textfield_ChannelName;

    /**
     * Used for selecting what type of detector is being used.
     */
    private final JComboBox combo_DetectorType;

    /**
     * Default detector types to initialize combobox to
     */
    private final String[] defaultDetectorTypes = {"Select Type", "Radar", "Loop", "Video"};

    /**
     * Used for adding detector types to the dropdown
     */
    private final JButton addDetectTypeButton;

    /**
     * Used for deleting detector types from the dropdown
     */
    private final JButton deleteDetectTypeButton;

    /**
     * Used for selecting the configuration type of the detector
     */
    private final JComboBox combo_DetectorConfig;

    /**
     * Used for selecting what kind of detector is being used.
     */
    private final JComboBox combo_ChannelType;

    /**
     * Used for indicating the direction the lane is heading (N, S, E, W)
     */
    private final JComboBox combo_Direction;

    /**
     * Used for selecting which device is going to be displayed in the table.
     */
    private final JComboBox combo_DeviceSelect;

    /**
     * Used to toggle the data in the edit pane so they can be enabled or disabled.
     */
    private final JButton button_EditUpdate;

    /**
     * Used to cancel & close the Input Mapping window.
     */
    private final JButton button_Cancel;


    /**
     * Keeps track of device types represented in combo_DeviceSelect.
     */
    Vector<EDeviceType> deviceTypes = new Vector<>();

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Input Mapping Construction

    public InputMapping()
    {
        // Set the main panel attributes that will contain all of the GUI elements. Setting the layout to 'null' allows positions to be absolute
        JPanel mainPanel = new JPanel();
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        mainPanel.setLayout(null);

        // Create the content area to store all of the editing attributes.
        JPanel editPanel = new JPanel();
        editPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
        editPanel.setBounds(850, 6, 180, 650);
        mainPanel.add(editPanel);
        editPanel.setLayout(null);

        // Create a static label as a title for the edit panel.
        JLabel label2 = new JLabel("Edit Channel Attributes");
        label2.setBounds(16, 9, 146, 16);
        editPanel.add(label2);

        // Create a static separator below the panel's title.
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.BLACK);
        separator.setBounds(6, 30, 166, 12);
        editPanel.add(separator);

        // Create a static label for the channel number
        JLabel label3 = new JLabel("Channel Number");
        label3.setHorizontalAlignment(SwingConstants.CENTER);
        label3.setBounds(6, 40, 166, 16);
        editPanel.add(label3);

        // Create the label to hold the bit number for the selected channel. This element is not changeable by
        // the user as it is the primary identifier that links all the data objects together.
        label_BitNumber = new JLabel("");
        label_BitNumber.setHorizontalAlignment(SwingConstants.CENTER);
        label_BitNumber.setBounds(6, 60, 165, 16);
        editPanel.add(label_BitNumber);

        // Create a static separator below the channel number.
        JSeparator separator_1 = new JSeparator();
        separator_1.setBounds(6, 80, 166, 12);
        editPanel.add(separator_1);

        // Create a static label for the lane number.
        JLabel label4 = new JLabel("Lane Number");
        label4.setHorizontalAlignment(SwingConstants.CENTER);
        label4.setBounds(6, 105, 166, 16);
        editPanel.add(label4);

        // Create a text field that will accept input from the user for the lane number.
        // Initializes its enabled status to false as there is no data immediately contained within it
        // Sets its max columns to 10
        // Adds a key listener for input.
        textfield_LaneNumber = new JTextField();
        textfield_LaneNumber.setEnabled(false);
        textfield_LaneNumber.setBounds(6, 130, 166, 28);
        textfield_LaneNumber.setColumns(10);
        textfield_LaneNumber.addKeyListener(this);
        editPanel.add(textfield_LaneNumber);

        // Create a static separator below the lane number.
        JSeparator separator_2 = new JSeparator();
        separator_2.setBounds(6, 155, 166, 12);
        editPanel.add(separator_2);

        // Create a static label for the channel name.
        JLabel label5 = new JLabel("Channel Name");
        label5.setHorizontalAlignment(SwingConstants.CENTER);
        label5.setBounds(6, 180, 166, 16);
        editPanel.add(label5);

        // Create a text field that will accept input from the user for the channel name.
        // Initializes its enabled status to false as there is no data immediately contained within it
        // Sets its max columns to 10
        // Adds a key listener for input.
        textfield_ChannelName = new JTextField();
        textfield_ChannelName.setEnabled(false);
        textfield_ChannelName.setColumns(10);
        textfield_ChannelName.setBounds(6, 205, 166, 28);
        textfield_ChannelName.addKeyListener(this);
        editPanel.add(textfield_ChannelName);

        // Create a static separator below the channel name
        JSeparator separator_3 = new JSeparator();
        separator_3.setBounds(6, 230, 166, 12);
        editPanel.add(separator_3);

        // Create a static label for the detector type
        JLabel label6 = new JLabel("Detector Type");
        label6.setHorizontalAlignment(SwingConstants.CENTER);
        label6.setBounds(6, 255, 166, 16);
        editPanel.add(label6);

        // Create the combo box with the available detector types.
        combo_DetectorType = new JComboBox();
        combo_DetectorType.setEnabled(false);
        combo_DetectorType.setModel(new DefaultComboBoxModel(defaultDetectorTypes));
        combo_DetectorType.setBounds(6, 280, 166, 27);
        editPanel.add(combo_DetectorType);

        // TODO: This naming scheme...should be fixed

        addDetectTypeButton = new JButton("Add");
        addDetectTypeButton.setEnabled(false);
        addDetectTypeButton.setBounds(6, 307, 83, 29);
        addDetectTypeButton.addActionListener(this);
        addDetectTypeButton.addActionListener(e -> {
            String newType = JOptionPane.showInputDialog("Enter a new type");
            if(!newType.equals(""))
            {
                combo_DetectorType.addItem(newType);
                combo_DetectorType.setSelectedItem(newType);
                changesMade = true;
            }
        });
        editPanel.add(addDetectTypeButton);

        deleteDetectTypeButton = new JButton("Delete");
        deleteDetectTypeButton.setEnabled(false);
        deleteDetectTypeButton.setBounds(89, 307, 83, 29);
        deleteDetectTypeButton.addActionListener(this);
        deleteDetectTypeButton.addActionListener(e -> {
            String currentType = (String) this.combo_DetectorType.getSelectedItem();
            if(inputMappingFile != null && !Arrays.asList(defaultDetectorTypes).contains(currentType))
            {
                for(DeviceInputMap dim : inputMapData)
                {
                    if(dim.getDetectorType() != null)
                    {
                        if(dim.getDetectorType().equals(currentType))
                            dim.setDetectorType((String) combo_DetectorType.getItemAt(0));
                    }
                }

                combo_DetectorType.setSelectedIndex(0);
                combo_DetectorType.removeItem(currentType);

                refreshTable();

                changesMade = true;
            }
        });
        editPanel.add(deleteDetectTypeButton);

//        // Create a static separator below the
//        JSeparator separator_4 = new JSeparator();
//        separator_4.setBounds(6, 360, 166, 12);
//        editPanel.add(separator_4);

        // Create a static label for the detector type
        JLabel label7 = new JLabel("Detector Config");
        label7.setHorizontalAlignment(SwingConstants.CENTER);
        label7.setBounds(6, 357, 166, 16);
        editPanel.add(label7);

        // Create the combo box with the available detector types.
        combo_DetectorConfig = new JComboBox();
        combo_DetectorConfig.setEnabled(false);
        combo_DetectorConfig.setModel(new DefaultComboBoxModel(new String[]{"Select Type", "Stop Bar", "Push Bar", "Pulse", "Presence"}));
        combo_DetectorConfig.setBounds(6, 382, 166, 27);
        editPanel.add(combo_DetectorConfig);

        // Create a static separator below the
        JSeparator separator_5 = new JSeparator();
        separator_5.setBounds(6, 407, 166, 12);
        editPanel.add(separator_5);

        // Create a static label for the channel type
        JLabel label8 = new JLabel("Channel Type");
        label8.setHorizontalAlignment(SwingConstants.CENTER);
        label8.setBounds(6, 432, 166, 16);
        editPanel.add(label8);

        // Create the combo box with the available channel types
        combo_ChannelType = new JComboBox();
        combo_ChannelType.setEnabled(false);
        combo_ChannelType.setModel(new DefaultComboBoxModel(new String[]{"Select Type", "Intersection", "Freeway", "Ramp"}));
        combo_ChannelType.setBounds(6, 457, 166, 27);
        editPanel.add(combo_ChannelType);

        // Create a static separator below the
        JSeparator separator_6 = new JSeparator();
        separator_6.setBounds(6, 482, 166, 12);
        editPanel.add(separator_6);

        // Create a static label for the channel direction
        JLabel label9 = new JLabel("Channel Direction");
        label9.setHorizontalAlignment(SwingConstants.CENTER);
        label9.setBounds(6, 507, 166, 16);
        editPanel.add(label9);

        // Create the combo box with the available directions.
        combo_Direction = new JComboBox();
        combo_Direction.setEnabled(false);
        combo_Direction.setModel(new DefaultComboBoxModel(new String[]{"Choose Direction", "Northbound", "Southbound", "Eastbound", "Westbound"}));
        combo_Direction.setBounds(6, 532, 166, 27);
        editPanel.add(combo_Direction);

        // Create the Edit/Update button.
        button_EditUpdate = new JButton("Edit");
        button_EditUpdate.setEnabled(false);
        button_EditUpdate.setBounds(30, 582, 120, 29);
        button_EditUpdate.addActionListener(this);
        editPanel.add(button_EditUpdate);


        // Create the content area to store the table layout
        JPanel tablePanel = new JPanel();
        tablePanel.setBounds(6, 6, 825, 650);
        mainPanel.add(tablePanel);
        tablePanel.setLayout(null);


        // Create a static label for the available channels
        JLabel label1 = new JLabel("Available Channels");
        label1.setHorizontalAlignment(SwingConstants.CENTER);
        label1.setBounds(157, 6, 354, 22);
        tablePanel.add(label1);

        // Create the scroll pane to contain the table and allow it to scroll
        scrollPane = new JScrollPane();
        scrollPane.setBounds(6, 34, 820, 615);
        tablePanel.add(scrollPane);

        // Create the table and add it to the scroll pane's viewport.
        table_Channels = new JTable();
        table_Channels.getSelectionModel().addListSelectionListener(e -> {
            index = table_Channels.getSelectedRow();
            updateTableRow();
        });
        scrollPane.setViewportView(table_Channels);

        // Create the device selection combo box and set its default text
        combo_DeviceSelect = new JComboBox();
        combo_DeviceSelect.setModel(new DefaultComboBoxModel(new String[]{"Select Device"}));
        combo_DeviceSelect.setBounds(6, 5, 139, 27);
        combo_DeviceSelect.addItemListener(this);
        tablePanel.add(combo_DeviceSelect);

        // Create the "Apply Changes" button
//		button_ApplyChanges = new JButton("Apply Changes");
//		button_ApplyChanges.addActionListener(this);
//		button_ApplyChanges.setBounds(260, 523, 128, 29);
//		mainPanel.add(button_ApplyChanges);

//		button_Cancel = new JButton("Cancel");
//		button_Cancel.setBounds(394, 523, 117, 29);
//		button_Cancel.addActionListener(this);
//		mainPanel.add(button_Cancel);

        // Create the close button so the user can close the window.
        button_Cancel = new JButton("Close");
        button_Cancel.setBounds(306, 660, 117, 29);
        button_Cancel.addActionListener(this);
        mainPanel.add(button_Cancel);

        // Set the size of the Input Mapping plane.
        this.setSize(1050, 725);

        // Lock width of window
        this.setResizable(false);

        // Initialize the data that will hold all of the table column names
        tableColumnData = new Vector<>();

        // Add the column names to the table column data array.
        tableColumnData.addAll(Arrays.asList(columnNames));

        this.addWindowListener(this);
    }

    private void importDetectorTypes()
    {
        int size = combo_DetectorType.getItemCount();
        HashSet<String> types = new HashSet<>();
        for(int i = 0; i < size; i++)
            types.add((String) combo_DetectorType.getItemAt(i));

        for(DeviceInputMap dim: inputMapData)
        {
            if(!types.contains(dim.getDetectorType()) && dim.getDetectorType() != null)
            {
                combo_DetectorType.addItem(dim.getDetectorType());
                types.add(dim.getDetectorType());
            }
        }
    }

    /**
     * This is the first function called when the input mapping button is clicked on the toolbar
     * and the last function when the window closes.
     * @param visible Toggles the visibility of the input mapping window
     */
    public void displayPanel(boolean visible)
    {
        System.out.println("The are" + (writeUpdatesToFile ? " " : " no ") + "changes to write to file.");
        if(!visible)
        {
            if(writeUpdatesToFile)
            {
                writeMappingToFile();
            }

            button_EditUpdate.setEnabled(false);
            combo_DeviceSelect.setSelectedIndex(0);
            inputMapData = null;
            refreshTable();
            resetFieldsToDefaults();
        }

        setVisible(visible);
    }

    private void resetFieldsToDefaults()
    {
        label_BitNumber.setText(null);
        textfield_LaneNumber.setText(null);
        textfield_ChannelName.setText(null);
        combo_ChannelType.setSelectedIndex(0);
        combo_Direction.setSelectedIndex(0);
        combo_DetectorType.setSelectedIndex(0);
        combo_DetectorConfig.setSelectedIndex(0);
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if(e.getSource() instanceof ListSelectionModel)
        {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if(lsm.isSelectionEmpty())
            {
                System.out.println("No rows are selected.");
            }
        }
        else
        {
            System.out.println("Source: " + e.getSource());
        }
    }

    public void setUIElementsEnabled(boolean enabled)
    {
        textfield_LaneNumber.setEnabled(enabled);
        textfield_ChannelName.setEnabled(enabled);
        combo_DetectorType.setEnabled(enabled);
        combo_DetectorConfig.setEnabled(enabled);
        combo_Direction.setEnabled(enabled);
        combo_ChannelType.setEnabled(enabled);
        addDetectTypeButton.setEnabled(enabled);
        deleteDetectTypeButton.setEnabled(enabled);

        if(enabled)
        {
            button_EditUpdate.setText("Update");
            combo_ChannelType.addItemListener(this);
            combo_Direction.addItemListener(this);
            combo_DetectorType.addItemListener(this);
            combo_DetectorConfig.addItemListener(this);
        }
        else
        {
            button_EditUpdate.setText("Edit");
            combo_ChannelType.removeItemListener(this);
            combo_Direction.removeItemListener(this);
            combo_DetectorType.removeItemListener(this);
            combo_DetectorConfig.removeItemListener(this);
        }
    }

    /**
     * FIXME: This needs needs to be corrected so that it ia properly called when a row is changed or a new devices is selected in the combo box. The  mapping file needs to be properly updated before moving on.
     * <p>
     *     Updates row cells with data provided by the user. This data may come from the
     *     edit dialog box on the right side or it may come from text entered into a cell.
     * </p>
     */
    public void performUpdateForData()
    {
        if(button_EditUpdate.getText().equals("Edit"))
        {
            System.out.println("Enabling data to be edited");
        }
        else
        {
            if(changesMade)
            {
                System.out.println("Changes were made to data.");
                writeUpdatesToFile = true;
                changesMade = false;

                DeviceInputMap dim = inputMapData.elementAt(this.index);
                dim.setLaneNumber(Integer.parseInt((this.textfield_LaneNumber.getText() == null || this.textfield_LaneNumber.getText().equals("")) ? "0" : this.textfield_LaneNumber.getText()));
                dim.setChannelName(this.textfield_ChannelName.getText());
                dim.setDetectorType((String) this.combo_DetectorType.getSelectedItem());
                dim.setDirection((String) this.combo_Direction.getSelectedItem());
                dim.setChannelType((String) this.combo_ChannelType.getSelectedItem());
                dim.setDetectorConfig((String) this.combo_DetectorConfig.getSelectedItem());
                refreshTable();

                dmp.updateInputMapForDevice(deviceTypes.elementAt(combo_DeviceSelect.getSelectedIndex()), inputMapData);
                inputMappingFile.setDeviceInputMap(dim, this.index);
            }
        }
    }

    /**
     * Saves user made changes to the mapping file. This should be called whenever a row is
     * modified or a new devices is selected from the combo box.
     */
    public void writeMappingToFile()
    {
        inputMappingFile.writeFile();
        writeUpdatesToFile = false;
    }

    public void setDataModelProxy(DataModelProxy dmp)
    {
        this.dmp = dmp;
    }

    @Override
    public void update(Observable arg0, Object arg1)
    {
        if(arg1 instanceof DataModelProxy)
        {
            dmp = (DataModelProxy) arg1;
        }

        if(arg1 instanceof String[])
        {
            // Set the combo box data to the string array that was sent
            combo_DeviceSelect.setModel(new DefaultComboBoxModel((String[]) arg1));
        }
        else if(arg1 instanceof Map)
        {
            //Get a set of devices sorted by key.
            Set<Map.Entry<EDeviceType, String>> devices = (Set<Map.Entry<EDeviceType, String>>) ((Map) arg1).entrySet();

            //Loop through each device and store device type and display string separately.
            //Use an array instead of vector for deviceStrings to ensure that order does not change.
            int index = 0;
            String[] deviceStrings = new String[devices.size()];
            Vector<EDeviceType> deviceTypes = new Vector<>();
            for(Map.Entry<EDeviceType, String> device : devices)
            {
                deviceTypes.add(device.getKey());
                deviceStrings[index] = device.getValue();
                index++;
            }

            //Set device types and combo box strings.
            //The index of the combo box should correspond to the index of deviceTypes.
            this.deviceTypes = deviceTypes;
            combo_DeviceSelect.setModel(new DefaultComboBoxModel(deviceStrings));
        }
    }

    public void refreshTable()
    {

        if(this.tableRowData != null)
        {
            this.tableRowData.clear();
        }
        else
        {
            this.tableRowData = new Vector<>();
        }

        if(this.inputMapData != null)
        {
            for(int i = 0; i < this.inputMapData.size(); i++)
            {
                DeviceInputMap dim = inputMapData.elementAt(i);

                Vector<String> rowData = new Vector<>();

                rowData.add(Integer.toString(dim.getChipNumber()));
                rowData.add(Integer.toString(dim.getPinNumber()));
                rowData.add(Integer.toString(dim.getChannelNumber()));
                rowData.add(dim.getChannelName());
                rowData.add(Integer.toString(dim.getLaneNumber()));
                rowData.add(dim.getChannelType());
                rowData.add(dim.getDirection());
                rowData.add(dim.getDetectorType());
                rowData.add(dim.getDetectorConfig());

                this.tableRowData.add(rowData);
            }
        }

        ArrayList<Integer> colWidths = new ArrayList<>();
        for(Enumeration<TableColumn> col = table_Channels.getColumnModel().getColumns(); col.hasMoreElements();)
            colWidths.add(col.nextElement().getPreferredWidth());

        this.table_Channels = new JTable(this.tableRowData, this.tableColumnData);
        this.table_Channels.getSelectionModel().addListSelectionListener(e -> {
            index = table_Channels.getSelectedRow();
            updateTableRow();
        });

        this.table_Channels.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table_Channels.addKeyListener(this);
        this.table_Channels.setModel(new DefaultTableModel(tableRowData, tableColumnData)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });

        for(int i = 0; i < tableColumnData.size(); i++)
        {
            TableColumn column = table_Channels.getColumnModel().getColumn(i);

            if(colWidths.size() <= 0)
            {
                switch(i)
                {
                    case 0:
                    case 1:
                        column.setPreferredWidth(30);
                        break;
                    case 2:
                    case 3:
                        column.setPreferredWidth(80);
                        break;
                    default:
                        column.setPreferredWidth(60);
                }
            }
            else
            {
                column.setPreferredWidth(colWidths.get(i));
            }
        }

        int pos = scrollPane.getVerticalScrollBar().getValue();

        JScrollPane sp = new JScrollPane(this.table_Channels);
        sp.getVerticalScrollBar().setValue(pos);
        scrollPane.setViewport(sp.getViewport());
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Java Event Listeners

    /**
     * Gets called when the Input Mapping window is closed or when the table rows are being edited.
     *
     * @param e action event
     */
    public void actionPerformed(ActionEvent e)
    {
        // Save changes and close window.
        if(e.getSource() == button_Cancel)
        {
            displayPanel(false);
        }
        else if(e.getSource() == button_EditUpdate)
        {
            // Save changes made to input mapping row.
            if(!isEditing)
            {
                isEditing = true;
            }
            // Make input mapping row editable.
            else
            {
                performUpdateForData();
                isEditing = false;
            }
            setUIElementsEnabled(isEditing);
        }
    }

    public void setEditPaneContents()
    {
        DeviceInputMap dim = inputMapData.elementAt(this.index);

        System.out.println("------------------------------------------------------------------------");
        System.out.println(" -- setEditPaneContents()");
        System.out.println(" -- DeviceInputMap: " + dim);
        System.out.println("------------------------------------------------------------------------");

        label_BitNumber.setText(Integer.toString(dim.getChannelNumber()));

        if(dim.getLaneNumber() != 0)
        {
            textfield_LaneNumber.setText(Integer.toString(dim.getLaneNumber()));
        }

        if(dim.getChannelName() != null)
        {
            textfield_ChannelName.setText(dim.getChannelName());
        }

        if(dim.getDetectorType() != null)
        {
            combo_DetectorType.setSelectedItem(dim.getDetectorType());
        }
        else
        {
            combo_DetectorType.setSelectedIndex(0);
            combo_DetectorType.setSelectedItem(combo_DetectorType.getSelectedIndex());
        }

        if(dim.getDetectorConfig() != null)
        {
            combo_DetectorConfig.setSelectedItem(dim.getDetectorConfig());
        }
        else
        {
            combo_DetectorConfig.setSelectedIndex(0);
            combo_DetectorConfig.setSelectedItem(combo_DetectorConfig.getSelectedIndex());
        }

        if(dim.getDirection() != null)
        {
            combo_Direction.setSelectedItem(dim.getDirection());
        }
        else
        {
            combo_Direction.setSelectedIndex(0);
            combo_Direction.setSelectedItem(combo_Direction.getSelectedIndex());
        }

        if(dim.getChannelType() != null)
        {
            combo_ChannelType.setSelectedItem(dim.getChannelType());
        }
        else
        {
            combo_ChannelType.setSelectedIndex(0);
            combo_ChannelType.setSelectedItem(combo_ChannelType.getSelectedIndex());
        }
    }

    /**
     * <p>
     * Used for detecting and updating UI elements if there was a change the combo_DeviceSelect box.
     * The combo_DeviceSelect would have been previously populated by a file read which triggered the
     * inputMapping as an observer.
     * </p>
     * <p>
     * May also indicated that the details of a DeviceInputMap have been edited.
     * </p>
     */
    public void itemStateChanged(ItemEvent e)
    {
        // If the source is the device selection combo box, we want to populate the view with the correct channel information.
        if(e.getSource() == combo_DeviceSelect)
        {
            inputMappingFile = dmp.getInputMappingFile((String) combo_DeviceSelect.getSelectedItem());
            if(inputMappingFile != null)
            {
                inputMapData = inputMappingFile.getDeviceInputMapVector();
                importDetectorTypes();
            }
            refreshTable();
        }
        else
        {
            changesMade = true;
        }
    }

    /**
     * Used for detecting if a key is typed from any of the TextFields
     *
     * @param event KeyEvent created when something was typed
     */
    public void keyTyped(KeyEvent event)
    {
        // Update the changesMade value to true.
        changesMade = true;
    }

    /**
     * Function used to update table row, invoked by table's ListSelectionListener
     */
    private void updateTableRow()
    {
        this.button_EditUpdate.setEnabled(true);

        int n = 0;

        if(changesMade)
        {
            System.err.println("Changes were made to a field...need to confirm cancel before moving on");

            Object[] options = {"Yes", "No"};

            n = JOptionPane.showOptionDialog(this,
                    "Unsaved changes were made to the channel. Are you sure you want to continue without saving?", null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        }

        if(n == 0)
        {
            if(changesMade)
            {
                changesMade = false;
            }

            if(isEditing)
            {
                setUIElementsEnabled(false);
            }

            setEditPaneContents();
        }
        else if(n == 1)
        {
            changesMade = false;
            setUIElementsEnabled(false);
        }
    }

    /**
     * The following functions pertain to the various implementations that are currently not being used by the class.
     */
    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Java Event Listener methods
    // -- NOTE: None of the following are currently implemented in this version
    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    @Override
    public void windowOpened(WindowEvent e) { }

    @Override
    public void windowClosing(WindowEvent e)
    {
        System.out.println("The are" + (writeUpdatesToFile ? " " : " no ") + "changes to write to file.");

        if(writeUpdatesToFile)
        {
            writeMappingToFile();
        }

        button_EditUpdate.setEnabled(false);
        combo_DeviceSelect.setSelectedIndex(0);
        inputMapData = null;
        refreshTable();
        resetFieldsToDefaults();
    }

    @Override
    public void windowClosed(WindowEvent e) { }

    @Override
    public void windowIconified(WindowEvent e) { }

    @Override
    public void windowDeiconified(WindowEvent e) { }

    @Override
    public void windowActivated(WindowEvent e) { }

    @Override
    public void windowDeactivated(WindowEvent e) { }
}
