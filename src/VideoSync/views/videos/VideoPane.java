package VideoSync.views.videos;

import javax.swing.*;

public class VideoPane extends JPanel
{
    public VideoPane()
    {
        super(false);
    }

    /**
     * Create a new VideoPanel object, pass in the VideoPlayer, give the VideoPlayer a reference to the panel,
     * and add the new VideoPanel object into this VideoPane (JPanel).
     *
     * @param vp VideoPlayer object for which this VideoPanel is being created for
     */
    public void addVideoPanel(VideoPlayer vp)
    {
        VideoPanel temp = new VideoPanel(vp);
        vp.setVideoPanel(temp);
        add(temp);
    }
}
