/*
 * File: HotkeysPanel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Panel that holds a list of hotkey panels for the user
 *          to configure hotkeys for updating channel car counts
 */

package VideoSync.views.modals.event_logger.modals;

import VideoSync.models.DataModel;
import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;
import VideoSync.views.modals.event_logger.commands.CommandApplyHotkeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;

public class HotkeysPanel extends JPanel implements Observer
{

    /**
     * Constant index value for selected channel index
     */
    private final int CHANNEL_INDEX = 0;

    /**
     * Constant index value to determine if hotkey is enabled or disabled
     */
    private final int ENABLED_INDEX = 1;

    /**
     * Constant value for total number of hotkey panels to be generated
     */
    private final int HOTKEY_COMBOBOX_COUNT = 10;

    /**
     * Data model proxy reference
     */
    private DataModelProxy dataModelProxy;

    /**
     * List of hotkey panels
     */
    private final List<HotkeyPanel> hotkeysPanels = new Vector<>();

    /**
     * Checkbox that allows user to enable/disable the automatic incrementation/decrementation
     * of a channel's car count when the associated hotkey is pressed
     */
    private final JCheckBox updateCheckBox;

    /**
     * Button to confirm the configuration of hotkeys
     */
    private final JButton apply;

    private final Preferences prefs;

    public HotkeysPanel(DataModelProxy dmp)
    {
        setLayout(new GridLayout(HOTKEY_COMBOBOX_COUNT + 2, 1));

        dataModelProxy = dmp;

        prefs = Preferences.userRoot().node(dataModelProxy.getEventLogUUID().toString());

        for(int i = 0; i < HOTKEY_COMBOBOX_COUNT; i++)
        {
            HotkeyPanel hotkeyPanel = new HotkeyPanel(dataModelProxy, i);
            hotkeysPanels.add(hotkeyPanel);
            add(hotkeyPanel);
        }

        JPanel updateModePanel = new JPanel();
        updateCheckBox = new JCheckBox();
        updateCheckBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED)
            {
                prefs.put("updateCheckBox", Boolean.toString(updateCheckBox.isSelected()));
            }
        });

        JLabel updateLabel = new JLabel("Increment/Decrement Channel Car Count on Hotkey Press");
        updateLabel.setFont(new Font(updateLabel.getFont().toString(), Font.PLAIN, updateLabel.getFont().getSize()));
        updateModePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        updateModePanel.add(updateLabel);
        updateModePanel.add(updateCheckBox);
        add(updateModePanel);

        JPanel applyPanel = new JPanel();
        applyPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        apply = new JButton();
        applyPanel.add(apply);
        add(applyPanel);
    }

    /**
     * Initializes the hotkey panel combo box lists
     *
     * @param channels list of channel names
     */
    public void initializeChannelLists(List<String> channels)
    {
        for(HotkeyPanel hotkeyPanel : hotkeysPanels)
        {
            hotkeyPanel.setChannelList(channels);
        }
    }

    /**
     * Initializes a command object to bind the application of hotkey configurations
     * to the apply button
     *
     * @param dataModel data model reference
     */
    public void initializeCommandList(DataModel dataModel)
    {
        CommandApplyHotkeys commandApplyHotkeys = new CommandApplyHotkeys(dataModel);
        commandApplyHotkeys.setTargets(hotkeysPanels, updateCheckBox);
        apply.setAction(commandApplyHotkeys);
        apply.setText("Apply Changes");
    }

    /**
     * Initializes hotkeymaps and their selected channel indices and enabled state
     *
     * @param hotkeyMaps map of configured hotkeys
     */
    public void initializeHotkeyMaps(Map<Integer, List<Object>> hotkeyMaps)
    {
        if(hotkeyMaps != null)
        {
            for(Integer indexKey : hotkeyMaps.keySet())
            {
                List<Object> hotKeyPanelAttributes = hotkeyMaps.get(indexKey);
                hotkeysPanels.get(indexKey).setHotkeyEnabled((Boolean) hotKeyPanelAttributes.get(ENABLED_INDEX));
                hotkeysPanels.get(indexKey).setSelectedIndex((Integer) hotKeyPanelAttributes.get(CHANNEL_INDEX));
            }
        }
    }

    /**
     * Initializes the update channel car count upon hotkey select checkbox
     *
     * @param isSelected boolean flag to enable/disable the feature
     */
    public void initializeUpdateCheckBoxMode(boolean isSelected)
    {
        boolean enabled = Boolean.parseBoolean(prefs.get("updateCheckBox", "false"));
        if(enabled)
        {
            updateCheckBox.setSelected(true);
        }
        else
            updateCheckBox.setSelected(isSelected);
    }

    @Override
    public void update(Observable o, Object arg)
    {
        if(arg.equals("Input"))
        {
            List<String> channelNames = new Vector<>();
            for(DeviceInputMap dim : dataModelProxy.getC1InputMap())
            {
                channelNames.add(dim.getChannelName());
            }

            initializeChannelLists(channelNames);
        }
    }
}
