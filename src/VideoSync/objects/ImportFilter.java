/*
 * ****************************************************************
 * File: 			ImportFilter.java
 * Date Created:  	August 12, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To limit the file types that can be opened.
 *
 * ****************************************************************
 */
package VideoSync.objects;

import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import uk.co.caprica.vlcj.filter.VideoFileFilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ImportFilter extends FileFilter
{
    /**
     * Returns if a file can be opened up or not.
     */
    public boolean accept(File f)
    {
        // If the file is a directory we can go ahead and upload it.
        if(f.isDirectory())
            return true;

        // Gets the extension from the file to determine if it can be opened or not.
        String extension = Utils.getExtension(f);

        // If the extension is not null, then check to see if it can actually be opened.
        if(extension != null)
        {
            // Check to see if the extension is one we can use
            return Utils.isExtensionVideo(extension) || extension.equals(Utils.dat) || extension.equals(Utils.mpf) || extension.equals(Utils.vbm) || extension.equals(Utils.c1) || extension.equals(Utils.c1maxim);
        }

        // If we get to this point then we haven't found a valid file, then we return false.
        return false;
    }

    /**
     * This is used for making it quick and easy to access the file extensions
     */
    public static class Utils
    {
        public final static String[] videoExtensions = new VideoFileFilter().getExtensions();
        public final static String dat = "dat";
        public final static String mpf = "mpf";
        public final static String vbm = "vbm";
        public final static String c1 = "c1";
        public final static String c1maxim = "c1max";

        /**
         * Returns the extension of the file.
         *
         * @param f - The file passed
         * @return extension of the file
         */
        public static String getExtension(File f)
        {
            // Initialize the extension to null
            String ext = null;

            // Get the name of the file.
            String s = f.getName();

            // Get the index position of the last . for the extension
            int i = s.lastIndexOf('.');

            // As long as i > 0 and i < the length of the string, then get the extension
            if(i > 0 && i < s.length() - 1)
            {
                // Set the extension of the file from the substring of s.
                ext = s.substring(i + 1).toLowerCase();
            }

            // Return the extension of the file if one was found
            return ext;
        }

        /**
         * Returns if the given extension is a video format.
         *
         * @param extension The extension to check
         * @return Whether or not this is a video extension.
         */
        public static Boolean isExtensionVideo(String extension)
        {
            for(String videoExtension : videoExtensions)
            {
                if(videoExtension.equalsIgnoreCase(extension))
                {
                    return true;
                }
            }

            return false;
        }

        /**
         * Takes a JavaFX file chooser and adds all supported extensions
         *
         * @param fc JavaFX file chooser
         */
        public static void setAllExtensionFilters(FileChooser fc)
        {
            //Create filters
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files", "*.*");
            FileChooser.ExtensionFilter datFilter = new FileChooser.ExtensionFilter("Log170 Data", "*." + dat, "*.log", "*.vsc");
            FileChooser.ExtensionFilter c1Filter = new FileChooser.ExtensionFilter("C1 Data", "*." + c1);
            FileChooser.ExtensionFilter c1MaximFilter = new FileChooser.ExtensionFilter("C1 Maxim Data", "*." + c1maxim);
            FileChooser.ExtensionFilter mpfFilter = new FileChooser.ExtensionFilter("VideoSync Mapping Files", "*." + mpf);
            FileChooser.ExtensionFilter vbmFilter = new FileChooser.ExtensionFilter("Legacy VideoSync Mapping Files", "*." + vbm);

            ObservableList<FileChooser.ExtensionFilter> filters = fc.getExtensionFilters();

            //Video filters need to be built up per individual extension
            String[] convertedExtensions = new String[videoExtensions.length];
            for(int extensionIndex = 0; extensionIndex < videoExtensions.length; extensionIndex++)
            {
                convertedExtensions[extensionIndex] = "*." + videoExtensions[extensionIndex];
            }

            FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video Files", convertedExtensions);

            //Add filters
            filters.add(allFilter);
            filters.add(videoFilter);
            filters.add(c1Filter);
            filters.add(c1MaximFilter);
            filters.add(datFilter);
            filters.add(mpfFilter);
            filters.add(vbmFilter);
        }

        /**
         * Takes a JavaFX file chooser and adds all data extensions
         *
         * @param fc JavaFX file chooser
         */
        public static void setDataExtensionFilters(FileChooser fc)
        {
            //Create filters
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files", "*.*");
            FileChooser.ExtensionFilter dataFilter = new FileChooser.ExtensionFilter("Data files", "*.c1", "*.dat", "*.log", "*.vsc");
            FileChooser.ExtensionFilter datFilter = new FileChooser.ExtensionFilter("Log170 Data", "*." + dat, "*.log", "*.vsc");
            FileChooser.ExtensionFilter c1Filter = new FileChooser.ExtensionFilter("C1 Data", "*." + c1);
            FileChooser.ExtensionFilter c1MaximFilter = new FileChooser.ExtensionFilter("C1 Maxim Data", "*." + c1maxim);

            //Add filters
            ObservableList<FileChooser.ExtensionFilter> filters = fc.getExtensionFilters();
            filters.add(allFilter);
            filters.add(dataFilter);
            filters.add(c1Filter);
            filters.add(c1MaximFilter);
            filters.add(datFilter);

            //Default to all data filters.
            fc.setSelectedExtensionFilter(dataFilter);
        }

        /**
         * Takes a JavaFX file chooser and adds all mapping extensions
         *
         * @param fc JavaFX file chooser
         */
        public static void setMappingExtensionFilters(FileChooser fc)
        {
            //Create filters
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files", "*.*");
            FileChooser.ExtensionFilter mpfFilter = new FileChooser.ExtensionFilter("VideoSync Mapping Files", "*." + mpf);
            FileChooser.ExtensionFilter vbmFilter = new FileChooser.ExtensionFilter("Legacy VideoSync Mapping Files", "*." + vbm);
            FileChooser.ExtensionFilter mapFilter = new FileChooser.ExtensionFilter("Mapping Files", "*." + mpf, "*." + vbm);

            //Add filters
            ObservableList<FileChooser.ExtensionFilter> filters = fc.getExtensionFilters();
            filters.add(allFilter);
            filters.add(mapFilter);
            filters.add(mpfFilter);
            filters.add(vbmFilter);


            //Default to all mapping files
            fc.setSelectedExtensionFilter(mapFilter);
        }

        public static void setVideoExtensionFilters(FileChooser fc)
        {
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files", "*.*");

            //Video filters need to be built up per individual extension
            String[] convertedExtensions = new String[videoExtensions.length];
            for(int extensionIndex = 0; extensionIndex < videoExtensions.length; extensionIndex++)
            {
                convertedExtensions[extensionIndex] = "*." + videoExtensions[extensionIndex];
            }

            FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video Files", convertedExtensions);

            ObservableList<FileChooser.ExtensionFilter> filters = fc.getExtensionFilters();

            filters.add(allFilter);
            filters.add(videoFilter);

            //Default to video files
            fc.setSelectedExtensionFilter(videoFilter);
        }
    }

    // -- This is an abstract method from FileFilter and is required with the extension

    /**
     * Returns a description of the Import Filter
     */
    public String getDescription()
    {
        return null;
    }
}
