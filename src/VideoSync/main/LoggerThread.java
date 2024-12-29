/*
 * ****************************************************************
 * File: 			LogFileManager.java
 * Date Created:  	Jan 9, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To maintain all appropriate log files associated
 * 					with each of the threads that may be running at
 * 					any given time. This allows for more detailed
 * 					information to be analyzed without having to
 * 					worry about the console getting all cluttered.
 * 					It uses a FIFO buffer
 *
 * Modified			August 26, 2016
 * Programmer:		Danny Hale
 * 					Made the logger more self contained by adding
 * 					the ability to make the logs directory.
 * ****************************************************************
 */

package VideoSync.main;

import VideoSync.objects.LogItem;

import java.io.*;
import java.util.Calendar;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public class LoggerThread extends Thread
{
    /**
     * Used in granting other threads to safely access this thread and add data to the lists.
     */
    private static Semaphore access = new Semaphore(1, true);

    /**
     * Used in keeping track if the thread is to be shutdown or kept alive. This should only be changed
     * from shutdownThread() which is invoked from the Device Daemon
     */
    private boolean threadAlive = false;

    /**
     * Used in specifying what directory the log file is going to be stored in.
     */
    private String baseDirectory = null;

    /**
     * Used to keep track of all log files that are currently in use by each of the threads as well as
     * custom log files that are being maintained.
     */
    private Vector<File> logFiles = new Vector<>();

    /**
     * Used in temporarily storing all log messages as they are received from the various threads. They
     * are one by one taken out and processed by the addToFile() function.
     */
    private static Vector<LogItem> logList = new Vector<>();

    /**
     * Construct the Log File Manager with the name of the thread so we can see it be closed when the program terminates
     *
     * @param threadName
     */
    public LoggerThread(String threadName, String logPath)
    {
        // Set the name of this thread.
        this.setName(threadName);

        // If this is the first time running VideoSync, make the logs directory
        File logDirectory = new File(logPath);
        if(!(logDirectory).exists())
        {
            logDirectory.mkdir();
        }

        // Set the path for where the log files will be written to. In this case
        // we set it to the same directory parent directory of the project location
        String date = String.format("%02d.%02d.%02d", Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.DATE));
        String time = String.format("_%d.%d.%d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), Calendar.getInstance().get(Calendar.SECOND));

        String logDir = logPath + date + time;
        baseDirectory = new File(logDir).getAbsolutePath();
    }

    // -- Log File Manager Threading Methods

    /**
     * Every second this thread checks the number of messages to be written and if there is any,
     * it sends them off to be written and then sleeps.
     */
    public void run()
    {
        new File(baseDirectory).mkdir();

        threadAlive = true;

        // This allows the thread to run constantly as long as the thread has not been shutdown.
        while(threadAlive)
        {
            // If the number of log elements in the array has more than 0, add each one to the correct file
            while(logList.size() > 0)
            {
                // This gets the first element in the list and helps make sure they are written out in sequential order
                // as they were received from the sending thread.
                try
                {
                    addToFile(logList.remove(0));
                }
                catch(Exception e)
                {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));

                    System.out.println(" *** ERROR - Log File Manager - Exception found when getting a log file from the list. Exception Message: '" + e.getMessage() + "' -- " + errors);
                }
            }

            // Have the Log File Manager Thread sleep before checking for any new error messages
            try
            {
                LoggerThread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                // The log File Manager Thread does not have its own log file as any errors it produces will most likely cause a crash, so just display the error ot the console.
                System.err.println("__ LogFileManager __ --- An Error while trying to sleep");
                e.printStackTrace();
            }
        }
    }


    /**
     * Tells the thread to begin shutting down and release all of its resources
     */
    public void initializeShutdownProcess()
    {
        this.threadAlive = false;

        shutdownThread();
    }

    // -- Log File Manager Access Management

    /**
     * Adds a new log element to the list to be written to a log file
     *
     * @param li- A string array containing the log name as well as the message to be written
     */
    public static void addToList(LogItem li)
    {
        logList.add(li);
    }

    /**
     * Allows other threads to safely add information to this thread
     *
     * @throws InterruptedException
     */
    public static void acquireAccess() throws InterruptedException
    {
        access.acquire();
    }

    /**
     * Releases the thread so that it may receive information from other threads.
     */
    public static void releaseAccess()
    {
        access.release();
    }

    // -- Log File Manager File Writer

    /**
     * Adds the logData to the correct log file
     *
     * @param li
     */
    private void addToFile(LogItem li)
    {
        boolean foundFile = false;

        File fileElement = null;

        // Search through all of the log files to see if we have a log file already created for it
        for(int i = 0; i < logFiles.size(); i++)
        {
            // If the log file equals the first portion of the log data, then we have a log file
            if(logFiles.elementAt(i).getName().contains(li.getClassName()))
            {
                // Get the log file we are going to write to and temporarily store it in fileElement
                fileElement = logFiles.elementAt(i);

                // Indicate that we found the log file
                foundFile = true;

                // Break out of the for loop since we have no reason to check any other log files
                break;
            }
        }

        // If we did not find a log file, we need to create one for this log element
        if(!foundFile)
        {
            // Print out a list of all the current log files so we know what was created and what was being searched for.
            StringBuilder currentFiles = new StringBuilder("Did not find a file for " + li.getClassName());
            currentFiles.append("\nFiles currently in Vector<File>: ");
            for(File f : logFiles)
            {
                currentFiles.append("\t\t").append(f.getName());
            }

            // Write the current files in the Log File Manager to a Log File
//			String log[] = {"Log File Manager", Calendar.getInstance().getTime().toString() + " " + currentFiles + "\n"};
            LogItem nli = new LogItem("Log File Manager", Calendar.getInstance().getTime().toString(), currentFiles.toString(), "Notice");
            addToList(nli);

            fileElement = new File(baseDirectory + "/" + li.getClassName() + ".txt");
            logFiles.add(fileElement);
        }

        try
        {
            // Write the data to the file system
            FileWriter fileWriter = new FileWriter(fileElement, true);
            fileWriter.write(li.toString() + "\n");
            fileWriter.close();
        }
        catch(IOException ioE)
        {
            System.err.println("Error while writing data to file " + fileElement.getName());
            ioE.printStackTrace();
        }
    }

    // -- Log File Manager Thread Shutdown Management

    /**
     * Begins the shutdown process for the Log File Thread
     */
    private void shutdownThread()
    {
        // Generates a string stating what all was shutdown and the status of the objects released.s
        String shutdownThreadString = "\n"
                + "********************* THREAD SHUTDOWN PROCESS *********************"
                + "Shutting down thread " + this.getName()
                + stopThread()
                + "*******************************************************************"
                + "\n";

        // Prints out the result of the thread shutdown process
        System.out.println(shutdownThreadString);

        // This kills the thread
        this.interrupt();
    }

    /**
     * Returns a string telling us that the thread has successfully closed down and released all resources
     *
     * @return
     */
    private String stopThread()
    {
        String toReturn = "";

        if(baseDirectory == null)
        {
            toReturn += "baseDirectory: null\n";
        }
        else
        {
            toReturn += "baseDirectory: " + baseDirectory + "\n";
        }

        logFiles = null;
        if(logFiles == null)
        {
            toReturn += "logFiles: null\n";
        }
        else
        {
            toReturn += "logFiles: " + logFiles.toString() + "\n";
        }

        logList = null;
        if(logList == null)
        {
            toReturn += "logList: null\n";
        }
        else
        {
            toReturn += "logList: " + logList.toString() + "\n";
        }

        toReturn += "*******************************************************************"
                + "\n";

        return toReturn;
    }
}
