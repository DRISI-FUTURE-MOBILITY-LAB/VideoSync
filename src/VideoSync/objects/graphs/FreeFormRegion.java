/*
 * ****************************************************************
 * File: 			FreeFormRegion.java
 * Date Created:  	September 18, 2019
 * Programmer:		Aleksey Zasorin
 *
 * Purpose:			Used to create free-form regions with
 *                  individually move-able vertices.
 * ****************************************************************
 */

package VideoSync.objects.graphs;

import java.awt.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Vector;

@SuppressWarnings("SuspiciousIntegerDivAssignment")
public class FreeFormRegion extends Region implements Iterable<Point>
{
    // Order of points in the Vector is important as they must form a cycle graph
    // Consider converting to ArrayDeque instead to make sure order can't change
    /**
     * Relative coordinates of the four vertices
     * Lines are drawn between adjacent indices
     */
    private final Vector<Point> vertices = new Vector<>();

    /**
     * Number of vertices in this region.
     */
    private int vertexCount;

    public FreeFormRegion()
    {
        super();
        vertexCount = 4;
        vertices.add(new Point(0, 0));
        vertices.add(new Point(MIN_WIDTH, 0));
        vertices.add(new Point(MIN_WIDTH, MIN_HEIGHT));
        vertices.add(new Point(0, MIN_HEIGHT));
    }

    /**
     * Sets the X and Y coordinate positions of the vertices in this region.
     *
     * @param xVert Array of X coordinate positions of vertices in the free-form region relative to the region's origin
     *              (upper left corner).
     * @param yVert Array of Y coordinate positions of vertices in the free-form region relative to the region's origin
     *              (upper left corner).
     */
    public void setVertices(int[] xVert, int[] yVert)
    {
        if(xVert == null || yVert == null)
            throw new IllegalArgumentException("Arrays cannot be null.");

        if(xVert.length <= 2 || yVert.length <= 2)
            throw new IllegalArgumentException("Array size too small. Region must have more than 2 points.");

        vertices.clear();
        vertexCount = 0;
        for(int i = 0; i < xVert.length && i < yVert.length; i++)
        {
            vertices.add(new Point(xVert[i], yVert[i]));
            vertexCount++;
        }
    }

    /**
     * Finds the closest vertex to the given point within the given radius.
     *
     * @param p      Point for which to find the closest vertex in this region.
     * @param radius Radius in which to search around the given point.
     * @return The closest vertex as a Point object or null if no vertex has been found within the given radius.
     */
    public Point getClosestPoint(Point p, int radius)
    {
        int squaredX;
        int squaredY;
        int squaredRadius = (int) Math.pow(radius, 2.0);
        int dist;
        for(Point vertex : vertices)
        {
            squaredX = (int) Math.pow(p.getX() - (vertex.getX() + getCoordX()), 2.0);
            squaredY = (int) Math.pow(p.getY() - (vertex.getY() + getCoordY()), 2.0);
            dist = squaredX + squaredY;
            if(dist < squaredRadius)
            {
                return vertex;
            }
        }

        return null;
    }

    /**
     * Translates the given point in the X and Y directions by the amount specified.
     *
     * @param originalPos Point representing the point to be moved.
     * @param dx          Change in the X direction.
     * @param dy          Change in the Y direction.
     */
    public void movePoint(Point originalPos, int dx, int dy)
    {
        // In the case that the vertex we are moving is the origin (0, 0), we have to update the region coordinates
        // and translate every vertex *except* the origin accordingly.
        if(originalPos.getX() == 0 && originalPos.getY() == 0)
        {
            this.setCoordX(this.getCoordX() + dx);
            this.setCoordY(this.getCoordY() + dy);

            for(Point p : vertices)
            {
                if(p != originalPos)
                    p.translate(-dx, -dy);
            }
        }
        else
        {
            int index = vertices.indexOf(originalPos);
            vertices.get(index).translate(dx, dy);
        }
    }

