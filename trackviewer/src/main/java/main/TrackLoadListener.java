package main;

import track.TrackCollection;

/**
 * A notification callback for loaded tracks.
 *
 * Be prepared, that all methods are called off the EDT!
 * 
 * @author Martin Steiger
 */
public interface TrackLoadListener {
    public void startReading();
    public void finishReading();

    /**
     * @param track the track
     */
    public void trackLoaded(TrackCollection track);
    
    /**
     * Called in case of an error.
     * 
     * @param message
     * @param ex 
     */
    public void reportError(String message, Exception ex);
}
