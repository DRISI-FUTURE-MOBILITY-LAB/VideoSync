/*
 * ****************************************************************
 * File: 			CommandAddGraphPanel.java
 * Date Created:  	August 20, 2020
 * Programmer:		Jenzel Arevalo
 *
 * Purpose:			Used to add additional graph panels to main
 * panel of UI
 ****************************************************************/

package VideoSync.views.tabbed_panels.graphs.commands;

import VideoSync.models.DataModel;
import VideoSync.views.tabbed_panels.graphs.GraphPane;
import VideoSync.views.tabbed_panels.graphs.GraphPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CommandAddGraphPanel extends AbstractAction
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
        addGraphPanel();
    }

    /**
     * Invoked when additional panels need to be added during load time of a data set
     * @param quantity amount of GraphPanels to add
     */
    public void addGraphPanels(int quantity)
    {
        for(int i = 0; i < quantity; i++)
        {
            addGraphPanel();
        }
    }

    /**
     * Adds a single instance of a graph panel
     */
    private void addGraphPanel()
    {
        //Create a new graph panel
        GraphPanel graphPanel = new GraphPanel(dm.returnProxy());


        //Because the default combo box model is set at the instantiation of a new graph panel object, we need to update
        //the combo box device model to reflect the devices currently loaded in VideoSync.
        graphPanel.updateDeviceModel();


        graphPane.getGraphPanels().add(graphPanel);


        //Add a new video region for the created graph panel
        dm.addVideoRegion(graphPanel);


        dm.addObserver(graphPanel);


        //Insert new graph panel into graph pane
        int insertAt = Math.max(0, graphPane.getComponentCount() - 1);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        graphPane.add(graphPanel, gbc, insertAt);

        //Update data window
        dm.getDataWindow().revalidate();
        dm.notifyObservers();
    }
}
