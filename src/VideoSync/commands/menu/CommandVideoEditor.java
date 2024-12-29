package VideoSync.commands.menu;

import VideoSync.models.DataModelProxy;
import VideoSync.views.modals.convert_video.FFmpeg;
import VideoSync.views.modals.video_editor.VideoEditor;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandVideoEditor extends AbstractAction
{
    private final VideoEditor cve;

    private DataWindow dataWindow;

    public CommandVideoEditor()
    {
        // Construct the InputMapping so we can use it
        cve = new VideoEditor();
    }

    public void setTargets(DataModelProxy dmp, DataWindow dw)
    {
        // Set the data model proxy for the Input Mapping
        this.dataWindow = dw;
    }

    public VideoEditor getConvertVideo()
    {
        return this.cve;
    }

    public void actionPerformed(ActionEvent e)
    {
        if(!FFmpeg.checkInstalled()) {
            JOptionPane.showMessageDialog(dataWindow,
                    "FFmpeg is not installed, please install it");
        }
        if(!cve.isVisible() && FFmpeg.checkInstalled())
        {
            cve.setLocationRelativeTo(dataWindow);
            cve.setVisible(true);
        }
    }
}
