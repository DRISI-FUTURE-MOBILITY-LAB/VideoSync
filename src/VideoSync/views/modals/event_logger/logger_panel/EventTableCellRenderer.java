/*
 * File: EventTableCellRenderer.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used for a custom table cell component
 *          to render event proxies in a JTable cell
 */

package VideoSync.views.modals.event_logger.logger_panel;

import VideoSync.objects.event_logger.EventProxy;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class EventTableCellRenderer implements TableCellRenderer
{

    /**
     * Event table cell component associated to a cell
     */
    EventTableCellComponent eventTableCellComponent;

    public EventTableCellRenderer()
    {
        eventTableCellComponent = new EventTableCellComponent();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        EventProxy eventProxy = (EventProxy) value;
        eventTableCellComponent.updateData(eventProxy, isSelected, table);
        return eventTableCellComponent;
    }
}
