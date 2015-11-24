package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.AbstractPainter;
import track.TrackPoint;
import track.Waypoint;

/**
 * Paints colored markers along the track
 *
 * @author Martin Steiger
 */
public class MarkerPainter extends AbstractPainter<JXMapViewer> {
    private static BufferedImage baseIcon;
    
    private synchronized static BufferedImage loadBaseIcon() {
        if(baseIcon == null) {
            try {
                baseIcon = ImageIO.read(MarkerPainter.class.getResource("/images/location-icon.png"));
            } catch (IOException ex) {
                throw new IllegalStateException("Can't load location-icon.png", ex);
            }
        }
        
        return baseIcon;
    }
    
    private BufferedImage icon;
            
    private final List<TrackPoint> track;
    private final List<Waypoint> waypoints;
    private final List<Integer> markers = new ArrayList<>();
    private final Color color;

    /**
     * @param track the track
     * @param color the color
     */
    public MarkerPainter(List<TrackPoint> track, Color color) {
        this(track, Collections.EMPTY_LIST, color);
    }
    
    public MarkerPainter(List<TrackPoint> track, List<Waypoint> waypoints, Color color) {
        this.color = color;
        this.track = track;
        this.waypoints = waypoints;
    }

    private BufferedImage getIcon() {
        if (this.icon == null) {
            BufferedImage image = loadBaseIcon();
            
            BufferedImage newIcon = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

            newIcon.createGraphics().drawImage(image, 0, 0, null);

            int width = newIcon.getWidth();
            int height = newIcon.getHeight();
            WritableRaster raster = newIcon.getRaster();

            for (int xx = 0; xx < width; xx++) {
                for (int yy = 0; yy < height; yy++) {
                    int[] pixels = raster.getPixel(xx, yy, (int[]) null);
                    pixels[0] = Math.max(pixels[0] - (255 - color.getRed()), 0);
                    pixels[1] = Math.max(pixels[1] - (255 - color.getGreen()), 0);
                    pixels[2] = Math.max(pixels[2] - (255 - color.getBlue()), 0);
                    raster.setPixel(xx, yy, pixels);
                }
            }

            this.icon = newIcon;
        }
        
        return this.icon;
    }
    
    /**
     * Clears all markers
     */
    public void clearMarkers() {
        markers.clear();
    }

    /**
     * Adds a marker at the specified index
     *
     * @param index the index in the data
     */
    public void addMarker(int index) {
        if (index < 0 || index > track.size()) {
            throw new IllegalArgumentException("Pos " + index + " not in track");
        }

        markers.add(index);

        setDirty(true);
    }

    @Override
    public void doPaint(Graphics2D g, JXMapViewer map, int unused1, int unused2) {
        // incorporate zoom to some extent
        int width = Math.max(1, 10 - map.getZoom() * 2);

        // do the drawing again
        g.setColor(color);
        g.setStroke(new BasicStroke(width + 2));

        draw(g, map, 5 * width);
        
        if (!waypoints.isEmpty()) {
            Image drawIcon = getIcon();
            int iconWidth = drawIcon.getWidth(null);
            int iconHeight = drawIcon.getHeight(null);
            for (Waypoint wp : waypoints) {
                Point2D p = map.convertGeoPositionToPoint(wp.getPos());
                g.drawImage(
                        drawIcon,
                        (int) p.getX() - (iconWidth / 2),
                        (int) p.getY() - iconHeight, 
                        null);
            }
        }
    }

    private void draw(Graphics2D g, JXMapViewer map, double len) {
        for (Integer idx : markers) {
            GeoPosition gp = track.get(idx).getPos();
            Point2D p = map.convertGeoPositionToPoint(gp);
            Point2D dir = getDirection(idx, map);

            if (dir != null) {
                Point2D n = new Point2D.Double(dir.getY(), -dir.getX());

                g.drawLine(
                        (int) (p.getX() - n.getX() * len), (int) (p.getY()
                        - n.getY() * len),
                        (int) (p.getX() + n.getX() * len), (int) (p.getY()
                        + n.getY() * len));
            }
        }
    }

    private Point2D getDirection(int index, JXMapViewer map) {
        int range = 1;
        double distSq = 0;

        double dx = 0;
        double dy = 0;

        while (distSq < 50 && range < 20) {
            // compute direction from [-ran^ge..range] around index
            int lowBound = Math.max(index - range, 0);
            int highBound = Math.min(index + range, track.size() - 1);

            range++;

            GeoPosition gpHigh = track.get(highBound).getPos();
            GeoPosition gpLow = track.get(lowBound).getPos();

            Point2D ptHigh = map.convertGeoPositionToPoint(gpHigh);
            Point2D ptLow = map.convertGeoPositionToPoint(gpLow);

            dx = ptHigh.getX() - ptLow.getX();
            dy = ptHigh.getY() - ptLow.getY();

            distSq = dx * dx + dy * dy;

            if (lowBound == 0 && highBound == track.size() - 1) {
                break;		// this is as good as it gets
            }
        }

        if (Math.abs(distSq) < 0.01) {
            return null;
        }

        double dist = Math.sqrt(distSq);

        return new Point2D.Double(dx / dist, dy / dist);
    }

    /**
     * Return the list of route positions as unmodifiable list
     *
     * @return the route
     */
    public List<TrackPoint> getRoute() {
        return Collections.unmodifiableList(track);
    }
}
