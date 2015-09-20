package main;

import gpx.GpxAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tcx.TcxAdapter;
import track.Track;

import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;

/**
 * Loads a series of track files from a folder in an asynchronous manner.
 *
 * @author Martin Steiger
 */
public class TrackLoader extends Thread {

    private static final Log log = LogFactory.getLog(TrackLoader.class);
    private File folder;
    private TrackLoadListener cb;
    
    private TrackLoader(final File folder, final TrackLoadListener cb) {
        this.folder = folder;
        this.cb = cb;
    }
    
    /**
     * @param folder the folder that contains the track files
     * @param cb the callback
     * @return TrackLoader Thread
     */
    public static TrackLoader readTracks(final File folder, final TrackLoadListener cb) {
        if(cb == null) {
            throw new NullPointerException("TrackLoadListener must be supplied");
        }
        if(cb == null) {
            throw new NullPointerException("Folder must be supplied");
        }
        TrackLoader tl = new TrackLoader(folder, cb);
        tl.start();
        return tl;
    }

    @Override
    public void run() {
        try {
            String[] files = folder.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tcx") || name.endsWith(".gpx");
                }
            });

            TcxAdapter tcxAdapter;
            GpxAdapter gpxAdapter;

            try {
                tcxAdapter = new TcxAdapter();
            } catch (JAXBException e) {
                cb.reportError("Error initializing TcxAdapter", e);
                log.error("Error initializing TcxAdapter", e);
                return;
            }
            try {
                gpxAdapter = new GpxAdapter();
            } catch (JAXBException e) {
                cb.reportError("Error initializing GpxAdapter", e);
                log.error("Error initializing GpxAdapter", e);
                return;
            }

            OUTER: for (String fname : files) {
                try(FileInputStream fis = new FileInputStream(new File(folder, fname))) {
                    exceptOnInterrupt();
                    if (fname.toLowerCase().endsWith(".tcx")) {
                        TrainingCenterDatabaseT data = tcxAdapter.unmarshallObject(fis);
                        List<Track> read = tcxAdapter.convertToTracks(data);

                        for (Track t : read) {
                            // skip empty tracks
                            if (!t.getPoints().isEmpty()) {
                                TrackComputer.repairTrackData(t);
                                exceptOnInterrupt();
                                cb.trackLoaded(t);
                            }
                        }
                    } else if (fname.toLowerCase().endsWith(".gpx")) {
                        List<Track> read = gpxAdapter.read(fis);
                        for (Track t : read) {
                            // skip empty tracks
                            if (!t.getPoints().isEmpty()) {
                                TrackComputer.repairTrackData(t);
                                exceptOnInterrupt();
                                cb.trackLoaded(t);
                            }
                        }
                    }

                    log.debug("Loaded " + fname);
                } catch (IOException | JAXBException e) {
                    String message = String.format("Failed to read '%s'.", fname);
                    cb.reportError(message, e);
                    log.error(message, e);
                } catch (InterruptedException ex) {
                    break OUTER;
                }
            }
        } finally {
            cb.finished();
        }
    }
    
    private void exceptOnInterrupt() throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
    }
}
}
