package VideoSync.objects.c1;

import java.awt.*;

public class C1GroupIdentifier
{
    public final Point pos;
    public final String name;

    public C1GroupIdentifier(int x, int y, String name)
    {
        pos = new Point(x, y);
        this.name = name;
    }
}
