package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.autoanalysis.AutoAnalysis;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandOpenAutoAnalysis extends AbstractAction
{
    private DataModel dataModel;
    private EventLogger eventLogger;

    private AutoAnalysis autoAnalysis;

    public CommandOpenAutoAnalysis(DataModel dataModel, EventLogger eventLogger)
    {
        this.dataModel = dataModel;
        this.eventLogger = eventLogger;

        autoAnalysis = new AutoAnalysis(dataModel);
        autoAnalysis.initPanel();
        dataModel.addObserver(autoAnalysis);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataModel.getC1InputMap() != null && dataModel.getVideoFiles() != null && dataModel.getInputMappingFiles() != null)
        {
            if(dataModel.getEventLogFile() != null)
            {
                autoAnalysis.setVisible(true);
                autoAnalysis.pack();
                autoAnalysis.setLocationRelativeTo(eventLogger);
            }
            else
            {
                JOptionPane.showMessageDialog(eventLogger, "No event log file loaded in to Event Logger.", "No Event Log File Loaded to Event Logger", JOptionPane.OK_OPTION);
            }
        }
        else
        {
            JOptionPane.showMessageDialog(eventLogger, "The following files need to be loaded in to VideoSync before utilizing Auto Analysis:\n"
                    + "- C1 data file (.c1)\n"
                    + "- Input mapping file (.mpf)\n"
                    + "- Video file (.mp4)", "Missing Required Files", JOptionPane.OK_OPTION);
        }
    }
}
