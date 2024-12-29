package VideoSync.analyzers;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.Pair;
import VideoSync.objects.c1.C1Channel;
import VideoSync.objects.c1.C1Event;
import VideoSync.objects.c1.C1Group;
import VideoSync.objects.c1.C1GroupIdentifier;
import VideoSync.views.modals.c1_viewer.C1Viewer;
import VideoSync.views.modals.c1_viewer.C1ViewerGraphPane;
import com.opencsv.CSVWriter;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


public class C1DataCollector implements Serializable
{
    private static final int IDENTIFIER_OFFSET = 20;

    private transient DataModelProxy dmp;

    private transient C1Viewer c1Viewer;

    private transient C1ViewerGraphPane observedGraphPane;
    private transient C1ViewerGraphPane referenceGraphPane;

    private final Vector<C1Event> selectedObservedEvents;
    private final Vector<C1Event> selectedReferenceEvents;

    private final HashMap<ChipPinRelationship, Vector<C1Group>> groups;

    private final HashMap<ChipPinRelationship, Vector<C1Event>> falsePositives;
    private final HashMap<ChipPinRelationship, Vector<C1Event>> falseNegatives;

    private final HashMap<ChipPinRelationship, Vector<C1Event>> ignoredObserved;
    private final HashMap<ChipPinRelationship, Vector<C1Event>> ignoredReference;

    private final HashMap<ChipPinRelationship, HashMap<C1Event, C1Group>> eventMap;

    private class ChipPinRelationship implements Serializable
    {
        public final Pair<Integer, Integer> observedChipPin;
        public final Pair<Integer, Integer> referenceChipPin;

        public ChipPinRelationship(Pair<Integer, Integer> observedChipPin, Pair<Integer, Integer> referenceChipPin)
        {
            this.observedChipPin = observedChipPin;
            this.referenceChipPin = referenceChipPin;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if(!(o instanceof ChipPinRelationship)) return false;

            ChipPinRelationship rel = (ChipPinRelationship) o;
            return Objects.equals(observedChipPin, rel.observedChipPin) && Objects.equals(referenceChipPin, rel.referenceChipPin);
        }

        @Override
        public int hashCode()
        {
            int result = observedChipPin.hashCode();
            result = 47 * result + referenceChipPin.hashCode();
            return result;
        }
    }

    public C1DataCollector(C1Viewer c1Viewer, DataModelProxy dataModelProxy, C1ViewerGraphPane observedGraphPane, C1ViewerGraphPane referenceGraphPane)
    {
        this.c1Viewer = c1Viewer;
        this.dmp = dataModelProxy;
        this.observedGraphPane = observedGraphPane;
        this.referenceGraphPane = referenceGraphPane;
        this.selectedObservedEvents = new Vector<>();
        this.selectedReferenceEvents = new Vector<>();
        this.groups = new HashMap<>();
        this.falsePositives = new HashMap<>();
        this.falseNegatives = new HashMap<>();
        this.ignoredObserved = new HashMap<>();
        this.ignoredReference = new HashMap<>();

        this.eventMap = new HashMap<>();
    }

    public void loadC1DataCollector(DataModelProxy dmp, C1Viewer c1Viewer, C1ViewerGraphPane observedGraphPane, C1ViewerGraphPane referenceGraphPane)
    {
        this.dmp = dmp;
        this.c1Viewer = c1Viewer;
        this.observedGraphPane = observedGraphPane;
        this.referenceGraphPane = referenceGraphPane;
    }

    public void selectObservedEvent(int lineLeftPx, int lineRightPx, int chip, int pin, boolean useStartTime, boolean selectGroup, boolean boxSelect)
    {
        selectEvent(lineLeftPx, lineRightPx, chip, pin, useStartTime, true, selectGroup, boxSelect);
    }

    public void selectReferenceEvent(int lineLeftPx, int lineRightPx, int chip, int pin, boolean useStartTime, boolean selectGroup, boolean boxSelect)
    {
        selectEvent(lineLeftPx, lineRightPx, chip, pin, useStartTime, false, selectGroup, boxSelect);
    }

    private void selectEvent(int lineLeftPx, int lineRightPx, int chip, int pin, boolean useStartTime, boolean observedData, boolean selectGroup, boolean boxSelect)
    {
        C1ViewerGraphPane graphPane;
        Vector<C1Channel> c1Channels;
        if(observedData)
        {
            graphPane = observedGraphPane;
            c1Channels = c1Viewer.getObservedEventChannels();
        }
        else
        {
            graphPane = referenceGraphPane;
            c1Channels = c1Viewer.getReferenceEventChannels();
        }

        double gSeconds = dmp.getGraphWindowSeconds();
        double time;
        if(observedData || c1Viewer.isCurrentReferenceMode(C1Viewer.ReferenceMode.SELF_REFERENCE))
            time = dmp.getCurrentPosition() + dmp.getGraphOffset();
        else
            time = dmp.getCurrentPosition();

        // This is the number of milliseconds that are being displayed
        double difference = (gSeconds * 1000) / 2;

        // Min and Max is the range of times being displayed on the graph
        long min = (long) (time - difference);
        long max = (long) (time + difference);

        C1Channel c = getCorrespondingChannel(c1Channels, chip, pin);
        if(c != null)
        {
            Vector<C1Event> c1Events = c.getEvents(min, max);

            long timeBeingDisplayed = max - min;

            for(C1Event e : c1Events)
            {
                double eventLeftPx = 0.0, eventRightPx = 0.0;
                double eventLeft = (e.getStartTime() - min);
                double eventRight = (e.getEndTime() - min);

                if(eventLeft != 0.0)
                    eventLeftPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventLeft;

                if(eventRight != 0.0)
                    eventRightPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventRight;

                if(selectGroup)
                {
                    C1Group selectedGroup = getEventGroup(e);

                    if(selectedGroup != null)
                    {
                        Vector<C1Event> observedGroupEvents = selectedGroup.getObservedEventsList();
                        Vector<C1Event> referenceGroupEvents = selectedGroup.getReferenceEventsList();

                        for(C1Event evt : observedGroupEvents)
                        {
                            if(useStartTime)
                            {
                                if(((int) eventLeftPx) == lineLeftPx || (c1Events.size() == 1 && e.getStartTime() < min && e.getEndTime() > max))
                                {
                                    if(boxSelect)
                                        toggleSelectedEvent(evt, true, true);
                                    else
                                        toggleSelectedEvent(evt, true);
                                }
                            }
                            else
                            {
                                if(((int) eventRightPx) == lineRightPx || (c1Events.size() == 1 && e.getStartTime() < min && e.getEndTime() > max))
                                {
                                    if(boxSelect)
                                        toggleSelectedEvent(evt, true, true);
                                    else
                                        toggleSelectedEvent(evt, true);
                                }
                            }
                        }

                        for(C1Event evt : referenceGroupEvents)
                        {
                            if(useStartTime)
                            {
                                if(((int) eventLeftPx) == lineLeftPx || (c1Events.size() == 1 && e.getStartTime() < min && e.getEndTime() > max))
                                {
                                    if(boxSelect)
                                        toggleSelectedEvent(evt, true, false);
                                    else
                                        toggleSelectedEvent(evt, false);
                                }
                            }
                            else
                            {
                                if(((int) eventRightPx) == lineRightPx || (c1Events.size() == 1 && e.getStartTime() < min && e.getEndTime() > max))
                                {
                                    if(boxSelect)
                                        toggleSelectedEvent(evt, true, false);
                                    else
                                        toggleSelectedEvent(evt, false);
                                }
                            }
                        }
                    }
                }
                else
                {
                    if(useStartTime)
                    {
                        if(((int) eventLeftPx) == lineLeftPx || (c1Events.size() == 1 && e.getStartTime() < min && e.getEndTime() > max))
                        {
                            if(boxSelect)
                                toggleSelectedEvent(e, true, observedData);
                            else
                                toggleSelectedEvent(e, observedData);
                        }
                    }
                    else
                    {
                        if(((int) eventRightPx) == lineRightPx || (c1Events.size() == 1 && e.getStartTime() < min && e.getEndTime() > max))
                        {
                            if(boxSelect)
                                toggleSelectedEvent(e, true, observedData);
                            else
                                toggleSelectedEvent(e, observedData);
                        }
                    }
                }
            }
        }
    }

