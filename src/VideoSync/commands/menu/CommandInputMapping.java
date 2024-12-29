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
import VideoSync.views.modals.InputMapping;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandInputMapping extends AbstractAction
{
    private static final long serialVersionUID = 589846253157734111L;

    /**
     * Primarily used for keeping track of the Input Mappings visibility
     */
    private InputMapping im;

    /**
     * Reference to DataWindow for positioning InputMapping Window
     */
    private DataWindow dataWindow;

    public CommandInputMapping()
    {
        // Construct the InputMapping so we can use it
        im = new InputMapping();
    }

    /**
     * Sets the InputMapping DataModelProxy reference.
     *
     * @param dmp DataModelProxy object to pass to InputMapping object
     */
    public void setTargets(DataModelProxy dmp, DataWindow dw)
    {
        // Set the data model proxy for the Input Mapping
        im.setDataModelProxy(dmp);
        this.dataWindow = dw;
    }

    /**
     * Return the InputMapping object
     *
     * @return InputMapping object
     */
    public InputMapping getInputMapping()
    {
        return this.im;
    }

    /**
     * Called when the user selects the "Input Mapping" option from the Options menu.
     *
     * @param e ActionEvent triggered by button press
     */
    public void actionPerformed(ActionEvent e)
    {
        // Check to see if the Input Mapping is visible. If not, display it.
        if(!im.isVisible())
        {
            im.setLocationRelativeTo(dataWindow);
            im.displayPanel(true);
        }

    }
}
