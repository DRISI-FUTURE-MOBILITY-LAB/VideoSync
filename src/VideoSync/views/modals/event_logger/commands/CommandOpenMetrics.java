/*
 * File: CommandOpenMetrics.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to configure
 *          tracked metrics in a window
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.modals.MetricsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CommandOpenMetrics extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to LoggerPanel component for use in positioning
     */
    private final EventLogger eventLogger;

    public CommandOpenMetrics(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            if(dataModel.getEventLogFile() != null)
            {

                /*
                 * Set up the metrics window
                 */
                JFrame jFrame = new JFrame();
                jFrame.setTitle("Configure Metrics");
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

                MetricsPanel metricsPanel = new MetricsPanel();
                metricsPanel.updateMetricsList(dataModel.getMetrics());
                metricsPanel.setDataModel(dataModel);
                metricsPanel.setCommandRemoveMetric();
                metricsPanel.setCommandOpenMetricTemplates(eventLogger);

                jFrame.add(metricsPanel, BorderLayout.CENTER);
                jFrame.add(applyPanel, BorderLayout.SOUTH);
                jFrame.setPreferredSize(new Dimension(600, 540));
                jFrame.pack();
                jFrame.setResizable(false);

                jFrame.setLocationRelativeTo(eventLogger);
                jFrame.setVisible(true);
            }
            else
            {
                JOptionPane.showMessageDialog(eventLogger, "No event log file loaded in to Event Logger.", "No Event Log File Loaded to Event Logger", JOptionPane.OK_OPTION);
            }
        }
        else
        {
            JOptionPane.showMessageDialog(eventLogger, "The following files need to be loaded in to VideoSync before utilizing Event Logger:\n"
                    + "- C1 data file (.c1)\n"
                    + "- Input mapping file (.mpf)\n"
                    + "- Video file (.mp4)", "Missing Required Files", JOptionPane.OK_OPTION);
        }


    }
}
