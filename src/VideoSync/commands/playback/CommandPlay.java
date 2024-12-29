/*
 * ****************************************************************
 * File: 			CommandPlay.java
 * Date Created:  	June 12, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the play back
 *                  view to start playing the video and graphs
 *
 * ****************************************************************
 */
package VideoSync.commands.playback;

import VideoSync.models.DataModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

public class CommandPlay extends AbstractAction implements Observer
{
    private static final long serialVersionUID = 1L;

    /**
     * Sets the base timing rate to play the graphs at 1/30th of a second.
     */
    private final double baseRate = (1.0 / 30.0) * 1000;

    /**
     * Used to reference to the DataModel
     */
    private DataModel dm;

    /**
     * Timer to maintain the ability to play the graphs at a consistent rate
     */
    private final Timer playTimer;

    /**
     * Sets the initial timer speed to the same as the base rate.
     */
    private double timerSpeed = baseRate;

    /**
     * Create the Command Play Object
     */
    public CommandPlay()
    {
        // Create the new Timer Instance and set its duration and caller
        playTimer = new Timer((int) timerSpeed, this);

        // Immediately pause the timer so that it isn't running upon startup
        playTimer.stop();
    }

    /**
     * Sets the references to the DataModel
     *
     * @param dm DataModel object reference
     */
    public void setTarget(DataModel dm)
    {
        // Set the data model to the one passed
        this.dm = dm;

        // Add the Command Play object as an observer to the data model
        this.dm.addObserver(this);
    }

    /**
     * Called when the user chooses to play the data
     *
     * @param e ActionEvent triggered by Play button press
     */
    public void actionPerformed(ActionEvent e)
    {
        // If the source of the ActionEvent is a JButton, it is the user
        // requesting to start playing the data and not the Timer.
        if(e.getSource() instanceof JButton)
        {
            // Check to see if the data model is already playing.
            // If so, stop the timer and tell the Data Model
            // to stop any videos from playing.
            if(this.dm.isPlaying())
            {
                playTimer.stop();
                this.dm.setPlaying(false);
            }
            else
            {
                playTimer.start();
                this.dm.setPlaying(true);
            }
        }

        // If the data model is currently playing, then update the graphs position.
        if(e.getSource() instanceof Timer && this.dm.isPlaying())
        {
            if(this.dm.getCurrentPosition() < this.dm.getMaxVideoLength())
            {
                // Get the last time value of the graph data
                long lastPos = this.dm.getCurrentPosition();

                // Calculate a new time value based on the last time value and the value of the timer speed.
                long newPos = lastPos + (int) timerSpeed;

                // Update the current position of the graph window to the one we have calculated.
                this.dm.setCurrentPosition(newPos, false);
            }
            else
            {
                this.dm.setPlaying(false);
                long newPos = (int) timerSpeed;
                this.dm.setCurrentPosition(newPos, true);
            }
        }
    }

    /**
     * Updates the Command Play via a notification from the data model.
     *
     * @param arg0 Observable that triggered this update
     * @param arg1 Argument passed to update
     */
    public void update(Observable arg0, Object arg1)
    {
        // If the argument passed is a type Double, then we can proceed with updating variables.
        // Otherwise we can just ignore it.
        if(arg1 instanceof Double)
        {
            // Update the timer speed based on the base time value and the parameter passed.
            timerSpeed = baseRate * (Double) arg1;
        }
    }
}
