package VideoSync.views.modals.c1_viewer.commands;

import VideoSync.analyzers.C1DataCollector;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CommandClearAutoGroup extends AbstractAction
{
    private C1DataCollector cdc;

    public CommandClearAutoGroup(String name)
    {
        super(name);
    }

    public void setTargets(C1DataCollector dataCollector)
    {
        cdc = dataCollector;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        cdc.clearAutoGroups();
    }
}
