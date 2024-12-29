package VideoSync.commands.menu;

import VideoSync.models.DataModel;
import VideoSync.models.DataModelProxy;
import VideoSync.views.modals.average_speed.AverageSpeed;
import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandAverageSpeed extends AbstractAction
{
    private final AverageSpeed cas;
    private DataWindow dataWindow;
    private DataModelProxy dataModelProxy;

    public CommandAverageSpeed(DataModel dm)
    {
        cas = new AverageSpeed(dm.returnProxy());
    }

    public AverageSpeed getAverageSpeed()
    {
        return this.cas;
    }

    public void actionPerformed(ActionEvent e)
    {
        if(!cas.isVisible())
        {
            cas.setLocationRelativeTo(dataWindow);
            cas.setVisible(true);
            cas.update();
        }
    }
}
