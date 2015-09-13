package main.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import main.TrackLoader;
import main.table.TrackTableModel;
import track.Track;

public class ExportTrackAction extends AbstractAction {

    private static final long serialVersionUID = -3691668348789171952L;
    private final JTable table;

    public ExportTrackAction(JTable table) {
        super("Export data");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        putValue(MNEMONIC_KEY, (int) 'E');
        this.table = table;
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
//		JDialog ...		
        try {
            TrackLoader.saveAsGpx("E:\\fixed.gpx", track);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
