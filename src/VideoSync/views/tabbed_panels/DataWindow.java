/*
 * ****************************************************************
 * File: 			DataWindow.java
 * Date Created:  	June 28, 2013
 * Programmer:		Dale Reed
 * 
 * Purpose:			To contain and display all of the view containers
 * 					pertaining to graphs, and statistics. 
 * 
 * Modified:		August 11, 2016
 * Programmer:		Danny Hale
 * 					Added CommandConnect class to the setCommands()
 * 					function.
 *
 * Modified			August 2020
 * Programmer:		Jenzel Arevalo
 * 					Refactored GraphPane to contain the graph
 * 					panels and allow for graphs to be added at
 * 					run time.
 * ****************************************************************
 */
package VideoSync.views.tabbed_panels;

import VideoSync.commands.CommandList;
import VideoSync.controllers.EventDetectionPane;
import VideoSync.controllers.PlaybackPane;
import VideoSync.main.Constants;
import VideoSync.models.DataModel;
import VideoSync.views.menus.MainMenuBar;
import VideoSync.views.tabbed_panels.graphs.GraphOptions;
import VideoSync.views.tabbed_panels.graphs.GraphPane;
import VideoSync.views.tabbed_panels.graphs.GraphPanel;
import VideoSync.views.videos.VideoPane;
import VideoSync.views.videos.VideoPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

//import VideoSync.views.tabbed_panels.svo.SVOPanel;

public class DataWindow extends JFrame implements WindowListener
{
    private static final long serialVersionUID = 1L;

    private VideoPane video_pane;

    // -- Data Window Variable Declarations

    /**
     * Used for containing all functions that are not readily available from the keyboard & views
     */
    private MainMenuBar mainMenuBar;

    /**
     * Used for giving the user options with the graph panes.
     */
    private GraphOptions panelOptions;

    /**
     * Used for containing all of the graphs in a common area
     */
    private GraphPane graphsPane;

    /**
     * Used to contain the panelPlayback & panelEvent panels
     */
    private JPanel bottomPanel;

    /**
     * Used to display the Caltrans logo for legal
     */
    private LogoPane panelLogo;

    /**
     * Used for containing all of the playback controls
     */
    private PlaybackPane panelPlayback;

    /**
     * Used for containing the event detection methods
     */
    private EventDetectionPane panelEvents;

    /**
     * Used for notifying the DataModel of global application events
     * -- Application Shutdown
     *
     * Use the actual DataModel instead of the DataModelProxy since the user interface needs to access
     * functions not available through the proxy.
     */
    private final DataModel dm;

    private static final int WINDOW_WIDTH = 883;
    private static final int WINDOW_HEIGHT = 810;
    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Window Construction

    /**
     * Creates the DataWindow with an initial reference to the DataModel so that it may be initialized.
     * @param dm DataModel reference
     */
    public DataWindow(DataModel dm)
    {
        // Set the title of the JFrame
        this.setTitle("VideoSync © 2022 Caltrans®");

        // Set the size of the JFrame
        this.setSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        // Set the DataModel as defined from the constructor
        this.dm = dm;

        // Adds the components to the JFrame
        addComponents();

        // Finalize the JFrame so it will display
        setResizable(false);
        setVisible(true);

        // Have the JFrame listen to window events
        this.addWindowListener(this);

        //System.out.println("Panel Options Size: " + this.panelOptions.getSize().toString());
        dm.setDataWindow(this);

        //Prevent base JFrame class from closing window if user presses no on exit prompt
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setLocationRelativeTo(null);

        //Have DataModel tell all observers to update so window components can have proper state.
        dm.notifyObservers();
    }

