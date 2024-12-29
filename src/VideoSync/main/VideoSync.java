/*
 * ****************************************************************
 * File: 			VideoSync.java
 * Date Created:  	June 5, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			This is the main entry point for VideoSync.
 *                  It creates the Graph, DataModel, and CommandList
 *                  together since they are the minimum classes files
 *                  needed for the program to run.
 * ****************************************************************
 */

package VideoSync.main;

import VideoSync.commands.CommandList;
import VideoSync.models.DataModel;
import VideoSync.objects.LogItem;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings({"rawtypes", "unchecked"})
public class VideoSync
{
    /**
     * Reference to DataWindow object
     */
    private final DataWindow dataWindow;

    /**
     * Reference to DataModel object
     */
    private final DataModel dataModel;

    /**
     * Java architecture the system is using
     */
    private static String javaArchitecture;

    /**
     * Path to VLC installation
     */
    private static String vlcPath;

    /**
     * Entry Point for VideoSync to start
     *
     * @param exitOnClose Whether or not the application should exit when VideoSync itself closes
     */
    public static VideoSync init(Boolean exitOnClose)
    {
        return init(exitOnClose, null);
    }

    /**
     * Entry Point for VideoSync to start
     *
     * @param exitOnClose Whether or not the application should exit when VideoSync itself closes
     * @param args        Command line arguments if ran standalone
     */
    public static VideoSync init(Boolean exitOnClose, @SuppressWarnings("unused") String[] args)
    {
        VideoSync toReturn = null;

        //Check if we are running on an embedded JRE
        if(isRunningOnBundledJava())
        {

            //Check if the included Java is the right architecture
            if(!isJavaCompatible(false))
            {
                System.out.println("VideoSync is running on bundled 32-bit Java, but Windows is 64-bit. Switching to bundled 64-bit Java");

                try
                {
                    //Start VideoSync on the bundled 64-bit Java.
                    Process vs64 = Runtime.getRuntime().exec("JRE\\jre64\\bin\\java -jar VideoSync.jar");
                    printProcessMessages(vs64);

                    System.out.println("64-bit VideoSync has finished. 32-bit VideoSync is exiting.");
                    System.exit(0);
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(null, "Something is wrong with the Java Runtime included with VideoSync. You may need to re-install VideoSync, or manually install 64-Bit Java and run the Jar file directly.", "Unable to start VideoSync", JOptionPane.ERROR_MESSAGE);
                    System.out.println("Unable to start VideoSync on bundled 64-bit JRE");
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        }
        else
        {
            //No embedded Java. Check if Java is compatible, and exit if not.
            if(!isJavaCompatible(true))
            {
                System.exit(1);
            }
        }

        //Check if the program is already running and exit if so.
        //Check for exit on close since this could also be caused by VideoSync Reporter, which itself may launch VideoSync.
        //Do check outside of if to ensure that future calls return true.
        boolean alreadyRunning = isAlreadyRunning();
        if(exitOnClose && alreadyRunning)
        {
            JOptionPane.showMessageDialog(null, "Only one instance of VideoSync or VideoSync Reporter may run at once", "Unable to start VideoSync", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Before presenting any of the views for VideoSync, we need to ensure that all of the preferences and necessary
        // components have been installed & located before they are presented. If they have not been installed or located,
        // we need to either create them, or ask the user to locate them.
        try
        {
            // Get the name of the os for determining the URL patterns that are going to be used.
            String systemType = System.getProperty("os.name");
            javaArchitecture = System.getProperty("sun.arch.data.model");

            String vsLibLoc = System.getProperty("user.dir");
            String vlcLibLoc = "";
            if(systemType.startsWith("Mac"))
            {
                vlcLibLoc = "vlc/darwin/";

                //If mac is detected, do platform specific setup.

                //Use main menu bar. There is a cosmetic bug on the in-window menu bar, this works around it.
                System.setProperty("apple.laf.useScreenMenuBar", "true");

                //Set application name. Needed since we're using main menu bar, don't want to appear as AppMain.
                System.setProperty("apple.awt.application.name", "VideoSync");

                //Set dock icon to Caltrans logo
                //Need to do this without referencing com.apple in code or compiling breaks on other platforms.
                try
                {
                    Image caltransLogo = null;
                    try
                    {
                        //Since main is static, need to use another class to get access to resources.
                        URL logoResourceURL = VideoSync.class.getClassLoader().getResource("logo.png");
                        if(logoResourceURL != null)
                        {
                            caltransLogo = ImageIO.read(logoResourceURL);
                        }
                        else
                        {
                            caltransLogo = ImageIO.read(new File("logo.png"));
                        }
                    }
                    catch(IOException ex)
                    {
                        System.out.println("Failed to load caltrans logo:");
                        ex.printStackTrace();
                    }

                    //Get a reference to the Apple Application object.
                    Object AppleApplicationObject = Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null);
                    //Use the Apple Application object to set the dock icon.
                    AppleApplicationObject.getClass().getMethod("setDockIconImage", Image.class).invoke(AppleApplicationObject, caltransLogo);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            }
            else if(systemType.startsWith("Windows"))
            {
                //vlcLibLoc = "C:\\Program Files\\VideoLAN\\VLC\\";
                if(is64BitWindows())
                {
                    vlcLibLoc = "vlc\\win64\\";
                }
                else
                {
                    vlcLibLoc = "vlc\\win32\\";
                }
            }
            else if(systemType.startsWith("Linux"))
            {
                //vlcLibLoc = "/usr/lib/";
                vlcLibLoc = "vlc/linux/";
            }

            // Stores the final location of VLC - this gets passed to the VideoPlayer via the DataModel
            // so that it can be loaded with the NativeLibraries
            File vlcFile;

            // Temporarily stores the VideoSync preferences file so we can write the location of the VLC installation
            File prefsFile = new File(vsLibLoc + File.separator + "videosync.pref");

            // Stores the location of the log's directory so that any log files that get written out can be stored
            //File logsFile = new File(vsLibLoc + File.separator + "logs" + File.separator);

            // Starts up the LoggerThread so that we can easily log any messages that might help in diagnosing problems while in testing.
            //LoggerThread lt = new LoggerThread("LoggerThread", logsFile.getPath() + File.separator);
            // Start the Logging Thread so it may begin receiving log files.
            //lt.start();
            //sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "VideoSync Library found at " + vsLibLoc, "Notice"));

            //Only set vlc path if it isn't already set.
            //This allows the Reporter component to bring up VideoSync more than once without extracting libraries after the first time
            if(vlcPath == null)
            {
                // Temporarily stores the path of the VLC installation so it can be written to the prefs file.
                vlcPath = isVLCInstalled(systemType);
            }
            // Verify that there is a preferences file.
            // If there is one, we'll read the location of the VLC installation and store it into vlcPath for use
            if(prefsFile.exists())
            {
                vlcPath = new BufferedReader(new FileReader(prefsFile)).readLine();
            }

            // Check to make sure we have a valid vlcPath and and that the path exists.
            // If it does, we can continue on with as no other checking needs to be done.
            // If it either of the two fail, we need to request input from the user to correct the issue.

            if(vlcPath == null || !(new File(vlcPath)).exists())
            {
                // Reset the vlcPath to null since it may be incorrect.
                vlcPath = null;

                // Request the user to select their VLC installation. The result gets stored into VLC
                vlcFile = handleNoVLCInstall();

                // Update the preferences file so that it may be used the next time VideoSync runs
                // If we failed to create the preferences file, send a notice to the log manager.
                if(!createPrefsFile(prefsFile, vlcFile.toString(), vlcLibLoc))
                {
                    //sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "Failed to create VideoSync Preferences File at " + prefsFile.getPath(), "ERROR"));
                }
            }

            //System.out.println("Found VLC path \"" + vlcPath + "\"");
            // Create a new instance of VideoSync
            toReturn = new VideoSync(vlcPath, exitOnClose);
        }
        catch(FileNotFoundException e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));

            sendMessageToLogManager(new LogItem("Errors", Calendar.getInstance().getTime().toString(), e.getMessage() + " - Stack Trace: " + e, "ERROR"));
        }
        catch(IOException e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));

            //sendMessageToLogManager(new LogItem("Errors", Calendar.getInstance().getTime().toString(), e.getMessage() + " - Stack Trace: " + e, "ERROR"));
        }
        catch(SecurityException se)
        {
            se.printStackTrace();
        }

        return toReturn;
    }

