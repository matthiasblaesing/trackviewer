package main;

import track.Track;

/**
 * A notification callback for loaded tracks.
 *
 * Be prepared, that all methods are called off the EDT!
 * 
 * @author Martin Steiger
 */
public interface TrackLoadListener {

    /**
     * @param track the track
     */
    public void trackLoaded(Track track);

    /**
     * Called after all available tracks are read and the last trackLoaded call returns
     */
    public void finished();
    
    /**
     * Called in case of an error.
     * 
     * @param message
     * @param ex 
     */
    public void reportError(String message, Exception ex);
}
