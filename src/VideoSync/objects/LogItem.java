/*
 * ****************************************************************
 * File: 			LogItem.java
 * Date Created:  	August 5, 2013
 * Programmer:		Dale Reed
 *
 * Purpose:			Used for easily passing objects to the Log
 * 					File manager so that it can be written to the
 * 					appropriate log file.
 *
 * ****************************************************************
 */
package VideoSync.objects;

public class LogItem
{
    /**
     * The class name to be printed.
     */
    private final String className;

    /**
     * The date and time of the log element
     */
    private final String datetime;

    /**
     * The message to be written to the log file
     */
    private final String message;

    /**
     * The type of message to be written.
     */
    private final String type;

    /**
     * Constructs a new LogItem to keep track of the individual log files.
     *
     * @param cn - The Class Name
     * @param dt - The TimeStamp of the log element
     * @param mg - The message to be logged
     * @param ty - The type of message
     */
    public LogItem(String cn, String dt, String mg, String ty)
    {
        this.className = cn;
        this.datetime = dt;
        this.message = mg;
        this.type = ty;
    }

    /**
     * Return the class name that was sent to the Log Item.
     *
     * @return class name as a String
     */
    public String getClassName()
    {
        return this.className;
    }

    /**
     * Returns a string with the format of the message to be written to the log file
     */
    public String toString()
    {
        return String.format("%s [%s] - %s", datetime, type, message);
    }
}