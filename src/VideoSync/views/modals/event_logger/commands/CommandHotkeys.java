/*
 * File: CommandHotkeys.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to invoke hotkey configuration
 *          window
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.DeviceInputMap;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.modals.HotkeysPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;

public class CommandHotkeys extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Reference to parent window
     */
    private final EventLogger eventLogger;

    public CommandHotkeys(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        /*
         * Invoke hotkey configuration window
         */
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            if(dataModel.getEventLogFile() != null)
            {
                JFrame jFrame = new JFrame();
                jFrame.setTitle("Configure Hotkeys");
                jFrame.setLayout(new BorderLayout());
                jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                List<String> channels = new Vector<>();
                List<DeviceInputMap> deviceInputMaps = dataModel.getC1InputMap();
                for(DeviceInputMap deviceInputMap : deviceInputMaps)
                {
                    channels.add(deviceInputMap.getChannelName());
                }

                HotkeysPanel hotkeysPanel = new HotkeysPanel(dataModel.returnProxy());
                hotkeysPanel.initializeChannelLists(channels);
                hotkeysPanel.initializeCommandList(dataModel);
                hotkeysPanel.initializeHotkeyMaps(dataModel.getHotkeyMaps());
                hotkeysPanel.initializeUpdateCheckBoxMode(dataModel.getHotkeyUpdateMode());
                dataModel.addObserver(hotkeysPanel);

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

                jFrame.add(hotkeysPanel, BorderLayout.CENTER);
                jFrame.add(applyPanel, BorderLayout.SOUTH);
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
