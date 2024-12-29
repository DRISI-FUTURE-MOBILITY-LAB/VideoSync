/*
 * File: CommandOmitEvent.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to omit a
 *          selected event
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.EventProxy;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandOmitEvent extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Events table UI element reference
     */
    private JTable eventsTable;

    public CommandOmitEvent(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTargets(JTable eventsTable)
    {
        this.eventsTable = eventsTable;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        /*
         * Omit the event selected in the events table
         */
        if(eventsTable.getSelectedRow() != -1)
        {
            dataModel.omitEvent((EventProxy) eventsTable.getValueAt(eventsTable.getSelectedRow(), eventsTable.getSelectedColumn()));
        }
    }
}
