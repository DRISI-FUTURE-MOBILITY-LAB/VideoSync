package VideoSync.views.videos.commands;

import VideoSync.views.videos.DirectVideoRenderPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandToggleVertices extends AbstractAction
{
    /**
     * Used to reference and notify the DataModel of any changes
     */
    private DirectVideoRenderPanel directRenderPanel;

    /**
     * Sets the values of the targets this command will use to function.
     *
     * @param directRenderPanel Reference to the DirectVideoRenderPanel where we are toggling the vertices
     */
    public void setTargets(DirectVideoRenderPanel directRenderPanel)
    {
        this.directRenderPanel = directRenderPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        directRenderPanel.setVerticesEnabled(!directRenderPanel.getVerticesEnabled());
    }
}
