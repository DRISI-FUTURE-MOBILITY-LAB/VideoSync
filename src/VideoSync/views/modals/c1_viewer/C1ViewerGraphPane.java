package VideoSync.views.modals.c1_viewer;

import VideoSync.objects.c1.C1GroupIdentifier;
import VideoSync.objects.graphs.Line;
import VideoSync.views.tabbed_panels.graphs.GraphWindow;

import java.awt.*;
import java.util.Vector;

public class C1ViewerGraphPane extends GraphWindow
{
    /**
     * Collection of variance lines to be drawn
     */
    private Vector<Line> varianceLines;

    private Vector<Rectangle> selectionHighLights;

    private Vector<C1GroupIdentifier> groupIdentifiers;

    private Rectangle selectionRectangle = new Rectangle();

    /**
     * Color of variance lines
     */
    private Color varianceColor = Color.RED;

    private Color highlightColor = Color.RED;

    public static final int IDENTIFIER_SIZE = 18;

    private static final int HIGHLIGHT_OFFSET = -4;
    private static final int HIGHLIGHT_THICKNESS = 3;

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        if(groupIdentifiers != null && !groupIdentifiers.isEmpty())
        {
            for(C1GroupIdentifier p : groupIdentifiers)
            {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke((float) HIGHLIGHT_THICKNESS, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                g2d.fillRect(p.pos.x - IDENTIFIER_SIZE/2, p.pos.y - IDENTIFIER_SIZE/2, IDENTIFIER_SIZE, IDENTIFIER_SIZE);


                g2d.setColor(Color.WHITE);
                g2d.setFont(g2d.getFont().deriveFont(Font.BOLD,14.0f));
                FontMetrics metrics = g2d.getFontMetrics();
                int x = (p.pos.x - IDENTIFIER_SIZE/2) + (IDENTIFIER_SIZE - metrics.stringWidth(p.name)) / 2;
                int y = (p.pos.y - IDENTIFIER_SIZE/2) + ((IDENTIFIER_SIZE - metrics.getHeight()) / 2) + metrics.getAscent();
                g2d.drawString(p.name, x, y);
            }
        }

        if(selectionHighLights != null && !selectionHighLights.isEmpty())
        {
            for(Rectangle rect : selectionHighLights)
            {
                g2d.setColor(highlightColor);
                g2d.setStroke(new BasicStroke((float) HIGHLIGHT_THICKNESS, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

                int offset = HIGHLIGHT_OFFSET;
                if(rect.width < HIGHLIGHT_THICKNESS*2 + Math.abs(HIGHLIGHT_OFFSET)/2)
                    offset = -offset;

                g2d.drawRect(rect.x - offset, rect.y - HIGHLIGHT_OFFSET, rect.width + offset*2, rect.height + HIGHLIGHT_OFFSET*2);
            }
        }

        // Draw selection box
        if(selectionRectangle.width != 0 && selectionRectangle.height != 0)
        {
            g2d.setColor(highlightColor);
            g2d.setStroke(new BasicStroke((float) HIGHLIGHT_THICKNESS, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g2d.drawRect(selectionRectangle.x, selectionRectangle.y, selectionRectangle.width, selectionRectangle.height);
        }

        if(varianceLines != null && !varianceLines.isEmpty())
        {
            // Loop through all of the state objects to be drawn on the screen.
            for(int i = varianceLines.size() - 1; i >= 0; i--)
            {
                // Get the Line object from the specified index
                Line l = varianceLines.elementAt(i);

                // Set the line color to be drawn. This is changeable for each graph pane
                // Set the stroke color to the same as center stroke. This makes it distinguishable
                // Draw the line object onto the graphics window
                g2d.setColor(varianceColor);
                g2d.setStroke(CENTER_STROKE);
                g2d.drawLine(l.getX0(), l.getY0(), l.getX1(), l.getY1());
            }
        }
    }

    /**
     * Sets the variance lines to be drawn to the screen
     * @param lines collection of Lines to be drawn
     */
    public void setVarianceLines(Vector<Line> lines)
    {
        varianceLines = lines;
        repaint();
    }

    /**
     * Sets the color of the variance lines
     * @param color color to set variance lines to
     */
    public void setVarianceColor(Color color)
    {
        varianceColor = color;
        repaint();
    }

    public void setSelectionHighlights(Vector<Rectangle> lines)
    {
        selectionHighLights = lines;
        repaint();
    }

    public void setGroupIdentifiers(Vector<C1GroupIdentifier> points)
    {
        groupIdentifiers = points;
        repaint();
    }

    public void setSelectionRectangle(int x, int y, int width, int height)
    {
        selectionRectangle.setLocation(x, y);
        selectionRectangle.setSize(width, height);
        repaint();
    }
}