    /**
     * Adds all of the components to the JFrame so they will be presented upon startup.
     */
    private void addComponents()
    {
        // Create and set the menu bar for the view
        mainMenuBar = new MainMenuBar(dm.returnProxy());
        this.setJMenuBar(mainMenuBar);

        // Create a tabbed pane so we can use one view to display multiple views
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        // Add the tabbed pane to the center of the JFrame's content pane.
        this.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Create the graphs pane with 9 rows and 1 column.
        //The extra row is for the Graph Options at the top of the Graphs display.
        graphsPane = new GraphPane(dm);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(graphsPane);

        // Create the Graph Options Panel and have it observe the Data Model
        panelOptions = new GraphOptions(dm.returnProxy());
        dm.addObserver(panelOptions);

        JPanel graphPane = new JPanel();
        graphPane.setLayout(new BorderLayout());
        graphPane.add(panelOptions, BorderLayout.NORTH);
        graphPane.add(scrollPane, BorderLayout.CENTER);

        // Add the Graphs Pane to the tabbed panel.
        tabbedPane.addTab("Graphs", null, graphPane, "Traffic Events Tab");

        // Add video offsets pane to the tabbed panel.
        video_pane = new VideoPane();
        tabbedPane.addTab("Video", null, video_pane, "Video offsets");

        // Add the SVO Panel to the tabbed panel.
        //SVOPanel panelSVO = new SVOPanel();
        //tabbedPane.addTab("SVO", null, panelSVO, "Speed, Volume, Occupancy");

        // Create the bottom panel
        bottomPanel = new JPanel();

        // Set the size of the bottom panel
        bottomPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, 138));

        // Add the bottom panel to the 'South' portion of the JFrame's content pane.
        this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setLayout(null);

        // Create the playback panel and add it to the bottom panel.
        panelPlayback = new PlaybackPane();
        panelPlayback.setBounds(0, 0, 435, 138);
        bottomPanel.add(panelPlayback);

        // Create the events panel and add it to the bottom panel.
        panelEvents = new EventDetectionPane();
        panelEvents.setBounds(435, 0, 320, 138);
        bottomPanel.add(panelEvents);

        JPanel logoAndVersion = new JPanel();
        logoAndVersion.setLayout(new BoxLayout(logoAndVersion, BoxLayout.Y_AXIS));
        logoAndVersion.setAlignmentX(RIGHT_ALIGNMENT);
        logoAndVersion.setBounds(755, 0, WINDOW_WIDTH - panelPlayback.getWidth() - panelEvents.getWidth(), 138);

        panelLogo = new LogoPane();
        panelLogo.setBounds(0, 0, WINDOW_WIDTH - panelPlayback.getWidth() - panelEvents.getWidth(), 128);
        logoAndVersion.add(panelLogo);

        JLabel version = new JLabel("Version " + Constants.VS_VERSION);
        logoAndVersion.add(version);
        bottomPanel.add(logoAndVersion);

        // Have the playback & event panels observe the data model.
        dm.addObserver(panelPlayback);
        dm.addObserver(panelEvents);
        dm.addObserver(graphsPane);
    }

    /**
     * Sets all of the action commands for the menu items.
     * @param cl object containing list of commands
     */
    public void setCommands(CommandList cl)
    {
        mainMenuBar.setOpenActionCommand(cl.getCommandOpen());
        mainMenuBar.setExportActionCommand(cl.getCommandExport());
        mainMenuBar.setImportActionCommand(cl.getCommandImport());
        mainMenuBar.setConnectActionCommand(cl.getCommandConnect());
        mainMenuBar.setQuitActionCommand(cl.getCommandQuit());
        mainMenuBar.setInputMappingActionCommand(cl.getCommandInputMapping());
        mainMenuBar.setOffsetDetectionActionCommand(cl.getCommandOffsetDetection());
        mainMenuBar.setEventLoggerActionCommand(cl.getCommandEventLogger());
        mainMenuBar.setEventLoggerActionCommand(cl.getCommandEventLogger());
        mainMenuBar.setConvertVideoActionCommand(cl.getCommandConvertVideo());
        mainMenuBar.setVideoEditorCommand(cl.getCommandVideoEditor());
        mainMenuBar.setAverageSpeedCommand(cl.getCommandAverageSpeed());
        mainMenuBar.setC1ViewerActionCommand(cl.getCommandC1Viewer());
        mainMenuBar.setWindowFrontActionCommand(cl.getCommandWindowFront());

        panelOptions.setWidthActionCommand(cl.getCommandGraphWidth());

        panelPlayback.setPlayActionCommand(cl.getCommandPlay());
        panelPlayback.setFrameForwardActionCommand(cl.getCommandFrameForward());
        panelPlayback.setFrameBackwardActionCommand(cl.getCommandFrameReverse());

        // Set the key bindings to use some of the commands from the Command List
        setKeyBindings(cl);
    }

    /**
     * Sets all of the key bindings for the keyboard when the focus is in the playback panel.
     * @param cl object containing list of commands
     */
    private void setKeyBindings(CommandList cl)
    {
        // Create an input map using the input map from the root pane.
        InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Create two keystrokes for handling the player controls
        KeyStroke leftStroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        KeyStroke rightStroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);

        // Place the two keystrokes into the input map
        im.put(leftStroke, "frameBack");
        im.put(rightStroke, "frameForward");

        // Create an action map using the action map from the root pane.
        ActionMap amap = this.getRootPane().getActionMap();
        amap.put("frameBack", cl.getCommandFrameReverse());
        amap.put("frameForward", cl.getCommandFrameForward());

        // Have the playback panel listen to events.
        panelPlayback.requestFocus();
    }

    /**
     * Returns collection of GraphPanels
     * @return Vector of GraphPanels
     */
    public Vector<GraphPanel> getGraphPanels()
    {
        return (Vector<GraphPanel>) graphsPane.getGraphPanels();
    }


    public void addVideoPanel(VideoPlayer vp)
    {
        video_pane.addVideoPanel(vp);
    }

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Java Event Listeners

    public void windowClosing(WindowEvent e)
    {
        String[] options = {"Yes", "No"};

        // Show an option pane and get the result of their input.
        // Because JOptionPane requires a parent component to display the alert, we just create an empty JFrame so it will be displayed.
        int n = JOptionPane.showOptionDialog(this,
                "Are you sure you wish to exit VideoSync?",
                null,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        // User wants to quit.
        if(n == 0)
        {
            this.dm.performShutdownOperations();
        }
    }

    /**
     * Adds additional graph panels when a data set is loaded
     */
    public void addGraphPanels(int quantity)
    {
        graphsPane.addGraphPanels(quantity);
        revalidate();
        repaint();
    }

    /**
     * Removes additional graph panels when a data set is removed
     */
    public void removeGraphPanels(int quantity)
    {
        graphsPane.removeGraphPanels(quantity);
        revalidate();
        repaint();
    }


    /**
     * Resets graph pane
     */
    public void resetGraphsPane()
    {
        graphsPane.resetDefaultGraphPanels();
        revalidate();
        repaint();
    }

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Java Event Listeners
    // -- NOTE: None of the following are currently implemented in this version.

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
}
