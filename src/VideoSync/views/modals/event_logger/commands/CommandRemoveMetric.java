/*
 * File: CommandRemoveMetric.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to remove a
 *          tracked metric
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandRemoveMetric extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Tracked metrics table UI element reference
     */
    private JTable metricsTable;

    public CommandRemoveMetric(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTarget(JTable metricsTable)
    {
        this.metricsTable = metricsTable;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        /*
         * Remove the selected tracked metric
         */
        dataModel.removeMetric((String) metricsTable.getValueAt(metricsTable.getSelectedRow(), 0));
    }
}
