package VideoSync.views.modals.c1_viewer;

import VideoSync.analyzers.C1Analyzer;
import VideoSync.analyzers.C1DataCollector;
import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.EDeviceType;
import VideoSync.objects.InputMappingFile;
import VideoSync.objects.c1.C1Channel;
import VideoSync.objects.graphs.Line;
import VideoSync.views.modals.c1_viewer.commands.*;
import VideoSync.views.tabbed_panels.graphs.GraphWindow;
import VideoSync.views.textfilters.IntFilter;
import javafx.application.Platform;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

public class C1Viewer extends JFrame implements ActionListener, Observer
{
    public enum ReferenceMode
    {
        GENERATED_DATA
                {
                    public String toString()
                    {
                        return "Generated Data";
                    }
                },

        SELF_REFERENCE
                {
                    public String toString()
                    {
                        return "Self Reference";
                    }
                }
    }

    private DataModelProxy dmp;

    private JPanel c1Panel;
    private JComboBox<String> channelComboBox;
    private JSplitPane observedSplitPane;
    private JSplitPane referenceSplitPane;
    private JComboBox<String> deviceComboBox;
    private JButton loadC1DataButton;

    private JComboBox<String> varianceModeComboBox;
    private JCheckBox varianceCheckBox;

    private String c1FileName = "";
    private JLabel dataLabel;
    private JLabel dataFileNameLabel;
    private JTextField varianceTextField;
    private JButton ungroupButton;
    private JButton groupButton;
    private JButton falseEventButton;
    private JButton autoGroupButton;
    private JButton saveButton;
    private JButton loadButton;
    private JButton ignoreButton;
    private JButton clearButton;
    private JButton csvButton;

    private ReferenceMode currentReferenceMode;

    private JComboBox<ReferenceMode> referenceDataModeComboBox;
    private JComboBox<String> referenceDataDeviceComboBox;
    private JComboBox<String> referenceDataChannelComboBox;
    private JPanel observedReferenceModePanel;
    private JPanel generatedLoadDataPanel;

    private CommandAutoGroup commandAutoGroup;
    private CommandClearAutoGroup commandClearAutoGroup;
    private CommandGroup commandGroup;
    private CommandUngroup commandUngroup;
    private CommandIgnore commandIgnore;
    private CommandFalseEvent commandFalseEvent;
    private CommandSaveSession commandSaveSession;
    private CommandLoadSession commandLoadSession;
    private CommandExportDataToCSV commandExportDataToCSV;
    private CommandLoadReferenceC1 commandLoadReferenceC1;

    private C1ViewerGraphPane observedGraphPane;
    private C1ViewerGraphPane referenceGraphPane;

    private JLabel missingChannelLabel;
    private JLabel dataNotLoadedLabel;

    private C1Analyzer c1Analyzer;

    private C1DataCollector c1DataCollector;

    private boolean referenceC1Loaded = false;
    private boolean drawReferenceGraph = false;

    private Vector<C1Channel> observedEventChannels;
    private Vector<C1Channel> generatedEventChannels;

    private boolean startBoxDragging;

    private int mouseDownXObserved;
    private int mouseDownYObserved;
    private int latestObsSelectionBoxWidth;
    private int latestObsSelectionBoxHeight;

    private int mouseDownXReference;
    private int mouseDownYReference;
    private int latestRefSelectionBoxWidth;
    private int latestRefSelectionBoxHeight;

    /**
     * Used for setting the combo_Channel to its default when a null model is used.
     */
    private final String[] defaultChannelModel = new String[]{"Choose a Channel"};

    /**
     * Used in keeping track of the chip number associated with the current channel
     */
    private int currObsChannelChip;

    /**
     * Used in keeping track of the pin number associated with the current channel
     */
    private int currObsChannelPin;

    private int currRefChannelChip;

    private int currRefChannelPin;

    /**
     * Used in keeping track of the color that the graph line is going to be rendered with
     */
    private Color myColor = Color.BLACK;

    /**
     * Used to keep track of which device type is used for each combo box index.
     */
    private Vector<EDeviceType> deviceTypes = new Vector<>();

    private static class IsShiftPressed
    {
        private static volatile boolean shiftPressed = false;
        public static boolean isShiftPressed()
        {
            synchronized (IsShiftPressed.class)
            {
                return shiftPressed;
            }
        }

