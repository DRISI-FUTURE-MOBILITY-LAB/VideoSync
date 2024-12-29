package VideoSync.objects;

import java.io.Serializable;
import java.util.Objects;

public class Pair<X, Y> implements Serializable
{
    public final X x;
    public final Y y;

    public Pair(X x, Y y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if(!(o instanceof Pair)) return false;

        Pair pair = (Pair) o;
        return Objects.equals(x, pair.x) && Objects.equals(y, pair.y);
    }

    @Override
    public int hashCode()
    {
        int result = x.hashCode();
        result = 31 * result + y.hashCode();
        return result;
    }
}