    private void toggleSelectedEvent(C1Event e, boolean observedData)
    {
        if(observedData)
        {
            toggleSelectedEvent(e, !selectedObservedEvents.contains(e), true);
        }
        else
        {
            toggleSelectedEvent(e, !selectedReferenceEvents.contains(e), false);
        }
    }

    private void toggleSelectedEvent(C1Event e, boolean select, boolean observedData)
    {
        if(observedData)
        {
            if(!select)
            {
                selectedObservedEvents.remove(e);
                System.out.println("OBSERVED EVENT UNSELECTED, START TIME: " + e.getStartTime() + ", END TIME: " + e.getEndTime() + ", DURATION: " + e.getDuration());
            }
            else
            {
                selectedObservedEvents.add(e);
                System.out.println("OBSERVED EVENT SELECTED, START TIME: " + e.getStartTime() + ", END TIME: " + e.getEndTime() + ", DURATION: " + e.getDuration());
            }
        }
        else
        {
            if(!select)
            {
                selectedReferenceEvents.remove(e);
                System.out.println("REFERENCE EVENT UNSELECTED, START TIME: " + e.getStartTime() + ", END TIME: " + e.getEndTime() + ", DURATION: " + e.getDuration());
            }
            else
            {
                selectedReferenceEvents.add(e);
                System.out.println("REFERENCE EVENT SELECTED, START TIME: " + e.getStartTime() + ", END TIME: " + e.getEndTime() + ", DURATION: " + e.getDuration());
            }
        }
    }

    public void clearSelectedEvents()
    {
        selectedObservedEvents.removeAllElements();
        selectedReferenceEvents.removeAllElements();
        c1Viewer.calculateStateLines();

        System.out.println("SELECTED CHANNELS CLEARED");
    }

    public boolean collectedDataExists()
    {
        for(Vector<C1Group> list : groups.values())
        {
            if(!list.isEmpty())
                return true;
        }

        for(Vector<C1Event> list : falsePositives.values())
        {
            if(!list.isEmpty())
                return true;
        }

        for(Vector<C1Event> list : falseNegatives.values())
        {
            if(!list.isEmpty())
                return true;
        }

        for(Vector<C1Event> list : ignoredObserved.values())
        {
            if(!list.isEmpty())
                return true;
        }

        for(Vector<C1Event> list : ignoredReference.values())
        {
            if(!list.isEmpty())
                return true;
        }

        return false;
    }

    public void resetCollectedData()
    {
        groups.clear();
        eventMap.clear();

        falsePositives.clear();
        falseNegatives.clear();

        ignoredObserved.clear();
        ignoredReference.clear();

        System.out.println("COLLECTED DATA CLEARED");
    }

    public Vector<Rectangle> getObservedSelectionHighlights()
    {
        return generateSelectionHighLights(dmp.getCurrentPosition() + dmp.getGraphOffset(), true);
    }

    public Vector<Rectangle> getReferenceSelectionHighlights()
    {
        long time = (c1Viewer.isCurrentReferenceMode(C1Viewer.ReferenceMode.SELF_REFERENCE)) ?
                dmp.getCurrentPosition() + dmp.getGraphOffset() : dmp.getCurrentPosition();
        return generateSelectionHighLights(time, false);
    }

    private Vector<Rectangle> generateSelectionHighLights(double time, boolean observedData)
    {
        C1ViewerGraphPane graphPane;
        Vector<C1Event> c1Events;
        if(observedData)
        {
            graphPane = observedGraphPane;
            c1Events = selectedObservedEvents;
        }
        else
        {
            graphPane = referenceGraphPane;
            c1Events = selectedReferenceEvents;
        }

        Vector<Rectangle> rect = new Vector<>();

        double gSeconds = dmp.getGraphWindowSeconds();

        // This is the number of milliseconds that are being displayed
        double difference = (gSeconds * 1000) / 2;

        // Min and Max is the range of times being displayed on the graph
        long min = (long) (time - difference);
        long max = (long) (time + difference);

        long timeBeingDisplayed = max - min;

        int base = (int) (graphPane.getSize().height * 0.25);
        int top = (int) (graphPane.getSize().height * 0.75);

        for(C1Event evt : c1Events)
        {
            double eventLeftPx = 0.0, eventRightPx = 0.0;
            double eventLeft = (evt.getStartTime() - min);
            double eventRight = (evt.getEndTime() - min);

            if(eventLeft != 0.0)
                eventLeftPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventLeft;

            if(eventRight != 0.0)
                eventRightPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventRight;

            rect.add(new Rectangle((int) eventLeftPx, base, (int) (eventRightPx - eventLeftPx), top - base));
        }

        return rect;
    }

