
package common;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBException;
import track.TrackCollection;

public interface TrackCollectionReader {
    public TrackCollection getTrackCollection(InputStream is) throws JAXBException, IOException;
}
