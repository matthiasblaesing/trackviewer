package main;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.GeoUtils;
import java.util.ArrayList;

import track.Track;
import track.TrackPoint;
import track.TrackSegment;

/**
 * TODO Type description
 *
 * @author Martin Steiger
 */
public class TrackComputer {

    private static final Log log = LogFactory.getLog(TrackComputer.class);

    /**
     * @param track fill track with missing data
     */
    public static void repairTrackData(Track track) {
        fixInvalidElevations(track);
        fixDistances(track);
        fixTimes(track);
        computeSpeed(track);
    }

    private static void fixInvalidElevations(Track track) {
        TrackPoint lastValidPoint = null;
        TrackPoint nextValidPoint = null;

        for(TrackSegment ts: track.getSegments()) {
            for (int i = 0; i < ts.getPoints().size(); i++) {
                TrackPoint pt = ts.getPoints().get(i);

                if (Double.isNaN(pt.getElevation())) {
                    for (int j = i + 1; j < ts.getPoints().size(); j++) {
                        TrackPoint pt2 = ts.getPoints().get(j);
                        double nextEle = pt2.getElevation();
                        if (!Double.isNaN(nextEle)) {
                            nextValidPoint = pt2;
                            break;
                        }
                    }

                    if (lastValidPoint != null && nextValidPoint != null) {
                        long lastTime = lastValidPoint.getTime().getTime();
                        long nextTime = nextValidPoint.getTime().getTime();
                        long time = pt.getTime().getTime();

                        double ipol = (time - lastTime) / (double) (nextTime
                                - lastTime);

                        double lastEle = lastValidPoint.getElevation();
                        double nextEle = nextValidPoint.getElevation();

                        double ele = (1.0 - ipol) * lastEle + ipol * nextEle;

                        pt.setElevation(ele);
                    } else {
                        log.warn("Could not compute elevation");
                    }
                } else {
                    lastValidPoint = pt;
                }
            }
        }
    }

    private static void fixTimes(Track track) {
        List<TrackPoint> points = new ArrayList<>();

        for(TrackSegment ts: track.getSegments()) {
            points.addAll(ts.getPoints());
        }
        
        if (points.isEmpty()) {
            return;
        }

        long start = points.get(0).getTime().getTime();

        for (TrackPoint point : points) {
            long time = point.getTime().getTime();
            point.setRelativeTime(time - start);
        }
    }

    private static void fixDistances(Track track) {
        List<TrackPoint> points = new ArrayList<>();

        for(TrackSegment ts: track.getSegments()) {
            points.addAll(ts.getPoints());
        }

        if (points.isEmpty()) {
            return;
        }

        TrackPoint prevPoint = null;

        for (TrackPoint point : points) {
            if(prevPoint != null) {
                double delta = GeoUtils.computeDistance(prevPoint.getPos(), point.getPos());
                double dist = prevPoint.getDistance() + delta;
                point.setDistance(dist);
            } else {
                point.setDistance(0);
            }

            prevPoint = point;
        }
    }

    private static void computeSpeed(Track track) {
        final int range = 2;

        List<TrackPoint> points = new ArrayList<>();

        for(TrackSegment ts: track.getSegments()) {
            points.addAll(ts.getPoints());
        }

        for (int index = 0; index < points.size(); index++) {
            // compute speed from [-range..range] around index
            int lowBound = Math.max(index - range, 0);
            int highBound = Math.min(index + range, points.size() - 1);

            TrackPoint high = points.get(highBound);
            TrackPoint low = points.get(lowBound);

            double deltaDistance = high.getDistance() - low.getDistance();	     // meters
            long deltaTime = high.getTime().getTime() - low.getTime().getTime(); // milliseconds

            if (deltaTime != 0) {
                points.get(index).setSpeed(deltaDistance * 3600.0 / deltaTime);
            }
        }
    }
}
