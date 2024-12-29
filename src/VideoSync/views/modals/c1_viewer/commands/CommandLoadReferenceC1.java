package VideoSync.views.modals.c1_viewer.commands;

import VideoSync.analyzers.C1Analyzer;
import VideoSync.models.DataModelProxy;
import VideoSync.views.modals.c1_viewer.C1Viewer;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class CommandLoadReferenceC1 extends AbstractAction
{
    private C1Analyzer c1Analyzer;
    private C1Viewer c1ViewerWindow;
    private DataModelProxy dmp;

    /**
     * Creates action object that will load reference C1 data and gives it a name
     * @param name name given to action object
     */
    public CommandLoadReferenceC1(String name)
    {
        super(name);
    }

    /**
     * Sets target objects this command will operate on
     * @param c1Analyzer C1 analyzer
     * @param c1ViewerWindow window showing C1 data
     */
    public void setTargets(C1Analyzer c1Analyzer, C1Viewer c1ViewerWindow, DataModelProxy dmp)
    {
        this.c1Analyzer = c1Analyzer;
        this.c1ViewerWindow = c1ViewerWindow;
        this.dmp = dmp;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(dmp.getCurrentDirectory()));
        fc.setTitle("Open Reference C1 File");

        c1ViewerWindow.setEnabled(false);

        Platform.runLater(() -> {
            File chosenC1File = fc.showOpenDialog(null);

            //Need to switch back to Swing thread
            //Use several invokeAndWait instead of invokeLater to ensure certain tasks are completed before others execute.
            SwingUtilities.invokeLater(() -> {
                if(chosenC1File != null)
                {
                    if(chosenC1File.getName().endsWith(".c1"))
                    {
                        if(!c1ViewerWindow.isReferenceC1Loaded())
                            c1ViewerWindow.setReferenceC1Loaded(true);
                        else
                            c1Analyzer = c1ViewerWindow.resetAnalyzer();

                        c1ViewerWindow.setC1FileName(chosenC1File.getName());

                        c1Analyzer.performAnalysis(chosenC1File);
                        c1ViewerWindow.setGeneratedEventChannels(c1Analyzer.getC1Channels());

                        c1ViewerWindow.updateGraph();
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(c1ViewerWindow, "File must be a .c1 file");
                    }
                }

                c1ViewerWindow.setEnabled(true);
                c1ViewerWindow.requestFocus();
            });
        });
    }
}
