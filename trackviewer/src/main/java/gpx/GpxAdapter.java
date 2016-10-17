package gpx;

import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.jxmapviewer.viewer.GeoPosition;

import track.Track;
import track.TrackPoint;

import com.topografix.gpx._1._1.GpxType;
import com.topografix.gpx._1._1.ObjectFactory;
import com.topografix.gpx._1._1.TrkType;
import com.topografix.gpx._1._1.TrksegType;
import com.topografix.gpx._1._1.WptType;
import common.TrackCollectionReader;
import java.util.Date;
import track.TrackCollection;
import track.TrackSegment;
import track.Waypoint;

/**
 * Reads track data from .gpx files
 *
 * @author Martin Steiger
 */
public class GpxAdapter implements TrackCollectionReader {

    @Override
    public TrackCollection getTrackCollection(InputStream is) throws JAXBException, IOException {
        return read(is);
    }
    
    private final JAXBContext context;

    /**
     * @throws JAXBException occurs if ..
     * <ol>
     * <li>failure to locate either ObjectFactory.class or jaxb.index in the
     * packages</li>
     * <li>an ambiguity among global elements contained in the contextPath</li>
     * <li>failure to locate a value for the context factory provider
     * property</li>
     * <li>mixing schema derived packages from different providers on the same
     * contextPath</li>
     * </ol>
     */
    public GpxAdapter() throws JAXBException {
        Class<?> clazz = GpxType.class;
        String packageName = clazz.getPackage().getName();
        context = JAXBContext.newInstance(packageName);
    }

    /**
     * @param is the input stream
     * @return the track data
     * @throws IOException if the data cannot be read
     */
    public TrackCollection read(InputStream is) throws IOException {
        GpxType gpx;
        try {
            gpx = unmarshallObject(is);
        } catch (JAXBException e) {
            throw new IOException("Error parsing inputstream", e);
        }

        TrackCollection trackCollection = new TrackCollection();
        
        try {
            trackCollection.setName(gpx.getMetadata().getName());
        } catch (NullPointerException ex) {
        }
        
        for (TrkType trk : gpx.getTrk()) {
            Track track = new Track();
            track.setName(trk.getName());
            
            for (TrksegType seg : trk.getTrkseg()) {
                TrackSegment ts = new TrackSegment();

                for (WptType pt : seg.getTrkpt()) {
                    double ele;
                    double lat = pt.getLat().doubleValue();
                    double lon = pt.getLon().doubleValue();
                    if(pt.getEle() != null) {
                        ele = pt.getEle().doubleValue();
                    } else {
                        ele = Double.NaN;
                    }
                    Date cal = null;
                    try {
                        cal = pt.getTime().toGregorianCalendar().getTime();
                    } catch (NullPointerException ex) {
                    }
                    GeoPosition pos = new GeoPosition(lat, lon);

                    TrackPoint tp = new TrackPoint(pos, cal);

                    tp.setElevation(ele);
                    ts.addPoint(tp);
                }

                track.addSegment(ts);
            }
            
            trackCollection.addTrack(track);
        }
        
        for(WptType wpt: gpx.getWpt()) {
            Waypoint wp = new Waypoint(new GeoPosition(wpt.getLat().doubleValue(), wpt.getLon().doubleValue()));
            wp.setName(wpt.getName());
            try {
                wp.setElevation(wpt.getEle().doubleValue());
            } catch (NullPointerException ex) {
                wp.setElevation(Double.NaN);
            }
            wp.setDescription(wpt.getDesc());
            trackCollection.addWaypoint(wp);
        }

        return trackCollection;
    }

    private GpxType unmarshallObject(InputStream is) throws JAXBException {
        Unmarshaller unmarshaller = context.createUnmarshaller();

        JAXBElement<GpxType> jaxbObject = (JAXBElement<GpxType>) unmarshaller.unmarshal(is);
        return jaxbObject.getValue();
    }

    /**
     * @param os the output stream
     * @param tracks the list of tracks
     * @throws IOException if the data cannot be read
     */
    public void write(OutputStream os, TrackCollection tracks) throws IOException {
        DatatypeFactory factory;
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }

        GpxType gpx = new GpxType();

        for (Track track : tracks.getTracks()) {
            TrkType trk = new TrkType();
            
            for (TrackSegment tracksegment : track.getSegments()) {
                TrksegType seg = new TrksegType();

                for (TrackPoint pt : tracksegment.getPoints()) {
                    WptType wpt = new WptType();

                    wpt.setLat(BigDecimal.valueOf(pt.getPos().getLatitude()));
                    wpt.setLon(BigDecimal.valueOf(pt.getPos().getLongitude()));
                    wpt.setEle(BigDecimal.valueOf(pt.getElevation()));

                    if (pt.getTime() != null) {
                        GregorianCalendar cal = new GregorianCalendar();
                        cal.setTime(pt.getTime());
                        wpt.setTime(factory.newXMLGregorianCalendar(cal));
                    }
                    
                    seg.getTrkpt().add(wpt);
                }
                trk.getTrkseg().add(seg);
            }
            
            gpx.getTrk().add(trk);
        }

        try {
            marshallObject(os, gpx);
        } catch (JAXBException e) {
            throw new IOException("Error writing outputstream", e);
        }

    }

    private static <T> void marshallObject(OutputStream os, GpxType value) throws JAXBException {
        String packageName = value.getClass().getPackage().getName();
        JAXBContext context = JAXBContext.newInstance(packageName);
        ObjectFactory of = new ObjectFactory();

        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(of.createGpx(value), os);
    }

}
