/*
 * File: ChannelCount.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class to hold car counts and events
 *          associated to a channel.
 */

package VideoSync.objects.event_logger;

import java.util.*;

public class ChannelCount
{

    /**
     * Holds car count;
     */
    private int carCount;

    /**
     * Holds a collection of events
     */
    private final Map<String, List<Event>> eventsByCategory;

    /**
     * Flag to identify if event is omitted from calculations
     */
    private boolean omitted;

    public ChannelCount()
    {
        carCount = 0;
        eventsByCategory = new HashMap<>();
        omitted = false;
    }

    public ChannelCount(boolean omitted)
    {
        carCount = 0;
        eventsByCategory = new HashMap<>();
        this.omitted = omitted;
    }

    /**
     * Sets the car count of the channel count
     *
     * @param carCount car count of channel count
     */
    public void setCarCount(int carCount)
    {
        this.carCount = carCount;
    }

    /**
     * Sets the omission flag of the channel count
     *
     * @param omitted flag to indicate if channel count should be omitted from calculations
     */
    public void setOmitted(boolean omitted)
    {
        this.omitted = omitted;
    }

    /**
     * Returns the omission flag of the channel count
     *
     * @return omitted flag
     */
    public boolean isOmitted()
    {
        return omitted;
    }

    /**
     * Increments the channel car count
     */
    public void incCarCount()
    {
        ++carCount;
    }

    /**
     * Decrements the channel car count
     */
    public void decCarCount()
    {
        if(carCount > 0) --carCount;
    }

    /**
     * Returns the channel car count
     *
     * @return channel car count
     */
    public int getCarCount()
    {
        return carCount;
    }

    /**
     * Adds a new event to the corresponding event classification id
     *
     * @param timestamp_value timestamp of video playback
     * @param offset_value    offset value of graph/video
     * @param eventTag        event classification id number
     * @param comment         comment associated with event
     * @param omitted         omission status of event
     */
    public void addEvent(int timestamp_value, int offset_value, String eventTag, String comment, boolean omitted)
    {
        if(!eventsByCategory.containsKey(eventTag))
        {
            List<Event> events = new Vector<>();
            eventsByCategory.put(eventTag, events);
        }
        List<Event> events = eventsByCategory.get(eventTag);
        events.add(new Event(timestamp_value, offset_value, comment, omitted));
    }

    /**
     * Removes an existing event from the collection
     *
     * @param event event to be removed from collection
     */
    public void removeEvent(Event event)
    {
        for(String eventTag : eventsByCategory.keySet())
        {
            List<Event> events = eventsByCategory.get(eventTag);
            for(Event e : events)
            {
                if(e.equals(event))
                {
                    events.remove(e);
                    break;
                }
            }
        }
    }

    /**
     * Omits an existing event from the collection
     *
     * @param event event to be removed from collection
     */
    public void omitEvent(Event event)
    {
        for(String eventTag : eventsByCategory.keySet())
        {
            List<Event> events = eventsByCategory.get(eventTag);
            for(Event e : events)
            {
                if(e.equals(event))
                {
                    e.setOmitted(!e.isOmitted());
                    break;
                }
            }
        }
    }

    /**
     * Returns all the events of the channel based on event tag
     *
     * @param eventTag event tag
     * @return collection of events associated with event tag
     */
    public List<Event> getEventsByClassification(String eventTag)
    {
        return eventsByCategory.get(eventTag);
    }

    /**
     * Returns all the events for the channel
     *
     * @return list of all events associated with channel count
     */
    public List<Event> getAllEvents()
    {
        List<Event> list = new Vector<>();
        for(String eventTag : eventsByCategory.keySet())
        {
            list.addAll(eventsByCategory.get(eventTag));
        }
        return list;
    }

    /**
     * Returns a map of events associated to the channel count organized by event tag name
     *
     * @return map of events organized by event tag name
     */
    public Map<String, List<Event>> getEventsMap()
    {
        return eventsByCategory;
    }

    /**
     * Removes all events associated with an event tag
     *
     * @param eventTag event tag name of events to be removed
     */
    public void removeAllEventsByClassification(String eventTag)
    {
        eventsByCategory.remove(eventTag);
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder();
        List<Event> events = new Vector<>();

        str.append("Car Count: ").append(carCount).append("\n");

        for(String eventTag : eventsByCategory.keySet())
        {
            events.addAll(eventsByCategory.get(eventTag));
        }

        Collections.sort(events);
        for(Object event : events)
        {
            str.append(event.toString()).append("\n\n");
        }

        return str.toString();
    }
}
