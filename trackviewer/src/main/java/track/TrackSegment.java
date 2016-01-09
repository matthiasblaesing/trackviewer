
package track;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

public class TrackSegment {
    private final List<TrackPoint> points = new ArrayList<>();

    private final List<GeoPosition> route = new AbstractList<GeoPosition>() {
        @Override
        public GeoPosition get(int index) {
            return points.get(index).getPos();
        }

        @Override
        public int size() {
            return points.size();
        }

    };

    private Double altDiff;

    /**
     * @return an unmodifiable list of geo-positions
     */
    public List<GeoPosition> getRoute() {
        return route;		// read-only anyway
    }

    /**
     * @return an unmodifiable list of track points
     */
    public List<TrackPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    /**
     * @param point the track point
     */
    public void addPoint(TrackPoint point) {
        points.add(point);
    }

    /**
     * @return the average speed in km/h
     */
    public double getAverageSpeed() {
        double sum = 0;
        for (TrackPoint point : points) {
            sum += point.getSpeed();
        }

        return sum / points.size();
    }

    /**
     * @return the accumulated (ascending) elevation difference for the track segment
     */
    public double getTotalElevationDifference() {
        if (points.isEmpty()) {
            return 0;
        }

        if (altDiff == null) {
            double total = 0;

            double prevEle = points.get(0).getElevation();

            for (TrackPoint pt : points) {
                double ele = pt.getElevation();
                double delta = ele - prevEle;

                if (delta > 0) {
                    total += delta;
                }

                prevEle = ele;
            }

            altDiff = total;
        }

        return altDiff;
    }

    /**
     * @return the total distance of the track segment in meters
     */
    public double getTotalDistance() {
        if (points.isEmpty()) {
            return 0;
        }

        return points.get(points.size() - 1).getDistance() - points.get(0).getDistance();
    }

    /**
     * @return the total time of the track segment
     */
    public long getTotalTime() {
        if (points.size() < 2) {
            return 0;
        }
        try {
            return points.get(points.size() - 1).getTime().getTime()
                    - points.get(0).getTime().getTime();
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public Date getStartTime() {
        if(points.size() > 0) {
            return points.get(0).getTime();
        } else {
            return null;
        }
    }
}
