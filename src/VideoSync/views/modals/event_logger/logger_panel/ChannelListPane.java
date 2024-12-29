/*
 * File: ChannelListPane.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used for listing all the channel names and to allow
 *          the user to update channel car counts
 */

package VideoSync.views.modals.event_logger.logger_panel;

import VideoSync.models.DataModel;
import VideoSync.views.modals.event_logger.commands.CommandUpdateChannelCount;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.List;

public class ChannelListPane extends JPanel
{
    private DataModel dm;

    private final String unavailable = "Unavailable";                         //Unavailable string

    private final String na = "N/A";                                          //NA default string

    private final String channelColumnHeader = "Channels";                    //Channel header

    private final JTable channels;                                            //Channel count table list

    private final JLabel channel;                                             //Label for selected channel name

    private final JLabel channelCount;                                        //Label for selected channel car count

    private final JLabel mode;                                                //Label specifying the increment/decrement mode in which the update on hotkey select

    private CommandUpdateChannelCount commandUpdateChannelCount;

    public ChannelListPane(DataModel dm, int panelHeight)
    {
        this.dm = dm;

        setLayout(new BorderLayout());

        setPreferredSize(new Dimension(200, panelHeight));

        channels = new JTable(new DefaultTableModel())
        {
            @Override
            public boolean editCellAt(int row, int column, EventObject e)
            {
                return false;
            }
        };
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(channelColumnHeader);
        channels.setModel(model);
        channels.getTableHeader().setReorderingAllowed(false);
        channels.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(channels);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel channelDetails = new JPanel(new GridLayout(4, 1));
        channelDetails.setPreferredSize(new Dimension(200, 100));

        JLabel labelChannelCount = new JLabel("Channel Count");
        labelChannelCount.setHorizontalAlignment(JLabel.CENTER);
        labelChannelCount.setFont(new Font(labelChannelCount.getFont().toString(), Font.BOLD, 14));
        channelDetails.add(labelChannelCount);

        channel = new JLabel(unavailable);
        channel.setHorizontalAlignment(JLabel.CENTER);
        channel.setFont(new Font(labelChannelCount.getFont().toString(), Font.PLAIN, 12));
        channelDetails.add(channel);

        channelCount = new JLabel(na);
        channelCount.setHorizontalAlignment(JLabel.CENTER);
        channelCount.setFont(new Font(labelChannelCount.getFont().toString(), Font.PLAIN, 24));
        channelCount.setForeground(Color.BLUE);

        channelCount.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(channelCount.getText().equals(na))
                    return;

                UIManager.put("OptionPane.cancelButtonText", "CANCEL");
                UIManager.put("OptionPane.okButtonText", "ENTER");
                String str = JOptionPane.showInputDialog(null, "Car Count: ", null);

                if(str != null)
                {
                    try
                    {
                        int count = Integer.parseInt(str);
                        count = Math.max(count, 0);

                        int selectedIndex = channels.getSelectedRow();
                        dm.setChannelCarCount(count, selectedIndex);
                    }
                    catch(NumberFormatException exc)
                    {
                        JOptionPane.showMessageDialog(
                                null,
                                "Please enter a valid number",
                                "Invalid Number Entered",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });

        channelDetails.add(channelCount);

        mode = new JLabel("Increment");
        mode.setHorizontalAlignment(JLabel.CENTER);
        mode.setFont(new Font(labelChannelCount.getFont().toString(), Font.PLAIN, 12));

        JLabel updateMode = new JLabel("Update Mode: ");
        updateMode.setFont(new Font(labelChannelCount.getFont().toString(), Font.BOLD, 12));

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.add(updateMode);
        panel.add(mode);
        channelDetails.add(panel);

        channelDetails.setBorder(new EtchedBorder());

        add(scrollPane, BorderLayout.CENTER);
        add(channelDetails, BorderLayout.SOUTH);

        setVisible(true);
    }

    /**
     * Updates the channel list
     *
     * @param channelList list of channel names
     */
    public void updateChannelList(List<String> channelList)
    {

        System.out.println(channelList);

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(channelColumnHeader);

        for(String channelName : channelList)
        {
            model.addRow(new String[]{channelName});
        }

        int col = channels.getSelectedColumn();
        int row = channels.getSelectedRow();
        channels.setModel(model);

        if(col > -1 && row > -1)
        {
            channels.setColumnSelectionInterval(col, col);
            channels.setRowSelectionInterval(row, row);
        }

        revalidate();
        repaint();
    }

    /**
     * Clears the channel list table
     */
    public void clearChannelList()
    {

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(channelColumnHeader);

        channels.setModel(model);

        System.out.println("Channel Table list cleared");

        revalidate();
        repaint();
    }

    /**
     * Resets the selected channel pane
     */
    public void resetSelectedChannelPane()
    {
        channel.setText(unavailable);
        channelCount.setText(na);
        revalidate();
        repaint();
    }

    public String getSelectedChannel()
    {
        return channel.getText();
    }

    /**
     * Sets the channel update command
     *
     * @param commandUpdateChannelCount reference to channel update command
     */
    public void setCommandUpdateChannelCount(CommandUpdateChannelCount commandUpdateChannelCount)
    {
        this.commandUpdateChannelCount = commandUpdateChannelCount;
        this.commandUpdateChannelCount.setTargets(channels, channel, channelCount, mode);
    }
}
