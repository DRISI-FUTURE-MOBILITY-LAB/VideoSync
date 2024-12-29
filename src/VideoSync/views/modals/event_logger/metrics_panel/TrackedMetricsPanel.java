/*
 * File: Tracked Metrics Panel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used to hold metric panels for each detector
 *          type.
 */

package VideoSync.views.modals.event_logger.metrics_panel;

import VideoSync.models.DataModel;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.event_logger.ChannelCountProxy;
import VideoSync.objects.event_logger.metrics.Metric;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TrackedMetricsPanel extends JPanel implements ItemListener
{

    /**
     * Combo box used to list tracked metrics
     */
    private final JComboBox<String> comboBox;

    /**
     * Panel that holds metric panels
     */
    private final JPanel metricsPanel;

    /**
     * Data model reference
     */
    private DataModel dataModel;

    public TrackedMetricsPanel(int panelWidth, int panelHeight)
    {

        setLayout(new BorderLayout());

        setPreferredSize(new Dimension(panelWidth - 75, panelHeight - 75));

        comboBox = new JComboBox<>();

        DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<>();
        defaultComboBoxModel.addElement("Tracked Metrics");

        comboBox.setModel(defaultComboBoxModel);

        comboBox.addItemListener(this);

        comboBox.setEnabled(false);

        metricsPanel = new JPanel();

        metricsPanel.setLayout(new BoxLayout(metricsPanel, BoxLayout.PAGE_AXIS));

        JScrollPane scrollPane = new JScrollPane();

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        scrollPane.setViewportView(metricsPanel);

        JPanel top = new JPanel();

        top.setLayout(new FlowLayout(FlowLayout.LEFT));

        top.add(comboBox);

        add(top, BorderLayout.NORTH);

        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    /**
     * Updates the panel with a reference to the data model and updates
     * the view with the selected combo box item.
     *
     * @param dataModel reference to data model
     */
    public void updatePanel(DataModel dataModel)
    {
        this.dataModel = dataModel;

        // FIXME: equals() might produce NullPointerException
        if(!comboBox.getSelectedItem().equals("Tracked Metrics"))
        {

            String selectedItem = comboBox.getSelectedItem().toString();

            updateComboBox();

            if(dataModel.getMetrics().containsKey(selectedItem) || selectedItem.equals("General"))
            {
                Map<String, Metric> metricMap = new HashMap<>();
                if(selectedItem.equals("General"))
                {
                    metricMap = dataModel.getMetrics();
                }
                else
                {
                    metricMap.put(selectedItem, dataModel.getMetric(selectedItem));
                }
                comboBox.setSelectedItem(selectedItem);
                displayMetric(metricMap);
            }
            else
            {
                comboBox.setSelectedIndex(0);
                metricsPanel.setVisible(false);
            }
        }
        else
        {
            updateComboBox();
        }
    }

    /**
     * Updates the list of tracked metrics used by the combo box
     */
    private void updateComboBox()
    {
        DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<>();
        defaultComboBoxModel.addElement("Tracked Metrics");

        Map<String, Metric> metrics = dataModel.getMetrics();

        if(dataModel.getEventTags() != null)
        {
            defaultComboBoxModel.addElement("General");
        }

        if(metrics != null)
        {
            for(String metric : metrics.keySet())
            {
                defaultComboBoxModel.addElement(metric);
            }
        }

        comboBox.setModel(defaultComboBoxModel);
    }

    @Override
    public void itemStateChanged(ItemEvent e)
    {
        if(dataModel.getMetrics() != null)
        {
            // FIXME: equals() might produce NullPointerException
            if(!comboBox.getSelectedItem().equals("Tracked Metrics"))
            {
                Map<String, Metric> metricMap = new HashMap<>();
                if(comboBox.getSelectedItem().equals("General"))
                {
                    metricMap = dataModel.getMetrics();
                }
                else
                {
                    metricMap.put(comboBox.getSelectedItem().toString(), dataModel.getMetric(comboBox.getSelectedItem().toString()));
                }
                displayMetric(metricMap);
                metricsPanel.setVisible(true);
            }
            else
            {
                metricsPanel.setVisible(false);
            }
        }
    }

    /**
     * Displays the selected metric for each metric panel
     *
     * @param metricMap metric map of metric to be displayed
     */
    private void displayMetric(Map<String, Metric> metricMap)
    {

        /*
         * Create channel count proxy lists for each detector type
         */
        List<DeviceInputMap> inputMaps = dataModel.getC1InputMap();
        Map<String, List<ChannelCountProxy>> channelCountProxiesByDetector = new HashMap<>();

        for(DeviceInputMap inputMap : inputMaps)
        {
            if(inputMap.getDetectorType() != null && !inputMap.getDetectorType().equals("Select Type"))
            {
                channelCountProxiesByDetector.put(inputMap.getDetectorType(), new Vector<>());
            }
        }

        /*
         * Create channel count proxies for each channel count
         */
        for(int i = 0; i < inputMaps.size(); i++)
        {
            List<ChannelCountProxy> channelCountProxies = channelCountProxiesByDetector.get(inputMaps.get(i).getDetectorType());
            if(channelCountProxies != null)
            {
                channelCountProxies.add(new ChannelCountProxy(inputMaps.get(i).getChannelName(), dataModel.getChannelCountByInputMapIndex(i)));
                channelCountProxiesByDetector.put(inputMaps.get(i).getDetectorType(), channelCountProxies);
            }
        }

        metricsPanel.removeAll();

        /*
         * Create a metric panel for each detector type
         */
        for(String detectorType : channelCountProxiesByDetector.keySet())
        {
            MetricPanel metricPanel = new MetricPanel(detectorType, channelCountProxiesByDetector.get(detectorType));
            metricPanel.setDataModel(dataModel);
            // FIXME: equals() might produce NullPointerException
            metricPanel.calculateMetrics(comboBox.getSelectedItem().equals("General"), metricMap, dataModel.getEventTags());
            metricsPanel.add(metricPanel);
        }

        revalidate();
        repaint();
    }

    /**
     * Reset the panel metric panel to default configurations
     */
    public void resetPanel()
    {
        DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<>();
        defaultComboBoxModel.addElement("Tracked Metrics");
        comboBox.setModel(defaultComboBoxModel);
        comboBox.setEnabled(false);
        metricsPanel.removeAll();

        revalidate();
        repaint();
    }

    /**
     * Update the tracked metrics panel UI element states
     *
     * @param state boolean flag to enable/disable UI elements
     */
    public void updatePanelUIStates(boolean state)
    {
        comboBox.setEnabled(state);
    }
}
