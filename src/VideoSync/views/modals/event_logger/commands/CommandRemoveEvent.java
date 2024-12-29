/*
 * File: CommandRemoveEvent.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to remove a
 *          selected event in the event table
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.EventProxy;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandRemoveEvent extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Events table UI reference
     */
    private JTable eventsTable;

    public CommandRemoveEvent(DataModel dataModel)
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
         * Delete the selected event
         */
        if(eventsTable.getSelectedRow() != -1)
        {
            dataModel.removeEvent((EventProxy) eventsTable.getValueAt(eventsTable.getSelectedRow(), eventsTable.getSelectedColumn()));
        }
    }
}
