/*
 * File: MetricPanel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used to display the results of tracked
 *          metrics for channels under a detector type
 */

package VideoSync.views.modals.event_logger.metrics_panel;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.ChannelCountProxy;
import VideoSync.objects.event_logger.Event;
import VideoSync.objects.event_logger.metrics.Metric;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MetricPanel extends JPanel
{

    /**
     * Detector type label
     */
    private final JLabel detector;

    /**
     * Table that lists metrics for all channels associated to the detector type
     */
    private final JTable table;

    /**
     * Average for label for detector
     */
    private final JLabel detector_average;

    /**
     * Channel count proxies associated to the detector type
     */
    private final List<ChannelCountProxy> channelCountProxies;

    /**
     * Data model reference
     */
    private DataModel dataModel;

    public MetricPanel(String detectorName, List<ChannelCountProxy> channelCountProxies)
    {

        setLayout(new BorderLayout());

        this.detector = new JLabel(detectorName);

        this.detector_average = new JLabel();

        this.channelCountProxies = channelCountProxies;

        this.dataModel = null;

        table = new JTable(new DefaultTableModel())
        {
            // FIXME: Avoid raw use of parameterized class
            @Override
            public Class getColumnClass(int column)
            {
                if(getColumnName(column).equals("Omitted"))
                {
                    return Boolean.class;
                }
                else
                {
                    return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column)
            {
                return getColumnName(column).equals("Omitted");
            }

            @Override
            public void tableChanged(TableModelEvent e)
            {
                super.tableChanged(e);
                if(getColumnName(e.getColumn()).equals("Omitted") && dataModel != null)
                {
                    omitChannelCount();
                }
            }
        };

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        JPanel detectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel overallAveragePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        detectorPanel.add(this.detector);

        overallAveragePanel.add(this.detector_average);

        add(detectorPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(overallAveragePanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(-1, 0));
    }

    /**
     * Sets the data model reference for use by the metric panel
     *
     * @param dataModel data model reference
     */
    public void setDataModel(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    /**
     * Calculates the metric for each channel count proxy as well as the weighted average for the
     * detector type
     *
     * @param general   boolean flag to determine if the method call was invoked by the general "metric"
     * @param metricMap map of configured tracked metrics
     * @param eventTags event tags configured
     */
    public void calculateMetrics(boolean general, Map<String, Metric> metricMap, Map<String, String> eventTags)
    {

        /*
         * This map will hold the value for each variables/event tags
         */
        Map<String, Object> variableValueMap = new HashMap<>();

        /*
         * This map will hold the cumulative for all variables/event tags
         */
        Map<String, Object> weightedVariableValueMap = new HashMap<>();

        Double overall_average = 0.0;

        List<List<Object>> data = new Vector<>();

        for(ChannelCountProxy channelCountProxy : channelCountProxies)
        {
            /*
             * Clear out the values inside the map
             */
            variableValueMap.clear();

            Double result = null;

            /*
             * For each metric, calculate the metric result for the channel count
             * proxy and accumulate the variable values for that metric in the weighted
             * variable value map.
             */
            for(String metric_name : metricMap.keySet())
            {

                Metric metric = metricMap.get(metric_name);

                result = (Double) metric.calculate(channelCountProxy);

                if(result != null)
                {
                    if(!channelCountProxy.isOmitted())
                    {
                        Map<String, Object> variables = metric.getVariableValues();
                        for(String variable : variables.keySet())
                        {
                            int value = weightedVariableValueMap.get(variable) == null ? 0 : (Integer) weightedVariableValueMap.get(variable);
                            value += (Integer) variables.get(variable);
                            weightedVariableValueMap.put(variable, value);
                        }
                    }
                }

                /*
                 * Store the variable values associated for this channel count proxy in
                 * the variable value map
                 */
                variableValueMap.putAll(metric.getVariableValues());
            }

            /*
             * If this metric panel is used as a general metrics panel, then we only care
             * about displaying the variable values for each channel count proxy and not
             * the metric results
             */
            if(general)
            {
                for(String eventTag : eventTags.keySet())
                {
                    if(!variableValueMap.containsKey(eventTag))
                    {
                        int eventCount = 0;
                        if(channelCountProxy.getEventsByTag(eventTag) != null)
                        {
                            for(Event event : channelCountProxy.getEventsByTag(eventTag))
                            {
                                if(!event.isOmitted())
                                {
                                    ++eventCount;
                                }
                            }
                        }
                        variableValueMap.put(eventTag, eventCount);
                    }
                }
            }

            /*
             * Populate the row data with the variable values and metric results
             */
            List<Object> rowData = new Vector<>();

            /*
             * If this metric panel is being used to display general metrics, then
             * we don't care if the channel is omitted or not
             */
            if(!general)
            {
                rowData.add(channelCountProxy.isOmitted());
            }

            rowData.add(channelCountProxy.getChannelName());

            if(!general)
            {
                rowData.add(result != null ? result : 0.0);
            }

            /*
             * Add the channel count proxies car count
             */
            rowData.add(channelCountProxy.getCarCount());

            /*
             * Add the variable values stored in the variable value map
             */
            for(String variable : variableValueMap.keySet())
            {
                rowData.add(variableValueMap.get(variable) != null ? variableValueMap.get(variable) : 0);
            }

            data.add(rowData);
        }

        /*
         * If this is metric panel is not being used to display general metrics, then display
         * the weighted average of metric result
         */
        if(!general)
        {
            for(String metric_name : metricMap.keySet())
            {
                overall_average = (Double) metricMap.get(metric_name).calculate(weightedVariableValueMap);
                if(!overall_average.isNaN())
                {
                    overall_average = BigDecimal.valueOf(overall_average).setScale(2, RoundingMode.HALF_UP).doubleValue();
                }
            }
            detector_average.setText(overall_average.toString());
        }

        /*
         * Create the table model and add table headers
         */
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        if(!general)
        {
            model.addColumn("Omitted");
        }

        model.addColumn("Channel Name");

        if(!general)
        {
            model.addColumn("Result");
        }

        model.addColumn("Car Count");

        for(String variable : variableValueMap.keySet())
        {
            model.addColumn(variable);
        }

        for(List<Object> rowData : data)
        {
            model.addRow(rowData.toArray());
        }

        table.setModel(model);
    }

    /**
     * Omits a channel count when the user checks a checkbox next to the listed
     * channel count
     */
    private void omitChannelCount()
    {
        int selectedRow = table.getSelectedRow();
        String channelName = table.getValueAt(selectedRow, 1).toString();
        dataModel.omitChannelCount(detector.getText(), channelName);
    }
}
