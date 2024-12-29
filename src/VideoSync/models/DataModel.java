/*
 * File: 			DataModel.java
 * Date Created:  	June 13, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle and control all aspects of the data
 * 					that is being used for VideoSync. It is the central
 * 					container that delegates and manages information
 * 					that is shared between every object
 *
 * Modified			August 19, 2016
 * Programmer		Danny Hale
 * 					Added the ability to connect to the SQL database
 * 					to get C1 data directly.
 * 					A new function to cumulatively increase the
 * 					graph offset was added.
 * 					Added update to the current directory variable
 * 					which prevented C1 file from working correctly.
 * 					mpf files can now be read from.
 *
 * Modified         December 2018
 * Programmer       Jenzel Arevalo
 *                  Added business logic for Event Logger
 *
 * ****************************************************************
 */
package VideoSync.models;

import VideoSync.analyzers.C1Analyzer;
import VideoSync.analyzers.C1Maxim;
import VideoSync.analyzers.L170Analyzer;
import VideoSync.analyzers.VBM;
import VideoSync.objects.DeviceInputMap;
import VideoSync.objects.EDeviceType;
import VideoSync.objects.InputMappingFile;
import VideoSync.objects.c1.C1Channel;
import VideoSync.objects.c1.C1Object;
import VideoSync.objects.event_logger.ChannelCount;
import VideoSync.objects.event_logger.ChannelCountProxy;
import VideoSync.objects.event_logger.Event;
import VideoSync.objects.event_logger.EventProxy;
import VideoSync.objects.event_logger.metrics.Metric;
import VideoSync.objects.event_logger.metrics.SensitivityMetric;
import VideoSync.objects.graphs.FixedRegion;
import VideoSync.objects.graphs.FreeFormRegion;
import VideoSync.objects.graphs.Line;
import VideoSync.objects.graphs.Region;
import VideoSync.objects.log170.L170Channel;
import VideoSync.views.tabbed_panels.DataWindow;
import VideoSync.views.tabbed_panels.graphs.GraphPanel;
import VideoSync.views.videos.VideoPlayer;
import com.opencsv.CSVWriter;
import org.sqlite.SQLiteConfig;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

// TODO: Singleton pattern for DataModel?

public class DataModel extends Observable
{
    /**
     * Placed at the start of config files to identify them as such.
     * 0xCAFE was chosen because it's easily identified in a hexdump and corresponds to a large negative value, making it unlikely to
     * collide with an offset in projects predating this header.
     */
    private static final int HEADER_ID = 0xCAFE;

    /**
     * Current max version of config file supported.
     */
    private static final int HEADER_CURRENT_VERSION = 3;

    /**
     * Maximum amount of videos that can be open.
     */
    private static final int MAX_VIDEOS = 8;

    /**
     * Current event log database version
     */
    private static final int EVENT_LOG_DATABASE_VERSION = 2;

    /**
     * Version of the current loaded config
     */
    private int configVersion;

    /**
     * The instantiated DataModelProxy to give to other classes
     */
    private final DataModelProxy proxy = new DataModelProxy(this);

    /**
     * Used for keeping track of the playback rate
     */
    private float playbackRate = 1;

    /**
     * Used to indicate if we are currently playing the data
     */
    private boolean isPlaying = false;

    /**
     * Used to indicate if we have loaded data
     */
    private boolean dataLoaded = false;

    /**
     * Stores the current directory based on the file in use
     */
    private String currentDirectory;

    /**
     * Stores references to device files. Replaces L170File, C1File.
     */
    private Vector<File> deviceFiles = new Vector<>();

    /**
     * VideoSync mapping file
     */
    private File mpfFile;

    /**
     * VideoSync input mapping file(s)
     */
    private Vector<InputMappingFile> inputMappingFiles = new Vector<>();
    /**
     * Stores a reference to the c1 analyzer
     */
    private C1Analyzer c1Analyzer;

    /**
     * Stores a reference to the c1 analyzer
     */
    private C1Maxim c1MaximAnalyzer;

    /**
     * Stores a reference to the 170 analyzer
     */
    private L170Analyzer l170Analyzer;

    /**
     * Stores a list of all devices that can be used with the program.
     */
    private Map<EDeviceType, String> deviceList;

    /**
     * Stores a list of all the c1 channel objects
     */
    private Vector<C1Channel> c1Data;

    /**
     * Stores a list of all the c1 maxim channel objects
     */
    private Vector<C1Channel> c1MaximData;

    /**
     * Stores a list of all the 170 channel objects
     */
    private Vector<L170Channel> l170Data;

    /**
     * Stores a list of all video players
     */
    private Vector<VideoPlayer> videoPlayers = new Vector<>();

    /**
     * Stores and creates the input map for the c1 data
     */
    private Vector<DeviceInputMap> c1InputMap;

    /**
     * Stores and creates the input map for the c1 data
     */
    private Vector<DeviceInputMap> c1MaximInputMap;

    /**
     * Stores and creates the input map for the 170 data
     */
    private Vector<DeviceInputMap> l170InputMap;


    /**
     * Used for modifying playback slider rate position for
     */
    private int pbRateSliderPosition = 1;

    /**
     * Stores the graph width in seconds that will be used in the Data Window
     */
    private double gSeconds = 1;

    /**
     * Stores the current time position of all the data elements
     */
    private long currentPosition = 0;

    /**
     * Contains the channel count collection by chip number in Event Logger
     */
    private Map<Integer, Map<Integer, ChannelCount>> channelCountCollection;

    /**
     * Contains a map of all the event tags specified by the user in Event Logger
     */
    private Map<String, String> eventTags;

    /**
     * Contains a map of all hotkey maps used in Event Logger
     */
    private Map<Integer, List<Object>> hotkeyMaps;

    /**
     * Contains a map of all metrics tracked in Event Logger
     */
    private Map<String, Metric> metricsMap;

    /**
     * Contains a map of all metric templates to be loaded in Metric Templates Window
     */
    private Map<String, Metric> metricsTemplateMap;

    /**
     * Holds the update mode when a hotkey is pressed in Event Logger
     */
    private boolean hotkeyUpdateMode = false;

    /**
     * Holds event log database file
     */
    private File eventLogFile;

    private UUID eventLogUUID;

    private boolean logNeverSaved = false;

    private boolean unsavedChanges = false;

    /**
     * Stores the maximum video length
     */
    private long maxVideoLength;

    /**
     * Sets the initial value of the graph offset.
     *
     * TODO: This should probably be converted into an array format so that we can make the offset
     * work for each device type instead of just one. Currently it does all devices simultaneously
     */
    private int graphOffset = 0;

    /**
     * Keeps track of the amount of graph data that should be displayed
     */
    private int graphWidthIndex;

    /**
     * Stores the VLC path to be used with the VideoPlayers
     */
    private final String vlcPath;

    /**
     * Used to gather the current state information about the view
     */
    private DataWindow dw;

    /**
     * Used to save and read graph state information between program instances
     */
    private File config_file;

    /**
     * Whether or not VideoSync is running standalone or from another utility (ex. Reporter)
     */
    private final boolean standaloneInstance;

    /**
     * Whether or not performShutdownOperations has been called on this data model.
     * Used to prevent potential race conditions when the shutdown hook executes.
     */
    private boolean isShutDown = false;

    private final HashMap<Integer, Callable<Boolean>> eventLogUpdateFunctions;

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Model Construction and Initialization Methods

    /**
     * Create the DataModel with the path to the VLC library.
     *
     * @param vlcPath            file path to the vlc codecs
     * @param standaloneInstance Whether or not to close when performShutdownOperations is called
     */
    public DataModel(String vlcPath, Boolean standaloneInstance)
    {
        // Set the local VLC path variable to the one passed
        this.vlcPath = vlcPath;

        this.standaloneInstance = standaloneInstance;

        eventLogUpdateFunctions = new HashMap<>();

        eventLogUpdateFunctions.put(1, this::eventLogV1ToV2);

        initDeviceList();
    }

