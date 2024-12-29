/*
 * ****************************************************************
 * File: 			CommandOpen.java
 * Date Created:  	June 4, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the menu to
 *                  open a directory containing the files to be
 *                  loaded into VideoSync
 *
 * ****************************************************************
 */
package VideoSync.commands.menu;

import VideoSync.models.DataModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandWindowFront extends AbstractAction
{
    private static final long serialVersionUID = 1L;

    /**
     * Used to reference and notify the DataModel of any changes
     */
    private DataModel dm;

    /**
     * Sets the references to the DataModel
     *
     * @param dm Reference to DataModel object to save
     */
    public void setTargets(DataModel dm)
    {
        this.dm = dm;
    }

    /**
     * Called when the user selects the "Bring to Front" option from the Options menu.
     */
    public void actionPerformed(ActionEvent ae)
    {
        // Notify the data model to bring all the views to the front
        this.dm.presentAllViews();
    }
}
