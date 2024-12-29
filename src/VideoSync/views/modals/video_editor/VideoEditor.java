package VideoSync.views.modals.video_editor;

import VideoSync.views.modals.convert_video.TrimVideoInBackground;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

public class VideoEditor extends JFrame implements ActionListener {
    private JPanel container;
    private JLabel inputVideo;
    private JTextField start;
    private JTextField end;
    private JButton selectVideo;
    private JButton startTrim;
    private JLabel outputVideo;

    public VideoEditor() {
        setContentPane(container);
        setSize(500, 300);
        setResizable(true);
        selectVideo.addActionListener(this);
        startTrim.addActionListener(this);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                if (startTrim.isEnabled()) {
                    dispose();
                }
            }
        });
    }

    // Makes whole panel visible
    public void displayPanel(boolean visible) {
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == startTrim) {
            startTrim(inputVideo.getText(), start.getText(), end.getText());
        } else if(e.getSource() == selectVideo) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "MP4 videos", "mp4");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                inputVideo.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private void startTrim(String inputVideo, String start, String end) {
        if (new File(inputVideo).exists() &&
            checkTimeFormat(start) &&
            checkTimeFormat(end))
        {
            setEnableInputs(false);
            TrimVideoInBackground thread =
                    new TrimVideoInBackground(inputVideo, start, end,
                            outputVideo);
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

    }

    // Checks that timeStr is in the format 00:00:00
    // From http://www.ffmpeg.org/ffmpeg-utils.html#time-duration-syntax
    private boolean checkTimeFormat(String timeStr) {
        String[] list = timeStr.split(":");
        if (list.length == 3) {
            for (String t : list) {
                if (t.length() != 2 || !t.matches("[0-9]+")) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void setEnableInputs(boolean b) {
        startTrim.setEnabled(b);
        selectVideo.setEnabled(b);
        end.setEditable(b);
        start.setEditable(b);
        if (b == true) {
            startTrim.setText("Start Trim");
        } else {
            startTrim.setText("Trimming...");
        }
    }
}
