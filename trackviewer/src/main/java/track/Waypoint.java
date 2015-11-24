package track;

import org.jxmapviewer.viewer.GeoPosition;

/**
 * A waypoint
 *
 * @author Martin Steiger
 */
public class Waypoint {

    private String name;
    private String description;
    private double elevation;
    private final GeoPosition pos;

    /**
     * @param pos the position
     */
    public Waypoint(GeoPosition pos) {
        this.pos = pos;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    
    public GeoPosition getPos() {
        return pos;
    }
}
