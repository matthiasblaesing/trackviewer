package track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TrackCollection {
    private String name;
    private final List<Track> tracks = new ArrayList<>();
    private final List<Waypoint> waypoints = new ArrayList<>();

    public String getName() {
        if((name == null || "".equals(name.trim())) && tracks.size() > 0) {
            return tracks.get(0).getName();
        } else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @param track the track
     */
    public void addTrack(Track track) {
        tracks.add(track);
    }
    
    /**
     * @return an unmodifiable list of tracks
     */
    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }
    
    /**
     * @param point the waypoint
     */
    public void addWaypoint(Waypoint point) {
        waypoints.add(point);
    }

    /**
     * @return an unmodifiable list of waypoints
     */
    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
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
        for(Track track: tracks) {
            result += track.getTotalElevationDifference();
        }
        return result;
    }
    
    /**
     * @return the total distance of the track in meters
     */
    public double getTotalDistance() {
        double result = 0;
        for(Track track: tracks) {
            result += track.getTotalDistance();
        }
        return result;
    }
    
    /**
     * @return the total time of the track (in ms)
     */
    public long getTotalTime() {
        long result = 0;
        for(Track track: tracks) {
            result += track.getTotalTime();
        }
        return result;
    }
    
    public Date getStartTime() {
        for(Track track: tracks) {
            Date startTime = track.getStartTime();
            if(startTime != null) {
                return startTime;
            }
        }
        return null;
    }
}
