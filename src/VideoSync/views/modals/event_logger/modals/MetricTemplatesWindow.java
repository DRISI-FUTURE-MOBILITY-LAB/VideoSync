/*
 * File: MetricTemplatesWindow.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used to hold the metric templates for the
 *          user to add to the metric configuration
 */

package VideoSync.views.modals.event_logger.modals;

import VideoSync.models.DataModel;
import VideoSync.objects.event_logger.metrics.Metric;
import VideoSync.views.modals.event_logger.commands.CommandAddMetricTemplate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;
import java.util.Map;

public class MetricTemplatesWindow extends JFrame implements MouseListener
{

    /**
     * Sets the reference to the data model
     */
    private DataModel dataModel;

    /**
     * String constant for metric header
     */
    private final String metric_header = "Metric";

    /**
     * Table to list all metric templates
     */
    private final JTable metric_templates;

    /**
     * Text area to display metric description when a metric
     * is selected
     */
    private final JTextArea metric_description;

    /**
     * Button to add a selected metric template to the tracked
     * metrics table in the metric configuration window
     */
    private final JButton button_AddMetricTemplate;

    public MetricTemplatesWindow()
    {

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(300, 350));
        setResizable(false);

        JPanel panel_main = new JPanel();

        panel_main.setLayout(new BoxLayout(panel_main, BoxLayout.PAGE_AXIS));
        panel_main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel_main.setAlignmentX(Component.CENTER_ALIGNMENT);

        metric_templates = new JTable(new DefaultTableModel())
        {
            @Override
            public boolean editCellAt(int row, int column, EventObject e)
            {
                return false;
            }
        };

        DefaultTableModel defaultTableModel = new DefaultTableModel();
        defaultTableModel.addColumn(metric_header);

        metric_templates.setModel(defaultTableModel);
        metric_templates.addMouseListener(this);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(metric_templates);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));

        button_AddMetricTemplate = new JButton();

        buttons.add(button_AddMetricTemplate);

        metric_description = new JTextArea();
        metric_description.setEditable(false);
        metric_description.setLineWrap(true);
        metric_description.setBackground(getBackground());

        panel_main.add(Box.createRigidArea(new Dimension(0, 10)));
        panel_main.add(scrollPane);
        panel_main.add(Box.createRigidArea(new Dimension(0, 10)));
        panel_main.add(metric_description);
        panel_main.add(Box.createRigidArea(new Dimension(0, 10)));
        panel_main.add(buttons);
        panel_main.add(Box.createRigidArea(new Dimension(0, 10)));


        getContentPane().add(panel_main);

        pack();
    }

    /**
     * Adds the metric templates to the metric templates table
     */
    public void addMetrics()
    {

        Map<String, Metric> metrics = dataModel.getTemplateMetrics();

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(metric_header, metrics.keySet().toArray());
        metric_templates.setModel(model);
    }

    /*
     * Returns a reference to the metric templates table
     */
    public JTable getMetricTemplates()
    {
        return metric_templates;
    }

    /*
     * Sets the data model window reference
     * @param dataModel reference
     */
    public void setDataModel(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    /**
     * Bind the 'add metric template' command to the 'add metric template' button
     */
    public void initializeCommandsList()
    {
        CommandAddMetricTemplate commandAddMetricTemplate = new CommandAddMetricTemplate(dataModel);
        commandAddMetricTemplate.setTargets(metric_templates, this);
        button_AddMetricTemplate.setAction(commandAddMetricTemplate);
        button_AddMetricTemplate.setText("Add Template");
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {

        String metric = (String) metric_templates.getValueAt(metric_templates.getSelectedRow(), metric_templates.getSelectedColumn());
        metric_description.setText(dataModel.getTemplateMetric(metric).getDescription());

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
