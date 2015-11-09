package webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jxmapviewer.viewer.GeoPosition;

import track.Track;
import track.TrackPoint;
import track.TrackSegment;

/**
 * Fixes all elevations in a .tcx file using the {@link ElevationFixer} class.
 *
 * @author Martin Steiger
 */
public class TrackElevationFixer {

    private static final Log log = LogFactory.getLog(TrackElevationFixer.class);

    private final ElevationFixer elevationFixer;

    public TrackElevationFixer(String apiKey) {
        this.elevationFixer = new ElevationFixer(apiKey);
    }
    
    /**
     * @param track the track to fix
     */
    public void fixTrack(Track track) {
        try {
            List<GeoPosition> positions = new ArrayList<>();
            List<TrackPoint> points = new ArrayList<>();
            
            for(TrackSegment ts: track.getSegments()) {
                positions.addAll(ts.getRoute());
                points.addAll(ts.getPoints());
            }
            
            List<Double> elevations = elevationFixer.getElevations(positions);

            for (int i = 0; i < points.size(); i++) {
                Double elevation = elevations.get(i);
                if (elevation < -32000) { // return value -32768 indicates unknown value
                    points.get(i).setElevation(Double.NaN);
                } else {
                    points.get(i).setElevation(elevation);
                }
            }

            log.info("Updated " + points.size() + " elevations");
        } catch (IOException e) {
            log.error("Error converting " + track, e);
        }
    }

}
