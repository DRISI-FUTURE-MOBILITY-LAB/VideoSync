package VideoSync.objects.c1;

import java.io.Serializable;
import java.util.Vector;

public class C1Group implements Comparable<C1Group>, Serializable
{
    private final Vector<C1Event> observedEvents;

    private final Vector<C1Event> referenceEvents;

    private final int obsChip;

    private final int obsPin;

    private final int refChip;

    private final int refPin;

    private final boolean auto;


    public C1Group(Vector<C1Event> observedEvents, Vector<C1Event> referenceEvents, int obsChip, int obsPin, int refChip, int refPin)
    {
        this.observedEvents = new Vector<>(observedEvents);
        this.referenceEvents = new Vector<>(referenceEvents);
        this.obsChip = obsChip;
        this.obsPin = obsPin;
        this.refChip = refChip;
        this.refPin = refPin;
        this.auto = false;
    }

    public C1Group(Vector<C1Event> observedEvents, Vector<C1Event> referenceEvents, int obsChip, int obsPin, int refChip, int refPin, boolean auto)
    {
        this.observedEvents = new Vector<>(observedEvents);
        this.referenceEvents = new Vector<>(referenceEvents);
        this.obsChip = obsChip;
        this.obsPin = obsPin;
        this.refChip = refChip;
        this.refPin = refPin;
        this.auto = auto;
    }

    public int getObservedEventsAvgTime()
    {
        int sum = 0;
        for(C1Event e : observedEvents)
        {
            sum += e.getHalfwayTime();
        }

        return sum / observedEvents.size();
    }

    public int getReferenceEventsAvgTime()
    {
        int sum = 0;

        for(C1Event e: referenceEvents)
        {
            sum += e.getHalfwayTime();
        }

        return sum / referenceEvents.size();
    }

    @Override
    public int compareTo(C1Group o)
    {
        return this.getObservedEventsAvgTime() - o.getObservedEventsAvgTime();
    }

    public boolean contains(C1Event e)
    {
        return observedEvents.contains(e) || referenceEvents.contains(e);
    }

    @SuppressWarnings("unchecked")
    public Vector<C1Event> getObservedEventsList()
    {
        return (Vector<C1Event>) observedEvents.clone();
    }

    @SuppressWarnings("unchecked")
    public Vector<C1Event> getReferenceEventsList()
    {
        return (Vector<C1Event>) referenceEvents.clone();
    }

    public int getObsChip()
    {
        return obsChip;
    }

    public int getObsPin()
    {
        return obsPin;
    }

    public int getRefChip()
    {
        return refChip;
    }

    public int getRefPin()
    {
        return refPin;
    }

    public boolean isAuto()
    {
        return auto;
    }
}
