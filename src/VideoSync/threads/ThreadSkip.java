/*
 * ****************************************************************
 * File: 			ThreadSkip.java
 * Date Created:  	August 8, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle skipping the video and graphs
 * forwards or backwards utilizing a background
 * thread as to not hang up the UI.
 *
 * ****************************************************************
 */
package VideoSync.threads;

import VideoSync.models.DataModelProxy;


public class ThreadSkip extends Thread
{
    /**
     * Used for keeping the running as long as necessary.
     */
    private boolean threadAlive = false;

    /**
     * Used for letting the Data Model know how far to jump and which way.
     */
    private final DataModelProxy dmp;

    /**
     * Used for keeping track of which direction we are going to jump.
     *
     * If its 1, we go forwards, if its -1, we go backwards.
     */
    private final int direction;

    // -- Thread Skip Construction

    /**
     * Creates the Thread Skip thread with the data model and proxy and direction parameters
     *
     * @param dmp       datamodel proxy object
     * @param direction direction to skip in
     */
    public ThreadSkip(DataModelProxy dmp, int direction)
    {
        // Set the local direction variable to the one passed.
        this.direction = direction;

        // Set the local dmp variable to the one passed.
        this.dmp = dmp;
    }

    /**
     * Runs the thread until threadAlive gets set to false.
     */
    public void run()
    {
        // Set the threadAlive to true upon starting the thread.
        this.threadAlive = true;

        // Run as long as threadAlive is true
        while(this.threadAlive)
        {
            // Tell the data model proxy to skip forwards or backwards by 500 milliseconds.
            this.dmp.skipVideo(500 * this.direction);

            // Tell the thread to sleep for 100 milliseconds before skipping again
            // FIXME: Fix busy waiting loop?
            try
            {
                Thread.sleep(100);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Tells the thread to stop running by setting the threadAlive to false.
     */
    public void stopThread()
    {
        this.threadAlive = false;
    }
}
