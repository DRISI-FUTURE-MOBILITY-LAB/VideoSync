/*
 * ****************************************************************
 * File: 			DataModelProxy.java
 * Date Created:  	June 13, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To act as an interface to the Data Model and
 * 					not allow direct access to the Data Model
 * 					components.
 *
 * TODO: Note, this is not an ideal setup and should probably be
 * 		 switched over to a extension of the Data Model rather than
 * 		 its current implementation
 * Modified			August 24, 2016
 * 					Added the ability to get the mapping file.
 *
 * Modified			December 2018
 * Programmer		Jenzel Arevalo
 * 					Added the method signatures used for Event Logger
 * ****************************************************************
 */
package VideoSync.models;

import VideoSync.analyzers.C1Analyzer;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.EDeviceType;
import VideoSync.objects.InputMappingFile;
import VideoSync.objects.c1.C1Channel;
import VideoSync.objects.event_logger.ChannelCount;
import VideoSync.objects.graphs.Line;
import VideoSync.objects.graphs.Region;
import VideoSync.views.videos.VideoPlayer;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

// TODO: Singleton pattern for DataModel?

public class DataModelProxy
{
    /**
     * Reference to the master Data Model
     */
    private final DataModel dm;

    // -- Data Model Proxy Construction

    /**
     * Construct a Data Model Proxy with a reference to the Data Model
     *
     * @param dm Instance of the DataModel object
     */
    public DataModelProxy(DataModel dm)
    {
        // Set the DataModelProxy object
        this.dm = dm;
    }

    // -- Data Model Proxy Setters

    /**
     * Set the graph offset amount so we can sync between the video and graphs.
     */
    public void setGraphOffset(int offset)
    {
        dm.setGraphOffset(offset);
    }

    /**
     * Sets the current graph width index
     *
     * @param index Index of dropdown menu representing the graph width choice
     */
    public void setGraphWidthIndex(int index)
    {
        dm.setGraphWidthIndex(index);
    }

    /**
     * Increase the graph offset amount so we can sync between the video and graphs
     */
    public void increaseGraphOffset(int offset)
    {
        dm.increaseGraphOffset(offset);
    }

    /**
     * Notifies the data model to skip the data by a certain amount.
     *
     * @param amount Integer amount to skip video by
     */
    public void skipVideo(int amount)
    {
        this.dm.skipVideo(amount);
    }

    /**
     * Removes a video player from the data model.
     *
     * @param vp Video player to remove from the current session
     */
    public void unregisterVideo(VideoPlayer vp)
    {
        this.dm.unregisterVideo(vp);
    }

    /**
     * Update the input mapping data from the Input Mapping View
     *
     * @param device - The Device Type we are going to update.
     * @param data   - The mapping data to store
     */
    public void updateInputMapForDevice(EDeviceType device, Vector<DeviceInputMap> data)
    {
        switch(device)
        {
            case DEVICE_C1:
                dm.updateC1InputMap(data);
                break;
            case DEVICE_LOG170:
                dm.update170InputMap(data);
                break;
            case DEVICE_C1_MAXIM:
                dm.updateMaximInputMap(data);
                break;
        }
    }

    /**
     * Set the playback rate of the data
     *
     * @param rate The playback rate for the graph and the video(s)
     */
    public void setPlaybackRate(float rate)
    {
        this.dm.setPlaybackRate(rate);
    }

    /**
     * Set playback rate slider position
     *
     * @param x Integer representing the position to place the slider
     */
    public void setPlaybackRateSliderPosition(int x)
    {
        dm.setPlaybackRateSliderPosition(x);
    }

    /**
     * Returns the current position of the playback rate slider
     *
     * @return Current position of playback rate slider position
     */
    public int getPlaybackRateSliderPosition()
    {
        return dm.getPlaybackRateSliderPosition();
    }

    /**
     * Set the current position
     *
     * @param time Long value representing the current position of the graph data and video
     */
    public void setCurrentTime(long time)
    {
        dm.setCurrentPosition(time, false);
    }

    /**
     * Set the current position based on a slider's value and update video players
     *
     * @param position Integer value representing the position of the slider to set it to
     */
    public void setSliderPosition(int position)
    {
        this.dm.setCurrentPosition(position, true);
    }

    /**
     * Plays or Pauses the video based on the parameter value.
     *
     * @param isPlaying Boolean value representing whether to play or stop data/video
     */
    public void setPlaying(boolean isPlaying)
    {
        dm.setPlaying(isPlaying);
    }

