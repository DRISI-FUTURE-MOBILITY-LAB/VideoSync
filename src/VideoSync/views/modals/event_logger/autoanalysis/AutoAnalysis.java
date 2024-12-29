package VideoSync.views.modals.event_logger.autoanalysis;

import VideoSync.analyzers.C1Analyzer;
import VideoSync.models.DataModel;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.EDeviceType;
import VideoSync.objects.c1.C1Channel;
import VideoSync.views.modals.event_logger.commands.CommandLoadGeneratedC1;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.*;

public class AutoAnalysis extends JFrame implements ActionListener, Observer
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

    private DataModel dm;

    private C1Analyzer c1Analyzer;
    private boolean generatedC1Loaded;

    /**
     * Used to keep track of which device type is used for each combo box index.
     */
    private Vector<EDeviceType> deviceTypes = new Vector<>();

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

    private Vector<C1Channel> observedEventChannels;
    private Vector<C1Channel> generatedEventChannels;

    private JPanel mainJPanel;
    private JCheckBox useGeneratedDataCheckBox;
    private JButton loadGeneratedData;
    private JComboBox<String> observedChannelComboBox;
    private JComboBox<String> referenceChannelComboBox;
    private JButton analyzeButton;
    private JCheckBox additionalDetectionCheckBox;
    private JProgressBar analyzeProgressBar;
    private JLabel loadedDataFileName;
    private JComboBox<String> observedDeviceComboBox;
    private JComboBox<String> referenceDeviceComboBox;
    private JTextField startTimeTextField;
    private JTextField endTimeTextField;
    private JCheckBox limitSearchTimeCheckBox;

    private CommandLoadGeneratedC1 commandLoadGeneratedC1;

    private AutoAnalysisWorker autoAnalysisWorker;

    public AutoAnalysis(DataModel dataModel)
    {
        setTitle("Auto Analysis");
        setContentPane(mainJPanel);

        dm = dataModel;

        generatedC1Loaded = false;
    }

    public void initPanel()
    {
        observedChannelComboBox.addActionListener(this);
        observedDeviceComboBox.addActionListener(this);
        referenceChannelComboBox.addActionListener(this);
        referenceDeviceComboBox.addActionListener(this);

        observedChannelComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Choose a channel"}));
        observedDeviceComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Select a Device", "C1"}));
        referenceChannelComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Choose a channel"}));
        referenceDeviceComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"Selected a Device", "C1"}));

        useGeneratedDataCheckBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED)
                toggleGeneratedDataState(true);
            else if(e.getStateChange() == ItemEvent.DESELECTED)
                toggleGeneratedDataState(false);
        });

        loadedDataFileName.setText("No generated data loaded yet");

        c1Analyzer = new C1Analyzer();

        commandLoadGeneratedC1 = new CommandLoadGeneratedC1("Load Generated Data", c1Analyzer, this, dm);
        loadGeneratedData.setAction(commandLoadGeneratedC1);
        loadGeneratedData.setEnabled(false);

        limitSearchTimeCheckBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED)
                toggleSearchTimeState(true);
            else if(e.getStateChange() == ItemEvent.DESELECTED)
                toggleSearchTimeState(false);
        });

        startTimeTextField.setEnabled(false);
        endTimeTextField.setEnabled(false);

        startTimeTextField.setText("00:00:00.000");
        endTimeTextField.setText(convertToTimeFormat(dm.getMaxVideoLength()));

        analyzeButton.addActionListener(e -> startAnalysis(additionalDetectionCheckBox.isSelected()));
    }

    private void startAnalysis(boolean partialDetection)
    {
        long startTime = 0;
        long endTime = dm.getMaxVideoLength();

        if(limitSearchTimeCheckBox.isSelected())
        {
            if(!startTimeTextField.getText().equals(""))
            {
                try
                {
                    startTime = convertToMilliseconds(startTimeTextField.getText());
                }
                catch(InvalidTimeFormat e)
                {
                    JOptionPane.showMessageDialog(
                            this,
                            "Please enter a valid time in the HH:MM:SS or HH:MM:SS.sss format.",
                            "Invalid Time Entered",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }
            }

            if(!endTimeTextField.getText().equals(""))
            {
                try
                {
                    endTime = convertToMilliseconds(endTimeTextField.getText());
                }
                catch(InvalidTimeFormat e)
                {
                    JOptionPane.showMessageDialog(
                            this,
                            "Please enter a valid time in the HH:MM:SS or HH:MM:SS.sss format.",
                            "Invalid Time Entered",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }
            }
        }

        if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
        {
            if(!generatedC1Loaded)
            {
                JOptionPane.showMessageDialog(this, "You must load a generated data .c1 file before attempting to run analysis with \"Use Generated Data\" enabled.", "Invalid choice", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if(observedDeviceComboBox.getSelectedIndex() == 0 || observedChannelComboBox.getSelectedIndex() == 0)
            {
                JOptionPane.showMessageDialog(this, "Valid observed device and channel must be selected.", "Invalid choice", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean hasC1Channel = false;
            for(C1Channel channel : c1Analyzer.getC1Channels())
            {
                if(channel.getChip() == currObsChannelChip && channel.getPin() == currObsChannelPin)
                {
                    hasC1Channel = true;
                    break;
                }
            }

            if(!hasC1Channel)
            {
                JOptionPane.showMessageDialog(this, "Selected channel does not exist in generated data.", "Invalid choice", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        else
        {
            if(observedDeviceComboBox.getSelectedIndex() == 0 || observedChannelComboBox.getSelectedIndex() == 0 || referenceDeviceComboBox.getSelectedIndex() == 0 || referenceChannelComboBox.getSelectedIndex() == 0)
            {
                JOptionPane.showMessageDialog(this, "Valid device and channel must be selected.", "Invalid choice", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        C1Channel observedChannel = getCorrespondingChannel(getObservedEventChannels(), currObsChannelChip, currObsChannelPin);
        C1Channel referenceChannel;
        if(isCurrentReferenceMode(ReferenceMode.GENERATED_DATA))
        {
            referenceChannel = getCorrespondingChannel(getReferenceEventChannels(), currObsChannelChip, currObsChannelPin);
        }
        else
        {
            referenceChannel = getCorrespondingChannel(getReferenceEventChannels(), currRefChannelChip, currRefChannelPin);
        }

        autoAnalysisWorker = new AutoAnalysisWorker(this, dm, partialDetection, observedChannel, referenceChannel, startTime, endTime);
        autoAnalysisWorker.addPropertyChangeListener(evt -> {
            if("progress".equals(evt.getPropertyName()))
            {
                updateProgressBar((Integer) evt.getNewValue());
            }
            else if("state".equals(evt.getPropertyName()))
            {
                analysisStateUpdate((SwingWorker.StateValue) evt.getNewValue());
            }
        });

        analyzeProgressBar.setValue(0);

        autoAnalysisWorker.execute();
    }

    private void updateProgressBar(int progress)
    {
        analyzeProgressBar.setValue(progress);
    }

    private void analysisStateUpdate(SwingWorker.StateValue state)
    {
        if(state == SwingWorker.StateValue.DONE)
        {
            addRequiredTags();

            HashMap<Long, String> results = autoAnalysisWorker.getResults();

            Object[] options = {"Yes", "No"};
            int n = JOptionPane.showOptionDialog(this,
                    "Auto analysis discovered " + results.size() + " potential events.\nWould you like to save these events to event logger?",
                    "Auto Analysis Finished",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]);

            if(n == 0)
            {
                String falsePositive = "Sensor detected a vehicle when there wasn't one. EVENT GENERATED BY AUTO ANALYSIS.";
                String falseNegative = "Sensor failed to detect a vehicle that was present. EVENT GENERATED BY AUTO ANALYSIS.";
                String partialDetection = "Sensor failed to detect a vehicle for the majority of the vehicle's presence. EVENT GENERATED BY AUTO ANALYSIS.";

                for(HashMap.Entry<Long, String> entry : results.entrySet())
                {
                    long time = entry.getKey();
                    String tag = entry.getValue();
                    String description = "";
                    switch(tag)
                    {
                        case "False Positive":
                            description = falsePositive;
                            break;
                        case "False Negative":
                            description = falseNegative;
                            break;
                        case "Partial Detection":
                            description = partialDetection;
                            break;
                    }

                    dm.addEvent(currObsChannelChip, currObsChannelPin, (int) time, dm.getGraphOffset(), tag, description, false);
                }
            }
        }
    }

    private void addRequiredTags()
    {
        Map<String, String> tags = dm.getEventTags();
        if(!tags.containsKey("False Positive"))
        {
            dm.addTag("False Positive", "Sensor claims to have detected vehicle in detection region; however no vehicle is present.");
        }

        if(!tags.containsKey("False Negative"))
        {
            dm.addTag("False Negative", "Sensor fails to detect vehicle present in detection region.");
        }

        if(!tags.containsKey("Partial Detection"))
        {
            dm.addTag("Partial Detection", "Sensor failed to detect vehicle for the majority of its presence. Potential dropped call.");
        }
    }

    private void toggleGeneratedDataState(boolean state)
    {
        loadGeneratedData.setEnabled(state);
        referenceDeviceComboBox.setEnabled(!state);
        referenceChannelComboBox.setEnabled(!state);
    }

    private void toggleSearchTimeState(boolean state)
    {
        startTimeTextField.setEnabled(state);
        endTimeTextField.setEnabled(state);
    }

    public boolean isGeneratedC1Loaded()
    {
        return generatedC1Loaded;
    }

    public void setGeneratedC1Loaded(boolean b)
    {
        generatedC1Loaded = b;
    }

    public C1Analyzer resetAnalyzer()
    {
        c1Analyzer = new C1Analyzer();

        return c1Analyzer;
    }

    public void setC1FileName(String name)
    {
        loadedDataFileName.setText(name);
    }

    public void setGeneratedEventChannels(Vector<C1Channel> channels)
    {
        generatedEventChannels = channels;
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        // If the source of the ActionEvent is the devices combo box, retrieve the list of
        // available channels for that device.
        if(ae.getSource() == observedDeviceComboBox || ae.getSource() == referenceDeviceComboBox)
        {
            // Create a temporary combo box element from the ActionEvent source.
            // FIXME: Make sure cast is safe
            JComboBox<String> combo = (JComboBox<String>) ae.getSource();

            // Get the device of the selected item from the combo box.
            int comboIndex = combo.getSelectedIndex();
            if(comboIndex < deviceTypes.size())
            {
                // Set the combo box text from the information returned from the DataModelProxy
                setComboBoxText((combo.getSelectedIndex() == 0) ? null : this.dm.getC1InputMap());
            }
            else
            {
                System.out.println("Requested device index " + comboIndex + " is out of bounds for loaded devices");
            }
        }

        // If the source of the ActionEvent is the channels combo box, get the channel that was
        // selected and have the graph present that channel data
        if(ae.getSource() == observedChannelComboBox || ae.getSource() == referenceChannelComboBox)
        {
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
                if(ae.getSource() == observedChannelComboBox)
                {
                    setCurrObsChannelChip(this.dm.getChannelChipNumberFromName(deviceTypes.elementAt(observedDeviceComboBox.getSelectedIndex()), item));
                    setCurrObsChannelPin(this.dm.getChannelPinNumberFromName(deviceTypes.elementAt(observedDeviceComboBox.getSelectedIndex()), item));
                }
                else if(ae.getSource() == referenceChannelComboBox)
                {
                    setCurrRefChannelChip(this.dm.getChannelChipNumberFromName(deviceTypes.elementAt(referenceDeviceComboBox.getSelectedIndex()), item));
                    setCurrRefChannelPin(this.dm.getChannelPinNumberFromName(deviceTypes.elementAt(referenceDeviceComboBox.getSelectedIndex()), item));
                }
            }
        }
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
            observedDeviceComboBox.setModel(new DefaultComboBoxModel<>((String[]) data));
            referenceDeviceComboBox.setModel(new DefaultComboBoxModel<>((String[]) data));
        }
        else if(data instanceof Vector)
        {
            int selectedIndex = observedChannelComboBox.getSelectedIndex();
            int referenceDataSelectedIndex = referenceChannelComboBox.getSelectedIndex();

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
            observedChannelComboBox.setModel(new DefaultComboBoxModel<>(strings));
            referenceChannelComboBox.setModel(new DefaultComboBoxModel<>(strings));

            //Used to re-select the channel if the input mapping has changed
            if(selectedIndex < strings.length)
            {
                observedChannelComboBox.setSelectedIndex(selectedIndex);
                referenceChannelComboBox.setSelectedIndex(referenceDataSelectedIndex);
            }
            else
            {    //else we probably changed devices and need reset the selection.
                observedChannelComboBox.setSelectedIndex(0);
                referenceChannelComboBox.setSelectedIndex(0);
            }
        }
        else if(data == null)
        {
            observedChannelComboBox.setModel(new DefaultComboBoxModel<>(defaultChannelModel));
            referenceChannelComboBox.setModel(new DefaultComboBoxModel<>(defaultChannelModel));
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

    @Override
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

            observedEventChannels = dm.getC1AnalyzerChannels();

            // We need to reset any loaded generated data
            setGeneratedC1Loaded(false);
            setC1FileName("");
            setGeneratedEventChannels(null);
            commandLoadGeneratedC1.setTargets(resetAnalyzer(), this, dm);

            // Reset chip and pin
            setCurrObsChannelChip(0);
            setCurrObsChannelPin(0);
            setCurrRefChannelChip(0);
            setCurrRefChannelPin(0);

            startTimeTextField.setText("00:00:00.000");
            endTimeTextField.setText(convertToTimeFormat(dm.getMaxVideoLength()));
        }

        if(obj instanceof String)
        {
            if(observedDeviceComboBox.getSelectedIndex() != 0)
            {
                if(obj.equals("Input"))
                {
                    setComboBoxText(this.dm.getC1InputMap());

                    startTimeTextField.setText("00:00:00.000");
                    endTimeTextField.setText(convertToTimeFormat(dm.getMaxVideoLength()));
                }

                if(obj.equals("Reset"))
                {
                    resetPanel();
                }
            }
        }
    }

    /**
     * Resets the graph panel when the DataModel notifies the panel that major changes took place and everything needs to reset.
     */
    private void resetPanel()
    {
        this.observedDeviceComboBox.setSelectedIndex(0);
        this.observedChannelComboBox.setSelectedIndex(0);

        this.referenceDeviceComboBox.setSelectedIndex(0);
        this.referenceChannelComboBox.setSelectedIndex(0);

        analyzeProgressBar.setValue(0);
        generatedC1Loaded = false;
        loadedDataFileName.setText("No generated data loaded yet");
        loadGeneratedData.setEnabled(false);
        useGeneratedDataCheckBox.setSelected(false);
        referenceChannelComboBox.setEnabled(false);
        referenceDeviceComboBox.setEnabled(false);

        startTimeTextField.setText("00:00:00.000");
        endTimeTextField.setText(convertToTimeFormat(dm.getMaxVideoLength()));

        setVisible(false);
    }

    @SuppressWarnings("unchecked")
    public Vector<C1Channel> getObservedEventChannels()
    {
        return (Vector<C1Channel>) observedEventChannels.clone();
    }

    @SuppressWarnings("unchecked")
    public Vector<C1Channel> getReferenceEventChannels()
    {
        if(isCurrentReferenceMode(ReferenceMode.SELF_REFERENCE))
            return (Vector<C1Channel>) observedEventChannels.clone();
        else
            return (Vector<C1Channel>) generatedEventChannels.clone();
    }

    public boolean isCurrentReferenceMode(ReferenceMode mode)
    {
        if(mode == ReferenceMode.GENERATED_DATA)
            return useGeneratedDataCheckBox.isSelected();
        else
            return !useGeneratedDataCheckBox.isSelected();
    }

    public C1Channel getCorrespondingChannel(Vector<C1Channel> channels, int chip, int pin)
    {
        Optional<C1Channel> optional = channels.stream().filter(x -> x.getChip() == chip && x.getPin() == pin).findFirst();

        return optional.orElse(null);
    }

    /**
     * Convert the millisecond time value to HH:MM:SS.millis that is more human readable
     *
     * @param msTime Time in milliseconds
     * @return String representing time in HH:MM:SS.millis format
     */
    private String convertToTimeFormat(long msTime)
    {
        // Get the number of millis in the time
        int millis = (int) (msTime - ((msTime / 1000) * 1000));

        // Get the number of seconds in the time
        int seconds = (int) (msTime / 1000);

        // As long as seconds is greater than 59, subtract 60 from it
        while(seconds > 59)
        {
            seconds -= 60;
        }

        // Get the number of minutes from the millisecond time
        int minutes = (int) (msTime / 1000) / 60;

        // Get the total number of hours from the value of minutes
        int hours = minutes / 60;

        // Return the formatted string
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    /**
     * Converts the timestamp to milliseconds
     *
     * @param timestamp timestamp from event window timestamp label
     * @return timestamp converted in milliseconds
     */
    private int convertToMilliseconds(String timestamp) throws InvalidTimeFormat
    {
        int result = 0;
        StringTokenizer tokenizer = new StringTokenizer(timestamp, ":");

        if(tokenizer.countTokens() == 3)
        {
            result += Integer.parseInt(tokenizer.nextToken()) * 36000000;
            result += Integer.parseInt(tokenizer.nextToken()) * 60000;

            StringTokenizer secondsTokenizer = new StringTokenizer(tokenizer.nextToken(), ".");
            if(secondsTokenizer.countTokens() == 2)
            {
                result += Integer.parseInt(secondsTokenizer.nextToken()) * 1000;

                String decimals = secondsTokenizer.nextToken();
                if(decimals.length() == 3)
                {
                    result += Integer.parseInt(decimals);
                }
                else
                {
                    throw new InvalidTimeFormat("Time format must be HH:MM:SS or HH:MM:SS.sss");
                }
            }
            else if(secondsTokenizer.countTokens() == 1)
            {
                result += Integer.parseInt(secondsTokenizer.nextToken()) * 1000;
            }
            else
            {
                throw new InvalidTimeFormat("Time format must be HH:MM:SS or HH:MM:SS.sss");
            }
        }
        else
        {
            throw new InvalidTimeFormat("Time format must be HH:MM:SS or HH:MM:SS.sss");
        }

        System.out.println(result);

        return result;
    }

    private class InvalidTimeFormat extends Exception
    {
        public InvalidTimeFormat()
        {

        }

        public InvalidTimeFormat(String message)
        {
            super(message);
        }

        public InvalidTimeFormat(Throwable cause)
        {
            super(cause);
        }

        public InvalidTimeFormat(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
