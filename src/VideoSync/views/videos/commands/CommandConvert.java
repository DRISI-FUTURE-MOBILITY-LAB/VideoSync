package VideoSync.views.videos.commands;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.graphs.FixedRegion;
import VideoSync.objects.graphs.FreeFormRegion;
import VideoSync.objects.graphs.Region;
import VideoSync.views.videos.DirectVideoRenderPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandConvert extends AbstractAction
{
    /**
     * Reference to the render panel so we can add and remove video regions.
     */
    private DirectVideoRenderPanel directRenderPanel;

    /**
     * Region on which we are operating.
     */
    private Region dataRegion;

    /**
     * Reference to the data model proxy so that we can add and remove video regions.
     */
    private DataModelProxy dmp;

    /**
     * Sets the values of the targets this command will use to function.
     *
     * @param directRenderPanel Reference to the direct render panel in which the regions reside.
     * @param dataRegion        Reference to the region we're converting.
     * @param dmp               Reference to the data model proxy in order to update the graph panels correctly.
     */
    public void setTargets(DirectVideoRenderPanel directRenderPanel, Region dataRegion, DataModelProxy dmp)
    {
        this.directRenderPanel = directRenderPanel;
        this.dataRegion = dataRegion;
        this.dmp = dmp;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(dataRegion instanceof FixedRegion)
        {
            // Convert from FixedRegion to FreeFormRegion
            FixedRegion fixedRegion = (FixedRegion) dataRegion;
            FreeFormRegion freeRegion = new FreeFormRegion();

            // Copy attributes from the fixedRegion to the freeRegion
            freeRegion.setCoordX(fixedRegion.getCoordX());
            freeRegion.setCoordY(fixedRegion.getCoordY());
            freeRegion.setChip(fixedRegion.getChip());
            freeRegion.setPin(fixedRegion.getPin());
            freeRegion.setDeviceType(fixedRegion.getDeviceType());
            freeRegion.setDisplayColor(fixedRegion.getDisplayColor());
            freeRegion.setEnabled(fixedRegion.getEnabled());
            int width = fixedRegion.getWidth();
            int height = fixedRegion.getHeight();

            // Set the positions of the vertices in the freeRegion
            freeRegion.setVertices(new int[]{0, width, width, 0}, new int[]{0, 0, height, height});

            // We need to replace the region in the render panel
            // The new region object has to be placed into the same position as the old region object.
            directRenderPanel.replaceRegion(fixedRegion, freeRegion);

            // We need to replace the old region object reference in the respective graph panel with the new region object reference.
            dmp.replaceGraphVideoRegion(fixedRegion, freeRegion);
        }
        else if(dataRegion instanceof FreeFormRegion)
        {
            //Convert from FreeFormRegion to FixedRegion
            FreeFormRegion freeRegion = (FreeFormRegion) dataRegion;
            FixedRegion fixedRegion = new FixedRegion();

            // Copy attributes from the freeRegion to the fixedRegion
            fixedRegion.setCoordX(freeRegion.getAdjustedX());
            fixedRegion.setCoordY(freeRegion.getAdjustedY());
            fixedRegion.setChip(freeRegion.getChip());
            fixedRegion.setPin(freeRegion.getPin());
            fixedRegion.setDeviceType(freeRegion.getDeviceType());
            fixedRegion.setDisplayColor(freeRegion.getDisplayColor());
            fixedRegion.setEnabled(freeRegion.getEnabled());
            fixedRegion.setWidth(freeRegion.getWidth());
            fixedRegion.setHeight(freeRegion.getHeight());

            // We need to replace the region in the render panel
            // The new region object has to be placed into the same position as the old region object.
            directRenderPanel.replaceRegion(freeRegion, fixedRegion);

            // We need to replace the old region object reference in the respective graph panel with the new region object reference.
            dmp.replaceGraphVideoRegion(freeRegion, fixedRegion);
        }
    }
}
