/*
 * File: HotkeyPanel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Panel to select a channel for car count updates
 */

package VideoSync.views.modals.event_logger.modals;

import VideoSync.models.DataModelProxy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.prefs.Preferences;

public class HotkeyPanel extends JPanel
{
    private final int panelNumber;

    /**
     * Number key label for hotkey
     */
    private final JLabel buttonLabel;

    /**
     * Combo box that lists all the channel names
     */
    private final JComboBox<String> comboBox;

    /**
     * Checkbox to enable/disable hotkey
     */
    private final JCheckBox checkBox;

    private final DataModelProxy dmp;

    private final Preferences prefs;


    public HotkeyPanel(DataModelProxy dataModelProxy, int number)
    {
        setLayout(new FlowLayout(FlowLayout.CENTER));

        dmp = dataModelProxy;

        panelNumber = number;

        buttonLabel = new JLabel("Numpad Key " + panelNumber);

        prefs = Preferences.userRoot().node(dmp.getEventLogUUID().toString());

        comboBox = new JComboBox<>();

        checkBox = new JCheckBox();

        checkBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED)
            {
                comboBox.setEnabled(checkBox.isSelected());
                prefs.put("numpadCheckBox" + panelNumber, Boolean.toString(checkBox.isSelected()));
            }
        });

        comboBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED)
            {
                prefs.put("comboBox" + panelNumber, Integer.toString(comboBox.getSelectedIndex()));
            }
        });

        add(buttonLabel);

        add(comboBox);

        add(checkBox);

        boolean enabled = Boolean.parseBoolean(prefs.get("numpadCheckBox" + panelNumber, "false"));
        checkBox.setSelected(enabled);
        comboBox.setEnabled(enabled);

        add(new JLabel("Enabled"));
    }

    /**
     * Sets the channel list for the combo box
     *
     * @param channelList list of channel names
     */
    public void setChannelList(List<String> channelList)
    {
        DefaultComboBoxModel defaultComboBoxModel = new DefaultComboBoxModel();
        // FIXME: Unchecked call
        defaultComboBoxModel.addElement("Channels");
        for(String channel : channelList)
        {
            defaultComboBoxModel.addElement(channel);
        }
        comboBox.setModel(defaultComboBoxModel);

        boolean enabled = Boolean.parseBoolean(prefs.get("numpadCheckBox" + panelNumber, "false"));

        if(enabled)
            comboBox.setSelectedItem(Integer.parseInt(prefs.get("comboBox" + panelNumber, "0")));

        revalidate();
        repaint();
    }

    /**
     * Sets the flag to enable/disable the hotkey
     *
     * @param mode boolean flag enable/disable the hotkey
     */
    public void setHotkeyEnabled(boolean mode)
    {
        boolean enabled = Boolean.parseBoolean(prefs.get("numpadCheckBox" + panelNumber, "false"));
        if(enabled)
        {
            checkBox.setSelected(true);
            comboBox.setEnabled(true);
        }
        else
        {
            checkBox.setSelected(mode);
            comboBox.setEnabled(mode);
        }
    }

    /**
     * Sets the index value of the selected channel in the combo box
     *
     * @param index value of the selected channel in the combo box
     */
    public void setSelectedIndex(int index)
    {
        boolean enabled = Boolean.parseBoolean(prefs.get("numpadCheckBox" + panelNumber, "false"));
        int selectedItem = Integer.parseInt(prefs.get("comboBox" + panelNumber, "0"));
        if(enabled && selectedItem != 0)
            comboBox.setSelectedIndex(selectedItem);
        else
            comboBox.setSelectedIndex(index);
    }

    /**
     * Returns state of hotkey
     *
     * @return boolean flag that indicates if the hotkey is enabled/disabled
     */
    public boolean isHotkeyEnabled()
    {
        return checkBox.isSelected();
    }

    /**
     * Gets the index value of the selected channel in the combo box
     *
     * @return index value of the selected channel in the combo box
     */
    public int getSelectedIndex()
    {
        return comboBox.getSelectedIndex();
    }
}
