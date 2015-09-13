package main.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import main.table.TrackTableModel;
import webservice.TrackElevationFixer;

public class FixElevationAction extends AbstractAction {

    private static final long serialVersionUID = -3691668348789171952L;
    private final JTable table;
    private final TrackElevationFixer fixer;

    public FixElevationAction(String apiKey, JTable table) {
        super("Fix Elevation");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
        putValue(MNEMONIC_KEY, (int) 'F');
        this.table = table;
        if(apiKey != null && (! apiKey.trim().isEmpty())) {
            this.fixer = new TrackElevationFixer(apiKey);
        } else {
            this.fixer = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return fixer != null;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        int idx = table.getSelectedRow();
        idx = table.convertRowIndexToModel(idx);

        assert table.getModel() instanceof TrackTableModel;
        TrackTableModel ttm = (TrackTableModel) table.getModel();
        
        fixer.fixTrack(ttm.getTrack(idx));
    }
}
