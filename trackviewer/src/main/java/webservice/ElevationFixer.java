package webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Retrieves a list of elevations from a web service
 *
 * @author Martin Steiger
 */
public class ElevationFixer {

    private static final Log log = LogFactory.getLog(ElevationFixer.class);

    private static final String url = "http://open.mapquestapi.com/elevation/v1/profile?useFilter=true";

    /**
     * This is just a guess that seems to work for MapQuest Elevation API v1 200
     * seems to be ok for raw 1500 seems to be ok for compressed 
     * (manual test was done with 1350 trackpoints)
     */
    private static final int chunkSize = 200;
    private static final int chunkSizeCompressed = 1500;

    /**
     * Format to use for request - if false raw format is used, if true cmp format is used
     */
    private final boolean useCompression;
    
    /**
     * API-Access Key for Mapquest API
     */
    private final String apiKey;

    public ElevationFixer(String apiKey) {
        this(true, apiKey);
    }

    
    public ElevationFixer(boolean useCompression, String apiKey) {
        this.useCompression = useCompression;
        this.apiKey = apiKey;
    }
    
    /**
     * Retrieves a list of elevations from a web service using google polyline
     * compression URLs.
     *
     * @param routeFull the route
     * @return the list of elevations
     * @throws IOException if something goes wrong
     */
    public List<Double> getElevations(List<GeoPosition> routeFull) throws IOException {
        int requestChunk = useCompression ? chunkSizeCompressed : chunkSize;
        
        int min = 0;
        int max = Math.min(routeFull.size(), min + requestChunk);

        List<Double> ele = new ArrayList<>();

        while (min < max) {
            System.out.println("Converting [" + min + ".." + max + "]");

            List<GeoPosition> route = routeFull.subList(min, max);

            String query;

            if (useCompression) {
                query = getCompressedQuery(route);
            } else {
                query = getRawQuery(route);
            }

            List<Double> result = queryElevations(query);

            if (result.size() != max - min) {
                throw new IllegalStateException("Elevation query returned only "
                        + result.size() + " instead of " + (max - min)
                        + " points");
            }

            ele.addAll(result);

            min = max;
            max = Math.min(routeFull.size(), min + requestChunk);
        }

        return ele;
    }

    private static String getCompressedQuery(List<GeoPosition> route) {
        final String s = "&shapeFormat=cmp&latLngCollection=";

        String compressed = PolylineEncoder.compress(route, 5);

        return s + compressed;
    }

    private static String getRawQuery(List<GeoPosition> route) {
        String s = "&shapeFormat=raw&latLngCollection=";

        for (GeoPosition pos : route) {
            s = s + pos.getLatitude();
            s = s + ",";
            s = s + pos.getLongitude();
            s = s + ",";
        }

        return s;
    }

    private List<Double> queryElevations(String s) throws IOException {
        try {
            String response = queryUrl(url + "&key=" + apiKey + s);

            handleInfo(response);

            List<Double> data = handleResponse(response);
            return data;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static String queryUrl(String string) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(string);

            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader in = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static void handleInfo(String source) throws JSONException {
        JSONObject obj = new JSONObject(source);

        JSONObject info = obj.getJSONObject("info");

        int code = info.getInt("statuscode");
        if (code != 0) {
            log.info(code);
        }

        JSONArray msgs = (JSONArray) info.get("messages");
        for (int i = 0; i < msgs.length(); i++) {
            log.info(msgs.getString(i));
        }

    }

    private static List<Double> handleResponse(String source) throws JSONException {
        JSONObject obj = new JSONObject(source);

        JSONArray arr = obj.getJSONArray("elevationProfile");

        List<Double> data = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj2 = (JSONObject) arr.get(i);
            data.add(obj2.getDouble("height"));
        }

        return data;
    }
}
