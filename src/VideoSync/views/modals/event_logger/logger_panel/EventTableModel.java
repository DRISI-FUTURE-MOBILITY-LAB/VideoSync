/*
 * File: EventTableModel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used for a custom table model for
 *          events table in events panel
 */

package VideoSync.views.modals.event_logger.logger_panel;

import VideoSync.objects.event_logger.EventProxy;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class EventTableModel extends AbstractTableModel
{

    /**
     * List of event proxies used for event table cell components, editors and renderers
     */
    List<EventProxy> events;

    public EventTableModel(List<EventProxy> events)
    {
        this.events = events;
    }

    @Override
    public int getRowCount()
    {
        return events.size();
    }

    @Override
    public int getColumnCount()
    {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return (events == null ? null : events.get(rowIndex));
    }

    public String getColumnName(int columnIndex)
    {
        return "Events";
    }

    // FIXME: What is the Class class? Look into fixing raw parameterized warning
    public Class getColumnClass(int columnIndex)
    {
        return EventProxy.class;
    }

    public boolean isCellEditable(int columnIndex, int rowIndex)
    {
        return false;
    }
}
