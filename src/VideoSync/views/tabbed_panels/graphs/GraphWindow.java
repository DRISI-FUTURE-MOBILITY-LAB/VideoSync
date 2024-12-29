/*
 * ****************************************************************
 * File: 			GraphPane.java
 * Date Created:  	June 6, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			To render the graph's on the JPanel as the data
 * is sent from the GraphPanel
 *
 * ****************************************************************
 */

package VideoSync.views.tabbed_panels.graphs;

import VideoSync.objects.graphs.Line;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class GraphWindow extends JPanel
{
    private static final long serialVersionUID = 1L;

    /**
     * Line stroke for the tick lines
     */
    protected static final BasicStroke NORMAL_STROKE = new BasicStroke(1.0F);

    /**
     * Line stroke or for the center tick line
     */
    protected static final BasicStroke CENTER_STROKE = new BasicStroke(2.0F);

    /**
     * Line stroke for the thick center tick line.
     */
    protected static final BasicStroke CENTER_THICK_STROKE = new BasicStroke(2.0F);

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graph pane Variable Declarations

    /**
     * Set the state line default color to Black
     */
    private Color lineColor = Color.BLACK;

    /**
     * Set the tick line default color to Gray
     */
    protected final Color tickColor = Color.GRAY;

    /**
     * Used for storing the tick marks that are to be rendered
     */
    private Vector<Line> ticks;

    /**
     * Used for storing the state lines that are to be rendered
     */
    private Vector<Line> states;

    /**
     * Used for indicating if we need to draw the center line thicker.
     */
    private boolean thickCenter;

    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graph Pane Construction

    /**
     * Construct a new Graph Pane with no parameters. Calls repaint when the view has been created.
     */
    public GraphWindow()
    {
        super(null);

        this.repaint();
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------
    // -- Graph Pane Paint

    /**
     * Paints the components onto the graphics window. This will render the tick marks as well as the state diagrams
     *
     * @see Line
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // If the ticks array is not null and empty, we can draw the tick marks on the screen.
        if(ticks != null && !ticks.isEmpty())
        {
            // Loop through all of the tick marks in the array and draw them at the points
            for(int i = 0; i < ticks.size(); i++)
            {
                // Get the Line object from the specified index
                Line l = ticks.elementAt(i);

                g2d.setColor(tickColor);
                g2d.setStroke(NORMAL_STROKE);

                // Draw the line object onto the graphics window
                g2d.drawLine(l.getX0(), l.getY0(), l.getX1(), l.getY1());
            }
        }

        // Draw Center Line
        g2d.setColor(Color.RED);
        g2d.setStroke((this.thickCenter) ? CENTER_THICK_STROKE : CENTER_STROKE);
        g2d.drawLine((this.getWidth() / 2), 0, (this.getWidth() / 2), this.getHeight());

        // If the states array is not null,
        if(states != null && !states.isEmpty())
        {
            //Keep track of where the leftmost and rightmost points are on-screen
            int leftmostPos = this.getWidth();
            int leftHeight = this.getHeight() / 2;

            //Is rightmost line needed? Seems that the graph already handles positions after end of data...
            int rightmostPos = 0;
            int rightHeight = this.getHeight() / 2;

            // Loop through all of the state objects to be drawn on the screen.
            for(int i = states.size() - 1; i >= 0; i--)
            {
                // Get the Line object from the specified index
                Line l = states.elementAt(i);

                // Set the line color to be drawn. This is changeable for each graph pane
                // Set the stroke color to the same as center stroke. This makes it distinguishable
                // Draw the line object onto the graphics window
                g2d.setColor(lineColor);
                g2d.setStroke(CENTER_STROKE);
                g2d.drawLine(l.getX0(), l.getY0(), l.getX1(), l.getY1());

                //Update side lines
                if(i == states.size() - 1)
                {
                    if(leftmostPos >= l.getX0())
                    {
                        leftmostPos = l.getX0();
                        leftHeight = l.getY0();
                    }
                }

                if(rightmostPos <= l.getX1())
                {
                    rightmostPos = l.getX1();
                    rightHeight = l.getY1();
                }
            }

            //Set color for lines extending past beginning/end of data.
            //Black is a special case because Java cannot make black brighter.
            if(lineColor == Color.BLACK)
            {
                g2d.setColor(Color.GRAY);
            }
            else
            {
                //Calculate perceived color brightness and adjust in appropriate direction
                double colorBrightness = Math.sqrt((int) (0.241 * Math.pow(lineColor.getRed(), 2) + 0.691 * Math.pow(lineColor.getGreen(), 2) + 0.068 * Math.pow(lineColor.getBlue(), 2)));
                if(colorBrightness > 127)
                {
                    g2d.setColor(lineColor.darker().darker());
                }
                else
                {
                    g2d.setColor(lineColor.brighter().brighter());
                }
            }

            //Draw extended lines on graph
            g2d.drawLine(0, leftHeight, leftmostPos, leftHeight);

            //Use check to prevent a stray pixel on edge of graph when nothing is extended.
            if(rightmostPos != getWidth())
            {
                g2d.drawLine(rightmostPos, rightHeight, this.getWidth(), rightHeight);
            }
        }
    }

    // -- Graph Pane Setters

    /**
     * Set the states to be drawn by the panels paint method.
     * Calls repaint when states has been assigned.
     * @param states collection of states to be drawn
     */
    public void setStates(Vector<Line> states)
    {
        this.states = states;
        this.repaint();
    }

    public Vector<Line> getStates()
    {
        return states;
    }

    /**
     * Set the tick marks to be drawn by the panels paint method.
     * Calls repaint when ticks has been assigned.
     * @param ticks collection of tick marks to be drawn
     */
    public void setTicks(Vector<Line> ticks, boolean thickCenter)
    {
        this.thickCenter = thickCenter;
        this.ticks = ticks;
        this.repaint();
    }

    /**
     * Sets the line color to be used when rendering the states
     * @param lineColor color of lines to be drawn
     */
    public void setLineColor(Color lineColor)
    {
        this.lineColor = lineColor;
        this.repaint();
    }
}
