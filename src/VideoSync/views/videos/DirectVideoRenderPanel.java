/*
 * ****************************************************************
 * File: 			DirectVideoRenderPanel.java
 * Date Created:  	January 11, 2017
 * Programmer:		Elliot Hawkins
 *
 * Purpose:			To handle drawing on top of and rendering
 *                  video frames.
 * ****************************************************************
 */
package VideoSync.views.videos;

import VideoSync.models.DataModelProxy;
import VideoSync.objects.graphs.FixedRegion;
import VideoSync.objects.graphs.FreeFormRegion;
import VideoSync.objects.graphs.Line;
import VideoSync.objects.graphs.Region;
import VideoSync.views.menus.RegionContextMenu;
import VideoSync.views.tabbed_panels.graphs.GraphOptions;
import VideoSync.views.tabbed_panels.graphs.GraphPanel;
import VideoSync.views.videos.commands.*;
import uk.co.caprica.vlcj.player.MediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import static java.awt.geom.PathIterator.SEG_CLOSE;


public class DirectVideoRenderPanel extends JPanel implements MouseListener, MouseMotionListener, ActionListener
{
    //Used to keep track of which part of a region is under the mouse for resizing and moving.
    private enum RegionGrabLocation
    {
        NONE,
        CENTER,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
        UPPER_LEFT,
        UPPER_RIGHT,
        LOWER_LEFT,
        LOWER_RIGHT,
        VERTEX,
        LINE
    }

    private static final int NO_VIDEO_BACKGROUND_WIDTH = 400;
    private static final int NO_VIDEO_BACKGROUND_HEIGHT = 200;
    private static final int NO_VIDEO_FONT_SIZE = 48;
    private static final int NO_VIDEO_TEXT_OFFSET = 180;
    private static final String NO_VIDEO_MESSAGE = "End of Video!";
    private int regionBorderWidth = 2;
    private static final int REGION_FILLED_ALPHA = 148;

    /**
     * Framerate to repaint the panel at
     */
    private static final int TIMER_FRAMERATE = 60;

    /**
     * Framerate converted into a millisecond duration for the timer.
     */
    private static final int TIMER_MS = 1000 / TIMER_FRAMERATE;

    /**
     * The buffer containing the current video frame to render.
     */
    private BufferedImage videoFrame;

    /**
     * The buffer used to store a copy of the current frame to draw on.
     * This way, if the frame is updated mid-draw we don't get tearing and flicker.
     */
    private BufferedImage renderCopy;

    /**
     * Media player generating frames for this panel. Referenced so we can check the current playback time for the no video message.
     */
    private MediaPlayer mediaPlayer;

    /**
     * Parameters for stretching video frame in direct mode. Global so that we don't have overhead of recreating them every frame
     */
    private float directAspectRatio = 0;
    private int directWidth = 0;
    private int directHeight = 0;
    private int directFrameOffsetX = 0;
    private int directFrameOffsetY = 0;

    /**
     * Video frame size. Global so that each frame doesn't have function call and casting overhead
     */
    private float videoFrameWidth = 0;
    private float videoFrameHeight = 0;

    /**
     * Media time of the last frame. Used to determine if the No Video message should be shown.
     */
    private long endFrameTime;

    /**
     * Whether or not this render panel will display data regions.
     */
    private final Boolean enableDataRegions = true;

    /**
     * Data region objects to display if enableDataRegions is set to true.
     */
    private final Vector<Region> dataRegions = new Vector<>();

    /**
     * Contains all of the regions in dataRegions in the order in which they should be rendered.
     */
    private final Vector<Region> regionRenderOrder = new Vector<>();

    /**
     * Reference to data model proxy. Needed to get lines for the current data state.
     */
    private final DataModelProxy dmp;

    /**
     * The data region currently being dragged by the mouse.
     */
    private Region selectedRegion;

    /**
     * Location that the mouse is holding onto the region by.
     */
    private RegionGrabLocation grabLocation;

    /**
     * The vertex currently being dragged by the mouse.
     */
    private Point grabbedVertex;

    /**
     * Mouse coordinates scaled to fit video
     */
    private int scaledMouseX;
    private int scaledMouseY;

    /**
     * Offset of mouse to upper left corner of the item being dragged
     */
    private int mouseDownOffsetX;
    private int mouseDownOffsetY;

    /**
     * Timer to repaint video frame
     */
    private final Timer repaintTimer;

    /**
     * The GeneralPath of the current region being rendered. Putting the GeneralPath up here as one of the class fields
     * removes the need create a new GeneralPath object every frame.
     */
    private GeneralPath gFreePath;

    /**
     *
     */
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

    private Preferences graphOptionsPrefs = Preferences.userRoot().node(GraphOptions.class.getName());

    /**
     * DirectVideoRenderPanel is a panel that scales, renders and draws on top of video frames.
     *
     * @param dmp The data model proxy to use as a reference for drawing region data.
     */
    DirectVideoRenderPanel(DataModelProxy dmp)
    {
        //Set end frame time to the minimum possible value so that it never triggers for time 0.
        endFrameTime = Long.MIN_VALUE;
        this.dmp = dmp;

        //Set up event listeners
        addMouseListener(this);
        addMouseMotionListener(this);

        //Set up double buffering.
        setDoubleBuffered(true);

        //Set up a repeating timer for the constant framerate used to calculate TIMER_MS.
        //This timer drives painting.
        repaintTimer = new Timer(TIMER_MS, this);
        repaintTimer.start();
    }

