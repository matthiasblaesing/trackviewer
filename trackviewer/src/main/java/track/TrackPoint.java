package track;

import java.util.Date;

import org.jxmapviewer.viewer.GeoPosition;

/**
 * A single point in a track
 *
 * @author Martin Steiger
 */
public class TrackPoint {

    private final GeoPosition pos;

    private Date time;

    private long relativeTime;
    private double elevation;
    private double distance;
    private double speed;

    /**
     * @param pos the position
     * @param time the time
     */
    public TrackPoint(GeoPosition pos, Date time) {
        this.time = time;
        this.pos = pos;
    }
 
    /**
     * @return the pos
     */
    public GeoPosition getPos() {
        return pos;
    }

    /**
     * @return the elevation
     */
    public double getElevation() {
        return elevation;
    }
    
    /**
     * @return the time
     */
    public Date getTime() {
        return time;
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @param speed the speed to set
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }

    /**
     * @param elevation the elevation to set
     */
    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    /**
     * @return the distance in meters
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @param distance the distance in meters
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * @param time the new time
     */
    public void setTime(Date time) {
        this.time = time;
    }

    /**
     * @return milliseconds since track start
     */
    public long getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(long relativeTime) {
        this.relativeTime = relativeTime;
    }
}
