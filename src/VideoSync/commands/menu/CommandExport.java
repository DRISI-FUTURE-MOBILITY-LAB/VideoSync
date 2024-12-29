/*
 * ****************************************************************
 * File: 			CommandExport.java
 * Date Created:  	June 4, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the menu to
 * 					save and export a directory containing the
 * 					files to the computer
 *
 * Programmer:      Jenzel Arevalo
 * Modified:        October 25, 2019
 *                  Refactored class from CommandSaveSession to
 *                  CommandExport
 * ****************************************************************
 */
package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.models.DataModelProxy;
import VideoSync.objects.InputMappingFile;
import VideoSync.views.tabbed_panels.DataWindow;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

public class CommandExport extends AbstractAction implements PropertyChangeListener
{
    private static final long serialVersionUID = 1L;

    /**
     * Buffer size for copying files
     */
    private static final int COPY_BUF_SIZE = 4096;

    /**
     * Reference to DataModel object for being able to fetch and save files
     */
    private DataModel dm;

    /**
     * Reference to proxy object for DataModel
     */
    private DataModelProxy dmp;

    /**
     * Used for referencing the main view
     */
    private DataWindow dataWindow;

    /**
     * Reference to instance of inner class used for saving project files
     */
    private SaveTask saveTask;

    /**
     * ProgressMonitor object to track progress of saving files
     */
    private ProgressMonitor pm;

    /**
     * Location to export project files to
     */
    private File pathFile;

    /**
     * Sets the references to the DataModel and Data Window
     *
     * @param dm Reference to DataModel
     * @param g  Reference to DataWindow
     *           FIXME: This should probably take a data model proxy, not the data model directly.
     */
    public void setTargets(DataModel dm, DataWindow g)
    {
        dmp = new DataModelProxy(dm);
        this.dm = dm;
        dataWindow = g;
    }

    /**
     * Called when the user selects the "Export" option from the File menu.
     *
     * @param e ActionEvent triggered by button press
     */
    public void actionPerformed(ActionEvent e)
    {
        initiateExport();
    }