    /**
     * Tell the Data Model Proxy to jump to a specific event
     *
     * @param device - The device to jump with
     * @param event  - The event within the device
     * @param chip   - The chip associated to the channel to jump with
     * @param pin    - The pin associated to the channel to jump with
     * @param state  - The state of the event we want
     */
    public void jumpToEvent(EDeviceType device, int event, int chip, int pin, int state)
    {
        this.dm.jumpToEvent(device, event, chip, pin, state);
    }

    // -- Data Model Proxy Getters

    /**
     * Used to indicate if data was loaded into the Data model
     *
     * @return Boolean value representing whether data was loaded or not
     */
    public boolean dataLoaded()
    {
        return this.dm.isDataLoaded();
    }

    /**
     * Gets the graph offset used to sync the videos and graphs.
     *
     * @return Integer value representing the graph offset
     */
    public int getGraphOffset()
    {
        return dm.getGraphOffset();
    }

    /**
     * Gets the current graph width index
     *
     * @return Index of dropdown menu representing the graph width choice
     */
    public int getGraphWidthIndex()
    {
        return dm.getGraphWidthIndex();
    }

    /**
     * Return the value of the graph width in seconds
     *
     * @return The current graph width in seconds
     */
    public double getGraphWindowSeconds()
    {
        return this.dm.getSeconds();
    }

    /**
     * Return the data for a channel to be displayed in the graph.
     *
     * @param device EDeviceType object to determine what analyzer to use (C1Analyzer or C1Maxim)
     * @param width  Graph width in pixels
     * @param base   Graph bottom line pixel location
     * @param height Graph top line pixel location
     * @return Vector containing the Line objects to be drawn onto the graph
     */
    public Vector<Line> getDataForChannel(EDeviceType device, int chip, int pin, int width, int base, int height)
    {
        return this.dm.getStateDataForDevice(device, chip, pin, width, base, height);
    }

    /**
     * Returns variance data for a specific device for graphing
     *
     * @param width        Graph width in pixels
     * @param height       Graph top line pixel location
     * @param chip         Chip associated with channel to be retrieved
     * @param pin          Pin associated with channel to be retrieved
     * @param variance     Variance to represent around events in milliseconds
     * @param varianceMode Which events to draw variance lines around
     * @return Vector containing Line objects to be drawn onto the graph
     */
    public Vector<Line> getVarianceDataForC1Viewer(int width, int height, int chip, int pin, int variance, C1Analyzer.VarianceMode varianceMode)
    {
        return this.dm.getVarianceDataForC1Viewer(width, height, chip, pin, variance, varianceMode);
    }

    public Vector<C1Channel> getC1AnalyzerChannels()
    {
        return this.dm.getC1AnalyzerChannels();
    }

    /**
     * Return the value of if we are currently playing the data
     *
     * @return Boolean value representing whether data/video is currently playing
     */
    public boolean isPlaying()
    {
        return this.dm.isPlaying();
    }

    /**
     * Return the current position of the data
     *
     * @return Current position of the graph data and the video(s)
     */
    public long getCurrentPosition()
    {
        return this.dm.getCurrentPosition();
    }

    /**
     * Return the max value to set the slider to.
     *
     * @return Max video length
     */
    public long getSliderMax()
    {
        return this.dm.getMaxVideoLength();
    }

    /**
     * Return the input mapping of the C1 Data
     *
     * @return Mapping for C1 channels
     */
    public List<DeviceInputMap> getC1InputMap()
    {
        return dm.getC1InputMap();
    }

    /**
     * Return the input map based on the the type of device we are looking at
     *
     * @param device EDeviceType specifying C1, C1Maxim, or Log170
     * @return Mapping for C1 channels
     */
    public Vector<DeviceInputMap> getInputMapForDevice(EDeviceType device)
    {
        if(device == EDeviceType.DEVICE_C1)
        {
            return this.dm.getC1InputMap();
        }
        else if(device == EDeviceType.DEVICE_C1_MAXIM)
        {
            return this.dm.getMaximInputMap();
        }
        else
        {
            return this.dm.get170InputMap();
        }
    }

    /**
     * Return the channel chip number for the name used in the selection windows.
     *
     * NOTE: This should be updated to include additional devices when they are added in
     *
     * @param deviceType EDeviceType specifying C1 or C1Maxim
     * @param name       Channel name
     * @return Chip number for channel
     */
    public int getChannelChipNumberFromName(EDeviceType deviceType, String name)
    {
        return this.dm.getChannelChipNumberFromName(deviceType, name);
    }

