/*
 * File: CommandTags.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to configure
 *          event tags in 'configure event tags' window
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.modals.TagsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CommandTags extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to parent window
     */
    private final EventLogger loggerPanel;

    public CommandTags(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.loggerPanel = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            if(dataModel.getEventLogFile() != null)
            {

                // Create and display tag configuration window
                JFrame jFrame = new JFrame();
                jFrame.setTitle("Configure Tags");
                jFrame.setLayout(new BorderLayout());
                jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                JPanel center = new JPanel();
                center.setLayout(new FlowLayout(FlowLayout.CENTER));

                JPanel applyPanel = new JPanel();
                applyPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
                JButton applyButton = new JButton("Close");
                applyButton.addActionListener(e1 -> {
                    if(e1.getSource() == applyButton)
                    {
                        jFrame.dispose();
                    }
                });
                applyPanel.add(applyButton);

                TagsPanel tagsPanel = new TagsPanel();
                tagsPanel.updateTagsList(dataModel.getEventTags());
                tagsPanel.initializeCommands(dataModel);
                tagsPanel.setDataModelProxy(dataModel.returnProxy());
                dataModel.addObserver(tagsPanel);

                jFrame.add(tagsPanel, BorderLayout.CENTER);
                jFrame.add(applyPanel, BorderLayout.SOUTH);
                jFrame.setPreferredSize(new Dimension(600, 540));
                jFrame.pack();
                jFrame.setResizable(false);

                jFrame.setLocationRelativeTo(loggerPanel);
                jFrame.setVisible(true);
            }
            else
            {
                JOptionPane.showMessageDialog(loggerPanel, "No event log file loaded in to Event Logger.", "No Event Log File Loaded to Event Logger", JOptionPane.OK_OPTION);
            }
        }
        else
        {
            JOptionPane.showMessageDialog(loggerPanel, "The following files need to be loaded in to VideoSync before utilizing Event Logger:\n"
                    + "- C1 data file (.c1)\n"
                    + "- Input mapping file (.mpf)\n"
                    + "- Video file (.mp4)", "Missing Required Files", JOptionPane.OK_OPTION);
        }
    }
}
