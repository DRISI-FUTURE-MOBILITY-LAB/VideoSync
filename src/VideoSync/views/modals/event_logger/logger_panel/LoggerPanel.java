/*
 * File: LoggerPanel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used to hold the events panel and
 *          channel car count pane
 */

package VideoSync.views.modals.event_logger.logger_panel;

import VideoSync.models.DataModel;
import VideoSync.models.DataModelProxy;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.EDeviceType;
import VideoSync.objects.event_logger.ChannelCount;
import VideoSync.objects.event_logger.Event;
import VideoSync.objects.event_logger.EventProxy;
import VideoSync.views.modals.event_logger.commands.*;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class LoggerPanel extends JPanel
{

    /**
     * Channel list pane for use to update car counts associated to channels
     */
    private final ChannelListPane channelListPane;

    /**
     * Events panel to store recorded events
     */
    private final EventsPanel eventsPanel;

    public LoggerPanel(DataModel dm, int panelWidth, int panelHeight)
    {
        setLayout(new BorderLayout());

        setPreferredSize(new Dimension(panelWidth, panelHeight));

        channelListPane = new ChannelListPane(dm, getHeight());
        eventsPanel = new EventsPanel(getWidth(), getHeight());

        add(channelListPane, BorderLayout.WEST);
        add(eventsPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    /**
     * Updates channel and event tables with updated information invoked by changes made to
     * the input mapping file, addition/removal/update of events, etc.
     *
     * @param dataModelProxy reference to data model proxy
     */
    public void updateChannelAndEventTables(DataModelProxy dataModelProxy)
    {
        if(dataModelProxy.getEventLogFile() != null)
        {

            List<DeviceInputMap> inputMaps = dataModelProxy.getInputMapForDevice(EDeviceType.DEVICE_C1);
            List<String> channelNames = new Vector<>();
            for(DeviceInputMap dim : inputMaps)
            {
                channelNames.add(dim.getChannelName());
            }

            channelListPane.updateChannelList(channelNames);

            List<EventProxy> eventProxies = new Vector<>();
            Map<String, String> eventTags = dataModelProxy.getEventTags();

            Map<Integer, Map<Integer, ChannelCount>> channelCountCollection = dataModelProxy.getChannelCountCollection();

            for(Integer chip : channelCountCollection.keySet())
            {
                Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);
                for(Integer pin : pinMaps.keySet())
                {                                         //TODO: Is there a way to avoid this?;
                    for(String eventTag : eventTags.keySet())
                    {
                        List<Event> events = pinMaps.get(pin).getEventsByClassification(eventTag);
                        if(events != null)
                        {
                            for(Event event : events)
                            {
                                String channelName = dataModelProxy.getDeviceInputMapByChipAndPin(EDeviceType.DEVICE_C1, chip, pin).getChannelName();
                                String detector = dataModelProxy.getDeviceInputMapByChipAndPin(EDeviceType.DEVICE_C1, chip, pin).getDetectorType();
                                if(detector == null || detector.equals("Select Type"))
                                    detector = "Not Available";
                                eventProxies.add(new EventProxy(chip, pin, event, eventTag, detector, channelName));
                            }
                        }
                    }
                }
            }

            Collections.sort(eventProxies);

            eventsPanel.setEventProxies(eventProxies);

            repaint();
            revalidate();
        }
    }

    /**
     * Update combo box filters
     *
     * @param deviceInputMaps used for adding the channel names of each channel to the combo box
     * @param eventTags       used for adding the event tag names of each channel to the combo box
     */
    public void updateFilters(List<DeviceInputMap> deviceInputMaps, Map<String, String> eventTags)
    {
        List<String> channelNames = new Vector<>();
        for(DeviceInputMap deviceInputMap : deviceInputMaps)
        {
            channelNames.add(deviceInputMap.getChannelName());
        }
        eventsPanel.updateChannelsFilterList(channelNames);

        if(eventTags != null)
        {
            List<String> tags = new Vector<>(eventTags.keySet());
            eventsPanel.updateTagsFilterList(tags);
        }
    }

    /**
     * Resets the logger panel channel list pane and events panel UI components to default states
     */
    public void resetPanel()
    {
        channelListPane.clearChannelList();
        channelListPane.resetSelectedChannelPane();
        eventsPanel.clearEventsList();
        eventsPanel.resetFilterModels();
        eventsPanel.updatePanelUIStates(false);
    }

    public String getSelectedChannel()
    {
        return channelListPane.getSelectedChannel();
    }

    /**
     * Sets the update channel count command for use in the channel list pane
     *
     * @param commandUpdateChannelCount command for use in the channel list pane
     */
    public void setCommandUpdateChannelCount(CommandUpdateChannelCount commandUpdateChannelCount)
    {
        channelListPane.setCommandUpdateChannelCount(commandUpdateChannelCount);
    }

    /**
     * Sets the new event command for use in the events panel
     *
     * @param commandNewEvent command for use in the events panel
     */
    public void setCommandNewEvent(CommandNewEvent commandNewEvent)
    {
        eventsPanel.setCommandNewEvent(commandNewEvent);
    }

    /**
     * Sets the remove event command for use in the events panel
     *
     * @param commandRemoveEvent command for use in the events panel
     */
    public void setCommandRemoveEvent(CommandRemoveEvent commandRemoveEvent)
    {
        eventsPanel.setCommandRemoveEvent(commandRemoveEvent);
    }

    /**
     * Sets the edit event command for use in the events panel
     *
     * @param commandEditEvent command for use in the events panel
     */
    public void setCommandEditEvent(CommandEditEvent commandEditEvent)
    {
        eventsPanel.setCommandEditEvent(commandEditEvent);
    }

    /**
     * Sets the jump to event command for use in the events panel
     *
     * @param commandJumpToEvent command for use in the events panel
     */
    public void setCommandJumpToEvent(CommandJumpToEvent commandJumpToEvent)
    {
        eventsPanel.setCommandJumpToEvent(commandJumpToEvent);
    }

    /**
     * Sets the event omission command for use in the events panel
     *
     * @param commandOmitEvent command for use in the events panel
     */
    public void setCommandOmitEvent(CommandOmitEvent commandOmitEvent)
    {
        eventsPanel.setCommandOmitEvent(commandOmitEvent);
    }

    /**
     * Updates the UI states specifically for the events panel.
     *
     * @param state boolean flag to enable/disable the UI elements in the events panel
     */
    public void updatePanelUIStates(boolean state)
    {
        eventsPanel.updatePanelUIStates(state);
    }

}
