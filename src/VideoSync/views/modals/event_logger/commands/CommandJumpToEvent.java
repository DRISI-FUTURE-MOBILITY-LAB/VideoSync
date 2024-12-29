/*
 * File: CommandJumpToEvent.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to set video
 *          player to an event's recorded time stamp
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.EventProxy;
import VideoSync.views.modals.event_logger.EventLogger;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandJumpToEvent extends AbstractAction
{
    /**
     * Data model reference
     */
    private DataModel dataModel;

    /**
     * Reference to parent window
     */
    private final EventLogger eventLogger;

    /**
     * Events table UI reference
     */
    private JTable events;

    public CommandJumpToEvent(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    public void setTarget(JTable events)
    {
        this.events = events;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        /*
         * Acquire the timestamp from the selected event, and jump to the timestamp
         * if its within the loaded video time length
         */
        EventProxy eventProxy = (EventProxy) events.getValueAt(events.getSelectedRow(), events.getSelectedColumn());

        if(eventProxy.getTimestamp() >= 0 && eventProxy.getTimestamp() <= dataModel.getMaxVideoLength())
        {
            dataModel.jumpToEventTimestamp(eventProxy.getTimestamp(), eventProxy.getOffset());
        }
        else
        {
            JOptionPane.showMessageDialog(eventLogger, "Cannot jump to event timestamp.");
        }
    }
}
