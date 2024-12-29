/*
 * ****************************************************************
 * File: 			RegionContextMenu.java
 * Date Created:  	September 27, 2019
 * Programmer:		Aleksey Zasorin
 *
 * Purpose:			To display a right click context menu in the
 * video render panel.
 *
 * ****************************************************************
 */

package VideoSync.views.menus;

import VideoSync.views.videos.commands.*;

import javax.swing.*;

public class RegionContextMenu extends JPopupMenu
{
    public RegionContextMenu()
    {
    }

    /**
     * Sets the command of the "Convert" JMenuItem to a CommandConvert object and sets the text of the JMenuItem
     *
     * @param co CommandConvert object containing an action to be invoked by the JMenuItem on click
     * @param text String of text to be displayed in the menu
     */
    public void setConvertActionCommand(CommandConvert co, String text)
    {
        // Menu item for the convert to free-form/fixed region action.
        JMenuItem convert = new JMenuItem();
        add(convert);

        convert.setAction(co);
        convert.setText(text);
    }

    /**
     * Sets the command of the "Toggle Vertices" JMenuItem to a CommandToggleVertices object and sets the text of the
     * JMenuItem
     *
     * @param co CommandToggleVertices object containing an action to be invoked by the JMenuItem on click
     * @param text String of text to be displayed in the menu
     */
    public void setToggleVerticesCommand(CommandToggleVertices co, String text)
    {
        // Menu item for the toggle vertices action.
        JMenuItem toggleVertices = new JMenuItem();
        add(toggleVertices);

        toggleVertices.setAction(co);
        toggleVertices.setText(text);
    }

    /**
     * Sets the command of the "Add Vertex" JMenuItem to a CommandAddVertex object and sets the text of the JMenuItem
     *
     * @param co CommandAddVertex object containing an action to be invoked by the JMenuItem on click
     * @param text String of text to be displayed in the menu
     */
    public void setAddVertexCommand(CommandAddVertex co, String text)
    {

        //Menu item for the add vertex action.
        JMenuItem addVertex = new JMenuItem();
        add(addVertex);

        addVertex.setAction(co);
        addVertex.setText(text);
    }

    /**
     * Sets the command of the "Remove Vertex" JMenuItem to a CommandRemoveVertex object and sets the text of the
     * JMenuItem
     *
     * @param co CommandRemoveVertex object containing an action to be invoked by the JMenuItem on click
     * @param text String of text to be displayed in the menu
     */
    public void setRemoveVertexCommand(CommandRemoveVertex co, String text)
    {
        // Menu item for the remove vertex action.
        JMenuItem removeVertex = new JMenuItem();
        add(removeVertex);

        removeVertex.setAction(co);
        removeVertex.setText(text);
    }

    /**
     * Sets the command of the "Reset Regions" JMenuItem to a CommandResetRegions object and sets the text of the
     * JMenuItem
     *
     * @param co CommandResetRegions object containing an action to be invoked by the JMenuItem on click
     * @param text String of text to be displayed in the menu
     */
    public void setToggleResetRegions(CommandResetRegions co, String text)
    {
        // Menu item for the reset regions action.
        JMenuItem toggleResetRegions = new JMenuItem();
        add(toggleResetRegions);

        toggleResetRegions.setAction(co);
        toggleResetRegions.setText(text);
    }
}