    /**
     * Return the channel pin number for the name used in the selection windows.
     *
     * NOTE: This should be updated to include additional devices when they are added in
     *
     * @param deviceType EDeviceType specifying C1 or C1Maxim
     * @param name       Channel name
     * @return Pin number for channel
     */
    public int getChannelPinNumberFromName(EDeviceType deviceType, String name)
    {
        return this.dm.getChannelPinNumberFromName(deviceType, name);
    }

    /**
     * Returns an individual File in the indexed input mapping files array.
     *
     * @return individual mapping file
     */
    public InputMappingFile getInputMappingFile(String index)
    {
        return dm.getInputMappingFile(index);
    }

    /**
     * Returns the files for the state data. Includes both C1 and Log170 if both are present.
     *
     * @return vector containing File objects of both C1 and Log170 data if present
     */
    public Vector<File> getDataFiles()
    {
        return dm.getDataFiles();
    }

    /**
     * Returns a vector containing all input mapping files.
     *
     * @return vector of InputMappingFile objects
     */
    public Vector<InputMappingFile> getInputMappingFiles()
    {
        return dm.getInputMappingFiles();
    }

    /**
     * Returns a vector containing all loaded video files.
     *
     * @return vector of video files
     */
    public Vector<File> getVideoFiles()
    {
        return dm.getVideoFiles();
    }

    /**
     * Returns the config file
     *
     * @return config file
     */
    public File getConfigFile()
    {
        return dm.getConfigFile();
    }

    /**
     * Returns whether or not VideoSync is running standalone or was started by another utility (ex. Reporter)
     *
     * @return True if VideoSync is running standalone, false if it was started another utility
     */
    public boolean getStandaloneInstance()
    {
        return dm.getStandaloneInstance();
    }

    /**
     * Replaces any reference to oldRegion in the graph panels with newRegion.
     *
     * @param oldRegion Video region reference we're replacing
     * @param newRegion Video region reference that's replacing the oldRegion.
     */
    public void replaceGraphVideoRegion(Region oldRegion, Region newRegion)
    {
        dm.replaceGraphVideoRegion(oldRegion, newRegion);
    }

    /**
     * Loops through all video players in DataModel and sets their isSaved flag to true.
     */
    public void setVideoPlayersSaved()
    {
        dm.setVideoPlayersSaved();
    }

    /**
     * Get the Color of the GraphPanel with the corresponding chip and pin number
     *
     * @param chip chip associated with channel
     * @param pin  pin associated with channel
     * @return Color object representing the color of the GraphPanel
     */
    public Color getGraphPanelColor(int chip, int pin)
    {
        return dm.getGraphPanelColor(chip, pin);
    }

    /**
     * Returns a list of all devices we have loaded data for. Used for populating drop boxes.
     *
     * @return Map containing device types and display strings for current devices.
     */
    public Map<EDeviceType, String> getDeviceList()
    {
        return dm.getDeviceList();
    }

    /**
     * Returns reference to Event Log File object
     *
     * @return Event Log File
     */
    public File getEventLogFile()
    {
        return dm.getEventLogFile();
    }

    /**
     * Returns a map of all event tags the user had configured for Event Logger
     *
     * @return a reference to a event tags map
     */
    public Map<String, String> getEventTags()
    {
        return dm.getEventTags();
    }

    /**
     * Returns a map of channel counts organized by chip and pin
     *
     * @return reference to map of channel counts
     */
    public Map<Integer, Map<Integer, ChannelCount>> getChannelCountCollection()
    {
        return dm.getChannelCountCollection();
    }

    /**
     * Gets a device input map by device, chip and pin combination
     *
     * @param deviceType device in which the the device input map is associated to
     * @param chip       maxim chip in which the device input map is associated to
     * @param pin        maxim pin in which the device input map is associated to
     * @return device input map
     */
    public DeviceInputMap getDeviceInputMapByChipAndPin(EDeviceType deviceType, int chip, int pin)
    {
        return dm.getInputMapByDeviceChipAndPin(deviceType, chip, pin);
    }

    /**
     * Return the string with the current directory name.
     *
     * @return String absolute directory path
     */
    public String getCurrentDirectory()
    {
        return dm.getCurrentDirectory();
    }

    public UUID getEventLogUUID()
    {
        return dm.getEventLogUUID();
    }


    public ArrayList<Long> getHighStates(int chip, int pin) {
        return dm.getHighStates(chip,pin);
    }
}

