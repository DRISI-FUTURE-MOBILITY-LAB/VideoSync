/*
 * ****************************************************************
 * File: 			OffsetFindingWindow.java
 * Date Created:    January 30, 2018
 * Programmer:		Elliot Hawkins
 *
 * Purpose:			Defines the UI for offset finder parameters.
 * Based on SettingsPane.java from VideoSync Detector.
 *
 * ****************************************************************
 */
package VideoSync.views.modals;

import VideoSync.models.DataModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class OffsetFindingWindow extends JFrame implements ActionListener, ChangeListener
{
    //Settings window label strings
    private static final String SETTING_LABEL_LIMIT_VIDEO_ENABLE = "Stop Search At Timestamp";
    private static final String SETTING_LABEL_LIMIT_VIDEO_HOUR = "  End Hour";
    private static final String SETTING_LABEL_LIMIT_VIDEO_MINUTE = "  End Minute";
    private static final String SETTING_LABEL_LIMIT_VIDEO_SECOND = "  End Second";
    private static final String SETTING_LABEL_SEARCH_MODE = "Offset Finder Search Mode";
    private static final String SETTING_LABEL_TIMESTAMP_VARIANCE = "Offset Finder Variance (ms)";
    private static final String[] SEARCH_MODE_STATES = {"Use All States", "High States Only", "Low States Only"};
    private static final int OFFSET_DEFAULT_VARIANCE = 350;
    private static final int SEARCH_MODE_DEFAULT = 1;
    public static final String OFFSET_DETECTOR_JAR = "VideoSyncDetector.jar";

    //UI Elements
    JPanel m_containerPanel;
    JLabel m_limitVideoTimeEnableLabel;
    JCheckBox m_limitVideoTimeCheckbox;
    JLabel m_limitVideoTimeHourLabel;
    JTextField m_limitVideoTimeHoursField;
    JLabel m_limitVideoTimeMinuteLabel;
    JTextField m_limitVideoTimeMinutesField;
    JLabel m_limitVideoTimeSecondLabel;
    JTextField m_limitVideoTimeSecondsField;

    JLabel m_searchModeLabel;
    JComboBox<String> m_searchModeCombo;

    JLabel m_timestampVarianceLabel;
    JTextField m_timestampVarianceField;

    JButton m_okButton;
    JButton m_cancelButton;

    JLabel m_videoScaleLabel;
    JLabel m_videoScalePercentageLabel;
    JSlider m_videoScaleSlider;

    //Data model reference
    private final DataModel dataModel;

    //Actual settings
    private final int searchMode = SEARCH_MODE_DEFAULT;
    private int offsetVariance = OFFSET_DEFAULT_VARIANCE;
    private boolean bHasTimeLimit = false;
    private int endHour = 0;
    private int endMinute = 0;
    private int endSecond = 0;
    private int scaleFactor = 100;

    /**
     * Advanced Pane constructor
     */
    public OffsetFindingWindow(DataModel dm)
    {
        super();

        dataModel = dm;

        initUI();

        refreshState();

    }

    /**
     * Advanced Pane UI init
     */
    private void initUI()
    {
        m_containerPanel = new JPanel(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        GridBagConstraints uiConstraints = new GridBagConstraints();
        int uiElementY = -1;

        //Set initial UI constraints
        uiConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        uiConstraints.gridx = 0;
        uiConstraints.gridy = uiElementY;
        uiConstraints.weightx = 1;
        uiConstraints.weighty = 1;
        uiConstraints.fill = GridBagConstraints.NONE;

        //Set up Search Mode
        m_searchModeLabel = new JLabel(SETTING_LABEL_SEARCH_MODE);
        uiConstraints.gridy = ++uiElementY;
        mainPanel.add(m_searchModeLabel, uiConstraints);

        m_searchModeCombo = new JComboBox<>();
        //Add items for each search mode
        for(String mode : SEARCH_MODE_STATES)
        {
            m_searchModeCombo.addItem(mode);
        }
        m_searchModeCombo.addActionListener(this);
        uiConstraints.gridx = GridBagConstraints.LINE_END;
        uiConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(m_searchModeCombo, uiConstraints);

        //Set up time variance
        m_timestampVarianceLabel = new JLabel(SETTING_LABEL_TIMESTAMP_VARIANCE);
        Font sublabelFont = m_timestampVarianceLabel.getFont();
        sublabelFont = new Font(sublabelFont.getName(), 0, sublabelFont.getSize());
        uiConstraints.gridx = 0;
        uiConstraints.gridy = ++uiElementY;
        uiConstraints.fill = GridBagConstraints.NONE;
        mainPanel.add(m_timestampVarianceLabel, uiConstraints);

        m_timestampVarianceField = new JTextField(Integer.toString(OFFSET_DEFAULT_VARIANCE));
        m_timestampVarianceField.addActionListener(this);
        uiConstraints.gridx = GridBagConstraints.LINE_END;
        uiConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(m_timestampVarianceField, uiConstraints);

        //Set up Time Limit setting
        m_limitVideoTimeEnableLabel = new JLabel(SETTING_LABEL_LIMIT_VIDEO_ENABLE);
        uiConstraints.gridx = 0;
        uiConstraints.gridy = ++uiElementY;
        uiConstraints.fill = GridBagConstraints.NONE;
        mainPanel.add(m_limitVideoTimeEnableLabel, uiConstraints);

        m_limitVideoTimeCheckbox = new JCheckBox();
        m_limitVideoTimeCheckbox.addActionListener(this);
        uiConstraints.gridx = GridBagConstraints.LINE_END;
        mainPanel.add(m_limitVideoTimeCheckbox, uiConstraints);

        m_limitVideoTimeHourLabel = new JLabel(SETTING_LABEL_LIMIT_VIDEO_HOUR);
        m_limitVideoTimeHourLabel.setFont(sublabelFont);
        uiConstraints.gridx = 0;
        uiConstraints.gridy = ++uiElementY;
        mainPanel.add(m_limitVideoTimeHourLabel, uiConstraints);

        m_limitVideoTimeHoursField = new JTextField();
        m_limitVideoTimeHoursField.setText("0");
        m_limitVideoTimeHoursField.addActionListener(this);
        uiConstraints.gridx = GridBagConstraints.LINE_END;
        uiConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(m_limitVideoTimeHoursField, uiConstraints);

        m_limitVideoTimeMinuteLabel = new JLabel(SETTING_LABEL_LIMIT_VIDEO_MINUTE);
        m_limitVideoTimeMinuteLabel.setFont(sublabelFont);
        uiConstraints.gridx = 0;
        uiConstraints.gridy = ++uiElementY;
        uiConstraints.fill = GridBagConstraints.NONE;
        mainPanel.add(m_limitVideoTimeMinuteLabel, uiConstraints);

        m_limitVideoTimeMinutesField = new JTextField();
        m_limitVideoTimeMinutesField.setText("0");
        m_limitVideoTimeMinutesField.addActionListener(this);
        uiConstraints.gridx = GridBagConstraints.LINE_END;
        uiConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(m_limitVideoTimeMinutesField, uiConstraints);

        m_limitVideoTimeSecondLabel = new JLabel(SETTING_LABEL_LIMIT_VIDEO_SECOND);
        m_limitVideoTimeSecondLabel.setFont(sublabelFont);
        uiConstraints.gridx = 0;
        uiConstraints.gridy = ++uiElementY;
        uiConstraints.fill = GridBagConstraints.NONE;
        mainPanel.add(m_limitVideoTimeSecondLabel, uiConstraints);

        m_limitVideoTimeSecondsField = new JTextField();
        m_limitVideoTimeSecondsField.setText("0");
        m_limitVideoTimeSecondsField.addActionListener(this);
        uiConstraints.gridx = GridBagConstraints.LINE_END;
        uiConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(m_limitVideoTimeSecondsField, uiConstraints);

        m_videoScaleLabel = new JLabel("Processing Resolution");
        //m_videoScaleLabel.setFont(sublabelFont);
        uiConstraints.gridx = 0;
        uiConstraints.gridy = ++uiElementY;
        uiConstraints.fill = GridBagConstraints.NONE;
        mainPanel.add(m_videoScaleLabel, uiConstraints);

        //Sub-panel for resolution scale slider and label
        JPanel resolutionSliderPanel = new JPanel();
        {
            m_videoScaleSlider = new JSlider(25, 100, 100);
            Dimension sliderSize = new Dimension(100, (int) m_videoScaleSlider.getPreferredSize().getHeight());
            m_videoScaleSlider.setPreferredSize(sliderSize);
            m_videoScaleSlider.setMaximumSize(sliderSize);
            m_videoScaleSlider.addChangeListener(this);
            resolutionSliderPanel.add(m_videoScaleSlider);

            m_videoScalePercentageLabel = new JLabel("100%");
            m_videoScalePercentageLabel.setFont(sublabelFont);
            resolutionSliderPanel.add(m_videoScalePercentageLabel);
        }
        uiConstraints.gridx = GridBagConstraints.LINE_END;
        mainPanel.add(resolutionSliderPanel, uiConstraints);

        m_containerPanel.add(mainPanel, BorderLayout.CENTER);

        //Set up OK and Cancel buttons
        m_okButton = new JButton("OK");
        m_okButton.addActionListener(this);
        bottomPanel.add(m_okButton);

        m_cancelButton = new JButton("Cancel");
        m_cancelButton.addActionListener(this);
        bottomPanel.add(m_cancelButton);

        m_containerPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(m_containerPanel);

        pack();
    }

    /**
     * Updates UI Components with values from the data model
     */
    private void refreshState()
    {
        m_limitVideoTimeCheckbox.setSelected(bHasTimeLimit);
        m_limitVideoTimeHoursField.setText(Integer.toString(endHour));
        m_limitVideoTimeMinutesField.setText(Integer.toString(endMinute));
        m_limitVideoTimeSecondsField.setText(Integer.toString(endSecond));
        m_timestampVarianceField.setText(Integer.toString(offsetVariance));

        m_searchModeCombo.setSelectedIndex(searchMode);

        m_videoScalePercentageLabel.setText(scaleFactor < 100 ? scaleFactor + "%" : "Max");
        m_videoScaleSlider.setValue(scaleFactor);

        m_limitVideoTimeHoursField.setEnabled(bHasTimeLimit);
        m_limitVideoTimeMinutesField.setEnabled(bHasTimeLimit);
        m_limitVideoTimeSecondsField.setEnabled(bHasTimeLimit);
    }

    /**
     * Updates the data model with current UI values
     */
    private void commitState()
    {
        endHour = Integer.parseInt(m_limitVideoTimeHoursField.getText());
        endMinute = Integer.parseInt(m_limitVideoTimeMinutesField.getText());
        endSecond = Integer.parseInt(m_limitVideoTimeSecondsField.getText());
        offsetVariance = Integer.parseInt(m_timestampVarianceField.getText());
        bHasTimeLimit = m_limitVideoTimeCheckbox.isSelected();
        scaleFactor = m_videoScaleSlider.getValue();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        commonEventHandler(e.getSource());
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        commonEventHandler(e.getSource());
    }

    private void commonEventHandler(Object source)
    {
        if(source == m_limitVideoTimeCheckbox)
        {
            bHasTimeLimit = m_limitVideoTimeCheckbox.isSelected();
            refreshState();

        }
        else if(source == m_okButton)
        {
            if(verifyFields())
            {
                commitState();
                setVisible(false);
                startOffsetFinding();
            }
            else
            {
                JOptionPane.showMessageDialog(this, "One or more of your settings is not valid. Please only enter positive numeric values into text fields", "Invalid Settings", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(source == m_cancelButton)
        {
            setVisible(false);
            refreshState();
        }
        else if(source == m_videoScaleSlider)
        {
            int scale = m_videoScaleSlider.getValue();
            m_videoScalePercentageLabel.setText(scale < 100 ? scale + "%" : "Max");
        }
    }

    /**
     * Ensures that values entered into the UI fields are valid values
     * @return Whether or not each field has a valid entry
     */
    private boolean verifyFields()
    {
        boolean toReturn = true;

        try
        {
            int videoLimitHour = Integer.parseInt(m_limitVideoTimeHoursField.getText());
            int videoLimitMinute = Integer.parseInt(m_limitVideoTimeMinutesField.getText());
            int videoLimitSecond = Integer.parseInt(m_limitVideoTimeSecondsField.getText());
            int timestampVariance = Integer.parseInt(m_timestampVarianceField.getText());

            if(videoLimitHour < 0 || videoLimitMinute < 0 || videoLimitSecond < 0 || timestampVariance < 0)
            {
                toReturn = false;
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            toReturn = false;
        }

        return toReturn;
    }

    /**
     * Starts VideoSync Detector with the given settings
     */
    public void startOffsetFinding()
    {
        dataModel.writeConfigFile();

        String[] options = {"Yes", "No"};
        int result = JOptionPane.showOptionDialog(this, "Start offset detection for channels with video overlays set?", "Start Offset Detection?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, JOptionPane.YES_OPTION);
        if(result == JOptionPane.YES_OPTION)
        {
            StringBuilder detectorCommand = new StringBuilder();
            detectorCommand.append("java -jar " + OFFSET_DETECTOR_JAR + " ");

            // This line is for debugging the compiled VideoSync Detector jar file
            //detectorCommand.append("java -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 " + OFFSET_DETECTOR_JAR + " ");

            detectorCommand.append("-o ");
            detectorCommand.append("-a ");
            detectorCommand.append("-d ").append(dataModel.getCurrentDirectory().replaceAll(" ", "%20")).append(" ");
            detectorCommand.append("-rs ").append((double) scaleFactor / 100.0).append(" ");
            if(bHasTimeLimit)
            {
                detectorCommand.append("-t ").append(endHour).append(" ").append(endMinute).append(" ").append(endSecond).append(" ");
            }

            detectorCommand.append("-s " + searchMode + " ");
            try
            {
                System.out.println("Running " + detectorCommand);
                Process detectorProcess = Runtime.getRuntime().exec(detectorCommand.toString());

                //Read through results of the check PID process on a line by line basis.
                InputStream is = detectorProcess.getInputStream();
                InputStream es = detectorProcess.getErrorStream();
                BufferedReader resultReader = new BufferedReader(new InputStreamReader(is));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(es));
                String resultLine;
                while((resultLine = resultReader.readLine()) != null)
                {
                    System.out.println("Detector Output: " + resultLine);
                }
                while((resultLine = errorReader.readLine()) != null)
                {
                    System.out.println("Detector Error Output: " + resultLine);
                }

                System.out.println("Finished running");
            }
            catch(IOException ex)
            {
                System.out.println("Something went wrong running command " + detectorCommand);
                ex.printStackTrace();
            }

            //Reload config file to get the new offset
            dataModel.readConfigFile();
        }
    }
}
