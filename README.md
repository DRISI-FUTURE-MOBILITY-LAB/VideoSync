# VideoSync

# How to evaluate new sensors:

1. Data collection (onsite)

    Install C1 reader and DVR, leave them onsite for between a day and 2 weeks. After this data collection is finished bring them back to the lab. The laptop will have a .c1 file on it which will be used for the VideoSync project.

2. Collect DVR footage (in person)

    Setup the DVR in the lab and download the .dav files for the relevant timespan (1-3 hours of video).

3. Convert it [Video Guide](https://youtu.be/Lj_tpmGs7Ek)

    Use VideoSync to convert your collection of 30 minute .dav files to a merged .mp4 file that VideoSync can read.

4. Setup project

    Create a new folder with the name of the recorded footage and the date of the video. For example, "District 4 South Main Street Exit 7-6-22". In this folder place your merged.mp4 file and .c1 then open the folder in VideoSync.
    
    ![image](https://user-images.githubusercontent.com/39971693/184751352-c2343615-34c8-42db-9035-57fed41cac17.png)


5. Prepare input mapping (ask John and Jerry for diagram) [Video Guide](https://youtu.be/yb7O_hr6lQQ)

    Tell VideoSync how to associate channels (pins on the C1 connector) to specific lanes.

6. Setup video overlay

    Assign an overlay to each detector in VideoSync using either rectangles or free form regions where appropriate.
    
    ![image](https://user-images.githubusercontent.com/39971693/184766016-df0a5ed8-fd9b-49f6-9fb6-b97f59806986.png)

7. Find offset [Video Guide](https://youtu.be/R9qw9XulTac)

    Use lanes 1 and 2 and [VideoSync-Detector](https://github.com/julianofhernandez/VideoSync-Detector) to get a rough offset, this may be off by 0.1-0.5 seconds so adjust it manually with the video.

8. Label events and get car count [Video Guide](https://youtu.be/y-9YSPpMVsU)

    Go through each lane and label FP, FN, and TP events as needed using the video as ground truth evidence. Then go through each lane twice counting the cars on the upper and lower channels, then compare to ensure they are the same. [Here](https://github.com/julianofhernandez/VideoSync/blob/master/docs/sensitivity.md) are some examples to help with this process, use samples from the J drive and remember that practice makes perfect!

9. Generate report

    Ensure data looks good and export to .csv to incorporate into DRISI reports.
    ![image](https://user-images.githubusercontent.com/39971693/184764881-2fcc3944-f1ba-4cf6-81b6-286fc5863b85.png)

