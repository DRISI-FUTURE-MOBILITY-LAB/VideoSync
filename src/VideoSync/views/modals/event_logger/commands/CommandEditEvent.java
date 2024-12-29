/*
 * File: CommandEditEvent.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to edit an existing event
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.EventProxy;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.event_window.EventWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandEditEvent extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to LoggerPanel component for use in positioning
     */
    private final EventLogger eventLogger;

    /**
     * Events table UI element reference
     */
    private JTable eventsTable;

    public CommandEditEvent(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    public void setTargets(JTable eventsTable)
    {
        this.eventsTable = eventsTable;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        if(eventsTable.getSelectedRowCount() == 1)
        {
            dataModel.setPlaying(false);

            EventWindow eventWindow = new EventWindow("Edit Event");
            eventWindow.setDataModelProxy(dataModel.returnProxy());
            eventWindow.initializeUIElements();
            eventWindow.setSelectedAttributes((EventProxy) eventsTable.getValueAt(eventsTable.getSelectedRow(), eventsTable.getSelectedColumn()));

            CommandUpdateEvent commandUpdateEvent = new CommandUpdateEvent(dataModel);
            commandUpdateEvent.setTarget(eventWindow);
            eventWindow.setCommandUpdateEvent(commandUpdateEvent);

            eventWindow.setLocationRelativeTo(eventLogger);
            eventWindow.setVisible(true);
        }
    }
}
