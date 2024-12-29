/*
 * File: CommandUpdateChannelCount.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to allow user to save a
 *          event log file.
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CommandUpdateChannelCount implements KeyListener
{

    /**
     * Index constant for channel index in 2D array
     */
    private final int CHANNEL_INDEX = 0;

    /**
     * Index constant for enabled index in 2D array
     */
    private final int ENABLED_INDEX = 1;

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Channels table UI element reference
     */
    private JTable channels;

    /**
     * Channel label UI element reference
     */
    private JLabel channel;

    /**
     * Channel count label UI element reference
     */
    private JLabel channelCount;

    /**
     * Selected channel index
     */
    private int selectedIndex;

    /**
     * Increment/Decrement channel count label UI element reference
     */
    private JLabel mode;

    public CommandUpdateChannelCount(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTargets(JTable channels, JLabel channel, JLabel channelCount, JLabel mode)
    {
        this.channels = channels;
        this.channel = channel;
        this.channelCount = channelCount;
        this.mode = mode;

        InputMap inputMap = channels.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        KeyStroke stroke = KeyStroke.getKeyStroke("ENTER");
        inputMap.put(stroke, "none");

        this.channels.addKeyListener(this);
        this.channels.getSelectionModel().addListSelectionListener(e -> {
            selectedIndex = channels.getSelectedRow();
            System.out.println("Selected Index: " + selectedIndex);
            //System.out.println("Selected channel table index: " + selectedIndex);
            if(selectedIndex > -1)
                updateSelectedChannelPane();
        });
    }

    /**
     * Get the channel name from the C1 input map for the selected channel index
     *
     * @return channel name
     */
    public String getSelectedChannelCountName()
    {
        return dataModel.getC1InputMap().get(selectedIndex).getChannelName();
    }

    /**
     * Get the car count for the selected channel car count
     *
     * @return car count for the selected channel
     */
    public String getSelectedChannelCarCount()
    {
        return Integer.toString(dataModel.getChannelCountByInputMapIndex(selectedIndex).getCarCount());
    }

    /**
     * Updates the selected channel pane with the selected channel car count and name
     */
    private void updateSelectedChannelPane()
    {
        System.out.println(getSelectedChannelCountName() + "\t\t" + getSelectedChannelCarCount());
        channel.setText(getSelectedChannelCountName());
        channelCount.setText(getSelectedChannelCarCount());
    }

    /**
     * Returns an int value for the pressed number pad
     *
     * @param keyCode key code associated to the KeyEvent value
     * @return integer value of the number pad key pressed
     */
    private int getHotkeyMapKeyFromKeyCode(int keyCode)
    {
        if(keyCode == KeyEvent.VK_0 || keyCode == KeyEvent.VK_NUMPAD0)
        {
            return 0;
        }
        else if(keyCode == KeyEvent.VK_1 || keyCode == KeyEvent.VK_NUMPAD1)
        {
            return 1;
        }
        else if(keyCode == KeyEvent.VK_2 || keyCode == KeyEvent.VK_NUMPAD2)
        {
            return 2;
        }
        else if(keyCode == KeyEvent.VK_3 || keyCode == KeyEvent.VK_NUMPAD3)
        {
            return 3;
        }
        else if(keyCode == KeyEvent.VK_4 || keyCode == KeyEvent.VK_NUMPAD4)
        {
            return 4;
        }
        else if(keyCode == KeyEvent.VK_5 || keyCode == KeyEvent.VK_NUMPAD5)
        {
            return 5;
        }
        else if(keyCode == KeyEvent.VK_6 || keyCode == KeyEvent.VK_NUMPAD6)
        {
            return 6;
        }
        else if(keyCode == KeyEvent.VK_7 || keyCode == KeyEvent.VK_NUMPAD7)
        {
            return 7;
        }
        else if(keyCode == KeyEvent.VK_8 || keyCode == KeyEvent.VK_NUMPAD8)
        {
            return 8;
        }
        else if(keyCode == KeyEvent.VK_9 || keyCode == KeyEvent.VK_NUMPAD9)
        {
            return 9;
        }
        else
            return -1;
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if(e.getSource() == channels)
        {
            /*
             * Using the directional keyboard keys to select a channel from the channels table
             */
            if(e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)
            {
                selectedIndex = channels.getSelectedRow();
                updateSelectedChannelPane();
                //System.out.println("Selected channel table index: " + selectedIndex);
            }

            /*
             * Using keyboard 'I' or 'W' keys to increment a channel car count
             */
            else if(e.getKeyCode() == KeyEvent.VK_I || e.getKeyCode() == KeyEvent.VK_W)
            {
                dataModel.updateChannelCarCount(1, selectedIndex);
                updateSelectedChannelPane();
                channels.setRowSelectionInterval(selectedIndex, selectedIndex);
            }

            /*
             * Using keyboard 'D' or 'S' keys to decrement a channel car count
             */
            else if(e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_S)
            {
                dataModel.updateChannelCarCount(0, selectedIndex);
                updateSelectedChannelPane();
                channels.setRowSelectionInterval(selectedIndex, selectedIndex);
            }

            /*
             * Using the keyboard number pad to select a channel car count associated to a hotkey
             */
            else if(dataModel.getHotkeyMaps() != null && !dataModel.getHotkeyMaps().isEmpty())
            {
                if(e.getKeyCode() >= KeyEvent.VK_0 && e.getKeyCode() <= KeyEvent.VK_9 || e.getKeyCode() >= KeyEvent.VK_NUMPAD0 && e.getKeyCode() <= KeyEvent.VK_NUMPAD9)
                {

                    int mapKey = getHotkeyMapKeyFromKeyCode(e.getKeyCode());

                    if(mapKey != -1 &&
                            (int) dataModel.getHotkeyMaps().get(mapKey).get(CHANNEL_INDEX) > 0 &&
                            (boolean) dataModel.getHotkeyMaps().get(mapKey).get(ENABLED_INDEX))
                    {
                        int idx = (int) dataModel.getHotkeyMaps().get(mapKey).get(CHANNEL_INDEX) - 1;
                        channels.setRowSelectionInterval(idx, idx);

                        if(dataModel.getHotkeyUpdateMode())
                        {
                            dataModel.updateChannelCarCount(mode.getText().equals("Increment") ? 1 : 0, idx);
                            channels.setRowSelectionInterval(idx, idx);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if(e.getSource() == channels)
        {
            if(e.getKeyCode() == KeyEvent.VK_ENTER)
            {
                if(mode.getText().equals("Decrement"))
                {
                    mode.setText("Increment");
                    System.out.println("Increment Mode");
                }
                else
                {
                    mode.setText("Decrement");
                    System.out.println("Decrement Mode");
                }
            }
        }
    }
}
