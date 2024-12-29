package VideoSync.views.modals.c1_viewer.commands;

import VideoSync.analyzers.C1DataCollector;
import VideoSync.models.DataModelProxy;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class CommandSaveSession extends AbstractAction
{
    private C1DataCollector cdc;
    private DataModelProxy dmp;

    public CommandSaveSession(String name)
    {
        super(name);
    }

    public void setTargets(C1DataCollector dataCollector, DataModelProxy dataModelProxy)
    {
        cdc = dataCollector;
        dmp = dataModelProxy;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Platform.runLater(() -> {
            // Open OS system file directory
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(dmp.getCurrentDirectory()));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("C1 Data Collector File", "*.cdc"));
            fileChooser.setTitle("Save C1 Data Collector File");
            fileChooser.setInitialFileName("untitled.cdc");

            File file = fileChooser.showSaveDialog(null);

            if(file != null)
            {
                try
                {
                    FileOutputStream fileOut = new FileOutputStream(file);
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);
                    out.writeObject(cdc);
                    out.close();
                    fileOut.close();

                    EventQueue.invokeLater(() -> {
                        String message = "C1 data collector file saved successfully.";
                        JOptionPane.showMessageDialog(null, message);
                    });
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        });
    }
}
