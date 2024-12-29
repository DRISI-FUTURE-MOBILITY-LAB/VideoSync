/*
Programmer: Jenzel Arevalo
Date: 8 Aug 2018
Program Description: Temporary program solution to parse c1 Files and generate a new c1 File with trimmed data.
                     Credits to Toua Lee for algorithm design.
 */

import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;

public class c1FileParser
{
    static int totalDay, day, hours_C1, minutes_C1, seconds_C1, firstEvent_timeStamp;
    static int hours_V, minutes_V, seconds_V, hours_VL, minutes_VL, seconds_VL;
    static int ms_C1, ms_V, ms_VL, ms_begin, ms_end;
    //static final int HR_IN_MS = 60 * 60 * 1000;   //Jenzel - Investigate, constant contributes to miscalculation of C1 segment
    //static final int MIN_IN_MS = 60 * 1000;       //Jenzel - Investigate, constant contributes to miscalculation of C1 segment
    //static final int MS = 1000;                   //Jenzel - Investigate, constant contributes to miscalculation of C1 segment

    public static void main(String[] args)
    {
        Scanner kb = new Scanner(System.in);
        ArrayList<String> c1Data = new ArrayList<String>();
        ArrayList<String> trimmedC1Data;

        //Read c1File and input c1 data to c1Data
        readC1File(c1Data, kb);

        //Obtain amount of days from user
        getTotalDays(kb);

        //Obtain Initial C1 Time Stamp from user
        getInitialC1TimeStamp(kb);

        //Obtain Video Length from user
        getVideoLength(kb);

        do {
            //Obtain Video Time Stamp from user
            getVideoTimeStamp(kb);

            //Get day in which video segment takes place
            getDay(kb);

            //Calculate Segment with information obtained from user
            calculate();

            //Based on information obtained by user, add the c1Data to trimmedC1Data based on information obtained from user
            trimmedC1Data = trimC1File(c1Data);

            //Prints first element of trimmed C1 data
            System.out.println("First element in trimmed C1 data list: " + trimmedC1Data.get(0));

            //Prints last element of trimmed C1 data
            System.out.println("Last element in trimmed C1 data list: " + trimmedC1Data.get(trimmedC1Data.size()-1));

            //Create new C1 file new trimmed C1 data
            generateC1File(trimmedC1Data, kb);
        }while(!quit(kb));
    }

