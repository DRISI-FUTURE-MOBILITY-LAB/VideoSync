/*
 * File: EventTableCellComponent.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used for a custom table cell component
 *          to display event proxies in a JTable cell
 */

package VideoSync.views.modals.event_logger.logger_panel;

import VideoSync.objects.event_logger.EventProxy;
import VideoSync.views.videos.VideoPlayer;

import javax.swing.*;
import java.awt.*;

public class EventTableCellComponent extends JPanel
{

    private final JLabel channel;

    private final JLabel timestamp;

    private final JLabel offset;

    private final JLabel detector;

    private final JTextArea comment;

    private final JLabel omit;

    private final JLabel eventID;

    private final JPanel top;

    private final JPanel center;

    private final JPanel bottom;

    public EventTableCellComponent()
    {

        setLayout(new BorderLayout());

        top = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 5));
        center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        channel = new JLabel();
        timestamp = new JLabel();
        offset = new JLabel();
        detector = new JLabel();
        omit = new JLabel();
        eventID = new JLabel();
        comment = new JTextArea();

        comment.setEditable(false);

        channel.setFont(new Font(channel.getFont().toString(), Font.PLAIN, channel.getFont().getSize()));
        timestamp.setFont(new Font(timestamp.getFont().toString(), Font.PLAIN, timestamp.getFont().getSize()));
        offset.setFont(new Font(offset.getFont().toString(), Font.PLAIN, offset.getFont().getSize()));
        detector.setFont(new Font(detector.getFont().toString(), Font.PLAIN, detector.getFont().getSize()));
        comment.setFont(new Font(comment.getFont().toString(), Font.PLAIN, comment.getFont().getSize()));
        eventID.setFont(new Font(eventID.getFont().toString(), Font.PLAIN, eventID.getFont().getSize()));
        omit.setFont(new Font(omit.getFont().toString(), Font.PLAIN, omit.getFont().getSize()));

        top.add(new JLabel("Time: "));
        top.add(timestamp);
        top.add(new JLabel("Offset: "));
        top.add(offset);
        top.add(new JLabel("Channel: "));
        top.add(channel);
        top.add(new JLabel("Detector Type: "));
        top.add(detector);
        top.add(new JLabel("Event ID: "));
        top.add(eventID);

        center.add(comment);

        bottom.add(new JLabel("Omitted: "));
        bottom.add(omit);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void updateData(EventProxy eventProxy, boolean isSelected, JTable table)
    {
        // FIXME: empty catch block lmao
        try
        {
            channel.setText(eventProxy.getChannelName());
            timestamp.setText(VideoPlayer.convertToTimeFormat(eventProxy.getTimestamp()));
            offset.setText(Double.toString(eventProxy.getOffset() / 1000.0));
            detector.setText(eventProxy.getDetector());
            comment.setText(eventProxy.getComment());
            omit.setText((eventProxy.isOmitted() ? "Yes" : "No"));
            eventID.setText(eventProxy.getEventTag());
        }
        catch(NullPointerException e)
        {
        }

        if(isSelected)
        {
            top.setBackground(table.getSelectionBackground());
            center.setBackground(table.getSelectionBackground());
            bottom.setBackground(table.getSelectionBackground());
            comment.setBackground(table.getSelectionBackground());
        }
        else
        {
            top.setBackground(table.getBackground());
            center.setBackground(table.getBackground());
            bottom.setBackground(table.getBackground());
            comment.setBackground(table.getBackground());
        }
    }
}
