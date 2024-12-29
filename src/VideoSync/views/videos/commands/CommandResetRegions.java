package VideoSync.views.videos.commands;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.graphs.FixedRegion;
import VideoSync.objects.graphs.FreeFormRegion;
import VideoSync.objects.graphs.Region;
import VideoSync.views.videos.DirectVideoRenderPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Vector;

public class CommandResetRegions extends AbstractAction
{

    private static final int REGION_BORDER_WIDTH = 4;

    /**
     * Reference to the render panel so we can add and remove video regions.
     */
    private DirectVideoRenderPanel directRenderPanel;

    private DataModelProxy dmp;

    /**
     * Sets the values of the targets this command will use to function.
     *
     * @param directRenderPanel Reference to the direct render panel in which the regions reside.
     */
    public void setTargets(DirectVideoRenderPanel directRenderPanel, DataModelProxy dmp)
    {
        this.directRenderPanel = directRenderPanel;
        this.dmp = dmp;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Vector<Region> dataRegions = this.directRenderPanel.getDataRegions();
        for(int i = 0; i < dataRegions.size(); i++)
        {
            Region dataRegion = dataRegions.get(i);
            if(dataRegion instanceof FreeFormRegion)
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
                fixedRegion.setWidth(100);
                fixedRegion.setHeight(100);

                //The starting point for each region should be in the upper left, with each to the right of the one before it.
                int x = calculateRegionCoordinateX(i + 1);
                int y = calculateRegionCoordinateY(i + 1);
                fixedRegion.setCoordX(x);
                fixedRegion.setCoordY(y);

                // We need to replace the region in the render panel
                // The new region object has to be placed into the same position as the old region object.
                directRenderPanel.replaceRegion(freeRegion, fixedRegion);

                // We need to replace the old region object reference in the respective graph panel with the new region object reference.
                dmp.replaceGraphVideoRegion(freeRegion, fixedRegion);
            }
            else
            {
                FixedRegion fixedRegion = (FixedRegion) dataRegion;
                int x = calculateRegionCoordinateX(i + 1);
                int y = calculateRegionCoordinateY(i + 1);
                fixedRegion.setCoordX(x);
                fixedRegion.setCoordY(y);
                fixedRegion.setWidth(100);
                fixedRegion.setHeight(100);
            }
        }
    }

    private int calculateRegionCoordinateX(int regionCount)
    {
        return (100 * (regionCount % 9)) + (REGION_BORDER_WIDTH * (regionCount % 9));
    }

    private int calculateRegionCoordinateY(int regionCount)
    {
        if(regionCount < 9)
        {
            return REGION_BORDER_WIDTH / 2;
        }
        else if(regionCount < 18)
        {
            return REGION_BORDER_WIDTH * 50 / 2;
        }
        else
        {
            return REGION_BORDER_WIDTH * 100 / 2;
        }
    }

}
