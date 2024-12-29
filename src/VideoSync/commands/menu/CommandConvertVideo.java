/*
 * ****************************************************************
 * File: 			CommandImportMapping.java
 * Date Created:  	July 23, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the menu to
 *                  import mapping file.
 *
 * ****************************************************************
 */
package VideoSync.commands.menu;

import VideoSync.models.DataModelProxy;
import VideoSync.views.modals.convert_video.ConvertVideo;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import VideoSync.views.modals.convert_video.FFmpeg;

public class CommandConvertVideo extends AbstractAction
{
    private final ConvertVideo ccv;

    private DataWindow dataWindow;

    public CommandConvertVideo()
    {
        ccv = new ConvertVideo();
    }

    public void setTargets(DataModelProxy dmp, DataWindow dw)
    {
        this.dataWindow = dw;
    }

    public ConvertVideo getConvertVideo()
    {
        return this.ccv;
    }

    /**
     * Called when the user selects the "Convert Video" option from the Options
     * menu.
     *
     * @param e ActionEvent triggered by button press
     */
    public void actionPerformed(ActionEvent e)
    {
        if(!FFmpeg.checkInstalled()) {
            JOptionPane.showMessageDialog(dataWindow,
                    "FFmpeg is not installed, please install it");
        }
        if(!ccv.isVisible() && FFmpeg.checkInstalled())
        {
            ccv.setLocationRelativeTo(dataWindow);
            ccv.setVisible(true);
        }
    }
}
