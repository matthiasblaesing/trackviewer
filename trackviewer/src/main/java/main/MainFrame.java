package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
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
    public MainFrame(String tracksdir) {
        super("TrackViewer");

        File folder;
        
        if(tracksdir == null) {
            folder = new File(System.getProperty("user.home") + File.separator
                + "trackviewer");
        } else {
            folder = new File(tracksdir);
        }

        final List<Track> tracks = new CopyOnWriteArrayList<>();

        viewer = new MapViewer();

        table = createTable(tracks);

        automaticZoomAction = new AutomaticZoom(viewer);
        exportTrackAction = new ExportTrackAction(table);
        fixElevationAction = new FixElevationAction(table);
        insertGapsAction = new InsertGapsAction(table);
        quitAction = new QuitAction();
        
        TrackLoader.readTracks(folder, new TrackLoadListener() {
            @Override
            public void trackLoaded(Track track) {
                tracks.add(track);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
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

        add(createMenu(tracks), BorderLayout.NORTH);
        add(mainSplitPane);
    }

    private JTable createTable(final List<Track> tracks) {
        TrackTableModel model = new TrackTableModel(tracks);

        final JTable table = new JShadedTable(model);

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
        sorter.toggleSortOrder(0);		// sorts ascending
        sorter.toggleSortOrder(0);		// sorts descending

        // Set selection model
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListMultiSelectionListener() {
            @Override
            public void valueChanged(List<Integer> indices) {
                List<Track> selTracks = new ArrayList<>();

                for (Integer idx : indices) {
                    idx = table.convertRowIndexToModel(idx);
                    selTracks.add(tracks.get(idx));
                }

                viewer.showRoute(selTracks);
                trackChart.setTracks(selTracks);
            }
        });

        return table;
    }

    private JMenuBar createMenu(List<Track> tracks) {
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
        String tracksdir = null;
        if(args.length > 0) {
            tracksdir = args[args.length - 1];
        }
        JFrame frame = new MainFrame(tracksdir);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 768);
        frame.setVisible(true);
    }
}
