/*
 * ****************************************************************
 * File: 			FixedRegion.java
 * Date Created:  	September 18, 2019
 * Programmer:		Aleksey Zasorin
 *
 * Purpose:			Used to create rectangular regions where each
 *                  vertex/corner is fixed and only height and
 *                  width can be modified. This was originally
 *                  handled by the Region class.
 * ****************************************************************
 */

package VideoSync.objects.graphs;

import java.util.Objects;

public class FixedRegion extends Region
{
    /**
     * Size of the fixed region, relative to upper left corner.
     */
    private int width;
    private int height;

    public FixedRegion()
    {
        width = MIN_WIDTH;
        height = MIN_HEIGHT;
    }

    // TODO: Add proper hashCode method

    // Implemented a custom equals functions to make sure regions could be removed from Collections correctly
    @Override
    public boolean equals(Object o)
    {
        // Are we comparing an object to itself
        if(this == o)
            return true;

        if(o == null)
            return false;

        // Can't be equal if objects are of different classes
        if(getClass() != o.getClass())
            return false;

        FixedRegion fixedRegion = (FixedRegion) o;

        // Check if remaining defining properties are the same
        return Objects.equals(super.getCoordX(), fixedRegion.getCoordX()) &&
                Objects.equals(super.getCoordY(), fixedRegion.getCoordY()) &&
                Objects.equals(width, fixedRegion.getWidth()) &&
                Objects.equals(height, fixedRegion.getHeight()) &&
                Objects.equals(super.getChip(), fixedRegion.getChip()) &&
                Objects.equals(super.getPin(), fixedRegion.getPin()) &&
                Objects.equals(super.getDisplayColor(), fixedRegion.getDisplayColor());
    }

    /**
     * Gets the width of the fixed region
     *
     * @return Returns the width of the fixed region.
     */
    @Override
    public int getWidth()
    {
        return width;
    }

    /**
     * Sets the width of the fixed region.
     *
     * @param width Specifies the width of the fixed region.
     */
    public void setWidth(int width)
    {
        //Ensure that the width is not set below the minimum width.
        this.width = Math.max(width, MIN_WIDTH);
    }

    /**
     * Gets the height of the fixed region.
     *
     * @return Returns the height of the fixed region.
     */
    @Override
    public int getHeight()
    {
        return height;
    }

    /**
     * Sets the height of the fixed region.
     *
     * @param height Height of the fixed region.
     */
    public void setHeight(int height)
    {
        //Ensure that the height is not set below the minimum height.
        this.height = Math.max(height, MIN_HEIGHT);
    }
}
