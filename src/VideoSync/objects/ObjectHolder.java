/*
 * ****************************************************************
 * File: 			ObjectHolder.java
 * Date Created:  	June 27, 2017
 * Programmer:		Elliot Hawkins
 *
 * Purpose:			To provide a light way to store a single object
 * reference, which can be used to share values
 * between a function and an inner anonymous class.
 *
 * ****************************************************************
 */
package VideoSync.objects;

public class ObjectHolder<T>
{
    private T heldObject;

    public ObjectHolder()
    {
        setValue(null);
    }

    public ObjectHolder(T object)
    {
        setValue(object);
    }

    public void setValue(T object)
    {
        heldObject = object;
    }

    public T getValue()
    {
        return heldObject;
    }
}
