/**
 * ****************************************************************
 * File: 			CommandGraphWidth.java
 * Date Created:  	May 29, 2013
 * Programmer:		Dale Reed
 * <p>
 * Purpose:			To handle an action request from Data Window to
 * change the time width of the graph.
 * <p>
 * <p>
 * ****************************************************************
 */
package VideoSync.commands.windows.graph;

import VideoSync.models.DataModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

@SuppressWarnings("rawtypes")
public class CommandGraphWidth extends AbstractAction
{
    private static final long serialVersionUID = 1L;

    /**
     * Used to reference to the DataModel
     */
    private DataModel dm;

    /**
     * Sets the references to the DataModel
     *
     * @param dm Reference to DataModel object to set
     */
    public void setTarget(DataModel dm)
    {
        this.dm = dm;
    }

    /**
     * Called when the user chooses to change the Graph Scale
     *
     * @param e ActionEvent triggered by GraphWidth dropdown press
     */
    @SuppressWarnings("ConstantConditions")
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() instanceof JComboBox)
        {
            // Get the combo box source that initiated the action
            JComboBox cb = (JComboBox) e.getSource();

            // Ensure that the name of the combo box is "Seconds", if it is, then we can update the graph scale with
            // the new chosen string value of the combo box.
            if(cb.getName().equals("seconds"))
            {
                // Update the graph scale based on the value of the Combo box.
                dm.setGraphWidth(Double.parseDouble((String) cb.getSelectedItem()));
            }
        }
    }
}
