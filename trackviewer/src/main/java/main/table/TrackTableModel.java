package main.table;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.SwingUtilities;

import javax.swing.table.AbstractTableModel;

import track.Track;
import track.TrackCollection;

/**
 * A table model for {@link Track}s
 *
 * @author Martin Steiger
 */
public final class TrackTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 819860756869723997L;
    private final List<TrackCollection> tracks = new ArrayList<>();
    private final Map<File,TrackCollection> fileTrackCollectionMap = new HashMap<>();
    private final Map<File,Long> readState = new HashMap<>();
    private final String[] columnIds = {"date", "distance", "time", "speed", "altitude", "comments"};
    private final String[] columnLabels = {"Date", "Distance (km)", "Time", "Avg. Speed (km/h)", "Altitude Diff. (m)", "Comments"};
    private final Class<?>[] columnClass = {Date.class, Double.class, Date.class, Double.class, Double.class, String.class};
    private final boolean[] columnEditable = {false, false, false, false, false, true};

    public TrackTableModel() {
    }

    /**
     * @return the columnLabels
     */
    public String[] getColumnLabels() {
        return columnLabels;
    }

    @Override
    public String getColumnName(int col) {
        return columnIds[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return columnClass[col];
    }

    @Override
    public int getRowCount() {
        return tracks.size();
    }

    @Override
    public int getColumnCount() {
        return columnIds.length - 1;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return columnEditable[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        TrackCollection track = tracks.get(row);

        switch (col) {
            case 0:
                return track.getStartTime();

            case 1:
                return track.getTotalDistance();

            case 2:
                return track.getTotalTime();

            case 3:
                return track.getAverageSpeed();

            case 4:
                return track.getTotalElevationDifference();

            case 5:
                return track.getName();
        }

        return track;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        assert SwingUtilities.isEventDispatchThread();
        TrackCollection track = tracks.get(row);

        switch (col) {
            case 5:
                track.setName(String.valueOf(value));
                break;
        }

        fireTableCellUpdated(row, col);
    }
    
    public void clear() {
        assert SwingUtilities.isEventDispatchThread();
        tracks.clear();
        fireTableDataChanged();
    }
    
    public void addTrack(File backingFile, TrackCollection track) {
        assert SwingUtilities.isEventDispatchThread();
        Objects.requireNonNull(backingFile);
        Objects.requireNonNull(track);
        if (this.fileTrackCollectionMap.containsKey(backingFile))  {
            this.removeTrack(backingFile);
        }
        this.tracks.add(track);
        this.fileTrackCollectionMap.put(backingFile, track);
        this.readState.put(backingFile, backingFile.lastModified());
        fireTableDataChanged();
    }
    
    public TrackCollection getTrackCollection(int rowIdx) {
        assert SwingUtilities.isEventDispatchThread();
        return tracks.get(rowIdx);
    }

    public Map<File,Long> getReadState() {
        assert SwingUtilities.isEventDispatchThread();
        return Collections.unmodifiableMap(this.readState);
    }

    public void removeTrack(File backingFile) {
        assert SwingUtilities.isEventDispatchThread();
        Objects.requireNonNull(backingFile);
        TrackCollection referencedCollection = this.fileTrackCollectionMap.remove(backingFile);
        this.tracks.remove(referencedCollection);
        this.readState.remove(backingFile);
        fireTableDataChanged();
    }
}
