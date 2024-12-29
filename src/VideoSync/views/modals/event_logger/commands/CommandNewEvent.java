/*
 * File: CommandNewEvent.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to set video
 *          player to an event's recorded time stamp
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.event_window.EventWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandNewEvent extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to LoggerPanel component for use in positioning
     */
    private final EventLogger eventLogger;

    public CommandNewEvent(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        /*
         * Pause the video before opening an event window instance
         */
        dataModel.setPlaying(false);

        /*
         * Create an instance of the new event window
         */
        EventWindow eventWindow = new EventWindow("New Event");
        eventWindow.setDataModelProxy(dataModel.returnProxy());
        eventWindow.initializeUIElements();

        /*
         * Set the 'add event' command to the event window
         */
        eventWindow.setCommandAddEvent(new CommandAddEvent(dataModel));

        eventWindow.setSelectedChannel(eventLogger.getSelectedChannel());

        /*
         * Display the event window
         */
        eventWindow.setLocationRelativeTo(eventLogger);
        eventWindow.setVisible(true);
    }
}
