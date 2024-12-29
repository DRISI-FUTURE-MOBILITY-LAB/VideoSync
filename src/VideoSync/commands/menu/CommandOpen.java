/*
 * ****************************************************************
 * File: 			CommandOpen.java
 * Date Created:  	June 4, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the menu to
 *                  open a directory containing the files to be
 *                  loaded into VideoSync
 *
 * ****************************************************************
 */
package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.objects.ImportFilter;
import VideoSync.objects.ObjectHolder;
import VideoSync.views.tabbed_panels.DataWindow;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;


public class CommandOpen extends AbstractAction
{
    private static final long serialVersionUID = 1L;

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Command Open Variable Declarations

    /**
     * Used to reference and notify the DataModel of any changes
     */
    private DataModel dm;

    /**
     * Used to reference and notify the DataWindow of any changes.
     */
    private DataWindow g;

    /**
     * Used for confirming we found a mapping file.
     */
    private File mpf;

    /**
     * Used for confirming we found a Log 170 data file.
     */
    private File dat;

    /**
     * Used for confirming we found a Old VideoSync mapping file.
     */
    private File vbm;

    /**
     * Used for confirming we found a C1 data file.
     */
    private File c1;

    /**
     * Used for confirming we found a config file.
     */
    private File config;

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Command Open Methods

    /**
     * Sets the references to the DataModel and Data Window
     *
     * @param dm Reference to DataModel
     * @param g  Reference to DataWindow
     */
    public void setTargets(DataModel dm, DataWindow g)
    {
        // Set the DataModel from the passed parameter
        this.dm = dm;
        // Set the DataWindow from the passed parameter.
        this.g = g;
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Command Open Action Methods

    /**
     * Called when the user selects the "Open" option from the File menu.
     *
     * @param ae ActionEvent triggered by "Open" button in File menu
     */
    public void actionPerformed(ActionEvent ae)
    {
        //Run a JavaFX directory chooser so that the native OS file picker is used.
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Open Project Directory");

        g.setEnabled(false);

        Platform.runLater(() -> {
            File chosenDirectory = dc.showDialog(null);

            //Need to switch back to Swing thread
            //Use several invokeAndWait instead of invokeLater to ensure certain tasks are completed before others execute.
            SwingUtilities.invokeLater(() -> {

                if(chosenDirectory != null)
                {
                    // Check to see if the Data Model has already loaded up any data files.
                    // If so, save the config file and remove all data that has been loaded.
                    if(dm.isDataLoaded())
                    {
                        if(dm.getEventLogFile() != null)
                        {
                            int option = JOptionPane.showConfirmDialog(g, "Save " + dm.getEventLogFile().getName() + "?", "Save Event Log", JOptionPane.YES_NO_OPTION);
                            if(option == JOptionPane.YES_OPTION)
                            {
                                dm.writeEventLogDBFile();
                            }
                        }
                        dm.writeConfigFile();
                        dm.removeAllData();
                        dm.resetGraphsPane();
                        dat = null;
                        c1 = null;
                        vbm = null;
                        mpf = null;
                        config = null;
                    }

                    // Get a list of all the files that were found within the specified directory.
                    File[] files = chosenDirectory.listFiles();

                    if(files != null)
                    {
                        // Loop through all of the files located in the directory.
                        for(File f : files)
                        {
                            // Ensure that the file is not hidden.
                            if(!isFileHidden(f))
                            {
                                // Begin to check and see if the file can be loaded into VideoSync.
                                loadFile(f);
                            }
                        }
                    }

                    // If no mapping files have been found, see if
                    // a vbm file can be used instead.
                    if((mpf == null) && (vbm != null))
                    {    //Old format
                        // Update the data model with the VBM file.
                        dm.setVBMFile(vbm);
                    }

                    // If the config file is null, allow user to choose a config file from elsewhere
                    if(config == null)
                        promptMissingFile("Config");
                    else
                        dm.setConfigFile(config);

                    // If the data file is null and the c1 file is null,
                    // we need to notify the user that there was an error finding the file
                    // and allow them to choose a data file from elsewhere.
                    if(dat == null && c1 == null)
                        displayDataError();

                    // If the mapping file and vbm file is null,
                    // notify the user that a file wasn't found and ask if they want to
                    // select one or have one be automatically created.
                    if(mpf == null && vbm == null)
                        promptMissingFile("Mapping");

                    // Note: We don't use an if/else statement here because if both a
                    // c1 file and log 170 file were found, we want to load them both
                    // up. Doing an if/else will only load one of them.


                    // If the dat file is not null, then have the data model load it.
                    if(dat != null)
                    {
                        dm.set170Data(dat);
                    }

                    // If the c1 file is not null, then have the data model load it.
                    if(c1 != null)
                    {
                        dm.setC1Data(c1);
                    }

                    dm.organizeData();
                    //Load last running program's selected options
                    dm.readConfigFile();
                }

                g.setEnabled(true);
                g.requestFocus();
            });
        });
    }

    /**
     * Display an error message to the user that we were unable to find a
     * C1 or Log 170 Data file.
     */
    private void displayDataError()
    {
        // Set the button options to be displayed to the user.
        Object[] options = {"Yes", "No", "Quit"};

        // Return the result from the Option Dialog, using the Data Window (g) as the parent view.
        int n = JOptionPane.showOptionDialog(g,
                "No data (.dat or .c1) file was found, would you like to select one?",
                null,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);

        // If the user chooses Yes, then allow them to select a new data file
        if(n == 0)
        {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select a data file...");
            ImportFilter.Utils.setDataExtensionFilters(fc);

            selectNewDataFile(fc);

            // If either the dat file or c1 file has been found,
            // we can exit the recursion loop and continue on loading data.
            if((dat == null) && (c1 == null))
            {
                // Use recursion instead of previous while loop to avoid over complicating JavaFX/Swing thread interactions
                displayDataError();
            }
        }
        // If the user selects 1 or 2, we can exit the program and quit
        else if(n == 2)
        {
            System.exit(1);
        }
    }

    /**
     * Display an error message to the user that we were unable to find a
     * mapping or config file.
     */
    private void promptMissingFile(String filetype)
    {

        // Set the button options to be displayed to the user.
        Object[] options = {"Yes", "No"};

        // Return the result from the Option Dialog, using the Data Window (g) as the parent view.
        int n = JOptionPane.showOptionDialog(g,
                filetype + " not found. Would you like to select one?\nOne will be automatically generated if 'No' is selected",
                null,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);

        // If the user chooses Yes, then allow them to select a new data file
        //If not, use default values.
        if(n == JOptionPane.YES_OPTION)
        {
            FileChooser fc = new FileChooser();

            if(filetype.equals("Mapping")) fc.setTitle("Select a mapping file (.mpf/.vbm)...");
            else if(filetype.equals("Config")) fc.setTitle("Select a config ('config'/'config.db')");
            ImportFilter.Utils.setMappingExtensionFilters(fc);

            selectNewDataFile(fc);

            // If either the dat file or c1 file has been found,
            // we can exit the recursion loop and continue on loading data.
            if((dat == null) && (c1 == null))
            {
                //Use recursion instead of previous while loop to avoid over complicating JavaFX/Swing thread interactions
                promptMissingFile(filetype);
            }
        }
    }

    /**
     * Opens file chooser, waits for a new file to be selected, and loads the file
     *
     * @param fc FileChooser object to open Open File dialog window
     */
    private void selectNewDataFile(FileChooser fc)
    {
        // Inner class cannot set values in outer class functions. However, it can make function calls.
        // Keeps track of whether the runnable is running, and the file the user selected.
        ObjectHolder<File> selectedFile = new ObjectHolder<>();

        //FileChooser showOpenDialog needs to run on JavaFX thread
        Runnable fileChooserRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                File file = fc.showOpenDialog(null);

                if(file != null)
                {
                    selectedFile.setValue(file);
                }

                synchronized(this)
                {
                    this.notifyAll();
                }
            }
        };

