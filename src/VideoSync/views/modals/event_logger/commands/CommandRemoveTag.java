/*
 * File: CommandRemoveTag.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to remove an
 *          event tag
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.Event;
import VideoSync.objects.event_logger.metrics.Metric;
import VideoSync.views.modals.event_logger.modals.TagsPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class CommandRemoveTag extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to parent window
     */
    private TagsPanel tagsPanel;

    /**
     * Text field UI element reference
     */
    private JTextField textField;

    /**
     * Text area UI element reference
     */
    private JTextArea textArea;

    public CommandRemoveTag(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTargets(JTextField textField, JTextArea textArea, TagsPanel tagsPanel)
    {
        this.textField = textField;
        this.textArea = textArea;
        this.tagsPanel = tagsPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        /*
         * Check to see if there are events associated to the event-tag-to-be-removed
         */
        List<Event> associatedEvents = dataModel.getAllEventsByTag(textField.getText());

        /*
         * If there are events associated to the event tag, prompt to the user if they
         * wish to delete all the events associated to the event tag. Choosing not to
         * aborts the tag removal operation.
         */
        if(!associatedEvents.isEmpty())
        {
            int response = JOptionPane.showConfirmDialog(tagsPanel,
                    "There are recorded events associated with the event tag '" + textField.getText() + "'.\n" +
                            "Delete all events associated with this tag?", "Delete all " + textField.getText() + " events", JOptionPane.YES_NO_OPTION);
            if(response == JOptionPane.NO_OPTION)
            {
                cancelAction();
                return;
            }
        }


        /*
         * Check to see if there are metrics which contain this variable (event tag)
         */
        List<String> associatedMetrics = dataModel.getMetricsAssociatedByTag(textField.getText());

        /*
         * If there are metrics associated with this variable, prompt if the user wishes to delete delete the
         * tracked metrics. Choosing not to aborts the tag removal operation.
         */
        if(!associatedMetrics.isEmpty())
        {
            int response = JOptionPane.showConfirmDialog(tagsPanel,
                    "There are tracked metrics associated with the event tag '" + textField.getText() + "'.\n" +
                            "Delete all tracked metrics associated with this tag?", "Delete Tracked Metrics", JOptionPane.YES_NO_OPTION);
            if(response == JOptionPane.NO_OPTION)
            {
                cancelAction();
                return;
            }
        }

        /*
         * ...otherwise, delete all events and metrics associated to this variable/event tag
         */
        dataModel.removeAllEventsByEventTag(textField.getText());
        dataModel.removeAllMetricsByTag(textField.getText());
        dataModel.removeTag(textField.getText());

        /*
         * If there are no event tags, remove the general metric
         */
        Metric metric = dataModel.getMetric("General");
        if(dataModel.getEventTags().isEmpty())
        {
            dataModel.removeMetric("General");
        }
        /*
         * ...otherwise, set the general metric variables
         */
        else
        {
            if(metric != null)
            {
                metric.setVariables(dataModel.getEventTags());
            }
        }

        /*
         * Reset text field and area
         */
        textField.setText("Tag Name");
        textArea.setText("Tag Description");
    }

    /**
     * Aborts the tag removal operation
     */
    private void cancelAction()
    {
        JOptionPane.showMessageDialog(tagsPanel, "Event tag removal action canceled.", "Action Canceled", JOptionPane.OK_OPTION);
    }
}
