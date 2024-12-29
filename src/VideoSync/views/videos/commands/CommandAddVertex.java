package VideoSync.views.videos.commands;

import VideoSync.objects.graphs.FreeFormRegion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CommandAddVertex extends AbstractAction
{
    /**
     * Region on which we are operating.
     */
    private FreeFormRegion freeRegion;

    /**
     * Points between which the new vertex must be added.
     */
    private Point p1;
    private Point p2;

    /**
     * Position on the video region line segment where the new vertex must be added.
     */
    private Point pIntersection;

    /**
     * Sets the values of the targets this command will use to function.
     *
     * @param freeRegion Reference to the region where we're adding the new vertex.
     */
    public void setTargets(FreeFormRegion freeRegion, Point p1, Point p2, Point pIntersection)
    {
        this.freeRegion = freeRegion;
        this.p1 = p1;
        this.p2 = p2;
        this.pIntersection = pIntersection;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        freeRegion.addVertex(p1, p2, pIntersection);
    }
}
