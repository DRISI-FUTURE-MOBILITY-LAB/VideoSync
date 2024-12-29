/*
 * ****************************************************************
 * File: 			L170.java
 * Date Created:  	June 19, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To read in a .vbm/.dat file generated from a
 *                  170 controller and to convert it into a format
 *                  that can be utilized by VideoSync.
 * ****************************************************************
 */

package VideoSync.analyzers;

import VideoSync.objects.graphs.Line;
import VideoSync.objects.log170.L170Channel;
import VideoSync.objects.log170.L170Object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

public class L170Analyzer
{
    /**
     * Used for indicating where the start point of the data in the records are.
     */
    final int dataStartIndex = 7;

    /**
     * Used in determining how long it takes to perform the analysis of the file.
     */
    long sTime;

    /**
     * Used for keeping track of every event that was found in the data file.
     */
    private Vector<L170Object> events;

    /**
     * Used for keeping track of every channel that was found from the events.
     */
    private Vector<L170Channel> channels;

    /**
     * Used for storing the graph lines based on the parameters sent by the calling class
     */
    private Vector<Line> graphLines;


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- L170 Construction

    /**
     * Constructs the L170 Analyzer and initializes the event and channel arrays
     */
    public L170Analyzer()
    {
        events = new Vector<>();
        channels = new Vector<>();
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- L170 Analysis Methods

    /**
     * Analyzes the sent data file so that it may be used with VideoSync
     *
     * @param file File object to perform analysis on
     *             TODO: This analysis needs to be updated to handle 170 data files with different comment headers.
     *             Errors were found when analyzing data provided by Jerry Kwong
     */
    public void performAnalysis(File file)
    {
        boolean wantFirstLine = true;
        System.out.println("------------------------------------------------------");

        sTime = System.currentTimeMillis();
        System.out.println("Loading Log 170 File: " + file.getName());

        String line = null;

        // Surround everything in a try/catch block for catching any errors with reading the file
        try
        {
            // Assign a buffered reader from the file contents
            BufferedReader fileReader = new BufferedReader(new FileReader(file.getPath()));

            boolean baseTime = true;
            int baseTimeValue = 0;

            // As long as there is a line to be read, continue reading everything
            while((line = fileReader.readLine()) != null)
            {
                System.out.println("Line: " + line);
                if(!line.equals(""))
                {
                    // Get the length of the line. This is used for extracting all the elements out
                    int maxLineLength = line.length();

                    // Make sure that the starting character is not a '<' and that the length is greater than 10.
                    // If the length is less than 10, there is no event data
                    if(line.charAt(0) != '<' && line.length() > 10 || wantFirstLine)
                    {
                        wantFirstLine = false;

                        String timeLine;
                        int addition = 0;
                        // ...Not sure what the point of this is at the moment - maybe one of the datafiles has a null character somewhere in it causing problems?
                        if(line.charAt(0) == '\0')
                        {
                            addition = 1;
                            timeLine = line.substring(3, dataStartIndex + addition);
                        }
                        else
                        {
                            timeLine = line.substring(2, dataStartIndex + addition);
                        }

                        // Get the contents of the event data
                        String dataLine = line.substring(dataStartIndex + addition, maxLineLength - 2);


                        System.out.println("dataLine: " + dataLine);

                        // Check to see if we need to calculate the offset for the time to start at 0
                        if(baseTime)
                        {
                            baseTimeValue = Integer.parseInt(timeLine, 16);
                            baseTime = false;
                        }

                        // Get the time element for the current set of events
                        int time = Integer.parseInt(timeLine, 16);
                        // Adjust the time so that it calculating from 0
                        int newTime = time - baseTimeValue;

                        // Loop through all the data elements in the array, increasing a by 3 each time
                        // because the data elements are in groups of 3
                        for(int a = 0; a < dataLine.length(); a += 3)
                        {
                            // Retrieve the record to be analyzed and split up
                            String record = dataLine.substring(a, a + 3);

                            // Convert the record into a binary string

                            // If the binary string is less than 12, we need to add leading 0's to it
                            // so we can analyze it correctly.
                            StringBuilder dataBuilder = new StringBuilder(Integer.toBinaryString(Integer.parseInt(record, 16)));
                            while(dataBuilder.length() < 12)
                            {
                                dataBuilder.insert(0, "0");
                            }
                            String data = dataBuilder.toString();

                            // Retrieve the 60th time parameter from the binary string
                            String t = data.substring(0, 6);
                            // Retrieve the state parameter from the binary string
                            String s = data.substring(6, 7);
                            // Retrieve the channel number parameter from the binary string
                            String b = data.substring(7);

                            // Convert the 60th parameter from a binary string to an integer
                            int sixty = Integer.parseInt(t, 2);
                            // Convert the state parameter from a binary string to an integer
                            int state = Integer.parseInt(s, 2);
                            // Convert the channel number parameter from binary to an integer
                            int channelNumber = Integer.parseInt(b, 2);

                            // Add a new Log 170 Object with the integer parameters
                            events.add(new L170Object(newTime, sixty, state, channelNumber));
                        }
                    }
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        catch(StringIndexOutOfBoundsException e)
        {
            System.err.println("String Index Out Of Bounds for line '" + line + "'");
            e.printStackTrace();
        }

        // Now that we have all the elements extracted, we can create the finalized data structures
        createChannelArrays();

        // Assign all of the events to individual channels for easier use with the graphing system
        generateChannelData();

        // Insert an element at time 0 that has the opposite state of the first element
        insertStartElement();

        // Sort the channel array by channel number
        Collections.sort(channels);

        // The following only prints out the counts of events for each channel
        System.out.println();
        System.out.println("Total Events By Channel");

        for(L170Channel c : channels)
        {
            System.out.printf("Channel %d: %d events\n", c.getChannelNumber(), c.getObjects().size() / 2);
        }

        System.out.println();

        System.out.println("Finished Generating element records: " + (System.currentTimeMillis() - sTime) + " ms");
        System.out.println("------------------------------------------------------");

    }

    /**
     * Creates the channel array so we can sort all of our events by channels.
     */
    private void createChannelArrays()
    {
        // Loop through each event that we found to create the channel data
        for(L170Object o : events)
        {
            // Create a temporary variable to hold the objects channel number
            int channelNumber = o.getChannelNumber();

            // Used in determining if we found a valid pin
            boolean foundChannel = false;

            // Loop through all the channel objects we have so far
            for(L170Channel c : channels)
            {
                // If that channel object exists, we change the found flag to true
                if(c.getChannelNumber() == channelNumber)
                {
                    foundChannel = true;
                    break;
                }
            }

            // If the found flag is false, we didn't find that channel number anywhere
            // so we need to add that channel object with the channel number it will represent
            if(!foundChannel)
            {
                channels.add(new L170Channel(channelNumber));
            }
        }
    }

    /**
     * Adds all of the events found in the analysis to each channel individually
     */
    private void generateChannelData()
    {
        // Loop through each Log 170 event we detected
        for(L170Object o : events)
        {
            // Loop through each channel we have found
            for(L170Channel c : channels)
            {
                // If the channel number parameter matches, we can add the event object to the channel's array
                if(c.getChannelNumber() == o.getChannelNumber())
                {
                    c.addObject(o);
                }
            }
        }
    }

    /**
     * Inserts an element at the beginning of the array for making the graphing easier
     */
    private void insertStartElement()
    {
        // Loop through all of the channel objects
        for(L170Channel c : channels)
        {
            // Get the Log 170 Object currently at the start
            L170Object o = c.getObjects().elementAt(0);

            // Create  new Log 170 Object at time 0, with the state opposite of the current start element
            L170Object newObj = new L170Object(0, 0, (o.getState() == 0) ? 1 : 0, o.getChannelNumber());

            // Insert the new element at index 0. This will move all subsequent elements by +1
            c.getObjects().insertElementAt(newObj, 0);
        }
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- L170 Graph Segment Methods

    /**
     * Get the graph events to be displayed.
     *
     * @param gw       Graph width in pixels
     * @param time     Current time being displayed
     * @param gSeconds Graph width in seconds
     * @param channel  Channel to be retrieved
     * @param top      Top Pixel Location
     * @param bottom   Bottom Pixel Location
     * @return Vector of Line objects representing graph events to be drawn
     */
    public Vector<Line> getGraphEvents(int gw, long time, double gSeconds, int channel, double top, double bottom)
    {
        // This is the number of milliseconds that are being displayed
        double difference = (gSeconds * 1000) / 2;

        // Min and Max is the range of times being displayed on the graph
        long min = (long) (time - difference);
        long max = (long) (time + difference);

        // Loop through each of the channel objects to find which one we are going to use with the graph
        for(L170Channel c : channels)
        {
            // If the channel we want matches the current channel, generate the graph objects for it
            if(c.getChannelNumber() == channel)
            {
                Vector<L170Object> l170States = c.getStates(min, time, max);
                boolean isLastData = false;

                if(l170States.size() > 0)
                {
                    int objIndex = c.getObjects().indexOf(l170States.firstElement());
                    //TODO - find reason behind need for -2 instead of -1.
                    isLastData = (objIndex == (c.getObjects().size() - 2));
                }

                //If no state data exists, get edge approximation instead so we can draw extended graph line
                if(l170States.size() == 0)
                {
                    l170States.add(c.getEdgeApproximateState(min, time, max));
                    isLastData = true;
                }

                createGraphObjects(gw, min, max, l170States, top, bottom, isLastData);
                //createGraphObjects(gw, min, time, max, c.getStates(min, time, max), top, bottom);
            }
        }

        return graphLines;
    }

    /**
     * Creates the graph line objects to be drawn
     *
     * @param gw       Width of the graph we are drawing so we can scale everything properly
     * @param min      Left edge
     * @param max      Right edge
     * @param gObjects C1 or Log170 States
     * @param top      Vertical position of the high state lines
     * @param bottom   Vertical position of the low state lines
     * @param lastData Boolean specifying whether we're dealing with the end of the C1 data
     */
    private void createGraphObjects(double gw, long min, long max, Vector<L170Object> gObjects, double top, double bottom, boolean lastData)
    {
        graphLines = new Vector<>();

        // Used in helping figure out where the lines will be drawn at (represents milliseconds)
        long timeBeingDisplayed = max - min;

        // This gets reassigned as we loop through each of the objects to be drawn.
        long previousTime = max - min;

        // Loop through all of the objects to be graphed.
        // NOTE: These are listed in reverse order
        for(int i = 0; i < gObjects.size(); i++)
        {
            // Retrieve the object that is going to be drawn.
            L170Object o = gObjects.elementAt(i);

            // Create variables to hold the line left and right positions;
            double lineLeftPx = 0.0, lineRightPx;

            // This returns the millisecond position of the line to be drawn
            double lineLeft = (o.getMilli() - min);
            // If lineLeft in pixels is less than 0, we don't need to draw anything less than that, so we reset it to 0.0
            if(lineLeft < 0.0)
            {
                lineLeft = 0.0;
            }

            // Convert the lineLeft in milliseconds to pixels
            if(lineLeft != 0.0)
            {
                // This takes the ratio of the graph width and the time window and assigns it to a pixel value
                lineLeftPx = (gw / timeBeingDisplayed) * lineLeft;
            }

            // This returns the millisecond position of the line to be drawn
            double lineRight = (previousTime * 1.0);

            if(!lastData || (gObjects.size() > 1 && i != 0))
            {
                // This takes the ratio of the graph width and the time window and assigns it to a pixel value
                lineRightPx = (gw / timeBeingDisplayed) * lineRight;
            }
            else
            {
                // Right line is equal to left line so that display can extend graph
                lineRightPx = lineLeftPx;
            }

            // Get the location in pixels of where the horizontal line is to be drawn
            // NOTE: With the java graphics api, point (0, 0) is located in the TOP LEFT of the screen, not bottom Left.
            double horizontal = (o.getState() == 0) ? top : bottom;

            // Create a new Line object and add it to the array to be return for drawing
            graphLines.add(new Line((int) lineLeftPx, (int) horizontal, (int) lineRightPx, (int) horizontal));

            // If the current index is not 1 position smaller than the array size, we need to create a vertical line for it
            if(i != gObjects.size() - 1)
            {
                // Create a new vertical line object and add it to the array
                graphLines.add(new Line((int) lineLeftPx, (int) bottom, (int) lineLeftPx, (int) top));
            }

            // Re-assign the previous time.
            previousTime = o.getMilli() - min;
        }
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- L170 Event Jumping Methods

    /**
     * Returns the next time value for the specific channel and state so that the user can jump to that location.
     *
     * @param channel int value representing the channel number
     * @param state   int value representing the state of the channel (low/high, 0/1)
     * @return long value representing the next time value for the channel based on input
     */
    public long returnNextTimeValueForEvent(int channel, int state)
    {
        long time = 0;

        // Loop through each channel element for a match to the channel
        for(L170Channel c : channels)
        {
            // Check to make sure that the channel number matches the channel in question
            if(c.getChannelNumber() == channel)
            {
                // We found the correct channel. Now lets get the index and determine its state
                L170Object o = c.getObjects().elementAt(c.getCurrentJumpElementIndex());

                if(o.getState() == state)
                {
                    time = c.getObjects().elementAt(c.getCurrentJumpElementIndex() + 2).getMilli();
                }
                else
                {
                    time = c.getObjects().elementAt(c.getCurrentJumpElementIndex() + 1).getMilli();
                }
            }
        }

        return time;
    }

    /**
     * Returns the previous time value for the specific channel and state so that the user can jump to that location.
     *
     * @param channel int value representing the channel number
     * @param state   int value representing the state of the channel (low/high, 0/1)
     * @return long value representing the previous time value for the channel based on input
     */
    public long returnPreviousTimeValueForEvent(int channel, int state)
    {
        long time = 0;

        // Loop through each channel element for a match to the channel
        for(L170Channel c : channels)
        {
            // Check to make sure that the channel number matches the channel in question
            if(c.getChannelNumber() == channel)
            {
                // We found the correct channel. Now lets get the index and determine its state
                L170Object o = c.getObjects().elementAt(c.getCurrentJumpElementIndex());

                if(o.getState() == state)
                {
                    time = c.getObjects().elementAt((c.getCurrentJumpElementIndex() > 2) ? c.getCurrentJumpElementIndex() - 2 : 0).getMilli();
                }
                else
                {
                    time = c.getObjects().elementAt((c.getCurrentJumpElementIndex() > 1) ? c.getCurrentJumpElementIndex() - 1 : 0).getMilli();
                }
            }
        }

        return time;
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- L170 Getter Methods

    /**
     * Returns an array with all of the channel numbers
     *
     * @return Vector of L170Channel objects
     */
    public Vector<L170Channel> getChannels()
    {
        // Create an integer array to store the channel numbers
        Vector<L170Channel> numbers = new Vector<>();

        // Loop through all of the channel objects we have
        for(int i = 0; i < channels.size(); i++)
        {
            // assign the channel number to the correct index position for use
            numbers.add(channels.elementAt(i));
        }
        return numbers;
    }

    /**
     * Return the maximum time value found from all of the channel elements
     *
     * @return int representing time in milliseconds
     */
    public int getMaxTimeInMillis()
    {
        int max = 0;

        // Loop through each channel element
        for(L170Channel c : channels)
        {
            // Retrieve the last object in the channel's event array
            L170Object o = c.getObjects().lastElement();

            // If the Log 170 Object's time is greater than the max found already,
            // Update the max time.
            if(o.getMilli() > max)
            {
                max = o.getMilli();
            }
        }

        return max;
    }
}