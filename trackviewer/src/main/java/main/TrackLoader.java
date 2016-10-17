package main;

import gpx.GpxAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tcx.TcxAdapter;
import track.Track;

import common.TrackCollectionReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import track.TrackCollection;

/**
 * Loads a series of track files from a folder in an asynchronous manner.
 *
 * @author Martin Steiger
 */
public class TrackLoader {

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Log log = LogFactory.getLog(TrackLoader.class);
    private TrackLoadListener cb;
    private List<Future<TrackCollection>> runningList = new ArrayList<>();

    public TrackLoader(TrackLoadListener cb) {
        this.cb = cb;
    }

    public void readTracks(final File folder) throws InterruptedException {
        stopLoading();
        
        cb.startReading();
        
        if(folder.canRead()) {
            for(File f: folder.listFiles()) {
                if(f.isFile() && (f.getName().toLowerCase().endsWith(".gpx")|| f.getName().toLowerCase().endsWith(".tcx"))) {
                    Callable<TrackCollection> trackCollection = new TrackReader(f);
                    runningList.add(executor.submit(trackCollection));
                }
            }
        }
        
        for(Future<TrackCollection> f: new ArrayList<>(runningList)) {
            if(! f.isCancelled()) {
                try {
                    cb.trackLoaded(f.get());
                } catch (ExecutionException ex) {
                    cb.reportError(ex.getMessage(), ex);
                }
            }
            runningList.remove(f);
        }
        
        cb.finishReading();
    }
    
    private void stopLoading() {
        for (Future f: new ArrayList<>(runningList)) {
            f.cancel(true);
            runningList.remove(f);
        }
    }

    private static class TrackReader implements Callable<TrackCollection> {

        private final File input;
        private 

        TrackReader(File input) {
            this.input = input;
        }
        
        @Override
        public TrackCollection call() throws Exception {
            try (InputStream is = new FileInputStream(input)) {
                TrackCollectionReader tcr = null;
                if (input.getName().toLowerCase().endsWith(".tcx")) {
                    tcr = new TcxAdapter();
                } else if (input.getName().toLowerCase().endsWith(".gpx")) {
                    tcr = new GpxAdapter();
                } else {
                    throw new IOException("Failed to find reader for file: "
                            + input.getName());
                }

                if (tcr == null) {
                    return null;
                }

                TrackCollection collection = tcr.getTrackCollection(is);
                for (Track t : collection.getTracks()) {
                    TrackComputer.repairTrackData(t);
                    exceptOnInterrupt();
                }
                return collection;
            }
        }

        private void exceptOnInterrupt() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }
}
