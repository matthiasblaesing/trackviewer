package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;

/**
 * Paints a route
 *
 * @author Martin Steiger
 */
public class RoutePainter implements Painter<JXMapViewer> {

    private Color color;

    private List<GeoPosition> track;

    /**
     * @param track the track
     */
    public RoutePainter(List<GeoPosition> track) {
        this(track, Color.RED);
    }

    /**
     * @param track the track
     * @param color the color
     */
    public RoutePainter(List<GeoPosition> track, Color color) {
	// copy the list so that changes in the 
        // original list do not have an effect here
        this.track = new ArrayList<>(track);
        this.color = color;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        g = (Graphics2D) g.create();

        // convert from viewport to world bitmap
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // incorporate zoom to some extent
        int width = Math.max(1, 10 - map.getZoom() * 2);

        Stroke routeStroke = new BasicStroke(width + 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        
        // do the drawing again
        g.setColor(color);
        g.setStroke(routeStroke);

        drawRoute(g, map);

        g.dispose();
    }

    /**
     * @param g the graphics object
     * @param map the map
     */
    private void drawRoute(Graphics2D g, JXMapViewer map) {
        int points = track.size();
        int[] xPoints = new int[points];
        int[] yPoints = new int[points];

        for (int i = 0; i < points; i++) {
            GeoPosition gp = track.get(i);
            
            // convert geo-coordinate to world bitmap pixel
            Point2D pt = map.getTileFactory().geoToPixel(gp, map.getZoom());

            xPoints[i] = (int) pt.getX();
            yPoints[i] = (int) pt.getY();
        }
        
        g.drawPolyline(xPoints, yPoints, points);
    }
}