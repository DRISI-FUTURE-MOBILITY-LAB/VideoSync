/*
 * ****************************************************************
 * File: 			VideoPlayer.java
 * Date Created:  	June 28, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle and control all aspects of the video
 *                  player on its own and to receive updates from
 *                  the data model as outside events request the
 *                  video file to update positions, times, etc...
 *
 * Modified			October 19, 2016
 * Programmer:		Elliot Hawkins
 *                  Add ability to directly render video frames to
 *                  fix Mac compatibility on Java 7 and up. Uses
 *                  regular embedded player on other OS, or Mac on
 *                  Java 6 due to performance hit.
 * ****************************************************************
 */
package VideoSync.views.videos;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.graphs.Region;
import com.sun.jna.NativeLibrary;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.component.DirectMediaPlayerComponent;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.player.direct.RenderCallbackAdapter;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;


public class VideoPlayer extends JFrame implements KeyListener, MediaPlayerEventListener, ComponentListener, ChangeListener, WindowListener, Observer, MouseMotionListener, MouseWheelListener, MouseListener
{
    private static final long serialVersionUID = -1540486086323956431L;

    // -- Video Player Variable Declarations

    /*** Used for to debug vlcj events ***/
    final private boolean vlc_debug = false;

    /*** Use direct rendering even on Windows/Linux. Needed for drawing on frames. ***/
    static final private boolean ALWAYS_USE_DIRECT_RENDERING = true;

    /*** tells java when the video is buffered and ready to be queried ***/
    private boolean vlc_is_ready;

    private boolean mediaPlayerReadyFired;

    /**
     * Used for keeping track if the player is considered the 'Master Player'
     */
    private int playerID;

    /**
     * Used with the master play to notify the Data Model when the time changed notice gets fired.
     */
    private final DataModelProxy dmp;

    /**
     * Used in frameAdvance() for ensuring that the appropriate number of time has elapsed between frame jumps
     */
    private long lastSystemTime;

    /**
     * Keeps track of the video file used with this player.
     */
    private final File videoFile;

    /**
     * Determines whether to use the embedded media player or direct media player
     */
    private final boolean useDirectMediaPlayer;

    /**
     * VLCJ Media Components required for video playback.
     */
    private MediaPlayer mediaPlayer;
    private EmbeddedMediaPlayerComponent embeddedMediaPlayerComponent;
    private DirectMediaPlayerComponent directMediaPlayerComponent;

    /**
     * Stores the time parameter passed to the constructor on startup for jumping the video to that point in time.
     * This is so that if the video is loaded after we have started analysis, we can get to an approximate position
     * in the video that matches all the other time syncs.
     */
    private long currentTime;

    private long offset;

    /**
     * Stores the time parameter sent by the constructor during startup.
     */
    private final long sentTime;

    /**
     * Stores the length of the video so it can be referenced while paused/finished.
     */
    private long videoLength;

    /**
     * Stores the width and height of the video so it can be referenced by the direct playback component upon
     * creation of callback.
     */
    private int videoWidth;
    private int videoHeight;

    /**
     * Used for displaying the current time on the video player.
     */
    private JLabel label_PlaybackTime;

    /**
     * These variables are only used with the stand alone video player.F
     */
    private JSlider rateSlider;
    private JSlider positionSlider;
    private JTextField timeJumpField;
    private String jumpString;
    private JFrame mediaController;
    private DirectVideoRenderPanel directRenderPanel;
    private VideoPanel video_panel;

    /**
     * Boolean flag indicating whether this video player had been saved to the project directory or not.
     */
    private boolean isSaved;

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Video Player Construction

