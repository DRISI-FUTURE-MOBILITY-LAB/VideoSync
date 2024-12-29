package VideoSync.views.modals.convert_video;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;

import javax.swing.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ConvertVideo extends JFrame implements ActionListener {
    private double DEFAULT_FPS = 29.97;

    private JButton selectVideoButton;
    private JPanel convertVideoPanel;
    private JTextField FPSTextField;
    private JList videosToConvertList;
    private JList convertedVideosList;
    private JCheckBox mergeVideosCheckBox;
    private JButton startConversionButton;

    private DefaultListModel<String> inputVideoList;
    private DefaultListModel<String> outputVideoList;

    // Setup
    public ConvertVideo() {
        inputVideoList = new DefaultListModel<String>();
        outputVideoList = new DefaultListModel<String>();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                if (startConversionButton.isEnabled()) {
                    dispose();
                }
            }
        });

        setContentPane(convertVideoPanel);
        this.setSize(1000, 700);
        this.setResizable(true);
        FPSTextField.setText(String.valueOf(DEFAULT_FPS));
        selectVideoButton.addActionListener(this);
        startConversionButton.addActionListener(this);

        videosToConvertList.setModel(inputVideoList);
        convertedVideosList.setModel(outputVideoList);
    }

    // Makes whole panel visible
    public void displayPanel(boolean visible) {
        setVisible(true);
    }

    //Links buttons to functions
    @Override
    public void actionPerformed(ActionEvent e) {
        // Save changes and close window.
        if(e.getSource() == startConversionButton)
        {
            convertVideos();
        }
        else if(e.getSource() == selectVideoButton)
        {
            getInputVideos();
        }
    }

    //Open file selector menu
    public void getInputVideos() {
        //Run a JavaFX directory chooser so that the native OS file picker is used.
        inputVideoList.clear();
        outputVideoList.clear();
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Open Video Directory");

        Platform.runLater(() -> {
            File chosenDirectory = dc.showDialog(null);

            //Need to switch back to Swing thread
            //Use several invokeAndWait instead of invokeLater to ensure certain tasks are completed before others execute.
            SwingUtilities.invokeLater(() -> {

                if(chosenDirectory != null)
                {
                    // Get a list of all the files that were found within the specified directory.
                    File[] files = chosenDirectory.listFiles();

                    if(files != null)
                    {
                        // Loop through all of the files located in the directory.
                        for(File f : files)
                        {
                            String ext =
                                    NameParser.getExtension(f.getAbsolutePath());
                            if (ext.equals("dav") || ext.equals("mov")) {
                                inputVideoList.addElement(f.getAbsolutePath());
                            }
                        }
                    }
                }
            });
        });

    }

    //Start file conversion and call to ffmpeg
    public void convertVideos() {
        ArrayList<String> videos;
        ArrayList<String> intermediateVideos;
        ArrayList<String> mergeVideos;
        setEnableInputs(false);
        // CONVERT
        videos = Collections.list(inputVideoList.elements());
        outputVideoList.clear();
        ConvertVideosInBackground thread =
                new ConvertVideosInBackground(inputVideoList, outputVideoList
                        , getFps(), mergeVideosCheckBox.isSelected());
        thread.execute();

        thread.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public  void propertyChange(PropertyChangeEvent evt) {
                        if ("state".equals(evt.getPropertyName()) &&
                        thread.getState() == SwingWorker.StateValue.DONE) {
                            setEnableInputs(true);
                        }
                    }
                });
    }

    public void setEnableInputs(boolean b) {
        selectVideoButton.setEnabled(b);
        FPSTextField.setEditable(b);
        mergeVideosCheckBox.setEnabled(b);
        startConversionButton.setEnabled(b);
        if (b == true) {
            startConversionButton.setText("Start Conversion");
        } else {
            startConversionButton.setText("Converting...");
        }
    }

    private double getFps() {
        double fps;
        try {
            fps = Double.valueOf(this.FPSTextField.getText());
        } catch (NumberFormatException e) {
            fps = DEFAULT_FPS;
        }
        return fps;
    }
}

