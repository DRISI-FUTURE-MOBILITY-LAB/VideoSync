/*
 * File: CommandSaveLog.java
 * Programmer: Aleksey Zasorin
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to save a
 *          event log file without overwriting it.
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CommandSaveLog extends AbstractAction
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
     * Reference to Save As command so that we can trigger it in the case that "Save" was clicked but the file has
     * never been saved before.
     */
    private final CommandSaveLogAs commandSaveLogAs;

    public CommandSaveLog(DataModel dataModel, EventLogger eventLogger, CommandSaveLogAs commandSaveLogAs)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
        this.commandSaveLogAs = commandSaveLogAs;
    }

    // TODO: Change to actually update the DB file instead of overwriting it
    //  Could provide performance benefit if database ever becomes big?
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            if(dataModel.getEventLogFile() != null)
            {
                if(dataModel.getLogNeverSaved())
                {
                    commandSaveLogAs.actionPerformed(e);
                    return;
                }

                eventLogger.setEnabled(false);

                if(dataModel.getEventLogFile() != null)
                {
                    // Save recorded events to event log database file
                    dataModel.setLogNeverSaved(false);
                    dataModel.writeEventLogDBFile();

                    EventQueue.invokeLater(() -> {
                        String filename = dataModel.getEventLogFile().getName();
                        String message = filename + " saved successfully.";
                        JOptionPane.showMessageDialog(eventLogger, message);
                    });
                }

                dataModel.setUnsavedChanges(false);

                eventLogger.setEnabled(true);
                eventLogger.requestFocus();
            }
            else
            {
                JOptionPane.showMessageDialog(eventLogger, "No event log file loaded in to Event Logger.", "No Event Log File Loaded to Event Logger", JOptionPane.OK_OPTION);
            }
        }
        else
        {
            JOptionPane.showMessageDialog(eventLogger, "The following files need to be loaded in to VideoSync before utilizing Event Logger:\n"
                    + "- C1 data file (.c1)\n"
                    + "- Input mapping file (.mpf)\n"
                    + "- Video file (.mp4)", "Missing Required Files", JOptionPane.OK_OPTION);
        }
    }
}
