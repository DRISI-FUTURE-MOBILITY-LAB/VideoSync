/*
 * File: EventLogger.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Event Logger frame that holds the logger and tracked
 *          metrics panel. Main component that serves as a plate
 *          to hold all the event logger UI elements.
 */

package VideoSync.views.modals.event_logger;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.commands.*;
import VideoSync.views.modals.event_logger.logger_panel.LoggerPanel;
import VideoSync.views.modals.event_logger.metrics_panel.TrackedMetricsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

public class EventLogger extends JFrame implements Observer
{
    /**
     * Reference to data model
     */
    private DataModel dataModel;

    /**
     * Window width constant
     */
    private static final int DEFAULT_WINDOW_WIDTH = 1250;

    /**
     * Window height constant
     */
    private static final int DEFAULT_WINDOW_HEIGHT = 800;

    /**
     * Logger panel UI component
     */
    private LoggerPanel loggerPanel;

    /**
     * Tracked metrics panel UI component
     */
    private TrackedMetricsPanel trackedMetricsPanel;

    /**
     * Menu bar for Event Logger
     */
    private EventLoggerMenuBar eventLoggerMenuBar;

    public EventLogger(DataModel dataModel)
    {
        this.dataModel = dataModel;

        //JFrame attribute configuration
        setTitle("Event Logger v1.1.0");
        setSize(new Dimension(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT));
        setMinimumSize(new Dimension(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT / 2));
        setLayout(new BorderLayout());

        //Instantiation of core Event Logger UI elements
        eventLoggerMenuBar = new EventLoggerMenuBar();
        loggerPanel = new LoggerPanel(dataModel, getWidth(), getHeight());
        trackedMetricsPanel = new TrackedMetricsPanel(getWidth(), getHeight());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Logger", loggerPanel);
        tabbedPane.add("Metrics", trackedMetricsPanel);

        initializeCommandList();

        add(eventLoggerMenuBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public void update(Observable o, Object arg)
    {
        if(arg.equals("Reset"))
        {
            if(loggerPanel != null && trackedMetricsPanel != null)
            {
                loggerPanel.resetPanel();
                trackedMetricsPanel.resetPanel();
            }
        }
        else if(arg.equals("Input") || arg.equals("New Event Log") || arg.equals("Open Event Log") ||
                arg.equals("Add Event") || arg.equals("Add Tag") || arg.equals("Remove Tag") || arg.equals("Remove Event") ||
                arg.equals("Update Count") || arg.equals("Add Metric") || arg.equals("Remove Metric") ||
                arg.equals("Omitted"))
        {
            if(arg.equals("Input") || arg.equals("New Event Log") || arg.equals("Add Event") || arg.equals("Add Tag") ||
                    arg.equals("Remove Tag") || arg.equals("Remove Event") || arg.equals("Update Count") ||
                    arg.equals("Omitted"))
            {
                dataModel.setUnsavedChanges(true);
            }

            if(loggerPanel != null && trackedMetricsPanel != null)
            {
                loggerPanel.updateChannelAndEventTables(dataModel.returnProxy());
                trackedMetricsPanel.updatePanel(dataModel);
            }

            if(arg.equals("Input") || arg.equals("New Event Log") || arg.equals("Open Event Log") ||
                    arg.equals("Add Tag") || arg.equals("Remove Tag") || arg.equals("Add Metric") || arg.equals("Remove Metric"))
            {
                if(loggerPanel != null)
                {
                    loggerPanel.updateFilters(dataModel.getC1InputMap(), dataModel.getEventTags());
                }
            }

            if(arg.equals("New Event Log") || arg.equals("Open Event Log"))
            {
                if(dataModel.getEventLogFile() != null)
                {
                    loggerPanel.updatePanelUIStates(true);
                    trackedMetricsPanel.updatePanelUIStates(true);
                }
            }
        }
    }

    /**
     * Initializes all the command elements associated with Event Logger
     */
    public void initializeCommandList()
    {
        //Event Logger Menu Bar Commands
        eventLoggerMenuBar.setFileMenuNewCommand(new CommandNewLog(dataModel, this));
        eventLoggerMenuBar.setFileMenuOpenCommand(new CommandOpenLog(dataModel, this));

        CommandSaveLogAs commandSaveLogAs = new CommandSaveLogAs(dataModel, this);
        eventLoggerMenuBar.setFileMenuSaveAsCommand(commandSaveLogAs);
        eventLoggerMenuBar.setFileMenuSaveCommand(new CommandSaveLog(dataModel, this, commandSaveLogAs));

        eventLoggerMenuBar.setSettingsMenuHotkeysCommand(new CommandHotkeys(dataModel, this));
        eventLoggerMenuBar.setSettingsMenuTagsCommand(new CommandTags(dataModel, this));
        eventLoggerMenuBar.setSettingsMenuMetricsCommand(new CommandOpenMetrics(dataModel, this));
        eventLoggerMenuBar.setToolMenuGenerateCSVCommand(new CommandGenerateCSV(dataModel, this));
        eventLoggerMenuBar.setToolMenuAutoAnalysis(new CommandOpenAutoAnalysis(dataModel, this));

        //Logger Panel Commands
        loggerPanel.setCommandUpdateChannelCount(new CommandUpdateChannelCount(dataModel));
        loggerPanel.setCommandJumpToEvent(new CommandJumpToEvent(dataModel, this));
        loggerPanel.setCommandNewEvent(new CommandNewEvent(dataModel, this));
        loggerPanel.setCommandRemoveEvent(new CommandRemoveEvent(dataModel));
        loggerPanel.setCommandEditEvent(new CommandEditEvent(dataModel, this));
        loggerPanel.setCommandOmitEvent(new CommandOmitEvent(dataModel));
    }

    public String getSelectedChannel()
    {
        return loggerPanel.getSelectedChannel();
    }
}
