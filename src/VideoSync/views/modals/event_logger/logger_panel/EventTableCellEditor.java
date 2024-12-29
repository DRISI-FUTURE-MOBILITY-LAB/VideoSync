/*
 * File: EventTableCellEditor.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used for a custom table cell editor
 *          to select event proxies in a JTable cell
 */

package VideoSync.views.modals.event_logger.logger_panel;

import VideoSync.objects.event_logger.EventProxy;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class EventTableCellEditor extends AbstractCellEditor implements TableCellEditor
{

    /**
     * Event table cell component associated to a cell
     */
    EventTableCellComponent eventTableCellComponent;

    public EventTableCellEditor()
    {
        eventTableCellComponent = new EventTableCellComponent();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        EventProxy eventProxy = (EventProxy) value;
        eventTableCellComponent.updateData(eventProxy, true, table);
        return eventTableCellComponent;
    }

    @Override
    public Object getCellEditorValue()
    {
        return null;
    }
}
