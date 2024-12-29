/*
 * ****************************************************************
 * File: 			CommandConnect.java
 * Date Created:  	August 19, 2016
 * Programmer:		Danny Hale
 *
 * Purpose:			To handle an action request from the menu to
 *                  connect to the database and gather C1 data.
 *
 * ****************************************************************
 */
package VideoSync.commands.menu;

import VideoSync.database.C1Database;
import VideoSync.models.DataModel;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.sql.ResultSet;

/**
 * Start a connection to the SQL database in order to grab C1 data.
 */
public class CommandConnect extends AbstractAction
{
    private static final long serialVersionUID = 1L;
    /**
     * Used to accept data from the database to create square wave.
     */
    private DataModel dm;
    /**
     * Used for referencing the main view
     */
    private DataWindow g;

    /**
     * Sets the references to the DataModel and DataWindow
     *
     * @param dm Reference to DataModel object
     * @param g  Reference to DataWindow object
     */
    public void setTargets(DataModel dm, DataWindow g)
    {
        // Set the data model from the passed parameter
        this.dm = dm;

        // Set the data window from the passed parameter
        this.g = g;
    }

    /**
     * Create new C1Database object and fetch C1 data as a ResultSet
     *
     * @param e ActionEvent triggered by button press
     */
    public void actionPerformed(ActionEvent e)
    {
        ResultSet rs = new C1Database(g).getResultSet();
        if(rs != null)
        {
            dm.setC1Data(rs);
        }
    }
}
