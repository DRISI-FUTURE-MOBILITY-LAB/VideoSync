/*
 * ****************************************************************
 * File: 			CommandList.java
 * Date Created:  	June 6, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle and manage all of the Command Elements
 * 					for use with the various actions that can be
 * 					performed.
 *
 * Modified:		August 11, 2016
 * Programmer:		Danny Hale
 * 					Added the CommandConnect class to list of file
 * 					menu options.
 * ****************************************************************
 */
package VideoSync.commands;

import VideoSync.commands.menu.*;
import VideoSync.commands.playback.CommandFrameForward;
import VideoSync.commands.playback.CommandFrameReverse;
import VideoSync.commands.playback.CommandPlay;
import VideoSync.commands.windows.graph.CommandGraphWidth;
import VideoSync.models.DataModel;
import VideoSync.views.tabbed_panels.DataWindow;

public class CommandList
{
    /**
     * Opens the contents of a directory
     */
    private final CommandOpen co;

    /**
     * Saves the project into a folder
     */
    private final CommandExport cs;

    /**
     * Imports an individual file into the project
     */
    private final CommandImport ci;

    /**
     * Starts the shutdown process for VideoSync
     */
    private final CommandQuit cq;

    /**
     * Starts the command input mapping
     */
    private final CommandInputMapping cim;

    /**
     * Starts Event Logger
     */
    private final CommandEventLogger cel;

    /** Starts Convert Video */
    private final CommandConvertVideo ccv;

    private final CommandVideoEditor cve;

    private final CommandAverageSpeed cas;

    /**
     * Starts C1 Viewer
     */
    private final CommandC1Viewer c1v;

    /**
     * Start the command to connect to the SQL Database
     */
    private final CommandConnect cc;

    /**
     * Command to update the graph width
     */
    private final CommandGraphWidth cgw;

    /**
     * Command to play the videos and graphs
     */
    private final CommandPlay cp;

    /**
     * Command to jump forward frame by frame based on the video
     */
    private final CommandFrameForward cff;

    /**
     * Command to jump backward frame by frame based on the video
     */
    private final CommandFrameReverse cfr;

    /**
     * Command to bring all the windows to the front
     */
    private final CommandWindowFront cwf;

    /**
     * Command to start offset detection utility
     */
    private final CommandOffsetDetection cod;

    /**
     * Creates a CommandList object with the two passed parameters
     *
     * @param dm Reference to DataModel object to pass into certain Command objects
     * @param g  Reference to DataWindow object to pass into certain Command objects
     */
    public CommandList(DataModel dm, DataWindow g)
    {
        // Create the Command Open Object
        co = new CommandOpen();

        // Create the Command Save Object
        cs = new CommandExport();

        // Create the Command Import Object
        ci = new CommandImport();

        // Create the Command Quit Object
        cq = new CommandQuit();

        // Create the Command Input Mapping Object
        cim = new CommandInputMapping();

        // Create the Command Event Logger Object
        cel = new CommandEventLogger(dm, g);

        ccv = new CommandConvertVideo();

        cve = new CommandVideoEditor();

        cas = new CommandAverageSpeed(dm);

        // Create the Command C1 Viewer Object
        c1v = new CommandC1Viewer(g);

        //Create SQL Connect Object
        cc = new CommandConnect();

        // Add the Command Input Mapping's input map view as an observer to the Data model.
        dm.addObserver(cim.getInputMapping());

        // Create the Command Graph Width Object
        cgw = new CommandGraphWidth();

        // Create the Command Play Object
        cp = new CommandPlay();

        // Create the Command Frame Forward Object
        cff = new CommandFrameForward();

        // Create the Command Frame Reverse Object
        cfr = new CommandFrameReverse();

        // Create the Command Window Front Object
        cwf = new CommandWindowFront();

        // Create the Command Offset Detection Object
        cod = new CommandOffsetDetection();

        // The following setTargets commands require a data model and a graph object.
        // These are passed from the Constructor's parameters.

        // Set the targets for Command Open.
        co.setTargets(dm, g);

        // Set the targets for Command Save
        cs.setTargets(dm, g);

        // Set the targets for Command Import
        ci.setTargets(dm, g);

        // Set the targets for Command Quit
        cq.setTargets(dm, g);

        // Set the target for Command Import Mapping
        cim.setTargets(dm.returnProxy(), g);

        // Set the target for Command C1 Viewer
        c1v.setTargets(dm);

        //Set the target for the SQL command
        cc.setTargets(dm, g);

        // Set the target for Command Graph Width
        cgw.setTarget(dm);

        // Set the target for Command Play
        cp.setTarget(dm);

        // Set the target for Command Frame Forward
        cff.setTarget(dm);

        // Set the target for Command Frame Reverse
        cfr.setTarget(dm);

        // Set the target for Command Window Front
        cwf.setTargets(dm);

        // Set the target for Command Offset Detector
        cod.setTargets(dm, g);
    }

    /**
     * Returns the CommandOpen Object
     *
     * @return CommandOpen object
     */
    public CommandOpen getCommandOpen()
    {
        return co;
    }

    /**
     * Returns the CommandSaveSession Object
     *
     * @return CommandExport object
     */
    public CommandExport getCommandExport()
    {
        return cs;
    }

    /**
     * Returns the CommandImport Object
     *
     * @return CommandImport object
     */
    public CommandImport getCommandImport()
    {
        return ci;
    }

    /**
     * Returns the CommandConnect Object
     *
     * @return CommandConnect object
     */
    public CommandConnect getCommandConnect()
    {
        return cc;
    }

    /**
     * Returns the CommandQuit Object
     *
     * @return CommandQuit object
     */
    public CommandQuit getCommandQuit()
    {
        return cq;
    }

    /**
     * Returns the CommandGraphWidth Object
     *
     * @return CommandGraphWidth object
     */
    public CommandGraphWidth getCommandGraphWidth()
    {
        return cgw;
    }

    /**
     * Returns the CommandPlay Object
     *
     * @return CommandPlay object
     */
    public CommandPlay getCommandPlay()
    {
        return cp;
    }

    /**
     * Returns the CommandFrameForward Object
     *
     * @return CommandFrameForward object
     */
    public CommandFrameForward getCommandFrameForward()
    {
        return cff;
    }

    /**
     * Returns the CommandFrameReverse Object
     *
     * @return CommandFrameReverse object
     */
    public CommandFrameReverse getCommandFrameReverse()
    {
        return cfr;
    }

    /**
     * Returns the CommandInputMapping Object
     *
     * @return CommandInputMapping object
     */
    public CommandInputMapping getCommandInputMapping()
    {
        return this.cim;
    }

    /**
     * Returns the CommandEventLogger Object
     *
     * @return CommandEventLogger object
     */
    public CommandEventLogger getCommandEventLogger()
    {
        return this.cel;
    }

    public CommandConvertVideo getCommandConvertVideo() {return this.ccv; }

    public CommandVideoEditor getCommandVideoEditor() {return this.cve; }

    public CommandAverageSpeed getCommandAverageSpeed() {return this.cas; }

    /**
     * Returns the CommandC1Viewer Object
     *
     * @return CommandC1Viewer object
     */
    public CommandC1Viewer getCommandC1Viewer()
    {
        return c1v;
    }

    /**
     * Returns the CommandWindowFront Object
     *
     * @return CommandWindowFront object
     */
    public CommandWindowFront getCommandWindowFront()
    {
        return this.cwf;
    }

    /**
     * Returns the CommandOffsetDetection Object
     *
     * @return CommandOffsetDetection object
     */
    public CommandOffsetDetection getCommandOffsetDetection()
    {
        return cod;
    }
}