    /* Reads in a c1 file and stores it to an array of Strings*/
    public static void readC1File(ArrayList<String> c1Data, Scanner kb){
        boolean found = false;
        System.out.println("\n\n");
        do{
            try{
                //Ask user for file name
                System.out.print("Please enter a c1 file name: ");
                String fileName = kb.nextLine();
                File file = new File(fileName);
                String filePath = file.getAbsolutePath();
                BufferedReader inputFile = new BufferedReader(new FileReader(filePath));
                String line;
                while ((line = inputFile.readLine()) != null){
                    c1Data.add(line);
                }
                found = true;
                inputFile.close();
            }
            catch(IOException e)
            {
                System.out.println("\n");
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            System.out.println("\n");
        }while(!found);
    }

    /* Asks the user for total amount of days */
    public static void getTotalDays(Scanner kb){
        System.out.print("How many days of data do you have: ");
        String input = kb.nextLine();
        System.out.println("");
        if(input.matches("[0-9]+"))
            totalDay = Integer.parseInt(input);
        else {
            do{
                System.out.print("Invalid input. How many days of data do you have: ");
                input = kb.nextLine();
            }while(!input.matches("[0-9]+"));
            totalDay = Integer.parseInt(input);
        }
    }

    /* Gets Initial C1 Time Stamp*/
    public static void getInitialC1TimeStamp(Scanner kb)
    {
        String input = "";
        System.out.println("Enter initial C1 Time Stamp in military time [24:00:00]\n");
        do{
            System.out.print("Enter hour [0-23]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        hours_C1 = Integer.parseInt(input);

        do {
            System.out.print("Enter minutes [0-59]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        minutes_C1 = Integer.parseInt(input);

        do {
            System.out.print("Enter seconds [0-59]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        seconds_C1 = Integer.parseInt(input);
        System.out.println("You've entered " + hours_C1 + ":" + minutes_C1 + ":" + seconds_C1);
        System.out.println("");

        do {
            System.out.print("What is the first C1 event Time Stamp (ms): ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        firstEvent_timeStamp = Integer.parseInt(input);
        System.out.println("");
    }

    /* Gets Video Time Stamp */
    public static void getVideoTimeStamp(Scanner kb){
        String input = "";
        System.out.println("Enter Video Time Stamp in military time [23:59:59]\n");
        do{
            System.out.print("Enter hour [0-23]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        hours_V = Integer.parseInt(input);

        do {
            System.out.print("Enter minutes [0-59]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        minutes_V = Integer.parseInt(input);

        do {
            System.out.print("Enter seconds [0-59]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        seconds_V = Integer.parseInt(input);
        System.out.println("You've entered " + hours_V + ":" + minutes_V + ":" + seconds_V);
        System.out.println("");
    }

    /* Gets Video Length */
    public static void getVideoLength(Scanner kb)
    {
        String input = "";
        System.out.println("Enter length of video in military time [23:59:59]\n");
        do{
            System.out.print("Enter hour [0-23]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        hours_VL = Integer.parseInt(input);

        do {
            System.out.print("Enter minutes [0-59]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        minutes_VL = Integer.parseInt(input);

        do {
            System.out.print("Enter seconds [0-59]: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        seconds_VL = Integer.parseInt(input);
        System.out.println("You've entered " + hours_VL + ":" + minutes_VL + ":" + seconds_VL);
        System.out.println("");
    }

    /* Get day */
    public static void getDay(Scanner kb)
    {
        String input = "";
        System.out.println("There are " + totalDay + " days worth of videos. Which day does this video segment take place?");
        do {
            System.out.print("Enter day: ");
            input = kb.nextLine();
        }while(!input.matches("[0-9]+"));
        day = Integer.parseInt(input);
        System.out.println("");
    }

    /* Calculates new C1 Segment */
    public static void calculate()
    {
        ms_C1 = (hours_C1 * 60 * 60 * 1000) + (minutes_C1 * 60 * 1000) + (seconds_C1 * 1000);
        ms_V = (hours_V * 60 * 60 * 1000) + (minutes_V * 60 * 1000) + (seconds_V * 1000);
        ms_VL = (hours_VL * 60 * 60 * 1000) + (minutes_VL * 60 * 1000) + (seconds_VL * 1000);

        ms_V = ms_V + ((24 * 60 * 60 * 1000) * (day - 1));

        ms_begin = ms_V - ms_C1 + firstEvent_timeStamp;
        ms_end = ms_begin + ms_VL;

        System.out.println("Segment start: " + ms_begin);
        System.out.println("Segment end: " + ms_end);
    }

    /* Trims the c1Data based on calculations*/
    public static ArrayList<String> trimC1File(ArrayList<String> c1Data) {
        ArrayList<String> temp = new ArrayList<String>();
        String line = "";
        int minuteBuffer = ms_end + (60 * 1000); //adds 1 minute buffer to end
        System.out.println("Minute buffer: " + minuteBuffer);
        for(int i = 1; i < c1Data.size(); i++) {
            line = c1Data.get(i);
            String lineMsVal = line.substring(line.lastIndexOf(" ") + 1);
            int currentMsVal = Integer.parseInt(lineMsVal);
            if(currentMsVal >= ms_begin && currentMsVal <= minuteBuffer)
                temp.add(line);
        }
        return temp;
    }

    /* Writes the data from the trimmed c1 data array to a new c1 file */
    public static void generateC1File(ArrayList<String> trimmedC1Data, Scanner kb)
    {
        String answer = "";
        do {
            //Ask user if they want to save the trimmed c1 File
            System.out.print("Save file? ['Y' for Yes | 'N' for No]: ");
            answer = kb.nextLine();
            System.out.println("");
        }while(!answer.matches("[YyNn]"));

        //Exit Program if answer is no
        if(answer.equalsIgnoreCase("Y"))
        {
            //Ask user for a file name
            String newFileName;
            System.out.print("Enter a file name: ");
            newFileName = kb.nextLine();
            newFileName = newFileName.concat(".c1");

            try {
                File writeFile = new File(newFileName);
                FileWriter fw = new FileWriter(writeFile);
                BufferedWriter bw = new BufferedWriter(fw);
                for (int i = 0; i < trimmedC1Data.size(); i++) {
                    bw.write(trimmedC1Data.get(i));
                    bw.newLine();
                }
                bw.close();
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("\n");
                e.printStackTrace();
            }
        }
    }

    /* Checks if user wants to quit Program */
    public static boolean quit(Scanner kb){
        String quit;
        do {
            System.out.println("Quit program? ['Y' for yes | 'N' for no]: ");
            quit = kb.nextLine();
            if (quit.equalsIgnoreCase("Y"))
                return true;
            else if(quit.equalsIgnoreCase("N"))
                break;
            else
                System.out.println("");
        }while(!quit.equalsIgnoreCase("Y") || !quit.equalsIgnoreCase("N"));
        return false;
    }
}
