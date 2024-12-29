/*
 * File: MetricsPanel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used to allow the user to configure
 *          tracked metrics for Event Logger
 */

package VideoSync.views.modals.event_logger.modals;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.metrics.Metric;
import VideoSync.views.modals.event_logger.EventLogger;
import VideoSync.views.modals.event_logger.commands.CommandOpenMetricTemplates;
import VideoSync.views.modals.event_logger.commands.CommandRemoveMetric;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.*;

public class MetricsPanel extends JPanel implements Observer, MouseListener
{

    /**
     * String constant for metric name
     */
    private final String metric_name = "Metric";

    /**
     * String constant for metric description
     */
    private final String metric_description = "Description";

    /**
     * Data model reference
     */
    private DataModel dataModel;

    /**
     * Reference to parent window
     */
    private DataWindow dataWindow;

    /**
     * Table that lists all tracked metrics
     */
    private final JTable metricsTable;

    /**
     * Button that enables the user to create a new metric - feature to be implemented
     */
    private final JButton buttonNewMetric;

    /**
     * Button to remove tracked metrics
     */
    private final JButton buttonRemoveMetric;

    /**
     * Button to load a custom metric - feature to be implemented
     */
    private final JButton buttonLoadMetric;

    /**
     * Button to add pre-configured metric templates from the metric templates window
     */
    private final JButton buttonTemplateMetric;

    public MetricsPanel()
    {

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setAlignmentX(Component.CENTER_ALIGNMENT);

        metricsTable = new JTable(new DefaultTableModel())
        {
            @Override
            public boolean editCellAt(int row, int column, EventObject e)
            {
                return false;
            }
        };

        DefaultTableModel defaultTableModel = new DefaultTableModel();
        defaultTableModel.addColumn(metric_name);
        defaultTableModel.addColumn(metric_description);

        metricsTable.setModel(defaultTableModel);
        metricsTable.addMouseListener(this);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(metricsTable);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));

        buttonNewMetric = new JButton("New Metric");
        buttonRemoveMetric = new JButton("");
        buttonLoadMetric = new JButton("Load Metric");
        buttonTemplateMetric = new JButton();

        buttonNewMetric.setEnabled(false);
        buttonLoadMetric.setEnabled(false);

        buttons.add(buttonNewMetric);
        buttons.add(buttonRemoveMetric);
        buttons.add(buttonLoadMetric);
        buttons.add(buttonTemplateMetric);

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(scrollPane);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(buttons);
        add(Box.createRigidArea(new Dimension(0, 10)));

    }

    /**
     * Sets the reference to the data model
     *
     * @param dataModel data model reference
     */
    public void setDataModel(DataModel dataModel)
    {
        this.dataModel = dataModel;
        this.dataModel.addObserver(this);
    }

    /**
     * Sets the button template metric command
     */
    public void setCommandOpenMetricTemplates(EventLogger eventLogger)
    {
        buttonTemplateMetric.setAction(new CommandOpenMetricTemplates(dataModel, eventLogger));
        buttonTemplateMetric.setText("Templates");
    }

    /**
     * Sets the remove metric command to the remove metric button
     */
    public void setCommandRemoveMetric()
    {
        CommandRemoveMetric commandRemoveMetric = new CommandRemoveMetric(dataModel);
        commandRemoveMetric.setTarget(metricsTable);
        buttonRemoveMetric.setAction(commandRemoveMetric);
        buttonRemoveMetric.setText("Remove Metric");
    }

    @Override
    public void update(Observable o, Object arg)
    {
        if(arg.equals("Add Metric") || arg.equals("Remove Metric"))
        {
            Map<String, Metric> metricMap = dataModel.getMetrics();
            if(metricMap != null) updateMetricsList(metricMap);
        }
    }

    /**
     * Updates the tracked metrics list table with metrics in the metric map
     *
     * @param metricMap map for tracked metrics
     */
    public void updateMetricsList(Map<String, Metric> metricMap)
    {
        DefaultTableModel model = new DefaultTableModel();

        if(metricMap != null)
        {

            List<String> metric_descriptions = new Vector<>();
            for(String metric : metricMap.keySet())
            {
                metric_descriptions.add(metricMap.get(metric).getDescription());
            }

            model.addColumn(metric_name, metricMap.keySet().toArray());
            model.addColumn(metric_description, metric_descriptions.toArray());
            metricsTable.setModel(model);
        }
        else
        {
            model.addColumn(metric_name);
            model.addColumn(metric_description);
        }
        metricsTable.setModel(model);

        revalidate();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if(metricsTable.getValueAt(metricsTable.getSelectedRow(), metricsTable.getSelectedColumn()).equals("General"))
        {
            buttonRemoveMetric.setEnabled(false);
        }
        else
        {
            buttonRemoveMetric.setEnabled(true);
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }
}
