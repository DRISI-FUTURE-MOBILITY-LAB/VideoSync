/*
 * File: CommandAddTag.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to add event tags
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandAddTag extends AbstractAction
{

    /**
     * Reference to data model
     */
    private final DataModel dataModel;

    /**
     * Reference to text field UI element in configure tags window
     */
    private JTextField textField;

    /**
     * Referenced to text area UI element in configure tags window
     */
    private JTextArea textArea;

    public CommandAddTag(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTargets(JTextField textField, JTextArea textArea)
    {
        this.textField = textField;
        this.textArea = textArea;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        /*
         * Adds tag to be used in Event Logger
         */
        dataModel.addTag(textField.getText(), textArea.getText());

        textField.setText("Tag Name");
        textArea.setText("Tag Description");
    }
}
