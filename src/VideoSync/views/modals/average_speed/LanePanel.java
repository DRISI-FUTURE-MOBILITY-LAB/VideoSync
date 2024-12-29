package VideoSync.views.modals.average_speed;

import VideoSync.analyzers.C1Analyzer;
import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.graphs.Line;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import static VideoSync.objects.EDeviceType.DEVICE_C1;

public class LanePanel extends JPanel implements ActionListener {
    private DataModelProxy dmp;
    private int lane;
    private JLabel laneNumber;
    private JComboBox upperChannelCombo;
    private JComboBox lowerChannelCombo;
    private JLabel averageSpeed;
    private C1Analyzer c1Analyzer;
    private final String[] defaultUpperModel = new String[]{"Choose an upper channel"};
    private final String[] defaultLowerModel = new String[]{"Choose a lower channel"};
    //TODO: make dynamic
    private int start = 0;
    private int end = 1000 * 60 * 60; // initialize as 1 hour

    LanePanel(DataModelProxy dataModelProxy, int lane) {
        dmp = dataModelProxy;
        this.lane = lane;
        c1Analyzer = new C1Analyzer();

        averageSpeed = new JLabel(laneFormat(lane));
        add(averageSpeed);

        upperChannelCombo = new JComboBox();
        lowerChannelCombo = new JComboBox();
        updateComboBoxes();
        add(upperChannelCombo);
        add(lowerChannelCombo);

        averageSpeed = new JLabel(mphFormat(0d));
        add(averageSpeed);
        upperChannelCombo.addActionListener(this);
        lowerChannelCombo.addActionListener(this);
    }

    public void updateComboBoxes() {
        updateComboBoxOptions(upperChannelCombo, defaultUpperModel);
        updateComboBoxOptions(lowerChannelCombo, defaultLowerModel);
    }

    private void updateComboBoxOptions(JComboBox comboBox,
                                       String[] defaultSelection)
    {
        Object data = dmp.getInputMapForDevice(DEVICE_C1);
        ArrayList<String> strings = new ArrayList<>();
        if(data instanceof Vector)
        {
            int selectedIndex = comboBox.getSelectedIndex();
            int dataSize = ((Vector<DeviceInputMap>)data).size();
            strings.add(defaultSelection[0]);
            for(int i = 1; i <= dataSize; i++)
            {
                DeviceInputMap dim =
                        ((Vector<DeviceInputMap>) data).elementAt(i - 1);
                if(dim.getLaneNumber() == lane) {
                    strings.add(dim.getChannelName());
                }
            }

            // Set the combo box data to the string array we just generated
            comboBox.setModel(new DefaultComboBoxModel(strings.toArray()));
            //Used to re-select the channel if the input mapping has changed
            if(selectedIndex < strings.size() && selectedIndex!=-1)
            {
                comboBox.setSelectedIndex(selectedIndex);
            }
            else
            {    //else we probably changed devices and need reset the selection.
                comboBox.setSelectedIndex(0);
            }
        }
        else if(data == null)
        {
            comboBox.setModel(new DefaultComboBoxModel(defaultSelection));
        }
    }

    public void updateAverageSpeed() {
        if (upperChannelCombo.getSelectedItem() != defaultUpperModel[0] &&
            lowerChannelCombo.getSelectedItem() != defaultLowerModel[0]) {
            averageSpeed.setText(mphFormat(getAverageMph()));
        } else {
            averageSpeed.setText(mphFormat(0d));
        }
    }

    private String mphFormat(Double mph) {
        return "Average speed: " + String.valueOf(mph.intValue())
                + "mph";
    }

    private String laneFormat(int lane) {
        return "Lane: " + String.valueOf(lane);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==upperChannelCombo || e.getSource()==lowerChannelCombo) {
            updateComboBoxes();
            updateAverageSpeed();
        }
    }

    private int getChip(JComboBox combo) {
        String item = (String)combo.getSelectedItem();
        return dmp.getChannelChipNumberFromName(DEVICE_C1, item);
    }

    private int getPin(JComboBox combo) {
        String item = (String)combo.getSelectedItem();
        return dmp.getChannelPinNumberFromName(DEVICE_C1, item);
    }

    // Returns an array of all low to high transitions in ms
    public Double getAverageMph() {
        ArrayList<Double> carSpeeds = new ArrayList<>();
        ArrayList<Long> upperEvents = dmp.getHighStates(
                getChip(upperChannelCombo),
                getPin(upperChannelCombo));
        ArrayList<Long> lowerEvents = dmp.getHighStates(
                getChip(lowerChannelCombo),
                getPin(lowerChannelCombo));
        for (long upperEvent : upperEvents) {
            if (upperEvent < start || upperEvent > end) continue;
            Long milliseconds = binarySearch(lowerEvents, upperEvent, 0,
                    lowerEvents.size())-upperEvent;
            Double carSpeed = 20d/milliseconds.doubleValue() * 681.818;
            if (milliseconds != 0 && isSpeedReasonable(carSpeed)) {
                carSpeeds.add(carSpeed);
            }
        }
        double sum = 0;
        for (int i=0; i< carSpeeds.size(); i++) {
            sum += carSpeeds.get(i);
        }
        if (carSpeeds.size() != 0) {
            return sum / carSpeeds.size();
        } else return 0d;

    }

    private static long binarySearch(ArrayList<Long> arr, long key, int first, int last)
    {
        int position = Math.abs(Collections.binarySearch(arr, key)+1);
        try {
            return arr.get(position);
        } catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }

    private Boolean isSpeedReasonable(Double speed) {
        //TODO: needs tuning
        if (speed > 15d && speed < 100d) {
            return true;
        } else return false;
    }

    public void setStartTime(int start) {
        this.start = start;
    }

    public void setEndTime(int end) {
        this.end = end;
    }
}
