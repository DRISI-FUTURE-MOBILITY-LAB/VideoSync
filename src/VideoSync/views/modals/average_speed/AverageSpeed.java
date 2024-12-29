package VideoSync.views.modals.average_speed;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static VideoSync.objects.EDeviceType.DEVICE_C1;

public class AverageSpeed extends JFrame implements ActionListener {
    private JPanel averageSpeedPanel;
    private JPanel overallAverage;
    private JLabel averageSpeedLabel;
    private JButton calculateAverageButton;
    private JTextField startTime;
    private JTextField endTime;
    private DataModelProxy dmp;
    private final String[] defaultChannelModel = new String[]{"Choose a Channel"};
    private ArrayList<LanePanel> lanePanels = new ArrayList<>();
    private final int DEFAULT_END_TIME = 1000 * 60 * 60;
    private int unitMultiplier = 1000*60; //milliseconds to minutes

    public AverageSpeed(DataModelProxy dataModelProxy) {
        dmp = dataModelProxy;
        setContentPane(averageSpeedPanel);
        setSize(600, 700);
        setResizable(true);
        calculateAverageButton.addActionListener(this);
        addLanePanels();
        endTime.setText(String.valueOf(DEFAULT_END_TIME));
    }

    public void addLanePanels() {
        for (int i : getLanes()) {
            LanePanel lp = new LanePanel(dmp, i);
            averageSpeedPanel.add(lp);
            lanePanels.add(lp);
        }
    }

    public Vector<Integer> getLanes()
    {
        Vector<Integer> lanes = new Vector<>();
        Object data = dmp.getInputMapForDevice(DEVICE_C1);
        if(data instanceof Vector)
        {
            int dataSize = ((Vector<DeviceInputMap>) data).size();
            String[] strings = new String[dataSize];
            for(int i = 0; i <= dataSize-1; i++)
            {
                int lane = ((Vector<DeviceInputMap>)data).elementAt(i).getLaneNumber();
                // Ensures we only use unique lanes that aren't 0 (no lane)
                if (!lanes.contains(lane) && lane!=0) {
                    lanes.add(lane);
                }
            }
        }
        Collections.sort(lanes);
        return lanes;
    }

    public void update() {
        if (lanePanels.size()==0) {
            addLanePanels();
        }
        for(LanePanel lp : lanePanels) {
            lp.updateComboBoxes();
            lp.updateAverageSpeed();
        }

    }

    public void updateTotalAverage() {
        double sum = 0;
        int lanes = 0;
        for (int i=0; i< lanePanels.size(); i++) {
            try {
                sum += lanePanels.get(i).getAverageMph();
                if(lanePanels.get(i).getAverageMph() != 0) lanes++;
            } catch (Exception e) {
                continue;
            }
        }
        if (lanes!=0) {
            averageSpeedLabel.setText(mphFormat(sum/lanes));
        } else averageSpeedLabel.setText(mphFormat(0d));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==calculateAverageButton) {
            updateTotalAverage();
            updateStartTimes();
        }
        if (e.getSource()==startTime ) {
            updateStartTimes();
        }
    }

    private String mphFormat(Double mph) {
        return "Overall average speed: " + String.valueOf(mph.intValue())
                + "mph";
    }

    private void updateStartTimes() {
        for (LanePanel lp : lanePanels) {
            try {
                lp.setStartTime(getTimeFromTextBox(startTime));
            } catch (Exception e) {
                startTime.setText("0");
            }
            try {
                lp.setEndTime(getTimeFromTextBox(endTime));
            } catch (Exception e) {
                endTime.setText(String.valueOf(DEFAULT_END_TIME));
            }
            lp.updateAverageSpeed();
        }
    }

    private void print(String s) {
        System.out.println(s);
    }

    private int getTimeFromTextBox(JTextField jtf) {
        return Integer.parseInt(jtf.getText());
    }
}
