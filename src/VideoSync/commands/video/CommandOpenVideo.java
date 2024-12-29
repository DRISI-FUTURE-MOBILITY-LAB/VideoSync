/*
 * ****************************************************************
 * File: 			CommandOpenVideo.java
 * Date Created:  	October 4, 2019
 * Programmer:		Aleksey Zasorin
 *
 * Purpose:			To handle an action request from the video
 *                  menu to re-open the respective video window
 *                  if it's been closed.
 *
 * ****************************************************************
 */

package VideoSync.commands.video;

import VideoSync.views.videos.VideoPlayer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandOpenVideo extends AbstractAction
{
    /**
     * Reference to the video player this command will open
     */
    private VideoPlayer vp;

    /**
     * Sets the targets of the command.
     *
     * @param vp Video player object the command will manipulate
     */
    public void setTargets(VideoPlayer vp)
    {
        this.vp = vp;
    }

    /**
     * Called when someone pressed on an "Open Video" button in the Videos tab.
     *
     * @param e ActionEvent object for the button press that invoked this method
     */
    public void actionPerformed(ActionEvent e)
    {
        if(vp != null)
        {
            vp.centerWindow();

            if(!vp.isVisible())
                vp.openWindow();
        }
    }
}
