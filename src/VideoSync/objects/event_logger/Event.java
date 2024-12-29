/*
 * File: Event.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class to hold essential attributes that
 *          make up an event
 */

package VideoSync.objects.event_logger;

public class Event implements Comparable<Event>
{

    /**
     * Playback offset value in which event occurred
     */
    private int offset_value;

    /**
     * Playback timestamp in ms in which event occurred
     */
    private int timestamp_value;

    /**
     * Comment string associated with event
     */
    private String comment;

    /**
     * Flag to identify if event is ommitted from calculations
     */
    private boolean omitted;

    public Event(int timestamp_value, int offset_value, String comment, boolean omitted)
    {
        this.timestamp_value = timestamp_value;
        this.offset_value = offset_value;
        this.comment = comment;
        this.omitted = omitted;
    }

    public Event(int timestamp_value, int offset_value)
    {
        this.timestamp_value = timestamp_value;
        this.offset_value = offset_value;
        this.omitted = false;
    }

    /**
     * Sets the offset value of the event
     *
     * @param offset_value offset value of video/graph
     */
    public void setOffset(int offset_value)
    {
        this.offset_value = offset_value;
    }

    /**
     * Sets the timestamp value of the event
     *
     * @param timestamp_value playback timestamp
     */
    public void setTimestamp(int timestamp_value)
    {
        this.timestamp_value = timestamp_value;
    }

    /**
     * Sets the comment of the event
     *
     * @param comment comment associated with event
     */
    public void setComment(String comment)
    {
        this.comment = comment;
    }

    /**
     * Sets the omission flag of the event
     *
     * @param omitted flag indicating if event is omitted from calculations
     */
    public void setOmitted(boolean omitted)
    {
        this.omitted = omitted;
    }

    /**
     * Returns the offset value
     *
     * @return offset_value
     */
    public int getOffset()
    {
        return offset_value;
    }

    /**
     * Returns the timestamp value
     *
     * @return timestamp_value
     */
    public int getTimestamp()
    {
        return timestamp_value;
    }

    /**
     * Returns the comment
     *
     * @return comment
     */
    public String getComment()
    {
        return comment;
    }

    /**
     * Returns the omission flag
     *
     * @return omitted
     */
    public boolean isOmitted()
    {
        return omitted;
    }

    //TODO: Is this a valid compareTo implementation?
    @Override
    public int compareTo(Event o)
    {
        return timestamp_value - o.getTimestamp();
    }

    public String toString()
    {
        String str = "";
        str += "Timestamp (ms): " + timestamp_value + "\n";
        str += "Offset: " + offset_value + "\n";
        str += "Comment:\n" + comment + "\n";
        str += "Ommitted: " + (omitted ? "Yes" : "No");
        return str;
    }

    public boolean equals(Event event)
    {
        return (timestamp_value == event.getTimestamp() && offset_value == event.getOffset() &&
                comment.equals(event.getComment()) && omitted == event.isOmitted());
    }
}
