/*
 * ****************************************************************
 * File: 			CommandImport.java
 * Date Created:  	June 12, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To handle an action request from the menu to
 * 					import an additional file that may not have
 * 					been detected automatically.
 *
 * Modified:		August 24, 2016
 * Programmer:		Danny Hale
 * 					Added the ability to import vbm and mpf mapping
 * 					files. Expanded video formats accepted.
 * ****************************************************************
 */

package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.objects.ImportFilter;
import VideoSync.views.tabbed_panels.DataWindow;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class CommandImport extends AbstractAction
{
    private static final long serialVersionUID = 1L;

    /**
     * Used for notifying the data model what action is to be performed.
     */
    private DataModel dm;

    /**
     * Reference to DataWindow for positioning InputMapping Window
     */
    private DataWindow dw;

    /**
     * Sets the references to the DataModel and DataWindow
     *
     * @param dm Reference to DataModel
     */
    public void setTargets(DataModel dm, DataWindow dw)
    {
        // Set the data model from the passed parameter
        this.dm = dm;
        this.dw = dw;
    }

    /**
     * Called when the user selects the "Import" option from the File menu.
     */
    public void actionPerformed(ActionEvent ae)
    {
        //FileChooser needs to be ran on JavaFX thread
        Platform.runLater(() -> {
            FileChooser fc = new FileChooser();

            dw.setEnabled(false);

            //Set file filters for file select dialog.
            ImportFilter.Utils.setAllExtensionFilters(fc);

            //Set title string for file chooser.
            fc.setTitle("Select a file to import");

            File file = fc.showOpenDialog(null);
            if(file != null)
            {
                //Switch back to swing thread to work with file
                SwingUtilities.invokeLater(() -> {
                    // Variable to store the files extension
                    String extension = "";

                    // Retrieve the last index of a '.' so that we can get the extension
                    int extensionStart = file.getName().lastIndexOf('.');

                    // As long as the location of the period is not 0, we assume it is the correct one for finding the extension
                    if(extensionStart > 0)
                    {
                        // Set the extension based on the file name
                        extension = file.getName().substring(extensionStart + 1);
                    }

                    // If our extension is a c1 file, then we tell the data model to load the c1 file.
                    if(extension.equalsIgnoreCase("c1"))
                    {
                        dm.setC1Data(file);
                    }
                    // If our extension is a c1 file, then we tell the data model to load the c1 file.
                    else if(extension.equalsIgnoreCase("c1max"))
                    {
                        dm.setC1MaximData(file);
                    }
                    // If our extension is a 170 file, then we tell the data model to load the 170 file.
                    else if(extension.equalsIgnoreCase("vsc") || extension.equalsIgnoreCase("log") || extension.equalsIgnoreCase("dat"))
                    {
                        dm.set170Data(file);
                    }
                    // If our extension is a video file extension, then we tell the data model to load the video file and set the isSaved flag to false.
                    else if(ImportFilter.Utils.isExtensionVideo(extension))
                    {
                        dm.addVideoFile(file, false);
                    }
                    // If our extension is a vbm file, then the model gets the input mapping information
                    else if(extension.equalsIgnoreCase("vbm"))
                    {
                        dm.setVBMFile(file);
                    }
                    // If our extension is a mpf file, then the model gets the input mapping information
                    else if(extension.equalsIgnoreCase("mpf"))
                    {
                        dm.setMappingFile(file);
                    }
                    // If our file name is "config", then read the config file
                    else if(file.getName().equals("config.db"))
                    {
                        dm.readConfigFile();
                    }

                    dw.setEnabled(true);
                    dw.requestFocus();
                });
            }
            else
            {
                dw.setEnabled(true);
                dw.requestFocus();
            }
        });
    }
}
