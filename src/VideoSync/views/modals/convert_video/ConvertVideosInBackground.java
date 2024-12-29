package VideoSync.views.modals.convert_video;


import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class ConvertVideosInBackground extends SwingWorker<ArrayList<String>,
        Integer> {
    DefaultListModel<String> inputVideos;
    DefaultListModel<String> outputVideos;
    boolean DELETE_FILES = true;
    double fps;
    boolean merge;
    int progress; // TODO: Currently jumps to 100 but it should count up

    public ConvertVideosInBackground(DefaultListModel<String> inputVideos,
                                DefaultListModel<String> outputVideos,
                                     double fps, boolean merge) {
        this.inputVideos = inputVideos;
        this.outputVideos = outputVideos;
        this.fps = fps;
        this.merge = merge;
    }

    @Override
    public ArrayList<String> doInBackground() {
        ArrayList<String> videos;
        ArrayList<String> intermediateVideos;
        ArrayList<String> mergeVideos;

        videos = Collections.list(this.inputVideos.elements());
        this.outputVideos.clear();

        videos = FFmpeg.convertVideos(videos, this.fps);
        // MERGE
        if (this.merge) {
            intermediateVideos = FFmpeg.createIntermediates(videos);
            System.out.println(intermediateVideos);
            mergeVideos = FFmpeg.mergeVideos(intermediateVideos, this.fps);
            if (DELETE_FILES) {
                deleteFiles(intermediateVideos);
                deleteFiles(videos);
            }
            for (String video : mergeVideos) {
                this.outputVideos.addElement(video);
            }
            return mergeVideos;
        }
        for (String video : videos) {
            this.outputVideos.addElement(video);
        }
        return videos;
    }

    public void done() {
        // TODO progress bar
        progress = 100;
    }

    private static void deleteFiles(ArrayList<String> files) {
        return;
        //TODO: delete files
//        for (String file : files) {
//            try {
//                Files.delete(Paths.get(file));
//            } catch (IOException e) {
//                continue;
//            }
//        }
//        for (String file : files) {
//            try {
//                Files.delete(Paths.get(file));
//            } catch (IOException e) {
//                continue;
//            }
//        }
    }
}
