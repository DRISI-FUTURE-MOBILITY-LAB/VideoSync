/*
 * ****************************************************************
 * File: 			CommandRemoveGraphPanel.java
 * Date Created:  	August 20, 2020
 * Programmer:		Jenzel Arevalo
 *
 * Purpose:			Used to remove excess graph panels to main panel
 * of UI
 ****************************************************************/
package VideoSync.views.tabbed_panels.graphs.commands;

import VideoSync.models.DataModel;
import VideoSync.views.tabbed_panels.graphs.GraphPane;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandRemoveGraphPanel extends AbstractAction
{

    private DataModel dm;

    private GraphPane graphPane;

    public void setTargets(DataModel dm, GraphPane graphPane)
    {
        this.dm = dm;
        this.graphPane = graphPane;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(graphPane.getGraphPanels().size() > 1)
        {
            removeGraphPanel();
        }
    }

    /**
     * Invoked when excess panels need to be deleted during load time of a data set
     * @param quantity amount of GraphPanels to remove
     */
    public void removeGraphPanels(int quantity)
    {
        for(int i = 0; i < quantity; i++)
        {
            removeGraphPanel();
        }
    }

    /**
     * Removes a single instance of a graph panel
     */
    private void removeGraphPanel()
    {
        dm.deleteObserver(graphPane.getGraphPanels().get(graphPane.getGraphPanels().size() - 1));
        dm.removeVideoRegion(graphPane.getGraphPanels().get(graphPane.getGraphPanels().size() - 1));
        graphPane.remove(graphPane.getGraphPanels().get(graphPane.getGraphPanels().size() - 1));
        graphPane.getGraphPanels().remove(graphPane.getGraphPanels().size() - 1);
        graphPane.revalidate();
        graphPane.repaint();
    }
}