    public void groupSelectedEvents()
    {
        if(selectedObservedEvents.isEmpty() && selectedReferenceEvents.isEmpty())
            return;

        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();

        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));

        // We first need to check to see if any of the selected events are already marked as a false event
        for(C1Event e : selectedObservedEvents)
        {
            // Technically, it should be impossible to add events from different channels into one group since C1Viewer
            // should reset selected event vectors when the channel changes, but I'm adding this check just in case.
            if(e.getChip() != obsChip && e.getPin() != obsPin)
            {
                System.out.println("Unable to create group, selected events must be from the same channel.");
                return;
            }

            if(falsePositives.getOrDefault(chipPinRel, new Vector<>()).contains(e))
            {
                System.out.println("Unable to create group, some selected events are already marked as false positives.");
                return;
            }

            if(falseNegatives.getOrDefault(chipPinRel, new Vector<>()).contains(e))
            {
                System.out.println("Unable to create group, some selected events are already marked as false negatives.");
                return;
            }

            if(ignoredObserved.getOrDefault(chipPinRel, new Vector<>()).contains(e))
            {
                System.out.println("Unable to create group, some selected events are already marked as ignored.");
                return;
            }
        }

        for(C1Event e : selectedReferenceEvents)
        {
            // Technically, it should be impossible to add events from different channels into one group since C1Viewer
            // should reset selected event vectors when the channel changes, but I'm adding this check just in case.
            if(e.getChip() != refChip && e.getPin() != refPin)
            {
                System.out.println("Unable to create group, selected events must be from the same channel.");
                return;
            }

            if(falsePositives.getOrDefault(chipPinRel, new Vector<>()).contains(e))
            {
                System.out.println("Unable to create group, some selected events are already marked as false positives.");
                return;
            }

            if(falseNegatives.getOrDefault(chipPinRel, new Vector<>()).contains(e))
            {
                System.out.println("Unable to create group, some selected events are already marked as false negatives.");
                return;
            }

            if(ignoredReference.getOrDefault(chipPinRel, new Vector<>()).contains(e))
            {
                System.out.println("Unable to create group, some selected events are already marked as ignored.");
                return;
            }
        }

        Vector<C1Group> selectedGroups = new Vector<>();

        for(C1Event evt : selectedObservedEvents)
        {
            if(eventMap.containsKey(chipPinRel))
            {
                if(eventMap.get(chipPinRel).containsKey(evt))
                {
                    if(!selectedGroups.contains(eventMap.get(chipPinRel).get(evt)))
                        selectedGroups.add(eventMap.get(chipPinRel).get(evt));
                }
            }
        }

        for(C1Event evt : selectedReferenceEvents)
        {
            if(eventMap.containsKey(chipPinRel))
            {
                if(eventMap.get(chipPinRel).containsKey(evt))
                {
                    if(!selectedGroups.contains(eventMap.get(chipPinRel).get(evt)))
                        selectedGroups.add(eventMap.get(chipPinRel).get(evt));
                }
            }
        }

        if(selectedGroups.size() == 0)
        {
            boolean observedContiguous = isContiguous(new HashSet<>(selectedObservedEvents), true);
            boolean referenceContiguous = isContiguous(new HashSet<>(selectedReferenceEvents), false);

            if(observedContiguous && referenceContiguous)
            {
                addGroup(new Vector<>(selectedObservedEvents), new Vector<>(selectedReferenceEvents), obsChip, obsPin, refChip, refPin, false);
                System.out.println("Group created successfully. OBSERVED EVENTS: " + selectedObservedEvents.size() + ", REFERENCE EVENTS: " + selectedReferenceEvents.size());
            }
            else
            {
                System.out.println("Unable to create group, events must be contiguous.");
                return;
            }
        }
        else if(selectedGroups.size() == 1)
        {
            C1Group g = selectedGroups.get(0);
            HashSet<C1Event> combinedObserved = new HashSet<>(g.getObservedEventsList());
            combinedObserved.addAll(selectedObservedEvents);
            boolean observedContiguous = isContiguous(combinedObserved, true);

            HashSet<C1Event> combinedReference = new HashSet<>(g.getReferenceEventsList());
            combinedReference.addAll(selectedReferenceEvents);
            boolean referenceContiguous = isContiguous(combinedReference, false);

            if(observedContiguous && referenceContiguous)
            {
                removeGroup(g, obsChip, obsPin, refChip, refPin);
                addGroup(new Vector<>(combinedObserved), new Vector<>(combinedReference), obsChip, obsPin, refChip, refPin, false);
                System.out.println("Group modified successfully. OBSERVED EVENTS: " + selectedObservedEvents.size() + ", REFERENCE EVENTS: " + selectedReferenceEvents.size());
            }
            else
            {
                System.out.println("Unable to create group, events must be contiguous.");
                return;
            }
        }
        else
        {
            System.out.println("Unable to create group, selected events cannot be part of more than 1 group.");
            return;
        }

        selectedObservedEvents.removeAllElements();
        selectedReferenceEvents.removeAllElements();
        c1Viewer.calculateStateLines();
    }

    private void addGroup(Vector<C1Event> observedList, Vector<C1Event> referenceList, int obsChip, int obsPin, int refChip, int refPin, boolean auto)
    {
        C1Group newGroup = new C1Group(observedList, referenceList, obsChip, obsPin, refChip, refPin, auto);
        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));
        if(!groups.containsKey(chipPinRel))
            groups.put(chipPinRel, new Vector<>());
        groups.get(chipPinRel).add(newGroup);

        for(C1Event evt : observedList)
        {
            if(!eventMap.containsKey(chipPinRel))
                eventMap.put(chipPinRel, new HashMap<>());
            eventMap.get(chipPinRel).put(evt, newGroup);
        }

        for(C1Event evt: referenceList)
        {
            if(!eventMap.containsKey(chipPinRel))
                eventMap.put(chipPinRel, new HashMap<>());
            eventMap.get(chipPinRel).put(evt, newGroup);
        }

        Collections.sort(groups.get(chipPinRel));
    }

    private void removeGroup(C1Group g)
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();

        removeGroup(g, obsChip, obsPin, refChip, refPin);
    }

    private void removeGroup(C1Group g, int obsChip, int obsPin, int refChip, int refPin)
    {
        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));
        if(groups.containsKey(chipPinRel))
        {
            groups.get(chipPinRel).remove(g);
            Collections.sort(groups.get(chipPinRel));
        }

        for(C1Event evt : g.getObservedEventsList())
        {
            if(eventMap.containsKey(chipPinRel))
                eventMap.get(chipPinRel).remove(evt);
        }

        for(C1Event evt : g.getReferenceEventsList())
        {
            if(eventMap.containsKey(chipPinRel))
                eventMap.get(chipPinRel).remove(evt);
        }
    }

    private boolean isEventInGroup(C1Event event, C1Group group)
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();
        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));

        return isEventInGroup(event, group, chipPinRel);
    }

    private boolean isEventInGroup(C1Event event, C1Group group, ChipPinRelationship chipPinRel)
    {
        if(eventMap.containsKey(chipPinRel))
        {
            if(eventMap.get(chipPinRel).containsKey(event))
                return eventMap.get(chipPinRel).get(event).equals(group);
        }

        return false;
    }

    private boolean isEventInGroup(C1Event event)
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();
        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));

        return isEventInGroup(event, chipPinRel);
    }

    private boolean isEventInGroup(C1Event event, ChipPinRelationship chipPinRel)
    {
        if(eventMap.containsKey(chipPinRel))
            return eventMap.get(chipPinRel).containsKey(event);

        return false;
    }

    private C1Group getEventGroup(C1Event event)
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();
        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));

        return getEventGroup(event, chipPinRel);
    }

    private C1Group getEventGroup(C1Event event, ChipPinRelationship chipPinRel)
    {
        if(eventMap.containsKey(chipPinRel))
        {
            if(eventMap.get(chipPinRel).containsKey(event))
                return eventMap.get(chipPinRel).get(event);
        }

        return null;
    }

    public C1Channel getCorrespondingChannel(Vector<C1Channel> channels, int chip, int pin)
    {
        Optional<C1Channel> optional = channels.stream().filter(x -> x.getChip() == chip && x.getPin() == pin).findFirst();

        return optional.orElse(null);
    }

    public void ungroupSelectedEvents()
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();

        if(selectedObservedEvents.isEmpty() && selectedReferenceEvents.isEmpty())
            return;

        // Make a list of all groups represented by the selection (even if partial)
        // For every group, make a list of events from that group that were selected
        HashMap<C1Group, Pair<Vector<C1Event>, Vector<C1Event>>> groupEvtList = new HashMap<>();
        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));
        Vector<C1Group> groupList = groups.getOrDefault(chipPinRel, new Vector<>());
        for(C1Group g : groupList)
        {
            Vector<C1Event> groupsObservedEvents = new Vector<>();
            for(C1Event evt : selectedObservedEvents)
            {
                if(isEventInGroup(evt, g))
                    groupsObservedEvents.add(evt);
            }

            Vector<C1Event> groupsReferenceEvents = new Vector<>();
            for(C1Event evt : selectedReferenceEvents)
            {
                if(isEventInGroup(evt, g))
                {
                    groupsReferenceEvents.add(evt);
                }
            }

            if(!(groupsObservedEvents.isEmpty() && groupsReferenceEvents.isEmpty()))
                groupEvtList.put(g, new Pair<>(groupsObservedEvents, groupsReferenceEvents));
        }

        // For every entry, replace existing group with an updated group
        for(HashMap.Entry<C1Group, Pair<Vector<C1Event>, Vector<C1Event>>> entry : groupEvtList.entrySet())
        {
            C1Group g = entry.getKey();
            Pair<Vector<C1Event>, Vector<C1Event>> pair = entry.getValue();
            Vector<C1Event> observedGroupsEvtList = pair.x;
            Vector<C1Event> referenceGroupsEvtList = pair.y;

            Vector<C1Event> updatedObservedList = g.getObservedEventsList();
            Vector<C1Event> updatedReferenceList = g.getReferenceEventsList();
            updatedObservedList.removeAll(observedGroupsEvtList);
            updatedReferenceList.removeAll(referenceGroupsEvtList);

            int groupObsChip = g.getObsChip();
            int groupObsPin = g.getObsPin();
            int groupRefChip = g.getRefChip();
            int groupRefPin = g.getRefPin();
            boolean auto = g.isAuto();

            if(!(updatedObservedList.isEmpty() && updatedReferenceList.isEmpty()))
            {
                boolean observedContiguous = isContiguous(new HashSet<>(updatedObservedList), true);
                boolean referenceContiguous = isContiguous(new HashSet<>(updatedReferenceList), false);

                if(observedContiguous && referenceContiguous)
                {
                    removeGroup(g);
                    addGroup(updatedObservedList, updatedReferenceList, groupObsChip, groupObsPin, groupRefChip, groupRefPin, auto);
                    System.out.println("Group modified successfully. OBSERVED EVENTS: " + updatedObservedList.size() + ", REFERENCE EVENTS: " + updatedReferenceList.size());
                }
                else
                {
                    System.out.println("Unable to modify group, resulting group would be non-contiguous.");
                    return;
                }
            }
            else
            {
                removeGroup(g);
                System.out.println("Group removed successfully. OBSERVED EVENTS: " + g.getObservedEventsList().size() + ", REFERENCE EVENTS: " + g.getReferenceEventsList().size());
            }
        }

        selectedObservedEvents.removeAllElements();
        selectedReferenceEvents.removeAllElements();
        c1Viewer.calculateStateLines();
    }

    public void markFalseEvent()
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();

        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));

        if(!selectedObservedEvents.isEmpty())
        {
            for(C1Event evt : selectedObservedEvents)
            {
                if(ignoredObserved.getOrDefault(chipPinRel, new Vector<>()).contains(evt) || isEventInGroup(evt))
                {
                    System.out.println("Event is already either ignored or in a group.");
                    return;
                }

                if(falsePositives.containsKey(chipPinRel))
                {
                    if(!falsePositives.get(chipPinRel).contains(evt))
                        falsePositives.get(chipPinRel).add(evt);
                    else
                        falsePositives.get(chipPinRel).remove(evt);
                }
                else
                {
                    falsePositives.put(chipPinRel, new Vector<>());
                    falsePositives.get(chipPinRel).add(evt);
                }
            }

            selectedObservedEvents.removeAllElements();
            c1Viewer.calculateStateLines();
        }

        if(!selectedReferenceEvents.isEmpty())
        {
            for(C1Event evt : selectedReferenceEvents)
            {
                if(ignoredReference.getOrDefault(chipPinRel, new Vector<>()).contains(evt) || isEventInGroup(evt))
                {
                    System.out.println("Event is already either ignored or in a group.");
                    return;
                }

                if(falseNegatives.containsKey(chipPinRel))
                {
                    if(!falseNegatives.get(chipPinRel).contains(evt))
                        falseNegatives.get(chipPinRel).add(evt);
                    else
                        falseNegatives.get(chipPinRel).remove(evt);
                }
                else
                {
                    falseNegatives.put(chipPinRel, new Vector<>());
                    falseNegatives.get(chipPinRel).add(evt);
                }
            }

            selectedReferenceEvents.removeAllElements();
            c1Viewer.calculateStateLines();
        }
    }

    public Vector<C1GroupIdentifier> getObservedGroupIdentifiers(int obsChip, int obsPin, int refChip, int refPin)
    {
        return generateGroupIdentifiers(dmp.getCurrentPosition() + dmp.getGraphOffset(), obsChip, obsPin, refChip, refPin, true);
    }

    public Vector<C1GroupIdentifier> getReferenceGroupIdentifiers(int obsChip, int obsPin, int refChip, int refPin)
    {
        long time = (c1Viewer.isCurrentReferenceMode(C1Viewer.ReferenceMode.SELF_REFERENCE)) ?
                dmp.getCurrentPosition() + dmp.getGraphOffset() : dmp.getCurrentPosition();
        return generateGroupIdentifiers(time, obsChip, obsPin, refChip, refPin, false);
    }

    private Vector<C1GroupIdentifier> generateGroupIdentifiers(double time, int obsChip, int obsPin, int refChip, int refPin, boolean observedData)
    {
        C1ViewerGraphPane graphPane;
        if(observedData)
            graphPane = observedGraphPane;
        else
            graphPane = referenceGraphPane;

        Vector<C1GroupIdentifier> points = new Vector<>();

        double gSeconds = dmp.getGraphWindowSeconds();

        // This is the number of milliseconds that are being displayed
        double difference = (gSeconds * 1000) / 2;

        // Min and Max is the range of times being displayed on the graph
        long min = (long) (time - difference);
        long max = (long) (time + difference);

        long timeBeingDisplayed = max - min;

        int base = (int) (graphPane.getSize().height * 0.25);
        int top = (int) (graphPane.getSize().height * 0.75);

        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));
        Vector<C1Group> groupList = groups.getOrDefault(chipPinRel, new Vector<>());
        for(C1Group grp : groupList)
        {
            if(grp.getObsChip() == obsChip && grp.getObsPin() == obsPin && grp.getRefChip() == refChip && grp.getRefPin() == refPin)
            {
                Vector<C1Event> eventsList;
                if(observedData)
                    eventsList = grp.getObservedEventsList();
                else
                    eventsList = grp.getReferenceEventsList();

                for(C1Event evt : eventsList)
                {
                    double eventCenterPx = 0.0;
                    double eventCenter = (evt.getStartTime() - min) + (double) (evt.getEndTime() - evt.getStartTime()) / 2;

                    if(eventCenter != 0.0)
                        eventCenterPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventCenter;

                    double maxPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * (max - min);

                    double eventLeftPx = 0.0, eventRightPx = 0.0;
                    double eventLeft = (evt.getStartTime() - min);
                    double eventRight = (evt.getEndTime() - min);

                    if(eventLeft != 0.0)
                        eventLeftPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventLeft;

                    if(eventRight != 0.0)
                        eventRightPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventRight;

                    if(eventCenterPx > (maxPx - IDENTIFIER_OFFSET) && eventLeftPx < maxPx)
                    {
                        eventCenterPx = Math.max(maxPx - IDENTIFIER_OFFSET, eventLeftPx + ((float) C1ViewerGraphPane.IDENTIFIER_SIZE) / 2);
                    }
                    else if(eventCenterPx < IDENTIFIER_OFFSET && eventRightPx > 0)
                    {
                        eventCenterPx = Math.min(IDENTIFIER_OFFSET, eventRightPx - ((float) C1ViewerGraphPane.IDENTIFIER_SIZE) / 2);
                    }

                    points.add(new C1GroupIdentifier((int) eventCenterPx, base + (top - base) / 2, Integer.toString(groupList.indexOf(grp) + 1)));
                }
            }
        }

        Vector<C1Event> falseList;
        if(observedData)
            falseList = falsePositives.getOrDefault(chipPinRel, new Vector<>());
        else
            falseList = falseNegatives.getOrDefault(chipPinRel, new Vector<>());

        for(C1Event evt : falseList)
        {
            boolean falseChipPinCheck;
            if(observedData)
                falseChipPinCheck = evt.getChip() == obsChip && evt.getPin() == obsPin;
            else
                falseChipPinCheck = evt.getChip() == refChip && evt.getPin() == refPin;

            if(falseChipPinCheck)
            {
                double eventCenterPx = 0.0;
                double eventCenter = (evt.getStartTime() - min) + (double) (evt.getEndTime() - evt.getStartTime()) / 2;

                if(eventCenter != 0.0)
                    eventCenterPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventCenter;

                double maxPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * (max - min);

                double eventLeftPx = 0.0, eventRightPx = 0.0;
                double eventLeft = (evt.getStartTime() - min);
                double eventRight = (evt.getEndTime() - min);

                if(eventLeft != 0.0)
                    eventLeftPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventLeft;

                if(eventRight != 0.0)
                    eventRightPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventRight;

                if(eventCenterPx > (maxPx - IDENTIFIER_OFFSET) && eventLeftPx < maxPx)
                {
                    eventCenterPx = Math.max(maxPx - IDENTIFIER_OFFSET, eventLeftPx + ((float) C1ViewerGraphPane.IDENTIFIER_SIZE) / 2);
                }
                else if(eventCenterPx < IDENTIFIER_OFFSET && eventRightPx > 0)
                {
                    eventCenterPx = Math.min(IDENTIFIER_OFFSET, eventRightPx - ((float) C1ViewerGraphPane.IDENTIFIER_SIZE) / 2);
                }

                points.add(new C1GroupIdentifier((int) eventCenterPx, base + (top - base) / 2, observedData ? "FP" : "FN"));
            }
        }

        Vector<C1Event> ignoredList;
        if(observedData)
            ignoredList = ignoredObserved.getOrDefault(chipPinRel, new Vector<>());
        else
            ignoredList = ignoredReference.getOrDefault(chipPinRel, new Vector<>());

        for(C1Event evt : ignoredList)
        {
            boolean ignoredChipPinCheck;
            if(observedData)
                ignoredChipPinCheck = evt.getChip() == obsChip && evt.getPin() == obsPin;
            else
                ignoredChipPinCheck = evt.getChip() == refChip && evt.getPin() == refPin;

            if(ignoredChipPinCheck)
            {
                double eventCenterPx = 0.0;
                double eventCenter = (evt.getStartTime() - min) + (double) (evt.getEndTime() - evt.getStartTime()) / 2;

                if(eventCenter != 0.0)
                    eventCenterPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventCenter;

                double maxPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * (max - min);

                double eventLeftPx = 0.0, eventRightPx = 0.0;
                double eventLeft = (evt.getStartTime() - min);
                double eventRight = (evt.getEndTime() - min);

                if(eventLeft != 0.0)
                    eventLeftPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventLeft;

                if(eventRight != 0.0)
                    eventRightPx = ((double) graphPane.getSize().width / timeBeingDisplayed) * eventRight;

                if(eventCenterPx > (maxPx - IDENTIFIER_OFFSET) && eventLeftPx < maxPx)
                {
                    eventCenterPx = Math.max(maxPx - IDENTIFIER_OFFSET, eventLeftPx + ((float) C1ViewerGraphPane.IDENTIFIER_SIZE) / 2);
                }
                else if(eventCenterPx < IDENTIFIER_OFFSET && eventRightPx > 0)
                {
                    eventCenterPx = Math.min(IDENTIFIER_OFFSET, eventRightPx - ((float) C1ViewerGraphPane.IDENTIFIER_SIZE) / 2);
                }

                points.add(new C1GroupIdentifier((int) eventCenterPx, base + (top - base) / 2, "IG"));
            }
        }

        return points;
    }

    public boolean isContiguous(HashSet<C1Event> events, boolean observedData)
    {
        if(events.isEmpty())
            return true;

        Vector<C1Channel> eventChannelList;

        if(observedData)
            eventChannelList = c1Viewer.getObservedEventChannels();
        else
            eventChannelList = c1Viewer.getReferenceEventChannels();

        // Chip and pin to get channel
        C1Event elem = events.iterator().next();
        int chip = elem.getChip();
        int pin = elem.getPin();
        C1Channel ch = getCorrespondingChannel(eventChannelList, chip, pin);

        // Get all events between the first and last events in the list
        long startTime = events.stream().min(Comparator.comparing(C1Event::getStartTime)).get().getStartTime();
        long endTime = events.stream().max(Comparator.comparing(C1Event::getEndTime)).get().getEndTime();
        HashSet<C1Event> chEvents = new HashSet<>(ch.getEvents(startTime-1, endTime+1));

        // If the retrieved list of events from the channel event list is equal to the list of events we were given
        // this implies that there were no additional events in between that would have made the list of events
        // non-contiguous.
        return chEvents.equals(new HashSet<>(events));
    }

    public void autoGroupEvents()
    {
        Vector<C1Channel> observedChannels = c1Viewer.getObservedEventChannels();
        Vector<C1Channel> referenceChannels = c1Viewer.getReferenceEventChannels();

        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();

        C1Channel observedChannel = getCorrespondingChannel(observedChannels, obsChip, obsPin);
        C1Channel referenceChannel = getCorrespondingChannel(referenceChannels, refChip, refPin);

        Vector<C1Event> observedEvents = observedChannel.getEvents(Long.MIN_VALUE, Long.MAX_VALUE);
        Vector<C1Event> referenceEvents = referenceChannel.getEvents(Long.MIN_VALUE, Long.MAX_VALUE);

        Iterator<C1Event> iterObserved = observedEvents.iterator();
        Iterator<C1Event> iterReference = referenceEvents.iterator();
        while(iterObserved.hasNext() && iterReference.hasNext())
        {
            C1Event observedEvent = iterObserved.next();
            C1Event referenceEvent = iterReference.next();

            int graphOffset = (c1Viewer.isCurrentReferenceMode(C1Viewer.ReferenceMode.SELF_REFERENCE)) ? dmp.getGraphOffset() : 0;

            while(observedEvent.getEndTime() - dmp.getGraphOffset() < 0)
            {
                if(iterObserved.hasNext())
                    observedEvent = iterObserved.next();
                else
                    return;
            }

            while(referenceEvent.getEndTime() - graphOffset < 0)
            {
                if(iterReference.hasNext())
                    referenceEvent = iterReference.next();
                else
                    return;
            }

            if(observedEvent.getStartTime() - dmp.getGraphOffset() > dmp.getSliderMax() || referenceEvent.getStartTime() - graphOffset > dmp.getSliderMax())
                return;

            while(!eventValidForAutoGrouping(observedEvent, true))
            {
                if(iterObserved.hasNext())
                    observedEvent = iterObserved.next();
                else
                    return;
            }

            while(!eventValidForAutoGrouping(referenceEvent, false))
            {
                if(iterReference.hasNext())
                    referenceEvent = iterReference.next();
                else
                    return;
            }

            Vector<C1Event> tempObserved = new Vector<>();
            Vector<C1Event> tempReference = new Vector<>();

            tempObserved.add(observedEvent);
            tempReference.add(referenceEvent);

            addGroup(tempObserved, tempReference, obsChip, obsPin, refChip, refPin, true);
            System.out.println("Auto group created successfully. OBSERVED EVENTS: " + tempObserved.size() + ", REFERENCE EVENTS: " + tempReference.size());
        }
        c1Viewer.calculateStateLines();
    }

    private boolean eventValidForAutoGrouping(C1Event evt, boolean observedData)
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();
        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));

        boolean evtInGroup = eventMap.containsKey(chipPinRel) && eventMap.get(chipPinRel).containsKey(evt);

        boolean falseEvent;
        if(observedData)
            falseEvent = falsePositives.getOrDefault(chipPinRel, new Vector<>()).contains(evt);
        else
            falseEvent = falseNegatives.getOrDefault(chipPinRel, new Vector<>()).contains(evt);

        boolean evtIgnored;
        if(observedData)
            evtIgnored = ignoredObserved.getOrDefault(chipPinRel, new Vector<>()).contains(evt);
        else
            evtIgnored = ignoredReference.getOrDefault(chipPinRel, new Vector<>()).contains(evt);

        return !evtInGroup && !falseEvent && !evtIgnored;
    }

    public void clearAutoGroups()
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();

        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));
        if(groups.containsKey(chipPinRel))
            groups.get(chipPinRel).removeIf(C1Group::isAuto);

        if(eventMap.containsKey(chipPinRel))
            eventMap.get(chipPinRel).entrySet().removeIf(entries -> entries.getValue().isAuto());

        c1Viewer.calculateStateLines();
    }

    public void markIgnoredEvent()
    {
        int obsChip = c1Viewer.getCurrObsChannelChip();
        int obsPin = c1Viewer.getCurrObsChannelPin();
        int refChip = c1Viewer.getCurrRefChannelChip();
        int refPin = c1Viewer.getCurrRefChannelPin();

        ChipPinRelationship chipPinRel = new ChipPinRelationship(new Pair<>(obsChip, obsPin), new Pair<>(refChip, refPin));

        if(!selectedObservedEvents.isEmpty())
        {
            for(C1Event evt : selectedObservedEvents)
            {
                if(falsePositives.getOrDefault(chipPinRel, new Vector<>()).contains(evt) || isEventInGroup(evt))
                {
                    System.out.println("Event is already either marked as a false event or in a group.");
                    return;
                }

                if(ignoredObserved.containsKey(chipPinRel))
                {
                    if(!ignoredObserved.get(chipPinRel).contains(evt))
                        ignoredObserved.get(chipPinRel).add(evt);
                    else
                        ignoredObserved.get(chipPinRel).remove(evt);
                }
                else
                {
                    ignoredObserved.put(chipPinRel, new Vector<>());
                    ignoredObserved.get(chipPinRel).add(evt);
                }
            }

            selectedObservedEvents.removeAllElements();
            c1Viewer.calculateStateLines();
        }

        if(!selectedReferenceEvents.isEmpty())
        {
            for(C1Event evt: selectedReferenceEvents)
            {
                if(falseNegatives.getOrDefault(chipPinRel, new Vector<>()).contains(evt) || isEventInGroup(evt))
                {
                    System.out.println("Event is already either marked as a false event or in a group.");
                    return;
                }

                if(ignoredReference.containsKey(chipPinRel))
                {
                    if(!ignoredReference.get(chipPinRel).contains(evt))
                        ignoredReference.get(chipPinRel).add(evt);
                    else
                        ignoredReference.get(chipPinRel).remove(evt);
                }
                else
                {
                    ignoredReference.put(chipPinRel, new Vector<>());
                    ignoredReference.get(chipPinRel).add(evt);
                }
            }

            selectedReferenceEvents.removeAllElements();
            c1Viewer.calculateStateLines();
        }
    }

    public void exportDataAsCSV(File csvFile) throws IOException
    {
        CSVWriter writer = new CSVWriter(new FileWriter(csvFile.getPath()));

        String content = "";

        content = (new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("features.txt")))).lines().collect(Collectors.joining());

        ArrayList<String[]> stringArray = new ArrayList<>();

        stringArray.add(content.split("\r\n"));

        // Loop through all observed data events, ignoring everything but grouped events
            // For each event, fetch previous and next events on the observed graph
            // For each event, fetch previous n/2 events before the event's group's reference events, and
            // next n/2 events after the event's group's reference events where n is odd and > 0
            // Loop through collected reference events
                // Get previous and next events on the reference graph
                // Compare observed event (+ prev & next) to reference events (+ prev & next of each)

        int n = 5;

        for(ChipPinRelationship rel : groups.keySet())
        {
            int obsChip = rel.observedChipPin.x;
            int obsPin = rel.observedChipPin.y;
            int refChip = rel.referenceChipPin.x;
            int refPin = rel.referenceChipPin.y;

            C1Channel observedChannel = getCorrespondingChannel(c1Viewer.getObservedEventChannels(), obsChip, obsPin);
            if(observedChannel != null)
            {
                Vector<C1Event> observedC1Events = observedChannel.getEvents(Long.MIN_VALUE, Long.MAX_VALUE);
                Collections.sort(observedC1Events);
                for(C1Event observedEvent : observedC1Events)
                {
                    // Skip all events except those in groups
                    if(isEventInGroup(observedEvent, rel))
                    {
                        // Get the event's index and nearby events for the previous and next features
                        int idxObserved = observedC1Events.indexOf(observedEvent);
                        C1Event previousObservedEvent = (idxObserved != 0) ? observedC1Events.get(idxObserved - 1) : null;
                        C1Event nextObservedEvent = (idxObserved != observedC1Events.size() - 1) ? observedC1Events.get(idxObserved + 1) : null;
                        if(previousObservedEvent == null || nextObservedEvent == null)
                            continue;

                        // Here we need to fetch n/2 events before the first reference group event and n/2 events after
                        // the last reference group event and add it together into a sorted list including the
                        // reference group events.
                        C1Group eventGroup = getEventGroup(observedEvent, rel);
                        if(eventGroup != null)
                        {
                            Vector<C1Event> referenceGroupEvents = eventGroup.getReferenceEventsList();
                            Collections.sort(referenceGroupEvents);

                            long firstTime = referenceGroupEvents.get(0).getStartTime();
                            long lastTime = referenceGroupEvents.get(referenceGroupEvents.size() - 1).getEndTime();

                            C1Channel referenceChannel = getCorrespondingChannel(c1Viewer.getReferenceEventChannels(), refChip, refPin);
                            Vector<C1Event> allPreviousReferenceEvents = referenceChannel.getEvents(Long.MIN_VALUE, firstTime - 1);
                            Vector<C1Event> allNextReferenceEvents = referenceChannel.getEvents(lastTime + 1, Long.MAX_VALUE);
                            Collections.sort(allPreviousReferenceEvents);
                            Collections.sort(allNextReferenceEvents);

                            int num = n / 2;
                            Vector<C1Event> referenceEventsSubset = new Vector<>();
                            referenceEventsSubset.addAll(allPreviousReferenceEvents.subList(Math.max(allPreviousReferenceEvents.size() - num, 0), allPreviousReferenceEvents.size()));
                            referenceEventsSubset.addAll(referenceGroupEvents);
                            referenceEventsSubset.addAll(allNextReferenceEvents.subList(0, Math.min(num, allNextReferenceEvents.size())));
                            Collections.sort(referenceEventsSubset);

                            Vector<C1Event> referenceC1Events = new Vector<>();
                            referenceC1Events.addAll(allPreviousReferenceEvents);
                            referenceC1Events.addAll(referenceGroupEvents);
                            referenceC1Events.addAll(allNextReferenceEvents);
                            Collections.sort(referenceC1Events);

                            for(C1Event referenceEvent : referenceEventsSubset)
                            {
                                // Comparing observedEvent and referenceEvent, with the prev & next of observedEvent and prev & next of referenceEvent
                                int idxReference = referenceC1Events.indexOf(referenceEvent);
                                C1Event previousReferenceEvent = (idxReference != 0) ? referenceC1Events.get(idxReference - 1) : null;
                                C1Event nextReferenceEvent = (idxReference != referenceC1Events.size() - 1) ? referenceC1Events.get(idxReference + 1) : null;

                                if(previousReferenceEvent == null || nextReferenceEvent == null)
                                    continue;

                                // create data, probably use helper method
                                stringArray.add(generateFeatures(previousObservedEvent, observedEvent, nextObservedEvent, previousReferenceEvent, referenceEvent, nextReferenceEvent, rel));
                                stringArray.add(generateFeatures(previousReferenceEvent, referenceEvent, nextReferenceEvent, previousObservedEvent, observedEvent, nextObservedEvent, rel, true));
                            }
                        }
                    }
                }
            }
        }

        writer.writeAll(stringArray);
        writer.close();
    }

    private String[] generateFeatures(C1Event previousObservedEvent, C1Event observedEvent, C1Event nextObservedEvent,
                                      C1Event previousReferenceEvent, C1Event referenceEvent, C1Event nextReferenceEvent,
                                      ChipPinRelationship chipPinRel)
    {
        return generateFeatures(previousObservedEvent, observedEvent, nextObservedEvent, previousReferenceEvent, referenceEvent, nextReferenceEvent, chipPinRel, false);
    }

    @SuppressWarnings("DuplicatedCode")
    private String[] generateFeatures(C1Event previousObservedEvent, C1Event observedEvent, C1Event nextObservedEvent,
                                      C1Event previousReferenceEvent, C1Event referenceEvent, C1Event nextReferenceEvent,
                                      ChipPinRelationship chipPinRel, boolean swapAppliedOffset)
    {
        List<String> feat = new ArrayList<>();
        long tmp;
        double perc;

        // Explanation for this section: *Normally* the graph offset should be applied to the reference events if we
        // are in Generated Data mode. The swapAppliedOffset boolean allows me to swap the observed and reference
        // events to generate extra data where the order of features is swapped.
        int graphOffsetObs;
        int graphOffsetRef;
        if(!swapAppliedOffset)
        {
            graphOffsetObs = 0;
            graphOffsetRef = (c1Viewer.isCurrentReferenceMode(C1Viewer.ReferenceMode.SELF_REFERENCE)) ? 0 : dmp.getGraphOffset();
        }
        else
        {
            graphOffsetObs = (c1Viewer.isCurrentReferenceMode(C1Viewer.ReferenceMode.SELF_REFERENCE)) ? 0 : dmp.getGraphOffset();
            graphOffsetRef = 0;
        }

        long obsDuration = observedEvent.getDuration();
        long obsStart = observedEvent.getStartTime() + graphOffsetObs;
        long obsCenter = observedEvent.getHalfwayTime() + graphOffsetObs;
        long obsEnd = observedEvent.getEndTime() + graphOffsetObs;

        long prevObsDuration = previousObservedEvent.getDuration();
        long prevObsStart = previousObservedEvent.getStartTime() + graphOffsetObs;
        long prevObsCenter = previousObservedEvent.getHalfwayTime() + graphOffsetObs;
        long prevObsEnd = previousObservedEvent.getEndTime() + graphOffsetObs;

        long nextObsDuration = nextObservedEvent.getDuration();
        long nextObsStart = nextObservedEvent.getStartTime() + graphOffsetObs;
        long nextObsCenter = nextObservedEvent.getHalfwayTime() + graphOffsetObs;
        long nextObsEnd = nextObservedEvent.getEndTime() + graphOffsetObs;

        long refDuration = referenceEvent.getDuration();
        long refStart = referenceEvent.getStartTime() + graphOffsetRef;
        long refCenter = referenceEvent.getHalfwayTime() + graphOffsetRef;
        long refEnd = referenceEvent.getEndTime() + graphOffsetRef;

        long prevRefDuration = previousReferenceEvent.getDuration();
        long prevRefStart = previousReferenceEvent.getStartTime() + graphOffsetRef;
        long prevRefCenter = previousReferenceEvent.getHalfwayTime() + graphOffsetRef;
        long prevRefEnd = previousReferenceEvent.getEndTime() + graphOffsetRef;

        long nextRefDuration = nextReferenceEvent.getDuration();
        long nextRefStart = nextReferenceEvent.getStartTime() + graphOffsetRef;
        long nextRefCenter = nextReferenceEvent.getHalfwayTime() + graphOffsetRef;
        long nextRefEnd = nextReferenceEvent.getEndTime() + graphOffsetRef;

        // observed_event_duration
        feat.add(Long.toString(obsDuration));

        // observed_event_start
        //feat.add(Long.toString(obsStart));

        // observed_event_center
        //feat.add(Long.toString(obsCenter));

        // observed_event_end
        //feat.add(Long.toString(obsEnd));

        // observed_event_duration_relative_to_reference_duration
        tmp = obsDuration - refDuration;
        feat.add(Long.toString(tmp));

        // observed_event_start_relative_to_reference_start
        tmp = obsStart - refStart;
        feat.add(Long.toString(tmp));
        
        // observed_event_center_relative_to_reference_center
        tmp = obsCenter - refCenter;
        feat.add(Long.toString(tmp));
        
        // observed_event_end_relative_to_reference_end
        tmp = obsEnd - refEnd;
        feat.add(Long.toString(tmp));
        
        // reference_event_duration
        feat.add(Long.toString(refDuration));
        
        // reference_event_start
        //feat.add(Long.toString(refStart));
        
        // reference_event_center
        //feat.add(Long.toString(refCenter));
        
        // reference_event_end
        //feat.add(Long.toString(refEnd));

        // reference_event_duration_relative_to_observed_duration
        tmp = refDuration - obsDuration;
        feat.add(Long.toString(tmp));
        
        // reference_event_start_relative_to_reference_start
        tmp = refStart - obsStart;
        feat.add(Long.toString(tmp));
        
        // reference_event_center_relative_to_reference_center
        tmp = refCenter - obsCenter;
        feat.add(Long.toString(tmp));
        
        // reference_event_end_relative_to_reference_end
        tmp = referenceEvent.getEndTime() - observedEvent.getEndTime();
        feat.add(Long.toString(tmp));
        
        // perc_intersection_of_observed
//        if(!(refStart > obsEnd || obsStart > refEnd))
//            perc = (double) (Math.min(obsEnd, refEnd) - Math.max(obsStart, refStart)) / obsDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
        
        // perc_intersection_of_reference
//        if(!(refStart > obsEnd || obsStart > refEnd))
//            perc = (double) (Math.min(obsEnd, refEnd) - Math.max(obsStart, refStart)) / refDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
        
        // perc_intersection_of_union
        if(!(refStart > obsEnd || obsStart > refEnd))
            perc = (double) (Math.min(obsEnd, refEnd) - Math.max(obsStart, refStart)) / (Math.max(obsEnd, refEnd) - Math.min(obsStart, refStart));
        else
            perc = 0.0;

        feat.add(Double.toString(perc));
        
        // previous_observed_event_duration
        feat.add(Long.toString(prevObsDuration));
        
        // previous_observed_event_start
        //feat.add(Long.toString(prevObsStart));
        
        // previous_observed_event_center
        //feat.add(Long.toString(prevObsCenter));
        
        // previous_observed_event_end
        //feat.add(Long.toString(prevObsEnd));
        
        // previous_observed_event_start_relative_to_observed_start
        tmp = prevObsStart - obsStart;
        feat.add(Long.toString(tmp));
        
        // previous_observed_event_center_relative_to_observed_center
        tmp = prevObsCenter - obsCenter;
        feat.add(Long.toString(tmp));
        
        // previous_observed_event_end_relative_to_observed_end
        tmp = prevObsEnd - obsEnd;
        feat.add(Long.toString(tmp));

        // previous_observed_event_end_relative_to_observed_start
        tmp = prevObsEnd - obsStart;
        feat.add(Long.toString(tmp));

        // previous_observed_event_start_relative_to_reference_start
        tmp = prevObsStart - refStart;
        feat.add(Long.toString(tmp));

        // previous_observed_event_center_relative_to_reference_center
        tmp = prevObsCenter - refStart;
        feat.add(Long.toString(tmp));

        // previous_observed_event_end_relative_to_reference_end
        tmp = prevObsEnd - refEnd;
        feat.add(Long.toString(tmp));

        // previous_observed_event_end_relative_to_reference_start
        tmp = prevObsEnd - refStart;
        feat.add(Long.toString(tmp));

        // perc_intersection_of_previous_observed_with_reference
//        if(!(refStart > prevObsEnd || prevObsStart > refEnd))
//            perc = (double) (Math.min(prevObsEnd, refEnd) - Math.max(prevObsStart, refStart)) / prevObsDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));

//        // perc_intersection_of_reference_with_previous_observed
//        if(!(refStart > prevObsEnd || prevObsStart > refEnd))
//            perc = (double) (Math.min(prevObsEnd, refEnd) - Math.max(prevObsStart, refStart)) / refDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
        
        // perc_intersection_of_union_of_previous_observed_and_reference
        if(!(refStart > prevObsEnd || prevObsStart > refEnd))
            perc = (double) (Math.min(prevObsEnd, refEnd) - Math.max(prevObsStart, refStart)) / (Math.max(prevObsEnd, refEnd) - Math.min(prevObsStart, refStart));
        else
            perc = 0.0;

        feat.add(Double.toString(perc));
        
        // next_observed_event_duration
        feat.add(Long.toString(nextObsDuration));

        // next_observed_event_start
        //feat.add(Long.toString(nextObsStart));
        
        // next_observed_event_center
        //feat.add(Long.toString(nextObsCenter));
        
        // next_observed_event_end
        //feat.add(Long.toString(nextObsEnd));
        
        // next_observed_event_start_relative_to_observed_start
        tmp = nextObsStart - obsStart;
        feat.add(Long.toString(tmp));

        // next_observed_event_center_relative_to_observed_center
        tmp = nextObsCenter - obsCenter;
        feat.add(Long.toString(tmp));

        // next_observed_event_end_relative_to_observed_end
        tmp = nextObsEnd - obsEnd;
        feat.add(Long.toString(tmp));

        // next_observed_event_start_relative_to_observed_end
        tmp = nextObsStart - obsEnd;
        feat.add(Long.toString(tmp));

        // next_observed_event_start_relative_to_reference_start
        tmp = nextObsStart - refStart;
        feat.add(Long.toString(tmp));

        // next_observed_event_center_relative_to_reference_center
        tmp = nextObsCenter - refCenter;
        feat.add(Long.toString(tmp));

        // next_observed_event_end_relative_to_reference_end
        tmp = nextObsEnd - refEnd;
        feat.add(Long.toString(tmp));

        // next_observed_event_start_relative_to_reference_end
        tmp = nextObsStart - refEnd;
        feat.add(Long.toString(tmp));

//        // perc_intersection_of_next_observed_with_reference
//        if(!(refStart > nextObsEnd || nextObsStart > refEnd))
//            perc = (double) (Math.min(nextObsEnd, refEnd) - Math.max(nextObsStart, refStart)) / nextObsDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
//
//        // perc_intersection_of_reference_with_next_observed
//        if(!(refStart > nextObsEnd || nextObsStart > refEnd))
//            perc = (double) (Math.min(nextObsEnd, refEnd) - Math.max(nextObsStart, refStart)) / refDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
        
        // perc_intersection_of_union_of_next_observed_and_reference
        if(!(refStart > nextObsEnd || nextObsStart > refEnd))
            perc = (double) (Math.min(nextObsEnd, refEnd) - Math.max(nextObsStart, refStart)) / (Math.max(nextObsEnd, refEnd) - Math.min(nextObsStart, refStart));
        else
            perc = 0.0;

        feat.add(Double.toString(perc));
        
        // previous_reference_event_duration
        feat.add(Long.toString(prevRefDuration));

        // previous_reference_event_start
        //feat.add(Long.toString(prevRefStart));

        // previous_reference_event_center
        //feat.add(Long.toString(prevRefCenter));

        // previous_reference_event_end
        //feat.add(Long.toString(prevRefEnd));

        // previous_reference_event_start_relative_to_reference_start
        tmp = prevRefStart - refStart;
        feat.add(Long.toString(tmp));

        // previous_reference_event_center_relative_to_reference_center
        tmp = prevRefCenter - refCenter;
        feat.add(Long.toString(tmp));

        // previous_reference_event_end_relative_to_reference_end
        tmp = prevRefEnd - refEnd;
        feat.add(Long.toString(tmp));

        // previous_reference_event_end_relative_to_reference_start
        tmp = prevRefEnd - refStart;
        feat.add(Long.toString(tmp));

        // previous_reference_event_start_relative_to_observed_start
        tmp = prevRefStart - obsStart;
        feat.add(Long.toString(tmp));

        // previous_reference_event_center_relative_to_observed_center
        tmp = prevRefCenter - obsCenter;
        feat.add(Long.toString(tmp));

        // previous_reference_event_end_relative_to_observed_end
        tmp = prevRefEnd - obsEnd;
        feat.add(Long.toString(tmp));

        // previous_reference_event_end_relative_to_observed_start
        tmp = prevRefEnd - obsStart;
        feat.add(Long.toString(tmp));

//        // perc_intersection_of_previous_reference_with_observed
//        if(!(obsStart > prevRefEnd || prevRefStart > obsEnd))
//            perc = (double) (Math.min(prevRefEnd, obsEnd) - Math.max(prevRefStart, obsStart)) / prevRefDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
//
//        // perc_intersection_of_observed_with_previous_reference
//        if(!(obsStart > prevRefEnd || prevRefStart > obsEnd))
//            perc = (double) (Math.min(prevRefEnd, obsEnd) - Math.max(prevRefStart, obsStart)) / obsDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
        
        // perc_intersection_of_union_of_previous_reference_and_observed
        if(!(obsStart > prevRefEnd || prevRefStart > obsEnd))
            perc = (double) (Math.min(prevRefEnd, obsEnd) - Math.max(prevRefStart, obsStart)) / (Math.max(prevRefEnd, obsEnd) - Math.min(prevRefStart, obsStart));
        else
            perc = 0.0;

        feat.add(Double.toString(perc));
        
        // next_reference_event_duration
        feat.add(Long.toString(nextRefDuration));
        
        // next_reference_event_start
        //feat.add(Long.toString(nextRefStart));
        
        // next_reference_event_center
        //feat.add(Long.toString(nextRefCenter));

        // next_reference_event_end
        //feat.add(Long.toString(nextRefEnd));

        // next_reference_event_start_relative_to_reference_start
        tmp = nextRefStart - refStart;
        feat.add(Long.toString(tmp));

        // next_reference_event_center_relative_to_reference_center
        tmp = nextRefCenter - refCenter;
        feat.add(Long.toString(tmp));

        // next_reference_event_end_relative_to_reference_end
        tmp = nextRefEnd - refEnd;
        feat.add(Long.toString(tmp));

        // next_reference_event_start_relative_to_reference_end
        tmp = nextRefStart - refEnd;
        feat.add(Long.toString(tmp));

        // next_reference_event_start_relative_to_observed_start
        tmp = nextRefStart - obsStart;
        feat.add(Long.toString(tmp));

        // next_reference_event_center_relative_to_observed_center
        tmp = nextRefCenter - obsCenter;
        feat.add(Long.toString(tmp));

        // next_reference_event_end_relative_to_observed_end
        tmp = nextRefEnd - obsEnd;
        feat.add(Long.toString(tmp));

        // next_reference_event_start_relative_to_observed_end
        tmp = nextRefStart - obsEnd;
        feat.add(Long.toString(tmp));

//        // perc_intersection_of_next_reference_with_observed
//        if(!(obsStart > nextRefEnd || nextRefStart > obsEnd))
//            perc = (double) (Math.min(nextRefEnd, obsEnd) - Math.max(nextRefStart, obsStart)) / nextRefDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
//
//        // perc_intersection_of_observed_with_next_reference
//        if(!(obsStart > nextRefEnd || nextRefStart > obsEnd))
//            perc = (double) (Math.min(nextRefEnd, obsEnd) - Math.max(nextRefStart, obsStart)) / obsDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));

        // perc_intersection_of_union_of_next_reference_and_observed
        if(!(obsStart > nextRefEnd || nextRefStart > obsEnd))
            perc = (double) (Math.min(nextRefEnd, obsEnd) - Math.max(nextRefStart, obsStart)) / (Math.max(nextRefEnd, obsEnd) - Math.min(nextRefStart, obsStart));
        else
            perc = 0.0;

        feat.add(Double.toString(perc));

        C1Group observedGroup = getEventGroup(observedEvent, chipPinRel);
        C1Group referenceGroup = getEventGroup(referenceEvent, chipPinRel);
        if(observedGroup != null)
            feat.add((observedGroup.equals(referenceGroup)) ? "TRUE" : "FALSE");
        else
            feat.add("FALSE");

        return feat.toArray(new String[0]);
    }
}
