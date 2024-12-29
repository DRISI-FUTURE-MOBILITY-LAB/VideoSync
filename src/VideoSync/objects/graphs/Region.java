/*
 * ****************************************************************
 * File: 			Region.java
 * Date Created:  	January 19, 2017
 * Programmer:		Elliot Hawkins
 *
 * Purpose:			Used for creating region elements that will
 *                  be used in rendering data on video frames.
 *
 * Modified:        September 18, 2019
 * Programmer:      Aleksey Zasorin
 * Purpose:         Converted class to an abstract class in order
 *                  to support two different types of regions. All
 *                  shared attributes and methods were kept in
 *                  this class and any shared methods with varying
 *                  implementations were declared here but
 *                  implemented in subclasses.
 *
 * ****************************************************************
 */

package VideoSync.objects.graphs;


import VideoSync.objects.EDeviceType;

import java.awt.*;

public abstract class Region
{
    /**
     * Specifies the minimum size of a fixed region. Attempts to set to smaller sizes will instead use these values.
     */
    public static final int MIN_WIDTH = 10;
    public static final int MIN_HEIGHT = 10;

    /**
     * Location of the region, relative to upper left corner.
     */
    private int coordX;
    private int coordY;

    /**
     * Sets the color the region will be rendered with.
     */
    private Color displayColor;

    /**
     * The chip that is associated with the channel
     */
    private int chip;

    /**
     * The pin that is associated with the channel
     */
    private int pin;

    /**
     * The device type that this region is supposed to display.
     */
    private EDeviceType deviceType;

    /**
     * Whether or not this region should be displayed.
     */
    private Boolean isEnabled;

    public Region()
    {
        coordX = 0;
        coordY = 0;
        displayColor = Color.BLACK;
        chip = -1;
        pin = -1;
        deviceType = EDeviceType.DEVICE_NONE;
        isEnabled = false;
    }

    /**
     * Gets the horizontal location of the leftmost side.
     *
     * @return Returns X coordinate position of the region.
     */
    public int getCoordX()
    {
        return coordX;
    }

    /**
     * Sets the vertical location of the region relative to its upper left coordinate
     *
     * @param x X coordinate position of the region.
     */
    public void setCoordX(int x)
    {
        coordX = x;
    }

    /**
     * Gets the vertical location of the topmost side
     *
     * @return Returns Y coordinate position of the region.
     */
    public int getCoordY()
    {
        return coordY;
    }

    /**
     * Sets the horizontal location of the region relative to its upper left coordinate
     *
     * @param y Y coordinate position of the region.
     */
    public void setCoordY(int y)
    {
        coordY = y;
    }

    /**
     * Gets the width of the region
     *
     * @return Returns the width of the region.
     */
    public abstract int getWidth();

    /**
     * Gets the height of the region.
     *
     * @return Returns the height of the region.
     */
    public abstract int getHeight();

    /**
     * @return the chip number that the channel is associated with
     */
    public int getChip()
    {
        return chip;
    }

    /**
     * @return the pin number that the channel is associated with
     */
    public int getPin()
    {
        return pin;
    }

    /**
     * Sets the chip number that is associated with the channel
     *
     * @param chip int value of the chip number associated with channel
     */
    public void setChip(int chip)
    {
        this.chip = chip;
    }

    /**
     * Sets the pin number that is associated with the channel
     *
     * @param pin int value of the pin number associated with channel
     */
    public void setPin(int pin)
    {
        this.pin = pin;
    }

    /**
     * Gets the color that this region will display with.
     *
     * @return Returns a Color value of the color of the region.
     */
    public Color getDisplayColor()
    {
        return displayColor;
    }

    /**
     * Sets the color used to display this region.
     *
     * @param color Color value this region will be set to.
     */
    public void setDisplayColor(Color color)
    {
        displayColor = color;
    }

    /**
     * Gets whether or not this region is enabled.
     *
     * @return Returns a boolean representing whether the region is enabled or not.
     */
    public Boolean getEnabled()
    {
        return isEnabled;
    }

    /**
     * Sets whether or not this region will be displayed
     *
     * @param enabled Boolean value representing whether this region will be displayed.
     */
    public void setEnabled(Boolean enabled)
    {
        isEnabled = enabled;
    }

    /**
     * Gets the type of device that this region is displaying.
     *
     * @return Returns a enum value of type EDeviceType representing the device type of this region.
     */
    public EDeviceType getDeviceType()
    {
        return deviceType;
    }

    /**
     * Sets the device type, ex. C1.
     *
     * @param deviceType EDeviceType enum variable representing the type of device this region represents.
     */
    public void setDeviceType(EDeviceType deviceType)
    {
        this.deviceType = deviceType;
    }
}
