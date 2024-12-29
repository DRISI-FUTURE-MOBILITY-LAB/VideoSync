/*
 * ****************************************************************
 * File: 			L170Channel.java
 * Date Created:  	June 18, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			Used for keeping track of all the Log 170
 * 					Channel events so they can be easily sorted
 * 					and searched for the appropriate events
 *
 * ****************************************************************
 */
package VideoSync.objects.log170;

import java.util.Vector;

public class L170Channel implements Comparable<L170Channel>
{
    /**
     * Used for keeping track of the channel number
     */
    private final int channelNumber;

    /**
     * Used for keeping track of the current element so we can easily jump forwards
     * or backwards to the event were looking for
     */
    private int currentJumpElementIndex;

    /**
     * Stores all of the elements for the specific channel in chronological order
     */
    private final Vector<L170Object> objects;

    /**
     * Stores a subset of the states that are to be graphed
     */
    private Vector<L170Object> graphObjects;

    // -- L170Channel Constructor

    /**
     * Construct a new Log 170 Channel Object with a specific channel number
     *
     * @param channelNumber channel number associated with Log 170 channel
     */
    public L170Channel(int channelNumber)
    {
        this.channelNumber = channelNumber;
        objects = new Vector<>();
    }

    // -- L170Channel Getter's and Setters

    /**
     * Returns this objects channel number
     *
     * @return channel number associated with Log 170 channel
     */
    public int getChannelNumber()
    {
        return this.channelNumber;
    }

    /**
     * Return the array with all of this channel's objects
     *
     * @return collection of L170Objects
     */
    public Vector<L170Object> getObjects()
    {
        return this.objects;
    }

    /**
     * Add a 170 Object to the objects array
     *
     * @param object object to add to collection of L170Objects
     */
    public void addObject(L170Object object)
    {
        objects.add(object);
    }

    // -- L170Channel Object Comparison

    /**
     * Compares the current object with another one
     *
     * @param two object to compare to
     */
    // FIXME: This is a really bad compareTo() method
    public int compareTo(L170Channel two)
    {
        return this.channelNumber - two.getChannelNumber();
    }

    // -- L170Channel Event Retrieval Methods

    /**
     * Returns all of the states between the min and max time, centered around the current time.
     *
     * @param minTime     earliest time to fetch state information from
     * @param currentTime current time of the video/graphs
     * @param maxTime     latest time to fetch state information from
     * @return collection of L170Objects from between minTime and maxTime
     */
    public Vector<L170Object> getStates(long minTime, long currentTime, long maxTime)
    {
        // Assign/Re-assign the graph objects for storing the objects to be graphed
        graphObjects = new Vector<>();

        // Start at the beginning of the array and work towards the end searching for any objects
        for(int i = 0; i < objects.size(); i++)
        {
            // Assign a 170 object to be checked against
            L170Object o1 = objects.elementAt(i);

            // Check to see if right is less than the object
            // If we reach the end, use all elements to avoid graph dropping off at end of data.
            // Still need to do search to keep performance up.
            if(maxTime < o1.getMilli() || (i == objects.size() - 1))
            {
                // As long as we are not looking at the element at index 0, we can find all previous elements
                if(i > 0)
                {
                    // Get all previous elements starting at index i, and ones that are greater than left
                    getPreviousElements(i, currentTime, minTime);
                }

                break;
            }
        }

        return graphObjects;
    }

    /**
     * Creates a state with minTime or maxTime as its timestamp from either the first or last states, dependent on which
     * is closer to currentTime. Used for extending the graph past data start/end.
     *
     * @param minTime     Graph left time
     * @param currentTime Time to compare first and last states against
     * @param maxTime     Graph right time
     * @return approximate L170Object that extends data before or after real data
     */
    public L170Object getEdgeApproximateState(long minTime, long currentTime, long maxTime)
    {
        //Get the first and last C1 states and figure out which is closer to the current timestamp
        //This assumes that the objects vector is already sorted by timestamp
        L170Object first = objects.firstElement();
        L170Object last = objects.lastElement();
        L170Object edgeApproximation;
        if((currentTime - first.getMilli()) < (last.getMilli() - currentTime))
        {
            //First object is closest to timestamp
            edgeApproximation = new L170Object((int) maxTime, 0, first.getState(), first.getChannelNumber());
        }
        else
        {
            edgeApproximation = new L170Object((int) minTime, 0, last.getState(), last.getChannelNumber());
        }

        return edgeApproximation;
    }

    /**
     * Get all previous elements starting from the index, and that are greater than left
     *
     * @param index       index to start at when looking for previous elements
     * @param currentTime current time of the video/graphs
     * @param left        earliest time to fetch state information from
     */
    private void getPreviousElements(int index, long currentTime, long left)
    {
        // The counter is used to help determine what index value to look at.
        int counter = 1;
        boolean done = false;

        // This tells us we need to set the current jump-to index.
        boolean setJumpIndex = true;

        while(!done)
        {
            // Get the new index value to use
            int newIndex = index - counter;

            // If the new index value is greater than 0, retrieve the element at that position
            if(newIndex >= 0)
            {
                // Assign a 170 object from the selected index
                L170Object o = objects.elementAt(newIndex);

                // Add the 170 object to the graphObjects array
                graphObjects.add(o);

                if(setJumpIndex && o.getMilli() <= currentTime)
                {
                    currentJumpElementIndex = newIndex;
                    setJumpIndex = false;
                }

                // If the current object is less than left, we have finished searching through the array.
                if(o.getMilli() < left)
                {
                    done = true;
                }
            }
            // If we reach this else there is nothing left to check, so we are done
            else
            {
                done = true;
            }

            // Increment the counter for use with the next index
            counter++;
        }
    }

    /**
     * Returns the currentJumpElementIndex
     *
     * @return current jump element index
     */
    public int getCurrentJumpElementIndex()
    {
        return this.currentJumpElementIndex;
    }
}