    /**
     * Creates a video player with the associated file. If no file is passed throw a NullPointerException
     *
     * @param file video file object
     * @param currentTime current time
     * @param playerID ID of video player
     */
    public VideoPlayer(String vlcPath, File file, long currentTime, int playerID, DataModelProxy dmp, boolean isSaved)
    {
        //Prevent base JFrame class from closing window if user presses no on exit prompt
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        LibXUtil.initialise();    //Added for linux support
        setVLCReadyFlag(false);

        if(vlcPath == null)
            throw new NullPointerException("A Valid VLC Application must be available to run");
        if(file == null || !file.exists())
            throw new NullPointerException("A Valid Video File was not passed to the Video Player.");

        // Set the saved state of the video player
        this.isSaved = isSaved;

        // Set the playerID
        this.playerID = playerID;

        // Set the DataModelProxy object
        this.dmp = dmp;

        // Set the current video time to match what was sent from the data model.
        // This is so that we can add a video at any point during playback and it should jump to the same point in time.
        this.currentTime = currentTime;

        // Set the time sent by the constructor
        this.sentTime = currentTime;

        // Set the video file to be played back.
        this.videoFile = file;

        if(ALWAYS_USE_DIRECT_RENDERING)
        {
            useDirectMediaPlayer = true;
        }
        else
        {
            //Determine the video player type based on OS + Java version
            //Assumes that Java 1.6 is minimum.
            if(System.getProperty("os.name").startsWith("Mac") && !System.getProperty("java.version").startsWith("1.6"))
            {
                //Use direct media player as AWT implementation is too incomplete on Oracle maintained Mac JVMs for embedded media player
                this.useDirectMediaPlayer = true;
                System.out.println("Mac with Java 1.7 or higher, using direct media player!");
            }
            else
            {
                //Use embedded media player for better performance.
                this.useDirectMediaPlayer = false;
                System.out.println("Using embedded media player!");
            }
        }

        // Create the video player layout and set it on the screen.
        createVideoPlayer(vlcPath);

        // Set the video file so it can be used with VLC
        setVideoFile();

        // Create the Video Controller and Key listener only for the standalone version.
        //createVideoController();
        lastSystemTime = System.currentTimeMillis();

        // Set the Video Player's Title
        this.setTitle(file.getName());

        this.addWindowListener(this);
    }

    /**
     * Creates the video container to display the video file in
     */
    public void createVideoPlayer(String vlcPath)
    {
        System.out.println("JNA: Attempting to use \"" + RuntimeUtil.getLibVlcLibraryName() + "\" in \"" + vlcPath + "\" on " + System.getProperty("os.name"));
        // Load up the native VLC library for the corresponding OS
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcPath);

        //System.out.println("JNA paths: " + System.getProperty("jna.platform.library.path"));
        //System.out.println("Java paths: " + System.getProperty("java.library.path"));
        // Create a media player component to display the video file within

        //Initialize the appropriate Media Player Component
        if(useDirectMediaPlayer)
        {
            //Direct Media Player requires a buffer format callback
            //RV32 is included in VLCJ
            BufferFormatCallback buffFormatCallback = RV32BufferFormat::new;

            //Create a temp component to load video with so we can get frame size.
            directMediaPlayerComponent = new DirectMediaPlayerComponent(buffFormatCallback);
            mediaPlayer = directMediaPlayerComponent.getMediaPlayer();
            mediaPlayer.setVolume(0);
            mediaPlayer.addMediaPlayerEventListener(this);

            String[] options = {"quiet=true"};

            // Tell the instantiate with the video file and use the options included
            mediaPlayer.playMedia(videoFile.getAbsolutePath(), options);

            // FIXME: Busy waiting?
            while(!isVLCReady())
            {
                try
                {
                    Thread.sleep(500);
                }
                catch(InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            //Reset VLCReadyFlag so that it can be used when reloading video after getting frame size
            setVLCReadyFlag(false);

            videoWidth = mediaPlayer.getVideoDimension().width;
            videoHeight = mediaPlayer.getVideoDimension().height;

            //Free temp components
            mediaPlayer.release();
            directMediaPlayerComponent.release();

            //Create the actual media player component
            directMediaPlayerComponent = new DirectMediaPlayerComponent(buffFormatCallback)
            {

                @Override
                protected RenderCallback onGetRenderCallback()
                {
                    System.out.println("Created render callback size: " + videoWidth + "x" + videoHeight);
                    return new RenderCallbackAdapter(new int[videoWidth * videoHeight * 4])
                    {
                        @Override
                        protected void onDisplay(DirectMediaPlayer displayingPlayer, int[] rgb)
                        {

                            if((displayingPlayer != null) && (displayingPlayer.isPlayable()) && (displayingPlayer.getVideoDimension() != null))
                            {
                                int videoWidth = displayingPlayer.getVideoDimension().width;
                                int videoHeight = displayingPlayer.getVideoDimension().height;
                                BufferedImage directFrameBuffer = directRenderPanel.getVideoFrame();

                                //Ensures that the image is instantiated and properly sized for the buffer
                                if(directFrameBuffer == null || directFrameBuffer.getWidth() != videoWidth || directFrameBuffer.getHeight() != videoHeight)
                                {
                                    //Even though we don't need alpha channel, using ARGB is fastest since Java converts to ARGB internally upon rendering.
                                    directFrameBuffer = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_INT_ARGB);
                                    directFrameBuffer.setAccelerationPriority(1);
                                    directRenderPanel.setVideoFrame(directFrameBuffer);
                                }

                                directFrameBuffer.flush();

                                directFrameBuffer.setRGB(0, 0, videoWidth, videoHeight, rgb, 0, videoWidth);
                            }
                        }
                    };
                }
            };
        }
        else
        {
            embeddedMediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        }

        //Create the media player component and add it to the video window
        if(useDirectMediaPlayer)
        {
            //Direct media player cannot be directly added.
            //Instead, we must render its contents onto an image, which will be contained in a JPanel.
            directRenderPanel = new DirectVideoRenderPanel(dmp);

            // Retrieve the media player from the component so that we can register some event listeners to the player
            mediaPlayer = directMediaPlayerComponent.getMediaPlayer();
            mediaPlayer.addMediaPlayerEventListener(this);
            directRenderPanel.setMediaPlayer(mediaPlayer);
            getContentPane().setLayout(new BorderLayout(0, 0));

            // Add the mediaPlayer to the layout's center of the JFrame
            getContentPane().add(directRenderPanel);

        }
        else
        {
            // Retrieve the media player from the component so that we can register some event listeners to the player
            mediaPlayer = embeddedMediaPlayerComponent.getMediaPlayer();
            mediaPlayer.addMediaPlayerEventListener(this);
            getContentPane().setLayout(new BorderLayout(0, 0));

            // Add the mediaPlayer to the layout's center of the JFrame
            getContentPane().add(embeddedMediaPlayerComponent);
        }

        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.SOUTH);
        panel.setLayout(new GridLayout(0, 1, 0, 0));

