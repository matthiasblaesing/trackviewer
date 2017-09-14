package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.LocalResponseCache;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;

import track.Track;
import track.TrackCollection;
import track.TrackPoint;
import track.TrackSegment;
import track.Waypoint;

/**
 * A wrapper for the actual {@link JXMapViewer} component. It connects to the
 * typical application classes.
 *
 * @author Martin Steiger
 */
public class MapViewer extends JComponent {

    private static final long serialVersionUID = -1636285199192286728L;

    private final CompoundPainter<JXMapViewer> painter;

    private JXMapViewer mapViewer = new JXMapViewer();

    private final List<RoutePainter> routePainters = new ArrayList<>();
    private final List<MarkerPainter> markerPainters = new ArrayList<>();
    
    private boolean fitViewportOnChange = true;

    /**
     * Constructs a new instance
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public MapViewer() {
        // Create a TileFactoryInfo for OpenStreetMap
        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(8);
        mapViewer.setTileFactory(tileFactory);

        // Setup local file cache
        String baseURL = info.getBaseURL();
        File cacheDir = new File(System.getProperty("user.home")
                + File.separator + ".jxmapviewer2");
        LocalResponseCache.installResponseCache(baseURL, cacheDir, false);

        // Add interactions
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mapViewer.requestFocus();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mapViewer.requestFocus();
            }
            
            
        });

        painter = new CompoundPainter<>();
        mapViewer.setOverlayPainter(painter);

        GeoPosition frankfurt = new GeoPosition(50, 7, 0, 8, 41, 0);

        // Set the focus
        mapViewer.setZoom(10);
        mapViewer.setAddressLocation(frankfurt);

        setLayout(new BorderLayout());
        add(mapViewer, BorderLayout.CENTER);
    }

    /**
     * Displays one or more track routes
     *
     * @param tracks the list of track
     */
    public void showRoute(List<TrackCollection> tracks) {
        markerPainters.clear();
        routePainters.clear();

        Set<GeoPosition> positions = new HashSet<>();

        int i = 0;
        
        for (TrackCollection tc : tracks) {
            List<Waypoint> waypoints = new ArrayList<>(tc.getWaypoints());
            
            for (Track track : tc.getTracks()) {
                Color color = ColorProvider.getMainColor(i++);

                for (TrackSegment ts : track.getSegments()) {
                    List<GeoPosition> route = ts.getRoute();
                    positions.addAll(route);

                    MarkerPainter markerPainter = new MarkerPainter(ts.getPoints(), waypoints, color);
                    RoutePainter routePainter = new RoutePainter(route, color);

                    markerPainters.add(markerPainter);
                    routePainters.add(routePainter);
                    
                    waypoints = Collections.EMPTY_LIST;
                }
            }
        }

        List<Painter<JXMapViewer>> painters = new ArrayList<>();
        painters.addAll(routePainters);
        painters.addAll(markerPainters);
        painter.setPainters(painters);
        
        if(fitViewportOnChange) {
            mapViewer.zoomToBestFit(positions, 1F);
        }
    }

    /**
     * @param unit value type
     * @param value selected value
     */
    public void setMarker(ValueType unit, double value) {
        for (MarkerPainter mp: markerPainters) {
            mp.clearMarkers();

            List<TrackPoint> route = mp.getRoute();

            if(route.isEmpty()) {
                continue;
            }
            
            if (unit == ValueType.Time) {
                double startVal = route.get(0).getRelativeTime();
                if(value < startVal) {
                    continue;
                }
            } else if (unit == ValueType.Distance) {
                double startVal = route.get(0).getDistance();
                if (value < startVal) {
                    continue;
                }
            }
            
            if (unit == ValueType.Time || unit == ValueType.Distance) {
                for (int i = 1; i < (route.size() - 1); i++) {
                    double pointVal = Double.NaN;
                    double previousVal = Double.MIN_VALUE;
                    if (unit == ValueType.Time) {
                        pointVal = route.get(i).getRelativeTime();
                        previousVal = route.get(i - 1).getRelativeTime();
                    } else if (unit == ValueType.Distance) {
                        pointVal = route.get(i).getDistance();
                        previousVal = route.get(i - 1).getDistance();
                    }
                    
                    if (pointVal >= value) {
                        double distPrev = value - previousVal;
                        double distNext = pointVal - value;
                        if (distNext > distPrev) {
                            mp.addMarker(i - 1);
                            break;
                        } else if (distPrev >= distNext) {
                            mp.addMarker(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Determine whether the 
     * 
     * @return 
     */
    public boolean isFitViewportOnChange() {
        return fitViewportOnChange;
    }

    /**
     * When true, the viewport (zoom + position) will be adjusted to display the
     * whole track when the Tracks are changed.
     * 
     * @param fitViewportOnChange 
     */
    public void setFitViewportOnChange(boolean fitViewportOnChange) {
        boolean oldValue = isFitViewportOnChange();
        this.fitViewportOnChange = fitViewportOnChange;
        this.firePropertyChange("fitViewportOnChange", oldValue, fitViewportOnChange);
    }
}
