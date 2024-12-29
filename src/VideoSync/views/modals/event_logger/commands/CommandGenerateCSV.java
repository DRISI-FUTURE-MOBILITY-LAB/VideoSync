/*
 * File: CommandGenerateCSV.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to generate CSV files using
 *          OpenCSV
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
import java.io.IOException;

public class CommandGenerateCSV extends AbstractAction
{

    /**
     * Data Model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to LoggerPanel component for use in positioning
     */
    private final EventLogger eventLogger;

    public CommandGenerateCSV(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        /*
         * Inquire where the user would like to generate the CSV file
         */
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            if(dataModel.getEventLogFile() != null)
            {
                eventLogger.setEnabled(false);

                Platform.runLater(() -> {

                    FileChooser fileChooser = new FileChooser();
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma Separated Values File", "*.csv"));
                    fileChooser.setTitle("Save CSV File");
                    fileChooser.setInitialDirectory(new File(dataModel.getCurrentDirectory()));
                    fileChooser.setInitialFileName("untitled");

                    File file = fileChooser.showSaveDialog(null);

                    if(file != null)
                    {
                        try
                        {
                            dataModel.generateEventLogCSV(file);
                            EventQueue.invokeLater(() -> {
                                String filename = file.getName();
                                String message = filename + " saved successfully.";
                                JOptionPane.showMessageDialog(eventLogger, message);
                            });
                        }
                        catch(IOException e1)
                        {
                            e1.printStackTrace();
                        }
                    }

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
