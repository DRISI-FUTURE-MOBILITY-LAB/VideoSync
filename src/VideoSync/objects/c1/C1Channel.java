package VideoSync.objects.c1;

import VideoSync.objects.Pair;

import java.util.Vector;

public class C1Channel implements Comparable<C1Channel>
{
    /**
     * Used for keeping track of the pin number
     */
    private final int pin;

    /**
     * Used for keeping track of the channel number
     */
    private final int channelNumber;

    /**
     * Used for keeping track of the chip number
     */
    private final int chip;

    /**
     * Used for keeping track of the current element so we can easily jump forwards
     * or backwards to the event were looking for
     */
    private int currentJumpElementIndex;

    /**
     * Stores all of the elements for the specific channel in chronological order
     */
    private final Vector<C1Object> c1Objects;

    /**
     * Returns a subset of the states that are to be graphed
     */
    private Vector<C1Object> graphC1Objects;

    private Vector<C1Event> c1EventObjects;

    private Vector<C1Event> graphC1EventObjects;

    // -- C1Channel Constructor

    /**
     * Construct a new C1 Channel Object with a specific pin number
     *
     * @param chip chip associated with channel
     * @param pin  pin associated with channel
     */
    public C1Channel(int chip, int pin)
    {
        this.pin = pin;
        this.chip = chip;
        this.channelNumber = c1PinFinder(chip, pin);
        c1Objects = new Vector<>();
        c1EventObjects = new Vector<>();
    }

    // -- C1Channel Getter's and Setters

    /**
     * Returns this objects pin number
     *
     * @return pin number associated with channel
     */
    public int getPin()
    {
        return this.pin;
    }

    /**
     * Return this objects chip number
     *
     * @return pin number associated with channel
     */
    public int getChip()
    {
        return this.chip;
    }

    public Pair<Integer, Integer> getChipPinPair()
    {
        return new Pair<>(chip, pin);
    }

    /**
     * Returns this objects channel number
     *
     * @return this objects channel number
     */
    public int getChannelNumber()
    {
        return this.channelNumber;
    }

    /**
     * Return the array with all of this channel's objects
     *
     * @return collection of C1Objects
     */
    public Vector<C1Object> getC1Objects()
    {
        return this.c1Objects;
    }

    /**
     * Add a C1 Object to the objects array
     *
     * @param object C1Object to add to list of objects
     */
    public void addC1Object(C1Object object)
    {
        c1Objects.add(object);
    }

    public void addC1Event(C1Event event)
    {
        c1EventObjects.add(event);
    }

    // -- C1 Object Comparison

    /**
     * Compares the current object with another one
     */
    public int compareTo(C1Channel two)
    {
        return this.channelNumber - two.getChannelNumber();
    }

    // -- C1 Event Retrieval Methods

    /**
     * Returns all of the states between the min and max time, centered around the current time.
     *
     * @param minTime     earliest time to fetch state information from
     * @param currentTime current time of the video/graphs
     * @param maxTime     latest time to fetch state information from
     * @return collection of C1Objects from between minTime and maxTime
     */
    public Vector<C1Object> getStates(long minTime, long currentTime, long maxTime)
    {
        // Assign/Re-assign the graph objects for storing the objects to be graphed
        graphC1Objects = new Vector<>();

        // Start at the beginning of the array and work towards the end searching for any objects
        for(int i = 0; i < c1Objects.size(); i++)
        {
            // Assign a C1 object to be checked against
            C1Object o1 = c1Objects.elementAt(i);

            // Check to see if right is less than the object
            // If we reach the end, use all elements to avoid graph dropping off at end of data.
            // Still need to do search to keep performance up.
            // TODO: These conditions are super confusing...should be cleaned up
            if(maxTime < o1.getMilli() || (i == c1Objects.size() - 1))
            {
                if(i == c1Objects.size() - 1 && o1.getMilli() > minTime)
                {
                    graphC1Objects.add(o1);
                }

                // As long as we are not looking at the element at index 0, we can find all previous elements
                if(i > 0 && o1.getMilli() > minTime)
                {
                    // Get all previous elements starting at index i, and ones that are greater than left
                    getPreviousElements(i, currentTime, minTime);
                }

                break;
            }
        }

        return graphC1Objects;
    }

