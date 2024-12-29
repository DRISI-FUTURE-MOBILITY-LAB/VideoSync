package VideoSync.objects.c1;

import java.io.Serializable;

public class C1Object implements Serializable
{
    /**
     * Used to keep track of the millisecond value for the event
     */
    private final long millis;

    /**
     * Used to keep track of the state
     */
    private final int state;

    /**
     * Used to keep track of the pin number
     */
    private final int pin;

    /**
     * Used to keep track of the chip number
     */
    private final int chip;

    // -- C1Object Constructor

    /**
     * Constructs the new Log C1 Events object
     *
     * @param time  - Time sent in seconds
     * @param state - The state of the event
     * @param pin   - The pin number of the event
     */
    public C1Object(long time, int state, int chip, int pin)
    {
        this.millis = time;
        this.state = state;
        this.chip = chip;
        this.pin = pin;
    }

    // -- C1Object Getters & Setters

    /**
     * Returns the milliseconds value of the event.
     *
     * @return millisecond value of the vent
     */
    public long getMilli()
    {
        return this.millis;
    }

    /**
     * Returns the state of the event.
     *
     * @return 0 or 1 representing the state of the event
     */
    public int getState()
    {
        return this.state;
    }

    /**
     * Returns the pin number of the event.
     *
     * @return pin number of the event
     */
    public int getPin()
    {
        return this.pin;
    }

    /**
     * Returns the chip number of the event.
     *
     * @return chip number of the event
     */
    public int getChip()
    {
        return this.chip;
    }

    @Override
    public String toString()
    {
        return String.format("Chip: %d Pin %d: -- Milli: %d -- State: %d", this.chip, this.pin, this.millis, this.state);
    }
}
