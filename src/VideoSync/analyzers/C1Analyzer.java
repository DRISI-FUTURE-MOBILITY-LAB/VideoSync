/*
 * ****************************************************************
 * File: 			C1Analyzer.java
 * Date Created:  	May 28, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle the analysis of the c1 data file and
 *                  prepare it for use with the graphs
 *
 * Modified			August 19, 2016
 * Programmer		Danny Hale
 *                  Added ability to analyze raw C1 data, C1 database
 *                  files, and C1 database result sets.
 *                  Added a naive fix for jumping backwards through events.
 *
 * Modified			April 2020
 * Programmer		Jenzel Arevalo
 *                  Appended C1PinFinder helper function to take in
 *                  to account of C11 pins found in the C1 file.
 *
 *                  August 2020
 *                  Moved C1PinFinder helper function to C1Channel
 *                  class to be used by constructor method of C1
 *                  Channel.
 *
 * Modified         July 2020
 * Programmer       Aleksey Zasorin
 *                  Added function to output variance lines for the C1 Viewer
 *                  graph pane. Variance lines visualize various offset detector
 *                  variance values around rising/fall edge events.
 * ****************************************************************
 */
package VideoSync.analyzers;

import VideoSync.objects.c1.C1Channel;
import VideoSync.objects.c1.C1Event;
import VideoSync.objects.c1.C1Object;
import VideoSync.objects.graphs.Line;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Vector;

// TODO: Remove duplicate code from analyzers

public class C1Analyzer
{
    /**
     * Used for keeping track of the start time for the analysis
     */
    private long sTime;

    /**
     * Used for storing all of the raw events that are detected during analysis
     */
    private Vector<C1Object> c1Objects;

    /**
     * Used for storing all the individual channel information after analysis.
     * This is what we search through when looking for events
     */
    private Vector<C1Channel> channels;

    /**
     * Used for storing all the graph lines that are to be rendered
     */
    private Vector<Line> graphLines;

    /**
     * Enum used for differentiating between the three different variance modes.
     * ALL_STATES shows variance lines for high and low states
     * HIGH_STATES shows variance lines for only high states
     * LOW_STATES shows variance lines for only low states
     */
    public enum VarianceMode
    {
        ALL_STATES,
        HIGH_STATES,
        LOW_STATES
    }

    /**
     * Constructor for creating the C1 Analyzer methods
     */
    public C1Analyzer()
    {
        // Create an array to store all of the individual events
        c1Objects = new Vector<>();

        // Create an array to store all of the individual channels
        channels = new Vector<>();
    }

    // -- C1Analyzer Analysis methods

