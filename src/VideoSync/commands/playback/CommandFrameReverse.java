/*
 * ****************************************************************
 * File: 			CommandFrameReverse.java
 * Date Created:  	June 5, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the play back
 *                  view to reverse the video and graphs forward
 *                  by one frame based on the video timings
 *
 * ****************************************************************
 */
package VideoSync.commands.playback;

import VideoSync.models.DataModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandFrameReverse extends AbstractAction
{
    private static final long serialVersionUID = 1L;

    /**
     * Reference to the DataModel
     */
    private DataModel dm;

    /**
     * Sets the references to the DataModel
     *
     * @param dm DataModel object reference
     */
    public void setTarget(DataModel dm)
    {
        // Set the data model to the one passed
        this.dm = dm;
    }

    /**
     * Called when the user chooses to frame reverse
     *
     * @param arg0 ActionEvent triggered by Frame Reverse button press
     */
    public void actionPerformed(ActionEvent arg0)
    {
        // Notify the data model to reverse the videos and graphs by one frame.
        this.dm.reverseFrame();
    }
}