    /**
     * Paints the video frame and regions.
     *
     * @param g Graphics object to draw with.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        //Set rendering hint to prioritize speed. This slightly improves framerate on mac.
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        //Clear the panel and draw the contents of the image buffer
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        //If any of our image buffers or the media player are not set, we can't render anything.
        if(videoFrame == null || mediaPlayer == null || renderCopy == null)
        {
            return;
        }

        //Get a graphics context for the frame buffer so we can draw on it.
        Graphics2D frameGraphics = (Graphics2D) renderCopy.getGraphics();
        frameGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        //Set rendering hint to use anti-aliasing
        frameGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        frameGraphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        //Copy video frame into drawing buffer so that if it gets updated mid-paint, we don't get tearing background and flickering regions.
        frameGraphics.drawImage(videoFrame, 0, 0, null);

        //Get the current media time.
        long currentPlaybackTime = mediaPlayer.getTime();

        //Draw a "No Video" message if no video at this point.
        if((currentPlaybackTime == endFrameTime) || (currentPlaybackTime > mediaPlayer.getLength()) || (currentPlaybackTime == -1))
        {
            drawNoVideoMessage(frameGraphics);
        }
        else if(enableDataRegions)
        {
            //If there is video at this point and regions are enabled, draw them.
            drawDataRegions(frameGraphics);
        }

        //We are done with drawing on the frame at this point, so we can dispose graphics.
        frameGraphics.dispose();

        //Update current scaling so that when we draw the image to the panel, it is at the proper size.
        calculateFrameScaling();

        //Draw the image in the center of the panel.
        java.awt.Image temp = renderCopy.getScaledInstance(directWidth, directHeight, BufferedImage.SCALE_FAST);
        g2d.drawImage(temp, directFrameOffsetX, directFrameOffsetY, null);

        //We are done with rendering, so we can dispose graphics.
        g.dispose();
    }

    /**
     * Updates the values for scaling the current frame.
     */
    private void calculateFrameScaling()
    {
        directAspectRatio = Math.min(getWidth() / videoFrameWidth, getHeight() / videoFrameHeight);
        directWidth = Math.round(videoFrameWidth * directAspectRatio);
        directHeight = Math.round(videoFrameHeight * directAspectRatio);

        //Calculate position where frame is centered
        directFrameOffsetX = (getWidth() - directWidth) / 2;
        directFrameOffsetY = (getHeight() - directHeight) / 2;
    }

    /**
     * Draws the No Video message onto the given graphics object.
     *
     * @param frameGraphics Graphics object to draw the message onto.
     */
    private void drawNoVideoMessage(Graphics2D frameGraphics)
    {
        //Background
        frameGraphics.setColor(Color.BLACK);
        frameGraphics.fillRect((int) videoFrameWidth / 2 - (NO_VIDEO_BACKGROUND_WIDTH / 2), (int) videoFrameHeight / 2 - (NO_VIDEO_BACKGROUND_HEIGHT / 2), NO_VIDEO_BACKGROUND_WIDTH, NO_VIDEO_BACKGROUND_HEIGHT);

        //Text
        frameGraphics.setColor(Color.WHITE);
        frameGraphics.setFont(new Font("SansSerif", Font.BOLD, NO_VIDEO_FONT_SIZE));
        frameGraphics.drawString(NO_VIDEO_MESSAGE, (int) videoFrameWidth / 2 - NO_VIDEO_TEXT_OFFSET, (int) videoFrameHeight / 2);
    }

