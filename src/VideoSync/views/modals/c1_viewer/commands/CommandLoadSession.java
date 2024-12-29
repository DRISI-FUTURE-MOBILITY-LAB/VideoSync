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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class CommandLoadSession extends AbstractAction
{
    private C1Viewer cv;
    private DataModelProxy dmp;

    public CommandLoadSession(String name)
    {
        super(name);
    }

    public void setTargets(C1Viewer c1Viewer, DataModelProxy dataModelProxy)
    {
        cv = c1Viewer;
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
            fileChooser.setTitle("Load C1 Data Collector File");

            File file = fileChooser.showOpenDialog(null);

            if(file != null)
            {
                C1DataCollector cdc;
                try
                {
                    FileInputStream fileIn = new FileInputStream(file);
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    cdc = (C1DataCollector) in.readObject();
                    in.close();
                    fileIn.close();

                    cv.loadC1DataCollector(cdc);

                    EventQueue.invokeLater(() -> {
                        String message = "C1 data collector file loaded successfully.";
                        JOptionPane.showMessageDialog(null, message);
                    });
                }
                catch(IOException | ClassNotFoundException ex)
                {
                    ex.printStackTrace();
                }
            }
        });
    }
}