    /**
     * Entry Point for VideoSync to start standalone
     *
     * @param args Command line arguments if ran standalone
     */
    public static void main(String[] args)
    {
        //Start VideoSync
        final VideoSync mainVS = init(true);

        //FIXME: On Mac, calling Quit from the application menu instead of the File menu causes the shutdown hook to run.

        //Create a shutdown hook so if the OS requests VideoSync to quit, it can do so cleanly.
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try
            {
                //Only attempt to save configuration if the data model reference hasn't been garbage collected and if
                //it wasn't closed by the user.
                if(mainVS != null && mainVS.getDataModel() != null && !mainVS.getDataModel().getIsShutdown())
                {
                    System.out.println("Shutdown hook caught an unexpected exit.");
                    //Write config file instead of calling performShutdownOperations as VLCJ seems to have trouble
                    //with releasing resources from other threads.
                    mainVS.getDataModel().writeConfigFile();
                }

                //Wait for main thread to complete.
                mainThread.join();

            }
            catch(InterruptedException ex)
            {
                ex.printStackTrace();
            }
        }));
    }

    // -- VideoSync Construction

    /**
     * Creates VideoSync allocating the DataModel, Graph, and Command List
     */
    private VideoSync(String vlcPath, Boolean exitOnClose)
    {
        dataModel = new DataModel(vlcPath, exitOnClose);
        dataWindow = new DataWindow(dataModel);

        CommandList cl = new CommandList(dataModel, dataWindow);

        dataWindow.setCommands(cl);
    }

    // -- VideoSync Startup Methods

    /**
     * Creates the preferences file for storing the VLC installation location.
     *
     * @param prefsFile Preferences file that stores location of VLC and VLC Lib
     * @param vlcLoc    Path to VLC
     * @param vlcLib    Path to VLC Lib
     * @return Boolean value indicating success of failure
     */

    private static boolean createPrefsFile(File prefsFile, String vlcLoc, String vlcLib)
    {
        try
        {
            // Ensure that the Library file has been created.
            // If not attempt to create it.
            // IF it cannot be created, return false.
            if(!prefsFile.getParentFile().exists())
            {
                if(!prefsFile.getParentFile().mkdir())
                {
                    sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "Failed to create VideoSync Preferences File at " + prefsFile.getAbsolutePath(), "Notice"));

                    return false;
                }
            }

            // Create a FileWriter object so we can write the prefs file
            FileWriter fw = new FileWriter(prefsFile);

            // Write the VLC Location & VLC Library to the preferences file.
            fw.write(String.format("%s\n%s\n", vlcLoc, vlcLib));

            // Close the FileWriter for the prefs file.
            fw.close();

            // Write the system information so we it can potentially be used for debugging information
            return writeSystemInfo(new File(prefsFile.getParent() + "/system.info"));
        }
        catch(IOException e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));

            sendMessageToLogManager(new LogItem("Errors", Calendar.getInstance().getTime().toString(), e.getMessage() + " - Stack Trace: " + e, "ERROR"));

            return false;
        }
    }

    /**
     * Write the system information to a file so it can be included in error reporting
     *
     * @param sysFile Path where to write system file
     * @return Boolean value indicating success or failure
     */

    private static boolean writeSystemInfo(File sysFile)
    {
        sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "Writing System Information at " + sysFile.getAbsolutePath(), "Notice"));

        try
        {
            FileWriter fw = new FileWriter(sysFile);
            fw.write("OS Name:\t\t\t" + System.getProperty("os.name") + "\n");
            fw.write("OS Version:\t\t\t" + System.getProperty("os.version") + "\n");
            fw.write("OS Architecture:\t" + System.getProperty("os.arch") + "\n");
            fw.write("Java Class Path:\t" + System.getProperty("java.class.path") + "\n");
            fw.write("Java Home: \t\t\t" + System.getProperty("java.home") + "\n");
            fw.write("Java Vendor:\t\t" + System.getProperty("java.vendor") + "\n");
            fw.write("Java Version:\t\t" + System.getProperty("java.version") + "\n");
            fw.write(" ----- System Information ------\n");
            fw.write("Number of Cores: \t\t\t\t" + Runtime.getRuntime().availableProcessors() + "\n");
            long maxMemory = Runtime.getRuntime().maxMemory();
            fw.write("Maximum memory for JVM (bytes):\t" + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory) + "\n");
            fw.write("Total Memory for JVM (bytes):\t" + Runtime.getRuntime().totalMemory() + "\n");
            fw.close();
        }
        catch(IOException e)
        {
            sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "Failed to write System Information at " + sysFile.getAbsolutePath(), "Notice"));

            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));

            sendMessageToLogManager(new LogItem("Errors", Calendar.getInstance().getTime().toString(), e.getMessage() + " - Stack Trace: " + e, "ERROR"));

            // Because we had an error, we need to return false.
            return false;
        }

        // Return true indicating success
        return true;
    }

    /**
     * Checks to make sure that VLC is installed and able to be found. This method only runs if its
     * the first time VideoSync has been run.
     *
     * @param systemType Operating system this program is running on
     * @return String representing the path to where VLC can be found
     */

    public static String isVLCInstalled(String systemType)
    {
        // Used for indexing a directory so it can be used to search for a pre-installed version of VLC
        // Will speed up execution times on windows systems.
        File applicationDirectory;
        // If we can't use a pre-installed version of VLC, we extract the necessary file from the jar.
        String extractPath;
        // The path to the VLC libraries or executable.
        String vlcPath = null;

        // Open the default directories with the correct URI's for each type of OS
        if(systemType.startsWith("Mac"))
        {
            //applicationDirectory = new File("/Applications");
            //vlcPath = "/Applications/VLC copy.app/Contents/MacOS/lib/";
            //uk.co.caprica.vlcj.binding.LibC.INSTANCE.setenv("VLC_PLUGIN_PATH", "/Applications/VLC copy.app/Contents/MacOS/plugins", 1);

            //vlcPath = "vlc/darwin/";
            //vlcPath = "vlc/darwin/VLC.app/Contents/MacOS/lib/";
            //uk.co.caprica.vlcj.binding.LibC.INSTANCE.setenv("VLC_PLUGIN_PATH", "vlc/darwin/VLC.app/Contents/MacOS/plugins", 1);

            //Store libraries in the system temp directory.
            //If they fail to delete, they're at least out of the user's way.
            //extractPath = System.getProperty("java.io.tmpdir") + "/VideoSync/";
            vlcPath = extractVLCLibs(systemType, getTempDir()) + "VLC.app/Contents/MacOS/lib";
        }
        else if(systemType.startsWith("Windows"))
        {
            //applicationDirectory = new File("C:\\Program Files\\");
            vlcPath = "C:\\Program Files\\VideoLAN\\VLC\\";

            //Use pre-installed
            if(!(new File(vlcPath)).exists())
            {
                //Set VLC lib directory to AppData instead of Temp on Windows so we don't trip any installed Antivirus
                extractPath = getTempDir() + "libs\\"; //System.getenv("APPDATA") + "VideoSync\\libs\\";
                vlcPath = extractVLCLibs(systemType, extractPath);
            }

        }
        else if(systemType.startsWith("Linux"))
        {
            //Since linux .so files are distro-specific and heavily symlinked to local libraries, require that VLC be installed on this platform.
            System.out.println("Not extracting libs on Linux, assuming VLC is installed...");

            //According to the Linux Foundation Filesystem Hierarchy Standard, non-essential programs accessible to
            //all users must be in /usr/bin.
            applicationDirectory = new File("/usr/bin");

            // Store all the applications into a temporary array.
            File[] apps = applicationDirectory.listFiles();

            if(apps != null)
            {
                // Iterate through all of the Applications found and search for VLC
                for(File app : apps)
                {
                    if(app.isFile())
                    {
                        // If the name of the application starts with VLC, we can stop searching
                        // and return the file for use in main();
                        if(app.getName().toUpperCase().startsWith("VLC"))
                        {
                            vlcPath = app.getParent();
                        }
                    }
                }
            }

            //vlcPath = "vlc/linux";

            //Store libraries in the system temp directory
            //If they fail to delete, they're at least out of the user's way.
            //extractPath = System.getProperty("java.io.tmpdir") + "/VideoSync/libs/";
            //vlcPath = extractVLCLibs(systemType, extractPath);

            //Needed if running out of a jar, causes problems in IDE.
            //vlcPath += "linux/";
        }
        else
        {
            System.out.println("Unknown operating system....");
        }

        // If we did not find a valid file, we return null.
        return vlcPath;
    }

    /**
     * Handles the user interaction for dealing with No Valid VLC installation
     *
     * @return File object in the case the user selected their VLC installation, null otherwise
     */
    //TODO - Is this needed anymore, now that VLC libs are bundled? Seems we can safely remove this and eliminate an old todo.
    private static File handleNoVLCInstall()
    {
        try
        {
            String[] options = {"Download VLC", "Select Installation", "Quit VideoSync"};

            // Show an option pane and get the result of their input.
            // Because JOptionPane requires a parent component to display the alert, we just create an empty JFrame so it will be displayed.
            int n = JOptionPane.showOptionDialog(new JFrame(),
                    "We were unable to find your VLC Installation. What would you like to do?",
                    "VLC Needed", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

            // User wants to download VideoSync
            if(n == JOptionPane.YES_OPTION)
            {
                // Notify the user that they are being redirected to VLC's home page so they can install VLC.
                JOptionPane.showMessageDialog(new JFrame(), "You will be redirected to VLC's download page.\nPlease download the " + javaArchitecture + "bit version of VLC and install it to your root directory (i.e. C:\\).\nWhen the installation is complete, restart VideoSync");

                // Open the default web browser to install vlc
                Desktop.getDesktop().browse(new URI("https://www.videolan.org/vlc/"));

                sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "User is downloading VLC from the web", "Notice"));

                // Sleep for 2 seconds to allow the log files to be finished writing and then exit.
                Thread.sleep(2000);
                System.exit(1);
            }
            // User wants to Select their VLC Installation
            else if(n == JOptionPane.NO_OPTION)
            {
                sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "User is selecting their VLC Installation.", "Notice"));

                // Present a JFileChooser so the user may select their VLC installation.
                JFileChooser fc = new JFileChooser();

                // Allow Files to be selected
                // TODO: NOTE - This may need to change depending on OS
                // 			- Mac applications are "files", while Windows applications should be a folder location and not the .exe
                //Mac applications are "files" from Java's point of view. Windows and Linux will require the directory containing the file.
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                if(fc.showOpenDialog(new JFrame()) == JFileChooser.APPROVE_OPTION)
                {
                    // Return the file the user selected.
                    return fc.getSelectedFile();
                }
            }
            // User wants to quit
            else
            {
                sendMessageToLogManager(new LogItem("Startup", Calendar.getInstance().getTime().toString(), "User elected not to install VLC. Quitting VideoSync", "Notice"));

                // Sleep for 2 seconds to allow the log files to be finished writing and then exit.
                Thread.sleep(2000);
                System.exit(1);
            }
        }
        catch(InterruptedException | IOException | URISyntaxException e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));

            sendMessageToLogManager(new LogItem("Errors", Calendar.getInstance().getTime().toString(), e.getMessage() + " - Stack Trace: " + e, "ERROR"));
        }

        return null;
    }

    /***
     * Extracts VLC libraries for the current platform to the specified path
     * Extracted files are marked for delete on exit, though they currently don't do so on Windows.
     *
     * @param systemType A string containing the current operating system
     * @param localLibPath The path to extract VLC libraries to.
     * @return Final VLC lib path
     */
    private static String extractVLCLibs(String systemType, String localLibPath)
    {
        String internalLibPath;
        String finalLibPath = localLibPath;
        final int copyBuffSize = 4096;
        final String fixedJarFileName = "VideoSync.jar";

        //Show a progress dialog so that the user knows VideoSync has launched
        ProgressMonitor pm = new ProgressMonitor(null, "VideoSync is starting, please wait...", "VideoSync © 2017 Caltrans®", 0, 10);
        //Show the progress dialog immediately

        pm.setMillisToPopup(0);
        pm.setMillisToDecideToPopup(0);

        try
        {
            //Gets a file handle for our current jar file
            String selfJarPath = VideoSync.class.getProtectionDomain().getCodeSource().getLocation().getPath();

            //Fix for jars exported with non-extracted dependency jars
            if(selfJarPath.equals("./"))
            {
                selfJarPath = fixedJarFileName;
            }

            File selfJarFile = new File(selfJarPath.trim());

            //Make the lib folder as a temp directory
            File libDir = new File(localLibPath);
            boolean success = libDir.mkdirs();
            if(!success)
                throw new IOException("Creation of temp lib folder failed");

            libDir.deleteOnExit();


            //Check that selfJarFile is a file and not a directory.
            //If it's a directory, this implies we are running from within an IDE
            //if(selfJarFile.isFile()){ //Old check. Does not appear to work on locked down machines
            if(selfJarFile.getAbsolutePath().endsWith(".jar"))
            { //New check. Appears to work on Bingjie's workstation.

                //Set internalLibPath to appropriate platform path
                //From perspective of jar file contents, vlc directory does not exist or is root.
                if(systemType.startsWith("Windows"))
                {
                    if(is64BitWindows())
                    {
                        //In context of Jar file, Windows uses forward slash like unix
                        internalLibPath = "win64/";
                    }
                    else
                    {
                        internalLibPath = "win32/";
                    }
                }
                else if(systemType.startsWith("Mac"))
                {
                    internalLibPath = "darwin/";
                }
                else if(systemType.startsWith("Linux"))
                {
                    internalLibPath = "linux/";
                }
                else
                {
                    System.out.println("Unknown operating system, no libs to extract");
                    System.out.println("OS: " + systemType);

                    //Since we don't have libs for the platform, return unsuccessful
                    return null;
                }

                //Append the OS folder to the final lib path for Jar files
                finalLibPath += internalLibPath;

                System.out.println("Extracting libraries from jar to " + finalLibPath);

                //Use built in JarFile class to extract the needed files
                //Replacing %20 with an actual space fixes issue where extract fails when path to jar file contains a space.
                JarFile selfJar = new JarFile(selfJarFile.getAbsolutePath().replace("%20", " "));
                Enumeration<JarEntry> entries = selfJar.entries();

                //Set the progress bar maximum to the number of files in the jar
                pm.setMaximum(selfJar.size());

                //Keep a record of progress bar progress
                int progress = 0;

                while(entries.hasMoreElements())
                {
                    JarEntry current = entries.nextElement();

                    //Check if this file is in the known library path.
                    //Skip over <platform>/.directory since copying chokes on these.
                    if(current.getName().startsWith(internalLibPath) && !current.getName().contains(".directory"))
                    {
                        String copyPath = localLibPath + File.separator + current.getName();
                        File newFile = new File(copyPath);

                        //Mark the file for delete on exit before creation. Otherwise, request is ignored.
                        newFile.deleteOnExit();

                        //Make sub directories instead of attempting to copy them
                        //This is needed to handle the the plugin folder.
                        if(current.isDirectory())
                        {
                            success = newFile.mkdirs();
                            if(!success)
                                throw new IOException("Creation of sub directories failed");
                            continue;
                        }

                        //Create file and open streams
                        success = newFile.createNewFile();
                        if(!success)
                            throw new IOException("Creation of new file failed");

                        InputStream fis = selfJar.getInputStream(current);
                        FileOutputStream fos = new FileOutputStream(newFile);

                        //Copy files from original to temp
                        byte[] buf = new byte[copyBuffSize];
                        int readBytes;
                        while((readBytes = fis.read(buf)) > 0)
                        {
                            fos.write(buf, 0, readBytes);
                        }

                        //Close streams
                        fis.close();
                        fos.close();
                    }

                    //If the user hits cancel, abort launch
                    if(pm.isCanceled())
                    {
                        System.out.println("User canceled VideoSync setup!");
                        System.exit(0);
                    }

                    //Increment and display progress
                    progress++;
                    pm.setProgress(progress);
                }

                selfJar.close();

                System.out.println("Libraries extracted successfully.");
            }
            else
            {
                System.out.println("Extract files - running in IDE, copying files instead of extracting");

                if(systemType.startsWith("Windows"))
                {
                    if(is64BitWindows())
                    {
                        internalLibPath = "vlc\\win64\\";
                    }
                    else
                    {
                        internalLibPath = "vlc\\win32\\";
                    }
                }
                else if(systemType.startsWith("Mac"))
                {
                    internalLibPath = "vlc/darwin/";
                }
                else if(systemType.startsWith("Linux"))
                {
                    internalLibPath = "vlc/linux/";
                }
                else
                {
                    System.out.println("Unknown operating system, no libs to extract");
                    System.out.println("OS: " + systemType);

                    //Since we don't have libs for the platform, return unsuccessful
                    return null;
                }

                //Store file listings in a vector so as we discover directories we can add their contents
                Vector internalLibFileVector = new Vector();
                //Get listing of library files at given path
                // TODO: Add check if null
                File[] internalLibFiles = new File(internalLibPath).listFiles();

                //Guarantee that our file handles can fit in the vector
                internalLibFileVector.ensureCapacity((internalLibFiles != null) ? internalLibFiles.length : 0);

                //Add files to vector.
                internalLibFileVector.addAll((internalLibFiles != null) ? Arrays.asList(internalLibFiles) : null);

                //Add files
                //Start by making the directory, if it doesnt exist.
                File libDirFile = new File(localLibPath);
                success = libDirFile.mkdirs();
                if(!success)
                    throw new IOException("Creation of temp lib directory failed");
                libDirFile.deleteOnExit();

                //Set the progress bar maximum
                pm.setMaximum(internalLibFileVector.size());

                //Keep a record of progress bar progress
                int progress = 0;

                //Attempt to make new directory
                //Loop through each entry and copy if it's a file
                //Using a for loop instead of foreach since foreach fails once we add more files
                int vectorSize = internalLibFileVector.size();
                for(int index = 0; index < vectorSize; index++)
                {
                    File file = (File) internalLibFileVector.elementAt(index);

                    File original = new File(file.toString());

                    //Cut off the internal lib path. This allows files in sub directories to be copied to the right location.
                    String tempPath = localLibPath + File.separator + original.getPath().substring(internalLibPath.length());
                    File tempCopy = new File(tempPath);

                    //Skip over file if it contains .directory.
                    if(tempPath.contains(".directory"))
                    {
                        continue;
                    }

                    //Mark the copy for delete on exit
                    tempCopy.deleteOnExit();

                    //Check if the original is a directory or file.
                    //If its a directory, add files to the vector
                    //If its a file, copy it.
                    if(original.isDirectory())
                    {
                        success = tempCopy.mkdirs();
                        if(!success)
                            throw new IOException("Creation of temp folder failed");

                        //Get all the files in the folder
                        // TODO: Add check if null
                        File[] subFiles = new File(original.getPath()).listFiles();

                        //Increase guaranteed vector size
                        vectorSize = internalLibFileVector.size() + ((subFiles != null) ? subFiles.length : 0);
                        internalLibFileVector.ensureCapacity(vectorSize);

                        //Add files to main vector
                        internalLibFileVector.addAll((subFiles != null) ? Arrays.asList(subFiles) : null);

                        //Increase the length of the progress bar
                        pm.setMaximum(internalLibFileVector.size());

                        continue;
                    }
                    else
                    {
                        //Create file and open streams
                        success = tempCopy.createNewFile();
                        if(!success)
                            throw new IOException("Creation of new file failed");
                        FileInputStream fis = new FileInputStream(original);
                        FileOutputStream fos = new FileOutputStream(tempCopy);

                        //Copy files from original to temp destination
                        byte[] buf = new byte[copyBuffSize];
                        int readBytes;
                        while((readBytes = fis.read(buf)) > 0)
                        {
                            fos.write(buf, 0, readBytes);
                        }

                        //Close streams
                        fis.close();
                        fos.close();
                    }

                    //If the user hits cancel, abort launch
                    if(pm.isCanceled())
                    {
                        System.out.println("User canceled VideoSync setup!");
                        System.exit(0);
                    }

                    //Increment and display progress
                    progress++;
                    pm.setProgress(progress);
                }

                System.out.println("Libraries copied successfully.");
            }

        }
        catch(IOException e)
        {
            System.out.println("Unable to extract VLC libraries: " + e);
            e.printStackTrace();
            //Set final lib path to null to indicate failure
            finalLibPath = null;
        }

        //Finish the progress monitor
        pm.close();

        return finalLibPath;
    }

    /**
     * Sends a new Log Item to the LoggerThread so it can be written to the appropriate Log File
     *
     * @param li LogItem object to be sent to the LogManager
     */
    private static void sendMessageToLogManager(LogItem li)
    {
        try
        {
            // Acquire access to the LoggerThread
            LoggerThread.acquireAccess();

            // Send the LogItem to the LoggerThread
            LoggerThread.addToList(li);

            // Release Access to the LoggerThread
            LoggerThread.releaseAccess();
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Provides access to the data model so that external utilities (ex. Reporter) can set data on it.
     *
     * @return DataModel instance
     */
    public DataModel getDataModel()
    {
        return dataModel;
    }

    /**
     * Whether or not the VideoSync window has been closed.
     * For use when VideoSync is brought up externally, ex. Reporter component.
     *
     * @return Boolean value representing if DataWindow is closed
     */
    @SuppressWarnings("unused")
    public boolean getIsClosed()
    {
        return (dataWindow == null || !dataWindow.isShowing());
    }

    /**
     * Detects if we are on 64-bit Windows.
     *
     * @return True if system is 64 bit Windows. False if not Windows, or 32-bit.
     */
    private static boolean is64BitWindows()
    {
        boolean toReturn = false;

        //We can't be 64-bit Windows if we aren't Windows at all.
        if(System.getProperty("os.name").startsWith("Win"))
        {
            //On 32-bit windows, only "Program Files" exists.
            //On 64, "Program Files(x86)" exists for 32-bit programs. If this folder is present, we are on 64-bit Windows.
            //Use this instead of System.getProperty("sun.arch.data.model").contains("64") as this returns the JVM arch, NOT the system arch!
            toReturn = (System.getenv("ProgramFiles(x86)") != null);
        }

        return toReturn;
    }

    /**
     * Whether or not another instance of VideoSync is already running.
     * This will only return false the first time it is called, for the first instance of the application. Future calls will return true.
     *
     * @return True if there is another instance of VideoSync. False if this is the only instance.
     */
    public static boolean isAlreadyRunning()
    {
        final String RUNNING_PID_NAME = "runningPID";

        //Assume that VideoSync is running until proven otherwise.
        //This makes the default a safe state if something goes wrong.
        boolean isRunning = true;


        File tempDir = new File(getTempDir());
        boolean success = tempDir.mkdirs();
        if(!success)
            System.out.println("Temp directory not created, likely already exists.");


        //Check if file exists.
        File pidFile = new File(getTempDir() + RUNNING_PID_NAME);

        //File exists. Check if the file references a running instance of VideoSync.
        if(pidFile.exists())
        {
            try
            {
                //Get the last ran PID from file.
                FileInputStream fis = new FileInputStream(pidFile);
                DataInputStream dis = new DataInputStream(fis);
                int runningPid = dis.readInt();

                dis.close();
                fis.close();

                //Check if PID is running.
                isRunning = isPIDRunning(runningPid);

                //If the pid is not running, delete the file.
                if(!isRunning)
                {
                    success = pidFile.delete();
                    if(!success)
                        throw new IOException("PID file deletion failed");
                }

            }
            catch(IOException ex)
            {
                System.out.println("Unable to read running PID file");
                ex.printStackTrace();
            }
        }
        else
        {
            //If file does not exist, we know we are the only running instance.
            isRunning = false;
        }

        //If the PID is not running, create a file containing the current PID.
        if(!isRunning)
        {
            //Get the current process name
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            String processName = bean.getName();

            //Get current process ID from process name. PID is stored in all characters before the @ character
            int pid = Integer.parseInt(bean.getName().substring(0, processName.indexOf("@")));
            System.out.println("Extracted PID: " + pid);

            //Set the file to delete when VideoSync exits.
            pidFile.deleteOnExit();

            try
            {
                //Ensure that a blank file exists.
                success = pidFile.createNewFile();
                if(!success)
                    throw new IOException("Creation of PID file failed");

                //Write the current PID
                FileOutputStream fos = new FileOutputStream(pidFile);
                DataOutputStream dos = new DataOutputStream(fos);
                dos.writeInt(pid);

                dos.close();
                fos.close();
            }
            catch(IOException ex)
            {
                System.out.println("Unable to create running PID file");
                ex.printStackTrace();
            }

        }

        return isRunning;
    }

    /**
     * Returns whether or not there is a VideoSync process with the given PID running.
     *
     * @param pid Process ID
     * @return Whether or not that PID is both active and an instance of VideoSync.
     */
    private static boolean isPIDRunning(int pid)
    {
        boolean isRunning = false;
        Process checkPIDProcess;

        try
        {
            //PID check requires running a system command, which will be different depending on OS.
            if(System.getProperty("os.name").startsWith("Windows"))
            {
                //FI flag allows for PID check. v flag displays window title so we can check for VideoSync.
                checkPIDProcess = Runtime.getRuntime().exec(new String[]{"tasklist", "/FI", "PID eq " + pid, "/v"});
            }
            else
            {
                //Should work on all *nix OS. -p flag specifies pid, -f flag shows full format information including execution path
                //so we can check for VideoSync.
                checkPIDProcess = Runtime.getRuntime().exec(new String[]{"ps", "-p", Integer.toString(pid), "-f"});
            }

            //Read through results of the check PID process on a line by line basis.
            InputStream is = checkPIDProcess.getInputStream();
            BufferedReader resultReader = new BufferedReader(new InputStreamReader(is));
            String resultLine;
            while((resultLine = resultReader.readLine()) != null)
            {
                //Check if the listed process has the correct PID, is running in a java VM and references VideoSync.
                //This combination ensures that if other processes reference VideoSync (ex. file manager path), they will be ignored.
                if(resultLine.contains(Integer.toString(pid)) && resultLine.contains("java") && resultLine.contains("VideoSync"))
                {
                    System.out.println("Found running PID " + pid + " matching PID file");
                    isRunning = true;
                }
            }

        }
        catch(IOException ex)
        {
            System.out.println("Unable to check if pid " + pid + " is running.");
            ex.printStackTrace();
        }

        return isRunning;
    }

    /**
     * Returns the path to the base temporary directory where files can be stored hidden from the user.
     *
     * @return String of the temporary directory where files can be stored hidden from the user
     */
    public static String getTempDir()
    {
        String tempDir;

        //Path will change depending on OS. Assuming that if not Windows, something Unix based.
        if(System.getProperty("os.name").startsWith("Windows"))
        {
            tempDir = System.getenv("APPDATA") + "VideoSync\\";
        }
        else
        {
            tempDir = System.getProperty("java.io.tmpdir") + "/VideoSync/";
        }

        return tempDir;
    }

    /**
     * Checks if the version of Java in use is compatible with VideoSync.
     * If Java is not compatible, displays a message on why.
     *
     * @param showMessages Enable showing messages or not
     * @return Whether java is compatible.
     */
    private static Boolean isJavaCompatible(boolean showMessages)
    {
        boolean isCompatible = true;
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String javaName = bean.getVmName();
        double javaVersion = Double.parseDouble(bean.getSpecVersion());
        boolean javaVersionSupported = (javaVersion >= 1.7);
        int javaArch = Integer.parseInt(System.getProperty("sun.arch.data.model"));
        boolean isJava32bit = (javaArch == 32);
        StringBuilder message = new StringBuilder();
        message.append("Your version of Java is incompatible with VideoSync:\n\n");

        //The only way we can check for JavaFX is to see if a class it includes exists.
        //JavaFX is required for color picker UI element.
        try
        {
            Class.forName("javafx.application.Platform");
        }
        catch(ClassNotFoundException ex)
        {
            System.out.println("Java VM does not include required JavaFX!");

            message.append("* No JavaFX support.");

            //Check if the running JVM is the official Oracle JVM or something else, like OpenJDK.
            if(!javaName.equals("Java HotSpot(TM) Server VM"))
            {
                message.append(" JavaFX is included in Oracle Java, but you are running ").append(javaName).append(".\n");
            }
            else
            {
                message.append("\n");
            }

            isCompatible = false;
        }

        //Java 1.7 or higher is required for VLC direct video playback.
        if(!javaVersionSupported)
        {
            System.out.println("Java VM is version " + javaVersion + ", 1.7 or higher required!");
            message.append("* Java version 1.7 or higher is required. Your version is ").append(javaVersion).append(".\n");
            isCompatible = false;
        }

        //Check for situation where 32-bit Java is used on 64-bit Windows.
        if(System.getProperty("os.name").startsWith("Windows") && isJava32bit && is64BitWindows())
        {
            System.out.println("Java is 32-bit on 64-bit windows!");
            message.append("* Operating system is 64-bit, but Java version is 32-bit.");
            isCompatible = false;
        }

        //If any incompatible element was found, show a message.
        if(!isCompatible && showMessages)
        {
            JOptionPane.showMessageDialog(null, message.toString(), "Unable to start VideoSync", JOptionPane.ERROR_MESSAGE);
        }

        System.out.println("Java compatible: " + (isCompatible ? "Yes" : "No") + "," + "(Version: " + javaVersion + ", Arch: " + javaArch + ")");
        return isCompatible;
    }

    /**
     * Checks if VideoSync is running on a bundled version of Java.
     *
     * @return true if running on a bundled Java runtime, false if not.
     */
    private static boolean isRunningOnBundledJava()
    {
        boolean toReturn = false;
        //Get file paths to both JVM and PreInstall
        String jvmLocation = System.getProperties().getProperty("java.home");
        String selfJarPath = VideoSync.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File selfJarFile = new File(selfJarPath);

        //Cut file name out of jar path so that we only have the directory.
        if(!selfJarFile.isDirectory())
        {
            selfJarPath = selfJarFile.getAbsolutePath().replace("%20", " ").replace(selfJarFile.getName(), "");
        }

        //If our JVM path contains our Jar path, we are running on a bundled JVM.
        if(jvmLocation.contains(selfJarPath))
        {
            toReturn = true;
        }

        System.out.println("Running on bundled java: " + (toReturn ? "yes" : "no"));
        System.out.println("JVM Path: " + jvmLocation);
        System.out.println("Self Path: " + selfJarPath);

        return toReturn;
    }

    /**
     * Prints output from the given running process. Will not return until the given process is no longer running.
     *
     * @param toPrint Process to print output of.
     */
    private static void printProcessMessages(Process toPrint)
    {
        BufferedReader processOutput = new BufferedReader(new InputStreamReader(toPrint.getInputStream()));
        String currentLine;
        do
        {
            try
            {
                while((currentLine = processOutput.readLine()) != null)
                {
                    System.out.println(currentLine);
                }
            }
            catch(IOException ex)
            {
                System.out.println("Failed to read current line!");
                ex.printStackTrace();
            }
        }
        while(toPrint.isAlive());
    }
}
