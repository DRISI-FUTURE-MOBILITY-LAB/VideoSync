/*
 * ****************************************************************
 * File: 			CommandOffsetDetection.java
 * Date Created:  	January 30, 2018
 * Programmer:		Elliot Hawkins
 *
 * Purpose:			To handle an action request from the menu to
 *                  find a video offset.
 *
 * ****************************************************************
 */
package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.views.modals.OffsetFindingWindow;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class CommandOffsetDetection extends AbstractAction
{
    /**
     * Reference to DataModel object for getting config file and passing to OffsetFindingWindow
     */
    private DataModel dataModel;

    /**
     * Reference to DataWindow for positioning InputMapping Window
     */
    private DataWindow dataWindow;

    /**
     * Reference to OffsetFindingWindow object
     */
    private OffsetFindingWindow offsetFindingWindow = null;

    /**
     * Displays C1 Viewer panel when ActionEvent is received
     *
     * @param e ActionEvent triggered by button press
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        System.out.println("Unimplemented Offset Detection!");

        File detectorJar = new File(OffsetFindingWindow.OFFSET_DETECTOR_JAR);
        System.out.print("Looking for " + detectorJar.getAbsolutePath());

        if(!detectorJar.exists())
        {
            JOptionPane.showMessageDialog(dataWindow, "Could not find Offset Detector program!\nMake sure that VideoSyncDetector.jar is in the same folder as VideoSync!", "Could not find Offset Detector!", JOptionPane.ERROR_MESSAGE);
        }
        else
        {

            if(dataModel.getConfigFile() != null)
            {
                if(offsetFindingWindow == null)
                {
                    offsetFindingWindow = new OffsetFindingWindow(dataModel);
                }

                offsetFindingWindow.setLocationRelativeTo(dataWindow);
                offsetFindingWindow.setVisible(true);
            }
            else
            {
                JOptionPane.showMessageDialog(dataWindow, "A VideoSync config file or directory must be opened to perform automated offset detection.", "No config file or directory", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Sets the references to the DataModel and Data Window
     *
     * @param dm DataModel instance to be set
     */
    public void setTargets(DataModel dm, DataWindow dw)
    {
        // Set the DataModel from the passed parameter
        dataModel = dm;
        dataWindow = dw;
    }
}
