/*
 * ****************************************************************
 * File: 			L170Object.java
 * Date Created:  	June 18, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To keep track of each individual Log 170
 * 					Channel Object
 *
 * ****************************************************************
 */
package VideoSync.objects.log170;

public class L170Object
{
    /**
     * Used to keep track of the millisecond value for the event
     */
    private final int millis;

    /**
     * Used to keep track of the sixtieth value for the event
     */
    private final int sixty;

    /**
     * Used to keep track of the state
     */
    private final int state;

    /**
     * Used to keep track of the channel number
     */
    private final int channelNumber;

    // -- L170Object Constructor

    /**
     * Constructs the new Log 170 Events object
     *
     * @param time          time sent in seconds
     * @param sixty         the number of sixtieth for the event
     * @param state         the state of the event
     * @param channelNumber the channel number of the event
     */
    public L170Object(int time, int sixty, int state, int channelNumber)
    {
        // Convert the seconds to milliseconds, and convert and the sixtieth to milliseconds
        this.millis = (int) (((float) time * 1000) + ((float) sixty / (float) 60) * 1000);

        // Store the total number of sixtieths for the event
        this.sixty = (time * 60) + sixty;

        // Store the state of the event
        this.state = state;

        // Store the channel number of the event
        this.channelNumber = channelNumber;
    }

    // -- L170Object Getters & Setters

    /**
     * Returns the milliseconds value of the event.
     *
     * @return millisecond value of the vent
     */
    public int getMilli()
    {
        return this.millis;
    }

    /**
     * Returns the state of the event.
     *
     * @return 0 or 1 representing the state of the event
     */
    public int getState()
    {
        return this.state;
    }

    /**
     * Returns the channel number of the event.
     *
     * @return channel number of the event
     */
    public int getChannelNumber()
    {
        return this.channelNumber;
    }

    /**
     * Returns a string with the format of the message to be written to the log file
     */
    public String toString()
    {
        return String.format("Channel Number %d: -- Sixty: %d -- Milli: %d -- State: %d", this.channelNumber, this.sixty, this.millis, this.state);
    }
}