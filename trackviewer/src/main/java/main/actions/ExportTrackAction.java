package main.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import main.TrackLoader;
import main.table.TrackTableModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import track.Track;

public class ExportTrackAction extends AbstractAction implements ListSelectionListener {
    private static final Log log = LogFactory.getLog(ExportTrackAction.class);    
    private static final long serialVersionUID = -3691668348789171952L;
    private final FileFilter gpxFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".gpx");
        }

        @Override
        public String getDescription() {
            return "GPX - GPS Exchange Format";
        }
    };
    private final JTable table;
    private File lastFolder;

    public ExportTrackAction(JTable table) {
        super("Export data");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        putValue(MNEMONIC_KEY, (int) 'E');
        this.table = table;
        this.table.getSelectionModel().addListSelectionListener(this);
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int idx = table.getSelectedRow();
        idx = table.convertRowIndexToModel(idx);

        assert table.getModel() instanceof TrackTableModel;
        TrackTableModel ttm = (TrackTableModel) table.getModel();
        
        exportToFile(ttm.getTrack(idx));
    }
    
    private void exportToFile(Track track) {	
        try {
            JFileChooser fileChooser = new JFileChooser(lastFolder);
            fileChooser.addChoosableFileFilter(gpxFilter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showSaveDialog(null);
            if(result == JFileChooser.APPROVE_OPTION) {
                String fileName = fileChooser.getSelectedFile().getAbsolutePath();
                if(! fileName.toLowerCase().endsWith(".gpx")) {
                    fileName += ".gpx";
                }
                lastFolder = fileChooser.getSelectedFile().getParentFile();
                TrackLoader.saveAsGpx(fileName, track);
            }
        } catch (IOException e) {
            log.error("Failed to save track", e);
            JOptionPane.showMessageDialog(null,
                    e.getLocalizedMessage(), 
                    "Failed to save track",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        updateEnabledState();
    }
    
    private void updateEnabledState() {
        setEnabled(table.getSelectedRowCount() > 0);
    }

}
