/*
 * File: EventLoggerMenuBar.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class for menu bar used in Event Logger window
 */

package VideoSync.views.modals.event_logger;

import VideoSync.views.modals.event_logger.commands.*;

import javax.swing.*;

public class EventLoggerMenuBar extends JMenuBar
{

    /**
     * Used for containing lists of actions that can be performed.
     */
    private final JMenu fileMenu;
    private final JMenu toolMenu;
    private final JMenu settingMenu;

    /**
     * 'File' menu items
     */
    private final JMenuItem fileMenuNew;
    private final JMenuItem fileMenuOpen;
    private final JMenuItem fileMenuSaveAs;
    private final JMenuItem fileMenuSave;

    /**
     * 'Tool' menu items
     */
    private final JMenuItem toolMenuCSV;
    private final JMenuItem autoAnalysis;   //TODO: Auto Analysis feature

    /**
     * 'Setting' menu items
     */
    private final JMenuItem settingMenuHotkeys;
    private final JMenuItem settingMenuTags;
    private final JMenuItem settingMenuMetrics;

    public EventLoggerMenuBar()
    {
        fileMenu = new JMenu("File");
        toolMenu = new JMenu("Tools");
        settingMenu = new JMenu("Settings");

        fileMenuNew = new JMenuItem();
        fileMenuOpen = new JMenuItem();
        fileMenuSave = new JMenuItem();
        fileMenuSaveAs = new JMenuItem();

        toolMenuCSV = new JMenuItem();
        autoAnalysis = new JMenuItem();

        settingMenuHotkeys = new JMenuItem();
        settingMenuTags = new JMenuItem();
        settingMenuMetrics = new JMenuItem();

        fileMenu.add(fileMenuNew);
        fileMenu.add(fileMenuOpen);
        fileMenu.add(fileMenuSave);
        fileMenu.add(fileMenuSaveAs);

        toolMenu.add(autoAnalysis);
        toolMenu.add(toolMenuCSV);

        settingMenu.add(settingMenuHotkeys);
        settingMenu.add(settingMenuTags);
        settingMenu.add(settingMenuMetrics);

        add(fileMenu);
        add(toolMenu);
        add(settingMenu);
    }

    /**
     * Sets the new event log file command
     *
     * @param commandNewLog command to create new event log files
     */
    public void setFileMenuNewCommand(CommandNewLog commandNewLog)
    {
        fileMenuNew.setAction(commandNewLog);
        fileMenuNew.setText("New Log");
    }

    /**
     * Sets the open log file command
     *
     * @param commandOpenLog command to open existing event log files
     */
    public void setFileMenuOpenCommand(CommandOpenLog commandOpenLog)
    {
        fileMenuOpen.setAction(commandOpenLog);
        fileMenuOpen.setText("Open Log");
    }

    /**
     * Sets the save event log as file command
     *
     * @param commandSaveLogAs command to save event log files as
     */
    public void setFileMenuSaveAsCommand(CommandSaveLogAs commandSaveLogAs)
    {
        fileMenuSaveAs.setAction(commandSaveLogAs);
        fileMenuSaveAs.setText("Save Log As");
    }

    /**
     * Sets the save event log file command
     *
     * @param commandSaveLog command to save event log files
     */
    public void setFileMenuSaveCommand(CommandSaveLog commandSaveLog)
    {
        fileMenuSave.setAction(commandSaveLog);
        fileMenuSave.setText("Save Log");
    }

    /**
     * Sets the hotkey configuration window command
     *
     * @param commandHotkeys command to open hotkey configurations window
     */
    public void setSettingsMenuHotkeysCommand(CommandHotkeys commandHotkeys)
    {
        settingMenuHotkeys.setAction(commandHotkeys);
        settingMenuHotkeys.setText("Configure Hotkeys");
    }

    /**
     * Sets the tag configuration window command
     *
     * @param commandTags command to open tag configurations window
     */
    public void setSettingsMenuTagsCommand(CommandTags commandTags)
    {
        settingMenuTags.setAction(commandTags);
        settingMenuTags.setText("Configure Tags");
    }

    /**
     * Sets the metrics configuration window command
     *
     * @param commandOpenMetrics command to open metric configurations window
     */
    public void setSettingsMenuMetricsCommand(CommandOpenMetrics commandOpenMetrics)
    {
        settingMenuMetrics.setAction(commandOpenMetrics);
        settingMenuMetrics.setText("Configure Metrics");
    }

    /**
     * Sets the CSV generation command
     *
     * @param commandGenerateCSV command to generate CSV files for event log reporting
     */
    public void setToolMenuGenerateCSVCommand(CommandGenerateCSV commandGenerateCSV)
    {
        toolMenuCSV.setAction(commandGenerateCSV);
        toolMenuCSV.setText("Generate CSV");
    }

    public void setToolMenuAutoAnalysis(CommandOpenAutoAnalysis commandOpenAutoAnalysis)
    {
        autoAnalysis.setAction(commandOpenAutoAnalysis);
        autoAnalysis.setText("Auto Analysis");
    }
}
