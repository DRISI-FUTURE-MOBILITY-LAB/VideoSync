/*
 * File: CommandAddEvent.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class used to bind the UI elements of Event Logger to
 *          associate Data Model methods to add events
 */

package VideoSync.views.modals.event_logger.commands;

import VideoSync.models.DataModel;
import VideoSync.objects.DeviceInputMap;
import VideoSync.views.modals.event_logger.event_window.EventWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.StringTokenizer;

public class CommandAddEvent extends AbstractAction
{

    /**
     * Data model reference
     */
    private final DataModel dataModel;

    /**
     * Event window associated to command
     */
    private EventWindow eventWindow;

    public CommandAddEvent(DataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    public void setTargets(EventWindow eventWindow)
    {
        this.eventWindow = eventWindow;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        int chip = -1;
        int pin = -1;

        /*
         * Find the chip and pin number for the channel to associate the event to
         */
        List<DeviceInputMap> deviceInputMapList = dataModel.getC1InputMap();
        for(DeviceInputMap deviceInputMap : deviceInputMapList)
        {
            // FIXME: toString() may produce NullPointerException
            if(deviceInputMap.getChannelName().equals(eventWindow.getChannels().getSelectedItem().toString()))
            {
                chip = deviceInputMap.getChipNumber();
                pin = deviceInputMap.getPinNumber();
                break;
            }
        }

        /*
         * Add the event to the channel count, if it exists
         */
        // FIXME: equals() may produce NullPointerException
        if(chip != -1 && pin != -1 && !eventWindow.getEventTags().getSelectedItem().equals("Event Tags"))
        {
            int offset = (int) (Double.parseDouble(eventWindow.getOffset().getText()) * 1000);

            dataModel.addEvent(chip, pin, convertToMilliseconds(eventWindow.getTimestamp().getText()), offset, eventWindow.getEventTags().getSelectedItem().toString(), eventWindow.getComment().getText(), eventWindow.getOmit().isSelected());
            eventWindow.dispose();
        }
        else
        {
            if(chip == -1 && pin == -1)
            {
                // FIXME: toString() may produce NullPointerException
                System.out.printf("Selected channel item '%s' not found in input map due to invalid chip and pin combination (Chip: %d\tPin: %d)\n", eventWindow.getChannels().getSelectedItem().toString(), chip, pin);
            }
            // FIXME: equals() may produce NullPointerException
            if(eventWindow.getEventTags().getSelectedItem().equals("Event Tags"))
            {
                System.out.printf("Selected invalid event tag item: %s", eventWindow.getEventTags().getSelectedItem());
            }
            JOptionPane.showMessageDialog(eventWindow, "Selected channel does not exist in the input map or selected event tag is invalid. Select another channel or tag.");
        }
    }

    /**
     * Used to convert timestamp displayed in event window to milliseconds
     *
     * @param timestamp timestamp label
     * @return millisecond value of timestamp label
     */
    private int convertToMilliseconds(String timestamp)
    {
        int result = 0;
        StringTokenizer tokenizer = new StringTokenizer(timestamp, ":|.");

        if(tokenizer.countTokens() == 4)
        {
            result += Integer.parseInt(tokenizer.nextToken()) * 36000000;
        }

        if(tokenizer.countTokens() == 3)
        {
            result += Integer.parseInt(tokenizer.nextToken()) * 60000;
        }

        if(tokenizer.countTokens() == 2)
        {
            result += Integer.parseInt(tokenizer.nextToken()) * 1000;
        }

        if(tokenizer.countTokens() == 1)
        {
            result += Integer.parseInt(tokenizer.nextToken());
        }

        System.out.println(result);

        return result;
    }
}
