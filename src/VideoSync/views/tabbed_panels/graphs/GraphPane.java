/*
 * ****************************************************************
 * File: 			GraphPane.java
 * Date Created:  	August 20, 2020
 * Programmer:		Jenzel Arevalo
 *
 * Purpose:			Used to dynamically add and remove graph panels
 ****************************************************************/

package VideoSync.views.tabbed_panels.graphs;

import VideoSync.models.DataModel;
import VideoSync.models.DataModelProxy;
import VideoSync.views.tabbed_panels.graphs.commands.CommandAddGraphPanel;
import VideoSync.views.tabbed_panels.graphs.commands.CommandRemoveGraphPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

public class GraphPane extends JPanel implements Observer
{
    /**
     * Compile time selection of the number of graphs
     */
    public final int GRAPH_ROWS = 8;

    /**
     * Holds the list of graph panels
     */
    private final List<GraphPanel> graphPanels;

    /**
     * Button to allow user to add new graph panels
     */
    private final JButton buttonAdd;

    /**
     * Button to allow user to remove a graph panel
     */
    private final JButton buttonRemove;

    /**
     * Command object for action of adding a graph panel
     */
    private final CommandAddGraphPanel commandAddGraphPanel;

    /**
     * Command object for action of removing a graph panel
     */
    private final CommandRemoveGraphPanel commandRemoveGraphPanel;

    public GraphPane(DataModel dm)
    {
        setLayout(new GridBagLayout());

        //Instantiate add graph button
        buttonAdd = new JButton("Add Graph");
        buttonAdd.setEnabled(false);

        //Instantiate remove graph button
        buttonRemove = new JButton("Remove Graph");
        buttonRemove.setEnabled(false);
        buttonRemove.setEnabled(false);

        //Make adjustments to size of add button
        buttonAdd.setPreferredSize(buttonRemove.getPreferredSize());


        //Create panel to house buttons
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelButtons.add(buttonAdd);
        panelButtons.add(buttonRemove);


        //Create panel to hold graph panels
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        graphPanels = new Vector<>();
        add(new JPanel(), gbc);

        for(int i = 0; i < GRAPH_ROWS; i++)
        {
            graphPanels.add(new GraphPanel(dm.returnProxy()));
            dm.addObserver(graphPanels.get(i));
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(graphPanels.get(i), gbc);
            revalidate();
            repaint();
        }

        //Add command to add graph panels
        commandAddGraphPanel = new CommandAddGraphPanel();
        commandAddGraphPanel.setTargets(dm, this);
        buttonAdd.addActionListener(commandAddGraphPanel);

        //Add command to remove graph panels
        commandRemoveGraphPanel = new CommandRemoveGraphPanel();
        commandRemoveGraphPanel.setTargets(dm, this);
        buttonRemove.addActionListener(commandRemoveGraphPanel);


        //Add panel buttons
        add(panelButtons);
    }

    //Adds additional graph panels when a data set has more than 8 graph panels
    public void addGraphPanels(int quantity)
    {
        commandAddGraphPanel.addGraphPanels(quantity);
    }

    //Removes excess graph panels when a data set has fewer than 8 graph panels
    public void removeGraphPanels(int quantity)
    {
        commandRemoveGraphPanel.removeGraphPanels(quantity);
    }

    //Resets graph panel to 8 graph panels when a new data set is loaded
    public void resetDefaultGraphPanels()
    {

        //Amount of excess or missing panels
        int extraPanels = graphPanels.size() - GRAPH_ROWS;

        //If there are no extra panels, then there are eight graph rows
        if(extraPanels != 0)
        {
            if(extraPanels < 0)
            {
                addGraphPanels(Math.abs(extraPanels));
            }
            else
                removeGraphPanels(Math.abs(extraPanels));
        }
    }

    public List<GraphPanel> getGraphPanels()
    {
        return graphPanels;
    }

    @Override
    public void update(Observable o, Object arg)
    {
        if(arg instanceof DataModelProxy)
        {
            if(((DataModelProxy) arg).dataLoaded())
            {
                buttonAdd.setEnabled(true);
                buttonRemove.setEnabled(true);
            }
            else
            {
                buttonAdd.setEnabled(false);
                buttonRemove.setEnabled(false);
            }
        }
    }
}
