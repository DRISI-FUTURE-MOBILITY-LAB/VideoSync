package VideoSync.views.videos.commands;

import VideoSync.objects.graphs.FreeFormRegion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CommandRemoveVertex extends AbstractAction
{
    /**
     * Region on which we are operating.
     */
    private FreeFormRegion freeRegion;

    /**
     * Vertex that needs to be removed.
     */
    private Point vertex;

    /**
     * Sets the values of the targets this command will use to function.
     *
     * @param freeRegion Reference to the region we're removing the vertex from.
     */
    public void setTargets(FreeFormRegion freeRegion, Point vertex)
    {
        this.freeRegion = freeRegion;
        this.vertex = vertex;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        freeRegion.removeVertex(vertex);
    }
}
