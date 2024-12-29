/*
 * ****************************************************************
 * File: 			MainMenuBar.java
 * Date Created:  	June 5, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To contain and control all actions that are
 * used from the Main Menu
 *
 * Modified:		August 11, 2016
 * Programmer:		Danny Hale
 * Added the to the MenuBar the ability to
 * connect to the database
 * ****************************************************************
 */
package VideoSync.views.menus;

import VideoSync.commands.menu.*;
import VideoSync.main.Constants;
import VideoSync.models.DataModelProxy;

import javax.swing.*;
import java.awt.*;

public class MainMenuBar extends JMenuBar
{
    private static final long serialVersionUID = 1L;

    // -- Main Menu Bar Variable Declarations

    /**
     * 'File' menu items
     */
    private final JMenuItem fileMenuConnect;
    private final JMenuItem fileMenuOpen;
    private final JMenuItem fileMenuQuit;
    private final JMenuItem fileMenuExport;
    private final JMenuItem fileMenuImport;

    /**
     * 'Tools' menu items
     */
    private final JMenuItem toolMenuMapping;
    private final JMenuItem toolMenuOffsetDetection;
    private final JMenuItem toolMenuEventLogger;
    private final JMenuItem toolMenuC1Viewer;
    private final JMenuItem toolMenuConvertVideo;
    private final JMenuItem toolMenuVideoEditor;
    private final JMenuItem toolMenuAverageSpeed;

    /**
     * 'Window' menu items
     */
    private final JMenuItem windowMenuFront;


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Main Menu Bar Construction

