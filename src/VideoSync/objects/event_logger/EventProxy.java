/*
 * File: EventProxy.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Proxy class to reference events.
 *          This class is mainly used for Event Logger UI
 */

package VideoSync.objects.event_logger;

import VideoSync.views.videos.VideoPlayer;

public class EventProxy implements Comparable<EventProxy>
{

    /**
     * Channel name
     */
    private final String channelName;

    /**
     * Playback offset value in which event occurred
     */
    private final int offset_value;

    /**
     * Playback timestamp in ms in which event occurred
     */
    private final int timestamp_value;

    /**
     * Comment string associated with event
     */
    private final String comment;

    /**
     * Flag to identify if event is omitted from calculations
     */
    private final boolean omitted;

    /**
     * Label to identify event classification
     */
    private final String eventTag;

    /**
     * Event detector type
     */
    private final String detector;

    /**
     * Chip associated to event
     */
    private final int chip;

    /**
     * Pin associated to event
     */
    private final int pin;

    public EventProxy(int chip, int pin, Event event, String eventTag, String detector, String channelName)
    {
        this.chip = chip;
        this.pin = pin;
        this.channelName = channelName;
        this.offset_value = event.getOffset();
        this.timestamp_value = event.getTimestamp();
        this.comment = event.getComment();
        this.omitted = event.isOmitted();
        this.eventTag = eventTag;
        this.detector = detector;
    }

    public EventProxy(int chip, int pin, int timestamp_value, int offset_value, String channelName, String detector, String comment, String eventTag, boolean omitted)
    {
        this.chip = chip;
        this.pin = pin;
        this.timestamp_value = timestamp_value;
        this.offset_value = offset_value;
        this.channelName = channelName;
        this.detector = detector;
        this.comment = comment;
        this.eventTag = eventTag;
        this.omitted = omitted;
    }

    /**
     * Proxy reference to channel name associated to event
     *
     * @return reference to channel name
     */
    public String getChannelName()
    {
        return channelName;
    }

    /**
     * Proxy reference to offset value recorded for event
     *
     * @return reference to offset
     */
    public int getOffset()
    {
        return offset_value;
    }

    /**
     * Proxy reference to timestamp value recorded for event
     *
     * @return reference to timestamp
     */
    public int getTimestamp()
    {
        return timestamp_value;
    }

    /**
     * Proxy reference to comment recorded for event
     *
     * @return reference to comment
     */
    public String getComment()
    {
        return comment;
    }

    /**
     * Proxy reference to omission status for event
     *
     * @return reference to omission status
     */
    public boolean isOmitted()
    {
        return omitted;
    }

    /**
     * Proxy reference to event tag name associated to event
     *
     * @return reference to event tag name
     */
    public String getEventTag()
    {
        return eventTag;
    }

    /**
     * Proxy reference to the detector type associated to the channel
     * in which the event is associated with
     *
     * @return reference to detector type
     */
    public String getDetector()
    {
        return detector;
    }

    /**
     * Proxy reference to maxim chip value associated to the channel
     * in which the event is associated with
     *
     * @return reference to maxim chip
     */
    public int getChip()
    {
        return chip;
    }

    /**
     * Proxy reference to maxim pin value associated to the channel
     * in which the event is associated with
     *
     * @return reference to maxim pin
     */
    public int getPin()
    {
        return pin;
    }

    @Override
    public int compareTo(EventProxy o)
    {
        return timestamp_value - o.getTimestamp();
    }

    public String toString()
    {
        String str = "";
        str += "Channel: " + channelName + "(Chip " + chip + " - Pin " + pin + ")\n" +
                "Timestamp: " + VideoPlayer.convertToTimeFormat(timestamp_value) + " (" + timestamp_value + " ms)" + "\n" +
                "Offset: " + offset_value + "\n" +
                "Event Tag: " + eventTag + "\n" +
                "Comment: " + comment + "\n" +
                "Detector Type: " + eventTag + "\n" +
                "Omitted: " + (omitted ? "Yes" : "No");
        return str;
    }
}
