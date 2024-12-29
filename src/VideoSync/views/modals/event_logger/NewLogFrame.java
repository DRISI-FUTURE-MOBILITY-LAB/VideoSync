/*
 * File: NewLogFrame.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Window that opens when user wishes to create a new event log.
 *          This window allows the user to conveniently configure the tracked
 *          metrics, event tags and hotkeys.
 */

package VideoSync.views.modals.event_logger;

import VideoSync.models.DataModel;
import VideoSync.objects.DeviceInputMap;
import VideoSync.views.modals.event_logger.modals.HotkeysPanel;
import VideoSync.views.modals.event_logger.modals.MetricsPanel;
import VideoSync.views.modals.event_logger.modals.TagsPanel;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

// TODO: This whole thing can potentially be done cleaner
public class NewLogFrame extends JFrame implements ActionListener
{
    /**
     * String constant for tag configuration window title
     */
    private final String config_tags_header = "Configure Event Log Tags";

    /**
     * String constant for hotkey configuration window title
     */
    private final String config_hotkeys_header = "Configure Channel Hotkeys";

    /**
     * String constant for metric configuration window title
     */
    private final String config_metrics_header = "Configure Metrics";

    /**
     * Window width constant
     */
    private static final int WINDOW_WIDTH = 600;

    /**
     * Window height constant
     */
    private static final int WINDOW_HEIGHT = 550;

    /**
     * Tag configuration panel
     */
    private final TagsPanel tagsPanel;

    /**
     * Hotkey configuration panel
     */
    private final HotkeysPanel hotkeysPanel;

    /**
     * Metrics configuration panel
     */
    private final MetricsPanel metricsPanel;

    /**
     * Back button to cycle to previous configuration panels
     */
    private final JButton buttonBack;

    /**
     * Next button to cycle to next configuration panels
     */
    private final JButton buttonNext;

    public NewLogFrame(DataModel dataModel, EventLogger eventLogger)
    {

        setTitle("New Event Log: " + config_metrics_header);

        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        setLayout(new BorderLayout());

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));

        tagsPanel = new TagsPanel();

        tagsPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT - 80));

        tagsPanel.initializeCommands(dataModel);

        tagsPanel.setDataModelProxy(dataModel.returnProxy());

        tagsPanel.setVisible(false);

        dataModel.addObserver(tagsPanel);

        center.add(tagsPanel);

        hotkeysPanel = new HotkeysPanel(dataModel.returnProxy());

        hotkeysPanel.initializeCommandList(dataModel);

        hotkeysPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT - 80));

        ArrayList<String> channels = new ArrayList<>();
        for(DeviceInputMap deviceInputMap : dataModel.getC1InputMap())
        {
            channels.add(deviceInputMap.getChannelName());
        }
        hotkeysPanel.initializeChannelLists(channels);

        center.add(hotkeysPanel, BorderLayout.CENTER);

        hotkeysPanel.setVisible(false);

        dataModel.addObserver(hotkeysPanel);

        metricsPanel = new MetricsPanel();

        metricsPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT - 80));

        metricsPanel.setDataModel(dataModel);

        metricsPanel.setCommandOpenMetricTemplates(eventLogger);

        metricsPanel.setCommandRemoveMetric();

        metricsPanel.setVisible(true);

        dataModel.addObserver(metricsPanel);

        center.add(metricsPanel, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        buttonBack = new JButton("Back");
        buttonNext = new JButton("Next");
        buttonBack.addActionListener(this);
        buttonNext.addActionListener(this);
        buttonBack.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(new EtchedBorder());
        buttons.add(buttonBack);
        buttons.add(buttonNext);

        add(buttons, BorderLayout.SOUTH);

        setVisible(false);

        setResizable(false);

        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == buttonNext || e.getSource() == buttonBack)
        {
            if(metricsPanel.isVisible())
            {
                tagsPanel.setVisible(true);
                metricsPanel.setVisible(false);
                buttonBack.setEnabled(true);
                buttonNext.setText("Next");
                setTitle("New Event Log: " + config_tags_header);
            }
            else if(tagsPanel.isVisible())
            {
                if(e.getSource() == buttonBack)
                {
                    tagsPanel.setVisible(false);
                    metricsPanel.setVisible(true);
                    buttonBack.setEnabled(false);
                    buttonNext.setText("Next");
                    setTitle("New Event Log: " + config_metrics_header);
                }
                else if(e.getSource() == buttonNext)
                {
                    tagsPanel.setVisible(false);
                    hotkeysPanel.setVisible(true);
                    buttonNext.setText("Finish");
                    setTitle("New Event Log: " + config_hotkeys_header);
                }
            }
            else if(hotkeysPanel.isVisible())
            {
                if(e.getSource() == buttonBack)
                {
                    hotkeysPanel.setVisible(false);
                    tagsPanel.setVisible(true);
                    buttonNext.setText("Next");
                    setTitle("New Event Log: " + config_tags_header);
                }
                else if(e.getSource() == buttonNext)
                {
                    dispose();
                }
            }
            revalidate();
            repaint();
        }
    }
}
