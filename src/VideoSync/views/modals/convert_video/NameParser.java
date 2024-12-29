package VideoSync.views.modals.convert_video;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameParser {
    public static String getExtension(String filePath) {
        int i = filePath.lastIndexOf('.');
        String extension= "";
        if (i > 0) {
            extension = filePath.substring(i+1);
        }
        return extension.toLowerCase();
    }

    public static String setExtension(String filePath, String extension) {
        int i = filePath.lastIndexOf('.');
        if (i == -1) {
            return filePath + "." + extension;
        }
        String newFileName= filePath.substring(0,i+1);
        if (i > 0) {
            newFileName += extension;
        }
        return newFileName;
    }

    public static String wrapInQuotes(String s) {
        return "\"" + s + "\"";
    }

    public static String getParentFolder(String filePath) {
        String parentDir = String.valueOf(new File(filePath).getParentFile());
        return parentDir;
    }

    public static String getMergedName(ArrayList<String> fileNames) {
        String mergeName = "merged";
        try {
            ArrayList<Long> starts = new ArrayList<>();
            ArrayList<Long> ends = new ArrayList<>();
            for (String fileName : fileNames) {
                fileName = new File(fileName).getName();
                //Remove extension
                int i = fileName.lastIndexOf('.');
                fileName = fileName.substring(0,fileName.lastIndexOf('.'));
                String[] times = fileName.split("_");
                starts.add(Long.valueOf(times[3]));
                ends.add(Long.valueOf(times[4]));
            }
            mergeName = "merged_" + String.valueOf(Collections.min(starts)) +
                    "_" +
                    String.valueOf(Collections.max(ends));
        } catch (Exception e) {
            System.out.println("Input files don't match naming scheme, using " +
                    "default merge name");
            mergeName = "merged";
        }
        // Re-add extension
        mergeName = setExtension(mergeName, "mp4");
        return mergeName;
    }

    public static String getTrimmedName(String filename) {
        int ext = filename.lastIndexOf(".");

        String result =
                filename.substring(0, ext) + "_trimmed" + filename.substring(ext);
        return result;
    }
}
