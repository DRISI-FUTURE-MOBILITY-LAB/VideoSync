/*
 * File: CommandOpenMetricTemplates.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to open a
 *          window for metric templates
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.modals.MetricTemplatesWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandOpenMetricTemplates extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to parent window
     */
    private EventLogger eventLogger;

    public CommandOpenMetricTemplates(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        /*
         * Create a new metric templates window
         */
        MetricTemplatesWindow metricTemplatesWindow = new MetricTemplatesWindow();
        metricTemplatesWindow.setDataModel(dataModel);
        metricTemplatesWindow.addMetrics();
        metricTemplatesWindow.initializeCommandsList();
        metricTemplatesWindow.setLocationRelativeTo(eventLogger);
        metricTemplatesWindow.setVisible(true);
    }
}
