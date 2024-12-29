/*
 * ****************************************************************
 * File: 			DeviceInputMap.java
 * Date Created:  	July 23, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To keep track of the individual channel
 * elements for the user to customize the channel
 * attributes.
 *
 * ****************************************************************
 */

package VideoSync.objects;

import java.io.Serializable;

public class DeviceInputMap implements Serializable
{
    private static final long serialVersionUID = 7654853157437949476L;

    /**
     * Used for referencing the maxim chip number that is associated with the channel
     */
    private int chipNumber;

    /**
     * Used for referencing the maxim pin number associated with the chip number
     */
    private int pinNumber;

    // TODO: Figure out how to change the name of this field while still keeping the input mapping files compatible
    /**
     * Used for referencing the original channel number that is used in all of the data structures.
     */
    private final int bitNumber;

    /**
     * Used for indicating which lane number it is.
     */
    private int laneNumber;

    /**
     * Used for storing a custom file name.
     */
    private String channelName;

    /**
     * Indicates if the channel is a intersection, freeway, or ramp
     */
    private String channelType;

    /**
     * Indicates which general direction the lane is heading.
     * -- Valid Inputs:
     * -- N: Northbound
     * -- S: Southbound
     * -- E: Eastbound
     * -- W: Westbound
     */
    private String direction;

    /**
     * Indicates what type of detector is being used
     *
     * -- Valid Inputs:
     * -- Radar
     * -- Loop
     * -- Video
     */
    private String detectorType;

    /**
     * Indicates the type of configuration is being used for detector
     *
     * -- Valid Inputs:
     * -- Push Bar
     * -- Presence
     * -- Pulse
     * -- Stop Bar
     */
    private String detectorConfig;

    // -- DeviceInputMap Constructor

    /**
     * Constructs a DeviceInputMap Object with only a channelNumber number
     *
     * @param channelNumber channel number associated with channel
     */
    public DeviceInputMap(int channelNumber)
    {
        // Calls the DeviceInputMap constructor with 2 parameters.
        this(-1, -1, channelNumber, null);
    }

    /**
     * Constructs a DeviceInputMap Object with a chip number and a pin number.
     * @param chipNumber maxim chip number that is associated with the channel
     * @param pinNumber  pin number associated with the the channel
     * @param channelNumber        channel number associated with channel
     */
    public DeviceInputMap(int chipNumber, int pinNumber, int channelNumber)
    {
        // Calls the DeviceInputMap constructor with 2 parameters.
        this(chipNumber, pinNumber, channelNumber, null);
    }

    /**
     * Constructs a DeviceInputMap Object with a chip number, pin number, channel number,  and a channelName.
     *
     * @param chipNumber  maxim chip number that is associated with the channel
     * @param pinNumber   pin number associated with the the channel
     * @param channelNumber         channel number associated with channel
     * @param channelName string name associated with channel
     */
    public DeviceInputMap(int chipNumber, int pinNumber, int channelNumber, String channelName)
    {
        this.chipNumber = chipNumber;

        this.pinNumber = pinNumber;

        // Sets the channel number to the one that was passed
        this.bitNumber = channelNumber;

        // If the channel name is not null, set it to the one passed, otherwise set it to a default one.
        this.channelName = (channelName == null) ? (isC11Pin(chipNumber, pinNumber) ? String.format("Channel %d (C11)", this.bitNumber) : String.format("Channel %d", this.bitNumber)) : channelName;
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- DeviceInputMap Getters & setters

    /**
     * Set the chip number
     *
     * @param chipNumber maxim chip number that is associated with the channel
     */
    public void setChipNumber(int chipNumber)
    {
        this.chipNumber = chipNumber;
    }

    /**
     * Set the pin number
     *
     * @param pinNumber pin number associated with the the channel
     */
    public void setPinNumber(int pinNumber)
    {
        this.pinNumber = pinNumber;
    }

    /**
     * Set the lane number
     *
     * @param number lane number
     */
    public void setLaneNumber(int number)
    {
        this.laneNumber = number;
    }

    /**
     * Set the channel name
     *
     * @param name channel name
     */
    public void setChannelName(String name)
    {
        this.channelName = name;
    }

    /**
     * Set the detector type
     *
     * @param detectorType type of detector
     */
    public void setDetectorType(String detectorType)
    {
        this.detectorType = detectorType;
    }

    /**
     * Set the detector configuration
     *
     * @param detectorConfig configuration of detector
     */
    public void setDetectorConfig(String detectorConfig)
    {
        this.detectorConfig = detectorConfig;
    }

    /**
     * Set the lane direction
     *
     * @param direction direction of traffic flow in the lane
     */
    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    /**
     * Set the channel type
     *
     * @param channelType channel type, ex: intersection, freeway, on-ramp, etc
     */
    public void setChannelType(String channelType)
    {
        this.channelType = channelType;
    }

    /**
     * Return the channel number for this object.
     *
     * @return channel number associated with channel
     */
    public int getChannelNumber()
    {
        return this.bitNumber;
    }

    /**
     * Return the pin number for this object
     *
     * @return pin number associated with channel
     */
    public int getPinNumber()
    {
        return this.pinNumber;
    }

    /**
     * Return the chip number for this object
     *
     * @return chip number associated with channel
     */
    public int getChipNumber()
    {
        return this.chipNumber;
    }

    /**
     * Return the lane number for this object
     *
     * @return number of the lane detector is in
     */
    public int getLaneNumber()
    {
        return this.laneNumber;
    }

    /**
     * Return the channel name for this object
     *
     * @return channel name
     */
    public String getChannelName()
    {
        return this.channelName;
    }

    /**
     * Return the detector type for this object
     *
     * @return detector type
     */
    public String getDetectorType()
    {
        return this.detectorType;
    }

    /**
     * Return the detector configuration for this object
     *
     * @return detector configuration
     */
    public String getDetectorConfig()
    {
        return this.detectorConfig;
    }

    /**
     * Return the direction for this object
     *
     * @return direction of traffic through detector
     */
    public String getDirection()
    {
        return this.direction;
    }

    /**
     * Return the channel type for this object.
     *
     * @return channel type, ex: intersection, freeway, on-ramp, etc
     */
    public String getChannelType()
    {
        return this.channelType;
    }

    @Override
    public String toString()
    {
        // Return the formatted string
        return String.format("Chip: %d\t Pin: %d\tChannel Number: %3d\t Name: %s\t Lane #: %d\t Detector Type: %s\t Detector Config: %s\t Direction: %s\t Channel Type: %5s\t", chipNumber, pinNumber, bitNumber, channelName, laneNumber, detectorType, detectorConfig, direction, channelType);
    }

    /**
     * Used for determining if a pin is routed to the C11 input connector
     *
     * @param chip maxim chip number that is associated with the channel
     * @param pin  pin number associated with the the channel
     * @return true if channel is C11, false if it is not
     */
    private boolean isC11Pin(int chip, int pin)
    {
        return chip == 5 && (pin > 9 && pin < 14 || pin > 14 && pin < 17 || pin > 17 && pin <= 23);
    }
}