        label_PlaybackTime = new JLabel("Playback Time");
        panel.add(label_PlaybackTime);

        // Make the video player visible.
        this.setVisible(true);
    }

    /**
     * Set the video player's source to the video file that was used in the constructor
     */
    public void setVideoFile()
    {
        //Count time slices until the media is ready for statistics.
        int time_quanta = 0;
        // Set the player to quite mode and not print out any information as it plays.
        String[] options = {"quiet=true"};

        // Tell the instantiate with the video file and use the options included
        mediaPlayer.playMedia(videoFile.getAbsolutePath(), options);
        //mediaPlayer.prepareMedia(videoFile.getAbsolutePath(), options);
        //mediaPlayer.play();

        // This gives the player enough time to startup and get ready to go
        // and also give enough time for "pause" to take effect. 400ms to 500ms
        // is enough during most load sequences but it could take an extra second
        // if the vlc drivers are completely unloaded.
        // FIXME: Busy waiting?
        while(!isVLCReady())
        {
            try
            {
                Thread.sleep(100);
                time_quanta++;    //used to debugging how long a video takes to prepare
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        videoLength = mediaPlayer.getLength();

        // Pause the media player.
        mediaPlayer.pause();

        // Jump the video to the time sent by the constructor.
        jumpToTime(this.sentTime);

        // If the current time & sent times do not match, it probably means the video attempted to play
        // prematurely, so we need to ensure the video time matches with the time sent.
        if(this.currentTime != this.sentTime)
            this.currentTime = this.sentTime;

        // FIXME: Empty catch block lmao
        // Occasionally this throws a NullPointerException which doesn't appear to be critical, so just silently
        // catch the error and continue on.
        try
        {
            // Display the video information for debugging information.
            System.out.println("-------------------------------- VIDEO INFORMATION --------------------------------");
            System.out.println(" - Video Setup time: " + time_quanta * 100 + " ms");
            System.out.println(" - Video File: " + videoFile.getName());
            System.out.println(" - Video Path: " + videoFile.getPath());
            System.out.println(" - Video Length: " + mediaPlayer.getLength() + " ms");
            System.out.println(" - Video Size: " + mediaPlayer.getVideoDimension().width + "x" + mediaPlayer.getVideoDimension().height);
            System.out.println(" - Video Frame Rate: " + mediaPlayer.getFps());
            System.out.println(" - Video Time between frames: " + (1000 / mediaPlayer.getFps()));
            System.out.println("-----------------------------------------------------------------------------------");
        }
        catch(NullPointerException npe)
        {

        }
        setSize(mediaPlayer.getVideoDimension().width - 80, mediaPlayer.getVideoDimension().height);
        this.setVideoTimeLabel(mediaPlayer.getTime());
    }

    /**
     * This is only used when using a static instance of the video controller.
     *
     * <p>
     * Creates a basic window to handle the sliders and jump to field for jumping the video around.
     */
    public void createVideoController()
    {
        // Ensure that the video player is visible before creating the controller
        if(this.isVisible())
        {
            mediaController = new JFrame();
            mediaController.setVisible(true);
            mediaController.setSize(this.getWidth(), 100);
            mediaController.setLocation(this.getX(), (int) (this.getHeight() + this.getLocation().getY()));
            mediaController.getContentPane().setLayout(new FlowLayout());

            rateSlider = new JSlider(0, 5);
            rateSlider.setValue(0);
            rateSlider.setMajorTickSpacing(1);
            rateSlider.setSnapToTicks(true);
            rateSlider.addChangeListener(this);
            rateSlider.setName("Rate");

            positionSlider = new JSlider(0, (int) mediaPlayer.getLength());
            positionSlider.setValue(0);
            positionSlider.addChangeListener(this);
            positionSlider.setName("Position");

            timeJumpField = new JTextField();
            timeJumpField.setColumns(10);
            timeJumpField.addKeyListener(new KeyAdapter()
            {
                public void keyReleased(KeyEvent e)
                {
                    jumpString = timeJumpField.getText();

                    if(e.getKeyCode() == 10)
                    {
                        try
                        {
                            jumpToTime(Integer.parseInt(jumpString + getOffset()));
                        }
                        catch(NumberFormatException nfe)
                        {
                            System.err.println("Number Format Exception");
                        }
                    }
                }
            });

            mediaController.getContentPane().add(rateSlider);
            mediaController.getContentPane().add(positionSlider);
            mediaController.getContentPane().add(timeJumpField);
        }
    }


    /*
     * The following functions pertain to accessing any of the private elements used with the Video Player
     */
    // -- Video Player Getter's & Setters

    /**
     * Returns the player ID
     *
     * @return playerID
     */
    public int getPlayerID()
    {
        return this.playerID;
    }

    /**
     * Sets the player ID. This is invoked when a video player has been removed and we need to re-assign one to
     * be the master player.
     *
     * @param id ID of video player
     */
    public void setPlayerID(int id)
    {
        this.playerID = id;
    }

    /**
     * Returns the video file used for the container.
     */
    public File getVideoFile()
    {
        return this.videoFile;
    }


    /*
     * The following functions pertain directly to the control of the player, as well
     * as the access and setting of information to be used by the media player after
     * the media player has been setup and configured by the constructor.
     */
    // -- Media Player Event Functions

    /**
     * Begin playing back the video from the current position
     */
    public void playVideo()
    {
        //System.out.println("VP: " + dmp.getCurrentPosition());
        mediaPlayer.play();
        //System.out.println("VP: " + dmp.getCurrentPosition());
    }

    /**
     * Pause the video at its current position.
     *
     * <p>
     * TODO: Remove the print statement
     * After a 75 ms delay, print out the current time of the video.
     */
    public long pauseVideo()
    {
        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
        }

        try
        {
            Thread.sleep(75);
        }
        catch(InterruptedException e)
        {
            System.err.println(" -- Error sleeping for 75 ms in nextFrame()");
        }

        System.out.println("Media Player Time: " + mediaPlayer.getTime());

        return mediaPlayer.getTime();
    }

    /**
     * Fast Forward or Rewind the video back by the specified amount.
     *
     * @param amount amount to skip video by
     */
    public void skipVideo(int amount)
    {
        mediaPlayer.skip(amount);
    }

    /**
     * Advance the video by one frame
     *
     * @return Returns the current time for use for other classes to determine where to sync up with the video at
     */
    public long nextFrame()
    {
        long currTime = System.currentTimeMillis();

        //If it's been half a second since the last advance, go ahead and advance.
        if(currTime - lastSystemTime >= 500)
        {
            System.out.println("\n **** nextFrame() ****");

            mediaPlayer.nextFrame();

            lastSystemTime = currTime;
            System.out.println("Player Time (ms): " + mediaPlayer.getTime());
        }

        this.setVideoTimeLabel(mediaPlayer.getTime());

        return mediaPlayer.getTime();
    }

    /**
     * Reverse the video by one frame
     *
     * FIXME: Fix the timings so that it will accurately move the video back by one frame. Currently moving
     * the video backwards 1/2 second in time. This appears to vary based on the type of codec used.
     * For example, the Casio Camera's can only do 500ms jumps, while EvoCam Recordings can
     * handle ~250ms jumps. This may not be possible due to how VLC itself operates internally.
     *
     * @return Returns the current time for use for other classes to determine where to sync up with the video at
     */
    public long previousFrame()
    {
        long currentTime = mediaPlayer.getTime();

        // Frame timing is calculated using 1000 / mediaPlayer.getFps() and done when the video loads.
        // It is the same value as the last line in the video information block in the console text
        long newTime = (long) (currentTime - 2000 / mediaPlayer.getFps());

        System.out.println("\n**** previousFrame() ****");
        System.out.println("Time to jump to (ms): " + newTime);

        jumpToTime(newTime);

        this.setVideoTimeLabel(mediaPlayer.getTime());

        return newTime;
    }

    /**
     * Jumps the video to a specific point in the video time.
     *
     * @param time - Time since the beginning in milliseconds
     */
    public void jumpToTime(long time)
    {
        System.out.println("\n **** jumpToTime(long time) ****");

        //Restart media player if it has previously stopped.
        //This could happen if the video finishes or was previously set to a time larger than the video length.
        //Also play if the data model is playing, since that could indicate jumping from outside the video bounds to a valid position.
        if(((time < videoLength) && (dmp.isPlaying())) || (dmp.isPlaying() && !mediaPlayer.isPlaying()))
        {
            boolean videoStarted = (mediaPlayer.getTime() == -1);

            mediaPlayer.play();
            if(videoStarted)
            {
                try
                {
                    Thread.sleep(75);
                }
                catch(InterruptedException e)
                {
                    System.err.println(" -- Error sleeping for 75 ms while waiting for video to start!");
                }
            }
        }

        System.out.println(playerID + ") Jumping Video To Time (ms): " + time + " on " + videoFile.getName());

        mediaPlayer.setTime(time);
    }

    /**
     * Adjusts the playback rate for the video.
     *
     * @param rate - 0.5 is half speed, 1.0 is normal speed, 2.0 is double speed, etc...
     */
    public void setPlaybackSpeed(float rate)
    {
        mediaPlayer.setRate(rate);
    }

    /**
     * Returns the current video time in milliseconds since the start of the video.
     *
     * @return The time to be returned
     */
    public long getVideoTime()
    {
        return mediaPlayer.getTime();
    }

    /**
     * Returns the number of milliseconds in the current video.
     *
     * @return video length
     */
    public long getVideoLength()
    {
        return videoLength;
    }

    /**
     * Update the label on the movie frame to the current video time.
     *
     * @param time - Currently this is the time since the beginning in milliseconds
     */
    private void setVideoTimeLabel(long time)
    {
        label_PlaybackTime.setText(convertToTimeFormat(time));
    }

    /**
     * Returns the media player being used so we can access elements of it for use with the other Java classes
     *
     * @return {@link EmbeddedMediaPlayer} - Returns the media player
     */
    public MediaPlayer getMediaPlayer()
    {
        return this.mediaPlayer;
    }

    /**
     * Notifies the model that the time has changed so that other time based components can be kept in sync.
     */
    private void notifyModelOfTimeChange()
    {
        // If the player ID is 1, it gets to update the model with the current video time.
        if(playerID == 1)
        {
            // Perform notification to model of time change.
            dmp.setCurrentTime(this.mediaPlayer.getTime());
        }
    }

    /**
     * Converts a time value into HH:MM:SS.sss format for display in the video player.
     *
     * @param msTime time in milliseconds
     * @return formatted time string
     */
    public static String convertToTimeFormat(long msTime)
    {
        int millis = (int) (msTime - ((msTime / 1000) * 1000));
        int seconds = (int) (msTime / 1000);

        while(seconds > 59)
        {
            seconds -= 60;
        }

        int minutes = (int) (msTime / 1000) / 60;
        int hours = minutes / 60;
        minutes = minutes % 60;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }
    
    // -- Java Event Listeners
    // -- NOTE: keyPressed and stateChanged are temporary listener while the Video player is being developed to full functionality

    public void update(Observable arg0, Object arg1)
    {
        if(arg1 instanceof String)
        {
            if(arg1.equals("Present"))
            {
                this.toFront();
            }
        }
    }

    public void keyPressed(KeyEvent ke)
    {
        if(ke.getKeyCode() == KeyEvent.VK_RIGHT)
        {
            nextFrame();
        }
        else if(ke.getKeyCode() == KeyEvent.VK_LEFT)
        {
            previousFrame();
        }
        else if(ke.getKeyCode() == KeyEvent.VK_SPACE)
        {
            if(mediaPlayer.isPlaying())
            {
                pauseVideo();
            }
            else
            {
                playVideo();
            }
        }
    }

    public void stateChanged(ChangeEvent ce)
    {

        if(ce.getSource() instanceof JSlider)
        {
            JSlider slider = (JSlider) ce.getSource();

            if(slider.getName().equals("Rate"))
            {
                int value = ((JSlider) ce.getSource()).getValue();

                switch(value)
                {
                    case 0:
                        setPlaybackSpeed((float) .25);
                        break;

                    case 1:
                        setPlaybackSpeed((float) .50);
                        break;

                    case 2:
                        setPlaybackSpeed((float) 1);
                        break;

                    case 3:
                        setPlaybackSpeed((float) 2);
                        break;

                    case 4:
                        setPlaybackSpeed((float) 4);
                        break;

                    case 5:
                        setPlaybackSpeed((float) 8);
                        break;
                }
            }
            else if(slider.getName().equals("Position"))
            {
                jumpToTime(slider.getValue());
            }
        }
    }

    /**
     * Invoked when the component's position changes
     */
    public void componentMoved(ComponentEvent ce)
    {
    }

    /**
     * Invoked when then component's size changes
     */
    public void componentResized(ComponentEvent ce)
    {
    }

    /**
     * Invoked when VLC notifies Java that the time has changed
     *
     * <p>
     * Note: this does not happen with each frame that changes. It only appears to happen around every few keyframes (approximately 300 milliseconds)
     */
    public void timeChanged(MediaPlayer mp, long time)
    {
        if(vlc_debug)
            System.out.println("Time Change Detected " + time);

        // Notifies the model that the time has changed.
        notifyModelOfTimeChange();

        // Update the time label to the current time as it has now changed.
        setVideoTimeLabel(time);
    }

    /*** Offset for each individual video file ***/
    public void setOffset(long time)
    {
        long jump = getVideoTime() - offset + time;
        offset = time;
        jumpToTime(jump);
    }

    public void setOffsetText(long l)
    {
        video_panel.setOffsetText(l);
        setOffset(l);
    }

    /*** Offset for each individual video file ***/
    public long getOffset()
    {
        return offset;
    }

    public void openWindow()
    {
        if(mediaPlayer != null)
        {
            //Resume the video at the specific time
            if(!mediaPlayer.isPlaying() && this.dmp.isPlaying())
            {
                this.jumpToTime(this.dmp.getCurrentPosition());
                mediaPlayer.play();
            }

            // Show the video player
            this.setVisible(true);
        }
    }

    public void centerWindow()
    {
        this.setLocationRelativeTo(null);
    }

    /**
     * Invoked when the window close button is pressed.
     */
    public void windowClosing(WindowEvent arg0)
    {
        if(mediaPlayer != null)
        {
            //Pause video so that the direct media player onDisplay callback doesn't receive any more frames
            if(mediaPlayer.isPlaying())
            {
                mediaPlayer.pause();
            }

            // Hide the video player
            this.setVisible(false);
        }
    }

    /**
     * Performed when the VideoPlayer needs to be shut down - either by itself or by some external caller.
     * It releases all VLC libraries before notifying the data model to unregister the video player.
     */
    public void performShutdown(boolean self)
    {
        if(mediaPlayer != null)
        {
            //Pause video so that the direct media player onDisplay callback doesn't receive any more frames
            if(mediaPlayer.isPlaying())
            {
                mediaPlayer.pause();
            }

            // Hide the video player
            this.setVisible(false);

            // Release the Media Player & Media Player Components
            this.mediaPlayer.release();

            //Since only one media player component will be instantiated, ensure the correct one is released
            if(useDirectMediaPlayer)
            {
                this.directMediaPlayerComponent.release();
                directMediaPlayerComponent = null;
            }
            else
            {
                this.embeddedMediaPlayerComponent.release();
                embeddedMediaPlayerComponent = null;
            }

            mediaPlayer = null;
        }

        //this.embeddedMediaPlayerComponent.release();

        // Dispose of all GUI components
        //Check if the window is displayable first so that if dispose was already called, VideoSync doesn't lock up.
        if(isDisplayable())
        {
            this.dispose();
        }

        // Notify the Data Model that the video is going away only if the shutdown task was called by windowClosing
        if(self)
        {
            this.dmp.unregisterVideo(this);
        }
    }

    /*
     * The following functions pertain to the various implementations that are currently not being used by the class.
     */
    // -- MediaPlayerEventListener methods
    // -- NOTE: None of the following are currently implemented in this version

    /*** ***/
    public void videoOutput(MediaPlayer player, int n_vids)
    {
        if(vlc_debug)
            System.out.println(n_vids + " video output fired on " + player);
    }

    public void backward(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("Backward " + arg0);
    }

    public void buffering(MediaPlayer arg0, float arg1)
    {
        if(vlc_debug)
            System.out.println("Buffering " + arg0 + " " + arg1);
    }

    public void endOfSubItems(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("endOfSubItems " + arg0);
    }

    public void error(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("error " + arg0);
    }

    public void finished(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("finished " + arg0);

        if(useDirectMediaPlayer)
        {
            //Restart the video and skip to the end.
            //This fixes crashing on any calls, direct or indirect, to mediaPlayer.getTime()
            mediaPlayer.play();
            mediaPlayer.setPosition(1.0f);
            mediaPlayer.pause();

            //Sleep is needed to ensure media player has actually restarted by the time it is next used.
            try
            {
                Thread.sleep(75);
            }
            catch(InterruptedException e)
            {
                System.err.println(" -- Error sleeping for 75 ms while pausing at end of video!");
            }

            //Tell render panel that this timestamp is the final frame.
            directRenderPanel.displayVideoFinished();
        }
    }

    public void forward(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("forward " + arg0);
    }

    public void lengthChanged(MediaPlayer arg0, long arg1)
    {
        if(vlc_debug)
            System.out.println("lengthChanged " + arg0 + " " + arg1);
    }

    public void mediaChanged(MediaPlayer arg0, libvlc_media_t arg1, String arg2)
    {
        if(vlc_debug)
            System.out.println("mediaChanged " + arg0 + " " + arg1 + " " + arg2);

        mediaPlayerReadyFired = false;
    }

    public void mediaDurationChanged(MediaPlayer arg0, long arg1)
    {
        if(vlc_debug)
            System.out.println("mediaDurationChanged " + arg0 + " " + arg1);
    }

    public void mediaFreed(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("MediaPlayer " + arg0);
    }

    public void mediaMetaChanged(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("mediaMetaChanged " + arg0 + " " + arg1);
    }

    public void mediaParsedChanged(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("mediaParsedChanged " + arg0 + " " + arg1);
    }

    public void mediaStateChanged(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("mediaStateChanged " + arg0 + " " + arg1);
    }

    public void mediaSubItemAdded(MediaPlayer arg0, libvlc_media_t arg1)
    {
        if(vlc_debug)
            System.out.println("mediaSubItemAdded " + arg0 + " " + arg1);
    }

    public void mediaPlayerReady(MediaPlayer mediaPLayer)
    {
        if(vlc_debug)
            System.out.println("Media player is ready on " + mediaPlayer);

        setVLCReadyFlag(true);
    }

    public void newMedia(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("newMedia " + arg0);
    }

    public void opening(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("opening " + arg0);
    }

    public void pausableChanged(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("pausableChanged " + arg0 + " " + arg1);
    }

    public void paused(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("paused " + arg0);
    }

    public void playing(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("playing " + arg0);
    }

    public void positionChanged(MediaPlayer arg0, float newPosition)
    {
        if(vlc_debug)
            System.out.println("positionChanged " + arg0);

        if(!mediaPlayerReadyFired && newPosition > 0)
        {
            mediaPlayerReadyFired = true;
            mediaPlayerReady(arg0);
        }
    }

    public void seekableChanged(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("seekableChanged " + arg0 + " " + arg1);
    }

    public void snapshotTaken(MediaPlayer arg0, String arg1)
    {
        if(vlc_debug)
            System.out.println("snapshotTaken " + arg0 + " " + arg1);
    }

    public void stopped(MediaPlayer arg0)
    {
        if(vlc_debug)
            System.out.println("stopped " + arg0);

        mediaPlayerReadyFired = false;
    }

    public void subItemFinished(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("subItemFinished " + arg0 + " " + arg1);
    }

    public void subItemPlayed(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("subItemPlayed " + arg0 + " " + arg1);
    }

    public void titleChanged(MediaPlayer arg0, int arg1)
    {
        if(vlc_debug)
            System.out.println("titleChanged " + arg0 + " " + arg1);
    }

    public void componentHidden(ComponentEvent arg0)
    {
        if(vlc_debug)
            System.out.println("componentHidden " + arg0);
    }

    public void componentShown(ComponentEvent arg0)
    {
        if(vlc_debug)
            System.out.println("componentShown " + arg0);
    }

    public void keyReleased(KeyEvent e)
    {
        if(vlc_debug)
            System.out.println("keyReleased " + e);
    }

    public void keyTyped(KeyEvent e)
    {
        if(vlc_debug)
            System.out.println("keyTyped " + e);
    }

    public void windowActivated(WindowEvent e)
    {
        if(vlc_debug)
            System.out.println("windowActivated " + e);
    }

    public void windowClosed(WindowEvent e)
    {
        if(vlc_debug)
            System.out.println("windowClosed " + e);
    }

    public void windowDeactivated(WindowEvent e)
    {
        if(vlc_debug)
            System.out.println("windowDeactivated " + e);
    }

    public void windowDeiconified(WindowEvent e)
    {
        if(vlc_debug)
            System.out.println("windowDeiconified " + e);
    }

    public void windowIconified(WindowEvent e)
    {
        if(vlc_debug)
            System.out.println("windowIconified " + e);
    }

    public void windowOpened(WindowEvent e)
    {
        if(vlc_debug)
            System.out.println("windowOpened " + e);
    }

    public void scrambledChanged(MediaPlayer m, int i)
    {
        if(vlc_debug)
            System.out.println("scrambledChanged " + m + " " + i);
    }

    public void mediaSubItemTreeAdded(MediaPlayer m, libvlc_media_t l)
    {
        if(vlc_debug)
            System.out.println("mediaSubItemTreeAdded " + m + " " + l);
    }

    public void elementaryStreamSelected(MediaPlayer m, int stream, int id)
    {
        if(vlc_debug)
            System.out.println("elementaryStreamSelected " + m + " " + stream + " " + id);
    }

    public void elementaryStreamDeleted(MediaPlayer m, int stream, int id)
    {
        if(vlc_debug)
            System.out.println("elementaryStreamDeleted " + m + " " + stream + " " + id);
    }

    public void elementaryStreamAdded(MediaPlayer m, int stream, int id)
    {
        if(vlc_debug)
            System.out.println("elementaryStreamAdded " + m + " " + stream + " " + id);
    }


    /***********
     * THIS IS THE BASICS FOR DRAWING ON THE CANVAS OF THE VIDEO PLAYER
     * @author caltrans
     *
     * NOTE: USED https://github.com/caprica/vlcj/blob/master/src/test/java/uk/co/caprica/vlcj/test/inputlistener/InputListenerTest.java AS THE BASIS FOR SETTING UP THE LISTENERS
     */


    //private int startX, startY;
    public void drawOnVideoPlayer()
    {
        System.out.println("Drawing on Video Player");


        //mediaPlayerComponent.getVideoSurface().paintAll(((Graphics2D)g).drawRect(30, 30, 50, 50));


    }


    @Override
    public void mouseClicked(MouseEvent arg0)
    {

    }

    @Override
    public void mouseEntered(MouseEvent arg0)
    {

    }

    @Override
    public void mouseExited(MouseEvent arg0)
    {

    }

    @Override
    public void mousePressed(MouseEvent arg0)
    {
        System.out.println(arg0);
    }

    @Override
    public void mouseReleased(MouseEvent arg0)
    {
        System.out.println(arg0);
        this.drawOnVideoPlayer();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent arg0)
    {


    }

    @Override
    public void mouseDragged(MouseEvent e)
    {

    }

    @Override
    public void mouseMoved(MouseEvent e)
    {

    }

    /***  ***/
    private void setVLCReadyFlag(boolean state)
    {
        vlc_is_ready = state;
    }

    public boolean isVLCReady()
    {
        return vlc_is_ready;
    }

    public void setVideoPanel(VideoPanel vp)
    {
        video_panel = vp;
    }

    public void addRegion(Region videoRegion)
    {
        if(useDirectMediaPlayer && (directRenderPanel != null))
        {
            directRenderPanel.addRegion(videoRegion);
        }
        else
        {
            System.out.println("Unable to add video region to render panel - panel is null or not using direct playback!");
        }
    }

    public void removeRegion(Region videoRegion)
    {
        if(useDirectMediaPlayer && (directRenderPanel != null))
        {
            directRenderPanel.removeRegion(videoRegion);
        }
        else
        {
            System.out.println("Unable to remove video region from render panel - panel is null or not using direct playback!");
        }
    }

    /**
     * Replaces oldRegion with newRegion in the DirectVideoRenderPanel.
     *
     * @param oldRegion Region to be removed
     * @param newRegion Region to replace oldRegion
     */
    public void replaceRegion(Region oldRegion, Region newRegion)
    {
        if(useDirectMediaPlayer && (directRenderPanel != null))
        {
            directRenderPanel.replaceRegion(oldRegion, newRegion);
        }
        else
        {
            System.out.println("Unable to replace video region from render panel - panel is null or not using direct playback!");
        }
    }

    /**
     * Return all of the regions in this video player.
     *
     * @return Vector of type Vector<Region> containing all of the region in the VideoPlayer.
     */
    public Vector<Region> getRegions()
    {
        return directRenderPanel.getDataRegions();
    }

    /**
     * Returns whether this VideoPlayer is saved or not.
     *
     * @return boolean value representing whether this VideoPlayer is saved or not.
     */
    public boolean isSaved()
    {
        return isSaved;
    }

    /**
     * Sets the VideoPlayer saved state based on input.
     *
     * @param b Boolean value to which to set the saved state of this VideoPlayer to
     */
    public void setSaved(boolean b)
    {
        isSaved = b;
    }
}