        g.setEnabled(false);

        Platform.runLater(fileChooserRunnable);

        // Wait for the runnable to complete
        synchronized(fileChooserRunnable)
        {
            try
            {
                fileChooserRunnable.wait();
            }
            catch(InterruptedException ex)
            {
                ex.printStackTrace();
                System.out.println("Thread was interrupted waiting for JavaFX file chooser!");
            }
        }

        //By this point, the runnable is done.
        if(selectedFile.getValue() != null)
        {
            loadFile(selectedFile.getValue());
        }

        g.setEnabled(true);
        g.requestFocus();
    }

    /**
     * Set the appropriate files to be loaded based on their extensions.
     *
     * @param file Reference to the file we're loading
     */
    private void loadFile(File file)
    {
        // If the file is a mapping file, then set the local variable for the mapping file.
        if(file.getName().endsWith(".mpf"))
        {
            this.mpf = file;
            //Add mapping file to data model so that it isn't ignored if another mpf gets loaded.
            dm.addInputMappingFile(file.getPath());
        }
        // If the file is a old videosync mapping file, then set the local variable for the old videosync mapping file.
        else if(file.getName().endsWith(".vbm"))
        {
            this.vbm = file;
        }
        // If the file is a log 170 file, then set the local variable for the log 170 file.
        else if(file.getName().endsWith(".dat"))
        {
            this.dat = file;
        }
        // If the file is a c1 file, then set the local variable for the c1 file.
        else if(file.getName().endsWith(".c1"))
        {
            this.c1 = file;
        }
        // If the file is a movie file, then set the local variable for the movie file. Set video player iSaved flag to true.
        else if(ImportFilter.Utils.isExtensionVideo(ImportFilter.Utils.getExtension(file)))
        {
            this.dm.addVideoFile(file, true);
        }
        // If the file is a config file, set local variable for config file
        else if(file.getName().equals("config") || file.getName().equals("config.db"))
        {
            this.config = file;
        }
    }

    /**
     * Checks if the file is hidden on both Windows and Unix-like Operating Systems
     *
     * @param file File to check.
     * @return If the file is hidden or not.
     */
    private boolean isFileHidden(File file)
    {
        boolean isHidden = false;

        if(System.getProperty("os.name").startsWith("Windows"))
        {
            try
            {
                //On Windows, need to check DOS file attributes in order to know if file is hidden.
                Path filePath = Paths.get(file.getAbsolutePath());
                isHidden = (Boolean) Files.getAttribute(filePath, "dos:hidden", LinkOption.NOFOLLOW_LINKS);
            }
            catch(IOException ex)
            {
                ex.printStackTrace();
            }
        }
        else
        { //If the system is not windows, assume that system is Unix-like.
            //Unix systems just have the file name start with .
            isHidden = file.getName().startsWith(".");
        }

        return isHidden;
    }
}