    /**
     * Draws regions onto the given graphics object based on the states of each region's channel.
     *
     * @param frameGraphics Graphics object to draw regions onto.
     */
    private void drawDataRegions(Graphics2D frameGraphics)
    {
        //Loop through and draw all regions.
        for(Region dataRegion : regionRenderOrder)
        {
            //Only draw the region if this particular region is turned on for drawing.
            if(dataRegion.getEnabled())
            {
                //Drawing color is set by region, should match graph display.
                Color regionColor = dataRegion.getDisplayColor();
                frameGraphics.setColor(regionColor);

                //Gets data for the current region's channel. Only way to get this is in the form of lines from the data model proxy.
                Vector<Line> data = dmp.getDataForChannel(dataRegion.getDeviceType(), dataRegion.getChip(), dataRegion.getPin(), GraphPanel.GRAPH_PREFERRED_WIDTH, GraphPanel.GRAPH_LINE_BASE, GraphPanel.GRAPH_LINE_TOP);

                //If there are no lines, we can't determine state. Move to next region.
                if(data == null)
                {
                    continue;
                }

                //Check if channel is high at the midpoint of the graph
                boolean bChannelHigh = false;
                int graphWidthMid = GraphPanel.GRAPH_PREFERRED_WIDTH / 2;

                //Go through lines and look for a horizontal line in the middle of the graph.
                for(Line currentLine : data)
                {
                    //If we find a line in the middle of the graph, use its height to set whether the channel is high.
                    if((currentLine.getX0() <= graphWidthMid) && (currentLine.getX1() >= graphWidthMid))
                    {
                        //Inverting this seems wrong. Not sure why this is necessary.
                        bChannelHigh = !(currentLine.getY0() == GraphPanel.GRAPH_LINE_TOP);
                    }
                }

                this.regionBorderWidth = Integer.parseInt(graphOptionsPrefs.get("regionThickness", "2"));

                //Set stroke for border. CAP_ROUND and JOIN_ROUND round out region corners
                frameGraphics.setStroke(new BasicStroke((float) regionBorderWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                //Draw either a rectangle for FixedRegion or a GeneralPath for FreeFormRegion
                if(dataRegion instanceof FixedRegion)
                {
                    FixedRegion fixedRegion = (FixedRegion) dataRegion;

                    // Draw a slightly thicker white outline before drawing the region if the this region is selected
                    if(selectedRegion != null && selectedRegion.equals(fixedRegion))
                    {
                        frameGraphics.setColor(Color.WHITE);
                        frameGraphics.setStroke(new BasicStroke((float) regionBorderWidth + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        frameGraphics.drawRect(fixedRegion.getCoordX(), fixedRegion.getCoordY(), fixedRegion.getWidth(), fixedRegion.getHeight());
                    }

                    // Draw rectangle
                    frameGraphics.setColor(regionColor);
                    frameGraphics.setStroke(new BasicStroke((float) regionBorderWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    frameGraphics.drawRect(fixedRegion.getCoordX(), fixedRegion.getCoordY(), fixedRegion.getWidth(), fixedRegion.getHeight());

                    //If the channel is high, fill in the rectangle with transparent version of color
                    if(bChannelHigh)
                    {
                        frameGraphics.setColor(new Color(regionColor.getRed(), regionColor.getGreen(), regionColor.getBlue(), REGION_FILLED_ALPHA));
                        frameGraphics.fillRect(fixedRegion.getCoordX(), fixedRegion.getCoordY(), fixedRegion.getWidth(), fixedRegion.getHeight());
                    }
                }
                else if(dataRegion instanceof FreeFormRegion)
                {
                    FreeFormRegion freeRegion = (FreeFormRegion) dataRegion;

                    // Update the GeneralPath object to look like the region we are currently drawing
                    updateGeneralPath(freeRegion);

                    // Draw a slightly thicker white outline before drawing the region if the this region is selected
                    if(selectedRegion != null && selectedRegion.equals(freeRegion))
                    {
                        frameGraphics.setColor(Color.WHITE);
                        frameGraphics.setStroke(new BasicStroke((float) regionBorderWidth + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        frameGraphics.draw(gFreePath);
                    }

                    // Draw the GeneralPath
                    frameGraphics.setColor(regionColor);
                    frameGraphics.setStroke(new BasicStroke((float) regionBorderWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    frameGraphics.draw(gFreePath);


                    //If the channel is high, fill in the free form region with transparent version of color
                    if(bChannelHigh)
                    {
                        frameGraphics.setColor(new Color(regionColor.getRed(), regionColor.getGreen(), regionColor.getBlue(), REGION_FILLED_ALPHA));
                        frameGraphics.fill(gFreePath);
                    }

                    if(getVerticesEnabled())
                    {
                        // Draw circles on the vertices so there's some sort of visual indicator
                        for(Point p : freeRegion)
                        {
                            frameGraphics.setColor(Color.WHITE);
                            frameGraphics.setStroke(new BasicStroke(2));

                            int radius = (regionBorderWidth / 2) + 3;
                            frameGraphics.fillOval((int) (p.getX() + freeRegion.getCoordX()) - radius, (int) (p.getY() + freeRegion.getCoordY()) - radius, radius * 2, radius * 2);
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the GeneralPath object to represent the FreeFormRegion passed into it.
     *
     * @param freeRegion Object of type FreeFormRegion used to update the GeneralPath object
     */
    private void updateGeneralPath(FreeFormRegion freeRegion)
    {
        // Check if the object exists yet
        if(gFreePath == null)
            gFreePath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, freeRegion.getVertexCount());

        // Undo all the changes applied to the object last time this function was called
        gFreePath.reset();

        // Start at the position of the region
        int coordX = freeRegion.getCoordX();
        int coordY = freeRegion.getCoordY();
        gFreePath.moveTo(coordX, coordY);

        // Make a line from the starting position to the next vertex in the list and so on
        boolean first = true;
        for(Point p : freeRegion)
        {
            // We need to skip the first element returned by the iterator so that we don't draw a line from
            // the first point to itself. Couldn't come up with a more elegant solution unfortunately.
            if(first)
            {
                first = false;
                continue;
            }
            gFreePath.lineTo((float) (p.getX() + coordX), (float) (p.getY() + coordY));
        }
        // Draw a line from the last vertex to the first vertex to finish the polygon
        gFreePath.closePath();
    }

    /**
     * Calculates the spot on the video frame where the mouse is pointing at.
     *
     * @param mouseX mouse pointer's x position
     * @param mouseY mouse pointer's y position
     */
    private void calculateMouseTransform(int mouseX, int mouseY)
    {
        //Calculate overall scale
        float mouseScale = videoFrameWidth / directWidth;

        //Calculate scaled offsets
        scaledMouseX = Math.round((mouseX - directFrameOffsetX) * mouseScale);
        scaledMouseY = Math.round((mouseY - directFrameOffsetY) * mouseScale);
    }

    /**
     * Returns the grab location for the given region based on mouse location.
     *
     * @param mouseRegion Region to check against.
     * @return region that would be grabbed if mouse is pressed.
     */
    private RegionGrabLocation getGrabLocation(Region mouseRegion)
    {
        // Handle fixed and freeform regions differently
        if(mouseRegion instanceof FixedRegion)
        {
            return fixedRegionGrabLoc((FixedRegion) mouseRegion);
        }
        else
        {
            return freeFormRegionGrabLoc((FreeFormRegion) mouseRegion);
        }
    }

    /**
     * Returns the grab location for the given FixedRegion based on mouse location.
     *
     * @param fixedRegion FixedRegion to check against.
     * @return region that would be grabbed if mouse is pressed.
     */
    private RegionGrabLocation fixedRegionGrabLoc(FixedRegion fixedRegion)
    {
        //Store region info so that we only make function calls once, instead of repeatedly throughout if/else if... block.
        int regionCoordX = fixedRegion.getCoordX();
        int regionCoordY = fixedRegion.getCoordY();
        int regionWidth = fixedRegion.getWidth();
        int regionHeight = fixedRegion.getHeight();

        //Detect which side or corner the mouse is on and set the grab location accordingly.
        if((scaledMouseX >= regionCoordX - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionBorderWidth))
        { //Upper left
            return RegionGrabLocation.UPPER_LEFT;
        }
        else if((scaledMouseX >= regionCoordX - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY + regionHeight - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth))
        { //Lower left
            return RegionGrabLocation.LOWER_LEFT;
        }
        else if((scaledMouseX >= regionCoordX + regionWidth - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionBorderWidth))
        { //Upper Right
            return RegionGrabLocation.UPPER_RIGHT;
        }
        else if((scaledMouseX >= regionCoordX + regionWidth - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY + regionHeight - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth))
        { //Lower Right
            return RegionGrabLocation.LOWER_RIGHT;
        }
        else if((scaledMouseX >= regionCoordX - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth))
        { //Left
            return RegionGrabLocation.LEFT;
        }
        else if((scaledMouseX >= regionCoordX + regionWidth - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth))
        { //Right
            return RegionGrabLocation.RIGHT;
        }
        else if((scaledMouseX >= regionCoordX - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionBorderWidth))
        { //Top
            return RegionGrabLocation.TOP;
        }
        else if((scaledMouseX >= regionCoordX - regionBorderWidth) &&
                (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                (scaledMouseY >= regionCoordY + regionHeight - regionBorderWidth) &&
                (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth))
        { //Bottom
            return RegionGrabLocation.BOTTOM;
        }
        else
        {
            return RegionGrabLocation.CENTER;
        }
    }

    /**
     * Returns the grab location for the given FixedRegion based on mouse location.
     *
     * @param freeRegion FreeRegion to check against.
     * @return region that would be grabbed if mouse is pressed.
     */
    private RegionGrabLocation freeFormRegionGrabLoc(FreeFormRegion freeRegion)
    {
        // Update the GeneralPath to the FreeFormRegion we're checking
        updateGeneralPath(freeRegion);

        // Check if the mouse is near a vertex, if it's inside a line, or just inside the region
        Area a = new Area(gFreePath);
        Point p = freeRegion.getClosestPoint(new Point(scaledMouseX, scaledMouseY), ((regionBorderWidth / 2) + 4));

        if(p != null)
        {
            grabbedVertex = p;
            return RegionGrabLocation.VERTEX;
        }
        else if(insideLine())
        {
            return RegionGrabLocation.LINE;
        }
        else if(a.contains(scaledMouseX, scaledMouseY))
        {
            return RegionGrabLocation.CENTER;
        }

        return RegionGrabLocation.NONE;
    }

    /**
     * Loops through regions and searches for one under the mouse cursor
     *
     * @return the region under the cursor
     */
    private Region getMouseRegion()
    {
        Region returnRegion = null;
        Region lineRegion = null;
        Region grabbedVertexRegion = null;

        //Go through all regions
        for(Region dataRegion : regionRenderOrder)
        {
            //Only check this region if the region is enabled. If not, region is invisible and shouldn't be grabbable
            if(dataRegion.getEnabled())
            {
                //If the region is under the mouse, return it.
                if(dataRegion instanceof FixedRegion)
                {
                    // Region is a fixed region
                    FixedRegion fixedRegion = (FixedRegion) dataRegion;

                    int regionCoordX = fixedRegion.getCoordX();
                    int regionCoordY = fixedRegion.getCoordY();
                    int regionWidth = fixedRegion.getWidth();
                    int regionHeight = fixedRegion.getHeight();

                    boolean insideLine =
                            ((scaledMouseX >= regionCoordX - regionBorderWidth) && (scaledMouseX <= regionCoordX + regionBorderWidth) &&
                                    (scaledMouseY >= regionCoordY - regionBorderWidth) && (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth)) ||
                                    ((scaledMouseX >= regionCoordX + regionWidth - regionBorderWidth) && (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                                            (scaledMouseY >= regionCoordY - regionBorderWidth) && (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth)) ||
                                    ((scaledMouseX >= regionCoordX - regionBorderWidth) && (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                                            (scaledMouseY >= regionCoordY - regionBorderWidth) && (scaledMouseY <= regionCoordY + regionBorderWidth)) ||
                                    ((scaledMouseX >= regionCoordX - regionBorderWidth) && (scaledMouseX <= regionCoordX + regionWidth + regionBorderWidth) &&
                                            (scaledMouseY >= regionCoordY + regionHeight - regionBorderWidth) && (scaledMouseY <= regionCoordY + regionHeight + regionBorderWidth));

                    if(insideLine)
                    {
                        lineRegion = dataRegion;
                    }
                    else if((scaledMouseX >= fixedRegion.getCoordX()) &&
                            (scaledMouseX <= (fixedRegion.getCoordX() + fixedRegion.getWidth())) &&
                            (scaledMouseY >= fixedRegion.getCoordY()) &&
                            (scaledMouseY <= (fixedRegion.getCoordY() + fixedRegion.getHeight())))
                    {
                        returnRegion = dataRegion;
                    }
                }
                else
                {
                    // Region is a freeform region
                    FreeFormRegion freeRegion = (FreeFormRegion) dataRegion;

                    // Update the GeneralPath to the FreeFormRegion we're checking
                    updateGeneralPath(freeRegion);

                    // Check if the mouse is near a vertex, if it's inside a line, or just inside the region
                    Area a = new Area(gFreePath);
                    Point p = freeRegion.getClosestPoint(new Point(scaledMouseX, scaledMouseY), ((regionBorderWidth / 2) + 4));

                    if(p != null)
                    {
                        grabbedVertex = p;
                        grabbedVertexRegion = dataRegion;
                    }
                    else if(insideLine())
                    {
                        lineRegion = dataRegion;
                    }
                    else if(a.contains(scaledMouseX, scaledMouseY))
                    {
                        returnRegion = dataRegion;
                    }
                }
            }
        }

        // Return either the region with the closest vertex or line
        // If there is no closest vertex or line found, return the closest region
        if(grabbedVertexRegion != null)
            return grabbedVertexRegion;
        else if(lineRegion != null)
            return lineRegion;
        else
            return returnRegion;
    }

    /**
     * Returns whether the cursor is inside one of the lines of the current GeneralPath.
     *
     * @return boolean value representing whether the cursor is inside a line
     */
    private boolean insideLine()
    {
        // Get the path iterator so we can check every line segment in the GeneralPath
        PathIterator pIter = gFreePath.getPathIterator(null);

        Point prev;
        Point curr;

        // Get the coordinates for the first point
        float[] coords = new float[2];
        pIter.currentSegment(coords);

        // Set the current point and save it for later use
        curr = new Point((int) coords[0], (int) coords[1]);
        Point firstPoint = curr;

        // Loop through remaining segments
        while(!pIter.isDone())
        {
            coords = new float[2];
            int type = pIter.currentSegment(coords);

            // If the type is SEG_CLOSE, it means this is the closing segment and the coordinates returned by
            // currentSegment() will just be (0.0, 0.0) so instead we use the coordinates of the first segment we saved
            if(type == SEG_CLOSE)
            {
                coords[0] = (float) firstPoint.getX();
                coords[1] = (float) firstPoint.getY();
            }

            // Set previous to current point and create a new current point with the coordinates we got from currentSegment()
            prev = curr;
            curr = new Point((int) coords[0], (int) coords[1]);

            float intersectX;
            float intersectY;
            // Calculate the slope of the current segment
            float slope = (float) (curr.getY() - prev.getY()) / (float) (curr.getX() - prev.getX());

            if(slope == 0)
            {
                // Segment is horizontal
                intersectX = scaledMouseX;
                intersectY = (float) prev.getY();
            }
            else if(slope == Float.POSITIVE_INFINITY || slope == Float.NEGATIVE_INFINITY)
            {
                // Segment is vertical
                intersectX = (float) prev.getX();
                intersectY = scaledMouseY;
            }
            else
            {
                // Calculate the slope of a line perpendicular to the line segment
                float perpSlope = (float) ((1.0 / slope) * -1);

                // Calculate the y-intercept of the line segment and of the perpendicular line
                float yIntercept = ((float) prev.getY()) - (slope * ((float) prev.getX()));
                float perpYIntercept = scaledMouseY - (perpSlope * scaledMouseX);

                // Calculate the intersection of the two lines
                intersectX = (perpYIntercept - yIntercept) / (slope - perpSlope);
                intersectY = (perpSlope * intersectX) + perpYIntercept;
            }

            // Check if the intersection point is on the line segment
            boolean betweenX = (intersectX >= prev.getX() && intersectX <= curr.getX()) || (intersectX <= prev.getX() && intersectX >= curr.getX());
            boolean betweenY = (intersectY >= prev.getY() && intersectY <= curr.getY()) || (intersectY <= prev.getY() && intersectY >= curr.getY());

            if(betweenX && betweenY)
            {
                // Calculate the distance squared between the mouse cursor and the line segment
                float dist = (float) (Math.pow((intersectX - scaledMouseX), 2) + Math.pow((intersectY - scaledMouseY), 2));
                // Calculate the maximum thickness squared
                float thickness = (float) Math.pow(regionBorderWidth, 2);

                // If the distance to the line is less than the thickness, then that means the cursor is inside the line segment
                if(dist < thickness)
                    return true;
            }

            // Move to the next segment
            pIter.next();
        }

        return false;
    }

    /**
     * Sets the media player to reference when checking playback time for no video message.
     *
     * @param player MediaPlayer object
     */
    void setMediaPlayer(MediaPlayer player)
    {
        mediaPlayer = player;
    }

    /**
     * Sets the reference to the image buffer that will be updated with video frames.
     *
     * @param buffer image buffer
     */
    void setVideoFrame(BufferedImage buffer)
    {
        videoFrame = buffer;

        //Create a copy buffer of the same size and type as the original.
        renderCopy = new BufferedImage(videoFrame.getWidth(), videoFrame.getHeight(), videoFrame.getType());

        //Set width and height of the video frame.
        videoFrameWidth = videoFrame.getWidth();
        videoFrameHeight = videoFrame.getHeight();
    }

    /**
     * Gets the buffer that is updated with current video frames by the media player.
     *
     * @return image buffer
     */
    BufferedImage getVideoFrame()
    {
        return videoFrame;
    }

    /**
     * Sets the internal timestamp referenced for displaying the end of video message.
     */
    void displayVideoFinished()
    {
        //Update timestamp.
        endFrameTime = mediaPlayer.getTime();
        System.out.println("Displaying video finished messages at frame time " + endFrameTime);
    }

    /**
     * Adds a region to be drawn on the frame.
     *
     * @param region Region object being added to the panel
     */
    void addRegion(Region region)
    {
        int regionCount;

        // Add the region to both vectors
        dataRegions.add(region);
        regionRenderOrder.add(region);
        regionCount = dataRegions.size() - 1;

        //The starting point for each region should be in the upper left, with each to the right of the one before it.
        int x = (100 * (regionCount % 9)) + (regionBorderWidth * (regionCount % 9));
        int y;
        if(regionCount < 9)
        {
            y = regionBorderWidth / 2;
        }
        else if(regionCount < 18)
        {
            y = regionBorderWidth * 50 / 2;
        }
        else
        {
            y = regionBorderWidth * 100 / 2;
        }
        region.setCoordX(x);
        region.setCoordY(y);

        // Two types of Regions
        if(region instanceof FixedRegion)
        {
            FixedRegion fixedRegion = (FixedRegion) region;
            fixedRegion.setWidth(100);
            fixedRegion.setHeight(100);
        }
        else
        {
            FreeFormRegion freeRegion = (FreeFormRegion) region;
            freeRegion.setVertices(new int[]{0, 100, 100, 0}, new int[]{0, 0, 100, 100});
        }
    }

    /**
     * Replace the oldRegion in the vectors with the newRegion
     *
     * @param oldRegion Region being replaced
     * @param newRegion Region replacing the old region
     */
    public void replaceRegion(Region oldRegion, Region newRegion)
    {
        int index = dataRegions.indexOf(oldRegion);
        dataRegions.set(index, newRegion);

        regionRenderOrder.remove(oldRegion);
        regionRenderOrder.add(newRegion);
    }

    /**
     * Removes the specified data region from region collection
     *
     * @param region The region instance to remove.
     */
    void removeRegion(Region region)
    {
        dataRegions.remove(region);
        regionRenderOrder.remove(region);
    }

    /**
     * Gets a copy of the data regions
     *
     * @return a copy of the data regions
     */
    public Vector<Region> getDataRegions()
    {
        return new Vector<>(dataRegions);
    }

    //Needed to use MouseListener interface
    public void mouseClicked(MouseEvent e)
    {

    }

    /**
     * Handles mouse down by selecting a region if under the mouse.
     *
     * @param e MouseEvent triggered by mouse press
     */
    public void mousePressed(MouseEvent e)
    {
        //Sets the region currently being moved by the mouse.
        selectedRegion = getMouseRegion();

        //If there is a region under the mouse, update the coordinate offsets and set cursor.
        if(selectedRegion != null)
        {
            regionRenderOrder.remove(selectedRegion);
            regionRenderOrder.add(selectedRegion);

            // If the MouseEvent is a right click, call helper method to create the right click context menu
            if(e.isPopupTrigger())
                regionRightClick(e, selectedRegion);

            mouseDownOffsetX = selectedRegion.getCoordX() - scaledMouseX;
            mouseDownOffsetY = selectedRegion.getCoordY() - scaledMouseY;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        else if(e.isPopupTrigger())
        {
            regionRightClick(e, null);
        }
    }

    /**
     * Handles mouse up by clearing the region and resetting the cursor
     *
     * @param e MouseEvent triggered by mouse release
     */
    public void mouseReleased(MouseEvent e)
    {
        //Gets the region under the mouse.
        Region mouseRegion = getMouseRegion();

        //Set cursor dependent on whether there is a region under the mouse or not.
        if(mouseRegion != null)
        {
            // We need to duplicate this in this function so that it works on Windows...
            // Linux triggers the popup trigger on mousePressed and Windows triggers it on mouseReleased
            // If the MouseEvent is a right click, call helper method to create the right click context menu
            if(e.isPopupTrigger())
                regionRightClick(e, selectedRegion);

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        else
        {
            if(e.isPopupTrigger())
                regionRightClick(e, null);

            setCursor(Cursor.getDefaultCursor());
        }

        //If the mouse is released and a region or vertex is being moved, it needs to be cleared.
        selectedRegion = null;
        grabbedVertex = null;
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    /**
     * Handles the mouse leaving the panel by clearing the region and resetting the cursor.
     *
     * @param e MouseEvent triggered by mouse exiting the panel
     */
    public void mouseExited(MouseEvent e)
    {
        //Reset the cursor and reference to grabbed region or vertex.
        setCursor(Cursor.getDefaultCursor());
        selectedRegion = null;
        grabbedVertex = null;
    }

    /**
     * Handles dragging of the mouse by moving or resizing the region.
     *
     * @param e MouseEvent triggered by mouse dragging
     */
    public void mouseDragged(MouseEvent e)
    {
        //Update screen to video transform for mouse
        calculateMouseTransform(e.getX(), e.getY());

        if(selectedRegion == null)
            return;

        // Call helper methods to handle the transformation of either a FixedRegion or a FreeFormRegion
        if(selectedRegion instanceof FixedRegion)
        {
            FixedRegion fixedRegion = (FixedRegion) selectedRegion;
            handleFixedRegionTransform(fixedRegion);
        }
        else
        {
            FreeFormRegion freeRegion = (FreeFormRegion) selectedRegion;
            handleFreeFormRegionTransform(freeRegion);
        }

        //Repaint so that regions don't look choppy if video is paused.
        repaint();
    }

    /**
     * Handle any transformation being applied to a fixed region.
     *
     * @param fixedRegion Object of type FixedRegion onto which a transformation is being applied
     */
    private void handleFixedRegionTransform(FixedRegion fixedRegion)
    {
        //Used to calculate new locations and lengths.
        int locDiff;
        int newWidth;
        int newHeight;

        //Check if this is a move or resize
        //If the grab location is the center, then the region is being moved.
        if(grabLocation == RegionGrabLocation.CENTER)
        {
            // Make sure that the region does not move outside the video frame

            fixedRegion.setCoordX(scaledMouseX + mouseDownOffsetX);
            if(fixedRegion.getCoordX() < 0)
            {
                fixedRegion.setCoordX(0);
            }
            else if(fixedRegion.getCoordX() + fixedRegion.getWidth() > videoFrameWidth)
            {
                fixedRegion.setCoordX((int) videoFrameWidth - fixedRegion.getWidth());
            }

            fixedRegion.setCoordY(scaledMouseY + mouseDownOffsetY);
            if(fixedRegion.getCoordY() < 0)
            {
                fixedRegion.setCoordY(0);
            }
            else if(fixedRegion.getCoordY() + fixedRegion.getHeight() > videoFrameHeight)
            {
                fixedRegion.setCoordY((int) videoFrameHeight - fixedRegion.getHeight());
            }
        }
        else
        {
            //Grab location is not the center. Region is being resized.
            //Handle left
            if((grabLocation == RegionGrabLocation.LEFT) ||
                    (grabLocation == RegionGrabLocation.LOWER_LEFT) ||
                    (grabLocation == RegionGrabLocation.UPPER_LEFT))
            {
                locDiff = fixedRegion.getCoordX() - (scaledMouseX + mouseDownOffsetX);
                newWidth = fixedRegion.getWidth() + locDiff;
                if(newWidth > Region.MIN_WIDTH)
                {
                    fixedRegion.setCoordX(scaledMouseX + mouseDownOffsetX);
                    fixedRegion.setWidth(newWidth);
                }
            }

            //Handle right
            if((grabLocation == RegionGrabLocation.RIGHT) ||
                    (grabLocation == RegionGrabLocation.LOWER_RIGHT) || (
                    grabLocation == RegionGrabLocation.UPPER_RIGHT))
            {
                locDiff = scaledMouseX - fixedRegion.getCoordX();
                fixedRegion.setWidth(locDiff);
            }

            //Handle top
            if((grabLocation == RegionGrabLocation.TOP) ||
                    (grabLocation == RegionGrabLocation.UPPER_LEFT) ||
                    (grabLocation == RegionGrabLocation.UPPER_RIGHT))
            {
                locDiff = fixedRegion.getCoordY() - (scaledMouseY + mouseDownOffsetY);
                newHeight = fixedRegion.getHeight() + locDiff;
                if(newHeight > Region.MIN_HEIGHT)
                {
                    fixedRegion.setCoordY(scaledMouseY + mouseDownOffsetY);
                    fixedRegion.setHeight(fixedRegion.getHeight() + locDiff);
                }
            }

            //Handle bottom
            if((grabLocation == RegionGrabLocation.BOTTOM) ||
                    (grabLocation == RegionGrabLocation.LOWER_LEFT) ||
                    (grabLocation == RegionGrabLocation.LOWER_RIGHT))
            {
                locDiff = scaledMouseY - fixedRegion.getCoordY();
                fixedRegion.setHeight(locDiff);
            }
        }
    }

    /**
     * Handle any transformation being applied to a freeform region.
     *
     * @param freeRegion Object of type FixedRegion onto which a transformation is being applied
     */
    private void handleFreeFormRegionTransform(FreeFormRegion freeRegion)
    {
        //Used to calculate new locations and lengths.
        int locDiffX;
        int locDiffY;

        //Check if this is a move or resize
        //If the grab location is the center or a line, then the region is being moved.
        if(grabLocation == RegionGrabLocation.CENTER || grabLocation == RegionGrabLocation.LINE)
        {
            // Prevent any part of the region from moving outside of the frame

            // These points serve as the bounds for the top and left side of the region
            Point leftMost = freeRegion.getLeftMostPoint();
            Point topMost = freeRegion.getTopMostPoint();

            freeRegion.setCoordX(scaledMouseX + mouseDownOffsetX);
            if(leftMost.getX() + freeRegion.getCoordX() < 0)
            {
                freeRegion.setCoordX((int) -leftMost.getX());
            }
            else if(leftMost.getX() + freeRegion.getCoordX() + freeRegion.getBoundingWidth() > videoFrameWidth)
            {
                freeRegion.setCoordX((int) (videoFrameWidth - freeRegion.getBoundingWidth() - leftMost.getX()));
            }

            freeRegion.setCoordY(scaledMouseY + mouseDownOffsetY);
            if(topMost.getY() + freeRegion.getCoordY() < 0)
            {
                freeRegion.setCoordY((int) -topMost.getY());
            }
            else if(topMost.getY() + freeRegion.getCoordY() + freeRegion.getBoundingHeight() > videoFrameHeight)
            {
                freeRegion.setCoordY((int) (videoFrameHeight - freeRegion.getBoundingHeight() - topMost.getY()));
            }
        }
        else if(grabLocation == RegionGrabLocation.VERTEX)
        {
            // Grab location is not the center or a line. Region is being resized.

            // Calculate difference between vertex and mouse position
            locDiffX = scaledMouseX - (int) (grabbedVertex.getX() + freeRegion.getCoordX());
            locDiffY = scaledMouseY - (int) (grabbedVertex.getY() + freeRegion.getCoordY());

            // Make sure vertex is not being moved off screen
            if(grabbedVertex.getX() + freeRegion.getCoordX() + locDiffX > videoFrameWidth)
                locDiffX = (int) (videoFrameWidth - (grabbedVertex.getX() + freeRegion.getCoordX()));
            else if(grabbedVertex.getX() + freeRegion.getCoordX() + locDiffX < 0)
                locDiffX = 0;

            if(grabbedVertex.getY() + freeRegion.getCoordY() + locDiffY > videoFrameHeight)
                locDiffY = (int) (videoFrameHeight - (grabbedVertex.getY() + freeRegion.getCoordY()));
            else if(grabbedVertex.getY() + freeRegion.getCoordY() + locDiffY < 0)
                locDiffY = 0;

            freeRegion.movePoint(grabbedVertex, locDiffX, locDiffY);
        }
    }

    /**
     * Handles moving of the mouse by checking for regions under and changing the cursor to indicate possible actions.
     *
     * @param e  MouseEvent triggered by mouse moving
     */
    public void mouseMoved(MouseEvent e)
    {
        //Update screen to video transform for mouse
        calculateMouseTransform(e.getX(), e.getY());

        //Get region under the mouse.
        Region mouseRegion = getMouseRegion();

        //If there is a region under the mouse, need to update the cursor accordingly.
        if(mouseRegion != null)
        {
            //Get the location of the cursor on the region
            grabLocation = getGrabLocation(mouseRegion);

            //Set cursor based on the grab location.
            switch(grabLocation)
            {
                case UPPER_LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    break;
                case UPPER_RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    break;
                case LOWER_LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    break;
                case LOWER_RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    break;
                case TOP:
                    setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    break;
                case BOTTOM:
                    setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    break;
                case LEFT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    break;
                case RIGHT:
                    setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    break;
                case VERTEX:
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    break;
                case LINE:
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    break;
                case CENTER:
                default:
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
        else
        {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Create and populate the right click context menu.
     *
     * @param e MouseEvent object representing the event that triggered this function
     * @param dataRegion Region object that was right-clicked, null if right click wasn't over a region
     */
    private void regionRightClick(MouseEvent e, Region dataRegion)
    {
        RegionContextMenu menu = new RegionContextMenu();

        // No region under right click
        if(dataRegion != null)
        {
            // Create convert menu item
            CommandConvert conv = new CommandConvert();
            conv.setTargets(this, dataRegion, dmp);

            String text = (dataRegion instanceof FixedRegion) ? "Convert to Free-Form Region" : "Convert to Fixed Region";
            menu.setConvertActionCommand(conv, text);

            // If selected region is freeform
            if(dataRegion instanceof FreeFormRegion)
            {
                FreeFormRegion freeRegion = (FreeFormRegion) dataRegion;
                Point p = freeRegion.getClosestPoint(new Point(scaledMouseX, scaledMouseY), (regionBorderWidth / 2) + 4);

                // If there isn't a nearby vertex
                if(p == null)
                {
                    // Create add vertex menu item
                    addVertex(freeRegion, menu);
                }
                else if(freeRegion.getVertexCount() > 3)
                {
                    // Create remove vertex menu item
                    CommandRemoveVertex cRemV = new CommandRemoveVertex();
                    cRemV.setTargets((FreeFormRegion) dataRegion, p);

                    text = "Remove Vertex";
                    menu.setRemoveVertexCommand(cRemV, text);
                }
            }
        }

        // Create toggle vertices menu item
        CommandToggleVertices togVert = new CommandToggleVertices();
        togVert.setTargets(this);

        String text = "Toggle Vertices";
        menu.setToggleVerticesCommand(togVert, text);

        // Create reset region placements menu item
        CommandResetRegions togReset = new CommandResetRegions();
        togReset.setTargets(this, dmp);
        text = "Reset Regions";
        menu.setToggleResetRegions(togReset, text);

        // Show/render the menu item
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Creates the add vertex menu item in the RegionContextMenu.
     *
     * @param freeRegion FreeFormRegion that was right clicked, where the new vertex is being added.
     * @param menu RegionContextMenu where the menu item needs to be added.
     */
    private void addVertex(FreeFormRegion freeRegion, RegionContextMenu menu)
    {
        int coordX = freeRegion.getCoordX();
        int coordY = freeRegion.getCoordY();

        // Point variables to fill out and pass into the add vertex command
        // Points between which the new vertex is situated
        Point p1 = null;
        Point p2 = null;
        // Point where the new vertex is to be created
        Point pIntersection = null;

        Point prev;
        Point curr = null;
        Point firstPoint = null;

        // Iterate through all of the points in the region and loop back around to the first point
        Iterator<Point> iter = freeRegion.iterator();
        for(int x = 0; x < freeRegion.getVertexCount() + 1; x++)
        {
            // Once the iterator runs out, we use the first point again
            Point p;
            if(iter.hasNext())
            {
                p = iter.next();
            }
            else
            {
                p = firstPoint;
            }

            // Set previous to current point and set current to the next point in the iterator
            prev = curr;
            curr = p;

            // Save the first point we get for later use
            if(firstPoint == null)
                firstPoint = curr;

            if(prev != null && curr != null)
            {
                float intersectX;
                float intersectY;
                // Calculate the slope of the current segment
                float slope = (float) (curr.getY() - prev.getY()) / (float) (curr.getX() - prev.getX());

                if(slope == 0)
                {
                    // Segment is horizontal
                    intersectX = scaledMouseX;
                    intersectY = (float) prev.getY() + coordY;
                }
                else if(slope == Float.POSITIVE_INFINITY || slope == Float.NEGATIVE_INFINITY)
                {
                    // Segment is vertical
                    intersectX = (float) prev.getX() + coordX;
                    intersectY = scaledMouseY;
                }
                else
                {
                    // Calculate the slope of a line perpendicular to the line segment
                    float perpSlope = (float) ((1.0 / slope) * -1);

                    // Calculate the y-intercept of the line segment and of the perpendicular line
                    float yIntercept = (float) (prev.getY() + coordY) - (slope * (float) (prev.getX() + coordX));
                    float perpYIntercept = scaledMouseY - (perpSlope * scaledMouseX);

                    // Calculate the intersection of the two lines
                    intersectX = (perpYIntercept - yIntercept) / (slope - perpSlope);
                    intersectY = (perpSlope * intersectX) + perpYIntercept;
                }

                // Check if the intersection point is on the line segment
                boolean betweenX = (intersectX >= (prev.getX() + coordX) && intersectX <= (curr.getX() + coordX)) || (intersectX <= (prev.getX() + coordX) && intersectX >= (curr.getX() + coordX));
                boolean betweenY = (intersectY >= (prev.getY() + coordY) && intersectY <= (curr.getY() + coordY)) || (intersectY <= (prev.getY() + coordY) && intersectY >= (curr.getY() + coordY));

                if(betweenX && betweenY)
                {
                    // Calculate the distance squared between the mouse cursor and the line segment
                    float dist = (float) (Math.pow((intersectX - scaledMouseX), 2) + Math.pow((intersectY - scaledMouseY), 2));
                    // Calculate the maximum thickness squared
                    float thickness = (float) Math.pow(regionBorderWidth, 2);

                    // If the distance to the line is less than the thickness, then that means the right click event is inside the line segment
                    if(dist < thickness)
                    {
                        p1 = prev;
                        p2 = curr;
                        pIntersection = new Point((int) intersectX, (int) intersectY);
                    }
                }
            }
        }

        // Create the add vertex menu item
        if(p1 != null)
        {
            CommandAddVertex cAddV = new CommandAddVertex();

            // Adjust the location of the new vertex relative to the position of the region
            pIntersection.translate(-coordX, -coordY);

            cAddV.setTargets(freeRegion, p1, p2, pIntersection);
            String text = "Add Vertex";

            menu.setAddVertexCommand(cAddV, text);
        }
    }

    /**
     * Returns whether or not vertices are enabled for drawing.
     *
     * @return boolean value representing whether vertices are enabled for drawing
     */
    public boolean getVerticesEnabled()
    {
        boolean b = this.prefs.getBoolean("verticesEnabled", true);
        return b;
    }

    /**
     * Sets the verticesEnabled flag to the specified boolean value.
     *
     * @param b boolean value representing whether vertices are enabled for drawing
     */
    public void setVerticesEnabled(boolean b)
    {
        this.prefs.putBoolean("verticesEnabled", b);
    }

    /**
     * Used to catch when the timer fires and repaint
     *
     * @param e ActionEvent created by timer firing
     */
    public void actionPerformed(ActionEvent e)
    {
        //Repaint if the repaint timer has fired.
        if(e.getSource() == repaintTimer)
        {
            repaint();
        }
    }
}
