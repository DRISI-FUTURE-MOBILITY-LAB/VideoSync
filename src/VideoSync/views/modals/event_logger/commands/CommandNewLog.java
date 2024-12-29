/*
 * File: CommandNewLog.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to create a
 *          new event log file
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.NewLogFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

public class CommandNewLog extends AbstractAction implements WindowListener
{

    /**
     * Reference to data model
     */
    private final DataModel dataModel;

    /**
     * Reference to parent window
     */
    private final EventLogger eventLogger;

    /**
     * New event log file
     */
    private File newEventLog;

    /**
     * Frame window which contains panels for
     * - configuring metrics
     * - configuring tags
     * - configuring hotkeys
     */
    private NewLogFrame newLogFrame;

    public CommandNewLog(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            boolean cancel_operation = false;
            /*
             * If there is an event log file that is already loaded, prompt user if they wish
             * to save the loaded file in to VideoSync before creating a new file...
             */
            if(dataModel.getEventLogFile() != null)
            {
                int response = JOptionPane.showConfirmDialog(eventLogger, "Save opened event log file?", "Save Event Log", JOptionPane.YES_NO_CANCEL_OPTION);
                if(response == JOptionPane.YES_OPTION)
                {
                    dataModel.writeEventLogDBFile();
                }
                else if(response == JOptionPane.CANCEL_OPTION)
                {
                    cancel_operation = true;
                }
            }
            /*
             * ...otherwise, create a new event log file
             */
            if(!cancel_operation)
            {
                EventQueue.invokeLater(() -> {
                    newEventLog = new File(dataModel.getCurrentDirectory() + File.separator + "untitled.db");

                    // Originally this functionality was part of newEventLog(), but was factored out since methods
                    // called by prompAdditionalConfigurationMessage() rely on a UUID to open/access a unique
                    // Preference user node to store hotkey settings between sessions.
                    dataModel.setRandomEventLogUUID();

                    promptAdditionalConfigurationMessage();
                    dataModel.newEventLog(newEventLog);
                });
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

    /**
     * Method that prompts user if they wish to configure metrics, event tags and
     * hotkeys as soon as a new event log file is created.
     */
    public void promptAdditionalConfigurationMessage()
    {
        int response = JOptionPane.showConfirmDialog(eventLogger, "Configure event classification tags and keyboard hotkeys now?", "Configure Logging Settings", JOptionPane.YES_NO_OPTION);
        newLogFrame = new NewLogFrame(dataModel, eventLogger);
        newLogFrame.addWindowListener(this);
        if(response == JOptionPane.YES_OPTION)
        {
            newLogFrame.setLocationRelativeTo(eventLogger);
            newLogFrame.setVisible(true);
        }
        else
        {
            newLogFrame.dispose();
        }
    }

    /**
     * Information prompt to remind users of VideoSync to configure channel input
     * mapping attributes before using Event Logger.
     */
    private void displayInputMappingReminder()
    {
        JOptionPane.showMessageDialog(eventLogger, "Remember to configure detector types for " +
                "observed channels with the Input Mapping Tool.");
    }

    @Override
    public void windowOpened(WindowEvent e)
    {

    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        displayInputMappingReminder();
    }

    @Override
    public void windowClosed(WindowEvent e)
    {
        displayInputMappingReminder();
    }

    @Override
    public void windowIconified(WindowEvent e)
    {

    }

    @Override
    public void windowDeiconified(WindowEvent e)
    {

    }

    @Override
    public void windowActivated(WindowEvent e)
    {

    }

    @Override
    public void windowDeactivated(WindowEvent e)
    {

    }
}
