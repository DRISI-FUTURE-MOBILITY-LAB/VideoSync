/*
 * ****************************************************************
 * File: 			CommandC1Viewer.java
 * Date Created:  	July 2020
 * Programmer:		Aleksey Zasorin
 *
 * Purpose:			To handle opening of the C1 Viewer window.
 *
 * ****************************************************************
 */

package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.views.modals.c1_viewer.C1Viewer;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandC1Viewer extends AbstractAction
{
    /**
     * Reference to C1Viewer panel object
     */
    private C1Viewer c1Viewer;

    /**
     * Reference to parent window
     */
    private DataWindow dataWindow;

    /**
     * Create a new C1Viewer panel object
     */
    public CommandC1Viewer(DataWindow dw)
    {
        c1Viewer = new C1Viewer();
        dataWindow = dw;
    }

    /**
     * Configure C1 Viewer to have reference to DataModelProxy, add C1Viewer as an observer to the DataModel
     * to receive graph updates and initialize the panel
     *
     * @param dataModel DataModel object to get proxy from and register observer to
     */
    public void setTargets(DataModel dataModel)
    {
        c1Viewer.setDataModelProxy(dataModel.returnProxy());
        dataModel.addObserver(c1Viewer);
        c1Viewer.initPanel();
    }

    /**
     * Displays C1 Viewer panel when ActionEvent is received
     *
     * @param e ActionEvent triggered by button press
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        c1Viewer.setLocationRelativeTo(dataWindow);
        c1Viewer.displayPanel();
    }
}
