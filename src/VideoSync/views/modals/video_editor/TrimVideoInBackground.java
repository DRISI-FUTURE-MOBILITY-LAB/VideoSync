package VideoSync.views.modals.convert_video;


import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class TrimVideoInBackground extends SwingWorker<String, Integer> {

    private String inputVideo;
    private String start;
    private String end;
    private String outputVideo;
    private JLabel outputVideoLabel;

    public TrimVideoInBackground(String inputVideo, String start, String end,
                                 JLabel outputLabel
                                 ) {
        this.inputVideo = inputVideo;
        this.start = start;
        this.end = end;
        this.outputVideo = "";
        this.outputVideoLabel = outputLabel;
    }

    @Override
    public String doInBackground() {
        outputVideoLabel.setText("");
        outputVideo = FFmpeg.trimVideos(this.inputVideo, start,
            end);
        return outputVideo;
    }

    public void done() {
        outputVideoLabel.setText(outputVideo);
    }
}
