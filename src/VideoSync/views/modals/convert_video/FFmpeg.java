package VideoSync.views.modals.convert_video;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static VideoSync.views.modals.convert_video.NameParser.*;

public class FFmpeg {
    private static String[] ffmpegPaths = new String[] {
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
            Paths.get(System.getProperty("user.dir"), "Jars", "ffmpeg",
                    "ffmpeg.exe").toString()
    };
    static String ffmpegPath = ffmpegPaths[0];

    public static boolean execFfmpeg(String[] cmd) {
        Process processDuration = null;
        System.out.println(Arrays.toString(cmd));
        try {
            processDuration = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder strBuild = new StringBuilder();
        try (BufferedReader processOutputReader =
                     new BufferedReader(
                             new InputStreamReader(processDuration.getInputStream(), Charset.defaultCharset()));)
        {
            String line;
            while ((line = processOutputReader.readLine()) != null) {
                System.out.println("ffmpeg: " + line);
            }
            processDuration.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    // Convert DAV to MP4
    public static ArrayList<String> convertVideos(ArrayList<String> inputFiles, double fps) {
        ArrayList<String> convertedFiles = new ArrayList<String>();
        for (String inputFile : inputFiles) {
            String outputFile = setExtension(inputFile, "mp4");
            if (!new File(outputFile).exists()) {
                if (getExtension(inputFile).equals("dav")) {
                    String[] cmd = {getFfmpegPath(), "-r", String.valueOf(fps), "-i",
                            NameParser.wrapInQuotes(inputFile), "-c:v",
                            "libx264",
                            NameParser.wrapInQuotes(outputFile)};
                    boolean status = execFfmpeg(cmd);
                } else if (getExtension(inputFile).equals("mov")) {
                    String[] cmd = {getFfmpegPath(),
                            "-i", inputFile, "-vcodec", "h264",
                            "-acodec", "aac",
                            NameParser.wrapInQuotes(outputFile)};
                    boolean status = execFfmpeg(cmd);
                }
            }
            convertedFiles.add(outputFile);
        }
        return convertedFiles;
    }

    // Converts MP4 to TS so they can be concatenated
    public static ArrayList<String> createIntermediates(ArrayList<String> inputFiles) {
        ArrayList<String> intermediateFiles = new ArrayList<String>();

        for (String inputVideo : inputFiles) {
            String outputFile = setExtension(inputVideo, "ts");
            if (!new File(outputFile).exists())
            {
                String[] cmd = {getFfmpegPath(), "-i",
                        NameParser.wrapInQuotes(inputVideo), "-c",
                        "copy", "-bsf:v", "h264_mp4toannexb", "-f", "mpegts",
                        NameParser.wrapInQuotes(outputFile)};
                boolean status = execFfmpeg(cmd);
            }
            intermediateFiles.add(outputFile);
        }
        return intermediateFiles;
    }

    // Merges ts files
    public static ArrayList<String> mergeVideos(ArrayList<String> intermediates, double fps) {
        ArrayList<String> outputFiles = new ArrayList<>();
        String mergedFile = String.valueOf(Paths.get(NameParser.getParentFolder(intermediates.get(0)))) + "\\" +
                getMergedName(intermediates);
        if (!new File(mergedFile).exists()) {
            String filesStr = "concat:";
            for (String intermediateFilePath : intermediates) {
                filesStr += intermediateFilePath + "|";
            }
            filesStr = filesStr.substring(0,filesStr.length()-1);
            String[] cmd = {getFfmpegPath(), "-r",
                    String.valueOf(fps), "-i", filesStr, "-c", "copy",
                    "-bsf:a",
                    "aac_adtstoasc", NameParser.wrapInQuotes(mergedFile)};
            execFfmpeg(cmd);
        }
        outputFiles.add(mergedFile);
        return outputFiles;
    }

    // Trim mp4 video
    public static String trimVideos(String inputMp4,
                                                String startTime,
                                                String endTime) {
        boolean status = false;
        String cutVideo = NameParser.getTrimmedName(inputMp4);
        if (!new File(cutVideo).exists()) {
            String[] cmd = {getFfmpegPath(),"-i",inputMp4,"-ss",startTime,"-to",
                    endTime,"-async","1","-strict","-2",cutVideo};
            for (String c : cmd) {
                System.out.print(c);
            }
            execFfmpeg(cmd);
        }
        return cutVideo;
    }

    public static boolean checkInstalled() {
        Runtime rt = Runtime.getRuntime();
        Process p = null;
        for (String path : ffmpegPaths) {
            if((new File(path)).exists()) {
                ffmpegPath = path;
                return true;
            } else {
                System.out.println("Not at " + path);
            }
        }
        return false;
    }

    public static String getFfmpegPath() {
        return ffmpegPath;
    }
}
