package VideoSync.views.modals.event_logger.commands;

import VideoSync.analyzers.C1Analyzer;
import VideoSync.models.DataModel;
import VideoSync.models.DataModelProxy;
import VideoSync.views.modals.event_logger.autoanalysis.AutoAnalysis;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class CommandLoadGeneratedC1 extends AbstractAction
{
    private C1Analyzer c1Analyzer;
    private AutoAnalysis autoAnalysisWindow;
    private DataModel dm;

    /**
     * Creates action object that will load reference C1 data and gives it a name
     * @param name name given to action object
     * @param autoAnalysisWindow
     */
    public CommandLoadGeneratedC1(String name, C1Analyzer c1Analyzer, AutoAnalysis autoAnalysisWindow, DataModel dm)
    {
        super(name);

        this.c1Analyzer = c1Analyzer;
        this.autoAnalysisWindow = autoAnalysisWindow;
        this.dm = dm;
    }

    /**
     * Sets target objects this command will operate on
     * @param c1Analyzer C1 analyzer
     * @param autoAnalysisWindow window showing C1 data
     */
    public void setTargets(C1Analyzer c1Analyzer, AutoAnalysis autoAnalysisWindow, DataModel dm)
    {
        this.c1Analyzer = c1Analyzer;
        this.autoAnalysisWindow = autoAnalysisWindow;
        this.dm = dm;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(dm.getCurrentDirectory()));
        fc.setTitle("Open Reference C1 File");

        autoAnalysisWindow.setEnabled(false);

        Platform.runLater(() -> {
            File chosenC1File = fc.showOpenDialog(null);

            //Need to switch back to Swing thread
            //Use several invokeAndWait instead of invokeLater to ensure certain tasks are completed before others execute.
            SwingUtilities.invokeLater(() -> {
                if(chosenC1File != null)
                {
                    if(chosenC1File.getName().endsWith(".c1"))
                    {
                        if(!autoAnalysisWindow.isGeneratedC1Loaded())
                            autoAnalysisWindow.setGeneratedC1Loaded(true);
                        else
                            c1Analyzer = autoAnalysisWindow.resetAnalyzer();

                        autoAnalysisWindow.setC1FileName(chosenC1File.getName());

                        c1Analyzer.performAnalysis(chosenC1File);
                        autoAnalysisWindow.setGeneratedEventChannels(c1Analyzer.getC1Channels());

                        autoAnalysisWindow.pack();
                        autoAnalysisWindow.revalidate();
                        autoAnalysisWindow.repaint();
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(autoAnalysisWindow, "File must be a .c1 file");
                    }
                }

                autoAnalysisWindow.setEnabled(true);
                autoAnalysisWindow.requestFocus();
            });
        });
    }
}