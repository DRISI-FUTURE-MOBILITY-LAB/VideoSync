/*
 * File: EventWindowTagsComboBoxRenderer.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used for custom combo box rendering
 *          for event window tags
 */

package VideoSync.views.modals.event_logger.event_window;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EventWindowTagsComboBoxRenderer extends DefaultListCellRenderer
{

    /**
     * List of event descriptions
     */
    List<String> tooltips;

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(-1 < index && null != value && null != tooltips)
        {
            list.setToolTipText(tooltips.get(index));
        }
        return component;
    }

    public void setTooltips(List<String> tooltips)
    {
        this.tooltips = tooltips;
    }
}
