package VideoSync.views.modals.event_logger.autoanalysis;

import VideoSync.models.DataModel;
import VideoSync.objects.c1.C1Channel;
import VideoSync.objects.c1.C1Event;
import VideoSync.objects.c1.C1Group;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import javax.swing.*;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class AutoAnalysisWorker extends SwingWorker<HashMap<Long, String>, Void>
{
    private final boolean additionalDetection;

    private final AutoAnalysis autoAnalysisWindow;
    private final DataModel dm;

    private final C1Channel referenceChannel;

    private final Vector<C1Event> observedEvents;
    private final Vector<C1Event> referenceEvents;

    private final HashMap<C1Event, C1Group> eventMap;
    private final Vector<C1Group> groups;

    private long startTime;
    private long endTime;

    HashMap<Long, String> results;

    public AutoAnalysisWorker(AutoAnalysis autoAnalysisWindow, DataModel dm, boolean additionalDetection, C1Channel observedChannel, C1Channel referenceChannel, long startTime, long endTime)
    {
        this.autoAnalysisWindow = autoAnalysisWindow;
        this.dm = dm;
        this.additionalDetection = additionalDetection;

        this.referenceChannel= referenceChannel;

        Vector<C1Event> observedEvents = observedChannel.getEvents(Long.MIN_VALUE, Long.MAX_VALUE);
        Vector<C1Event> referenceEvents = referenceChannel.getEvents(Long.MIN_VALUE, Long.MAX_VALUE);

        this.observedEvents = observedEvents;
        this.referenceEvents = referenceEvents;
        Collections.sort(observedEvents);
        Collections.sort(referenceEvents);

        this.startTime = startTime;
        this.endTime = endTime;

        this.eventMap = new HashMap<>();
        this.groups = new Vector<>();
    }

    @Override
    public HashMap<Long, String> doInBackground() throws Exception
    {
        int numProgressChunks = additionalDetection ? 5 : 4;

        setProgress(0);

        InputStream fi = getClass().getClassLoader().getResourceAsStream("auto-analysis-model-v2.model");
        RandomForest rf = (RandomForest) (new ObjectInputStream(fi)).readObject();

        setProgress(100/numProgressChunks);

        for(C1Event obs : observedEvents)
        {
            if(obs.getStartTime() - dm.getGraphOffset() > dm.getMaxVideoLength() || obs.getStartTime() - dm.getGraphOffset() < 0)
                continue;

            // Get the event's index and nearby events for the previous and next features
            int idxObserved = observedEvents.indexOf(obs);
            C1Event previousObservedEvent = (idxObserved != 0) ? observedEvents.get(idxObserved - 1) : null;
            C1Event nextObservedEvent = (idxObserved != observedEvents.size() - 1) ? observedEvents.get(idxObserved + 1) : null;
            if(previousObservedEvent == null || nextObservedEvent == null)
                continue;

            int graphOffset = autoAnalysisWindow.isCurrentReferenceMode(AutoAnalysis.ReferenceMode.GENERATED_DATA) ? dm.getGraphOffset() : 0;
            Vector<C1Event> referenceEventsSubset = referenceChannel.getEvents(obs.getStartTime() - graphOffset - 10000, obs.getEndTime() - graphOffset + 10000);
            for(C1Event ref : referenceEventsSubset)
            {
                // Comparing obs and ref, with the prev & next of obs and prev & next of ref
                int idxReference = referenceEvents.indexOf(ref);
                C1Event previousReferenceEvent = (idxReference != 0) ? referenceEvents.get(idxReference - 1) : null;
                C1Event nextReferenceEvent = (idxReference != referenceEvents.size() - 1) ? referenceEvents.get(idxReference + 1) : null;

                if(previousReferenceEvent == null || nextReferenceEvent == null)
                    continue;

                Instances instancesDataset = generateInstance(previousObservedEvent, obs, nextObservedEvent, previousReferenceEvent, ref, nextObservedEvent);
                Instance newInstance = instancesDataset.firstInstance();
                double pred = rf.classifyInstance(newInstance);
                String predString = instancesDataset.classAttribute().value((int) pred);

                if(predString.equals("TRUE"))
                {
                    // Check if either event are in a group yet
                    C1Group obsGroup = eventMap.get(obs);
                    C1Group refGroup = eventMap.get(ref);

                    if(obsGroup == null && refGroup == null)
                    {
                        // If no, create a new group and insert it
                        Vector<C1Event> obsTmp = new Vector<>();
                        obsTmp.add(obs);
                        Vector<C1Event> refTmp = new Vector<>();
                        refTmp.add(ref);
                        addGroup(obsTmp, refTmp);
                    }
                    else if(obsGroup == null)
                    {
                        // If one is in a group, add the one that isn't to the existing group and update the map
                        Vector<C1Event> obsTmp = refGroup.getObservedEventsList();
                        obsTmp.add(obs);
                        Vector<C1Event> refTmp = refGroup.getReferenceEventsList();

                        removeGroup(refGroup);

                        addGroup(obsTmp, refTmp);
                    }
                    else if(refGroup == null)
                    {
                        // If one is in a group, add the one that isn't to the existing group and update the map
                        Vector<C1Event> obsTmp = obsGroup.getObservedEventsList();
                        Vector<C1Event> refTmp = obsGroup.getReferenceEventsList();
                        obsTmp.add(ref);

                        removeGroup(obsGroup);

                        addGroup(obsTmp, refTmp);
                    }
                    else if(!obsGroup.equals(refGroup))
                    {
                        // If both are in different groups, merge the two groups into one group
                        Vector<C1Event> obsTmp1 = obsGroup.getObservedEventsList();
                        Vector<C1Event> refTmp1 = obsGroup.getReferenceEventsList();

                        Vector<C1Event> obsTmp2 = refGroup.getObservedEventsList();
                        Vector<C1Event> refTmp2 = refGroup.getReferenceEventsList();

                        obsTmp1.addAll(obsTmp2);
                        refTmp1.addAll(refTmp2);

                        removeGroup(obsGroup);
                        removeGroup(refGroup);

                        addGroup(obsTmp1, refTmp1);
                    }
                }
            }

            int progress = (int) ((((double) (observedEvents.indexOf(obs)+1) / (double)observedEvents.size()) * 100) * (1.0/numProgressChunks) + (100/numProgressChunks));
            setProgress(progress);
        }

        long minTime = observedEvents.firstElement().getStartTime() - dm.getGraphOffset();
        long maxTime = observedEvents.lastElement().getEndTime() - dm.getGraphOffset();

        HashMap<Long, String> discoveredEvents = new HashMap<>();

        // False Positives
        for(C1Event obs : observedEvents)
        {
            if(obs.getStartTime() - dm.getGraphOffset() > endTime
                    || obs.getStartTime() - dm.getGraphOffset() < startTime
                    || obs.getStartTime() - dm.getGraphOffset() > maxTime
                    || obs.getStartTime() - dm.getGraphOffset() < minTime)
                continue;

            if(observedEvents.firstElement().equals(obs)
                    || observedEvents.lastElement().equals(obs)
                    || obs.getDuration() < 100)
                continue;

            if(!eventMap.containsKey(obs))
            {
                discoveredEvents.put((obs.getStartTime() - dm.getGraphOffset()), "False Positive");
                System.out.println("FALSE POSITIVE: " + (obs.getStartTime() - dm.getGraphOffset()));
            }

            int progress = (int) ((((double) (observedEvents.indexOf(obs)+1) / (double)observedEvents.size()) * 100) * (1.0/numProgressChunks) + ((100/numProgressChunks)*2));
            setProgress(progress);
        }

        // False Negatives
        for(C1Event ref : referenceEvents)
        {
            int graphOffset = autoAnalysisWindow.isCurrentReferenceMode(AutoAnalysis.ReferenceMode.GENERATED_DATA) ? 0 : dm.getGraphOffset();

            if(ref.getStartTime() - graphOffset > endTime
                    || ref.getStartTime() - graphOffset < startTime
                    || ref.getStartTime() - graphOffset > maxTime
                    || ref.getStartTime() - graphOffset < minTime)
                continue;

            if(referenceEvents.firstElement().equals(ref) || referenceEvents.lastElement().equals(ref) || ref.getDuration() < 100)
                continue;

            if(!eventMap.containsKey(ref))
            {
                discoveredEvents.put(ref.getStartTime() - graphOffset, "False Negative");
                System.out.println("FALSE NEGATIVE: " + (ref.getStartTime() - graphOffset));
            }

            int progress = (int) ((((double) (referenceEvents.indexOf(ref)+1) / (double)referenceEvents.size()) * 100) * (1.0/numProgressChunks) + ((100/numProgressChunks)*3));
            setProgress(progress);
        }

        // Dropped Calls
        if(additionalDetection)
        {
            for(C1Group g : groups)
            {
                int graphOffset = autoAnalysisWindow.isCurrentReferenceMode(AutoAnalysis.ReferenceMode.GENERATED_DATA) ? 0 : dm.getGraphOffset();

                if(g.getObservedEventsAvgTime() - dm.getGraphOffset() > endTime
                        || g.getObservedEventsAvgTime() - dm.getGraphOffset() < startTime
                        || g.getReferenceEventsAvgTime() - graphOffset > endTime
                        || g.getReferenceEventsAvgTime() - graphOffset < startTime
                        || g.getObservedEventsAvgTime() - dm.getGraphOffset() > maxTime
                        || g.getObservedEventsAvgTime() - dm.getGraphOffset() < minTime
                        || g.getReferenceEventsAvgTime() - graphOffset > maxTime
                        || g.getReferenceEventsAvgTime() - graphOffset < minTime)
                    continue;

                Vector<C1Event> obsEvents = g.getObservedEventsList();
                Vector<C1Event> refEvents = g.getReferenceEventsList();

                int obsDuration = 0;
                for(C1Event evt : obsEvents)
                {
                    obsDuration += evt.getDuration();
                }

                int refDuration = 0;
                for(C1Event evt : refEvents)
                {
                    refDuration += evt.getDuration();
                }

                double percRelDur = (double) obsDuration / (double) refDuration;
                if(percRelDur < 0.5 && (refDuration - obsDuration) > 1000)
                {
                    discoveredEvents.put((obsEvents.firstElement().getStartTime() - dm.getGraphOffset()), "Partial Detection");
                    System.out.println("DROPPED CALL/PARTIAL DETECTION: " + (obsEvents.firstElement().getStartTime() - dm.getGraphOffset()));
                }

                int progress = (int) ((((double) (groups.indexOf(g)+1) / (double)groups.size()) * 100) * (1.0/numProgressChunks) + ((100/numProgressChunks)*4));
                setProgress(progress);
            }
        }

        setProgress(100);
        return discoveredEvents;
    }

    private void addGroup(Vector<C1Event> observedList, Vector<C1Event> referenceList)
    {
        C1Group newGroup = new C1Group(observedList, referenceList, 0, 0, 0, 0);
        groups.add(newGroup);

        for(C1Event evt : observedList)
            eventMap.put(evt, newGroup);

        for(C1Event evt: referenceList)
            eventMap.put(evt, newGroup);

        Collections.sort(groups);
    }

    private void removeGroup(C1Group g)
    {
        groups.remove(g);
        Collections.sort(groups);

        for(C1Event evt : g.getObservedEventsList())
        {
            eventMap.remove(evt);
        }

        for(C1Event evt : g.getReferenceEventsList())
        {
            eventMap.remove(evt);
        }
    }

    private Instances generateInstance(C1Event previousObservedEvent, C1Event observedEvent, C1Event nextObservedEvent,
                                      C1Event previousReferenceEvent, C1Event referenceEvent, C1Event nextReferenceEvent)
    {
        ArrayList<Attribute> atts;
        ArrayList<String> attVals;
        Instances data;
        double[] vals;

        // 1. set up attributes
        atts = new ArrayList<>();
        // - numeric
        atts.add(new Attribute("observed_event_duration"));
        atts.add(new Attribute("observed_event_duration_relative_to_reference_duration"));
        atts.add(new Attribute("observed_event_start_relative_to_reference_start"));
        atts.add(new Attribute("observed_event_center_relative_to_reference_center"));
        atts.add(new Attribute("observed_event_end_relative_to_reference_end"));
        atts.add(new Attribute("reference_event_duration"));
        atts.add(new Attribute("reference_event_duration_relative_to_observed_duration"));
        atts.add(new Attribute("reference_event_start_relative_to_observed_start"));
        atts.add(new Attribute("reference_event_center_relative_to_observed_center"));
        atts.add(new Attribute("reference_event_end_relative_to_observed_end"));
//        atts.add(new Attribute("perc_intersection_of_observed"));
//        atts.add(new Attribute("perc_intersection_of_reference"));
        atts.add(new Attribute("perc_intersection_of_union"));
        atts.add(new Attribute("previous_observed_event_duration"));
        atts.add(new Attribute("previous_observed_event_start_relative_to_observed_start"));
        atts.add(new Attribute("previous_observed_event_center_relative_to_observed_center"));
        atts.add(new Attribute("previous_observed_event_end_relative_to_observed_end"));
        atts.add(new Attribute("previous_observed_event_end_relative_to_observed_start"));
        atts.add(new Attribute("previous_observed_event_start_relative_to_reference_start"));
        atts.add(new Attribute("previous_observed_event_center_relative_to_reference_center"));
        atts.add(new Attribute("previous_observed_event_end_relative_to_reference_end"));
        atts.add(new Attribute("previous_observed_event_end_relative_to_reference_start"));
//        atts.add(new Attribute("perc_intersection_of_previous_observed_with_reference"));
//        atts.add(new Attribute("perc_intersection_of_reference_with_previous_observed"));
        atts.add(new Attribute("perc_intersection_of_union_of_previous_observed_and_reference"));
        atts.add(new Attribute("next_observed_event_duration"));
        atts.add(new Attribute("next_observed_event_start_relative_to_observed_start"));
        atts.add(new Attribute("next_observed_event_center_relative_to_observed_center"));
        atts.add(new Attribute("next_observed_event_end_relative_to_observed_end"));
        atts.add(new Attribute("next_observed_event_start_relative_to_observed_end"));
        atts.add(new Attribute("next_observed_event_start_relative_to_reference_start"));
        atts.add(new Attribute("next_observed_event_center_relative_to_reference_center"));
        atts.add(new Attribute("next_observed_event_end_relative_to_reference_end"));
        atts.add(new Attribute("next_observed_event_start_relative_to_reference_end"));
//        atts.add(new Attribute("perc_intersection_of_next_observed_with_reference"));
//        atts.add(new Attribute("perc_intersection_of_reference_with_next_observed"));
        atts.add(new Attribute("perc_intersection_of_union_of_next_observed_and_reference"));
        atts.add(new Attribute("previous_reference_event_duration"));
        atts.add(new Attribute("previous_reference_event_start_relative_to_reference_start"));
        atts.add(new Attribute("previous_reference_event_center_relative_to_reference_center"));
        atts.add(new Attribute("previous_reference_event_end_relative_to_reference_end"));
        atts.add(new Attribute("previous_reference_event_end_relative_to_reference_start"));
        atts.add(new Attribute("previous_reference_event_start_relative_to_observed_start"));
        atts.add(new Attribute("previous_reference_event_center_relative_to_observed_center"));
        atts.add(new Attribute("previous_reference_event_end_relative_to_observed_end"));
        atts.add(new Attribute("previous_reference_event_end_relative_to_observed_start"));
//        atts.add(new Attribute("perc_intersection_of_previous_reference_with_observed"));
//        atts.add(new Attribute("perc_intersection_of_observed_with_previous_reference"));
        atts.add(new Attribute("perc_intersection_of_union_of_previous_reference_and_observed"));
        atts.add(new Attribute("next_reference_event_duration"));
        atts.add(new Attribute("next_reference_event_start_relative_to_reference_start"));
        atts.add(new Attribute("next_reference_event_center_relative_to_reference_center"));
        atts.add(new Attribute("next_reference_event_end_relative_to_reference_end"));
        atts.add(new Attribute("next_reference_event_start_relative_to_reference_end"));
        atts.add(new Attribute("next_reference_event_start_relative_to_observed_start"));
        atts.add(new Attribute("next_reference_event_center_relative_to_observed_center"));
        atts.add(new Attribute("next_reference_event_end_relative_to_observed_end"));
        atts.add(new Attribute("next_reference_event_start_relative_to_observed_end"));
//        atts.add(new Attribute("perc_intersection_of_next_reference_with_observed"));
//        atts.add(new Attribute("perc_intersection_of_observed_with_next_reference"));
        atts.add(new Attribute("perc_intersection_of_union_of_next_reference_and_observed"));

        attVals = new ArrayList<>();
        attVals.add("FALSE");
        attVals.add("TRUE");
        atts.add(new Attribute("match", attVals));

        // 2. create Instances object
        data = new Instances("MyRelation", atts, 0);

        List<String> feat = new ArrayList<>();
        long tmp;
        double perc;

        int graphOffset = (autoAnalysisWindow.isCurrentReferenceMode(AutoAnalysis.ReferenceMode.SELF_REFERENCE)) ? 0 : dm.getGraphOffset();

        long obsDuration = observedEvent.getDuration();
        long obsStart = observedEvent.getStartTime();
        long obsCenter = observedEvent.getHalfwayTime();
        long obsEnd = observedEvent.getEndTime();

        long prevObsDuration = previousObservedEvent.getDuration();
        long prevObsStart = previousObservedEvent.getStartTime();
        long prevObsCenter = previousObservedEvent.getHalfwayTime();
        long prevObsEnd = previousObservedEvent.getEndTime();

        long nextObsDuration = nextObservedEvent.getDuration();
        long nextObsStart = nextObservedEvent.getStartTime();
        long nextObsCenter = nextObservedEvent.getHalfwayTime();
        long nextObsEnd = nextObservedEvent.getEndTime();

        long refDuration = referenceEvent.getDuration();
        long refStart = referenceEvent.getStartTime() + graphOffset;
        long refCenter = referenceEvent.getHalfwayTime() + graphOffset;
        long refEnd = referenceEvent.getEndTime() + graphOffset;

        long prevRefDuration = previousReferenceEvent.getDuration();
        long prevRefStart = previousReferenceEvent.getStartTime() + graphOffset;
        long prevRefCenter = previousReferenceEvent.getHalfwayTime() + graphOffset;
        long prevRefEnd = previousReferenceEvent.getEndTime() + graphOffset;

        long nextRefDuration = nextReferenceEvent.getDuration();
        long nextRefStart = nextReferenceEvent.getStartTime() + graphOffset;
        long nextRefCenter = nextReferenceEvent.getHalfwayTime() + graphOffset;
        long nextRefEnd = nextReferenceEvent.getEndTime() + graphOffset;

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

//        // perc_intersection_of_observed
//        if(!(refStart > obsEnd || obsStart > refEnd))
//            perc = (double) (Math.min(obsEnd, refEnd) - Math.max(obsStart, refStart)) / obsDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
//
//        // perc_intersection_of_reference
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

//        // perc_intersection_of_previous_observed_with_reference
//        if(!(refStart > prevObsEnd || prevObsStart > refEnd))
//            perc = (double) (Math.min(prevObsEnd, refEnd) - Math.max(prevObsStart, refStart)) / prevObsDuration;
//        else
//            perc = 0.0;
//
//        feat.add(Double.toString(perc));
//
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

        // 3. fill with data
        vals = new double[data.numAttributes()];
        // - numeric
        for(String s : feat)
        {
            int idx = feat.indexOf(s);
            boolean isLong;

            try
            {
                vals[idx] = Long.parseLong(s);
                isLong = true;
            }
            catch(NumberFormatException e)
            {
                isLong = false;
            }

            if(!isLong)
            {
                vals[idx] = Double.parseDouble(s);
            }
        }

        // add
        data.add(new DenseInstance(1.0, vals));
        data.setClassIndex(data.numAttributes() - 1);

        return data;
    }

    @Override
    public void done()
    {
        try
        {
            results = get();
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        catch(ExecutionException e)
        {
            e.printStackTrace();
        }
    }

    public HashMap<Long, String> getResults()
    {
        return (HashMap<Long, String>) results.clone();
    }
}
