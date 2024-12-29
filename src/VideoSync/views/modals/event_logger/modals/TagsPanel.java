/*
 * File: TagsPanel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is used to allow the user to configure
 *          event tags for use in Event Logger
 */

package VideoSync.views.modals.event_logger.modals;

import VideoSync.models.DataModel;
import VideoSync.models.DataModelProxy;
import VideoSync.views.modals.event_logger.commands.CommandAddTag;
import VideoSync.views.modals.event_logger.commands.CommandRemoveTag;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class TagsPanel extends JPanel implements Observer, MouseListener
{

    /**
     * String constant for tag name
     */
    private final String tagNameHeader = "Tag Name";

    /**
     * String constant for tag description
     */
    private final String tagDescriptionHeader = "Tag Description";

    /**
     * Update tag button
     */
    private final JButton updateTag;

    /**
     * Remove tag button
     */
    private final JButton removeTag;

    /**
     * Tag table list
     */
    private final JTable tagsList;

    /**
     * Tag text field
     */
    private final JTextField tagTextfield;

    /**
     * Tag description text area
     */
    private final JTextArea tagDescriptionTextArea;

    /**
     * Data model proxy reference
     */
    private DataModelProxy dataModelProxy;

    public TagsPanel()
    {

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setAlignmentX(Component.CENTER_ALIGNMENT);

        tagTextfield = new JTextField("Tag Name");
        tagTextfield.setPreferredSize(new Dimension(tagTextfield.getWidth(), 12));

        tagDescriptionTextArea = new JTextArea("Tag Description");
        tagDescriptionTextArea.setBorder(new EtchedBorder());
        tagDescriptionTextArea.setPreferredSize(new Dimension(tagDescriptionTextArea.getWidth(), 24));
        tagDescriptionTextArea.setLineWrap(true);

        tagsList = new JTable(new DefaultTableModel())
        {
            @Override
            public boolean editCellAt(int row, int column, EventObject e)
            {
                return false;
            }
        };

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(tagNameHeader);
        model.addColumn(tagDescriptionHeader);
        tagsList.setModel(model);
        tagsList.getColumnModel().getColumn(0).setPreferredWidth(20);
        tagsList.addMouseListener(this);

        updateTag = new JButton();
        removeTag = new JButton();

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewportView(tagsList);
        scrollPane.setPreferredSize(new Dimension(getWidth(), 300));

        add(scrollPane);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(tagTextfield);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(tagDescriptionTextArea);
        add(Box.createRigidArea(new Dimension(0, 10)));

        Box updateBox = Box.createHorizontalBox();
        updateBox.add(Box.createHorizontalGlue());
        updateBox.add(updateTag);
        add(updateBox);

        add(Box.createRigidArea(new Dimension(0, 10)));

        Box removeBox = Box.createHorizontalBox();
        removeBox.add(Box.createHorizontalGlue());
        removeBox.add(removeTag);
        add(removeBox);
    }

    /**
     * Initializes the commands used in the tags panel
     *
     * @param dataModel data model reference
     */
    public void initializeCommands(DataModel dataModel)
    {

        /*
         * Initialize the add tag command and bind to 'add tag' button
         */
        CommandAddTag commandAddTag = new CommandAddTag(dataModel);
        commandAddTag.setTargets(tagTextfield, tagDescriptionTextArea);
        updateTag.setAction(commandAddTag);
        updateTag.setText("Update Tag");

        /*
         * Initialize the remove tag command and bind to 'remove tag' button
         */
        CommandRemoveTag commandRemoveTag = new CommandRemoveTag(dataModel);
        commandRemoveTag.setTargets(tagTextfield, tagDescriptionTextArea, this);
        removeTag.setAction(commandRemoveTag);
        removeTag.setText("Remove Tag");
    }

    @Override
    public void update(Observable o, Object arg)
    {
        if(arg.equals("Add Tag") || arg.equals("Remove Tag"))
        {
            Map<String, String> eventTags = dataModelProxy.getEventTags();
            if(eventTags != null) updateTagsList(eventTags);
        }
    }

    /**
     * Sets the data model proxy reference
     *
     * @param dataModelProxy reference
     */
    public void setDataModelProxy(DataModelProxy dataModelProxy)
    {
        this.dataModelProxy = dataModelProxy;
    }

    /**
     * Updates the tags list table
     *
     * @param eventTags map of added tags and their descriptions
     */
    public void updateTagsList(Map<String, String> eventTags)
    {

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(tagNameHeader);
        model.addColumn(tagDescriptionHeader);

        if(!eventTags.isEmpty())
        {
            for(String tag : eventTags.keySet())
            {
                model.addRow(new String[]{tag, eventTags.get(tag)});
                System.out.println(tag + "\t\t" + eventTags.get(tag));
            }
            removeTag.setEnabled(true);
        }
        else
        {
            removeTag.setEnabled(false);
        }

        tagsList.setModel(model);
        tagsList.getColumnModel().getColumn(0).setPreferredWidth(20);

        revalidate();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if(e.getSource() == tagsList)
        {
            tagTextfield.setText(tagsList.getModel().getValueAt(tagsList.getSelectedRow(), 0).toString());
            tagDescriptionTextArea.setText(tagsList.getModel().getValueAt(tagsList.getSelectedRow(), 1).toString());
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }
}
