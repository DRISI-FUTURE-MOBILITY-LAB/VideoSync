/*
 * File: CommandAddMetricTemplate.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to add a metric from the
 *          metric template window
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.metrics.Metric;
import VideoSync.views.modals.event_logger.modals.MetricTemplatesWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

public class CommandAddMetricTemplate extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Metric table UI element reference in configure metrics window
     */
    private JTable metric_templates;

    /**
     * Reference to parent window
     */
    private MetricTemplatesWindow metricTemplatesWindow;

    public CommandAddMetricTemplate(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTargets(JTable metric_templates, MetricTemplatesWindow mtw)
    {
        this.metric_templates = metric_templates;
        this.metricTemplatesWindow = mtw;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        EventQueue.invokeLater(() -> {

            /*
             * Get the metric name displayed in the metric templates table
             */
            if(metric_templates.getSelectedRow() != -1 || metric_templates.getSelectedColumn() != -1)
            {
                String metric_name = (String) metric_templates.getValueAt(metric_templates.getSelectedRow(), metric_templates.getSelectedColumn());
                Map<String, Metric> loadedMetrics = dataModel.getMetrics();

                boolean overwrite_existing_metric = false;
                int option;

                /*
                 * Inquire if user would like to overwrite an existing metric
                 */
                if(loadedMetrics.containsKey(metric_name))
                {
                    option = JOptionPane.showConfirmDialog(metricTemplatesWindow, "Metric already exists. Overwrite existing metric?", "Metric Already Exists", JOptionPane.YES_NO_OPTION);

                    if(option == JOptionPane.YES_OPTION)
                    {
                        overwrite_existing_metric = true;
                    }
                }

                /*
                 * If the user wishes to overwrite an existing metric, or if the metric is not already being tracked,
                 * add the metric
                 */
                if(overwrite_existing_metric || !loadedMetrics.containsKey(metric_name))
                {

                    /*
                     * Get the metric from the metric templates
                     */
                    Metric metric_template = dataModel.getTemplateMetric(metric_name);
                    Map<String, String> variables = metric_template.getVariables();
                    Map<String, String> eventTags = dataModel.getEventTags();

                    /*
                     * Check to see if tag that is associated to the template metric already exists
                     * in the event tags map. If so, inquire from user if they wish to overwrite the
                     * tag. Otherwise, do not overwrite the tag and move on to the next tag to check.
                     */
                    for(String variable : variables.keySet())
                    {
                        if(eventTags.containsKey(variable))
                        {
                            option = JOptionPane.showConfirmDialog(metricTemplatesWindow, variable + " event tag already exists. Overwrite with template tag?.",
                                    "Event Tag Already Exists", JOptionPane.YES_NO_OPTION);
                            if(option == JOptionPane.YES_OPTION)
                            {
                                dataModel.addTag(variable, variables.get(variable));
                            }
                        }
                        else
                        {
                            dataModel.addTag(variable, variables.get(variable));
                        }
                    }

                    /*
                     * Finally add metric to be tracked in Event Logger
                     */
                    dataModel.addMetric(metric_name, metric_template);
                }
            }
        });
    }
}
