package main.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.MNEMONIC_KEY;
import javax.swing.KeyStroke;
import main.MapViewer;

public class CopyMap extends AbstractAction {
    private final MapViewer viewer;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public CopyMap(MapViewer viewer) {
        super("Copy Map");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        putValue(MNEMONIC_KEY, (int) 'C');
        this.viewer = viewer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BufferedImage image = new BufferedImage(
                viewer.getWidth(), 
                viewer.getHeight(), 
                BufferedImage.TYPE_INT_RGB);
        viewer.paint(image.getGraphics());
        try {
            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            DataHandler dh = new DataHandler(baos.toByteArray(), "image/png");
            c.setContents(dh, null);
        } catch (IOException ex) {
            Logger.getLogger(CopyMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