    /**
     * Exports current project to a selected directory
     */
    private void initiateExport()
    {
        //Create a folder chooser for the export location.
        DirectoryChooser saveLocationChooser = new DirectoryChooser();
        saveLocationChooser.setTitle("Export Project to Directory");

        dataWindow.setEnabled(false);

        //Since we need to go into inner class runnable for JavaFX thread, "this" will no longer point to CommandExport.
        final CommandExport currentCommandExport = this;

        //Directory chooser must run on JavaFX thread
        Platform.runLater(() -> {
            pathFile = saveLocationChooser.showDialog(null);
            if(pathFile != null)
            {
                if(pathFile.getPath().equals(dm.getCurrentDirectory()))
                {
                    do
                    {
                        JOptionPane.showMessageDialog(dataWindow, "Cannot export to the current working directory.\nYou must export to another directory path.", "Invalid Path", JOptionPane.ERROR_MESSAGE);
                        pathFile = saveLocationChooser.showDialog(null);
                    }
                    while(pathFile.getPath().equals(dm.getCurrentDirectory()));
                }

                //Switch back to Swing thread to use file
                // FIXME: This seems fishy... SwingWorker executed inside SwingUtilities.invokeLater()?
                // NOTE: The invokeLater() wrapping was removed, keep an eye out for weirdness
                pm = new ProgressMonitor(dataWindow, "Exporting Project", "Copying files...", 0, 100);
                saveTask = new SaveTask();
                saveTask.addPropertyChangeListener(currentCommandExport);
                saveTask.execute();

                synchronized(saveTask)
                {
                    try
                    {
                        saveTask.wait();
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                dataWindow.setEnabled(true);
                dataWindow.requestFocus();
            }

            dataWindow.setEnabled(true);
            dataWindow.requestFocus();
        });
    }

    /**
     * Called when there's a progress update on the export process
     *
     * @param evt PropertyChangeEvent object representing the type of property change that occurred
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        //Update progress bar if progress update.
        if("progress".equals(evt.getPropertyName()))
        {
            pm.setProgress((Integer) evt.getNewValue());
        }
    }

    //Inner task class for actual data transfer, so that progress bar stays responsive.
    private class SaveTask extends SwingWorker<Void, Void>
    {
        //Total number of bytes to copy in this save task.
        private long totalFileSize = 0;
        //Number of bytes that have currently been copied.
        private long bytesCopied = 0;

        @Override
        public Void doInBackground()
        {
            saveProject();
            return null;
        }

        @Override
        public void done()
        {
            System.out.println("Save task has finished running");
            pm.close();
            pm = null;
            pathFile = null;
            saveTask = null;

            synchronized(this)
            {
                this.notifyAll();
            }
        }

        /**
         * Saves the loaded files to the directory pointed to in pathFile.
         */
        private void saveProject()
        {
            Vector<File> dataFiles = dmp.getDataFiles();
            Vector<File> videoFiles = dmp.getVideoFiles();
            Vector<InputMappingFile> inputMappingFiles = dmp.getInputMappingFiles();
            File configFile = dmp.getConfigFile();
            Vector<File> filesToCopy = new Vector<>();

            boolean fileCopiesSuccessful = true;
            int currentProgress = 0;
            bytesCopied = 0;

            //Move all files to the vector containing all files.
            filesToCopy.addAll(dataFiles);
            filesToCopy.addAll(videoFiles);
            filesToCopy.addAll(inputMappingFiles);

            //Build up total file size for progress bar.
            for(File file : filesToCopy)
            {
                totalFileSize += file.length();
            }

            //Increment total file size by 1 so that when we finish copying files, the task doesn't immediately stop.
            totalFileSize++;

            //Copy files
            for(File file : filesToCopy)
            {
                //If any files fail to copy, stop but allow message.
                if(!fileCopiesSuccessful)
                {
                    break;
                }

                //If the progress monitor is canceled, stop.
                if(pm.isCanceled())
                {
                    return;
                }

                fileCopiesSuccessful = copyFile(file, pathFile.getAbsolutePath() + File.separator + file.getName());
            }

            // Any open video players should be marked as saved
            dmp.setVideoPlayersSaved();

            if(fileCopiesSuccessful)
            {
                pm.setNote("Creating configuration file...");

                //Handle config file.
                if(configFile != null)
                {
                    //If file exists, write any changes and copy to new location.
                    dm.writeConfigFile();
                    copyFile(configFile, pathFile.getAbsolutePath() + File.separator + configFile.getName());

                    //Now that config file has been copied, change file handle to new path.
                    configFile = new File(pathFile.getAbsolutePath() + File.separator + "config.db");
                }
                else
                {
                    //If it doesnt, move to new location and then write.
                    //If the file still doesn't exist, it will be created upon being set.
                    configFile = new File(pathFile.getAbsolutePath() + File.separator + "config.db");
                    dm.setConfigFile(configFile);
                    dm.writeConfigFile();
                }

                //Progress length +1 so that we don't immediately finish upon processing the last file.
                int progressLength = filesToCopy.size() + 1;

                //Set progress bar back to beginning, +1 for config file.
                setProgress((++currentProgress * 100) / (progressLength));

                pm.setNote("Switching to new project directory...");

                //Need to reset data model, re-load all data from newly created project.
                dm.removeAllData();

                System.out.println("Old data cleared");

                for(File dataFile : dataFiles)
                {
                    if(dataFile.getName().endsWith(".c1"))
                    {
                        dm.setC1Data(new File(pathFile.getAbsolutePath() + File.separator + dataFile.getName()));
                    }
                    else if(dataFile.getName().endsWith(".dat"))
                    {
                        dm.set170Data(new File(pathFile.getAbsolutePath() + File.separator + dataFile.getName()));
                    }

                    setProgress((++currentProgress * 100) / (progressLength));
                }

                System.out.println("Data files reloaded");

                for(InputMappingFile inputMapFile : inputMappingFiles)
                {
                    dm.addInputMappingFile(pathFile.getAbsolutePath() + File.separator + inputMapFile.getName());
                    setProgress((++currentProgress * 100) / (progressLength));
                }

                System.out.println("Mapping files reloaded");

                for(File videoFile : videoFiles)
                {
                    dm.addVideoFile(new File(pathFile.getAbsoluteFile() + File.separator + videoFile.getName()), true);
                    setProgress((++currentProgress * 100) / (progressLength));
                }

                System.out.println("Video files reloaded");

                dm.organizeData();

                System.out.println("New data organized");

                //Reset/Reload config file.
                dm.setConfigFile(configFile);
                dm.readConfigFile();

                System.out.println("Config file loaded. Export complete");
            }
            else
            {
                JOptionPane.showMessageDialog(dataWindow, "Unable to export project to directory!", "Unable to export project!", JOptionPane.ERROR_MESSAGE);
            }
        }

        /**
         * Copies a file to the given path.
         *
         * @param original The file to copy.
         * @param copyPath The path to copy the file to.
         * @return boolean representing whether copying of file was successful or not
         */
        private boolean copyFile(File original, String copyPath)
        {
            boolean copySuccess = false;

            try
            {
                File copyFile = new File(copyPath);
                copyFile.createNewFile();

                FileInputStream fis = new FileInputStream(original);
                FileOutputStream fos = new FileOutputStream(copyFile);

                byte[] buf = new byte[COPY_BUF_SIZE];
                int readBytes;
                while((readBytes = fis.read(buf)) > 0)
                {
                    //Write bytes
                    fos.write(buf, 0, readBytes);

                    //Update bytes copied for progress
                    bytesCopied += readBytes;
                    long progress = bytesCopied * 100;
                    progress /= totalFileSize;

                    //Update task progress, but only if it wouldn't cause the task to complete.
                    if(progress != 100)
                    {
                        setProgress((int) progress);
                    }

                    //Ensure the user hasn't clicked cancel.
                    if(pm != null && pm.isCanceled())
                    {
                        System.out.println("Progress monitor canceled mid copy");
                        return false;
                    }
                }
                fis.close();
                fos.close();
                copySuccess = true;
            }
            catch(IOException ex)
            {
                System.out.println("Unable to copy file " + original.getName() + ": " + ex.toString());
                ex.printStackTrace();
            }

            return copySuccess;
        }
    }
}
