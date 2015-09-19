package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import main.actions.AutomaticZoom;
import main.actions.ExportTrackAction;
import main.actions.FixElevationAction;
import main.actions.InsertGapsAction;
import main.actions.QuitAction;

import main.chart.StatusBar;
import main.table.DistanceFormat;
import main.table.FormatRenderer;
import main.table.JShadedTable;
import main.table.SpeedFormat;
import main.table.TimeFormat;
import main.table.TrackTableModel;
import track.Track;

/**
 * A simple sample application that shows a OSM map of Europe
 *
 * @author Martin Steiger
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -9215006987029836062L;
    private MapViewer viewer;
    private JTable table;
    private final StatusBar statusBar;
    private final TrackChart trackChart;
    private final Action automaticZoomAction;
    private final Action exportTrackAction;
    private final Action fixElevationAction;
    private final Action insertGapsAction;
    private final Action quitAction;

    /**
     * Constructs a new instance
     */
    public MainFrame(String tracksdir, String apiKey) {
        super("TrackViewer");

        File folder;
        
        if(tracksdir == null) {
            folder = new File(System.getProperty("user.home") + File.separator
                + "trackviewer");
        } else {
            folder = new File(tracksdir);
        }

        viewer = new MapViewer();

        createTable();

        automaticZoomAction = new AutomaticZoom(viewer);
        exportTrackAction = new ExportTrackAction(table);
        fixElevationAction = new FixElevationAction(apiKey != null ? apiKey: "", table);
        insertGapsAction = new InsertGapsAction(table);
        quitAction = new QuitAction();
        
        TrackLoader.readTracks(folder, new TrackLoadListener() {
            @Override
            public void trackLoaded(final Track track) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ((TrackTableModel) table.getModel()).addTracks(track);
                    }
                });
            }
        });

        // put in a scrollpane to add scroll bars
        JScrollPane tablePane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        trackChart = new TrackChart();

        trackChart.addSelectionListener(new SelectionListener() {
            @Override
            public void selected(int series, int index) {
                viewer.setMarker(series, index);
            }
        });

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
                List<Track> selTracks = new ArrayList<>(indices.size());

                for (Integer idx : indices) {
                    idx = table.convertRowIndexToModel(idx);
                    selTracks.add(model.getTrack(idx));
                }

                viewer.showRoute(selTracks);
                trackChart.setTracks(selTracks);
            }
        });

        return table;
    }

    private JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        menu.add(exportTrackAction);
        menu.addSeparator();
        menu.add(fixElevationAction);
        menu.add(insertGapsAction);
        menu.addSeparator();
        menu.add(quitAction);
        
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');
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