    /**
     * Calculates the width of the region based on the difference between the average X coordinate position of the
     * left-most and right-most vertices relative to the origin point (upper left corner).
     *
     * @return Returns the calculated height of the free-form region.
     */
    @SuppressWarnings({"unchecked"})
    public int getWidth()
    {
        // This function is written to support any region with >2 vertices

        // Find left-most points and right-most points by sorting the vector by output of getX()
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getX));

        // Find average X positions of the vertices
        int rightAverage = 0;
        int leftAverage = 0;
        for(int x = 0; x < vertexCount / 2; x++)
        {
            rightAverage += tmp.get(tmp.size() - x - 1).getX();
            leftAverage += tmp.get(x).getX();
        }
        rightAverage /= (vertexCount / 2);
        leftAverage /= (vertexCount / 2);

        // Return the difference (width) of the differences
        return rightAverage - leftAverage;
    }

    /**
     * Calculates the height of the region based on the difference between the average Y coordinate position of the
     * top-most and bottom-most vertices relative to the origin point (upper left corner).
     *
     * @return Returns the calculated height of the free-form region.
     */
    @SuppressWarnings("unchecked")
    public int getHeight()
    {
        // This function is written to support any region with >2 vertices

        // Find top-most points and bottom-most points by sorting the vector by output of getY()
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getY));

        // Find average Y positions of the vertices
        int bottomAverage = 0;
        int topAverage = 0;
        for(int x = 0; x < vertexCount / 2; x++)
        {
            bottomAverage += tmp.get(tmp.size() - x - 1).getY();
            topAverage += tmp.get(x).getY();
        }
        bottomAverage /= (vertexCount / 2);
        topAverage /= (vertexCount / 2);

        // Return the difference (height) of the differences
        return bottomAverage - topAverage;
    }

    /**
     * Gets the left-most vertex in the region.
     *
     * @return Point object representing the vertex furthest to the left.
     */
    @SuppressWarnings("unchecked")
    public Point getLeftMostPoint()
    {
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getX));

        return tmp.get(0);
    }

    /**
     * Gets the top-most vertex in the region.
     *
     * @return Point object representing the vertex furthest to the top.
     */
    @SuppressWarnings("unchecked")
    public Point getTopMostPoint()
    {
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getY));

        return tmp.get(0);
    }

    /**
     * Get the width of the bounding rectangle around the region.
     *
     * @return Value representing the bounding width
     */
    @SuppressWarnings("unchecked")
    public int getBoundingWidth()
    {
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getX));

        return (int) (tmp.get(tmp.size() - 1).getX() - tmp.get(0).getX());
    }

    /**
     * Get the height of the bounding rectangle around the region.
     *
     * @return Value representing the bounding height.
     */
    @SuppressWarnings("unchecked")
    public int getBoundingHeight()
    {
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getY));

        return (int) (tmp.get(tmp.size() - 1).getY() - tmp.get(0).getY());
    }

    /**
     * Calculates the new region position based on the average X value of the left-most points. Used to find the
     * new position of the region when converting from a free-form region to a fixed region.
     *
     * @return Value representing the new X coordinate position.
     */
    @SuppressWarnings("unchecked")
    public int getAdjustedX()
    {
        // This function is written to support any region with >2 vertices

        // Find left-most points by sorting the vector by output of getX()
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getX));

        int leftAverage = 0;
        for(int x = 0; x < vertexCount / 2; x++)
            leftAverage += tmp.get(x).getX();

        leftAverage /= (vertexCount / 2);

        return this.getCoordX() + leftAverage;
    }

    /**
     * Calculates the new region position based on the average Y value of the top-most points. Used to find the
     * new position of the region when converting from a free-form region to a fixed region.
     *
     * @return Value representing the new Y coordinate position.
     */
    @SuppressWarnings("unchecked")
    public int getAdjustedY()
    {
        // This function is written to support any region with >2 vertices

        // Find top-most points by sorting the vector by output of getY()
        Vector<Point> tmp = (Vector<Point>) vertices.clone();
        tmp.sort(Comparator.comparing(Point::getY));

        int topAverage = 0;
        for(int x = 0; x < vertexCount / 2; x++)
            topAverage += tmp.get(x).getY();

        topAverage /= (vertexCount / 2);

        return this.getCoordY() + topAverage;
    }

    /**
     * Gets the amount of vertices in the region.
     *
     * @return int representing the amount of vertices in the region.
     */
    public int getVertexCount()
    {
        return vertexCount;
    }

    // TODO: Can probably be rewritten to not need p2

    /**
     * Adds a vertex at a specified position on an existing region line between two specified points.
     *
     * @param p1            The point after which we need to insert the new vertex
     * @param p2            The point before which we need to insert the new vertex
     * @param pIntersection The position on the line segment where we need to insert the new point
     */
    public void addVertex(Point p1, Point p2, Point pIntersection)
    {
        vertexCount++;

        // We're looking for prev == p1 and curr == p2
        Point prev;
        Point curr = null;
        // Needed so that we can wrap around to the first point at the end
        Point firstPoint = null;

        int x;
        Iterator<Point> pIter = vertices.iterator();
        for(x = 0; x < vertices.size() + 1; x++)
        {
            prev = curr;

            if(pIter.hasNext())
                curr = pIter.next();
            else
                curr = firstPoint;

            if(x == 0)
                firstPoint = curr;

            if(prev == p1 && curr == p2)
                break;
        }

        vertices.add(x, pIntersection);
    }

    /**
     * Removes the specified point from the region.
     *
     * @param p Point that needs to be removed
     */
    public void removeVertex(Point p)
    {
        // Region must have at least 3 vertices to form a polygon
        if(vertices.size() < 4)
            return;

        // If the point being removed is the origin, we need to set the next vertex as
        // the origin and translate the rest of the vertices accordingly.
        // Else, we can just remove the point.
        if(vertices.indexOf(p) == 0)
        {
            vertices.remove(p);

            int dx = (int) (0 - vertices.get(0).getX());
            int dy = (int) (0 - vertices.get(0).getY());

            setCoordX((int) (getCoordX() + vertices.get(0).getX()));
            setCoordY((int) (getCoordY() + vertices.get(0).getY()));

            vertices.get(0).setLocation(0, 0);

            for(Point vertex : vertices)
            {
                if(vertices.indexOf(vertex) == 0)
                    continue;

                vertex.translate(dx, dy);
            }
        }
        else
        {
            vertices.remove(p);
        }

        vertexCount--;
    }

    // TODO: Add proper hashCode method

    // Implemented a custom equals functions to make sure regions could be removed from Collections correctly
    @Override
    public boolean equals(Object o)
    {
        // Are we comparing an object to itself
        if(this == o)
            return true;

        if(o == null)
            return false;

        // Can't be equal if objects are of different classes
        if(getClass() != o.getClass())
            return false;

        FreeFormRegion freeRegion = (FreeFormRegion) o;

        // Check if vertex coordinates are the same
        Iterator<Point> iter = iterator();
        int x = 0;
        while(iter.hasNext() && vertices.get(x) != null)
        {
            if(Objects.equals(vertices.get(x), iter.next()))
                return false;

            x++;
        }

        // Check if remaining defining properties are the same
        return Objects.equals(super.getCoordX(), freeRegion.getCoordX())
                && Objects.equals(super.getCoordY(), freeRegion.getCoordY())
                && Objects.equals(getVertexCount(), freeRegion.getVertexCount())
                && Objects.equals(super.getChip(), freeRegion.getChip())
                && Objects.equals(super.getPin(), freeRegion.getPin())
                && Objects.equals(super.getDisplayColor(), freeRegion.getDisplayColor());
    }

    /**
     * Gets the iterator from the internal vertex collection.
     *
     * @return Iterator of type Iterator<Point> that iterates over the collection of vertices in the region.
     */
    public Iterator<Point> iterator()
    {
        return vertices.iterator();
    }
}
