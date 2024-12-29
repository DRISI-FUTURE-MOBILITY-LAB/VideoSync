/*
 * ****************************************************************
 * File: 			VBM.java
 * Date Created:  	August 1, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			This analyzes an existing VBM file that was
 *                  created using the previous non-java version of
 *                  VideoSync
 *
 * ****************************************************************
 */
package VideoSync.analyzers;

import VideoSync.objects.DeviceInputMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class VBM
{
    /**
     * Used for keeping track of the file that will generate the new input map data.
     */
    private File vbmFile;

    /**
     * Used for storing the input map data so that it is compatible with the current version.
     */
    private Vector<DeviceInputMap> l170InputMap = new Vector<>();

    /**
     * Creates the VBM Object with the old mapping file so it can be converted.
     *
     * @param vbmFile File object representing VBM file
     */
    public VBM(File vbmFile)
    {
        this.vbmFile = vbmFile;
    }

    /**
     * Analyzes the old input mapping file and extracts the important information needed of use with VideoSync
     *
     * @return Vector of DeviceInputMap objects
     */
    public Vector<DeviceInputMap> analyze()
    {
        try
        {
            // Create a BufferedReader to read the contents of the vbmFile.
            BufferedReader fileReader = new BufferedReader(new FileReader(vbmFile.getPath()));

            // Used for reading in each line from the file
            String line;

            // Used for recording the entire file contents into a string.
            StringBuilder originalData = new StringBuilder();

            // As long as we can read in a line from the mapping file,
            // continue adding it to the original Data
            while((line = fileReader.readLine()) != null)
            {
                originalData.append(line).append("\n");
            }

            // Split the original data into individual lines
            String[] parts = originalData.toString().split("\n");

            // Indicates if we want to continue analyzing the data
            boolean analyze = false;

            // Stores the channel number we are working with
            int channel = -1;

            // Stores the channel name we are working with
            String name = null;

            // Loop through each of the strings so we can analyze it.
            for(String s : parts)
            {
                // If the string contains a '</' in it, that indicates the end of the channel information.
                if(s.startsWith("</"))
                {
                    // As long as the name is not blank, its length is greater than 0 and the channel is not -1
                    // we can add it to the input mapping data
                    if(channel != -1 && name != null && !name.equals("") && name.length() > 0)
                    {
                        l170InputMap.add(new DeviceInputMap(-1, -1, channel, name));
                    }

                    // Indicate we don't want to analyze anything at this point.
                    analyze = false;
                }
                // If we see that the starting character is a '<'
                // we want to go ahead and start analyzing the data because it pertains to channel information.
                else if(s.startsWith("<"))
                {
                    // Set the analyze flag to true
                    analyze = true;

                    // Extract the channel number from the line data
                    channel = Integer.parseInt((String) s.subSequence(1, s.length() - 1));
                }
                else
                {
                    // If the analyze flag is true, we want to extract the name and set the analyze flag to false
                    // since there is no other data we care about in this file.
                    if(analyze)
                    {
                        name = s;
                        analyze = false;
                    }
                }
            }

            // Close the FileReader since were now finished with it.
            fileReader.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        // Return the array containing the data we found.s
        return l170InputMap;
    }
}