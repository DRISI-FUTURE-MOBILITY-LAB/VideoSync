/*
 * File: CommandApplyHotkeys.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to apply hotkeys used for
 *          car counting
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.modals.HotkeyPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class CommandApplyHotkeys extends AbstractAction
{

    /**
     * List of hotkey UI panels
     */
    private List<HotkeyPanel> hotkeyPanels;

    /**
     * Checkbox enabling the update of car counts upon pressing a hotkey
     */
    private JCheckBox checkbox;

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    public CommandApplyHotkeys(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTargets(List<HotkeyPanel> hotkeyPanels, JCheckBox checkBox)
    {
        this.hotkeyPanels = hotkeyPanels;
        this.checkbox = checkBox;
    }

    private Map<Integer, List<Object>> getHotkeyMaps()
    {
        Map<Integer, List<Object>> hotkeyMaps = new HashMap<>();
        for(int i = 0; i < hotkeyPanels.size(); i++)
        {
            List<Object> hotkeyPanelAttributes = new Vector<>();
            hotkeyPanelAttributes.add(hotkeyPanels.get(i).getSelectedIndex());
            hotkeyPanelAttributes.add(hotkeyPanels.get(i).isHotkeyEnabled());
            hotkeyMaps.put(i, hotkeyPanelAttributes);
        }
        return hotkeyMaps;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        dataModel.setHotkeyMaps(getHotkeyMaps());
        dataModel.setHotkeyUpdateMode(checkbox.isSelected());
    }
}
