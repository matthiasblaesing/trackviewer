package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import main.actions.AutomaticZoom;
import main.actions.CopyAction;
import main.actions.ExportTrackAction;
import main.actions.FixElevationAction;
import main.actions.QuitAction;
import main.actions.ReloadAction;

import main.chart.StatusBar;
import main.table.DistanceFormat;
import main.table.FormatRenderer;
import main.table.JShadedTable;
import main.table.SpeedFormat;
import main.table.TimeFormat;
import main.table.TrackTableModel;
import track.TrackCollection;

/**
 * A simple sample application that shows a OSM map of Europe
 *
 * @author Martin Steiger
 */
public class MainFrame extends JFrame {

    private static final Logger LOG = Logger.getLogger(MainFrame.class.getName());
    private static final long serialVersionUID = -9215006987029836062L;
    private MapViewer viewer;
    private JTable table;
    private final StatusBar statusBar;
    private final TrackChart trackChart;
    private final Action automaticZoomAction;
    private final Action exportTrackAction;
    private final Action fixElevationAction;
    private final Action quitAction;
    private final Action reloadTracks;
    private final Action copyMap;
    private volatile File tracksdir = null;

    private final TrackLoadListener tracklistener = new TrackLoadListener() {
        private Map<File,Long> knownReadState = Collections.emptyMap();

        @Override
        public boolean doRead(File file) {
            Long mtime = knownReadState.remove(file);
            return mtime == null || file.lastModified() > mtime;
        }

        @Override
        public void startReading() {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    this.knownReadState = new HashMap<>(((TrackTableModel) table.getModel()).getReadState());
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void finishReading() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for(File f: knownReadState.keySet()) {
                        ((TrackTableModel) table.getModel()).removeTrack(f);
                    }
                }
            });
        }

        @Override
        public void trackLoaded(final File file, final TrackCollection trackCollection) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ((TrackTableModel) table.getModel()).addTrack(file, trackCollection);
                }
            });
        }

        @Override
        public void reportError(final String message, final Exception ex) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    if(message != null) {
                        sb.append(message);
                    }
                    if(ex != null) {
                        if(sb.length() > 0) {
                            sb.append("\n\n");
                        }
                        sb.append(ex.getLocalizedMessage());
                    }
                    String message = sb.toString();
                    LOG.log(Level.WARNING, message);
                    JOptionPane.showMessageDialog(null, message);
                }
            });
        }
    };
    
    private volatile TrackLoader trackLoader = new TrackLoader(tracklistener);
    
    /**
     * Constructs a new instance
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public MainFrame(String tracksdir, String apiKey) {
        super("TrackViewer");

        viewer = new MapViewer();
        trackChart = new TrackChart();

        trackChart.addSelectionListener(new SelectionListener() {
            @Override
            public void selected(ValueType axis, double value) {
                viewer.setMarker(axis, value);
            }
        });

        createTable();

        automaticZoomAction = new AutomaticZoom(viewer);
        copyMap = new CopyAction(viewer, trackChart);
        exportTrackAction = new ExportTrackAction(table);
        fixElevationAction = new FixElevationAction(apiKey != null ? apiKey: "", table);
        quitAction = new QuitAction();
        reloadTracks = new ReloadAction(this);
        
        setTracksdir(tracksdir == null ? null : new File(tracksdir));

        // put in a scrollpane to add scroll bars
        JScrollPane tablePane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);

        //Create the main split pane 
        JSplitPane chartSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, viewer, trackChart);
        chartSplitPane.setDividerLocation(550);

        //Create the main split pane 
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePane, chartSplitPane);
        mainSplitPane.setDividerLocation(230);

        //Provide minimum sizes for the two components in the split pane
        Dimension minimumSize = new Dimension(100, 50);
        tablePane.setMinimumSize(minimumSize);
        chartSplitPane.setMinimumSize(minimumSize);

        add(createMenu(), BorderLayout.NORTH);
        add(mainSplitPane);
    }

    public void setTracksdir(File tracksdir) {
        if(tracksdir == null) {
            this.tracksdir = new File(System.getProperty("user.home") + File.separator + "trackviewer");
        } else {
            this.tracksdir = tracksdir;
        }
        updateTracks();
    }
    
    public void updateTracks() {
        new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                trackLoader.readTracks(tracksdir);
                return null;
            }
        }.execute();
    }
    
    public boolean isLoading() {
        return trackLoader != null;
    }
    
    private JTable createTable() {
        final TrackTableModel model = new TrackTableModel();

        table = new JShadedTable(model);

	// Workaround to separate IDs from labels
        // By default, ID is not set or used by JTable
        // but the columnModel uses it. If not available it uses
        // the ID that is defined by the TableModel
        // So, the ID must be explicitly set for the columnModel to continue
        // to work.
        String[] labels = model.getColumnLabels();
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setIdentifier(model.getColumnName(i));
            table.getColumnModel().getColumn(i).setHeaderValue(labels[i]);
        }

        // set formatting of columns
        FormatRenderer dateRenderer = new FormatRenderer(SimpleDateFormat.getDateTimeInstance(), SwingConstants.LEFT);
        FormatRenderer distanceRenderer = new FormatRenderer(new DistanceFormat());
        FormatRenderer timeRenderer = new FormatRenderer(new TimeFormat());
        FormatRenderer speedRenderer = new FormatRenderer(new SpeedFormat());
        FormatRenderer altiRenderer = new FormatRenderer(new DecimalFormat("# m"));

        table.getColumn("date").setCellRenderer(dateRenderer);
        table.getColumn("distance").setCellRenderer(distanceRenderer);
        table.getColumn("time").setCellRenderer(timeRenderer);
        table.getColumn("speed").setCellRenderer(speedRenderer);
        table.getColumn("altitude").setCellRenderer(altiRenderer);

        // Set row sorter
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING)));

        // Set selection model
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListMultiSelectionListener() {
            @Override
            public void valueChanged(List<Integer> indices) {
                List<TrackCollection> selTracks = new ArrayList<>(indices.size());

                for (Integer idx : indices) {
                    idx = table.convertRowIndexToModel(idx);
                    selTracks.add(model.getTrackCollection(idx));
                }

                viewer.showRoute(selTracks);
                trackChart.setTracks(selTracks);

                if(selTracks.isEmpty()) {
                    statusBar.setExtra("");
                } else {
                    double totalDistance = 0;
                    long totalTime = 0;
                    for(TrackCollection tc: selTracks) {
                        totalDistance += tc.getTotalDistance();
                        totalTime += tc.getTotalTime();
                    }
                    statusBar.setExtra(String.format(
                        "\u2211 Distance: %.2f km, \u2211 Time: %d:%02d h, Average speed: %.2f km/h",
                        totalDistance / 1000d,
                        totalTime / (3600 * 1000),
                        (totalTime % (3600 * 1000)) / (60 * 1000),
                        totalDistance / totalTime * 3600
                    ));
                }
            }
        });

        return table;
    }

    private JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        menu.add(reloadTracks);
        menu.addSeparator();
        menu.add(exportTrackAction);
        menu.addSeparator();
        menu.add(fixElevationAction);
        menu.addSeparator();
        menu.add(quitAction);
        
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(editMenu);
        
        editMenu.add(copyMap);
        
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(viewMenu);
        
        viewMenu.add(new JCheckBoxMenuItem(automaticZoomAction));
        
        return menuBar;
    }


    /**
     * @param args the program args (ignored)
     */
    public static void main(String[] args) {
        OptionParser op = new OptionParser();
        op.acceptsAll(Arrays.asList(new String[]{"k", "apiKey"}), "MapQuest API Key to use for requests")
                .withRequiredArg()
                .ofType(String.class);
        op.acceptsAll(Arrays.asList(new String[]{"t", "tracksdir"}), "Track directory")
                .withRequiredArg()
                .ofType(String.class);
        op.nonOptions("First argument is recognised as trackdirectory (deprectated)");
                
        op.acceptsAll(Arrays.asList(new String[]{"h", "help"}), "Show help");
        
        
        try {
            final OptionSet optionset = op.parse(args);
            
            if(optionset.has("help")) {
                try {
                    op.printHelpOn(System.out);
                } catch (IOException ex1) {
                }
                System.exit(0);
            } else {
                final String tracksdir;
                if(optionset.has("tracksdir")) {
                    tracksdir = (String) optionset.valueOf("tracksdir");
                } else if (optionset.nonOptionArguments().size() > 0) {
                    tracksdir = (String) optionset.nonOptionArguments().get(0);
                } else {
                    tracksdir = null;
                }  
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JFrame frame = new MainFrame(tracksdir, (String) optionset.valueOf("apiKey"));
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        frame.setSize(1024, 768);
                        frame.setVisible(true);
                    }
                });
            }
        } catch (OptionException ex) {
            System.err.println(ex.getMessage());
            try {
                op.printHelpOn(System.err);
            } catch (IOException ex1) {}
            System.exit(1);
        }
    }
}
