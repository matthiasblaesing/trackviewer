package main.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import main.GapInserter;
import main.table.TrackTableModel;

public class InsertGapsAction extends AbstractAction {

    private static final long serialVersionUID = -3691668348789171952L;
    private final JTable table;

    public InsertGapsAction(JTable table) {
        super("Insert Gaps");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
        putValue(MNEMONIC_KEY, (int) 'G');
        this.table = table;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int[] idx = table.getSelectedRows();

        if (idx.length != 2) {
            return;
        }

        int idx1 = table.convertRowIndexToModel(idx[0]);
        int idx2 = table.convertRowIndexToModel(idx[1]);

        assert table.getModel() instanceof TrackTableModel;
        TrackTableModel ttm = (TrackTableModel) table.getModel();
        
        GapInserter.insertGaps(ttm.getTrack(idx1), ttm.getTrack(idx2));
    }
}