        public static void main(String[] args)
        {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ke -> {
                synchronized (IsShiftPressed.class)
                {
                    switch (ke.getID())
                    {
                        case KeyEvent.KEY_PRESSED:
                            if (ke.getKeyCode() == KeyEvent.VK_SHIFT)
                                shiftPressed = true;
                            break;

                        case KeyEvent.KEY_RELEASED:
                            if (ke.getKeyCode() == KeyEvent.VK_SHIFT)
                                shiftPressed = false;
                            break;
                    }
                    return false;
                }
            });
        }
    }

    public C1Viewer()
    {
        setContentPane(c1Panel);
        setSize(1000, 400);
        setTitle("C1 Viewer");

        startBoxDragging = false;

        IsShiftPressed.main(new String[0]);
    }

    public void setDataModelProxy(DataModelProxy dataModelProxy)
    {
        dmp = dataModelProxy;
    }

    public void displayPanel()
    {
        Vector<InputMappingFile> inputMappingFiles = dmp.getInputMappingFiles();
        boolean c1Loaded = false;

        Vector<File> dataFiles = dmp.getDataFiles();
        for(File file : dataFiles)
        {
            if(file.getName().endsWith(".c1"))
            {
                c1Loaded = true;
                break;
            }
        }

        if(inputMappingFiles.isEmpty() || !c1Loaded)
        {
            setEnabled(false);
            JOptionPane.showMessageDialog(this, "An input mapping file and a C1 data file"
                    + "\nmust be loaded in to VideoSync to utilize the C1 viewer");
            this.dispose();
            return;
        }
        else
        {
            setEnabled(true);
        }

        setVisible(true);
    }

    public void initPanel()
    {
        channelComboBox.addActionListener(this);
        deviceComboBox.addActionListener(this);
        referenceDataChannelComboBox.addActionListener(this);
        referenceDataDeviceComboBox.addActionListener(this);

        channelComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Choose a channel"}));
        deviceComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Select a Device", "C1"}));
        referenceDataChannelComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Choose a channel"}));
        referenceDataDeviceComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"Selected a Device", "C1"}));

        observedGraphPane = new C1ViewerGraphPane();
        observedGraphPane.setPreferredSize(new Dimension(0, 0));
        observedGraphPane.setLayout(new BorderLayout(0, 0));
        observedGraphPane.setLineColor(myColor);
        observedGraphPane.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    selectCorrespondingObservedEvent(e, false);

                    startBoxDragging = true;
                    mouseDownXObserved = e.getX();
                    mouseDownYObserved = e.getY();
                    latestObsSelectionBoxWidth = 0;
                    latestObsSelectionBoxHeight = 0;
                    observedGraphPane.setSelectionRectangle(mouseDownXObserved, mouseDownYObserved, 0, 0);
                }
                else if(e.getButton() == MouseEvent.BUTTON2)
                {
                    selectCorrespondingObservedEvent(e, true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    int x = Math.min(e.getX(), mouseDownXObserved);
                    int y = Math.min(e.getY(), mouseDownYObserved);
                    boxSelectCorrespondingObservedEvent(e, x, y, latestObsSelectionBoxWidth, latestObsSelectionBoxHeight);

                    startBoxDragging = false;
                    latestObsSelectionBoxWidth = 0;
                    latestObsSelectionBoxHeight = 0;
                    observedGraphPane.setSelectionRectangle(mouseDownXObserved, mouseDownYObserved, 0, 0);
                }
            }
        });
        observedGraphPane.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                if(startBoxDragging)
                {
                    int x = Math.min(e.getX(), mouseDownXObserved);
                    int y = Math.min(e.getY(), mouseDownYObserved);
                    latestObsSelectionBoxWidth = Math.abs(e.getX() - mouseDownXObserved);
                    latestObsSelectionBoxHeight = Math.abs(e.getY() - mouseDownYObserved);
                    observedGraphPane.setSelectionRectangle(x, y, latestObsSelectionBoxWidth, latestObsSelectionBoxHeight);
                }
            }
        });
        observedSplitPane.setRightComponent(observedGraphPane);

        currentReferenceMode = ReferenceMode.GENERATED_DATA;

        referenceDataModeComboBox.setModel(new DefaultComboBoxModel<>(ReferenceMode.values()));
        referenceDataModeComboBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED)
            {
                if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
                {
                    Platform.runLater(() -> {
                        if(currentReferenceMode != ReferenceMode.GENERATED_DATA)
                        {
                            if(c1DataCollector.collectedDataExists())
                            {
                                int option = JOptionPane.showConfirmDialog(this, "Switching modes will cause any unsaved C1 Viewer data to be lost. Are you sure?", "Warning", JOptionPane.YES_NO_OPTION);

                                if(option == JOptionPane.YES_OPTION)
                                {
                                    c1DataCollector.resetCollectedData();
                                }
                                else
                                {
                                    referenceDataModeComboBox.setSelectedItem(ReferenceMode.SELF_REFERENCE);
                                    return;
                                }
                            }

                            generatedLoadDataPanel.setVisible(true);
                            generatedLoadDataPanel.setEnabled(true);
                            observedReferenceModePanel.setVisible(false);
                            observedReferenceModePanel.setEnabled(false);
                            currentReferenceMode = ReferenceMode.GENERATED_DATA;

                            setCurrRefChannelChip(currObsChannelChip);
                            setCurrRefChannelPin(currObsChannelPin);

                            revalidate();
                            updateGraph();
                        }
                    });
                }
                else if(isCurrentReferenceMode(ReferenceMode.SELF_REFERENCE))
                {
                    if(currentReferenceMode != ReferenceMode.SELF_REFERENCE)
                    {
                        if(c1DataCollector.collectedDataExists())
                        {
                            int option = JOptionPane.showConfirmDialog(this, "Switching modes will cause any unsaved C1 Viewer data to be lost. Are you sure?", "Warning", JOptionPane.YES_NO_OPTION);

                            if(option == JOptionPane.YES_OPTION)
                            {
                                c1DataCollector.resetCollectedData();
                            }
                            else
                            {
                                referenceDataModeComboBox.setSelectedItem(ReferenceMode.GENERATED_DATA);
                                return;
                            }
                        }

                        generatedLoadDataPanel.setVisible(false);
                        generatedLoadDataPanel.setEnabled(false);
                        observedReferenceModePanel.setVisible(true);
                        observedReferenceModePanel.setEnabled(true);
                        currentReferenceMode = ReferenceMode.SELF_REFERENCE;

                        String item = (String) referenceDataChannelComboBox.getSelectedItem();
                        setCurrRefChannelChip(this.dmp.getChannelChipNumberFromName(deviceTypes.elementAt(referenceDataDeviceComboBox.getSelectedIndex()), item));
                        setCurrRefChannelPin(this.dmp.getChannelPinNumberFromName(deviceTypes.elementAt(referenceDataDeviceComboBox.getSelectedIndex()), item));

                        revalidate();
                        updateGraph();
                    }
                }
            }
        });

        generatedLoadDataPanel.setVisible(true);
        generatedLoadDataPanel.setEnabled(true);
        observedReferenceModePanel.setVisible(false);
        observedReferenceModePanel.setEnabled(false);

        referenceGraphPane = new C1ViewerGraphPane();
        referenceGraphPane.setPreferredSize(new Dimension(0, 0));
        referenceGraphPane.setLayout(new BorderLayout(0, 0));
        referenceGraphPane.setLineColor(myColor);
        referenceGraphPane.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    selectCorrespondingReferenceEvent(e, false);

                    startBoxDragging = true;
                    mouseDownXReference = e.getX();
                    mouseDownYReference = e.getY();
                    latestRefSelectionBoxWidth = 0;
                    latestRefSelectionBoxHeight = 0;
                    referenceGraphPane.setSelectionRectangle(mouseDownXReference, mouseDownYReference, 0, 0);
                }
                else if(e.getButton() == MouseEvent.BUTTON2)
                {
                    selectCorrespondingReferenceEvent(e, true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    int x = Math.min(e.getX(), mouseDownXReference);
                    int y = Math.min(e.getY(), mouseDownYReference);
                    boxSelectCorrespondingReferenceEvent(e, x, y, latestRefSelectionBoxWidth, latestRefSelectionBoxHeight);

                    startBoxDragging = false;
                    latestRefSelectionBoxWidth = 0;
                    latestRefSelectionBoxHeight = 0;
                    referenceGraphPane.setSelectionRectangle(mouseDownXReference, mouseDownYReference, 0, 0);
                }
            }
        });
        referenceGraphPane.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                if(startBoxDragging)
                {
                    int x = Math.min(e.getX(), mouseDownXReference);
                    int y = Math.min(e.getY(), mouseDownYReference);
                    latestRefSelectionBoxWidth = Math.abs(e.getX() - mouseDownXReference);
                    latestRefSelectionBoxHeight = Math.abs(e.getY() - mouseDownYReference);
                    referenceGraphPane.setSelectionRectangle(x, y, latestRefSelectionBoxWidth, latestRefSelectionBoxHeight);
                }
            }
        });
        referenceSplitPane.setRightComponent(referenceGraphPane);

        missingChannelLabel = new JLabel("Selected channel is not present in reference data");
        missingChannelLabel.setHorizontalAlignment(JLabel.CENTER);
        String fontName = missingChannelLabel.getFont().getFontName();
        int fontStyle = missingChannelLabel.getFont().getStyle();
        missingChannelLabel.setFont(new Font(fontName, fontStyle, 20));

        dataNotLoadedLabel = new JLabel("No reference C1 data has been loaded");
        dataNotLoadedLabel.setHorizontalAlignment(JLabel.CENTER);
        dataNotLoadedLabel.setFont(new Font(fontName, fontStyle, 20));

        varianceModeComboBox.addActionListener(this);
        varianceCheckBox.addActionListener(this);

        IntFilter intFilter = new IntFilter();
        ((PlainDocument) varianceTextField.getDocument()).setDocumentFilter(intFilter);

        c1Analyzer = new C1Analyzer();

        commandLoadReferenceC1 = new CommandLoadReferenceC1("Load Data");
        commandLoadReferenceC1.setTargets(c1Analyzer, this, dmp);

        loadC1DataButton.setAction(commandLoadReferenceC1);

        c1DataCollector = new C1DataCollector(this, dmp, observedGraphPane, referenceGraphPane);

        commandAutoGroup = new CommandAutoGroup("Auto Group");
        commandAutoGroup.setTargets(c1DataCollector);
        autoGroupButton.setAction(commandAutoGroup);

        commandClearAutoGroup = new CommandClearAutoGroup("Clear");
        commandClearAutoGroup.setTargets(c1DataCollector);
        clearButton.setAction(commandClearAutoGroup);

        commandGroup = new CommandGroup("Group");
        commandGroup.setTargets(c1DataCollector);
        groupButton.setAction(commandGroup);

        commandUngroup = new CommandUngroup("Ungroup");
        commandUngroup.setTargets(c1DataCollector);
        ungroupButton.setAction(commandUngroup);

        commandFalseEvent = new CommandFalseEvent("False Event");
        commandFalseEvent.setTargets(c1DataCollector);
        falseEventButton.setAction(commandFalseEvent);

        commandIgnore = new CommandIgnore("Ignore");
        commandIgnore.setTargets(c1DataCollector);
        ignoreButton.setAction(commandIgnore);

        commandSaveSession = new CommandSaveSession("Save");
        commandSaveSession.setTargets(c1DataCollector, dmp);
        saveButton.setAction(commandSaveSession);

        commandLoadSession = new CommandLoadSession("Load");
        commandLoadSession.setTargets(this, dmp);
        loadButton.setAction(commandLoadSession);

        commandExportDataToCSV = new CommandExportDataToCSV("CSV");
        commandExportDataToCSV.setTargets(c1DataCollector, this, dmp);
        csvButton.setAction(commandExportDataToCSV);

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                updateGraph();
            }
        });
    }

    public void loadC1DataCollector(C1DataCollector cdc)
    {
        cdc.loadC1DataCollector(dmp, this, observedGraphPane, referenceGraphPane);
        this.c1DataCollector = cdc;

        commandAutoGroup.setTargets(c1DataCollector);
        commandClearAutoGroup.setTargets(c1DataCollector);
        commandGroup.setTargets(c1DataCollector);
        commandUngroup.setTargets(c1DataCollector);
        commandFalseEvent.setTargets(c1DataCollector);
        commandIgnore.setTargets(c1DataCollector);
        commandSaveSession.setTargets(c1DataCollector, dmp);
        commandExportDataToCSV.setTargets(c1DataCollector, this, dmp);

        calculateStateLines();
    }

    public Vector<C1Channel> getObservedEventChannels()
    {
        return (Vector<C1Channel>) observedEventChannels.clone();
    }

    public Vector<C1Channel> getReferenceEventChannels()
    {
        if(isCurrentReferenceMode(ReferenceMode.SELF_REFERENCE))
            return (Vector<C1Channel>) observedEventChannels.clone();
        else
            return (Vector<C1Channel>) generatedEventChannels.clone();
    }

    public void actionPerformed(ActionEvent ae)
    {
        // If the source of the ActionEvent is the devices combo box, retrieve the list of
        // available channels for that device.
        if(ae.getSource() == deviceComboBox || ae.getSource() == referenceDataDeviceComboBox)
        {
            // Create a temporary combo box element from the ActionEvent source.
            // FIXME: Make sure cast is safe
            JComboBox<String> combo = (JComboBox<String>) ae.getSource();

            // Get the device of the selected item from the combo box.
            int comboIndex = combo.getSelectedIndex();
            if(comboIndex < deviceTypes.size())
            {
                EDeviceType device = deviceTypes.elementAt(comboIndex);

                // Set the combo box text from the information returned from the DataModelProxy
                setComboBoxText((combo.getSelectedIndex() == 0) ? null : this.dmp.getInputMapForDevice(device));
            }
            else
            {
                System.out.println("Requested device index " + comboIndex + " is out of bounds for loaded devices");
            }
        }

        // If the source of the ActionEvent is the channels combo box, get the channel that was
        // selected and have the graph present that channel data
        if(ae.getSource() == channelComboBox || ae.getSource() == referenceDataChannelComboBox)
        {
            c1DataCollector.clearSelectedEvents();
            // Create a temporary combo box element from the ActionEvent source
            JComboBox<String> combo = (JComboBox<String>) ae.getSource();

            // Get the string of the selected item from the combo box.
            String item = (String) combo.getSelectedItem();

            // If the selected item does not equal the conditions, then we want to have the graph update
            // its display with the selected channel
            if(item != null && !item.equals("Select a Device") && !item.equals("Choose a Channel"))
            {
                // Get the channel number from the item by requesting the Data model to return the appropriate
                // channel number for the selected device.
                if(ae.getSource() == channelComboBox)
                {
                    setCurrObsChannelChip(this.dmp.getChannelChipNumberFromName(deviceTypes.elementAt(deviceComboBox.getSelectedIndex()), item));
                    setCurrObsChannelPin(this.dmp.getChannelPinNumberFromName(deviceTypes.elementAt(deviceComboBox.getSelectedIndex()), item));

                    if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
                    {
                        setCurrRefChannelChip(this.dmp.getChannelChipNumberFromName(deviceTypes.elementAt(deviceComboBox.getSelectedIndex()), item));
                        setCurrRefChannelPin(this.dmp.getChannelPinNumberFromName(deviceTypes.elementAt(deviceComboBox.getSelectedIndex()), item));
                    }
                }
                else if(ae.getSource() == referenceDataChannelComboBox)
                {
                    setCurrRefChannelChip(this.dmp.getChannelChipNumberFromName(deviceTypes.elementAt(deviceComboBox.getSelectedIndex()), item));
                    setCurrRefChannelPin(this.dmp.getChannelPinNumberFromName(deviceTypes.elementAt(deviceComboBox.getSelectedIndex()), item));
                }

                // Tell the graph to update its contents.
                updateGraph();
            }
            else
            {
                // Set the panel graph states to null so that we don't continue rendering them if we reach this point.
                if(ae.getSource() == channelComboBox)
                {
                    observedGraphPane.setStates(null);
                    observedGraphPane.setVarianceLines(null);
                    observedGraphPane.setSelectionHighlights(null);
                    observedGraphPane.setGroupIdentifiers(null);

                    if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
                    {
                        referenceGraphPane.setStates(null);
                        referenceGraphPane.setVarianceLines(null);
                        referenceGraphPane.setSelectionHighlights(null);
                        referenceGraphPane.setGroupIdentifiers(null);
                    }
                }
                else if(ae.getSource() == referenceDataChannelComboBox)
                {
                    referenceGraphPane.setStates(null);
                    referenceGraphPane.setVarianceLines(null);
                    referenceGraphPane.setSelectionHighlights(null);
                    referenceGraphPane.setGroupIdentifiers(null);
                }
            }
        }

        if(ae.getSource() == varianceCheckBox || ae.getSource() == varianceModeComboBox)
        {
            updateGraph();
        }
    }

    public void update(Observable obs, Object obj)
    {
        // If the notification argument passed is a map, then we can update
        // the combo box text
        if(obj instanceof Map)
        {
            //Get a set of devices sorted by key.
            // FIXME: Make this cast safer
            Set<Map.Entry<EDeviceType, String>> devices = ((Map<EDeviceType, String>) obj).entrySet();

            //Loop through each device and store device type and display string separately.
            //Use an array instead of vector for deviceStrings to ensure that order does not change.
            int index = 0;
            String[] deviceStrings = new String[devices.size()];
            Vector<EDeviceType> deviceTypes = new Vector<>();
            for(Map.Entry<EDeviceType, String> device : devices)
            {
                if(device.getKey() == EDeviceType.DEVICE_C1 || device.getKey() == EDeviceType.DEVICE_NONE)
                {
                    deviceTypes.add(device.getKey());
                    deviceStrings[index] = device.getValue();
                    index++;
                }
            }

            //Set device types and combo box strings.
            //The index of the combo box should correspond to the index of deviceTypes.
            this.deviceTypes = deviceTypes;
            setComboBoxText(deviceStrings);

            observedEventChannels = dmp.getC1AnalyzerChannels();

            // We need to reset any loaded generated data
            c1DataCollector.resetCollectedData();
            setReferenceC1Loaded(false);
            setC1FileName("");
            setGeneratedEventChannels(null);
            commandLoadReferenceC1.setTargets(resetAnalyzer(), this, dmp);

            // Reset chip and pin
            setCurrObsChannelChip(0);
            setCurrObsChannelPin(0);
            setCurrRefChannelChip(0);
            setCurrRefChannelPin(0);
        }

        if(obj instanceof String)
        {
            if(deviceComboBox.getSelectedIndex() != 0)
            {
                if(obj.equals("Input"))
                {
                    setComboBoxText(this.dmp.getInputMapForDevice(deviceTypes.elementAt(deviceComboBox.getSelectedIndex())));
                }

                if(obj.equals("Reset"))
                {
                    resetPanel();
                }

                if(obj.equals("Mouse"))
                {
                    updateGraph();
                }
            }
        }

        boolean c1Loaded = false;

        Vector<File> dataFiles = dmp.getDataFiles();
        for(File file : dataFiles)
        {
            if(file.getName().endsWith(".c1"))
            {
                c1Loaded = true;
                break;
            }
        }

        if(obj instanceof DataModelProxy && c1Loaded)
        {
            updateGraph();
        }
    }

    /**
     * Resets the graph panel when the DataModel notifies the panel that major changes took place and everything needs to reset.
     */
    private void resetPanel()
    {
        this.deviceComboBox.setSelectedIndex(0);
        this.channelComboBox.setSelectedIndex(0);

        this.referenceDataDeviceComboBox.setSelectedIndex(0);
        this.referenceDataChannelComboBox.setSelectedIndex(0);

        updateGraph();
    }

    /**
     * Calculates the vertical tick marks for the graph so they will be rendered at the appropriate locations.
     */
    private void calculateTickMarks(GraphWindow pane)
    {
        // Get the width and height of the graph panel so we can determine the spacing of the gap for the tick lines
        int width = pane.getSize().width;
        int height = pane.getSize().height;

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
        pane.setTicks(ticks, this.dmp.getGraphWindowSeconds() == 64);
    }

    /**
     * Calculates the state lines for the graph to display them in the graph window
     */
    public void calculateStateLines()
    {
        if(deviceComboBox.getSelectedIndex() < deviceTypes.size())
        {
            // Get the device type from the device combo box.
            EDeviceType device = deviceTypes.elementAt(deviceComboBox.getSelectedIndex());

            // If the device name equals Devices, then we nullify out the graph's state data
            // Otherwise get the state data from the data model proxy and pass it to the graph to be rendered
            if(device == EDeviceType.DEVICE_NONE || device == EDeviceType.MAX_DEVICES)
            {
                observedGraphPane.setStates(null);
                referenceGraphPane.setStates(null);

                observedGraphPane.setVarianceLines(null);
                referenceGraphPane.setVarianceLines(null);

                observedGraphPane.setSelectionHighlights(null);
                referenceGraphPane.setSelectionHighlights(null);

                observedGraphPane.setGroupIdentifiers(null);
                referenceGraphPane.setGroupIdentifiers(null);
            }
            else
            {
                int base = (int) (observedGraphPane.getSize().height * 0.25);
                int top = (int) (observedGraphPane.getSize().height * 0.75);

                // Request the state data from the data model proxy and send it too the graph panel
                Vector<Line> states = dmp.getDataForChannel(EDeviceType.DEVICE_C1, currObsChannelChip, currObsChannelPin, observedGraphPane.getSize().width, base, top);

                int variance = 200;
                if(!varianceTextField.getText().isEmpty())
                    variance = Math.max(Integer.parseInt(varianceTextField.getText()), 1);

                if(varianceCheckBox.isSelected())
                {
                    Vector<Line> varianceObserved = dmp.getVarianceDataForC1Viewer(observedGraphPane.getSize().width, observedGraphPane.getSize().height, currObsChannelChip, currObsChannelPin, variance, getVarianceMode());
                    observedGraphPane.setVarianceLines(varianceObserved);
                }
                else
                {
                    observedGraphPane.setVarianceLines(null);
                }

                // Send the state data off to the graph for rendering
                observedGraphPane.setStates(states);
                observedGraphPane.setSelectionHighlights(c1DataCollector.getObservedSelectionHighlights());
                observedGraphPane.setGroupIdentifiers(c1DataCollector.getObservedGroupIdentifiers(getCurrObsChannelChip(), getCurrObsChannelPin(), getCurrRefChannelChip(), getCurrRefChannelPin()));

                if((referenceC1Loaded || isCurrentReferenceMode(ReferenceMode.SELF_REFERENCE)) && drawReferenceGraph)
                {
                    base = (int) (referenceGraphPane.getSize().height * 0.25);
                    top = (int) (referenceGraphPane.getSize().height * 0.75);
                    // Get the graph events from the C1 Analysis
                    Vector<Line> referenceStates;
                    if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
                        referenceStates = c1Analyzer.getGraphLines(referenceGraphPane.getSize().width, dmp.getCurrentPosition(), dmp.getGraphWindowSeconds(), currRefChannelChip, currRefChannelPin, (top * 1.0), (base * 1.0));
                    else
                        referenceStates = dmp.getDataForChannel(EDeviceType.DEVICE_C1, currRefChannelChip, currRefChannelPin, referenceGraphPane.getSize().width, base, top);

                    //If we have no events and are at beginning of graph, return the first events so that an extended graph can be displayed
                    // Does not apply if current mode is SELF_REFERENCE since getDataForChannel handle's this part automatically
                    if(((referenceStates == null) || (referenceStates.isEmpty())) && (c1Analyzer.getMaxTimeInMillis() > (dmp.getCurrentPosition())) && isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
                    {
                        referenceStates = c1Analyzer.getGraphLines(referenceGraphPane.getSize().width, 0, 0, currRefChannelChip, currRefChannelPin, (top * 1.0), (base * 1.0));
                    }

                    if(varianceCheckBox.isSelected())
                    {
                        Vector<Line> varianceReference = dmp.getVarianceDataForC1Viewer(referenceGraphPane.getSize().width, referenceGraphPane.getSize().height, currRefChannelChip, currRefChannelPin, variance, getVarianceMode());
                        referenceGraphPane.setVarianceLines(varianceReference);
                    }
                    else
                    {
                        referenceGraphPane.setVarianceLines(null);
                    }

                    referenceGraphPane.setStates(referenceStates);
                    referenceGraphPane.setSelectionHighlights(c1DataCollector.getReferenceSelectionHighlights());
                    referenceGraphPane.setGroupIdentifiers(c1DataCollector.getReferenceGroupIdentifiers(getCurrObsChannelChip(), getCurrObsChannelPin(), getCurrRefChannelChip(), getCurrRefChannelPin()));
                }
            }
        }
        else
        {
            System.out.println("Unable to calculate state lines, selected device index is not in device types");
        }
    }

    private void updateReferenceGraph()
    {
        boolean hasC1Channel = false;

        if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
        {
            for(C1Channel channel : c1Analyzer.getC1Channels())
            {
                if(channel.getChip() == currRefChannelChip && channel.getPin() == currRefChannelPin)
                {
                    hasC1Channel = true;
                    break;
                }
            }
        }
        else
        {
            hasC1Channel = true;
        }

        if(referenceC1Loaded || isCurrentReferenceMode(ReferenceMode.SELF_REFERENCE))
        {
            if(!hasC1Channel && referenceSplitPane.getRightComponent() != missingChannelLabel)
            {
                referenceSplitPane.setRightComponent(missingChannelLabel);
                drawReferenceGraph = false;
            }
            else if(hasC1Channel && (referenceSplitPane.getRightComponent() != referenceGraphPane || isCurrentReferenceMode(ReferenceMode.SELF_REFERENCE)))
            {
                referenceSplitPane.setRightComponent(referenceGraphPane);
                drawReferenceGraph = true;
            }
        }
        else
        {
            referenceSplitPane.setRightComponent(dataNotLoadedLabel);
            drawReferenceGraph = false;
        }

        dataFileNameLabel.setText(c1FileName);

        referenceSplitPane.setDividerLocation(observedSplitPane.getDividerLocation());
        //observedSplitPane.getLeftComponent().setPreferredSize(new Dimension(-1, observedSplitPane.getRightComponent().getHeight()));
        revalidate();
    }

    /**
     * Updates the graph by rendering the tick & state lines
     */
    public synchronized void updateGraph()
    {
        myColor = dmp.getGraphPanelColor(currObsChannelChip, currObsChannelPin);

        updateReferenceGraph();

        // Calculate the tick marks to be rendered on the graph
        calculateTickMarks(observedGraphPane);
        calculateTickMarks(referenceGraphPane);

        // If a device and channel are chosen draw state lines
        // Otherwise nullify the state lines in the graph panel so they are not rendered
        if(channelComboBox.getSelectedIndex() != 0 && deviceComboBox.getSelectedIndex() != 0)
        {
            // Calculate the state lines
            calculateStateLines();
        }
        else
        {
            observedGraphPane.setStates(null);
            observedGraphPane.setVarianceLines(null);
            observedGraphPane.setSelectionHighlights(null);
            observedGraphPane.setGroupIdentifiers(null);

            if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
            {
                referenceGraphPane.setStates(null);
                referenceGraphPane.setVarianceLines(null);
                referenceGraphPane.setSelectionHighlights(null);
                referenceGraphPane.setGroupIdentifiers(null);
            }
        }

        if(isCurrentReferenceMode(ReferenceMode.SELF_REFERENCE))
        {
            if(referenceDataChannelComboBox.getSelectedIndex() != 0 && referenceDataDeviceComboBox.getSelectedIndex() != 0)
            {
                calculateStateLines();
            }
            else
            {
                referenceGraphPane.setStates(null);
                referenceGraphPane.setVarianceLines(null);
                referenceGraphPane.setSelectionHighlights(null);
                referenceGraphPane.setGroupIdentifiers(null);
            }
        }

        observedGraphPane.setLineColor(myColor);
        referenceGraphPane.setLineColor(myColor);
    }

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
            deviceComboBox.setModel(new DefaultComboBoxModel<>((String[]) data));
            referenceDataDeviceComboBox.setModel(new DefaultComboBoxModel<>((String[]) data));
        }
        else if(data instanceof Vector)
        {
            int selectedIndex = channelComboBox.getSelectedIndex();
            int referenceDataSelectedIndex = referenceDataChannelComboBox.getSelectedIndex();

            // Get the size of the vector array that was passed.
            // FIXME: Make sure this cast is safe
            int dataSize = ((Vector<DeviceInputMap>) data).size();

            // Create a string array to the length of the data sent + 1
            // This allows us to place a default string at the first position
            String[] strings = new String[dataSize + 1];

            // Set the first element to our default string
            strings[0] = "Choose a Channel";

            // Create string a string array for each data element and place it in the string array
            for(int i = 1; i <= dataSize; i++)
            {
                // FIXME: Make sure this cast is safe
                strings[i] = ((Vector<DeviceInputMap>) data).elementAt(i - 1).getChannelName();
            }

            // Set the combo box data to the string array we just generated
            // FIXME: Make sure this assignment and call is safe
            channelComboBox.setModel(new DefaultComboBoxModel<>(strings));
            referenceDataChannelComboBox.setModel(new DefaultComboBoxModel<>(strings));

            //Used to re-select the channel if the input mapping has changed
            if(selectedIndex < strings.length)
            {
                channelComboBox.setSelectedIndex(selectedIndex);
                referenceDataChannelComboBox.setSelectedIndex(referenceDataSelectedIndex);
            }
            else
            {    //else we probably changed devices and need reset the selection.
                channelComboBox.setSelectedIndex(0);
                referenceDataChannelComboBox.setSelectedIndex(0);
            }
        }
        else if(data == null)
        {
            channelComboBox.setModel(new DefaultComboBoxModel<>(defaultChannelModel));
            referenceDataChannelComboBox.setModel(new DefaultComboBoxModel<>(defaultChannelModel));

            observedGraphPane.setStates(null);
            referenceGraphPane.setStates(null);

            observedGraphPane.setVarianceLines(null);
            referenceGraphPane.setVarianceLines(null);

            observedGraphPane.setSelectionHighlights(null);
            referenceGraphPane.setSelectionHighlights(null);

            observedGraphPane.setGroupIdentifiers(null);
            referenceGraphPane.setGroupIdentifiers(null);
        }
    }

    /**
     * Set the chip number of the panel's current channel
     *
     * @param currObsChannelChip current channel's chip number
     */
    public void setCurrObsChannelChip(int currObsChannelChip)
    {
        this.currObsChannelChip = currObsChannelChip;
    }

    /**
     * Set the pin number of the panel's current channel
     *
     * @param currObsChannelPin current channel's pin number
     */
    public void setCurrObsChannelPin(int currObsChannelPin)
    {
        this.currObsChannelPin = currObsChannelPin;
    }

    public void setCurrRefChannelChip(int currRefChannelChip)
    {
        this.currRefChannelChip = currRefChannelChip;
    }

    public void setCurrRefChannelPin(int currRefChannelPin)
    {
        this.currRefChannelPin = currRefChannelPin;
    }

    public int getCurrObsChannelChip()
    {
        return currObsChannelChip;
    }

    public int getCurrObsChannelPin()
    {
        return currObsChannelPin;
    }

    public int getCurrRefChannelChip()
    {
        return currRefChannelChip;
    }

    public int getCurrRefChannelPin()
    {
        return currRefChannelPin;
    }

    public void setReferenceC1Loaded(boolean b)
    {
        referenceC1Loaded = b;

        dataLabel.setVisible(true);
    }

    public void setC1FileName(String s)
    {
        c1FileName = s;
    }

    public boolean isReferenceC1Loaded()
    {
        return referenceC1Loaded;
    }

    public C1Analyzer resetAnalyzer()
    {
        c1Analyzer = new C1Analyzer();

        return c1Analyzer;
    }

    private C1Analyzer.VarianceMode getVarianceMode()
    {
        C1Analyzer.VarianceMode varianceMode = C1Analyzer.VarianceMode.ALL_STATES;

        String selectedItem = (String) varianceModeComboBox.getSelectedItem();

        if(selectedItem != null)
        {
            switch(selectedItem)
            {
                case "Draw All States":
                    varianceMode = C1Analyzer.VarianceMode.ALL_STATES;
                    break;
                case "Draw High States Only":
                    varianceMode = C1Analyzer.VarianceMode.HIGH_STATES;
                    break;
                case "Draw Low States Only":
                    varianceMode = C1Analyzer.VarianceMode.LOW_STATES;
                    break;
            }
        }

        return varianceMode;
    }

    public void setGeneratedEventChannels(Vector<C1Channel> channels)
    {
        generatedEventChannels = channels;
    }

    private void selectCorrespondingObservedEvent(MouseEvent e, boolean selectGroup)
    {
        selectCorrespondingEvent(e, true, selectGroup);
    }

    private void selectCorrespondingReferenceEvent(MouseEvent e, boolean selectGroup)
    {
        selectCorrespondingEvent(e, false, selectGroup);
    }

    private void selectCorrespondingEvent(MouseEvent e, boolean observedData, boolean selectGroup)
    {
        // TODO: Consider rewriting this.
        //  In retrospect, the that this function and selectObservedEvent in C1DataCollector were written was not ideal.
        //  The initial idea here was that we would find the dimensions of the event we were looking for, and send it
        //  to C1DataCollector class to figure out the event that it lined up with best. This ends up giving it a bit of
        //  a messy implementation. It might have been best to just pass the MouseEvent to C1DataCollector and have it
        //  decide if that was within the bounds of an event. On the other hand, this would mean having to send info to
        //  it like base and top height. I don't think C1DataCollector had references to the graphPanes when this was
        //  written, but now that it is does... it could easily handle doing the time <-> pixel conversions.
        //  Essentially, there is no longer any advantage to splitting up the responsibilities of selecting an event
        //  in this way.

        C1ViewerGraphPane graphPane;
        if(observedData)
            graphPane = observedGraphPane;
        else
            graphPane = referenceGraphPane;

        int mouseX = e.getX();
        int mouseY = e.getY();

        // TODO: Extract into separate private method
        int base = (int) (graphPane.getSize().height * 0.25);
        int top = (int) (graphPane.getSize().height * 0.75);

        if((mouseY < base || mouseY > top) && !IsShiftPressed.isShiftPressed())
        {
            c1DataCollector.clearSelectedEvents();
            return;
        }

        Vector<Line> lines = graphPane.getStates();
        if(lines == null)
            return;

        boolean selectionOccurred = false;
        for(Line l : lines)
        {
            if(l.getY0() == l.getY1() && l.getY0() == base)
            {
                // Line is horizontal and state is 1
                if(mouseX >= l.getX0() && mouseX <= l.getX1())
                {
                    // Mouse click is between the beginning and end of the high state
                    selectionOccurred = true;

                    boolean useStartTime = lines.indexOf(l) != lines.size() - 1;
                    // Initially this was == 0 (index 0, the start), but it turns out that the lines fetched from the
                    // graphPane are in reverse order, so we have to look to see if it's the last element instead
                    // This horizontal line is likely chopped off since it's the first one being rendered

                    if(observedData)
                        c1DataCollector.selectObservedEvent(l.getX0(), l.getX1(), currObsChannelChip, currObsChannelPin, useStartTime, selectGroup, false);
                    else
                        c1DataCollector.selectReferenceEvent(l.getX0(), l.getX1(), currRefChannelChip, currRefChannelPin, useStartTime, selectGroup, false);

                    calculateStateLines();
                }
            }
        }

        if(!selectionOccurred && !IsShiftPressed.isShiftPressed())
        {
            c1DataCollector.clearSelectedEvents();
        }
    }

    private void boxSelectCorrespondingObservedEvent(MouseEvent e, int x, int y, int width, int height)
    {
        boxSelectCorrespondingEvents(x, y, width, height, true);
    }

    private void boxSelectCorrespondingReferenceEvent(MouseEvent e, int x, int y, int width, int height)
    {
        boxSelectCorrespondingEvents(x, y, width, height, false);
    }

    private void boxSelectCorrespondingEvents(int x, int y, int width, int height, boolean observedData)
    {
        C1ViewerGraphPane graphPane;
        if(observedData)
            graphPane = observedGraphPane;
        else
            graphPane = referenceGraphPane;

        int base = (int) (graphPane.getSize().height * 0.25);
        int top = (int) (graphPane.getSize().height * 0.75);

        if(y > base || y + height < top)
            return;

        Vector<Line> lines = graphPane.getStates();
        if(lines == null)
            return;

        for(Line l : lines)
        {
            if(l.getY0() == l.getY1() && l.getY0() == base)
            {
                // Line is horizontal and state is 1
                if(x <= l.getX0() && x + width >= l.getX1())
                {
                    // Selection box is around the beginning and end of the high state

                    boolean useStartTime = lines.indexOf(l) != lines.size() - 1;
                    // Initially this was == 0 (index 0, the start), but it turns out that the lines fetched from the
                    // graphPane are in reverse order, so we have to look to see if it's the last element instead
                    // This horizontal line is likely chopped off since it's the first one being rendered

                    if(observedData)
                        c1DataCollector.selectObservedEvent(l.getX0(), l.getX1(), currObsChannelChip, currObsChannelPin, useStartTime, false, true);
                    else
                        c1DataCollector.selectReferenceEvent(l.getX0(), l.getX1(), currRefChannelChip, currRefChannelPin, useStartTime, false, true);

                    calculateStateLines();
                }
            }
        }
    }

    public boolean isCurrentReferenceMode(ReferenceMode mode)
    {
        return String.valueOf(referenceDataModeComboBox.getSelectedItem()).equals(mode.toString());
    }
}
