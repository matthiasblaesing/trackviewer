package main.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

public class QuitAction extends AbstractAction {

    public QuitAction() {
        super("Quit");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
        putValue(MNEMONIC_KEY, (int) 'Q');
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.exit(0);
    }

}
