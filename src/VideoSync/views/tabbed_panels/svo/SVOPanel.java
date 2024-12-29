package VideoSync.views.tabbed_panels.svo;

import javax.swing.*;
import java.awt.*;

// TODO: What is this for? Can we get rid of it?
// Seems to stand for "Speed, Volume, Occupancy"
@SuppressWarnings({"rawtypes", "unchecked"})
public class SVOPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private final JTextField textField_StartTime;
    private final JTextField textField_EndTime;

    private final JComboBox combo_TimeRange;
    private final JButton button_Calculate;

    private final JCheckBox checkbox_Speed;
    private final JCheckBox checkbox_Volume;
    private final JCheckBox checkbox_Occupancy;
    private final JButton button_Export;

    private final JButton button_CalculateAll;

    private final JComboBox combo_SVO1;
    private final JComboBox combo_SVO2;
    private final JComboBox combo_SVO3;
    private final JComboBox combo_SVO4;
    private final JComboBox combo_SVO5;

    private final JComboBox combo_Channel1;
    private final JComboBox combo_Channel2;
    private final JComboBox combo_Channel3;
    private final JComboBox combo_Channel4;
    private final JComboBox combo_Channel5;

    private final JTable table_Events;

    public SVOPanel()
    {
        setSize(new Dimension(724, 532));
        setLayout(null);

        JPanel panel = new JPanel();
        panel.setBounds(6, 34, 712, 86);
        add(panel);
        panel.setLayout(null);

        JLabel lblStartTime = new JLabel("Start Time:");
        lblStartTime.setBounds(6, 18, 73, 16);
        panel.add(lblStartTime);

        JLabel lblEndTime = new JLabel("End Time:");
        lblEndTime.setBounds(6, 55, 73, 16);
        panel.add(lblEndTime);

        textField_StartTime = new JTextField();
        textField_StartTime.setText("Start Time");
        textField_StartTime.setBounds(75, 12, 94, 28);
        panel.add(textField_StartTime);
        textField_StartTime.setColumns(10);

        textField_EndTime = new JTextField();
        textField_EndTime.setText("End Time");
        textField_EndTime.setColumns(10);
        textField_EndTime.setBounds(75, 48, 94, 28);
        panel.add(textField_EndTime);

        combo_TimeRange = new JComboBox();
        combo_TimeRange.setModel(new DefaultComboBoxModel(new String[]{"5 Seconds", "10 Seconds", "30 Seconds", "1 Minute", "2 Minutes", "5 Minutes", "10 Minutes", "30 Minutes", "1 Hour", "2 Hours"}));
        combo_TimeRange.setBounds(181, 12, 119, 27);
        panel.add(combo_TimeRange);

        button_Calculate = new JButton("Calculate");
        button_Calculate.setBounds(180, 48, 121, 29);
        panel.add(button_Calculate);

        button_CalculateAll = new JButton("Calculate All");
        button_CalculateAll.setBounds(589, 13, 117, 29);
        panel.add(button_CalculateAll);

        JLabel calculatesDataFromBeginning = new JLabel("<html><em><center>Calculates data<br>from beginning to end</center></em></html>");
        calculatesDataFromBeginning.setHorizontalAlignment(SwingConstants.CENTER);
        calculatesDataFromBeginning.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
        calculatesDataFromBeginning.setBounds(589, 36, 117, 48);
        panel.add(calculatesDataFromBeginning);

        checkbox_Speed = new JCheckBox("Speed");
        checkbox_Speed.setBounds(336, 4, 73, 23);
        panel.add(checkbox_Speed);

        checkbox_Volume = new JCheckBox("Volume");
        checkbox_Volume.setBounds(336, 30, 79, 23);
        panel.add(checkbox_Volume);

        checkbox_Occupancy = new JCheckBox("Occupancy");
        checkbox_Occupancy.setBounds(336, 56, 101, 23);
        panel.add(checkbox_Occupancy);

        button_Export = new JButton("Export");
        button_Export.setBounds(445, 27, 117, 29);
        panel.add(button_Export);

        JLabel lblSpeedVolumeOccupancy = new JLabel("Speed, Volume, Occupancy");
        lblSpeedVolumeOccupancy.setHorizontalAlignment(SwingConstants.CENTER);
        lblSpeedVolumeOccupancy.setBounds(273, 9, 180, 16);
        add(lblSpeedVolumeOccupancy);

        JPanel panel_1 = new JPanel();
        panel_1.setBounds(6, 120, 712, 63);
        add(panel_1);
        panel_1.setLayout(null);

        JLabel label_Static2 = new JLabel("Time");
        label_Static2.setBounds(6, 41, 76, 16);
        panel_1.add(label_Static2);

        combo_Channel1 = new JComboBox();
        combo_Channel1.setBounds(108, 37, 110, 27);
        panel_1.add(combo_Channel1);

        combo_Channel2 = new JComboBox();
        combo_Channel2.setBounds(230, 37, 110, 27);
        panel_1.add(combo_Channel2);

        combo_Channel3 = new JComboBox();
        combo_Channel3.setBounds(352, 37, 110, 27);
        panel_1.add(combo_Channel3);

        combo_Channel4 = new JComboBox();
        combo_Channel4.setBounds(474, 37, 110, 27);
        panel_1.add(combo_Channel4);

        combo_Channel5 = new JComboBox();
        combo_Channel5.setBounds(596, 37, 110, 27);
        panel_1.add(combo_Channel5);

        combo_SVO1 = new JComboBox();
        combo_SVO1.setModel(new DefaultComboBoxModel(new String[]{"", "Speed", "Volume", "Occupancy"}));
        combo_SVO1.setBounds(108, 6, 110, 27);
        panel_1.add(combo_SVO1);

        combo_SVO2 = new JComboBox();
        combo_SVO2.setModel(new DefaultComboBoxModel(new String[]{"", "Speed", "Volume", "Occupancy"}));
        combo_SVO2.setBounds(230, 6, 110, 27);
        panel_1.add(combo_SVO2);

        combo_SVO3 = new JComboBox();
        combo_SVO3.setModel(new DefaultComboBoxModel(new String[]{"", "Speed", "Volume", "Occupancy"}));
        combo_SVO3.setBounds(352, 6, 110, 27);
        panel_1.add(combo_SVO3);

        combo_SVO4 = new JComboBox();
        combo_SVO4.setModel(new DefaultComboBoxModel(new String[]{"", "Speed", "Volume", "Occupancy"}));
        combo_SVO4.setBounds(474, 6, 110, 27);
        panel_1.add(combo_SVO4);

        combo_SVO5 = new JComboBox();
        combo_SVO5.setModel(new DefaultComboBoxModel(new String[]{"", "Speed", "Volume", "Occupancy"}));
        combo_SVO5.setBounds(596, 6, 110, 27);
        panel_1.add(combo_SVO5);

        JScrollPane scrollPane_Table = new JScrollPane();
        scrollPane_Table.setBounds(6, 183, 712, 343);
        add(scrollPane_Table);

        table_Events = new JTable();
        scrollPane_Table.setViewportView(table_Events);
    }


    public JComboBox getCombo_Channel3()
    {
        return combo_Channel3;
    }

    public JTextField getTextField_StartTime()
    {
        return textField_StartTime;
    }

    public JComboBox getCombo_SVO1()
    {
        return combo_SVO1;
    }

    public JComboBox getCombo_SVO2()
    {
        return combo_SVO2;
    }

    public JComboBox getCombo_Channel2()
    {
        return combo_Channel2;
    }

    public JButton getButton_CalculateAll()
    {
        return button_CalculateAll;
    }

    public JCheckBox getCheckbox_Occupancy()
    {
        return checkbox_Occupancy;
    }

    public JComboBox getCombo_SVO3()
    {
        return combo_SVO3;
    }

    public JButton getButton_Calculate()
    {
        return button_Calculate;
    }

    public JComboBox getCombo_Channel1()
    {
        return combo_Channel1;
    }

    public JComboBox getCombo_Channel4()
    {
        return combo_Channel4;
    }

    public JComboBox getCombo_SVO4()
    {
        return combo_SVO4;
    }

    public JTextField getTextField_EndTime()
    {
        return textField_EndTime;
    }

    public JButton getButton_Export()
    {
        return button_Export;
    }

    public JCheckBox getCheckbox_Volume()
    {
        return checkbox_Volume;
    }

    public JCheckBox getCheckbox_Speed()
    {
        return checkbox_Speed;
    }

    public JComboBox getCombo_Channel5()
    {
        return combo_Channel5;
    }

    public JComboBox getCombo_TimeRange()
    {
        return combo_TimeRange;
    }

    public JTable getTable_Events()
    {
        return table_Events;
    }
}