    /**
     * Performs the analysis of the C1 file and creates all the array data for use with the graphs
     *
     * @param file File object to read C1 data from
     */
    public void performAnalysis(File file)
    {
        System.out.println("File to load for C1 Analysis: " + file.getPath());
        System.out.println("------------------------------------------------------");

        // Set the sTime to the current time
        sTime = System.currentTimeMillis();

        System.out.println("Analyzing C1 File: " + file.getName());

        // Surround everything in a try/catch block for catching any errors with reading the file
        try
        {
            // Create a buffered reader to read the contents of the C1 File
            //@SuppressWarnings("resource")
            BufferedReader fileReader = new BufferedReader(new FileReader(file.getPath()));

            //Read initial line to get header information ignoring any lines without a non-whitespace character
            String line;
            do
            {
                line = fileReader.readLine().trim();
            }
            while(line.isEmpty());

            //Load the first line to be parsed
            String c1StateRegex = "([0-9] ([0-9]{1,2}) ([01]) [0-9]*)";
            if(line.equals("data_id,C1_Chip_Num,C1_Reader_Channel,C1_Pin,C1_Reader_State,C1_Reader_Ticks,C1_Reader_Millis,C1_Reader_Unix,C1_Date"))
            {
                readC1Database(fileReader);
            }
            else if(line.startsWith("Pi"))
            {
                readC1Raw(fileReader, null);
            }
            else if(line.matches(c1StateRegex))
            {
                readC1Raw(fileReader, line);
            }
            else
            {
                System.out.println("Unknown C1 file format.");
            }
            fileReader.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        analyzeLoadedData();
    }

    /**
     * Performs the analysis of the C1 file and creates all the array data for use with the graphs
     *
     * @param rs A table of data representing a database result set containing C1 data
     */
    public void performAnalysis(ResultSet rs)
    {
        // Initialize the base time to -1.
        long baseTimeValue = -1;

        try
        {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            if(columnsNumber == 3)
            {
                while(rs.next())
                {
                    long time = rs.getLong(3);
                    // If the baseTime is -1, then we need to initialize it to the first records time.
                    // This allows us to use the first event as time 0, making the time values small and more manageable.
                    if(baseTimeValue == -1)
                        baseTimeValue = time;

                    // Adjust the time based on the baseTimeValue.
                    time = time - baseTimeValue;

                    int chip = rs.getInt(0);
                    int channel = rs.getInt(1);
                    int state = rs.getInt(2);

                    //System.out.println("Event Read: " + channel + " " + state);
                    c1Objects.add(new C1Object(time, state, chip, channel));
                }
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        analyzeLoadedData();
    }

    /**
     * Helper method to handle data analysis after performAnalysis loads in data from a C1 file or ResultSet
     */
    private void analyzeLoadedData()
    {
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

        // Loop through all of the channels and print out the number of events
        for(C1Channel c : channels)
        {
            System.out.printf("Chip %d Channel %d: %d events\n", c.getChip(), c.getChannelNumber(), c.getC1Objects().size() / 2);
        }
        System.out.println();

        System.out.println("Finished Generating C1 element records: " + (System.currentTimeMillis() - sTime) + " ms");
        System.out.println("------------------------------------------------------");
    }

    /**
     * Creates the channel arrays an initializes them for use
     */
    private void createChannelArrays()
    {
        // Loop through each event that we found to create the channel data
        for(C1Object o : c1Objects)
        {
            // Create a temporary pin to hold the objects pin number
            int chip = o.getChip();
            int pin = o.getPin();

            // Used in determining if we found a valid pin
            boolean foundChannel = false;

            // Loop through all the channel objects we have so far
            for(C1Channel c : channels)
            {
                // If that channel object exists, we change the found flag to true
                if(c.getPin() == pin && c.getChip() == chip)
                {
                    foundChannel = true;
                    break;
                }
            }

            // If the found flag is false, we didn't find that channel anywhere
            // so we need to add that channel object with the chip and pin it will represent
            if(!foundChannel)
            {
                channels.add(new C1Channel(chip, pin));
            }
        }
    }

    /**
     * Adds all of the events to the appropriate channel's event arrays
     */
    private void generateChannelData()
    {
        // Loop through each C1 event we detected
        for(C1Object o : c1Objects)
        {
            // Loop through each channel we have found
            for(C1Channel c : channels)
            {
                // If the chip and pin parameters match, we can add the event object to the channel's array
                if(c.getPin() == o.getPin() && c.getChip() == o.getChip())
                {
                    c.addC1Object(o);
                }
            }
        }

        for(C1Channel c : channels)
        {
            C1Object lastElement = c.getC1Objects().lastElement();
            if(lastElement.getState() == 1 && c.getC1Objects().size() != 1)
                c.getC1Objects().remove(lastElement);
        }

        // Groups C1Objects that form high-state "events" and adds them to their respective channel
        generateC1Events();
    }

    /**
     * Inserts an element at the beginning of the array for making the graphing easier
     */
    private void insertStartElement()
    {
        // Loop through all of the channel objects
        for(C1Channel c : channels)
        {
            // Get the Log 170 Object currently at the start
            C1Object o = c.getC1Objects().elementAt(0);

            // Create  new Log 170 Object at time 0, with the state opposite of the current start element
            C1Object newObj = new C1Object(0, (o.getState() == 0) ? 1 : 0, o.getChip(), o.getPin());

            // Insert the new element at index 0. This will move all subsequent elements by +1
            c.getC1Objects().insertElementAt(newObj, 0);
        }
    }

    // -- C1Analyzer Event Retrieval methods

    /**
     * Get the graph events to be displayed.
     *
     * @param gw       graph width in pixels
     * @param time     current time being displayed
     * @param gSeconds Graph width in seconds (From the combo box)
     * @param chip     chip associated with channel to be retrieved
     * @param pin      pin associated with channel to be retrieved
     * @param top      graph top line pixel location
     * @param bottom   graph bottom line pixel location
     * @return vector containing line objects to be drawn onto the graph
     */
    @SuppressWarnings("unchecked")
    public Vector<Line> getGraphLines(int gw, long time, double gSeconds, int chip, int pin, double top, double bottom)
    {
        // This is the number of milliseconds that are being displayed
        double difference = (gSeconds * 1000) / 2;

        // Min and Max is the range of times being displayed on the graph
        long min = (long) (time - difference);
        long max = (long) (time + difference);

        // Loop through each of the channel objects to find which one we are going to use with the graph
        boolean graphObjectsUpdated = false;
        for(C1Channel c : channels)
        {
            // If the channel we want matches the current channel, generate the graph objects for it
            if(c.getChip() == chip && c.getPin() == pin)
            {
                Vector<C1Object> c1States = c.getStates(min, time, max);
                boolean isLastData = false;
                if(c1States.size() > 0)
                {
                    int objIndex = c.getC1Objects().indexOf(c1States.firstElement());
                    isLastData = (objIndex == (c.getC1Objects().size() - 1));
                }

                //If no state data exists, get edge approximation instead so we can draw extended graph line
                if(c1States.size() == 0)
                {
                    c1States.add(c.getEdgeApproximateState(min, time, max));
                    isLastData = true;
                }

                createGraphObjects(gw, min, max, c1States, top, bottom, isLastData);
                graphObjectsUpdated = true;
            }
        }

        if(graphObjectsUpdated)
            // This cast is safe
            return (Vector<Line>) graphLines.clone();
        else
            return new Vector<>();
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
    private void createGraphObjects(double gw, long min, long max, Vector<C1Object> gObjects, double top, double bottom, boolean lastData)
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
            // Retrieve the state that is going to be drawn.
            C1Object o = gObjects.elementAt(i);

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
                // Right line is equal to left line so that display can extend graph.
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

    /**
     * Creates variance lines to be drawn around rising/falling edge state events in the C1 Viewer graph
     *
     * @param gw       Width of the graph we are drawing so we can scale everything properly
     * @param gh       Height of the graph we are drawing
     * @param time     Current time the graph is at
     * @param gSeconds Number of seconds being displayed
     * @param chip     Chip value of channel for which we're drawing the graph
     * @param pin      Pin value of channel for which we're drawing the graph
     * @param variance Time offset in milliseconds to draw variance lines around rising/falling edge state events
     * @param mode     Specifies the variance mode between showing all states, high states, or low states
     * @return Vector of Line objects of variance lines to be drawn on the graph
     */
    public Vector<Line> getVarianceLines(int gw, int gh, long time, double gSeconds, int chip, int pin, int variance, VarianceMode mode)
    {
        // This is the number of milliseconds that are being displayed
        double difference = (gSeconds * 1000) / 2;

        // Min and Max is the range of times being displayed on the graph
        long min = (long) (time - difference);
        long max = (long) (time + difference);

        Vector<Line> varianceLines = new Vector<>();

        // Loop through each of the channel objects to find which one we are going to use with the graph
        for(C1Channel c : channels)
        {
            // If the channel we want matches the current channel, generate the graph objects for it
            if(c.getChip() == chip && c.getPin() == pin)
            {
                Vector<C1Object> c1States = c.getStates(min, time, max);

                createVarianceGraphObjects(gw, gh, min, max, variance, c1States, varianceLines, mode);
            }
        }

        return varianceLines;
    }

    /**
     * Populates a given Vector of Line objects with Line objects representing variance lines to be drawn
     *
     * @param gw            Width of the graph we are drawing so we can scale everything properly
     * @param gh            Height of the graph we are drawing
     * @param min           Left edge
     * @param max           Right edge
     * @param variance      Time offset in milliseconds to draw variance lines around rising/falling edge state events
     * @param gObjects      C1 or Log170 States
     * @param varianceLines Vector to populate with Line objects representing variance lines to be drawn
     * @param mode          Specifies the variance mode between showing all states, high states, or low states
     */
    private void createVarianceGraphObjects(double gw, double gh, long min, long max, long variance, Vector<C1Object> gObjects, Vector<Line> varianceLines, VarianceMode mode)
    {
        // Used in helping figure out where the lines will be drawn at (represents milliseconds)
        long timeBeingDisplayed = max - min;

        // Loop through all of the objects to be graphed.
        // NOTE: These are listed in reverse order
        for(int i = 0; i < gObjects.size(); i++)
        {
            // Retrieve the state that is going to be drawn.
            C1Object o = gObjects.elementAt(i);

            if((mode == VarianceMode.HIGH_STATES && o.getState() == 0) || (mode == VarianceMode.LOW_STATES && o.getState() == 1))
                continue;

            // Create variables to hold the line left and right positions;
            double lineLeftPx, lineRightPx;

            // This returns the millisecond position of the line to be drawn
            double lineLeft = (o.getMilli() - min) - variance;
            double lineRight = (o.getMilli() - min) + variance;

            // Convert the lineLeft in milliseconds to pixels
            // This takes the ratio of the graph width and the time window and assigns it to a pixel value
            lineLeftPx = (gw / timeBeingDisplayed) * lineLeft;

            // This takes the ratio of the graph width and the time window and assigns it to a pixel value
            lineRightPx = (gw / timeBeingDisplayed) * lineRight;

            // Create new Line objects and add them to the array to be returned for drawing
            varianceLines.add(new Line((int) lineLeftPx, 0, (int) lineLeftPx, (int) gh));
            varianceLines.add(new Line((int) lineRightPx, 0, (int) lineRightPx, (int) gh));
        }
    }

    /**
     * Returns an array with all of the channels
     *
     * @return Vector containing all C1Channel objects
     */
    public Vector<C1Channel> getC1Channels()
    {
        return new Vector<>(channels);
    }

    /**
     * Return the maximum time value found from all of the channel elements
     *
     * @return int value representing maximum time value found in the channel elements
     */
    public int getMaxTimeInMillis()
    {
        int max = 0;

        // Loop through each channel element
        for(C1Channel c : channels)
        {
            // Retrieve the last object in the channel's event array
            C1Object o = c.getC1Objects().lastElement();

            // If the Log 170 Object's time is greater than the max found already, then update the max time.
            if(o.getMilli() > max)
            {
                max = (int) o.getMilli();
            }
        }

        return max;
    }

    // -- Event Jumping

    /**
     * Returns the next time event for a specific channel and state
     *
     * @param chip  int value representing the chip number of the channel
     * @param pin   int value representing the pin number of the channel
     * @param state int value representing the state of the channel (low/high, 0/1)
     * @return long value representing the next time value for the channel based on input
     */
    public long returnNextTimeValueForEvent(int chip, int pin, int state)
    {
        long time = 0;

        // Loop through each channel element for a match to the channel
        for(C1Channel c : channels)
        {
            // Check to make sure that the channel number matches the channel in question
            if(c.getChip() == chip && c.getPin() == pin)
            {
                // We found the correct channel. Now lets get the index and determine its state
                C1Object o = c.getC1Objects().elementAt(c.getCurrentJumpElementIndex());

                // If the object state is the same as we are looking for, then we need to jump back by two states
                // Otherwise we only need to jump back by one position
                if(o.getState() == state)
                {
                    time = c.getC1Objects().elementAt(c.getCurrentJumpElementIndex() + 2).getMilli();
                }
                else
                {
                    time = c.getC1Objects().elementAt(c.getCurrentJumpElementIndex() + 1).getMilli();
                }

                // Since we found an event, then we can break out of the loop as there is no reason to continue searching.
                break;
            }
        }

        // Return the time found after we have finished searching
        return time;
    }

    /**
     * Returns the previous time event for a specific channel and state
     *
     * @param chip  int value representing the chip number of the channel
     * @param pin   int value representing the pin number of the channel
     * @param state int value representing the state of the channel (low/high, 0/1)
     * @return long value representing the previous time value for the channel based on input
     */
    public long returnPreviousTimeValueForEvent(int chip, int pin, int state)
    {
        long time = 0;

        // Loop through each channel element for a match to the channel
        for(C1Channel c : channels)
        {
            // Check to make sure that the channel number matches the channel in question
            if(c.getChip() == chip && c.getPin() == pin)
            {
                int index;
                // We found the correct channel. Now lets get the index and determine its state
                C1Object o = c.getC1Objects().elementAt(c.getCurrentJumpElementIndex());

                // If the object state is the same as we are looking for, then we need to jump back by two states
                // Otherwise we only need to jump back by one position
                if(o.getState() == state)
                {
                    index = c.getCurrentJumpElementIndex() - 2;
                }
                else
                {
                    index = c.getCurrentJumpElementIndex() - 1;
                }
                //Ensure that we stay at the first event if we try to jump too far backward.
                if(index < 0)
                {
                    index = 1;
                }

                time = c.getC1Objects().elementAt(index).getMilli();

                // Since we found an event, then we can break out of the loop as there is no reason to continue searching.
                break;
            }
        }

        // Return the time found after we have finished searching
        return time;
    }

    //TODO: This method needs to be refactored or deleted. The channel assignment is not consistent with the channel assignment of the readC1Raw method. This method hardly gets used so deletion may be considered...
    private void readC1Database(BufferedReader fileReader)
    {
        // Initialize the base time to -1.
        long baseTimeValue = -1;
        try
        {
            String line = fileReader.readLine();

            // Read each line from the file and store it into line and continue running as long as line is not null
            while(line != null)
            {
                // Get the time parameter from the line by splitting the line around the comma.
                String separator = ",";
                long time = Long.parseLong(line.split(separator)[6]);

                // If the baseTime is -1, then we need to initialize it to the first records time.
                // This allows us to use the first event as time 0, making the time values small and more manageable.
                if(baseTimeValue == -1)
                    baseTimeValue = time;

                // Adjust the time based on the baseTimeValue.
                time = time - baseTimeValue;

                //
                int chip = Integer.parseInt(line.split(separator)[1]);
                int pin = Integer.parseInt(line.split(separator)[3]);
                int state = Integer.parseInt(line.split(separator)[4]);
                //System.out.println("Event Read: " + channel + " " + state);
                c1Objects.add(new C1Object(time, state, chip, pin));
                line = fileReader.readLine();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void readC1Raw(BufferedReader fileReader, String firstLine)
    {
        // Initialize the base time to -1.
        long baseTimeValue = -1;
        try
        {
            String line;
            if(firstLine != null)
                line = firstLine;
            else
                line = fileReader.readLine();

            while(line != null)
            {
                if(line.length() > 0)
                {
                    // Get the time parameter from the line by splitting the line around the space.
                    long time = Long.parseLong(line.split(" ")[3]);
                    // If the baseTime is -1, then we need to initialize it to the first records time.
                    // This allows us to use the first event as time 0, making the time values small and more manageable.
                    if(baseTimeValue == -1)
                        baseTimeValue = time;

                    // Adjust the time based on the baseTimeValue.
                    time = time - baseTimeValue;
                    int chip = Integer.parseInt((line.split(" ")[0]).trim());
                    int pin = Integer.parseInt(line.split(" ")[1]);
                    int state = Integer.parseInt(line.split(" ")[2]);
                    //System.out.println("Event Read: " + channel + " " + state);
                    c1Objects.add(new C1Object(time, state, chip, pin));
                }
                line = fileReader.readLine();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void generateC1Events()
    {
        for(C1Channel c : channels)
        {
            Vector<C1Object> objs = c.getC1Objects();

            for(int x = 0; x < objs.size(); )
            {
                C1Object tmp = objs.get(x);
                if(tmp.getState() == 0)
                {
                    x += 1;
                }
                else
                {
                    C1Object tmp2 = objs.get(x+1);
                    if(tmp2.getState() == 0)
                    {
                        c.addC1Event(new C1Event(tmp.getChip(), tmp.getPin(), tmp, tmp2));
                        x += 2;
                    }
                    else
                    {
                        x++;
                    }
                }
            }
        }
    }
}
