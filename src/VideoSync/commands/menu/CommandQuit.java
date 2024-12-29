/*
 * ****************************************************************
 * File: 			CommandQuit.java
 * Date Created:  	June 4, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the menu to
 *                  quit VideoSync
 *
 * ****************************************************************
 */

package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandQuit extends AbstractAction
{
    private static final long serialVersionUID = 1L;

    /**
     * Used to reference and notify the DataModel of any changes
     */
    private DataModel dm;

    /**
     * Used to reference and notify the DataWindow of any changes.
     */
    private DataWindow g;

    /**
     * Sets the references to the DataModel and Data Window
     *
     * @param dm Reference to DataModel object
     * @param g  Reference to DataWindow object
     */
    public void setTargets(DataModel dm, DataWindow g)
    {
        // Set the DataModel from the passed parameter
        this.dm = dm;
        // Set the DataWindow from the passed parameter.
        this.g = g;
    }

    /**
     * Called when the user selects the "Quit" option from the File menu.
     *
     * @param ae ActionEvent triggered by Quit button press
     */
    public void actionPerformed(ActionEvent ae)
    {
        // Set the button options to be displayed to the user.
        Object[] options = {"Yes", "No"};

        // Return the result from the Option Dialog, using the Data Window (g) as the parent view.
        int n = JOptionPane.showOptionDialog(g.getRootPane(),
                "Are you sure you would like to exit VideoSync?",
                "Exit VideoSync",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);

        // If the user chooses "Yes", then notify the data model to begin shutting down VideoSync
        if(n == 0)
        {
            dm.performShutdownOperations();

            //If VideoSync was started by another utility, need to manually close the window since the process isn't ending.
            if(!dm.getStandaloneInstance())
            {
                g.dispose();
            }
        }
    }
}