    /**
     * Initialize the device list for use in any views that utilize the devices
     */
    private void initDeviceList()
    {
        //Create a new linked hash map to store device types and associated display strings.
        //Linked hash map is used to preserve insert order.
        deviceList = new LinkedHashMap<>();

        // Add an initial value to the list so it will be displayed.
        deviceList.put(EDeviceType.DEVICE_NONE, "Devices");
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Model Getters and Setters

    /**
     * Returns the DataModelProxy object from the DataModel
     *
     * @return DataModelProxy
     */
    public DataModelProxy returnProxy()
    {
        return proxy;
    }

    /**
     * Removes all existing data from the workspace so we can load up a new set
     */
    public void removeAllData()
    {
        // If the deviceList has more than 1 element in it, reinitialize the array
        if(deviceList.size() > 1)
            initDeviceList();

        // Loop through all of the VideoPlayers and shut them down.
        for(VideoPlayer vp : videoPlayers)
        {
            vp.performShutdown(false);
        }

        // Reinitialize all of the array objects that contain data to be used
        videoPlayers = new Vector<>();
        c1Data = new Vector<>();
        l170Data = new Vector<>();
        c1InputMap = new Vector<>();
        l170InputMap = new Vector<>();
        inputMappingFiles = new Vector<>();
        deviceFiles = new Vector<>();
        config_file = null;
        eventLogFile = null;
        channelCountCollection = null;
        eventTags = null;
        hotkeyMaps = null;
        metricsMap = null;
        metricsTemplateMap = null;

        //Reset graph offset
        setGraphOffset(0);

        // Notify all of the observers that the data model had reset and they need to do the same
        setChanged();
        notifyObservers("Reset");

    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Model: Input Mapping Methods

    /**
     * Create a vectorized list of Input Mappings for each device added.
     */
    public void addInputMappingFile(String path)
    {
        InputMappingFile temp = new InputMappingFile(path);

        switch(temp.getDeviceName())
        {
            case "C1":
                c1InputMap = temp.getDeviceInputMapVector();
                break;
            case "170":
                l170InputMap = temp.getDeviceInputMapVector();
                break;
            default:
                System.out.println("Unknown device mapping loaded.");
        }

        inputMappingFiles.add(temp);
        System.out.println("Input mapping file: " + temp);
    }

    /**
     * Set the mapping file for the old VideoSync vbm file format.
     *
     * @param file dat file for Log170 data
     */
    public void setVBMFile(File file)
    {
        // Create a new VBM object from the source file
        VBM vbm = new VBM(file);

        // Create the input map file from the vbm file analysis.
        l170InputMap = vbm.analyze();
    }

    /**
     * Set the mapping file based on the new VideoSync format
     *
     * @param mapping vbm file for Log170 channel mappings
     */
    public void setMappingFile(File mapping)
    {
        mpfFile = mapping;

        // Surround all of this in a try catch to catch any errors that may arise while reading in the data
        try
        {
            // Read the contents of the mapping file
            FileInputStream fis = new FileInputStream(mapping);

            // Create an object input stream from the mapping data
            ObjectInputStream ois = new ObjectInputStream(fis);

            // If the mapping file's name is for the 170, then set the mapping file
            if(mapping.getName().contains("170"))
            {
                // If the current input map file is not null, then clear it out
                if(this.l170InputMap != null)
                    this.l170InputMap.clear();

                // Temporary variable to loop through the object input stream contents
                DeviceInputMap dim;

                // Loop through all objects in the file as long as its not null
                // and add it to the array
                int elements = ois.readInt();
                for(int i = 0; i < elements; i++)
                {
                    dim = (DeviceInputMap) ois.readObject();
                    l170InputMap.add(dim);
                }
            }// Added the ability to read C1 file mappings.
            else if(mapping.getName().contains("C1") || mapping.getName().contains("c1"))
            {
                // If the current input map file is not null, then clear it out
                if(c1InputMap != null)
                {
                    c1InputMap.clear();
                }
                else
                {
                    //If the map is null, ensure it is initialized.
                    c1InputMap = new Vector<>();
                }

                // Temporary variable to loop through the object input stream contents
                DeviceInputMap dim;

                // Loop through all objects in the file as long as its not null
                // and add it to the array
                int elements = ois.readInt();
                for(int i = 0; i < elements; i++)
                {
                    dim = (DeviceInputMap) ois.readObject();
                    c1InputMap.add(dim);
                }
            }

            ois.close();
            fis.close();
        }
        catch(IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Set the initial C1 InputMap data.
     */
    private void setC1InputMap()
    {
        //If there is no C1 Input map, initialize it.
        if(c1InputMap == null)
        {
            c1InputMap = new Vector<>();
        }

        // If our input map size is 0, then we are going to add all the channels to it
        if(c1InputMap.size() == 0)
        {
            // Create a temporary array from the current channel list.
            Vector<C1Channel> channels = c1Analyzer.getC1Channels();
            // Loop through all of the channels we found from the C1 data
            for(C1Channel channel : channels)
            {
                // Add a new DeviceInputMap object with the channel number to the array
                c1InputMap.add(new DeviceInputMap(channel.getChip(), channel.getPin(), channel.getChannelNumber()));

            }
        }
    }

    /**
     * Set the initial C1 Maxim InputMap data.
     */
    private void setC1MaximInputMap()
    {
        //Ensure maxim input map is initialized.
        if(c1MaximInputMap == null)
        {
            c1MaximInputMap = new Vector<>();
        }

        // If our input map size is 0, then we are going to add all the channels to it
        if(c1MaximInputMap.size() == 0)
        {
            // Create a temporary array from the current channel list.
            Vector<C1Channel> channels = c1Analyzer.getC1Channels();
            // Loop through all of the channels we found from the C1 data
            for(C1Channel channel : channels)
            {
                // Add a new DeviceInputMap object with the channel number to the array
                c1MaximInputMap.add(new DeviceInputMap(channel.getChip(), channel.getPin(), channel.getChannelNumber()));
            }
        }
    }

    /**
     * Return the input map for the C1 data
     *
     * @return Mapping for C1 channels
     */
    public Vector<DeviceInputMap> getC1InputMap()
    {
        return this.c1InputMap;
    }

    /**
     * Updates the C1 input map from the InputMap View.
     * This will update all channel listings so they will automatically reflect them on the graphing window.
     *
     * @param updated new C1 Device Mapping to replace the current C1 mapping
     */
    public void updateC1InputMap(Vector<DeviceInputMap> updated)
    {
        // Update the input map data
        this.c1InputMap = updated;

        // Notify all the observers of a change
        this.setChanged();
        this.notifyObservers("Input");
    }

    /**
     * Updates the C1 input map from the InputMap View.
     * This will update all channel listings so they will automatically reflect them on the graphing window.
     *
     * @param updated new Maxim Device Mapping to replace the current Maxim mapping
     */
    public void updateMaximInputMap(Vector<DeviceInputMap> updated)
    {
        // Update the input map data
        this.c1MaximInputMap = updated;

        // Notify all the observers of a change
        this.setChanged();
        this.notifyObservers("Input");
    }

    /**
     * Return the input map for the 170 data
     *
     * @return c1MaximInputMap
     */
    public Vector<DeviceInputMap> getMaximInputMap()
    {
        return this.c1MaximInputMap;
    }


    /**
     * Set the initial 170 InputMap data.
     */
    private void set170InputMap()
    {
        //Insure input map is initialized.
        if(l170InputMap == null)
        {
            l170InputMap = new Vector<>();
        }

        // If our input map size is 0, then we are going to add all the channels to it
        if(l170InputMap.size() == 0)
        {
            // Create a temporary array from the current channel list.
            Vector<L170Channel> channels = l170Analyzer.getChannels();
            // Loop through all of the channels we found from the 170 data
            for(L170Channel channel : channels)
            {
                // Add a new DeviceInputMap object with the channel number to the array
                l170InputMap.add(new DeviceInputMap(channel.getChannelNumber()));
            }
        }
    }

    /**
     * Return the input map for the 170 data
     *
     * @return l170InputMap
     */
    public Vector<DeviceInputMap> get170InputMap()
    {
        return this.l170InputMap;
    }

    /**
     * Updates the 170 input map from the InputMap View.
     * This will update all channel listings so they will automatically reflect them on the graphing window.
     *
     * @param updated modified Log170 mapping array
     */
    public void update170InputMap(Vector<DeviceInputMap> updated)
    {
        // Update the input map data
        this.l170InputMap = updated;

        // Notify all the observers of a change
        this.setChanged();
        this.notifyObservers("Input");
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Model: Channel Methods

    /**
     * Returns a list of all devices we have loaded data for. Used for populating drop boxes.
     *
     * @return Map containing device types and display strings for current devices.
     */
    public Map<EDeviceType, String> getDeviceList()
    {
        //Create a new map so that the returned instance is not a reference to the actual map.
        Map<EDeviceType, String> devicesCopy = new LinkedHashMap<>();

        //Loop through entries and make new copies so that the new map does not contain references to the original
        //map entries.
        for(Map.Entry<EDeviceType, String> device : deviceList.entrySet())
        {
            devicesCopy.put(device.getKey(), device.getValue());
        }

        return devicesCopy;
    }

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Model: C1 Data Methods

    /**
     * Set the C1 data file and run the analysis on it.
     *
     * @param c1File .c1 file containing c1 data
     */
    public void setC1Data(File c1File)
    {
        // If were loading a new data file, deallocate the current data set
        if(c1Data != null)
        {
            c1Data = null;
        }

        // Initialize the C1 Analyzer
        c1Analyzer = new C1Analyzer();

        //Add reference to C1 file in device files vector for future use.
        deviceFiles.add(c1File);

        // Analyze the contents of the new file
        c1Analyzer.performAnalysis(c1File);

        // Indicate that we loaded some data
        dataLoaded = true;

        // If our list of devices does not contain "C1", then add it
        if(!deviceList.containsKey(EDeviceType.DEVICE_C1))
        {
            deviceList.put(EDeviceType.DEVICE_C1, "C1");
        }

        // Set the current directory to the absolute path to our new file
        this.currentDirectory = c1File.getParentFile().getAbsolutePath();

        // Set the C1 input map
        setC1InputMap();

        // Indicate that we have a change to all of the observers
        setChanged();

        // Notify any observers that respond directly to the Vector for the device list
        notifyObservers(getDeviceList());

        // Notify all observers of other changes
        notifyObservers();
    }

    /**
     * Set the C1 data file and run the analysis on it.
     *
     * @param rs ResultSet from a database
     */
    public void setC1Data(ResultSet rs)
    {
        // If were loading a new data file, deallocate the current data set
        if(c1Data != null)
        {
            c1Data = null;
        }

        // Initialize the C1 Analyzer
        c1Analyzer = new C1Analyzer();

        // Analyze the contents of the new file
        c1Analyzer.performAnalysis(rs);

        // Indicate that we loaded some data
        dataLoaded = true;

        // If our list of devices does not contain C1, then add it
        if(!deviceList.containsKey(EDeviceType.DEVICE_C1))
        {
            deviceList.put(EDeviceType.DEVICE_C1, "C1");
        }

        // Set the current directory to the absolute path to our new file
        //this.currentDirectory = c1MaximFile.getParentFile().getAbsolutePath();

        // Set the C1 input map
        setC1InputMap();

        // Indicate that we have a change to all of the observers
        setChanged();

        // Notify any observers that respond directly to the Vector for the device list
        notifyObservers(getDeviceList());

        // Notify all observers of other changes
        notifyObservers();
    }

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Model: C1 Maxim Data Methods

    /**
     * Set the C1 data file and run the analysis on it.
     *
     * @param c1MaximFile unused raw maxim data
     */
    public void setC1MaximData(File c1MaximFile)
    {
        // If were loading a new data file, deallocate the current data set
        if(c1MaximData != null)
        {
            c1MaximData = null;
        }

        // Initialize the C1 Analyzer
        c1MaximAnalyzer = new C1Maxim();

        // Analyze the contents of the new file
        c1MaximAnalyzer.performAnalysis(c1MaximFile);

        // Indicate that we loaded some data
        dataLoaded = true;

        // If our list of devices does not contain "Maxim", then add it
        if(!deviceList.containsKey(EDeviceType.DEVICE_C1_MAXIM))
        {
            deviceList.put(EDeviceType.DEVICE_C1_MAXIM, "Maxim");
        }

        // Set the current directory to the absolute path to our new file
        this.currentDirectory = c1MaximFile.getParentFile().getAbsolutePath();

        // Set the C1 input map
        setC1MaximInputMap();

        // Indicate that we have a change to all of the observers
        setChanged();

        // Notify any observers that respond directly to the Vector for the device list
        notifyObservers(getDeviceList());

        // Notify all observers of other changes
        notifyObservers();
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Data Model: 170 Data Methods

    /**
     * Set the 170 data file and run the analysis on it.
     *
     * @param logFile .dat file
     */
    public void set170Data(File logFile)
    {
        // If were loading a new data file, deallocate the current data set
        if(l170Data != null)
        {
            l170Data = null;
        }

        // Initialize the 170 Analyzer
        l170Analyzer = new L170Analyzer();

        //Add reference to 170 file in device files vector for future use.
        deviceFiles.add(logFile);

        // Analyze the contents of the new file
        l170Analyzer.performAnalysis(logFile);

        // Indicate that we loaded some data
        dataLoaded = true;

        // If our list of devices does not contain "170", then add it
        if(!deviceList.containsKey(EDeviceType.DEVICE_LOG170))
        {
            deviceList.put(EDeviceType.DEVICE_LOG170, "170");
        }

        // Set the current directory to the absolute path to our new file
        this.currentDirectory = logFile.getParentFile().getAbsolutePath();

        // Set the 170 input map
        set170InputMap();

        // Indicate that we have a change to all of the observers
        setChanged();

        // Notify any observers that respond directly to the Vector for the device list
        notifyObservers(getDeviceList());

        // Notify all observers of other changes
        notifyObservers();
    }

    /**
     * Returns an individual File in the indexed input mapping files array.
     *
     * @return individual mapping file
     */
    public InputMappingFile getInputMappingFile(String index)
    {
        InputMappingFile mappingFile = null;

        for(int i = 0, j = 0; i < inputMappingFiles.size() && j == 0; i++)
        {
            if(inputMappingFiles.elementAt(i).equals(index))
            {
                mappingFile = inputMappingFiles.elementAt(i);
                j = 1;
            }
        }

        return mappingFile;
    }

    /**
     * Returns the files for the state data. Includes both C1 and Log170 if both are present.
     *
     * @return vector containing File objects of both C1 and Log170 data if present
     */
    public Vector<File> getDataFiles()
    {
        //Build up a new vector of files so that any operations do not affect data model vector.
        return new Vector<>(deviceFiles);
    }

    /**
     * Returns a vector containing all input mapping files.
     *
     * @return vector of InputMappingFile objects
     */
    public Vector<InputMappingFile> getInputMappingFiles()
    {
        return new Vector<>(inputMappingFiles);
    }

    /**
     * Returns a vector containing all loaded video files.
     *
     * @return vector of video files
     */
    public Vector<File> getVideoFiles()
    {
        Vector<File> videoFiles = new Vector<>();
        for(VideoPlayer player : videoPlayers)
        {
            videoFiles.add(player.getVideoFile());
        }

        return videoFiles;
    }

    /**
     * Returns the config file
     *
     * @return config file
     */
    public File getConfigFile()
    {
        return config_file;
    }


    // -- Data Model: Video Methods

    /**
     * Adds a video to the current workspace to be displayed.
     *
     * @param vidFile file path of the video to be loaded into VideoSync
     */
    public void addVideoFile(File vidFile, boolean isSaved)
    {
        // Ensure that we don't create more video players than MAX_VIDEOS
        if(videoPlayers.size() + 1 > MAX_VIDEOS)
            return;

        // Ensure that the new video file has not already been loaded up.
        boolean addVideo = true;
        for(VideoPlayer vp : videoPlayers)
        {
            if(vidFile.getName().equals(vp.getVideoFile().getName()))
            {
                addVideo = false;
            }
        }

        if(!addVideo)
        {
            // Create the buttons for the JOptionPane
            Object[] options = {"Yes", "No"};

            // Show an option pane and get the result of their input.
            // Because JOptionPane requires a parent component to display the alert, we just create an empty JFrame so it will be displayed.
            int answer = JOptionPane.showOptionDialog(new JFrame(), "This video file has already been loaded.\nAre you sure you wish to add it anyways?", null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

            // If 'n' is 0, then the user wants to add the video anyways.
            if(answer == JOptionPane.YES_OPTION)
            {
                addVideo = true;
            }
        }

        // Adds the video to the workspace
        if(addVideo)
        {
            // Add the video player to the videoPlayers array.
            // The following parameters (listed in order) are passed on VideoPlayer Construction:
            // -- VLC path so the video libraries are loaded correctly
            // -- Video File to load
            // -- The current time position of the data
            // -- The ID number for the video player
            // -- A DataModelProxy Object created from this class
            // -- A reference to this class.
            //
            VideoPlayer newPlayer = new VideoPlayer(this.vlcPath, vidFile, currentPosition, (videoPlayers.size() + 1), proxy, isSaved);
            videoPlayers.add(newPlayer);
            dw.addVideoPanel(videoPlayers.get(videoPlayers.size() - 1));

            //Add the player as an observer so that the player does not need a reference to the Data Model to add itself.
            addObserver(newPlayer);

            Vector<GraphPanel> graphPanels = dw.getGraphPanels();

            //Create regions for the player and register with graph panel
            for(GraphPanel graphPanel : graphPanels)
            {
                Region newRegion = new FixedRegion();
                newPlayer.addRegion(newRegion);
                graphPanel.addVideoRegion(newRegion);
            }

            long maxVideoLength = videoPlayers.firstElement().getVideoLength();
            for(VideoPlayer videoPlayer : videoPlayers)
            {
                if(videoPlayer.getVideoLength() > maxVideoLength)
                    maxVideoLength = videoPlayer.getVideoLength();
            }
            System.out.println("Max video length: " + maxVideoLength);
            this.maxVideoLength = maxVideoLength;
        }
    }

    /**
     * Adds a new region for a graph panel, this is used when new graph panels are
     * added at run time
     *
     * @param graphPanel Panel where channels are selected and their data is drawn
     */
    public void addVideoRegion(GraphPanel graphPanel)
    {
        Region newRegion = new FixedRegion();
        graphPanel.addVideoRegion(newRegion);
        for(VideoPlayer videoPlayer : videoPlayers)
        {
            videoPlayer.addRegion(newRegion);
        }
    }

    // -- Data Model: Playback Methods

    /**
     * Advance all data graphs and video by a frame
     */
    public void advanceFrame()
    {
        // Loop through all of the video players
        for(VideoPlayer vp : videoPlayers)
        {
            // If the video player's id is 1, then it will also update the graphs to the same position.
            if(vp.getPlayerID() == 1)
            {
                // Set the current position to the value returned from jumping the video forward by a frame.
                setCurrentPosition(vp.nextFrame(), false);
            }
            else
            {
                // Update all other video players if their id is not 1
                vp.nextFrame();
            }
        }

        // Notify any observers of the change
        notifyObservers();
    }

    /**
     * Reverse all data graphs and video by a 'frame'
     *
     * NOTE: Due to VLC's implementation, there is no "frame back', so the data will jump back by ~500milliseconds
     */
    public void reverseFrame()
    {
        // Loop through all of the video players
        for(VideoPlayer vp : videoPlayers)
        {
            // If the video player's id is 1, then it will also update the graphs to the same position.
            if(vp.getPlayerID() == 1)
            {
                // Set the current position to the value returned from jumping the video backwards by a frame.
                setCurrentPosition(vp.previousFrame(), false);
            }
            // Update all other video players if their id is not 1
            else
            {
                vp.previousFrame();
            }
        }

        // Notify any observers of the change
        notifyObservers();
    }

    /**
     * Skip the video by the amount specified.
     *
     * @param amount Integer amount to skip video by
     */
    public void skipVideo(int amount)
    {
        // Loop through all of the video players and skip them by the amount passed.
        for(VideoPlayer vp : videoPlayers)
        {
            vp.skipVideo(amount);
        }

        // Set the new position for the graphs by adding the current time to the new time value.
        long pos = this.getCurrentPosition() + amount;

        // Ensure that the new time value is not less than 0 and not greater than the max time found,
        // If the position is less than 0, assign it to 0
        if(pos < 0)
        {
            pos = 0;
        }
        // If the position is greater than the max time, assign it to the max time
        else if(pos > this.maxVideoLength)
        {
            pos = this.maxVideoLength;
        }

        // Update the current position to our new value.
        this.setCurrentPosition(pos, false);
    }

    /**
     * Return if we are currently playing the data
     *
     * @return Boolean value representing whether data/video is currently playing
     */
    public boolean isPlaying()
    {
        return isPlaying;
    }

    /**
     * Plays or Pauses the video based on the parameters value.
     *
     * @param isPlaying Boolean value representing whether to play or stop data/video
     */
    public void setPlaying(boolean isPlaying)
    {
        // Set our isPlaying flag to the one passed
        this.isPlaying = isPlaying;

        // Loop through all of the video players
        for(VideoPlayer vp : videoPlayers)
        {
            // If we are now playing, we can play all the videos.
            if(this.isPlaying)
            {
                //Only start playback if within bounds of video.
                System.out.println("Current media player length: " + vp.getMediaPlayer().getLength());
                if(getCurrentPosition() < vp.getVideoLength())
                {
                    // Have the video players play
                    vp.jumpToTime(getCurrentPosition());
                    vp.playVideo();
                }
            }
            else
            {
                // Since we paused, we need to update the graph to the position of the master video player.
                // If the Video Player ID is 1, then update the graph position based on the video time of the master player.
                // Check if media is playing so that if the graph position is before or after video, we don't jump back to time 0.
                if((vp.getPlayerID() == 1) && (vp.getMediaPlayer().isPlaying()))
                {
                    setCurrentPosition(vp.pauseVideo(), false);
                }
                // Otherwise pause the videos.
                else
                {
                    vp.pauseVideo();

                    //Needed to update play button text if after the video position.
                    notifyObservers();
                }
            }
        }
    }

    /**
     * Set the playback rate of all the data
     *
     * @param rate The playback rate for the graph and the video(s)
     */
    public void setPlaybackRate(float rate)
    {
        // Update the Data Model's Playback rate to the one passed.
        this.playbackRate = rate;

        // Loop through all of the video players and update their playback rate.
        for(VideoPlayer vp : videoPlayers)
        {
            vp.setPlaybackSpeed(rate);
        }

        // Set the changed flag
        setChanged();

        // Notify all of our observers we have changes and we will send the value we want to update with.
        notifyObservers((double) playbackRate);
    }

    /**
     * Set playback rate slider position
     *
     * @param x Integer representing the position to place the slider
     */
    public void setPlaybackRateSliderPosition(int x)
    {
        this.pbRateSliderPosition = x;
        notifyObservers();
    }

    /**
     * Returns the current position of the playback rate slider
     *
     * @return Current position of playback rate slider position
     */
    public int getPlaybackRateSliderPosition()
    {
        return pbRateSliderPosition;
    }

    /**
     * Return the current position of the data
     *
     * @return Current position of the graph data and the video(s)
     */
    public long getCurrentPosition()
    {
        return currentPosition;
    }

    /**
     * Set the current data position to a new value.
     *
     * @param currentPosition    Long value representing the current position of the graph data and video
     * @param updateVideoPlayers Whether to update video players or just DataModel and its observers
     */
    public synchronized void setCurrentPosition(long currentPosition, boolean updateVideoPlayers)
    {
        // Update the current position based on the new time value.
        this.currentPosition = currentPosition;

        // If updateVideoPlayers is true, we also need to update the video players to the correct position.
        if(updateVideoPlayers)
        {
            // Ensure that we have video players to update.
            if(videoPlayers != null)
            {
                // Loop through all of the video players and update their positions by jumping to a specific time.
                for(VideoPlayer vp : videoPlayers)
                {
                    vp.jumpToTime(currentPosition);
                }
            }
        }

        setChanged();

        // Call the local method to notify all observers of changes.
        notifyObservers();
    }

    /**
     * Get the max video length
     *
     * @return Max video length
     */
    public long getMaxVideoLength()
    {
        return this.maxVideoLength;
    }

    // -- Data Model: Graphing Methods

    /**
     * Returns all of the state information for a specific device for graphing.
     *
     * NOTE: This can be updated in the future with more devices and allow for expandability.
     *
     * @param device EDeviceType object to determine what analyzer to use (C1Analyzer or C1Maxim)
     * @param chip   Chip associated with channel to be retrieved
     * @param pin    Pin associated with channel to be retrieved
     * @param width  Graph width in pixels
     * @param bottom Graph bottom line pixel location
     * @param height Graph top line pixel location
     * @return Vector containing Line objects to be drawn onto the graph
     */
    public Vector<Line> getStateDataForDevice(EDeviceType device, int chip, int pin, int width, int bottom, int height)
    {
        Vector<Line> toReturn = null;

        // If the device is equal to "C1", then return the states for the C1 data.
        if(device == EDeviceType.DEVICE_C1)
        {
            // Return the graph events from the C1 Analysis
            toReturn = c1Analyzer.getGraphLines(width, currentPosition + this.graphOffset, gSeconds, chip, pin, (height * 1.0), (bottom * 1.0));

            //If we have no events and are at beginning of graph, return the first events so that an extended graph can be displayed
            if(((toReturn == null) || (toReturn.isEmpty())) && (c1Analyzer.getMaxTimeInMillis() > (currentPosition + graphOffset)))
            {
                toReturn = c1Analyzer.getGraphLines(width, 0, 0, chip, pin, (height * 1.0), (bottom * 1.0));
            }
        }
        // If the device is equal to "C1", then return the states for the C1 data.
        else if(device == EDeviceType.DEVICE_C1_MAXIM)
        {
            // Return the graph events from the C1 Analysis
            toReturn = c1MaximAnalyzer.getGraphEvents(width, currentPosition + this.graphOffset, gSeconds, chip, pin, (height * 1.0), (bottom * 1.0));

            //If we have no events and are at beginning of graph, return the first events so that an extended graph can be displayed
            if(((toReturn == null) || (toReturn.isEmpty())) && (c1MaximAnalyzer.getMaxTimeInMillis() > (currentPosition + graphOffset)))
            {
                toReturn = c1MaximAnalyzer.getGraphEvents(width, 0, 0, chip, pin, (height * 1.0), (bottom * 1.0));
            }
        }

        // If we get to this point without getting events from an analyzer, we don't have a device to return.
        return toReturn;
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
        return c1Analyzer.getVarianceLines(width, height, currentPosition + this.graphOffset, gSeconds, chip, pin, variance, varianceMode);
    }

    public Vector<C1Channel> getC1AnalyzerChannels()
    {
        return c1Analyzer.getC1Channels();
    }

    /**
     * Sets the graph width based on the parameter value.
     *
     * @param d Width of the graph
     */
    public void setGraphWidth(double d)
    {
        // Set the graph width
        this.gSeconds = d;

        // Notify all observers of the changes
        notifyObservers();
    }

    /**
     * Return the current graph width
     *
     * @return The current graph width in seconds
     */
    public double getSeconds()
    {
        return this.gSeconds;
    }

    /**
     * Set the graph offset amount so we can sync between the video and graphs.
     *
     * FIXME: This needs to be expanded to allow multiple data sets to be adjusted individually.
     *
     * @param offset Offset amount to set graph to
     */
    public void setGraphOffset(int offset)
    {
        // Set the graph offset amount to the parameter
        graphOffset = offset;

        // Notify all observers that we have changes
        notifyObservers();
    }

    /**
     * Increase the graph offset amount so we can sync between the video and graphs.
     *
     * @param offset Amount to increase graph offset by
     */
    public void increaseGraphOffset(int offset)
    {
        // Set the graph offset amount to the parameter
        graphOffset += offset;

        // Notify all observers that we have changes
        setChanged();
        notifyObservers("Mouse");
    }

    /**
     * Gets the graph offset used to sync the videos and graphs.
     *
     * @return Integer value representing the graph offset
     */
    public int getGraphOffset()
    {
        return graphOffset;
    }

    /**
     * Sets the current graph width index
     *
     * @param ind Index of dropdown menu representing the graph width choice
     */
    public void setGraphWidthIndex(int ind)
    {
        graphWidthIndex = ind;
        setChanged();
        notifyObservers("GraphWidth");
    }

    /**
     * Gets the current graph width index
     *
     * @return Index of dropdown menu representing the graph width choice
     */
    public int getGraphWidthIndex()
    {
        return graphWidthIndex;
    }

    // -- Data Model: Observer Methods

    /**
     * Notify all observers that we have changes to make.
     */
    public void notifyObservers()
    {
        // Indicate to the observer that we have a change.
        setChanged();

        // Notify all observers with the DataModelProxy object.
        notifyObservers(proxy);
    }

    // -- Data Model: TO FINISH SORTING....

    /**
     * Return the string with the current directory name.
     *
     * @return String absolute directory path
     */
    public String getCurrentDirectory()
    {
        return this.currentDirectory;
    }

    /**
     * Return the flag indicating if we have loaded data
     *
     * @return Boolean returns true if state data has been loaded into VideoSync
     */
    public boolean isDataLoaded()
    {
        return dataLoaded;
    }

    /**
     * Jumps to a specific event in time for a specific device.
     *
     * FIXME: This needs to be updated to account for each individual graph offset, this may not be
     * able to be done in this method and should only set the base time value and let the
     * analyzers handle the offsets instead.
     *
     * @param device EDeviceType specifying C1 or C1Maxim
     * @param event  1: Forward, 0: Backward
     * @param chip   Chip associated with channel of event we're jumping to
     * @param pin    Pin associated with channel of event we're jumping to
     * @param state  High or low state we're jumping to
     */
    public void jumpToEvent(EDeviceType device, int event, int chip, int pin, int state)
    {
        // Set our initial position value to 0
        long position = 0;

        // If the device is a 170 type, then jump to one of its events
        switch(device)
        {
            // If the device is a C1 type, then jump to one of its events
            case DEVICE_C1:
                // If our event is 0 (meaning backwards), jump back by one event
                if(event == 0)
                {
                    // Jump back to the previous event based on the chip & pin value.
                    position = c1Analyzer.returnPreviousTimeValueForEvent(chip, pin, state);
                }
                // Otherwise jump forwards
                else
                {
                    // Jump forward to the previous event based on the chip & pin value.
                    position = c1Analyzer.returnNextTimeValueForEvent(chip, pin, state);
                }
                break;
            // If the device is a C1 type, then jump to one of its events
            case DEVICE_C1_MAXIM:
                // If our event is 0 (meaning backwards), jump back by one event
                if(event == 0)
                {
                    // Jump back to the previous event based on the chip & pin value.
                    position = c1MaximAnalyzer.returnPreviousTimeValueForEvent(chip, pin, state);
                }
                // Otherwise jump forwards
                else
                {
                    // Jump forward to the previous event based on the channel number & bit value.
                    position = c1MaximAnalyzer.returnNextTimeValueForEvent(chip, pin, state);
                }
                break;
        }

        // NOTE: This method can be expanded in the future to include other devices

        // Set the new position based on the graph's offset.
        setCurrentPosition(position - this.graphOffset, true);
    }

    /**
     * Performs all shutdown operations for closing down VideoSync
     */
    public void performShutdownOperations()
    {
        performShutdownOperations(false);
    }

    /**
     * Performs all shutdown operations for closing down VideoSync
     *
     * @param ignoreExit if shutdown operations would normally result in exit being called, do not call exit.
     */
    private void performShutdownOperations(boolean ignoreExit)
    {
        //Ensure that shutdown operations are only ran once.
        if(!isShutDown)
        {
            isShutDown = true;

            //Write a configuration file for loaded data
            if(config_file != null)
            {
                writeConfigFile();
            }

            // If we have more than 1 video player currently in use, then we need to remove it before shutting down.
            if(videoPlayers != null && videoPlayers.size() > 0)
            {
                // Loop through all video players and have them perform shutdown operations.
                for(VideoPlayer vp : videoPlayers)
                {
                    // The false means that some external source is closing it
                    vp.performShutdown(false);
                }
            }

            if(eventLogFile != null && unsavedChanges)
            {
                int result = JOptionPane.showConfirmDialog(dw, "Save event log file?", "Save Event Log", JOptionPane.YES_NO_OPTION);
                if(result == JOptionPane.YES_OPTION)
                {
                    // TODO: Trigger FileChooser in this situation
                    writeEventLogDBFile();
                }
            }

            // Quit VideoSync
            if(standaloneInstance && !ignoreExit)
            {
                System.exit(0);
            }
        }
        else
        {
            System.out.println("Data model has already shut down.");
        }
    }

    /**
     * Presents all views to the front of the screen.
     */
    public void presentAllViews()
    {
        // Notify the Observer that we have changes to make
        setChanged();

        // Notify all Observers that we have changes.
        notifyObservers("Present");
    }

    /**
     * Removes a video player from the list. This is called when the video player shuts down from its own window by the user.
     *
     * @param vp Video player to remove from the current session
     */
    public void unregisterVideo(VideoPlayer vp)
    {
        // Remove the video player object from the array
        videoPlayers.remove(vp);

        // Update the remaining players with a new video id number so that there is always one that is the master player.
        for(int i = 0; i < videoPlayers.size(); i++)
        {
            videoPlayers.elementAt(i).setPlayerID(i + 1);
        }
    }

    /**
     * Return the channel chip number for the name used in the selection windows.
     *
     * NOTE: This should be updated to include additional devices when they are added in
     *
     * @param device EDeviceType specifying C1 or C1Maxim
     * @param name   Channel name
     * @return Chip number for channel
     */
    public int getChannelChipNumberFromName(EDeviceType device, String name)
    {
        // Make sure that the device being searched for is in our device list.
        if(deviceList.containsKey(device))
        {
            // Check to see if our name matches the 170
            if(device == EDeviceType.DEVICE_LOG170)
            {
                return -1;
            }
            // Check to see if our name matches the C1
            else if(device == EDeviceType.DEVICE_C1)
            {
                // Loop through the device input map contents searching for a channel with a matching name
                for(DeviceInputMap dim : c1InputMap)
                {
                    // If the input map data has an element matching that name,
                    // return the bit number for that name.
                    if(dim.getChannelName().equals(name))
                    {
                        return dim.getChipNumber();
                    }
                }
            }
            // Check to see if our name matches the Maxim
            else if(device == EDeviceType.DEVICE_C1_MAXIM)
            {
                // Loop through the device input map contents searching for a channel with a matching name
                for(DeviceInputMap dim : c1MaximInputMap)
                {
                    // If the input map data has an element matching that name,
                    // return the bit number for that name.
                    if(dim.getChannelName().equals(name))
                    {
                        return dim.getChipNumber();
                    }
                }
            }
        }

        // Return -1 if we didn't find a matching device in the list
        return -1;
    }

    /**
     * Return the channel pin number for the name used in the selection windows.
     *
     * NOTE: This should be updated to include additional devices when they are added in
     *
     * @param device EDeviceType specifying C1 or C1Maxim
     * @param name   channel name
     * @return pin number associated with channel
     */
    public int getChannelPinNumberFromName(EDeviceType device, String name)
    {
        // Make sure that the device being searched for is in our device list.
        if(deviceList.containsKey(device))
        {
            // Check to see if our name matches the 170
            if(device == EDeviceType.DEVICE_LOG170)
            {
                return -1;
            }
            // Check to see if our name matches the C1
            else if(device == EDeviceType.DEVICE_C1)
            {
                // Loop through the device input map contents searching for a channel with a matching name
                for(DeviceInputMap dim : c1InputMap)
                {
                    // If the input map data has an element matching that name,
                    // return the bit number for that name.
                    if(dim.getChannelName().equals(name))
                    {
                        return dim.getPinNumber();
                    }
                }
            }
            // Check to see if our name matches the Maxim
            else if(device == EDeviceType.DEVICE_C1_MAXIM)
            {
                // Loop through the device input map contents searching for a channel with a matching name
                for(DeviceInputMap dim : c1MaximInputMap)
                {
                    // If the input map data has an element matching that name,
                    // return the bit number for that name.
                    if(dim.getChannelName().equals(name))
                    {
                        return dim.getPinNumber();
                    }
                }
            }
        }

        // Return -1 if we didn't find a matching device in the list
        return -1;
    }

    /**
     * Returns whether or not VideoSync is running standalone or was started by another utility (ex. Reporter)
     *
     * @return true if VideoSync is running standalone, false if it was started another utility
     */
    public boolean getStandaloneInstance()
    {
        return standaloneInstance;
    }

    /**
     * Stores the DataWindow object instance in the DataModel
     *
     * @param d instance of DataWindow object
     */
    public void setDataWindow(DataWindow d)
    {
        dw = d;
    }

    /**
     * Used to synchronize the state data to the input mapping
     * that describes each channel. Will create an input
     * mapping file if one does not already exist.
     */
    public void organizeData()
    {
        if((c1Analyzer != null) && (mpfFile == null))
        {
            String absolute_file_path = getCurrentDirectory() + File.separator + "C1_mapping.mpf";
            Vector<C1Channel> c1Channels = c1Analyzer.getC1Channels();

            //Check to see if chip and pin numbers for c1 channels in Input Map are not 0, if the chip and pin numbers for channels are all 0, it is likely
            //that the loaded input mapping file is from an older version of the mapping file and the chip numbers should be added
            for(DeviceInputMap deviceInputMap : c1InputMap)
            {
                for(C1Channel c1Channel : c1Channels)
                {
                    if(c1Channel.getChannelNumber() == deviceInputMap.getChannelNumber())
                    {
                        if(deviceInputMap.getChipNumber() == 0) deviceInputMap.setChipNumber(c1Channel.getChip());
                        if(deviceInputMap.getPinNumber() == 0) deviceInputMap.setPinNumber(c1Channel.getPin());
                        break;
                    }
                }
            }

            inputMappingFiles.add(new InputMappingFile(absolute_file_path, "C1", c1InputMap));
            inputMappingFiles.lastElement().writeFile();
        }
        if((l170Analyzer != null) && (mpfFile == null))
        {
            String absolute_file_path = getCurrentDirectory() + File.separator + "Log170_mapping.mpf";
            inputMappingFiles.add(new InputMappingFile(absolute_file_path, "170", l170InputMap));
            inputMappingFiles.lastElement().writeFile();
        }
    }

    /**
     * Replaces any reference to oldRegion in the graph panels with newRegion.
     *
     * @param oldRegion Video region reference we're replacing
     * @param newRegion Video region reference that's replacing the oldRegion.
     */
    public void replaceGraphVideoRegion(Region oldRegion, Region newRegion)
    {
        Vector<GraphPanel> graphPanels = dw.getGraphPanels();
        for(GraphPanel graphPanel : graphPanels)
            graphPanel.replaceVideoRegion(oldRegion, newRegion);
    }

    /**************************************************************************************
     * Save the session information so that the last state of the program can be reloaded.
     * Note that the order in which the data is written is the order in which the data
     * must be read.
     *************************************************************************************/
    public void writeConfigFile()
    {
        if(config_file != null)
        {
            //If an old config file is loaded in to VideoSync, delete it and create a new config database file
            if(config_file.getName().equals("config"))
            {
                config_file.delete();
                createNewConfigDBFile();
            }

            writeConfigDBFile();

            /*
             * Code section below was used for configuration file versions 2 and earlier, which wrote
             * the VideoSync configurations in a 'config' object file. While the object config file was
             * easy to write data into, it was complex to append new data to as the order in which data
             * is written in an object file cannot be altered. This section of commented code is deprecated
             * and is only kept for referential purposes only.
             */

            /*
            Vector<GraphPanel> graph_panels = dw.getGraphPanels();
            System.out.println(" **** Saving session file ****");

            try
            {
                DataOutputStream os = new DataOutputStream(new FileOutputStream(config_file));

                //Write header.
                os.writeInt(HEADER_ID);
                os.writeInt(HEADER_CURRENT_VERSION);

                //Save the graph state(s)
                os.writeInt(getGraphOffset());
                os.writeInt(getGraphWidthIndex());
                for(GraphPanel gp : graph_panels)
                {
                    os.writeBoolean(gp.isCheckboxSelected());
                    os.writeBoolean(gp.isShowInVideoSelected());
                    os.writeInt(gp.getDeviceIndex());
                    os.writeInt(gp.getChannelIndex());

                    //Write color RGB
                    Color panelColor = gp.getColor();
                    os.writeInt(panelColor.getRed());
                    os.writeInt(panelColor.getGreen());
                    os.writeInt(panelColor.getBlue());
                }

                //Save the video(es) state
                os.writeLong(getCurrentPosition());        //Save current video position without offset

                // Count how many video players are saved to determine how many will be written to config file
                // We do this instead of just calling videoPlayers.size() so that we don't save any data from video
                // players whose video file is not saved in the primary directory.
                int savedSize = 0;
                for(VideoPlayer vp : videoPlayers)
                {
                    if(vp.isSaved())
                        savedSize++;
                }

                System.out.println("Saved video players: " + savedSize);

                os.writeInt(savedSize);        //number of saved video files
                for(VideoPlayer videoPlayer : videoPlayers)
                {
                    // Make sure we skip saving data for unsaved video players
                    if(!videoPlayer.isSaved())
                    {
                        System.out.println("Skipping unsaved video player");
                        continue;
                    }

                    os.writeLong(videoPlayer.getOffset());    //individual video offset
                    os.writeInt(videoPlayer.getX());
                    os.writeInt(videoPlayer.getY());
                    os.writeInt(videoPlayer.getWidth());
                    os.writeInt(videoPlayer.getHeight());

                    //Save video region state
                    Vector<Region> videoRegions = videoPlayer.getRegions();
                    for(Region dataRegion : videoRegions)
                    {
                        // All regions have these values
                        os.writeInt(dataRegion.getCoordX());
                        os.writeInt(dataRegion.getCoordY());

                        // Two types of regions possible
                        if(dataRegion instanceof FixedRegion)
                        {
                            // Fixed Region
                            // Saving isFixed flag, height, and width
                            //System.out.println("Saving a fixed region!");
                            //System.out.println("X: " + dataRegion.getCoordX() + ", Y: " + dataRegion.getCoordY());
                            FixedRegion fixedRegion = (FixedRegion) dataRegion;

                            os.writeBoolean(true);
                            os.writeInt(fixedRegion.getWidth());
                            os.writeInt(fixedRegion.getHeight());
                            //System.out.println("isFixed: " + true);
                            //System.out.println("Width: " + fixedRegion.getWidth() + ", Height: " + fixedRegion.getHeight());
                        }
                        else if(dataRegion instanceof FreeFormRegion)
                        {
                            // Freeform Region
                            // Saving isFixed flag, vertex count, and vertex position coordinates
                            //System.out.println("Saving a freeform region!");
                            //System.out.println("X: " + dataRegion.getCoordX() + ", Y: " + dataRegion.getCoordY());
                            FreeFormRegion freeRegion = (FreeFormRegion) dataRegion;

                            os.writeBoolean(false);
                            //System.out.println("isFixed: " + false);

                            os.writeInt(freeRegion.getVertexCount());
                            //System.out.println("vertexCount: " + freeRegion.getVertexCount());

                            //System.out.println("Vertices:");
                            for(Point p : freeRegion)
                            {
                                os.writeInt((int) p.getX());
                                os.writeInt((int) p.getY());

                                //System.out.println("X: " + p.getX() + ", Y: " + p.getY());
                            }
                        }
                        //System.out.println();
                    }
                }
                os.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            */
        }
        else
        {
            System.out.println("No session file to save.");
        }
    }

    /**
     * Load state data from last recorded session. Note that the order in which
     * the data was saved is the order in which the data must be loaded.
     */
    public void readConfigFile()
    {
        if(config_file != null)
        {
            //If loaded config file is a database file, read from the database
            if(config_file.getName().equals("config.db"))
            {
                Connection configDBConnection = dbConnect(config_file);
                boolean valid = verifyConfigDatabaseTables();
                if(valid)
                {
                    try
                    {
                        configDBConnection.setAutoCommit(false);

                        String query;
                        PreparedStatement statement;
                        ResultSet resultSet;

                        //Load the metadata
                        query = "SELECT * FROM metadata WHERE header_id=? ORDER BY ROWID";
                        statement = configDBConnection.prepareStatement(query);
                        statement.setInt(1, HEADER_ID);
                        resultSet = statement.executeQuery();
                        configVersion = resultSet.getInt(2);
                        long currentPosition = resultSet.getLong(3);
                        int graphOffset = resultSet.getInt(4);
                        int graphWidthIndex = resultSet.getInt(5);
                        setCurrentPosition(currentPosition, true);
                        setGraphOffset(0);
                        increaseGraphOffset(graphOffset);
                        setGraphWidthIndex(graphWidthIndex);


                        //Load graph panels
                        query = "SELECT COUNT(*) FROM graph_panel ORDER BY ROWID";
                        statement = configDBConnection.prepareStatement(query);
                        resultSet = statement.executeQuery();
                        int graphCount = resultSet.getInt(1);
                        if(graphCount > 8 && graphCount != dw.getGraphPanels().size())
                        {
                            dw.addGraphPanels(graphCount - 8);
                        }
                        else if(graphCount < 8 && graphCount != dw.getGraphPanels().size())
                        {
                            dw.removeGraphPanels(8 - graphCount);
                        }
                        query = "SELECT * FROM graph_panel ORDER BY ROWID";
                        statement = configDBConnection.prepareStatement(query);
                        resultSet = statement.executeQuery();
                        Vector<GraphPanel> graph_panels = dw.getGraphPanels();
                        int index = 0;

                        while(resultSet.next())
                        {
                            GraphPanel graphPanel = graph_panels.get(index);
                            //Retrieve values for data display from config file
                            boolean enableCheckbox = resultSet.getBoolean(2);
                            boolean showInVideo = resultSet.getBoolean(3);
                            int deviceIndex = resultSet.getInt(4);
                            int channelIndex = resultSet.getInt(5);

                            //Only apply data display configuration if a data file is loaded
                            if(dataLoaded)
                            {
                                graphPanel.setCheckboxEnable(enableCheckbox);
                                graphPanel.setShowInVideo(showInVideo);
                                graphPanel.setDeviceIndex(deviceIndex);
                                graphPanel.setChannelIndex(channelIndex);
                            }
                            else
                            {
                                System.out.println("Data not loaded, skipping config file graph panel information");
                            }

                            //Retrieve graph color.
                            int red = resultSet.getInt(6);
                            int green = resultSet.getInt(7);
                            int blue = resultSet.getInt(8);
                            graphPanel.setColor(new Color(red, green, blue));
                            ++index;
                        }


                        //Load video players
                        query = "SELECT COUNT(*) FROM video_player ORDER BY ROWID";
                        statement = configDBConnection.prepareStatement(query);
                        resultSet = statement.executeQuery();
                        int savedSize = resultSet.getInt(1);
                        for(int i = 0; i < savedSize; i++)
                        {

                            query = "SELECT * from video_player WHERE video_player_id=? ORDER BY ROWID";
                            statement = configDBConnection.prepareStatement(query);
                            statement.setInt(1, i);
                            resultSet = statement.executeQuery();
                            int x = resultSet.getInt(2);
                            int y = resultSet.getInt(3);
                            int width = resultSet.getInt(4);
                            int height = resultSet.getInt(5);
                            int offset = resultSet.getInt(6);
                            videoPlayers.get(i).setOffsetText(offset);
                            videoPlayers.get(i).setBounds(x, y, width, height);

                            //Load Regions for Video Player
                            Vector<Region> videoRegions = videoPlayers.get(i).getRegions();
                            for(int j = 0; j < videoRegions.size(); j++)
                            {
                                Region dataRegion = videoRegions.get(j);
                                //Acquire the regions associated with the current video player
                                query = "SELECT * FROM region WHERE video_player_id=? AND region_id=? ORDER BY ROWID";
                                statement = configDBConnection.prepareStatement(query);
                                statement.setInt(1, i);
                                statement.setInt(2, j);
                                resultSet = statement.executeQuery();

                                int regionId = resultSet.getInt(1);
                                boolean fixed = resultSet.getBoolean(3);
                                int region_x = resultSet.getInt(4);
                                int region_y = resultSet.getInt(5);

                                if(fixed)
                                {
                                    System.out.println("Loading fixed region!\nisFixed: " + true);
                                    FixedRegion fixedRegion = (FixedRegion) dataRegion;
                                    int region_width = resultSet.getInt(6);
                                    int region_height = resultSet.getInt(7);
                                    fixedRegion.setCoordX(region_x);
                                    fixedRegion.setCoordY(region_y);
                                    System.out.println("X: " + region_x + ", Y: " + region_y);
                                    fixedRegion.setWidth(region_width);
                                    fixedRegion.setHeight(region_height);
                                    System.out.println("Width: " + region_width + ", Height: " + region_height);
                                }
                                else
                                {
                                    System.out.println("Loading freeform region!\nisFixed: " + false);
                                    FreeFormRegion freeRegion = new FreeFormRegion();
                                    freeRegion.setDeviceType(dataRegion.getDeviceType());
                                    freeRegion.setDisplayColor(dataRegion.getDisplayColor());
                                    freeRegion.setEnabled(dataRegion.getEnabled());
                                    videoPlayers.get(i).replaceRegion(dataRegion, freeRegion);
                                    replaceGraphVideoRegion(dataRegion, freeRegion);
                                    freeRegion.setCoordX(region_x);
                                    freeRegion.setCoordY(region_y);
                                    System.out.println("X: " + region_x + ", Y: " + region_y);

                                    //Determine the number of points associated with the free-form region
                                    query = "SELECT COUNT(*) FROM point WHERE region_id=? AND video_player_id=? ORDER BY ROWID";
                                    statement = configDBConnection.prepareStatement(query);
                                    statement.setInt(1, regionId);
                                    statement.setInt(2, i);
                                    int vertices = statement.executeQuery().getInt(1);

                                    //Load points associated with current free-form region
                                    query = "SELECT * FROM point WHERE region_id=? AND video_player_id=? ORDER BY ROWID";
                                    statement = configDBConnection.prepareStatement(query);
                                    statement.setInt(1, regionId);
                                    statement.setInt(2, i);
                                    ResultSet pointSet = statement.executeQuery();
                                    int[] xVert = new int[vertices];
                                    int[] yVert = new int[vertices];
                                    System.out.println("vertexCount: " + vertices);
                                    int v = 0;
                                    while(pointSet.next())
                                    {
                                        xVert[v] = pointSet.getInt(3);
                                        yVert[v] = pointSet.getInt(4);
                                        ++v;
                                    }
                                    freeRegion.setVertices(xVert, yVert);
                                    System.out.println("X-Vertices: " + Arrays.toString(xVert));
                                    System.out.println("Y-Vertices: " + Arrays.toString(yVert));
                                }
                            }
                        }
                        configDBConnection.commit();
                    }
                    catch(SQLException e)
                    {
                        e.printStackTrace();
                    }

                    dbDisconnect(configDBConnection);
                }
                else
                {
                    System.out.println("Database file corrupted and unable to load data from file.");
                }
            }
            //File loaded is an old config file
            else
            {
                Vector<GraphPanel> graph_panels = dw.getGraphPanels();

                try
                {
                    DataInputStream is = new DataInputStream(new FileInputStream(config_file));

                    //increaseGraphOffset is required to ensure that the dataWindow text box updates properly,
                    //but the offset first needs to be cleared so repeated openings don't stack.
                    setGraphOffset(0);

                    //Check if this config file has a header.
                    int configID = is.readInt();

                    //If the first int is not equal to the header ID, this file predates the addition of the header or isn't a config file.
                    if(configID != HEADER_ID)
                    {
                        System.out.println("Config file does not start with header ID!");
                        int result = JOptionPane.showConfirmDialog(dw, "Config file does not have a valid header. This could be because it is an older format, or because it is not a config file.\nAttempt to load anyway?", "Load config file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                        if(result == JOptionPane.NO_OPTION)
                        {
                            System.out.println("User chose not to load config file");
                            return;
                        }

                        //Recreate data input stream so that we start over from byte 0. This is necessary because DataInputStream does not support the reset function.
                        is.close();
                        is = new DataInputStream(new FileInputStream(config_file));
                    }
                    else
                    {
                        //Check version
                        configVersion = is.readInt();
                        System.out.println("Config Version: " + configVersion);
                        if(configVersion > HEADER_CURRENT_VERSION)
                        {
                            System.out.println("Config version " + configVersion + " greater than max supported version " + HEADER_CURRENT_VERSION);
                            int result = JOptionPane.showConfirmDialog(dw, "Config file version is newer than this version of VideoSync supports.\nAttempt to load anyway?", "Load config file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                            if(result == JOptionPane.NO_OPTION)
                            {
                                System.out.println("User chose not to load config file");
                                return;
                            }
                        }
                    }

                    increaseGraphOffset(is.readInt());

                    setGraphWidthIndex(is.readInt());

                    //Check if save file has actual color data.
                    //If the remaining bytes are 276, this config file was written back when we had indices to hard-coded colors.
                    boolean containsRawColor = (is.available() != 276);
                    System.out.println("Remaining config file bytes: " + is.available());
                    for(GraphPanel graph_panel : graph_panels)
                    {
                        //Retrieve values for data display from config file
                        boolean enableCheckbox = is.readBoolean();
                        boolean showInVideo = is.readBoolean();
                        int deviceIndex = is.readInt();
                        int channelIndex = is.readInt();

                        //Only apply data display configuration if a data file is loaded
                        if(dataLoaded)
                        {
                            graph_panel.setCheckboxEnable(enableCheckbox);
                            graph_panel.setShowInVideo(showInVideo);
                            graph_panel.setDeviceIndex(deviceIndex);
                            graph_panel.setChannelIndex(channelIndex);
                        }
                        else
                        {
                            System.out.println("Data not loaded, skipping config file graph panel information");
                        }

                        //Retrieve graph color.
                        if(containsRawColor)
                        {
                            //Config file has individual RGB values. Load separately to ensure they load in proper order.
                            int red = is.readInt();
                            int green = is.readInt();
                            int blue = is.readInt();
                            graph_panel.setColor(new Color(red, green, blue));
                        }
                        else
                        {
                            //Config file uses old index to hard-coded colors.
                            int colorIndex = is.readInt();
                            switch(colorIndex)
                            {
                                case 0:
                                    graph_panel.setColor(Color.BLACK);
                                    break;
                                case 1:
                                    graph_panel.setColor(Color.RED);
                                    break;
                                case 2:
                                    graph_panel.setColor(Color.GREEN);
                                    break;
                                case 3:
                                    graph_panel.setColor(Color.YELLOW);
                                    break;
                                case 4:
                                    graph_panel.setColor(Color.BLUE);
                                    break;
                                case 5:
                                    graph_panel.setColor(Color.MAGENTA);
                                    break;
                                case 6:
                                    graph_panel.setColor(Color.PINK);
                                    break;
                                case 7:
                                    graph_panel.setColor(Color.ORANGE);
                                    break;
                                case 8:
                                    graph_panel.setColor(Color.CYAN);
                                    break;
                                default:
                                    System.out.println("Unrecognized graph color index " + colorIndex + " in config file");
                                    graph_panel.setColor(Color.BLACK);
                            }
                        }
                    }

                    //Load the video(es) state
                    setCurrentPosition(is.readLong(), true);    // May cause trouble if view is not updated
                    int size = is.readInt();        //number of video files
                    for(int i = 0; i < size; i++)
                    {
                        videoPlayers.get(i).setOffsetText(is.readLong());
                        videoPlayers.get(i).setBounds(is.readInt(), is.readInt(), is.readInt(), is.readInt());

                        //Load video region state
                        Vector<Region> videoRegions = videoPlayers.get(i).getRegions();
                        for(Region dataRegion : videoRegions)
                        {
                            int coordX = is.readInt();
                            int coordY = is.readInt();

                            boolean isFixed = true;
                            // Config file contains a boolean value representing region type in version 2 and onwards
                            if(configVersion >= 2)
                                isFixed = is.readBoolean();

                            if(isFixed)
                            {
                                // Region is fixed
                                // Load/set the region coordinates, width, and height
                                System.out.println("Loading fixed region!\nisFixed: " + true);
                                FixedRegion fixedRegion = (FixedRegion) dataRegion;

                                fixedRegion.setCoordX(coordX);
                                fixedRegion.setCoordY(coordY);
                                System.out.println("X: " + coordX + ", Y: " + coordY);
                                int width = is.readInt();
                                int height = is.readInt();
                                fixedRegion.setWidth(width);
                                fixedRegion.setHeight(height);
                                System.out.println("Width: " + width + ", Height: " + height);
                            }
                            else
                            {
                                // Region is free form
                                // Create new free form region object to replace auto-instantiated fixed region
                                System.out.println("Loading freeform region!\nisFixed: " + false);
                                FreeFormRegion freeRegion = new FreeFormRegion();

                                // Copy data from original region object
                                freeRegion.setChip(dataRegion.getChip());
                                freeRegion.setPin(dataRegion.getPin());
                                freeRegion.setDeviceType(dataRegion.getDeviceType());
                                freeRegion.setDisplayColor(dataRegion.getDisplayColor());
                                freeRegion.setEnabled(dataRegion.getEnabled());

                                // Remove dataRegion from the dataRegions vector in DirectVideoRenderPanel (through
                                // VideoPlayer) and add freeRegion instead
                                videoPlayers.get(i).replaceRegion(dataRegion, freeRegion);
                                // Replace video region reference in graph panels with new one
                                replaceGraphVideoRegion(dataRegion, freeRegion);

                                // Load/set video region coordinates, vertex count, and vertex position information
                                freeRegion.setCoordX(coordX);
                                freeRegion.setCoordY(coordY);
                                System.out.println("X: " + coordX + ", Y: " + coordY);

                                int vertexCount = is.readInt();
                                System.out.println("vertexCount: " + vertexCount);

                                int[] xVert = new int[vertexCount];
                                int[] yVert = new int[vertexCount];
                                for(int x = 0; x < vertexCount; x++)
                                {
                                    xVert[x] = is.readInt();
                                    yVert[x] = is.readInt();
                                }
                                freeRegion.setVertices(xVert, yVert);
                                System.out.println("X-Vertices: " + Arrays.toString(xVert));
                                System.out.println("Y-Vertices: " + Arrays.toString(yVert));
                            }
                            System.out.println();
                        }
                    }

                    //If config format is updated in the future, do version checks here to see if newer fields can be added.

                    is.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
        {
            createNewConfigDBFile();
        }
    }

    /**
     * Set the config file field to the given config file
     *
     * @param config configuration file to set
     */
    public void setConfigFile(File config)
    {
        config_file = config;
    }

    /**
     * Whether this Data Model has been shut down by performShutdownOperations.
     *
     * @return boolean value representing whether the DataModel has been shutdown already.
     */
    public boolean getIsShutdown()
    {
        return isShutDown;
    }

    /**
     * Loops through all video players in DataModel and sets their isSaved flag to true.
     */
    public void setVideoPlayersSaved()
    {
        for(VideoPlayer vp : videoPlayers)
            vp.setSaved(true);
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
        Color color = new Color(0, 0, 0);
        for(GraphPanel panel : dw.getGraphPanels())
        {
            if(panel.getCurrChannelChip() == chip && panel.getCurrChannelPin() == pin)
            {
                color = panel.getColor();
            }
        }

        return color;
    }

    /**
     * Method writes all data associated with VideoSync config to config database file
     * TODO: Marked for future refactorization - generalize method functionality
     */
    @SuppressWarnings("JpaQueryApiInspection")
    public void writeConfigDBFile()
    {
        if(config_file != null)
        {
            Connection configDBConnection = dbConnect(config_file);
            if(configDBConnection != null)
            {
                boolean valid = verifyConfigDatabaseTables();
                if(valid)
                {
                    clearConfigDBTables();
                    try
                    {
                        configDBConnection.setAutoCommit(false);

                        //Write into metadata table
                        String statement = "INSERT INTO metadata(header_id, header_version, current_position, graph_offset, graph_width) " +
                                "VALUES(?,?,?,?,?)";
                        PreparedStatement preparedStatement = configDBConnection.prepareStatement(statement);
                        preparedStatement.setInt(1, HEADER_ID);
                        preparedStatement.setInt(2, HEADER_CURRENT_VERSION);
                        preparedStatement.setLong(3, getCurrentPosition());
                        preparedStatement.setInt(4, getGraphOffset());
                        preparedStatement.setInt(5, getGraphWidthIndex());
                        preparedStatement.execute();

                        //Write into graph_panel table
                        Vector<GraphPanel> graph_panels = dw.getGraphPanels();
                        statement = "INSERT INTO graph_panel(graph_id, enabled, displayed, device_index, channel_index, red, green, blue) " +
                                    "VALUES(?,?,?,?,?,?,?,?)";
                        preparedStatement = configDBConnection.prepareStatement(statement);
                        for(int i = 0; i < graph_panels.size(); i++)
                        {
                            GraphPanel gp = graph_panels.get(i);
                            Color panelColor = gp.getColor();
                            preparedStatement.setInt(1, i);
                            preparedStatement.setBoolean(2, gp.isCheckboxSelected());
                            preparedStatement.setBoolean(3, gp.isShowInVideoSelected());
                            preparedStatement.setInt(4, gp.getDeviceIndex());
                            preparedStatement.setInt(5, gp.getChannelIndex());
                            preparedStatement.setInt(6, panelColor.getRed());
                            preparedStatement.setInt(7, panelColor.getGreen());
                            preparedStatement.setInt(8, panelColor.getBlue());
                            preparedStatement.addBatch();
                        }
                        preparedStatement.executeBatch();

                        //Write into video_player table
                        statement = "INSERT INTO video_player(video_player_id, x, y, width, height, offset) " +
                                "VALUES(?,?,?,?,?,?)";
                        preparedStatement = configDBConnection.prepareStatement(statement);
                        for(int i = 0; i < videoPlayers.size(); i++)
                        {

                            VideoPlayer videoPlayer = videoPlayers.get(i);
                            // Make sure we skip saving data for unsaved video players
                            if(!videoPlayer.isSaved())
                            {
                                System.out.println("Skipping unsaved video player");
                                continue;
                            }

                            preparedStatement.setInt(1, i);
                            preparedStatement.setInt(2, videoPlayer.getX());
                            preparedStatement.setInt(3, videoPlayer.getY());
                            preparedStatement.setInt(4, videoPlayer.getWidth());
                            preparedStatement.setInt(5, videoPlayer.getHeight());
                            preparedStatement.setLong(6, videoPlayer.getOffset());
                            preparedStatement.execute();

                            //Save video region state
                            // SQL statement for saving regions
                            String vpStatement = "INSERT INTO region(region_id, video_player_id, fixed, x, y, width, height) " +
                                                 "VALUES(?,?,?,?,?,?,?)";
                            PreparedStatement vpPreparedStatement = configDBConnection.prepareStatement(vpStatement);

                            // SQL statement for saving free region points
                            String pointStatement = "INSERT INTO point(region_id, video_player_id, x, y) VALUES (?,?,?,?)";
                            PreparedStatement pointPreparedStatement = configDBConnection.prepareStatement(pointStatement);

                            Vector<Region> videoRegions = videoPlayer.getRegions();
                            for(int j = 0; j < videoRegions.size(); j++)
                            {
                                Region dataRegion = videoRegions.get(j);
                                vpPreparedStatement.setInt(1, j);
                                vpPreparedStatement.setInt(2, i);
                                vpPreparedStatement.setInt(4, dataRegion.getCoordX());
                                vpPreparedStatement.setInt(5, dataRegion.getCoordY());

                                // Two types of regions possible
                                if(dataRegion instanceof FixedRegion)
                                {
                                    // Fixed Region
                                    // Saving isFixed flag, height, and width
                                    System.out.println("Saving a fixed region!");
                                    System.out.println("X: " + dataRegion.getCoordX() + ", Y: " + dataRegion.getCoordY());
                                    FixedRegion fixedRegion = (FixedRegion) dataRegion;
                                    vpPreparedStatement.setBoolean(3, true);
                                    vpPreparedStatement.setInt(6, fixedRegion.getWidth());
                                    vpPreparedStatement.setInt(7, fixedRegion.getHeight());
                                    vpPreparedStatement.addBatch();
                                    System.out.println("isFixed: " + true);
                                    System.out.println("Width: " + fixedRegion.getWidth() + ", Height: " + fixedRegion.getHeight());
                                }
                                else if(dataRegion instanceof FreeFormRegion)
                                {

                                    // Freeform Region
                                    // Saving isFixed flag, vertex count, and vertex position coordinates
                                    System.out.println("Saving a freeform region!");
                                    System.out.println("X: " + dataRegion.getCoordX() + ", Y: " + dataRegion.getCoordY());
                                    FreeFormRegion freeRegion = (FreeFormRegion) dataRegion;

                                    vpPreparedStatement.setBoolean(3, false);
                                    vpPreparedStatement.setObject(6, null);
                                    vpPreparedStatement.setObject(7, null);
                                    vpPreparedStatement.addBatch();
                                    System.out.println("isFixed: " + false);

                                    //Write into point table
                                    for(Point p : freeRegion)
                                    {
                                        pointPreparedStatement.setInt(1, j);
                                        pointPreparedStatement.setInt(2, i);
                                        pointPreparedStatement.setInt(3, (int) p.getX());
                                        pointPreparedStatement.setInt(4, (int) p.getY());
                                        pointPreparedStatement.addBatch();
                                    }
                                }
                                System.out.println();
                            }
                            preparedStatement.executeBatch();
                            vpPreparedStatement.executeBatch();
                            pointPreparedStatement.executeBatch();
                        }
                        configDBConnection.commit();
                        dbDisconnect(configDBConnection);
                    }
                    catch(SQLException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    System.out.println("Database file corrupted and unable to load data from file.");
                }

                dbDisconnect(configDBConnection);
            }
            else
            {
                System.out.println("Error connecting to " + config_file);
            }
        }
    }

    /**
     * Clears table entries for an existing config database file
     *
     */
    public void clearConfigDBTables()
    {
        Connection configDBConnection = dbConnect(config_file);
        try
        {
            PreparedStatement statement;

            //Query statement base
            String queryBase = "DELETE FROM ";

            //Delete point table
            statement = configDBConnection.prepareStatement(queryBase + "point;");
            statement.execute();

            //Delete region table
            statement = configDBConnection.prepareStatement(queryBase + "region;");
            statement.execute();

            //Delete video player table
            statement = configDBConnection.prepareStatement(queryBase + "video_player;");
            statement.execute();

            //Delete metadata table
            statement = configDBConnection.prepareStatement(queryBase + "metadata;");
            statement.execute();

            //Delete graph panel table
            statement = configDBConnection.prepareStatement(queryBase + "graph_panel;");
            statement.execute();

            System.out.println("Cleared config database table entries.");
        }
        catch(SQLException e)
        {
            System.out.println(e.getMessage());
        }

        dbDisconnect(configDBConnection);
    }

    /**
     * Creates a new config SQLite database file
     * TODO: Marked for future refactorization - generalize method functionality
     */
    public void createNewConfigDBFile()
    {
        config_file = new File(getCurrentDirectory() + File.separator + "config.db");
        try
        {

            //Connect to database
            Connection configDBConnection = dbConnect(config_file);
            if(configDBConnection != null)
            {

                // FIXME: Pretty sure this is a messed up way of creating SQL queries...
                //Query statement base
                String queryBase = "CREATE TABLE IF NOT EXISTS ";

                //Metadata table creation
                String metadata = "metadata(" +
                        "header_id integer PRIMARY KEY NOT NULL, " +
                        "header_version integer, " +
                        "current_position long NOT NULL, " +
                        "graph_offset integer NOT NULL, " +
                        "graph_width integer NOT NULL);";

                //Graph panel table creation
                String graph_panel = "graph_panel(" +
                        "graph_id integer PRIMARY KEY NOT NULL, " +
                        "enabled boolean NOT NULL," +
                        "displayed boolean NOT NULL, " +
                        "device_index integer NOT NULL, " +
                        "channel_index integer NOT NULL," +
                        "red integer NOT NULL, " +
                        "green integer NOT NULL, " +
                        "blue integer NOT NULL);";

                //Video player table creation
                String video_player = "video_player(" +
                        "video_player_id integer PRIMARY KEY NOT NULL," +
                        "x integer NOT NULL, " +
                        "y integer NOT NULL," +
                        "width integer NOT NULL," +
                        "height integer NOT NULL," +
                        "offset integer NOT NULL);";

                //Region table creation
                String region = "region(" +
                        "region_id integer NOT NULL," +
                        "video_player_id integer NOT NULL, " +
                        "fixed boolean NOT NULL, x integer NOT NULL," +
                        "y integer NOT NULL," +
                        "width integer," +
                        "height integer, " +
                        "PRIMARY KEY(region_id, video_player_id), " +
                        "FOREIGN KEY(video_player_id) REFERENCES video_player(video_player_id) ON DELETE CASCADE);";

                //Point table creation
                String point = "point(" +
                        "region_id integer NOT NULL, " +
                        "video_player_id integer NOT NULL, " +
                        "x integer NOT NULL, " +
                        "y integer NOT NULL," +
                        "PRIMARY KEY (region_id, video_player_id, x, y), " +
                        "FOREIGN KEY(region_id, video_player_id) REFERENCES region(region_id, video_player_id) ON DELETE CASCADE);";


                //Execute metadata query
                PreparedStatement statement;
                statement = configDBConnection.prepareStatement(queryBase + metadata);
                statement.execute();

                //Execute graph panel query
                statement = configDBConnection.prepareStatement(queryBase + graph_panel);
                statement.execute();

                //Execute video player query
                statement = configDBConnection.prepareStatement(queryBase + video_player);
                statement.execute();

                //Execute region query
                statement = configDBConnection.prepareStatement(queryBase + region);
                statement.execute();

                //Execute point query
                statement = configDBConnection.prepareStatement(queryBase + point);
                statement.execute();


                //Verify if tables have been successfully implemented in the database file
                boolean creationSuccess = verifyConfigDatabaseTables();
                if(creationSuccess)
                    System.out.println("Initialization of new config database successful.");
                else
                    System.out.println("Initialization of new config database unsuccessful.");


                //Disconnect from database
                dbDisconnect(configDBConnection);
            }
            else
                System.out.println("Error encountered. Config database not created.");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Checks to see if config database file contains the tables
     * TODO: Marked for future refactorization - generalize method functionality
     */
    public boolean verifyConfigDatabaseTables()
    {
        Connection configDBConnection = dbConnect(config_file);

        try
        {
            //Query statement base
            String queryBase = "SELECT name FROM sqlite_master WHERE type='table' AND name=";
            PreparedStatement statement;
            ResultSet resultSet;

            //Used to collect missing tables, if any
            Vector<String> missingTables = new Vector<>();

            //Verify metadata table has been created
            statement = configDBConnection.prepareStatement(queryBase + "'metadata'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("metadata");
            }

            //Verify graph_panel table has been created
            statement = configDBConnection.prepareStatement(queryBase + "'graph_panel'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("graph_panel");
            }

            //Verify point table has been created
            statement = configDBConnection.prepareStatement(queryBase + "'point'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("point");
            }

            //Verify region table has been created
            statement = configDBConnection.prepareStatement(queryBase + "'region'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("region");
            }

            //Verify video_player table has been created
            statement = configDBConnection.prepareStatement(queryBase + "'video_player'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("video_player");
            }

            dbDisconnect(configDBConnection);

            StringBuilder message = new StringBuilder();

            //If there is something in the missingTables list, then the table listed was not successfully created
            if(!missingTables.isEmpty())
            {
                message.append("The following tables are not found in the database: ");
                for(String table : missingTables)
                {
                    message.append(table).append("\t\t");
                }
                message.append("\nDatabase file failed config database verification.");
            }
            else
                message.append("Database file passed config database verification.");
            System.out.println(message);

            if(!missingTables.isEmpty()) return false;
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Connects to a SQLite database file and ensures that foreign keys are enforced in session
     *
     * @param db : Holds the filepath of the database file
     */
    public Connection dbConnect(File db)
    {
        try
        {
            String url = "jdbc:sqlite:" + db.getPath();
            Class.forName("org.sqlite.JDBC");
            SQLiteConfig sqLiteConfig = new SQLiteConfig();
            sqLiteConfig.enforceForeignKeys(true);
            Connection connection = DriverManager.getConnection(url, sqLiteConfig.toProperties());
            System.out.println("Connection to SQLite database file " + db.getPath() + " established.");
            return connection;
        }
        catch(SQLException | ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
        return null;
    }

    /**
     * Disconnects from a SQLite database file
     *
     * @param conn : Holds the filepath of the database file
     */
    public void dbDisconnect(Connection conn)
    {
        try
        {
            conn.close();
            System.out.println("Successfully closed connection to SQLite database file");
        }
        catch(SQLException e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Resets data window to reflect eight graph panels
     */
    public void resetGraphsPane()
    {
        dw.resetGraphsPane();
        notifyObservers("Reset");
    }

    /**
     * Removes one video region associated with a graph panel
     */
    public void removeVideoRegion(GraphPanel graphPanel)
    {
        for(VideoPlayer videoPlayer : videoPlayers)
        {
            Vector<Region> videoRegions = graphPanel.getVideoRegions();
            for(Region videoRegion : videoRegions)
            {
                videoPlayer.removeRegion(videoRegion);
            }
        }
        notifyObservers();
    }

    /**
     * Returns the data window
     *
     * @return Reference to DataWindow object
     */
    public DataWindow getDataWindow()
    {
        return dw;
    }

    /**
     * Creates a new event log file
     *
     * @param eventLogFile eventLogFile created by the user
     */
    public void newEventLog(File eventLogFile)
    {

        //Generate the channel count collections for a C1 Input Map File
        if(c1InputMap != null)
        {
            channelCountCollection = new HashMap<>();
            eventTags = new HashMap<>();
            hotkeyMaps = new HashMap<>();
            metricsMap = new HashMap<>();

            initializeMetricTemplates();

            //Create a Channel Count object for each entry in the device input map using chip and pin numbers
            for(DeviceInputMap dim : c1InputMap)
            {

                //Acquire the list of pin maps associated with the chip key
                Map<Integer, ChannelCount> pinMaps;

                //If the chip is already in the channel count collection map, get the list of pin maps associated with that chip
                if(channelCountCollection.containsKey(dim.getChipNumber()))
                {
                    pinMaps = channelCountCollection.get(dim.getChipNumber());
                }

                //...otherwise, create a new pin map list to associate with the chip
                else
                {
                    pinMaps = new HashMap<>();
                }

                //System.out.println("Pin Map Key Set: " + pinMap.keySet());

                pinMaps.put(dim.getPinNumber(), new ChannelCount());

                channelCountCollection.put(dim.getChipNumber(), pinMaps);

                //System.out.println("Channel Count Collection Key Set :" + channelCountCollection.keySet());
            }
        }

        this.eventLogFile = eventLogFile;
        this.logNeverSaved = true;

        System.out.println("New Event Log File Created: " + eventLogFile.getName());

        setChanged();
        notifyObservers("New Event Log");
    }


    /**
     * Adds a new event to the channel associated with the chip and pin
     *
     * @param chip            chip associated with channel
     * @param pin             pin associated with channel
     * @param timestamp_value playback timestamp
     * @param offset_value    graph/video offset
     * @param eventTag        event tag associated with event
     * @param comment         comment associated with event
     * @param omitted         omission flag associated with event
     */
    public void addEvent(int chip, int pin, int timestamp_value, int offset_value, String eventTag, String comment, boolean omitted)
    {
        //Acquire the pin maps associated to the chip number
        Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);
        ChannelCount updated = null;

        //Acquire the channel count associated to the pin number, if any
        if(pinMaps.containsKey(pin))
        {
            updated = pinMaps.get(pin);
        }

        //Add a new event to the channel count
        if(updated != null)
        {
            updated.addEvent(timestamp_value, offset_value, eventTag, comment, omitted);
            pinMaps.put(pin, updated);
            channelCountCollection.put(chip, pinMaps);

            printChannelEvents(chip, pin);

            //Notify observers that a new event has been added
            setChanged();
            notifyObservers("Add Event");
        }
    }

    /**
     * Removes an event associated to a channel
     *
     * @param eventProxy proxy event
     */
    public void removeEvent(EventProxy eventProxy)
    {

        //Acquire the channel count associated to the chip number
        ChannelCount channelCount = channelCountCollection.get(eventProxy.getChip()).get(eventProxy.getPin());

        //Remove the event
        channelCount.removeEvent(new Event(eventProxy.getTimestamp(), eventProxy.getOffset(), eventProxy.getComment(), eventProxy.isOmitted()));

        //Notify observers that an event has been removed
        setChanged();
        notifyObservers("Remove Event");
    }

    /**
     * Prints all the events associated to a channel
     *
     * @param chip chip associated with channel
     * @param pin  pin associated with channel
     */
    private void printChannelEvents(int chip, int pin)
    {

        //Acquire the pin maps associated to the chip number
        Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);
        ChannelCount updated = null;

        //Acquire the channel count associated to the pin number, if any
        if(pinMaps.containsKey(pin))
        {
            updated = pinMaps.get(pin);
        }

        //Print the channel events of the channel count
        if(updated != null)
        {
            System.out.println(updated);
        }
    }

    /**
     * Returns the event log file
     *
     * @return event log file
     */
    public File getEventLogFile()
    {
        return eventLogFile;
    }

    /**
     * Returns a list of all events for each channel
     *
     * @return a list of all events
     */
    public Map<String, List<ChannelCountProxy>> getAllEventsByDetectorType()
    {

        //Create a map, whose key is the detector type
        Map<String, List<ChannelCountProxy>> channelCountProxiesByDetector = new HashMap<>();

        //Use the C1 input map to determine the detector type associated to each channel
        Vector<DeviceInputMap> inputMaps = getC1InputMap();

        //Create a map entry for all detector types - excluding null or detector types with the label 'Select Type'
        for(DeviceInputMap inputMap : inputMaps)
        {
            if(inputMap.getDetectorType() != null && !inputMap.getDetectorType().equals("Select Type"))
            {
                channelCountProxiesByDetector.put(inputMap.getDetectorType(), new Vector<>());
            }
        }

        //Populate the list of channel count proxies for each detector type
        for(int i = 0; i < inputMaps.size(); i++)
        {
            List<ChannelCountProxy> channelCountProxies = channelCountProxiesByDetector.get(inputMaps.get(i).getDetectorType());
            if(channelCountProxies != null)
            {
                channelCountProxies.add(new ChannelCountProxy(inputMaps.get(i).getChannelName(), getChannelCountByInputMapIndex(i)));
                channelCountProxiesByDetector.put(inputMaps.get(i).getDetectorType(), channelCountProxies);
            }
        }

        //Return the list of channel count proxies
        return channelCountProxiesByDetector;
    }

    /**
     * Returns a list of events for a tag for each channel
     *
     * @param eventTag event tag
     * @return all events associated to event tag
     */
    public List<Event> getAllEventsByTag(String eventTag)
    {

        //Create a list for storing all the events
        List<Event> events = new Vector<>();

        //Collect the events for each channel count
        for(Integer chip : channelCountCollection.keySet())
        {
            Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);
            for(Integer pin : pinMaps.keySet())
            {
                if(pinMaps.get(pin).getEventsByClassification(eventTag) != null)
                    events.addAll(pinMaps.get(pin).getEventsByClassification(eventTag));
            }
        }

        //Return the list of events
        return events;
    }

    /**
     * Removes all events associated to an event tag that is to be removed
     *
     * @param eventTag tag associated to the events to be removed
     */
    public void removeAllEventsByEventTag(String eventTag)
    {
        for(Integer chip : channelCountCollection.keySet())
        {
            Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);
            for(Integer pin : pinMaps.keySet())
            {
                pinMaps.get(pin).removeAllEventsByClassification(eventTag);
            }
        }

        //Notify the observers that all the events that are associated to a tag have been removed, allowing the tag itself to be removed
        setChanged();
        notifyObservers("Remove Tag");
    }

    /**
     * Updates a channel's car count
     *
     * @param mode  value to either increment or decrement a car count
     * @param index index value associated to the index position of a device input map in the list of input maps
     */
    public void updateChannelCarCount(int mode, int index)
    {
        //Get the channel count associated to the device input map with the given index
        ChannelCount channelCount = getChannelCountByInputMapIndex(index);

        //Increment the car count
        if(mode == 1 && channelCount != null)
        {
            channelCount.incCarCount();
        }
        //...or decrement the car count
        else if(mode == 0 && channelCount != null)
        {
            channelCount.decCarCount();
        }

        //Sets the channel count by input map index
        setChannelCountByInputMapIndex(index, channelCount);

        printChannelCarCountByInputMapIndex(index);

        //Notify the observers that the car count of a channel has been updated
        setChanged();
        notifyObservers("Update Count");
    }

    public void setChannelCarCount(int count, int index)
    {
        ChannelCount channelCount = getChannelCountByInputMapIndex(index);
        channelCount.setCarCount(count);

        //Sets the channel count by input map index
        setChannelCountByInputMapIndex(index, channelCount);

        printChannelCarCountByInputMapIndex(index);

        //Notify the observers that the car count of a channel has been updated
        setChanged();
        notifyObservers("Update Count");
    }

    /**
     * Gets the channel count associated to the index position of a device input map in the list of input maps
     *
     * @param index index value of the device input map in the list of input maps
     * @return returns the channel count associated to the index position of a device input map in the list of input maps
     */
    public ChannelCount getChannelCountByInputMapIndex(int index)
    {

        int chip = c1InputMap.get(index).getChipNumber();
        int pin = c1InputMap.get(index).getPinNumber();

        ChannelCount channelCount = null;

        //Get the pin maps associated to the chip number
        Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);

        //Get the channel count associated to the pin number
        if(pinMaps.containsKey(pin))
        {
            channelCount = pinMaps.get(pin);
        }

        //Return the channel count
        return channelCount;
    }

    /**
     * Sets the channel count by input map index
     *
     * @param index        index value to indicate the position of a device input map in a list of input maps
     * @param channelCount channel count to be modified
     */
    private void setChannelCountByInputMapIndex(int index, ChannelCount channelCount)
    {
        int chip = c1InputMap.get(index).getChipNumber();
        int pin = c1InputMap.get(index).getPinNumber();

        //Get the pin maps associated to a chip number
        Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);

        //Associate the channel count to the pin
        if(pinMaps.containsKey(pin))
        {
            pinMaps.put(pin, channelCount);
        }

        //Put the updated channel count in th
        channelCountCollection.put(chip, pinMaps);
    }

    /**
     * Prints the channel car count by the input map index
     *
     * @param index index value to indicate the position of a device input map in a list of input maps
     */
    private void printChannelCarCountByInputMapIndex(int index)
    {
        int chip = c1InputMap.get(index).getChipNumber();
        int pin = c1InputMap.get(index).getPinNumber();

        //Acquire the pin maps associated to the chip number
        Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);

        //Print the car count associated to the pin number
        if(pinMaps.containsKey(pin))
        {
            ChannelCount channelCount = pinMaps.get(pin);
            System.out.println("Car Count: " + channelCount.getCarCount());
        }
    }

    /**
     * Add a tag for labeling events in Event Logger
     *
     * @param tagName        name of tag
     * @param tagDescription description of tag
     */
    public void addTag(String tagName, String tagDescription)
    {

        //Add the tag to the events tag map, or update the description of an existing tag
        eventTags.put(tagName, tagDescription);
        System.out.println(tagName + " event tag added");

        //Notify the observers that an event tag has been added, or edited
        setChanged();
        notifyObservers("Add Tag");
    }

    /**
     * Remove a tag
     *
     * @param tagID name of tag
     */
    public void removeTag(String tagID)
    {

        //Remove the tag from the event tags map
        eventTags.remove(tagID);
        System.out.println(tagID + " event tag removed");

        //Notify the observers that an event tag has been removed
        setChanged();
        notifyObservers("Remove Tag");
    }

    /**
     * Get the event tags map
     *
     * @return reference to event tags map
     */
    public Map<String, String> getEventTags()
    {
        return eventTags;
    }

    /**
     * Sets the hotkey maps for quickly updating channel car counts for channels associated to them
     *
     * @param hotkeyMaps map of hotkeys
     */
    public void setHotkeyMaps(Map<Integer, List<Object>> hotkeyMaps)
    {

        //Set the data model attribute to reference the new hotkey maps
        this.hotkeyMaps = hotkeyMaps;

        for(Integer key : hotkeyMaps.keySet())
        {
            List<Object> hotkeyMapAttributes = hotkeyMaps.get(key);
            System.out.println("Number Key " + key + " mapped to channel: " + ((Integer) hotkeyMapAttributes.get(0) == 0 ? "Not Applicable" : c1InputMap.get((Integer) hotkeyMapAttributes.get(0) - 1).getChannelName()));
        }

        //Notify the observers that the hotkey maps have been assigned
        notifyObservers();
    }

    /**
     * Gets the reference to hotkey maps
     *
     * @return the hotkey maps
     */
    public Map<Integer, List<Object>> getHotkeyMaps()
    {
        return hotkeyMaps;
    }

    /**
     * Update the hotkey mode
     *
     * @param mode true enables the automatic incrementation/decrementation of channel car counts upon pressing a hotkey
     */
    public void setHotkeyUpdateMode(boolean mode)
    {
        this.hotkeyUpdateMode = mode;

        System.out.println("Update-Count-On-Hotkey Mode Enabled: " + (hotkeyUpdateMode ? "On" : "Off"));

        //Notify the observers that the automatic incrementation/decrementation of channel car counts has been enabled/disabled
        notifyObservers();
    }

    /**
     * Returns the hotkey update mode
     *
     * @return hotkey mode
     */
    public boolean getHotkeyUpdateMode()
    {
        return hotkeyUpdateMode;
    }

    /**
     * Returns the channel count collection
     *
     * @return reference to the channel count collection
     */
    public Map<Integer, Map<Integer, ChannelCount>> getChannelCountCollection()
    {
        return channelCountCollection;
    }

    /**
     * Get the device input map associated to device type, chip and pin
     *
     * @param deviceType device type associated to a device input map
     * @param chip       chip number associated to a device input map
     * @param pin        pin number associated to a device input map
     * @return returns device input map, or null
     */
    public DeviceInputMap getInputMapByDeviceChipAndPin(EDeviceType deviceType, int chip, int pin)
    {
        if(deviceType == EDeviceType.DEVICE_C1)
        {
            for(DeviceInputMap deviceInputMap : c1InputMap)
            {
                if(deviceInputMap.getChipNumber() == chip && deviceInputMap.getPinNumber() == pin)
                {
                    return deviceInputMap;
                }
            }
        }
        return null;
    }

    /**
     * Creates tables needed for event log database
     * TODO: Marked for future refactorization - generalize method functionality
     */
    public void createNewEventLogDBFile()
    {
        try
        {
            Connection eventLogDBConnection = dbConnect(eventLogFile);
            eventLogDBConnection.setAutoCommit(false);
            if(eventLogDBConnection != null)
            {
                //Query statement base
                String queryBase = "CREATE TABLE IF NOT EXISTS ";

                String metadata = "metadata(" +
                        "version_id integer NOT NULL," +
                        "log_UUID string NOT NULL," +
                        "PRIMARY KEY(version_id));";

                //Input Map table creation
                String input_map = "input_map(" +
                        "chip integer NOT NULL," +
                        "pin integer NOT NULL," +
                        "PRIMARY KEY(chip, pin));";

                //Event Tag table creation
                String event_tag = "event_tag(" +
                        "tag text NOT NULL," +
                        "description text," +
                        "PRIMARY KEY(tag));";

                //Channel Count table creation
                String channel_count = "channel_count(" +
                        "chip integer NOT NULL," +
                        "pin integer NOT NULL," +
                        "car_count integer NOT NULL," +
                        "omitted boolean NOT NULL," +
                        "PRIMARY KEY(chip, pin, car_count, omitted)," +
                        "FOREIGN KEY(chip, pin) REFERENCES input_map(chip, pin) ON DELETE CASCADE);";

                //Event table creation
                String event = "event(" +
                        "event_id integer PRIMARY KEY AUTOINCREMENT," +
                        "chip integer NOT NULL," +
                        "pin integer NOT NULL," +
                        "tag text NOT NULL," +
                        "timestamp_ms integer NOT NULL," +
                        "offset_ms integer NOT NULL," +
                        "comment text," +
                        "omitted boolean NOT NULL," +
                        "FOREIGN KEY(chip, pin) REFERENCES input_map(chip, pin) ON DELETE CASCADE," +
                        "FOREIGN KEY(tag) REFERENCES event_tag(tag) ON DELETE CASCADE);";

                //Execute metadata query
                PreparedStatement statement;
                statement = eventLogDBConnection.prepareStatement(queryBase + metadata);
                statement.execute();

                //Execute input map query
                statement = eventLogDBConnection.prepareStatement(queryBase + input_map);
                statement.execute();

                //Execute event tag query
                statement = eventLogDBConnection.prepareStatement(queryBase + event_tag);
                statement.execute();

                //Execute channel count query
                statement = eventLogDBConnection.prepareStatement(queryBase + channel_count);
                statement.execute();

                //Execute event query
                statement = eventLogDBConnection.prepareStatement(queryBase + event);
                statement.execute();

                // Commit changes
                eventLogDBConnection.commit();
                // Disconnect from database
                dbDisconnect(eventLogDBConnection);

                boolean valid = verifyEventLogDatabaseTables();

                if(valid)
                {
                    System.out.println("Initialization of new event log database successful.");
                }
                else
                {
                    System.out.println("Initialization of new event log database unsuccessful.");
                }
            }
            else
            {
                System.out.println("Error encountered. Config database not created.");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean verifyEventLogDatabaseTables()
    {
        Connection eventLogDBConnection = dbConnect(eventLogFile);
        boolean success = verifyEventLogDatabaseTablesHelper(eventLogDBConnection);
        dbDisconnect(eventLogDBConnection);
        return success;
    }

    public boolean verifyEventLogDatabaseTables(File dbFile)
    {
        Connection eventLogDBConnection = dbConnect(dbFile);
        boolean success = verifyEventLogDatabaseTablesHelper(eventLogDBConnection);
        dbDisconnect(eventLogDBConnection);
        return success;
    }

    /**
     * Checks to see if event log database file contains the tables
     * TODO: Marked for future refactorization - generalize method functionality
     */
    private boolean verifyEventLogDatabaseTablesHelper(Connection eventLogDBConnection)
    {
        try
        {
            //Query statement base
            String queryBase = "SELECT name FROM sqlite_master WHERE type='table' AND name=";
            PreparedStatement statement;
            ResultSet resultSet;

            //Used to collect missing tables, if any
            Vector<String> missingTables = new Vector<>();

            //Verify metadata table has been created
            statement = eventLogDBConnection.prepareStatement(queryBase + "'metadata'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("metadata");
            }

            //Verify metadata table has been created
            statement = eventLogDBConnection.prepareStatement(queryBase + "'input_map'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("input_map");
            }

            //Verify graph_panel table has been created
            statement = eventLogDBConnection.prepareStatement(queryBase + "'channel_count'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("channel_count");
            }

            //Verify point table has been created
            statement = eventLogDBConnection.prepareStatement(queryBase + "'event_tag'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("event_tag");
            }

            //Verify region table has been created
            statement = eventLogDBConnection.prepareStatement(queryBase + "'event'");
            resultSet = statement.executeQuery();
            if(resultSet.isClosed())
            {
                missingTables.add("event");
            }

            StringBuilder message = new StringBuilder();

            //If there is something in the missingTables list, then the table listed was not successfully created
            if(!missingTables.isEmpty())
            {
                message.append("The following tables are not found in the database: ");
                for(String table : missingTables)
                {
                    message.append(table).append("\t\t");
                }
                message.append("\nDatabase file failed event log database verification.");
            }
            else
                message.append("Database file passed event database verification.");
            System.out.println(message);
            if(!missingTables.isEmpty()) return false;
        }
        catch(SQLException e)
        {
            System.out.println("Database file failed event log database verification.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Write all recorded events and channel counts to event log database file
     */
    public void writeEventLogDBFile()
    {

        //Create a new event log file
        createNewEventLogDBFile();

        //Once event log db file has been created, insert the data into the database
        if(eventLogFile != null)
        {
            Connection eventLogDBConnection = dbConnect(eventLogFile);
            if(eventLogDBConnection != null)
            {
                try
                {
                    eventLogDBConnection.setAutoCommit(false);

                    boolean valid = verifyEventLogDatabaseTables();
                    if(valid)
                    {

                        clearEventLogDatabaseTableEntries();

                        String statement;
                        PreparedStatement preparedStatement;

                        //Write metadata
                        statement = "INSERT INTO metadata(version_id, log_UUID) " +
                                    "VALUES (?, ?)";
                        preparedStatement = eventLogDBConnection.prepareStatement(statement);
                        preparedStatement.setInt(1, EVENT_LOG_DATABASE_VERSION);
                        preparedStatement.setString(2, eventLogUUID.toString());
                        preparedStatement.execute();

                        //Write input map chip and pins
                        statement = "INSERT INTO input_map(chip, pin) " +
                                    "VALUES (?, ?)";
                        preparedStatement = eventLogDBConnection.prepareStatement(statement);
                        for(DeviceInputMap deviceInputMap : c1InputMap)
                        {
                            preparedStatement.setInt(1, deviceInputMap.getChipNumber());
                            preparedStatement.setInt(2, deviceInputMap.getPinNumber());
                            preparedStatement.addBatch();
                        }
                        preparedStatement.executeBatch();

                        statement = "INSERT INTO event_tag(tag, description) " +
                                    "VALUES (?, ?)";
                        preparedStatement = eventLogDBConnection.prepareStatement(statement);
                        //Write event tags
                        for(String key : eventTags.keySet())
                        {
                            preparedStatement.setString(1, key);
                            preparedStatement.setString(2, eventTags.get(key));
                            preparedStatement.addBatch();
                        }
                        preparedStatement.executeBatch();

                        //Write channel count and events
                        for(Integer chip : channelCountCollection.keySet())
                        {
                            Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(chip);
                            for(Integer pin : pinMaps.keySet())
                            {

                                ChannelCount channelCount = pinMaps.get(pin);

                                //Write channel count
                                statement = "INSERT INTO channel_count(chip, pin, car_count, omitted) " +
                                        "VALUES(?, ?, ?, ?)";
                                preparedStatement = eventLogDBConnection.prepareStatement(statement);
                                preparedStatement.setInt(1, chip);
                                preparedStatement.setInt(2, pin);
                                preparedStatement.setInt(3, channelCount.getCarCount());
                                preparedStatement.setBoolean(4, channelCount.isOmitted());
                                preparedStatement.execute();

                                String eventStatement = "INSERT INTO event(chip, pin, tag, timestamp_ms, offset_ms, comment, omitted)" +
                                                        "VALUES(?, ?, ?, ?, ?, ?, ?)";
                                PreparedStatement eventPreparedStatement = eventLogDBConnection.prepareStatement(eventStatement);
                                //Write events
                                for(String tag : eventTags.keySet())
                                {
                                    List<Event> events = channelCount.getEventsByClassification(tag);
                                    System.out.println(events);
                                    if(events != null)
                                    {
                                        for(Event event : events)
                                        {
                                            eventPreparedStatement.setInt(1, chip);
                                            eventPreparedStatement.setInt(2, pin);
                                            eventPreparedStatement.setString(3, tag);
                                            eventPreparedStatement.setInt(4, event.getTimestamp());
                                            eventPreparedStatement.setInt(5, event.getOffset());
                                            eventPreparedStatement.setString(6, event.getComment());
                                            eventPreparedStatement.setBoolean(7, event.isOmitted());
                                            eventPreparedStatement.addBatch();
                                        }
                                        eventPreparedStatement.executeBatch();
                                    }
                                }
                            }
                        }

                        eventLogDBConnection.commit();
                        dbDisconnect(eventLogDBConnection);

                        System.out.println("Events and channel counts written to event log database successfully.");
                    }
                    else
                    {
                        System.out.println("Error encountered. Program did not save events in event log database.");
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Clears the event log database table entries
     *
     */
    public void clearEventLogDatabaseTableEntries()
    {
        Connection eventLogDBConnection = dbConnect(eventLogFile);

        try
        {
            PreparedStatement statement;

            // FIXME: Pretty sure this is a messed up way of creating SQL queries...
            //Query statement base
            String queryBase = "DELETE FROM ";

            //Delete point table
            statement = eventLogDBConnection.prepareStatement(queryBase + "metadata;");
            statement.execute();

            //Delete region table
            statement = eventLogDBConnection.prepareStatement(queryBase + "input_map;");
            statement.execute();

            //Delete video player table
            statement = eventLogDBConnection.prepareStatement(queryBase + "channel_count;");
            statement.execute();

            //Delete metadata table
            statement = eventLogDBConnection.prepareStatement(queryBase + "event_tag;");
            statement.execute();

            //Delete graph panel table
            statement = eventLogDBConnection.prepareStatement(queryBase + "event;");
            statement.execute();

            System.out.println("Cleared event log database table entries.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        dbDisconnect(eventLogDBConnection);
    }

    /**
     * Opens an event log database file
     *
     * @param eventLogFile file reference to event log database file
     */
    public void openEventLog(File eventLogFile)
    {
        //Connect to the database file
        Connection eventLogDBConnection = dbConnect(eventLogFile);

        //If there is a connection a database file...
        if(eventLogDBConnection != null)
        {
            try
            {
                //Verify the event log database
                boolean valid = verifyEventLogDatabaseTables(eventLogFile);

                //If valid...
                if(valid)
                {

                    //Set the event log file
                    this.eventLogFile = eventLogFile;
                    this.logNeverSaved = false;

                    String statement;
                    PreparedStatement preparedStatement;
                    ResultSet resultSet;

                    // Load UUID
                    statement = "SELECT log_UUID FROM metadata LIMIT 1";
                    preparedStatement = eventLogDBConnection.prepareStatement(statement);
                    resultSet = preparedStatement.executeQuery();

                    if(resultSet.next())
                    {
                        eventLogUUID = UUID.fromString(resultSet.getString("log_UUID"));
                    }

                    //Load event tags
                    eventTags = new HashMap<>();
                    statement = "SELECT * FROM event_tag;";
                    preparedStatement = eventLogDBConnection.prepareStatement(statement);
                    resultSet = preparedStatement.executeQuery();

                    while(resultSet.next())
                    {
                        String eventTag = resultSet.getString(1);
                        String eventTag_description = resultSet.getString(2);

                        eventTags.put(eventTag, eventTag_description);
                    }

                    //Load channel counts
                    channelCountCollection = new HashMap<>();
                    for(DeviceInputMap deviceInputMap : c1InputMap)
                    {

                        Map<Integer, ChannelCount> pinMaps = channelCountCollection.get(deviceInputMap.getChipNumber());
                        if(pinMaps == null)
                        {
                            pinMaps = new HashMap<>();
                        }

                        ResultSet channelCount_resultSet;
                        statement = "SELECT * FROM channel_count " +
                                "WHERE chip == " + deviceInputMap.getChipNumber() +
                                " AND " +
                                "pin == " + deviceInputMap.getPinNumber() +
                                ";";
                        preparedStatement = eventLogDBConnection.prepareStatement(statement);
                        channelCount_resultSet = preparedStatement.executeQuery();

                        ChannelCount channelCount = new ChannelCount();

                        //Load events for each channel
                        if(!channelCount_resultSet.isClosed())
                        {
                            channelCount.setCarCount(channelCount_resultSet.getInt(3));
                            channelCount.setOmitted(channelCount_resultSet.getBoolean(4));

                            statement = "SELECT * FROM event " +
                                    "WHERE chip == " + deviceInputMap.getChipNumber() +
                                    " AND " +
                                    "pin == " + deviceInputMap.getPinNumber() +
                                    ";";
                            preparedStatement = eventLogDBConnection.prepareStatement(statement);
                            ResultSet event_resultSet = preparedStatement.executeQuery();

                            while(event_resultSet.next())
                            {
                                String tag = event_resultSet.getString(4);
                                int timestamp = event_resultSet.getInt(5);
                                int offset = event_resultSet.getInt(6);
                                String comment = event_resultSet.getString(7);
                                boolean omitted = event_resultSet.getBoolean(8);
                                channelCount.addEvent(timestamp, offset, tag, comment, omitted);
                            }
                        }
                        else
                        {
                            channelCount.setCarCount(0);
                            channelCount.setOmitted(false);
                        }

                        //Associate the populated channel count to the pin number
                        pinMaps.put(deviceInputMap.getPinNumber(), channelCount);

                        //Associate the pin maps associated to the chip number
                        channelCountCollection.put(deviceInputMap.getChipNumber(), pinMaps);
                    }

                    //Disconnect from the database
                    dbDisconnect(eventLogDBConnection);

                    //Initialize metric templates
                    metricsMap = new HashMap<>();
                    initializeMetricTemplates();

                    //Notify the observers that an event log has been loaded
                    setChanged();
                    notifyObservers("Open Event Log");

                }
                else
                {
                    System.out.println("Error. Could not load database file in to Event Logger.");
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("Error. Could not connect to database file.");
        }
    }

    /**
     * Sets the event log file
     *
     * @param eventLogFile event log database file
     */
    public void setEventLogFile(File eventLogFile)
    {
        this.eventLogFile = eventLogFile;

        notifyObservers();
    }

    public boolean getLogNeverSaved()
    {
        return logNeverSaved;
    }

    public boolean unsavedChangesExist()
    {
        return unsavedChanges;
    }

    public void setUnsavedChanges(boolean changes)
    {
        unsavedChanges = changes;
    }

    public void setLogNeverSaved(boolean neverSaved)
    {
        this.logNeverSaved = neverSaved;
    }

    /**
     * Sets the video playback timestamp to the timestamp associated to an event
     *
     * @param timestamp   time stamp in which the event had occurred
     * @param graphOffset offset in which the event had occurred
     */
    public void jumpToEventTimestamp(int timestamp, int graphOffset)
    {

        //Pause the video file
        setPlaying(false);

        //Set the timestamp
        setCurrentPosition(timestamp, true);

        //Set the graph offset
        setGraphOffset(graphOffset);

        //Play the video file
        //setPlaying(true);

        //Notify the observers that the timestamp of the video file has been set to the timestamp and offset associated ot an event
        setChanged();
        notifyObservers("Jump");

    }

    /**
     * Adds a metric to be tracked
     *
     * @param metricName name to a metric
     * @param metric     actual metric object to perform calculations
     */
    public void addMetric(String metricName, Metric metric)
    {

        //Put metric in metrics map
        metricsMap.put(metricName, metric);

        System.out.println(metricName + " metric added.");

        //Notify the observers that a metric has been added to be tracked
        setChanged();
        notifyObservers("Add Metric");
    }

    /**
     * Removes a metric that was tracked
     *
     * @param metricName name of metric
     */
    public void removeMetric(String metricName)
    {

        //Removes a metric
        metricsMap.remove(metricName);

        System.out.println(metricName + " metric removed.");

        //Notify the observers that a tracked metric is removed
        setChanged();
        notifyObservers("Remove Metric");
    }

    /**
     * Get metric object associated to the name of the metric
     *
     * @param metricName name of metric
     * @return actual metric object
     */
    public Metric getMetric(String metricName)
    {
        return metricsMap.get(metricName);
    }

    /**
     * Get metrics map
     *
     * @return metrics map
     */
    public Map<String, Metric> getMetrics()
    {
        return metricsMap;
    }

    /**
     * Get all metrics associated to an event tag
     *
     * @param eventTag event tag
     * @return list of all metrics containing an event tag as one of its variables
     */
    public List<String> getMetricsAssociatedByTag(String eventTag)
    {
        List<String> associatedMetrics = new Vector<>();
        for(String metric : metricsMap.keySet())
        {
            if(metricsMap.get(metric).getVariables().containsKey(eventTag))
            {
                associatedMetrics.add(metric);
            }
        }
        return associatedMetrics;
    }

    /**
     * Remove all metrics associated to an event tag
     *
     * @param eventTag event tag
     */
    public void removeAllMetricsByTag(String eventTag)
    {
        List<String> metrics_to_delete = new Vector<>();

        //Check each tracked metric if they contain the event tag as one of its variables
        for(String metric : metricsMap.keySet())
        {
            if(metricsMap.get(metric).getVariables().containsKey(eventTag))
            {
                metrics_to_delete.add(metric);
            }
        }

        //Delete the metrics associated to the event tag
        for(String metric : metrics_to_delete)
        {
            metricsMap.remove(metric);
        }

        //Notify the observers that a metric was removed
        setChanged();
        notifyObservers("Remove Metric");
    }

    /**
     * Initialize the template metrics for the user to select in the metrics modal of Event Logger
     */
    public void initializeMetricTemplates()
    {
        metricsTemplateMap = new HashMap<>();
        metricsTemplateMap.put("Sensitivity", new SensitivityMetric());
    }

    /**
     * Get the list of all template metrics
     *
     * @return map of metric templates
     */
    public Map<String, Metric> getTemplateMetrics()
    {
        return metricsTemplateMap;
    }

    /**
     * Get the metric template associated to the metric name
     *
     * @param metric_name name of metric template
     * @return actual metric object
     */
    public Metric getTemplateMetric(String metric_name)
    {
        return metricsTemplateMap.get(metric_name);
    }

    /**
     * Generates a CSV file detailing all recorded events, channel counts and metric results
     *
     * @param csvFile file reference to CSV file
     * @throws IOException throws IOException in case file does not exist
     */
    public void generateEventLogCSV(File csvFile) throws IOException
    {

        CSVWriter writer = new CSVWriter(new FileWriter(csvFile.getPath()));

        Map<String, List<ChannelCountProxy>> channelCountProxiesByDetector = new HashMap<>();
        Vector<DeviceInputMap> inputMaps = getC1InputMap();

        for(DeviceInputMap inputMap : inputMaps)
        {
            if(inputMap.getDetectorType() != null && !inputMap.getDetectorType().equals("Select Type"))
            {
                channelCountProxiesByDetector.put(inputMap.getDetectorType(), new Vector<>());
            }
        }

        for(int i = 0; i < inputMaps.size(); i++)
        {
            List<ChannelCountProxy> channelCountProxies = channelCountProxiesByDetector.get(inputMaps.get(i).getDetectorType());
            if(channelCountProxies != null)
            {
                channelCountProxies.add(new ChannelCountProxy(inputMaps.get(i).getChannelName(), getChannelCountByInputMapIndex(i)));
                channelCountProxiesByDetector.put(inputMaps.get(i).getDetectorType(), channelCountProxies);
            }
        }

        //Write Channel Input Maps
        writer.writeNext(new String[]{"Channel Input Mappings"});
        writer.writeNext(new String[]{"Chip", "Pin", "Channel Name"});
        for(Integer chip : channelCountCollection.keySet())
        {
            for(Integer pin : channelCountCollection.get(chip).keySet())
            {
                writer.writeNext(new String[]{Integer.toString(chip), Integer.toString(pin), getInputMapByDeviceChipAndPin(EDeviceType.DEVICE_C1, chip, pin).getChannelName()});
            }
        }
        writer.writeNext(new String[]{""});

        if(!eventTags.isEmpty())
        {
            Map<ChannelCountProxy, Map<String, Integer>> channelMetrics = new HashMap<>();

            for(String detector : channelCountProxiesByDetector.keySet())
            {
                List<ChannelCountProxy> channelCountProxies = channelCountProxiesByDetector.get(detector);
                for(ChannelCountProxy channelCountProxy : channelCountProxies)
                {
                    Map<String, Integer> variableValues = new HashMap<>();

                    //If there are tracked metrics, the metric may introduce variables that the user did not add as event tags, so add all the variable values associated.
                    if(!metricsMap.isEmpty())
                    {
                        for(String metric_name : metricsMap.keySet())
                        {
                            Metric metric = metricsMap.get(metric_name);
                            metric.calculate(channelCountProxy);
                            for(String variable : metric.getVariables().keySet())
                            {
                                variableValues.put(variable, (Integer) metric.getVariableValue(variable));
                            }
                        }
                    }
                    //This loop adds event tags not associated to metrics
                    for(String eventTag : eventTags.keySet())
                    {
                        if(!variableValues.containsKey(eventTag))
                        {
                            variableValues.put(eventTag, channelCountProxy.getEventsByTag(eventTag) == null ? 0 : channelCountProxy.getEventsByTag(eventTag).size());
                        }
                    }
                    channelMetrics.put(channelCountProxy, variableValues);
                }
            }

            //Metric Descriptions
            writer.writeNext(new String[]{"Metrics"});
            writer.writeNext(new String[]{"Metric Name", "Description"});
            for(String metric_name : metricsMap.keySet())
            {
                writer.writeNext(new String[]{metric_name, metricsMap.get(metric_name).getDescription()});
            }
            writer.writeNext(new String[]{""});

            //Event Tag Descriptions
            writer.writeNext(new String[]{"Event Tags"});
            writer.writeNext(new String[]{"Tag Name", "Description"});
            for(String eventTag : eventTags.keySet())
            {
                writer.writeNext(new String[]{eventTag, eventTags.get(eventTag)});
            }
            writer.writeNext(new String[]{""});

            //Adds metrics for each detector, if any
            if(!metricsMap.isEmpty())
            {
                writer.writeNext(new String[]{"Detector Metrics"});
                for(String detector : channelCountProxiesByDetector.keySet())
                {
                    writer.writeNext(new String[]{detector});
                    for(String metric_name : metricsMap.keySet())
                    {
                        Map<String, Object> variableValues = new HashMap<>();
                        Metric metric = metricsMap.get(metric_name);
                        for(String variable : metric.getVariables().keySet())
                        {
                            variableValues.put(variable, 0);
                        }
                        for(ChannelCountProxy channelCountProxy : channelCountProxiesByDetector.get(detector))
                        {
                            if(!channelCountProxy.isOmitted())
                            {
                                for(String variable : metric.getVariables().keySet())
                                {
                                    int currentValue = channelMetrics.get(channelCountProxy).get(variable) + (Integer) variableValues.get(variable);
                                    variableValues.put(variable, currentValue);
                                }
                            }
                        }
                        Double result = (Double) metric.calculate(variableValues);
                        if(!result.isNaN())
                        {
                            result = BigDecimal.valueOf((Double) metric.calculate(variableValues)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                        }
                        writer.writeNext(new String[]{metric_name, result.isNaN() ? "Not calculated" : result.toString()});
                    }
                }
            }
            writer.writeNext(new String[]{""});

            //Adds metrics for each channel
            writer.writeNext(new String[]{"Channel Metrics by Detector Type"});
            List<String> header = new Vector<>();
            header.add("Channel Name");
            //header.add("Omitted from Metrics");
            header.add("");
            header.addAll(metricsMap.keySet());
            header.add("Car Count");
            header.addAll(eventTags.keySet());
            String[] columnNames = header.toArray(new String[0]);
            for(String detectorType : channelCountProxiesByDetector.keySet())
            {
                writer.writeNext(new String[]{detectorType});
                writer.writeNext(columnNames);
                for(ChannelCountProxy channelCountProxy : channelCountProxiesByDetector.get(detectorType))
                {
                    Map<String, Integer> variableValues = channelMetrics.get(channelCountProxy);
                    List<String> data = new Vector<>();
                    data.add(channelCountProxy.getChannelName());
                    data.add(channelCountProxy.isOmitted() ? "Omitted" : "");
                    if(!channelCountProxy.isOmitted())
                    {
                        for(String metric_name : metricsMap.keySet())
                        {
                            data.add(Double.toString((Double) metricsMap.get(metric_name).calculate(channelCountProxy)));
                        }
                    }
                    data.add(Integer.toString(channelCountProxy.getCarCount()));
                    for(String variable : variableValues.keySet())
                    {
                        data.add(variableValues.get(variable) != null ? Integer.toString(variableValues.get(variable)) : Integer.toString(0));
                    }
                    String[] row = data.toArray(new String[0]);
                    writer.writeNext(row);
                }
            }
            writer.writeNext(new String[]{""});

            //Adds events for each detector
            if(getAllEventsByDetectorType().size() > 0)
            {
                header.clear();
                header.add("Timestamp");
                header.add("Offset");
                header.add("Tag");
                header.add("Description");
                //header.add("Omitted from Metric");
                columnNames = header.toArray(new String[0]);
                writer.writeNext(new String[]{"Events by Detector"});
                for(String detector : channelCountProxiesByDetector.keySet())
                {
                    writer.writeNext(new String[]{detector});
                    for(ChannelCountProxy channelCountProxy : channelCountProxiesByDetector.get(detector))
                    {
                        writer.writeNext(new String[]{channelCountProxy.getChannelName()});
                        writer.writeNext(columnNames);
                        for(String eventTag : eventTags.keySet())
                        {
                            List<Event> events = channelCountProxy.getEventsByTag(eventTag);
                            if(events != null)
                            {
                                for(Event event : events)
                                {
                                    writer.writeNext(new String[]{"'" + VideoPlayer.convertToTimeFormat(event.getTimestamp()), "'" + (event.getOffset() / 1000.0), eventTag, event.getComment(), event.isOmitted() ? "Omitted" : ""}); //Verify offset
                                }
                            }
                        }
                    }
                }
                writer.writeNext(new String[]{""});
            }
        }

        writer.close();
        System.out.println(csvFile.getName() + " generated");
    }

    /**
     * TODO: This method should use chip and pin values instead of detector and channel name to omit channel counts... Refactor when possible
     * Omits a channel count associated to a channel name
     *
     * @param detectorType detector associated to the channel
     * @param channelName  name of channel
     */
    public void omitChannelCount(String detectorType, String channelName)
    {

        for(int i = 0; i < c1InputMap.size(); i++)
        {
            if(c1InputMap.get(i).getDetectorType() != null)
            {
                if(c1InputMap.get(i).getDetectorType().equals(detectorType) &&
                        c1InputMap.get(i).getChannelName().equals(channelName))
                {

                    //Set the channel count to become omitted
                    ChannelCount channelCount = getChannelCountByInputMapIndex(i);
                    channelCount.setOmitted(!channelCount.isOmitted());
                    System.out.println(c1InputMap.get(i).getChannelName() + " is " + (channelCount.isOmitted() ? "omitted" : "not omitted"));
                    break;
                }
            }
        }

        //Notify the observers that a channel count has been omitted
        setChanged();
        notifyObservers("Omitted");
    }

    /**
     * Omits an event
     *
     * @param eventProxy event proxy object acquired from the events table in the logger panel of Event Logger
     */
    public void omitEvent(EventProxy eventProxy)
    {
        //Omit the event from the appropriate channel count
        ChannelCount channelCount = channelCountCollection.get(eventProxy.getChip()).get(eventProxy.getPin());
        channelCount.omitEvent(new Event(eventProxy.getTimestamp(), eventProxy.getOffset(), eventProxy.getComment(), eventProxy.isOmitted()));

        //Notify the observers that an event has been omitted
        setChanged();
        notifyObservers("Omitted");
    }

    public UUID getEventLogUUID()
    {
        return eventLogUUID;
    }

    public void setRandomEventLogUUID()
    {
        eventLogUUID = UUID.randomUUID();
    }

    public boolean checkEventLogDBVersion(File eventLogFile)
    {
        Connection eventLogDBConnection = dbConnect(eventLogFile);

        // I hate doing this, but we need to set the eventLogFile so that the update functions don't throw a null
        // pointer. We can't pass the file into the update function since we're using the Callable interface. The
        // solution to this is writing a separate class that implements Callable and has a setter for passing in the
        // file. I'm not happy with this solution, in particular I do not like how declaring each update method would
        // look. If someone in the future is interested in doing this: https://stackoverflow.com/a/46197290
        this.eventLogFile = eventLogFile;

        // We wrap our SQL statement and return false if there is an exception
        boolean result = true;
        try
        {
            // Fetch the one record in the metadata table
            PreparedStatement statement = eventLogDBConnection.prepareStatement("SELECT version_id FROM metadata LIMIT 1");
            ResultSet resultSet = statement.executeQuery();

            // Check if there is a record in the ResultSet
            int version;
            if(resultSet.next())
            {
                // If yes, get the version_id
                version = resultSet.getInt("version_id");
                dbDisconnect(eventLogDBConnection);

                // Check version against latest and call update methods
                if(version <= 0)
                {
                    // If the version ID record is 0, then the value was null so we return false
                    System.out.println("No version ID record found.");
                    result = false;
                }
                else
                {
                    // Loop through event log update functions starting from the current version of the file until it
                    // is updated to the latest version
                    for(int i = version; i < EVENT_LOG_DATABASE_VERSION; i++)
                    {
                        // We need to do this because the eventLogUpdateFunctions are Callables that return a boolean
                        // value indicating success or failure
                        ExecutorService service = Executors.newSingleThreadExecutor();
                        Future<Boolean> future = service.submit(eventLogUpdateFunctions.get(i));
                        try
                        {
                            result = future.get();
                        }
                        catch(InterruptedException e)
                        {
                            e.printStackTrace();
                            result = false;
                        }
                        catch(ExecutionException e)
                        {
                            e.getCause().printStackTrace();
                            result = false;
                        }

                        service.shutdown();
                    }
                }
            }
            else
            {
                // We return false if there was no record
                result = false;
            }
        }
        catch(SQLException e)
        {
            // We return false if a SQLException occurs as this likely means the file is not an event log db file
            // or is not in the proper format
            dbDisconnect(eventLogDBConnection);
            System.out.println("Error. File is not a proper Event Log DB file.");
            e.printStackTrace();
            result = false;
        }

        this.eventLogFile = null;

        return result;
    }

    /**
     * Updates event log database file from version 1 to version 2
     */
    @SuppressWarnings("JpaQueryApiInspection")
    private boolean eventLogV1ToV2()
    {
        Connection eventLogDBConnection = dbConnect(eventLogFile);
        try
        {
            eventLogDBConnection.setAutoCommit(false);

            String query = "ALTER TABLE metadata ADD log_UUID string NOT NULL DEFAULT 'temp string'";
            PreparedStatement statement;
            statement = eventLogDBConnection.prepareStatement(query);
            statement.execute();

            query = "UPDATE metadata SET log_UUID = ?";
            statement = eventLogDBConnection.prepareStatement(query);
            statement.setString(1, UUID.randomUUID().toString());
            statement.execute();

            eventLogDBConnection.commit();
            dbDisconnect(eventLogDBConnection);
            System.out.println("Event Log database file successfully updated from v1 to v2.");

            return true;
        }
        catch(SQLException e)
        {
            System.out.println("Event Log database file failed update from v1 to v2");
            e.printStackTrace();

            dbDisconnect(eventLogDBConnection);
            return false;
        }
    }

    public ArrayList<Long> getHighStates(int chip, int pin) {
        ArrayList<Long> highStates = new ArrayList<>();
        for(C1Channel c : getC1AnalyzerChannels())
        {
            // If the channel we want matches the current channel, generate the graph objects for it
            if(c.getChip() == chip && c.getPin() == pin)
            {
//              Vector<C1Object> c1States = c.getC1Objects();
                for (C1Object o : c.getC1Objects()) {
                    //TODO 0 is high state
                    if (o.getState() == 0) {
                        highStates.add(o.getMilli());
                    }
                }
            }
        }
        return highStates;
    }
}
