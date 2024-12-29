/*
 * ****************************************************************
 * File: 			VideoPanel.java
 * Date Created:  	December 19, 2016
 * Programmer:		Danny Hale
 *
 * Purpose:			Allows each video to maintain a separate offset
 * 					for the purpose of syncing videos recorded at
 * 					different time intervals.
 * ****************************************************************
 */
package VideoSync.views.videos;

import VideoSync.commands.video.CommandOpenVideo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class VideoPanel extends JPanel implements KeyListener
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /*** Reference to allow the video panel to change the time offset of the attached video. ***/
    private final VideoPlayer video_player;

    /*** Video Filename ***/
    private final JTextField movie_offset;

    /**
     * Button containing the command to open and close the respective video window.
     */
    private final JButton openVideoButton;

    public VideoPanel(VideoPlayer vp)
    {
        super(false);
        setLayout(new FlowLayout(FlowLayout.LEADING, 30, 20));

        video_player = vp;

        // Create the "Open Video" button and add it to the panel
        openVideoButton = new JButton();
        CommandOpenVideo cOpenVideo = new CommandOpenVideo();
        cOpenVideo.setTargets(video_player);
        openVideoButton.setAction(cOpenVideo);
        openVideoButton.setBounds(100, 75, 60, 28);
        openVideoButton.setText("Open Video Window");
        add(openVideoButton);

        movie_offset = new JTextField(12);
        movie_offset.addKeyListener(this);
        add(new JLabel(video_player.getVideoFile().getName() + " offset: "));
        add(movie_offset);

        setPreferredSize(new Dimension(720, 60));
    }

    public void setOffsetText(long l)
    {
        String temp = (l / 1000) + "." + (l % 1000);
        movie_offset.setText(temp);
    }

    public String getOffsetText()
    {
        return movie_offset.getText();
    }

    //Empty functions included as part of the key listener
    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
    }

    /*** Video Offset adjustment ***/
    @Override
    public void keyReleased(KeyEvent e)
    {
        String str_offset = getOffsetText();
        int neg = str_offset.indexOf('-');
        int sep = str_offset.indexOf('.');
        int start, end;
        long offset = 0;

        // Check for decimal number
        if(sep != -1)
        {
            end = sep;
            String temp = str_offset.substring(sep);
            if(temp.length() > 1)
            {
                System.out.println("millis: " + temp);
                offset = (long) (Double.parseDouble(temp) * 1000);
            }
        }
        else
        {
            end = str_offset.length();
        }

        // Check the sign of the value
        if(neg == -1)
        {
            start = 0;
        }
        else
        {
            start = 1;
        }

        if(((neg == -1) && (str_offset.length() > 0)) || (str_offset.length() > 1))
        {
            offset += Long.parseLong(str_offset.substring(start, end)) * 1000;

            System.out.println("Offset: " + offset);

            if(start == 0)
            {
                video_player.setOffset(offset);
            }
            else
            {
                video_player.setOffset(-offset);
            }
        }
        else
        {
            video_player.setOffset(0);
        }
    }
}
