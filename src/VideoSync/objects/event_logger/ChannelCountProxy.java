/*
 * File: ChannelCountProxy.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Proxy class which contains is used mainly
 *          in Event Logger UI.
 */

package VideoSync.objects.event_logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChannelCountProxy implements Comparable<String>
{

    private final String channelName;                           //Channel name

    private final int carCount;                                 //Channel car count

    private final Map<String, List<Event>> eventsByCategory;    //Channel events by category

    private final boolean omitted;                              //Omission status of channel count

    public ChannelCountProxy(String channelName, ChannelCount channelCount)
    {
        this.channelName = channelName;
        this.carCount = channelCount.getCarCount();
        this.eventsByCategory = channelCount.getEventsMap();
        this.omitted = channelCount.isOmitted();
    }

    /**
     * Proxy reference to channel count name
     *
     * @return channelName reference
     */
    public String getChannelName()
    {
        return channelName;
    }

    /**
     * Proxy reference to channel car count
     *
     * @return carCount reference
     */
    public int getCarCount()
    {
        return carCount;
    }

    /**
     * Proxy reference to channel car count omission status
     *
     * @return omitted reference
     */
    public boolean isOmitted()
    {
        return omitted;
    }

    /**
     * Proxy reference to events of a given event tag name
     *
     * @param eventTag event tag name to indicate which events to return
     * @return list of events associated to the given event tag name
     */
    public List<Event> getEventsByTag(String eventTag)
    {
        return eventsByCategory.get(eventTag);
    }

    /**
     * Proxy reference to get all event tag names
     *
     * @return set of event tag names
     */
    public Set<String> getTags()
    {
        return eventsByCategory.keySet();
    }

    @Override
    public int compareTo(String o)
    {
        return channelName.length() - o.length();
    }
}
