package VideoSync.objects;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ModifiedObjectInputStream extends ObjectInputStream
{
    public ModifiedObjectInputStream(InputStream in) throws IOException
    {
        super(in);
        super.enableResolveObject(true);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException
    {
        ObjectStreamClass read = super.readClassDescriptor();
        if (read.getName().startsWith("VideoSyncII.objects.")) {
            return ObjectStreamClass.lookup(Class.forName(read.getName().replace("VideoSyncII.objects.", "VideoSync.objects.")));
        }
        return read;
    }
}