    /**
     * Creates a new menu bar containing all of the menu items needed for VideoSync
     */
    public MainMenuBar(DataModelProxy dmp)
    {
        // Create the parent 'File' menu container and add it to the MenuBar

        // Used for containing lists of actions that can be performed.
        JMenu fileMenu = new JMenu("File");
        fileMenu.setBackground(Color.LIGHT_GRAY);
        this.add(fileMenu);

        // Create the 'Open' menu item.
        // This is created even if it won't be used by the user so that we don't have to store the data model
        // proxy after constructor returns.
        fileMenuOpen = new JMenuItem("Open Directory");

        // Create the 'Save' menu item.
        fileMenuExport = new JMenuItem("Save Directory As");

        //If this program was started from another utility, then that utility will provide VideoSync with data.
        //Opening a data set will just replace the given data, so only show open menu if running standalone.
        if(dmp.getStandaloneInstance())
        {
            // Add 'Open' menu item to the 'File' menu
            fileMenu.add(fileMenuOpen);

            // Add 'Save' menu item to the 'File' menu
            fileMenu.add(fileMenuExport);

            // Add a separator to the 'File' menu
            fileMenu.add(new JSeparator());
        }

        fileMenuImport = new JMenuItem("Import File");
        fileMenu.add(fileMenuImport);

        fileMenuConnect = new JMenuItem("Connect...");
        fileMenu.add(fileMenuConnect);

        fileMenu.add(new JSeparator());

        fileMenuQuit = new JMenuItem("Quit");
        fileMenu.add(fileMenuQuit);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setBackground(Color.LIGHT_GRAY);
        add(toolsMenu);

        toolMenuMapping = new JMenuItem("Input Mapping");
        toolsMenu.add(toolMenuMapping);

        toolMenuOffsetDetection = new JMenuItem("Offset Detection");
        toolsMenu.add(toolMenuOffsetDetection);

        toolMenuEventLogger = new JMenuItem("Event Logger");
        toolsMenu.add(toolMenuEventLogger);

        toolMenuConvertVideo = new JMenuItem("Convert DVR Videos");
        toolsMenu.add(toolMenuConvertVideo);

        toolMenuVideoEditor = new JMenuItem("Edit MP4 Video");
        toolsMenu.add(toolMenuVideoEditor);

        toolMenuAverageSpeed = new JMenuItem("Average Speed");
        toolsMenu.add(toolMenuAverageSpeed);

        if(Constants.DEBUG_MODE)
        {
            toolMenuC1Viewer = new JMenuItem("C1 Viewer");
            toolsMenu.add(toolMenuC1Viewer);
        }

        JMenu windowMenu = new JMenu("Window");

        windowMenuFront = new JMenuItem("Bring All To Front");
        windowMenu.add(windowMenuFront);
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Main Menu Bar Action Commands

    /**
     * Sets the Open Action Command to the 'Open' menu item.
     * @param co CommandOpen object
     */
    public void setOpenActionCommand(CommandOpen co)
    {
        fileMenuOpen.setAction(co);
        fileMenuOpen.setText("Open Directory");
    }

    /**
     * Sets the Export Action Command to the 'Save' menu item.
     * @param ce CommandExport object
     */
    public void setExportActionCommand(CommandExport ce)
    {
        fileMenuExport.setAction(ce);
        fileMenuExport.setText("Export Directory");
    }

    /**
     * Sets the Import Action Command to the 'Import' menu item.
     * @param ci CommandImport object
     */
    public void setImportActionCommand(CommandImport ci)
    {
        fileMenuImport.setAction(ci);
        fileMenuImport.setText("Import...");
    }

    /**
     * Sets the Connect Action Command to the 'Connect...' menu item.
     * @param cc CommandConnect object
     */
    public void setConnectActionCommand(CommandConnect cc)
    {
        fileMenuConnect.setAction(cc);
        fileMenuConnect.setText("Connect...");
    }

    /**
     * Sets the Quit Action Command to the 'Quit' menu item.
     * @param cq CommandQuit object
     */
    public void setQuitActionCommand(CommandQuit cq)
    {
        fileMenuQuit.setAction(cq);
        fileMenuQuit.setText("Quit");
    }

    /**
     * Sets the Input Mapping Action Command to the 'Input Mapping' menu item.
     * @param cim CommandInputMapping object
     */
    public void setInputMappingActionCommand(CommandInputMapping cim)
    {
        toolMenuMapping.setAction(cim);
        toolMenuMapping.setText("Input Mapping");
    }

    /**
     * Sets the Offset Detection Action Command to the 'Offset Detection' menu item.
     * @param cod CommandOffsetDetection object
     */
    public void setOffsetDetectionActionCommand(CommandOffsetDetection cod)
    {
        toolMenuOffsetDetection.setAction(cod);
        toolMenuOffsetDetection.setText("Offset Detection");
    }

    /**
     * Sets the Event Logger Action Command to the 'Event Logger' menu item.
     * @param cel CommandEventLogger object
     */
    public void setEventLoggerActionCommand(CommandEventLogger cel)
    {
        toolMenuEventLogger.setAction(cel);
        toolMenuEventLogger.setText("Event Logger");
    }

    /**
     * Sets the Convert Video Action Command to the 'Convert DVR Videos' menu item.
     * @param ccv CommandEventLogger object
     */
    public void setConvertVideoActionCommand(CommandConvertVideo ccv)
    {
        toolMenuConvertVideo.setAction(ccv);
        toolMenuConvertVideo.setText("Convert DVR Videos");
    }

    public void setVideoEditorCommand(CommandVideoEditor cve)
    {
        toolMenuVideoEditor.setAction(cve);
        toolMenuVideoEditor.setText("Video Editor");
    }

    public void setAverageSpeedCommand(CommandAverageSpeed cas)
    {
        toolMenuAverageSpeed.setAction(cas);
        toolMenuAverageSpeed.setText("Average Speed");
    }

    /**
     * Sets the C1 Viewer Action Command to the 'C1 Viewer' menu item.
     * @param c1v CommandC1Viewer object
     */
    public void setC1ViewerActionCommand(CommandC1Viewer c1v)
    {
        if(Constants.DEBUG_MODE)
        {
            toolMenuC1Viewer.setAction(c1v);
            toolMenuC1Viewer.setText("C1 Viewer");
        }
    }

    /**
     * Sets the Window Front Action Command to the 'Bring All To Front' menu item.
     * @param cwf CommandWindowFrontObject
     */
    public void setWindowFrontActionCommand(CommandWindowFront cwf)
    {
        windowMenuFront.setAction(cwf);
        windowMenuFront.setText("Bring All To Front");
    }
}