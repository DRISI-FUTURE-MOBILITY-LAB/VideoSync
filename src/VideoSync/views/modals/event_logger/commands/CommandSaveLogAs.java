/*
 * File: CommandSaveLogAs.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to save a
 *          event log file.
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class CommandSaveLogAs extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to LoggerPanel component for use in positioning
     */
    private final EventLogger eventLogger;

    public CommandSaveLogAs(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            if(dataModel.getEventLogFile() != null)
            {
                eventLogger.setEnabled(false);

                Platform.runLater(() -> {

                    // Open OS system file directory
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialDirectory(new File(dataModel.getCurrentDirectory()));
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database File", "*.db"));
                    fileChooser.setTitle("Save Log File");
                    fileChooser.setInitialDirectory(new File(dataModel.getCurrentDirectory()));
                    fileChooser.setInitialFileName(dataModel.getEventLogFile().getName());

                    File file = fileChooser.showSaveDialog(null);

                    if(file != null)
                    {
                        // Save recorded events to event log database file
                        dataModel.setEventLogFile(file);
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
                });
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
