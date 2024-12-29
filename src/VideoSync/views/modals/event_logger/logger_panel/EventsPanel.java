/*
 * File: EventsPanel.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class that is holds the panels for the events table, event options
 *          and hotkey legend
 */

package VideoSync.views.modals.event_logger.logger_panel;

import VideoSync.objects.event_logger.EventProxy;
import VideoSync.views.modals.event_logger.commands.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class EventsPanel extends JPanel implements MouseListener, ItemListener
{

    private final JTable events;                            //events table

    private final JButton buttonAdd;                        //add event button

    private final JButton buttonEdit;                       //update event button

    private final JButton buttonRemove;                     //remove event button

    private final JPopupMenu popupMenu;                     //right-click popup menu

    private final JComboBox<String> channelFilter;          //channel combo box filter

    private final JComboBox<String> tagFilter;              //tag combo box filter

    private final JComboBox<String> orderFilter;            //order combo box filter

    private List<EventProxy> eventProxies;                  //list of event proxies

    public EventsPanel(int panelWidth, int panelHeight)
    {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(panelWidth, panelHeight));

        events = new JTable();
        events.getTableHeader().setReorderingAllowed(false);
        events.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        events.addMouseListener(this);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewportView(events);

        popupMenu = new JPopupMenu();
        popupMenu.addMouseListener(this);

        buttonAdd = new JButton();
        buttonEdit = new JButton();
        buttonRemove = new JButton();

        channelFilter = new JComboBox<>();
        tagFilter = new JComboBox<>();
        orderFilter = new JComboBox<>();

        channelFilter.addItemListener(this);
        tagFilter.addItemListener(this);
        orderFilter.addItemListener(this);

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("All Channels");
        channelFilter.setModel(model);
        channelFilter.setEnabled(false);

        model = new DefaultComboBoxModel<>();
        model.addElement("All Events");
        tagFilter.setModel(model);
        tagFilter.setEnabled(false);

        model = new DefaultComboBoxModel<>();
        model.addElement("Most Recent");
        model.addElement("Least Recent");
        orderFilter.setModel(model);
        orderFilter.setEnabled(false);

        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelTop.add(channelFilter);
        panelTop.add(tagFilter);
        panelTop.add(orderFilter);

        JPanel panelBottom = new JPanel(new GridLayout(1, 2));
        panelBottom.setPreferredSize(new Dimension(panelWidth, 100));

        JPanel panelOptions = new JPanel(new BorderLayout(5, 10));
        panelOptions.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        panelOptions.add(new JLabel("Event Options", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(buttonAdd);
        buttons.add(buttonEdit);
        buttons.add(buttonRemove);
        panelOptions.add(buttons, BorderLayout.CENTER);

        JPanel panelLegend = new JPanel(new BorderLayout());
        panelLegend.add(new JLabel("Hotkey Mappings", SwingConstants.CENTER), BorderLayout.NORTH);
        panelLegend.setBorder(BorderFactory.createLoweredBevelBorder());
        JTextArea textArea = new JTextArea();
        textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setBackground(panelLegend.getBackground());
        textArea.setFont(new Font(textArea.getFont().toString(), Font.PLAIN, 11));
        textArea.setText(
                "\n" +
                        "[N] = New Event\t" + "[E] = Edit Selected Event\t\t" + "[O] = Omit Selected Event\n" +
                        "[J] = Jump to Event\t" + "[O] = Omit Selected Event\t\t" + "[0-9] = Channel Hotkeys\n" +
                        "[DEL] = Delete Selected Event\t" + "[SPACE] = Pause Video"
        );
        panelLegend.add(textArea, BorderLayout.CENTER);

        panelBottom.add(panelLegend);
        panelBottom.add(panelOptions);

        add(panelTop, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(panelBottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    /**
     * Sets the list of event proxies
     *
     * @param eventProxies list of event proxies for use in events JTable
     */
    public void setEventProxies(List<EventProxy> eventProxies)
    {
        this.eventProxies = eventProxies;
        filterEvents();
    }

    /**
     * Clears the events in the events JTable
     */
    public void clearEventsList()
    {
        events.setModel(new DefaultTableModel());
    }

    /**
     * Resets the filter combo box models
     */
    public void resetFilterModels()
    {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("All Channels");
        channelFilter.setModel(model);

        model = new DefaultComboBoxModel<>();
        model.addElement("All Events");
        tagFilter.setModel(model);
    }

    /**
     * Sets the add event command to add events
     *
     * @param commandNewEvent command object to bind new event action to UI elements
     */
    public void setCommandNewEvent(CommandNewEvent commandNewEvent)
    {
        buttonAdd.setAction(commandNewEvent);
        buttonAdd.setText("New Event");
        buttonAdd.setEnabled(false);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("N"), "New Event");
        getActionMap().put("New Event", commandNewEvent);

        JMenuItem jMenuItem = new JMenuItem();
        jMenuItem.setAction(commandNewEvent);
        jMenuItem.setText("New Event");
        popupMenu.add(jMenuItem);
    }

    /**
     * Sets the remove event command to remove selected events
     *
     * @param commandRemoveEvent command object to bind event removal action to UI elements
     */
    public void setCommandRemoveEvent(CommandRemoveEvent commandRemoveEvent)
    {
        commandRemoveEvent.setTargets(events);
        buttonRemove.setAction(commandRemoveEvent);
        buttonRemove.setText("Remove Event");
        buttonRemove.setEnabled(false);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "Delete Event");
        getActionMap().put("Delete Event", commandRemoveEvent);

        JMenuItem jMenuItem = new JMenuItem();
        jMenuItem.setAction(commandRemoveEvent);
        jMenuItem.setText("Remove Event");
        popupMenu.add(jMenuItem);
    }

    /**
     * Sets the buttonEditEvent to update selected events
     *
     * @param commandEditEvent command object to bind event update action to UI elements
     */
    public void setCommandEditEvent(CommandEditEvent commandEditEvent)
    {
        commandEditEvent.setTargets(events);
        buttonEdit.setAction(commandEditEvent);
        buttonEdit.setText("Edit Event");
        buttonEdit.setEnabled(false);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("E"), "Edit Event");
        getActionMap().put("Edit Event", commandEditEvent);

        JMenuItem jMenuItem = new JMenuItem();
        jMenuItem.setAction(commandEditEvent);
        jMenuItem.setText("Edit Event");
        popupMenu.add(jMenuItem);
    }

    /**
     * Sets the event omission command to omit/un-omit events
     *
     * @param commandOmitEvent command object to bind event omission action to UI elements
     */
    public void setCommandOmitEvent(CommandOmitEvent commandOmitEvent)
    {
        commandOmitEvent.setTargets(events);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("O"), "Omit Event");
        getActionMap().put("Omit Event", commandOmitEvent);

        JMenuItem jMenuItem = new JMenuItem();
        jMenuItem.setAction(commandOmitEvent);
        jMenuItem.setText("Omit Event");
        popupMenu.add(jMenuItem);
    }

    /**
     * Sets the jump to event command to set video playback time to event timestamp
     *
     * @param commandJumpToEvent command object to bind event jumping action to UI elements
     */
    public void setCommandJumpToEvent(CommandJumpToEvent commandJumpToEvent)
    {

        commandJumpToEvent.setTarget(events);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("J"), "Jump to Event");
        getActionMap().put("Jump to Event", commandJumpToEvent);

        JMenuItem jMenuItem = new JMenuItem();
        jMenuItem.setAction(commandJumpToEvent);
        jMenuItem.setText("Jump to Timestamp");
        popupMenu.add(jMenuItem);
    }

    /**
     * Method to update event tag filters list for use in event tags filter combo box
     *
     * @param tagsList list of event tags for in event tags filter combo box
     */
    public void updateTagsFilterList(List<String> tagsList)
    {
        // FIXME: toString() may produce NullPointerException
        String selectedTagsFilter = tagFilter.getSelectedItem().toString();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("All Events");
        for(String tag : tagsList)
        {
            model.addElement(tag);
        }
        if(tagsList.contains(selectedTagsFilter))
        {
            tagFilter.setSelectedItem(selectedTagsFilter);
        }
        else
        {
            tagFilter.setSelectedIndex(0);
        }
        tagFilter.setModel(model);
    }

    /**
     * Method to update channel filters list for use in channels filter combo box
     *
     * @param channels list of channel names for use in channels filter combo box
     */
    public void updateChannelsFilterList(List<String> channels)
    {
        String selectedChannelsFilter = Objects.requireNonNull(channelFilter.getSelectedItem()).toString();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("All Channels");
        for(String channel : channels)
        {
            model.addElement(channel);
        }
        if(channels.contains(selectedChannelsFilter))
        {
            channelFilter.setSelectedItem(selectedChannelsFilter);
        }
        else
        {
            channelFilter.setSelectedIndex(0);
        }
        channelFilter.setModel(model);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        /*
         * Display the pop-up menu when the user right-clicks an event
         */
        if(SwingUtilities.isRightMouseButton(e))
        {
            int row = events.rowAtPoint(e.getPoint());
            events.setRowSelectionInterval(row, row);
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
        else
        {
            popupMenu.setVisible(false);
        }
        System.out.println(events.getValueAt(events.getSelectedRow(), events.getSelectedColumn()).toString());
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        //for MacOS
        mouseClicked(e);
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

    @Override
    public void itemStateChanged(ItemEvent e)
    {
        filterEvents();
    }

    /**
     * Enables/disables the UI elements in the events panel
     *
     * @param state boolean flag to enable/disable UI element functionality
     */
    public void updatePanelUIStates(boolean state)
    {
        buttonAdd.setEnabled(state);
        buttonEdit.setEnabled(state);
        buttonRemove.setEnabled(state);
        channelFilter.setEnabled(state);
        tagFilter.setEnabled(state);
        orderFilter.setEnabled(state);
    }

    /**
     * Filters displayed events based on the combo box filter items selected
     */
    public void filterEvents()
    {
        List<EventProxy> filteredProxies = new Vector<>();
        String channel = Objects.requireNonNull(channelFilter.getSelectedItem()).toString();
        String tag = Objects.requireNonNull(tagFilter.getSelectedItem()).toString();

        if(this.eventProxies.size() > 0)
        {
            for(EventProxy eventProxy : eventProxies)
            {
                if(eventProxy.getChannelName().equals(channel) || channel.equals("All Channels"))
                {
                    if(eventProxy.getEventTag().equals(tag) || tag.equals("All Events"))
                    {
                        filteredProxies.add(eventProxy);
                    }
                }
            }
        }

        // FIXME: toString() may produce NullPointerException
        if(orderFilter.getSelectedItem().toString().equals("Least Recent"))
        {
            Collections.sort(filteredProxies);
        }
        else
        {
            filteredProxies.sort(Collections.reverseOrder());
        }

        events.setModel(new EventTableModel(filteredProxies));
        events.setDefaultRenderer(EventProxy.class, new EventTableCellRenderer());
        events.setDefaultEditor(EventProxy.class, new EventTableCellEditor());
        events.setRowHeight(120);
    }
}
