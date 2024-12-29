/*
 * File: EventWindow.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used for adding and updating events
 */

package VideoSync.views.modals.event_logger.event_window;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.event_logger.EventProxy;
import VideoSync.views.modals.event_logger.commands.CommandAddEvent;
import VideoSync.views.modals.event_logger.commands.CommandUpdateEvent;
import VideoSync.views.videos.VideoPlayer;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventWindow extends JFrame implements MouseListener, ItemListener
{
    /**
     * Window width
     */
    private final int WINDOW_WIDTH = 1350;

    /**
     * Window height
     */
    private final int WINDOW_HEIGHT = 200;

    /**
     * Data model proxy reference
     */
    private DataModelProxy dataModelProxy;

    /**
     * Time stamp label
     */
    private final JLabel timestamp;

    /**
     * Offset label
     */
    private final JLabel offset;

    /**
     * Hidden textfield for adjusting timestamp
     */
    private final JTextField textfield_timestamp;

    /**
     * Hidden textfield for adjusting offset
     */
    private final JTextField textField_offset;

    /**
     * Detector label
     */
    private final JLabel detector;

    /**
     * Combo box for available channels
     */
    private final JComboBox<String> channels;

    /**
     * Combo box for available event tags
     */
    private final JComboBox<String> eventTags;

    /**
     * Comment text area
     */
    private final JTextArea comment;

    /**
     * Event omission checkbox
     */
    private final JCheckBox omit;

    /**
     * Cancel button
     */
    private final JButton cancel;

    /**
     * Add/Update button
     */
    private final JButton update;

    /**
     * Old event proxy reference for updating events
     */
    private EventProxy oldEventProxy;

    public EventWindow(String title)
    {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setTitle(title);

        channels = new JComboBox<>();
        channels.addItemListener(this);

        eventTags = new JComboBox<>();

        comment = new JTextArea();
        comment.addMouseListener(this);
        comment.setText("Enter an event description.");
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        comment.addFocusListener(new FocusAdapter() {
             @Override
             public void focusGained(FocusEvent e)
             {
                 super.focusGained(e);
                 if(comment.getText().equals("Enter an event description."))
                    comment.setText("");
             }
        });

        omit = new JCheckBox();

        cancel = new JButton();
        cancel.setAction(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("Action canceled");
                dispose();
            }
        });
        cancel.setText("Cancel");

        update = new JButton();

        timestamp = new JLabel();
        offset = new JLabel();
        detector = new JLabel();

        timestamp.addMouseListener(this);
        offset.addMouseListener(this);

        detector.setFont(new Font(detector.getFont().toString(), Font.PLAIN, detector.getFont().getSize()));
        timestamp.setFont(new Font(timestamp.getFont().toString(), Font.PLAIN, timestamp.getFont().getSize()));
        offset.setFont(new Font(timestamp.getFont().toString(), Font.PLAIN, timestamp.getFont().getSize()));

        textfield_timestamp = new JTextField();
        textfield_timestamp.setVisible(false);

        textField_offset = new JTextField();
        textField_offset.setVisible(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 5));
        JPanel center = new JPanel(new BorderLayout());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        top.add(new JLabel("Timestamp: "));
        top.add(textfield_timestamp);
        top.add(timestamp);
        top.add(new JLabel("Offset: "));
        top.add(offset);
        top.add(textField_offset);
        top.add(new JLabel("Channels: "));
        top.add(channels);
        top.add(new JLabel("Detector Type: "));
        detector.setText("Not Available");
        top.add(detector);
        top.add(new JLabel("Event Tag: "));
        top.add(eventTags);

        center.add(comment);
        comment.setBorder(new EtchedBorder());

        bottom.add(omit);
        bottom.add(new JLabel("Omit Event from Calculations"));
        bottom.add(cancel);
        bottom.add(update);

        add(top, BorderLayout.NORTH);
        add(comment, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        pack();
    }

    public void setSelectedChannel(String channel)
    {
        channels.setSelectedItem(channel);
    }

    /**
     * Sets the event time in which the event had occurred
     *
     * @param msTime event timestamp in milliseconds
     */
    private void setEventTime(long msTime)
    {
        timestamp.setText(VideoPlayer.convertToTimeFormat(msTime));
    }

    /**
     * Sets the offset in which the event had occurred
     *
     * @param offset offset value
     */
    private void setOffset(long offset)
    {
        double ms = offset / 1000.0;
        this.offset.setText(Double.toString(ms));
    }

    /**
     * Sets the channel list for use in the channel combo box
     *
     * @param channelsList list of channel names
     */
    private void setChannelsList(List<String> channelsList)
    {
        List<String> list = new Vector<>();
        list.add("Channels");
        list.addAll(channelsList);

        DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<>();
        for(String channel : list)
        {
            defaultComboBoxModel.addElement(channel);
        }

        channels.setModel(defaultComboBoxModel);
    }

    /**
     * Sets the list of available event tags in the event tag combo list
     *
     * @param eventTagsList list of event tags
     * @param toolTipsList  list of event tag descriptions
     */
    private void setEventTagsList(List<String> eventTagsList, List<String> toolTipsList)
    {
        List<String> tagList = new Vector<>();
        tagList.add("Event Tags");
        tagList.addAll(eventTagsList);

        List<String> tipList = new Vector<>();
        tipList.add("Event Tags");
        tipList.addAll(toolTipsList);

        DefaultComboBoxModel<String> defaultComboBoxModel = new DefaultComboBoxModel<>();
        for(String eventTag : tagList)
        {
            defaultComboBoxModel.addElement(eventTag);
        }

        EventWindowTagsComboBoxRenderer renderer = new EventWindowTagsComboBoxRenderer();
        renderer.setTooltips(tipList);

        eventTags.setModel(defaultComboBoxModel);
        eventTags.setRenderer(renderer);
    }

    /**
     * Returns a reference to the old event proxy
     *
     * @return old event proxy reference
     */
    public EventProxy getOldEventProxy()
    {
        return oldEventProxy;
    }

    /**
     * Returns the offset label
     *
     * @return reference to offset label reference
     */
    public JLabel getOffset()
    {
        return offset;
    }

    /**
     * Returns the timestamp label
     *
     * @return reference to timestamp label
     */
    public JLabel getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns reference to channels combo box
     *
     * @return reference to channels combo box
     */
    public JComboBox<String> getChannels()
    {
        return channels;
    }

    /**
     * Returns reference to event tags combo box
     *
     * @return reference to event tags combo box
     */
    public JComboBox<String> getEventTags()
    {
        return eventTags;
    }

    /**
     * Returns reference to comment text area
     *
     * @return comment text area reference
     */
    public JTextArea getComment()
    {
        return comment;
    }

    /**
     * Returns reference to omission check box
     *
     * @return omission reference
     */
    public JCheckBox getOmit()
    {
        return omit;
    }

    /**
     * Helper method to verify timestamp inputted by user using regular expression patterns
     *
     * @param input timestamp inputted by user
     * @return input status match
     */
    private boolean isValidTimestampInput(String input)
    {
        Pattern timestampPattern = Pattern.compile("[0-9]+:[0-5][0-9]:[0-5][0-9](.[0-9]?[0-9]?[0-9]?)?");
        Matcher result = timestampPattern.matcher(input);
        return result.matches();
    }

    /**
     * Helper method to verify offset inputted by user using regular expression patterns
     *
     * @param input offset inputted by user
     * @return input status match
     */
    private boolean isValidOffsetInput(String input)
    {
        Pattern offsetPattern = Pattern.compile("(-)?[0-9]+(.[0-9]?[0-9]?[0-9]?)?");
        Matcher result = offsetPattern.matcher(input);
        System.out.println(result.matches());
        return result.matches();
    }

    /**
     * Sets the data model proxy
     *
     * @param dataModelProxy proxy for DataModel
     */
    public void setDataModelProxy(DataModelProxy dataModelProxy)
    {
        this.dataModelProxy = dataModelProxy;
    }

    /**
     * Initializes the UI elements once the data model proxy has been set
     */
    public void initializeUIElements()
    {
        Map<String, String> eventTags = dataModelProxy.getEventTags();
        List<String> tags = new Vector<>(eventTags.keySet());

        List<String> tooltips = new Vector<>(eventTags.values());

        List<DeviceInputMap> inputMaps = dataModelProxy.getC1InputMap();
        List<String> channels = new Vector<>();
        for(DeviceInputMap deviceInputMap : inputMaps)
        {
            channels.add(deviceInputMap.getChannelName());
        }

        setEventTime(dataModelProxy.getCurrentPosition());
        setOffset(dataModelProxy.getGraphOffset());
        setChannelsList(channels);
        setEventTagsList(tags, tooltips);
    }

    /**
     * Sets the command add event to the update button
     *
     * @param commandAddEvent command object to bind update button to add event action
     */
    public void setCommandAddEvent(CommandAddEvent commandAddEvent)
    {
        commandAddEvent.setTargets(this);
        update.setAction(commandAddEvent);
        update.setText("Add Event");
    }

    /**
     * Sets the command update event to the update button
     *
     * @param commandUpdateEvent command object to bind update button to update event action
     */
    public void setCommandUpdateEvent(CommandUpdateEvent commandUpdateEvent)
    {
        update.setAction(commandUpdateEvent);
        update.setText("Update Event");
    }

    /**
     * Pre-configures the event window UI elements using the details specified in the
     * event proxy. Mostly used for updating existing events.
     *
     * @param eventProxy event proxy of selected event for updating
     */
    public void setSelectedAttributes(EventProxy eventProxy)
    {

        oldEventProxy = new EventProxy(eventProxy.getChip(), eventProxy.getPin(), eventProxy.getTimestamp(), eventProxy.getOffset(), eventProxy.getChannelName(), eventProxy.getDetector(), eventProxy.getComment(), eventProxy.getEventTag(), eventProxy.isOmitted());

        for(int i = 0; i < channels.getItemCount(); i++)
        {
            if(channels.getItemAt(i).equals(eventProxy.getChannelName()))
            {
                channels.setSelectedIndex(i);
                break;
            }
        }

        for(int i = 0; i < eventTags.getItemCount(); i++)
        {
            if(eventTags.getItemAt(i).equals(eventProxy.getEventTag()))
            {
                eventTags.setSelectedIndex(i);
                break;
            }
        }

        setEventTime(eventProxy.getTimestamp());

        setOffset(eventProxy.getOffset());

        comment.setText(eventProxy.getComment());

        omit.setSelected(eventProxy.isOmitted());

        detector.setText(eventProxy.getDetector());

        revalidate();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        /*
         * Displays the timestamp text field and hides the
         * timestamp label if the timestamp label has been selected
         */
        if(e.getSource() == timestamp)
        {
            timestamp.setVisible(false);
            textfield_timestamp.setVisible(true);
            textfield_timestamp.setText(timestamp.getText());
        }

        /*
         * Displays the offset text field and hides the
         * offset label if the offset label has been selected
         */
        else if(e.getSource() == offset)
        {
            offset.setVisible(false);
            textField_offset.setVisible(true);
            textField_offset.setText(offset.getText());
        }

        /*
         * If comment text area is selected and timestamp and offset
         * textfields are visible, set the timestamp and offset label
         * text to what's been inputted in their text field counter parts.
         */
        else if(e.getSource() == comment)
        {
            if(textfield_timestamp.isVisible())
            {
                textfield_timestamp.setVisible(false);
                if(isValidTimestampInput(textfield_timestamp.getText()))
                {
                    timestamp.setText(textfield_timestamp.getText());
                }
                timestamp.setVisible(true);
            }

            if(textField_offset.isVisible())
            {
                textField_offset.setVisible(false);
                if(isValidOffsetInput(textField_offset.getText()))
                {
                    offset.setText(textField_offset.getText());
                }
                offset.setVisible(true);
            }
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

    @Override
    public void itemStateChanged(ItemEvent e)
    {
        /*
         * Sets the detector type label according to the channel selected in the channel
         * combo box
         */
        if(e.getSource() == channels)
        {
            if(channels.getSelectedIndex() > 0)
            {
                String detectorType = dataModelProxy.getC1InputMap().get(channels.getSelectedIndex() - 1).getDetectorType();
                if(detectorType == null || detectorType.equals("Select Type"))
                {
                    detector.setText("Not Available");
                }
                else
                {
                    detector.setText(detectorType);
                }
            }
        }
    }
}
