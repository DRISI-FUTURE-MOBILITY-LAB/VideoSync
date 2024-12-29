/*
 * ****************************************************************
 * File: 			GraphPanel.java
 * Date Created:  	June 7, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To control and render each graph as its own
 *                  entity as to allow each graph to contain its
 *                  own elements and attributes as it is notified
 *                  from the Data Model
 *
 * Modified:		August 23, 2016
 * Programmer:		Danny Hale
 *                  Added the ability to move graph lines using
 *                  the mouse.
 * ****************************************************************
 */

package VideoSync.views.tabbed_panels.graphs;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.EDeviceType;
import VideoSync.objects.graphs.Line;
import VideoSync.objects.graphs.Region;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GraphPanel extends JPanel implements EventHandler, ActionListener, ItemListener, Observer, MouseListener, MouseMotionListener
{
    private static final long serialVersionUID = 1L;

    public static final int GRAPH_PREFERRED_WIDTH = 500;
    public static final int GRAPH_PREFERRED_HEIGHT = 57;
    public static final int GRAPH_LINE_BASE = 10;
    public static final int GRAPH_LINE_TOP = 47;

    // -- Graph Panel Variable Declarations

    /**
     * Used to request state data from the DataModelProxy so that the graph can be rendered
     */
    private final DataModelProxy dmp;

    /**
     * Used in managing the graph data and keeping it up to date.
     */
    private final GraphWindow panel_Graph;

    /**
     * Used in enabling all of the graph options
     */
    private final JCheckBox checkbox_Enabled;

    /**
     * Used to set whether this graph is displayed in the video as a region.
     */
    private final JCheckBox checkbox_ShowInVideo;

    /**
     * Used in selecting one of the available devices that have been detected.
     */
    private final JComboBox combo_Devices;

    /**
     * Used in selecting the appropriate channel based on the device selection.
     */
    private final JComboBox combo_Channel;

    /**
     * JavaFX Panel for the color picker
     */
    private final JFXPanel panel_ColorPicker;

    /**
     * The color picker used to select the color that the graph lines will be rendered in.
     * TODO: For visual consistency, might want to replace other UI elements with JavaFX versions. At the very least, combo boxes.
     */
    private ColorPicker graphColorPicker;

    private final CountDownLatch colorPickerWaitLatch = new CountDownLatch(1);

    /**
     * Used in keeping track of the chip number associated with the current channel
     */
    private int currChannelChip;

    /**
     * Used in keeping track of the chip number associated with the current channel
     */
    private int currChannelPin;

    /**
     * Used in keeping track of if the graph is enabled or not
     */
    private boolean graphEnabled = false;

    /**
     * Used in keeping track of the color that the graph line is going to be rendered with
     */
    private Color myColor = Color.BLACK;

    /**
     * Used for maintaining all of the control elements for the Graph
     */
    private final JPanel panel_Control;

    /**
     * Used for setting the combo_Channel to its default when a null model is used.
     */
    private final String[] defaultChannelModel = new String[]{"Choose a Channel"};

    /**
     * Used for keeping track of the starting point for a mouse drag
     */
    private int previousX;
    //private int previousY;

    /**
     * Used to update color, channel and enabled status of region drawn on video frame.
     */
    private final Vector<Region> videoRegions;

    /**
     * Used to keep track of which device type is used for each combo box index.
     */
    private Vector<EDeviceType> deviceTypes = new Vector<>();

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graph Panel Construction

    /**
     * Creates a graph panel object for rendering the state data for the channel that is selected.
     */
    public GraphPanel(DataModelProxy dmp)
    {
        this.dmp = dmp;
        addMouseListener(this);
        addMouseMotionListener(this);

        // Set the size, border, background and layout of the entire graph panel
        this.setPreferredSize(new Dimension(722, 59));
        this.setBorder(new LineBorder(new Color(30, 144, 255)));
        this.setBackground(Color.LIGHT_GRAY);
        this.setLayout(new BorderLayout(0, 0));

        // Create the graph's control panel, setting its size, border, and layout (this is null because we are using the absolute layout).
        panel_Control = new JPanel();
        panel_Control.setPreferredSize(new Dimension(244, 54));
        panel_Control.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        panel_Control.setLayout(null);
        // Add the control panel to the primary graph panel
        this.add(panel_Control, BorderLayout.WEST);

        // Create the devices combo box and add it to the control panel
        combo_Devices = new JComboBox();
        combo_Devices.setEnabled(false);
        combo_Devices.setModel(new DefaultComboBoxModel(new String[]{"Devices", "C1", "170"}));
        combo_Devices.setBounds(2, 2, 113, 25);
        combo_Devices.addActionListener(this);
        panel_Control.add(combo_Devices);

        // Create the channel combo box and add it to the control panel
        combo_Channel = new JComboBox();
        combo_Channel.setEnabled(false);
        combo_Channel.setBounds(2, 30, 155, 25);
        combo_Channel.addActionListener(this);
        combo_Channel.setModel(new DefaultComboBoxModel(defaultChannelModel));
        panel_Control.add(combo_Channel);

        //Set up color picker.
        panel_ColorPicker = new JFXPanel();
        panel_ColorPicker.setBounds(118, 2, 120, 25);
        GraphPanel selfReference = this;
        Platform.runLater(() -> {
            Group root = new Group();
            Scene fxScene = new Scene(root);
            graphColorPicker = new ColorPicker();
            graphColorPicker.setValue(javafx.scene.paint.Color.BLACK);
            graphColorPicker.setDisable(true);
            graphColorPicker.setMaxSize(120, 25);
            graphColorPicker.setOnAction(selfReference);
            root.getChildren().add(graphColorPicker);
            panel_ColorPicker.setScene(fxScene);

            colorPickerWaitLatch.countDown();
        });
        panel_Control.add(panel_ColorPicker);

        // Create the enabled check box and add it to the control panel
        checkbox_Enabled = new JCheckBox("Enabled");
        checkbox_Enabled.setBounds(157, 28, 82, 15);
        panel_Control.add(checkbox_Enabled);
        checkbox_Enabled.setEnabled(false);
        checkbox_Enabled.addItemListener(this);

        //Create the video check box and add it to the control panel
        checkbox_ShowInVideo = new JCheckBox("Video");
        checkbox_ShowInVideo.setBounds(157, 42, 82, 15);
        panel_Control.add(checkbox_ShowInVideo);
        checkbox_ShowInVideo.setEnabled(false);
        checkbox_ShowInVideo.addItemListener(this);

        // Create the graph pane and add it to the entire graph panel
        panel_Graph = new GraphWindow();
        panel_Graph.setPreferredSize(new Dimension(GRAPH_PREFERRED_WIDTH, GRAPH_PREFERRED_HEIGHT));
        panel_Graph.setLayout(new BorderLayout(0, 0));
        this.add(panel_Graph, BorderLayout.CENTER);

        //Create the vector containing video regions
        videoRegions = new Vector<>();
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Java Event Listeners
    // -- NOTE: Also includes the Observer 'update' method

    /**
     * Invoked when any of the swing combo boxes' values are changed.
     */
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        // If the source of the ActionEvent is the devices combo box, retrieve the list of
        // available channels for that device.
        if(ae.getSource() == combo_Devices)
        {
            // Create a temporary combo box element from the ActionEvent source.
            JComboBox combo = (JComboBox) ae.getSource();

            // Get the device of the selected item from the combo box.
            int comboIndex = combo.getSelectedIndex();
            if(comboIndex < deviceTypes.size())
            {
                EDeviceType device = deviceTypes.elementAt(comboIndex);

                // Set the combo box text from the information returned from the DataModelProxy
                setComboBoxText((combo.getSelectedIndex() == 0) ? null : this.dmp.getInputMapForDevice(device));

                //Update video regions for new device
                updateVideoRegions();
            }
            else
            {
                System.out.println("Requested device index " + comboIndex + " is out of bounds for loaded devices");
            }
        }

        // If the source of the ActionEvent is the channels combo box, get the channel that was
        // selected and have the graph present that channel data
        if(ae.getSource() == combo_Channel)
        {
            // Create a temporary combo box element from the ActionEven source
            JComboBox combo = (JComboBox) ae.getSource();

            // Get the string of the selected item from the combo box.
            String item = (String) combo.getSelectedItem();

            // If the selected item does not equal the conditions, then we want to have the graph update
            // its display with the selected channel
            if(item != null && !item.equals("Select a Device") && !item.equals("Choose a Channel"))
            {
                // Get the channel number from the item by requesting the Data model to return the appropriate
                // channel number for the selected device.
                setCurrChannelChip(this.dmp.getChannelChipNumberFromName(deviceTypes.elementAt(combo_Devices.getSelectedIndex()), item));
                setCurrChannelPin(this.dmp.getChannelPinNumberFromName(deviceTypes.elementAt(combo_Devices.getSelectedIndex()), item));
                // Tell the graph to update its contents.
                updateGraph();

                // Update video regions for channel
                updateVideoRegions();
            }
            else
            {
                // Set the panel graph states to null so we don't continue rendering them if we reach this point.
                panel_Graph.setStates(null);
            }
        }
    }

    /**
     * Invoked when the JavaFX color picker combo box is changed
     */
    public void handle(Event t)
    {
        //Ensure that the source is the color picker before updating.
        if(t.getSource() == graphColorPicker)
        {
            //Need to make sure handling occurs on main thread.
            SwingUtilities.invokeLater(() -> {
                javafx.scene.paint.Color fxColor = graphColorPicker.getValue();
                //Color needs to be converted from JavaFX to AWT/Swing.
                myColor = new Color((int) (fxColor.getRed() * 255), (int) (fxColor.getGreen() * 255), (int) (fxColor.getBlue() * 255));
                panel_Graph.setLineColor(myColor);
                updateVideoRegions();
            });
        }
    }

    /**
     * Invoked when the checkbox's value is changed.
     */
    public void itemStateChanged(ItemEvent e)
    {
        // Verify that the source is the enabled check box
        if(e.getSource() == checkbox_Enabled)
        {
            // Ensure that the data model proxy has detected that data was loaded
            // If not we don't do anything about it.
            if(dmp.dataLoaded())
            {
                // If the checkbox has been selected,
                // enable the graph's option boxes so they can
                // be selected by the user.
                // Otherwise disable them all
                graphEnabled = ((JCheckBox) e.getSource()).isSelected();
                enableBoxes();
                // Update the graph so it can reflect any changes that were made.
                updateGraph();

                //Update the video regions so they can reflect any changes that were made
                updateVideoRegions();
            }
        }

        //If the source is the show video checkbox, only need to call updateVideoRegions.
        if(e.getSource() == checkbox_ShowInVideo)
        {
            updateVideoRegions();
        }
    }

    /**
     * Invoked when the Data Model sends out a notification that an event changed that requires the
     * observers to pay attention to the data coming in
     */
    @Override
    public void update(Observable arg0, Object arg1)
    {
        // If the notification argument passed is a map, then we can update
        // the combo box text
        if(arg1 instanceof Map)
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
            setComboBoxText(deviceStrings);
        }

        // If the notification argument is a string, then either update the channel list depending on the
        // current combo box selection or have the panel reset its content to defaults.
        if(arg1 instanceof String)
        {
            if(this.graphEnabled && combo_Devices.getSelectedIndex() != 0)
            {
                if(arg1.equals("Input"))
                {
                    this.setComboBoxText(this.dmp.getInputMapForDevice(deviceTypes.elementAt(combo_Devices.getSelectedIndex())));
                }

                if(arg1.equals("Reset"))
                {
                    resetPanel();
                }

                if(arg1.equals("Mouse"))
                {
                    updateGraph();
                }
            }
        }

        // If the notification argument passes is an instance of the DataModelProxy,
        // we then update the graph
        if(arg1 instanceof DataModelProxy)
        {

            if(dmp.dataLoaded())
            {
                checkbox_Enabled.setEnabled(true);
            }

            updateGraph();

            updateVideoRegions();
        }
    }

    /**
     * Resets the graph panel when the DataModel notifies the panel that major changes took place and everything needs to reset.
     */
    private void resetPanel()
    {
        this.graphEnabled = false;
        this.checkbox_Enabled.setSelected(false);
        this.checkbox_ShowInVideo.setSelected(false);
        this.combo_Devices.setSelectedIndex(0);
        //this.combo_Color.setSelectedIndex(0);
        myColor = Color.BLACK;
        Platform.runLater(() -> graphColorPicker.setValue(javafx.scene.paint.Color.BLACK));

        this.combo_Channel.setSelectedIndex(0);
        videoRegions.clear();
        updateGraph();
        updateVideoRegions();
    }

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graphing Pane Update Events

    /**
     * Calculates the vertical tick marks for the graph so they will be rendered at the appropriate locations.
     */
    private void calculateTickMarks()
    {
        // Get the width and height of the graph panel so we can determine the spacing of the gap for the tick lines
        int width = panel_Graph.getSize().width;
        int height = panel_Graph.getSize().height;

        // Set the gap to be the width / how many lines to show

        int lWidth = width / 2;
        int rWidth = width / 2;

        double seconds = this.dmp.getGraphWindowSeconds();
        int ticksPer = 4;

        if(seconds < .5)
        {
            ticksPer *= 4;
        }
        else if(seconds > 16)
        {
            ticksPer = 2;
        }

        int lGap = (int) (lWidth / (seconds * ticksPer));
        int rGap = (int) (rWidth / (seconds * ticksPer));

        Vector<Line> ticks = new Vector<>();

        for(int i = lWidth; i > 0; i -= Math.max(1, lGap))
        {
            ticks.add(new Line(i, 0, i, height));
        }

        for(int i = lWidth; i < width + 1; i += Math.max(1, rGap))
        {
            ticks.add(new Line(i, 0, i, height));
        }

        // Send the tick array to the graph panel so they can be rendered
        panel_Graph.setTicks(ticks, this.dmp.getGraphWindowSeconds() == 64);
    }

    /**
     * Updates video regions to reflect current pane settings
     */
    private void updateVideoRegions()
    {
        for(Region videoRegion : videoRegions)
        {
            videoRegion.setEnabled(this.isCheckboxSelected() && isShowInVideoSelected());
            //With refactoring device strings to enumerated types, it is now possible that update gets called before the devices are known.
            //Check prevents out of bounds access.
            if(deviceTypes.size() > 0)
            {
                videoRegion.setDeviceType(deviceTypes.elementAt(combo_Devices.getSelectedIndex()));
                // FIXME: toString() may produce NullPointerException
                videoRegion.setChip(dmp.getChannelChipNumberFromName(deviceTypes.elementAt(combo_Devices.getSelectedIndex()), combo_Channel.getSelectedItem().toString()));
                videoRegion.setPin(dmp.getChannelPinNumberFromName(deviceTypes.elementAt(combo_Devices.getSelectedIndex()), combo_Channel.getSelectedItem().toString()));
            }
            videoRegion.setDisplayColor(myColor);
        }
    }

    /**
     * Calculates the state lines for the graph to display them in the graph window
     */
    public void calculateStateLines()
    {
        if(combo_Devices.getSelectedIndex() < deviceTypes.size())
        {
            // Get the device type from the device combo box.
            EDeviceType device = deviceTypes.elementAt(combo_Devices.getSelectedIndex());

            // If the device name equals Devices, then we nullify out the graph's state data
            // Otherwise get the state data from the data model proxy and pass it to the graph to be rendered
            if(device == EDeviceType.DEVICE_NONE || device == EDeviceType.MAX_DEVICES)
            {
                panel_Graph.setStates(null);
            }
            else
            {
                // Request the state data from the data model proxy and send it too the graph panel
                Vector<Line> states = dmp.getDataForChannel(device, currChannelChip, currChannelPin, panel_Graph.getSize().width, GRAPH_LINE_BASE, GRAPH_LINE_TOP);

                // Send the state data off to the graph for rendering
                panel_Graph.setStates(states);
            }
        }
        else
        {
            System.out.println("Unable to calculate state lines, selected device index is not in device types");
        }
    }

    /**
     * Updates the graph by rendering the tick & state lines
     */
    public void updateGraph()
    {
        // Calculate the tick marks to be rendered on the graph
        calculateTickMarks();

        // If the graph is enabled, go ahead and calculate the state lines
        // Otherwise nullify the state lines in the graph panel so they are not rendered
        if(this.graphEnabled)
        {
            calculateStateLines();
        }
        else
        {
            panel_Graph.setStates(null);
        }
    }

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graph Panel Getter's & Setters

    /**
     * Sets the combo box text from the object data that was passed to the function
     *
     * @param data data passed to update or actionPerformed method
     */
    public void setComboBoxText(Object data)
    {
        // If the data passed is an instance of a String array, we can set the
        // combo devices data model from the object that was passed
        // Otherwise if its not a String array, we are instead setting the channel data
        if(data instanceof String[])
        {
            // Set the combo box data to the string array that was sent
            combo_Devices.setModel(new DefaultComboBoxModel((String[]) data));
        }
        else if(data instanceof Vector)
        {
            int selectedIndex = combo_Channel.getSelectedIndex();

            // Get the size of the vector array that was passed.
            int dataSize = ((Vector<DeviceInputMap>) data).size();

            // Create a string array to the length of the data sent + 1
            // This allows us to place a default string at the first position
            String[] strings = new String[dataSize + 1];

            // Set the first element to our default string
            strings[0] = "Choose a Channel";

            // Create string a string array for each data element and place it in the string array
            for(int i = 1; i <= dataSize; i++)
            {
                strings[i] = ((Vector<DeviceInputMap>) data).elementAt(i - 1).getChannelName();
            }

            // Set the combo box data to the string array we just generated
            combo_Channel.setModel(new DefaultComboBoxModel(strings));

            //Used to re-select the channel if the input mapping has changed
            if(selectedIndex < strings.length)
            {
                combo_Channel.setSelectedIndex(selectedIndex);
            }
            else
            {    //else we probably changed devices and need reset the selection.
                combo_Channel.setSelectedIndex(0);
            }
        }
        else if(data == null)
        {
            combo_Channel.setModel(new DefaultComboBoxModel(this.defaultChannelModel));
            panel_Graph.setStates(null);
        }
    }

    /**
     * Set the chip number of the panel's current channel
     *
     * @param currChannelChip current channel's chip number
     */
    public void setCurrChannelChip(int currChannelChip)
    {
        this.currChannelChip = currChannelChip;
    }

    /**
     * Set the pin number of the panel's current channel
     *
     * @param currChannelPin current channel's pin number
     */
    public void setCurrChannelPin(int currChannelPin)
    {
        this.currChannelPin = currChannelPin;
    }

    public int getCurrChannelChip()
    {
        return currChannelChip;
    }

    public int getCurrChannelPin()
    {
        return currChannelPin;
    }

    public void mouseClicked(MouseEvent e)
    {

    }

    public void mousePressed(MouseEvent e)
    {
        previousX = e.getX();
        //previousY = e.getY();
    }

    public void mouseReleased(MouseEvent e)
    {
        int x = e.getX();
        int diff = previousX - x;

        dmp.increaseGraphOffset(diff);
        previousX = x;
    }

    public void mouseDragged(MouseEvent e)
    {
        int moveX = e.getX();

        //TODO - Figure out why we need to scale by ~1.6, and if we can reference a constant elsewhere rather than
        //       having it here as a magic number.
        int diff = (int) Math.round((previousX - moveX) * dmp.getGraphWindowSeconds() * 1.6);
        dmp.increaseGraphOffset(diff);
        previousX = moveX;
    }

    public void mouseMoved(MouseEvent e)
    {

    }

    public void mouseEntered(MouseEvent e)
    {
        //change cursor appearance to HAND_CURSOR when the mouse pointed on images
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void mouseExited(MouseEvent e)
    {
        setCursor(Cursor.getDefaultCursor());
    }

    public void enableBoxes()
    {
        combo_Devices.setEnabled(graphEnabled);
        combo_Channel.setEnabled(graphEnabled);

        //Because color picker is a JavaFX component, changing its state must occur on the JavaFX thread.
        Platform.runLater(() -> graphColorPicker.setDisable(!graphEnabled));

        checkbox_ShowInVideo.setEnabled(graphEnabled);
    }

    public void setCheckboxEnable(boolean b)
    {
        graphEnabled = b;
        checkbox_Enabled.setEnabled(true);
        checkbox_Enabled.setSelected(graphEnabled);
        enableBoxes();
    }

    public boolean isCheckboxSelected()
    {
        return checkbox_Enabled.isSelected();
    }

    public void setShowInVideo(boolean show)
    {
        checkbox_ShowInVideo.setSelected(show);

    }

    public boolean isShowInVideoSelected()
    {
        return checkbox_ShowInVideo.isSelected();
    }

    public void setDeviceIndex(int i)
    {
        if(i < combo_Devices.getItemCount())
        {
            combo_Devices.setSelectedIndex(i);
        }
        else
        {
            System.out.println("Unable to set graph device index to " + i + ", only " + combo_Devices.getItemCount() + " devices!");
        }
    }

    public int getDeviceIndex()
    {
        return combo_Devices.getSelectedIndex();
    }

    public void setChannelIndex(int i)
    {
        if(combo_Devices.getSelectedIndex() < deviceTypes.size())
        {
            if(i < combo_Channel.getItemCount())
            {
                combo_Channel.setSelectedIndex(i);
                setCurrChannelChip(dmp.getChannelChipNumberFromName(deviceTypes.elementAt(combo_Devices.getSelectedIndex()), (String) combo_Channel.getSelectedItem()));
                setCurrChannelPin(dmp.getChannelPinNumberFromName(deviceTypes.elementAt(combo_Devices.getSelectedIndex()), (String) combo_Channel.getSelectedItem()));
            }
            else
            {
                System.out.println("Unable to set graph channel index to " + i + ", only " + combo_Devices.getItemCount() + " devices!");
            }
        }
        else
        {
            System.out.println("Unable to set channel to index " + i + ", device index is invalid");
        }
    }

    public int getChannelIndex()
    {
        return combo_Channel.getSelectedIndex();
    }

    public void setColor(Color graphColor)
    {
        myColor = graphColor;
        //Color must be set on JavaFX thread.
        Platform.runLater(() -> {
            //Need to convert color to JavaFX color.
            javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(graphColor.getRed(), graphColor.getGreen(), graphColor.getBlue());
            graphColorPicker.setValue(fxColor);
        });

        panel_Graph.setLineColor(myColor);
        updateVideoRegions();
    }

    public Color getColor()
    {
        //Need to convert from JavaFX color to awt/swing color
        try
        {
            if(graphColorPicker == null)
                colorPickerWaitLatch.await();

            javafx.scene.paint.Color fxColor = graphColorPicker.getValue();
            return new Color((int) (fxColor.getRed() * 255), (int) (fxColor.getGreen() * 255), (int) (fxColor.getBlue() * 255));
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        return Color.BLACK;
    }

    public void addVideoRegion(Region videoRegion)
    {
        videoRegions.add(videoRegion);
        updateVideoRegions();
    }

    /**
     * Replaces the video region reference held by this graph panel if it is equal to oldRegion with newRegion.
     *
     * @param oldRegion Video region reference we're replacing
     * @param newRegion Video region reference that's replacing the oldRegion.
     */
    public void replaceVideoRegion(Region oldRegion, Region newRegion)
    {
        int index = videoRegions.indexOf(oldRegion);

        if(index > -1)
        {
            videoRegions.set(index, newRegion);
            updateVideoRegions();
        }
    }

    public Vector<Region> getVideoRegions()
    {
        return videoRegions;
    }

    /**
     * Invoked whenever a new graph panel is created at run time and the device combo box model needs
     * to be updated manually
     */
    public void updateDeviceModel()
    {
        Map<EDeviceType, String> loadedDevices = dmp.getDeviceList();
        //Get a set of devices sorted by key.
        Set<Map.Entry<EDeviceType, String>> devices = loadedDevices.entrySet();

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
        setComboBoxText(deviceStrings);
    }
}
