package VideoSync.views.modals.c1_viewer.commands;

import VideoSync.analyzers.C1DataCollector;
import VideoSync.models.DataModelProxy;
import VideoSync.views.modals.c1_viewer.C1Viewer;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class CommandExportDataToCSV extends AbstractAction
{
    private C1DataCollector cdc;
    private C1Viewer c1v;
    private DataModelProxy dmp;

    public CommandExportDataToCSV(String name)
    {
        super(name);
    }

    public void setTargets(C1DataCollector dataCollector, C1Viewer c1Viewer, DataModelProxy dataModelProxy)
    {
        cdc = dataCollector;
        c1v = c1Viewer;
        dmp = dataModelProxy;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        c1v.setEnabled(false);

        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma Separated Values File", "*.csv"));
            fileChooser.setTitle("Save CSV File");
            fileChooser.setInitialDirectory(new File(dmp.getCurrentDirectory()));
            fileChooser.setInitialFileName("untitled");

            File file = fileChooser.showSaveDialog(null);

            if(file != null)
            {
                try
                {
                    cdc.exportDataAsCSV(file);
                    EventQueue.invokeLater(() -> {
                        String filename = file.getName();
                        String message = filename + " saved successfully.";
                        JOptionPane.showMessageDialog(c1v, message);
                    });
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
            }

            c1v.setEnabled(true);
            c1v.requestFocus();
        });
    }
}
