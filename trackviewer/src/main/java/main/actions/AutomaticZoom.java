package main.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.MNEMONIC_KEY;
import javax.swing.KeyStroke;
import main.MapViewer;

public class AutomaticZoom extends AbstractAction {
    private MapViewer viewer;
    
    public AutomaticZoom(MapViewer viewer) {
        super("Automatic Zoom");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
        putValue(MNEMONIC_KEY, (int) 'A');
        this.viewer = viewer;
        viewer.addPropertyChangeListener("fitViewportOnChange", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                AutomaticZoom.this.putValue(Action.SELECTED_KEY, (Boolean) evt.getNewValue());
            }
        });
        this.putValue(Action.SELECTED_KEY, viewer.isFitViewportOnChange());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        viewer.setFitViewportOnChange(! viewer.isFitViewportOnChange());
    }
}
