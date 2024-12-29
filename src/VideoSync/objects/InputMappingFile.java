package VideoSync.objects;

import java.io.*;
import java.util.Vector;

/**
 * <p>
 * Keeps track of device type and the channels used by device.
 * </p>
 * <p>
 * Created by Danny Hale on 5/23/17.
 * </p>
 */
public class InputMappingFile extends File
{
    // List of C1 or Log170 objects
    private Vector<DeviceInputMap> dim;
    // String name of the device being mapped
    private String device_name;

    /**
     * Loads an already existing mpf file
     *
     * @param file_path absolute file path to the mpf file
     */
    public InputMappingFile(String file_path)
    {
        super(file_path);
        file_path = file_path.substring(file_path.lastIndexOf('.'));

        if(file_path.equals(".mpf"))
        {
            try
            {
                FileInputStream fis = new FileInputStream(this);
                ModifiedObjectInputStream ois = new ModifiedObjectInputStream(fis);

                //Attempt to load device name. Older mapping files may not include this field.
                try
                {
                    device_name = (String) ois.readObject();
                }
                catch(OptionalDataException ex)
                {
                    System.out.println("Input mapping file is missing the device name. Attempting to determine from filename");

                    //If device name is not present, see if it is included in the file name.
                    if(getName().contains("C1"))
                    {
                        device_name = "C1";
                    }
                    else if(getName().contains("170"))
                    {
                        device_name = "170";
                    }
                    else
                    {
                        System.out.println("Unable to determine mapping file device name");
                        ex.printStackTrace();
                    }
                }

                int elements = ois.readInt();

                dim = new Vector<>();
                for(int i = 0; i < elements; i++)
                {
                    dim.add((DeviceInputMap) ois.readObject());
                    System.out.println(dim.elementAt(i));
                }

                ois.close();
                fis.close();
            }
            catch(ClassNotFoundException | IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("InputMappingFile tried to read a file with an .mpf extension.");
        }
    }

    /**
     * Creates a new mapping file at the absolute file path.
     *
     * @param file_path   absolute file path of where the mpf file will be created
     * @param device_name String description of the device being mapped
     */
    public InputMappingFile(String file_path, String device_name, Vector<DeviceInputMap> dim)
    {
        super(file_path);
        this.device_name = device_name;
        this.dim = dim;
    }

    public void writeFile()
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(this);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(device_name);

            oos.writeInt(dim.size());
            for(DeviceInputMap temp : dim)
            {
                oos.writeObject(temp);
                System.out.println(temp);
            }

            oos.close();
            fos.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Used to identify the device being mapped
     *
     * @return String name of the device being mapped
     */
    public String getDeviceName()
    {
        return device_name;
    }

    /**
     * Get the Vector with all the individual channels for the device.
     *
     * @return Vector<DeviceInputMap> Vectorized list of all the channels
     */
    public Vector<DeviceInputMap> getDeviceInputMapVector()
    {
        return dim;
    }

    public void setDeviceInputMap(DeviceInputMap dm, int index)
    {
        dim.setElementAt(dm, index);
    }

    /**
     * Checks to see if the device name matches the passed in string.
     *
     * @param str string sequence to be compared to
     * @return boolean
     */
    public boolean equals(String str)
    {
        return device_name.equals(str);
    }
}
