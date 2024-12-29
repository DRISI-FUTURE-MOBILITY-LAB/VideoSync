/*
 * ****************************************************************
 * File: 			Line.java
 * Date Created:  	June 5, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			Used for creating line elements that will
 * 					be used in creating all of the graph lines
 *
 * ****************************************************************
 */
package VideoSync.objects.graphs;

// FIXME: Replace this with regular Java Line class

public class Line
{
    /**
     * The first point for the line
     */
    private final int x0;
    private final int y0;

    /**
     * The second point for the line
     */
    private final int x1;
    private final int y1;

    // -- Line Constructor

    /**
     * Construct a line element that can be displayed in the graph panel
     *
     * @param x0 x value of the first point
     * @param y0 y value of the first point
     * @param x1 x value of the second point
     * @param y1 y value of the second point
     */
    public Line(int x0, int y0, int x1, int y1)
    {
        this.x0 = x0;
        this.y0 = y0;

        this.x1 = x1;
        this.y1 = y1;
    }

    // -- Line Getters

    /**
     * Return the x0 position
     *
     * @return first point x value
     */
    public int getX0()
    {
        return this.x0;
    }

    /**
     * Return the y0 position
     *
     * @return first point y value
     */
    public int getY0()
    {
        return this.y0;
    }

    /**
     * Return the x1 position
     *
     * @return second point x value
     */
    public int getX1()
    {
        return this.x1;
    }

    /**
     * Return the y1 position
     *
     * @return second point y value
     */
    public int getY1()
    {
        return this.y1;
    }

    /**
     * Returns a string representation of the line object.
     */
    public String toString()
    {
        return String.format("Line from (x0, y0) to (x1, y1): (%d, %d) to (%d, %d)", this.x0, this.y0, this.x1, this.y1);
    }
}
