/*
 * File: CommandOpenLog.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to open an
 *          event log database file (.db)
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.metrics.Metric;
import VideoSync.views.modals.event_logger.EventLogger;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.prefs.Preferences;

public class CommandOpenLog extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to LoggerPanel component for use in positioning
     */
    private final EventLogger eventLogger;

    public CommandOpenLog(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            eventLogger.setEnabled(false);

            Platform.runLater(() -> {

                /*
                 * Open a system file chooser
                 */
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(new File(dataModel.getCurrentDirectory()));
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database File", "*.db"));
                fileChooser.setTitle("Open Log File");
                File eventLogFile = fileChooser.showOpenDialog(null);

                if(eventLogFile != null && eventLogFile.getName().substring(eventLogFile.getName().lastIndexOf('.')).equals(".db"))
                {
                    boolean versionCheck = dataModel.checkEventLogDBVersion(eventLogFile);

                    /*
                     * Validate if selected database file is actually an event log data base file by checking the tables
                     */
                    boolean valid = dataModel.verifyEventLogDatabaseTables(eventLogFile);

                    if(valid && versionCheck)
                    {
                        boolean cancel_operation = false;

                        /*
                         * If an event log file is already loaded, prompt the user if they wish to save the loaded
                         * event log, before loading another one
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
                         * ...otherwise, open an event log
                         */
                        if(!cancel_operation)
                        {

                            /*
                             * Open the event log file
                             */
                            dataModel.openEventLog(eventLogFile);

                            Preferences prefs = Preferences.userRoot().node(dataModel.getEventLogUUID().toString());

                            Map<Integer, List<Object>> hotkeyMaps = new HashMap<>();
                            for(int i = 0; i < 10; i++)
                            {
                                List<Object> hotkeyPanelAttributes = new Vector<>();
                                hotkeyPanelAttributes.add(Integer.parseInt(prefs.get("comboBox" + i, "0")));
                                hotkeyPanelAttributes.add(Boolean.parseBoolean(prefs.get("numpadCheckBox" + i, "false")));
                                hotkeyMaps.put(i, hotkeyPanelAttributes);
                            }

                            if(!hotkeyMaps.isEmpty())
                                dataModel.setHotkeyMaps(hotkeyMaps);
                            dataModel.setHotkeyUpdateMode(Boolean.parseBoolean(prefs.get("updateCheckBox", "false")));

                            /*
                             * Acquire the event tags that were saved in the loaded event log file
                             */
                            List<String> loadedEventTags = new Vector<>(dataModel.getEventTags().keySet());

                            if(loadedEventTags.size() > 0)
                            {

                                /*
                                 * Check to see if there are event tags that are associated to a metric template.
                                 * If the loaded event tags contains all the tags to track a metric template, then
                                 * prompt to the user if they wish to track that metric.
                                 */
                                Map<String, Metric> metricTemplates = new HashMap<>(dataModel.getTemplateMetrics());
                                // FIXME: List is updated but never used???
                                List<String> metricsToAdd = new Vector<>();
                                for(String metricTemplate : metricTemplates.keySet())
                                {
                                    boolean addMetric = true;

                                    /*
                                     * Cycle through the list of metric templates and check if the event tags associated
                                     * to a metric template exist in the event tags map loaded from the event database file.
                                     * If a loaded database event log file contains all the event tags for a metric template,
                                     * add it.
                                     */
                                    for(String variable : metricTemplates.get(metricTemplate).getVariables().keySet())
                                    {
                                        if(!loadedEventTags.contains(variable))
                                        {
                                            addMetric = false;
                                            break;
                                        }
                                    }
                                    if(addMetric)
                                    {
                                        metricsToAdd.add(metricTemplate);
                                    }
                                }

                                /*
                                 * Display a prompt to the user if they wish to add the metric templates
                                 */
                                if(metricTemplates.size() > 0)
                                {
                                    EventQueue.invokeLater(() -> {
                                        for(String metricTemplate : metricTemplates.keySet())
                                        {
                                            int result = JOptionPane.showConfirmDialog(eventLogger, "Track " + metricTemplate + "?", "Track Metric", JOptionPane.YES_NO_OPTION);

                                            /*
                                             * If the user wishes to track the metric template, add it to the collection of tracked metrics
                                             */
                                            if(result == JOptionPane.YES_OPTION)
                                            {
                                                dataModel.addMetric(metricTemplate, metricTemplates.get(metricTemplate));
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                    else
                    {
                        String message;
                        if(!versionCheck && valid)
                            message = "Event log database update failed.";
                        else
                            message = "Invalid event log database file.";

                        JOptionPane.showMessageDialog(eventLogger, message, "Invalid Event Log Database File", JOptionPane.OK_OPTION);
                    }
                }

                eventLogger.setEnabled(true);
                eventLogger.requestFocus();
            });
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