    public Vector<C1Event> getEvents(long minTime, long maxTime)
    {
        // Assign/Re-assign the graph objects for storing the objects to be graphed
        graphC1EventObjects = new Vector<>();

        for(C1Event e : c1EventObjects)
        {
            if(e.getStartTime() <= maxTime && e.getEndTime() >= minTime)
            {
                graphC1EventObjects.add(e);
            }
        }

        return graphC1EventObjects;
    }

    public Vector<C1Event> getEventsByCount(long centerTime, int n)
    {
        Vector<C1Event> collectedEvents = new Vector<>();

        // Find event closest to centerTime
        C1Event closest = c1EventObjects.firstElement();
        for(C1Event e : c1EventObjects)
        {
            if(Math.abs(e.getHalfwayTime() - centerTime) < Math.abs(closest.getHalfwayTime() - centerTime))
            {
                closest = e;
            }
        }

        int objIdx = c1EventObjects.indexOf(closest);

        for(int i = Math.max(objIdx - n, 0); i <= Math.min(objIdx + n, c1EventObjects.size()-1); i++)
        {
            collectedEvents.add(c1EventObjects.get(i));
        }

        return collectedEvents;
    }

    /**
     * Creates a state with minTime or maxTime as its timestamp from either the first or last states, dependent on which
     * is closer to currentTime. Used for extending the graph past data start/end.
     *
     * @param minTime     graph left time
     * @param currentTime time to compare first and last states against
     * @param maxTime     graph right time
     * @return approximate C1Object that extends data before or after real data
     */
    public C1Object getEdgeApproximateState(long minTime, long currentTime, long maxTime)
    {
        //Get the first and last C1 states and figure out which is closer to the current timestamp
        //This assumes that the objects vector is already sorted by timestamp
        C1Object first = c1Objects.firstElement();
        C1Object last = c1Objects.lastElement();
        C1Object edgeApproximation;
        if((currentTime - first.getMilli()) < (last.getMilli() - currentTime))
        {
            //First object is closest to timestamp
            edgeApproximation = new C1Object(maxTime, first.getState(), first.getChip(), first.getPin());
        }
        else
        {
            edgeApproximation = new C1Object(minTime, last.getState(), last.getChip(), last.getPin());
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
                // Assign a C1 object from the selected index
                C1Object o = c1Objects.elementAt(newIndex);

                // Add the C1 object to the graphObjects array
                graphC1Objects.add(o);

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
        return currentJumpElementIndex;
    }

    /**
     * Get channel number based on chip and pin number
     *
     * @param chip     chip associated with channel
     * @param pin      maxim pin associated with channel
     * @return channel number associated with given chip and pin
     */
    private int c1PinFinder(int chip, int pin)
    {
        int channel = -1;

        switch(chip)
        {
            case 1:
                if(pin > 3 && pin < 16)
                {
                    channel = pin - 2;
                }
                else if(pin > 15 && pin < 28)
                {
                    channel = pin - 1;
                }
                break;
            case 2:
                if(pin > 3 && pin < 28)
                {
                    channel = pin + 23;
                }
                break;
            case 3:
                if(pin > 3 && pin < 28)
                {
                    channel = pin + 47;
                }
                break;
            case 4:
                if(pin > 3 && pin < 21)
                {
                    channel = pin + 71;
                }
                else if(pin > 20 && pin < 28)
                {
                    channel = pin + 72;
                }
                break;
            case 5:
                if(pin > 3 && pin < 8)
                {
                    channel = pin + 96;
                }

                //Pins routed to C11 sockets
                else if(pin > 9 && pin < 14 || pin > 14 && pin < 17 || pin > 17 && pin < 23)
                {
                    channel = pin;
                }
                else if(pin == 23)
                {
                    channel = pin - 6;
                }
                break;
        }

        return channel;
    }
}
