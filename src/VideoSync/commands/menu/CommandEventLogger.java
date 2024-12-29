/*
 * ****************************************************************
 * File: 			CommandEventLogger.java
 * Date Created:  	December 26, 2018
 * Programmer:		Jenzel Arevalo
 *
 * Purpose:			To handle an action request from the menu to
 *                  record events
 * ****************************************************************
 */

package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandEventLogger extends AbstractAction
{

    /**
     * Primarily used for keeping track of Event Logger's visibility
     */
    private final EventLogger eventLogger;

    /**
     * Reference to parent window
     */
    private final DataWindow dataWindow;

    public CommandEventLogger(DataModel dm, DataWindow dw)
    {
        dataWindow = dw;

        eventLogger = new EventLogger(dm);
        dm.addObserver(eventLogger);
    }

    public EventLogger getEventLogger()
    {
        return eventLogger;
    }

    /**
     * Called when the user selects the "Event Logger" option from the Options menu.
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Check to see if the Input Mapping is visible. If not, display it.
        if(!eventLogger.isVisible())
        {
            eventLogger.setLocationRelativeTo(dataWindow);
            eventLogger.setVisible(true);
        }
    }
}
