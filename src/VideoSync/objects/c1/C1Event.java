package VideoSync.objects.c1;

import java.io.Serializable;
import java.util.Objects;

public class C1Event implements Serializable, Comparable<C1Event>
{
    private final C1Object startObject;
    private final C1Object endObject;

    /**
     * Used to keep track of the chip number
     */
    private final int chip;

    /**
     * Used to keep track of the pin
     */
    private final int pin;

    public C1Event(int chip, int pin, C1Object start, C1Object end)
    {
        assert start.getState() == 1;
        assert end.getState() == 0;
        assert start.getMilli() > end.getMilli();
        assert start.getChip() == end.getChip();
        assert start.getPin() == end.getPin();

        startObject = start;
        endObject = end;

        this.pin = pin;
        this.chip = chip;
    }

    public long getStartTime()
    {
        return startObject.getMilli();
    }

    public long getHalfwayTime()
    {
        return startObject.getMilli() + (endObject.getMilli() - startObject.getMilli())/2;
    }

    public long getEndTime()
    {
        return endObject.getMilli();
    }

    public long getDuration()
    {
        return endObject.getMilli() - startObject.getMilli();
    }

    public int getChip()
    {
        return this.chip;
    }

    public int getPin()
    {
        return this.pin;
    }

    @Override
    public int hashCode()
    {
        // Prime numbers minimize hash code collisions
        int prime = 11;
        int result = 1;

        result = prime * result + Objects.hash(getStartTime());
        result = prime * result + Objects.hash(getEndTime());
        result = prime * result + Objects.hash(getChip());
        result = prime * result + Objects.hash(getPin());

        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        // Are we comparing an object to itself
        if(this == o)
            return true;

        if(o == null)
            return false;

        // Can't be equal if objects are of different classes
        if(getClass() != o.getClass())
            return false;

        C1Event evt = (C1Event) o;

        // Check if remaining defining properties are the same
        return Objects.equals(getStartTime(), evt.getStartTime())
                && Objects.equals(getEndTime(), evt.getEndTime())
                && Objects.equals(getChip(), evt.getChip())
                && Objects.equals(getPin(), evt.getPin());
    }

    @Override
    public int compareTo(C1Event o)
    {
        return (int) (this.getStartTime() - o.getStartTime());
    }
}
