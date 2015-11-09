package track;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jxmapviewer.viewer.GeoPosition;

/**
 * Represents a track
 *
 * @author Martin Steiger
 */
public class Track {
    private List<TrackSegment> segments = new ArrayList<>();
    
    private String name;
    private String comments;

    /**
     * Default constructor (no name set)
     */
    public Track() {
    }

    public void addSegment(TrackSegment segment) {
        segments.add(segment);
    }
    
    public List<TrackSegment> getSegments()  {
        return Collections.unmodifiableList(segments);
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the comments
     */
    public String getComments() {
        return comments;
    }
    
    /**
     * @param comments the comments to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * @return the average speed in km/h
     */
    public double getAverageSpeed() {
        return getTotalDistance() / getTotalTime() * 3600;
    }
    
    /**
     * @return the accumulated (ascending) elevation difference for the track
     */
    public double getTotalElevationDifference() {
        double result = 0;
        for (TrackSegment ts : segments) {
            result += ts.getTotalElevationDifference();
        }
        return result;
    }
    
    /**
     * @return the total distance of the track in meters
     */
    public double getTotalDistance() {
        double result = 0;
        for(TrackSegment ts: segments) {
            result += ts.getTotalDistance();
        }
        return result;
    }
    
    /**
     * @return the total time of the track (in ms)
     */
    public long getTotalTime() {
        long result = 0;
        for(TrackSegment ts: segments) {
            result += ts.getTotalTime();
        }
        return result;
    }
    
    public Date getStartTime() {
        for(TrackSegment ts: segments) {
            Date startTime = ts.getStartTime();
            if(startTime != null) {
                return startTime;
            }
        }
        return null;
    }
}
