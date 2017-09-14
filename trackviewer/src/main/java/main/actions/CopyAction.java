package main.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
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
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import main.MapViewer;
import main.TrackChart;

public class CopyAction extends AbstractAction {
    private final MapViewer viewer;
    private final TrackChart chart;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public CopyAction(MapViewer viewer, TrackChart chart) {
        super("Copy");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        putValue(MNEMONIC_KEY, (int) 'C');
        this.viewer = viewer;
        this.chart = chart;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Component focusedComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JComponent targetComponent;
        if(focusedComponent.equals(viewer) || viewer.isAncestorOf(focusedComponent)) {
            targetComponent = viewer;
        } else if (focusedComponent.equals(chart) || chart.isAncestorOf(focusedComponent)) {
            targetComponent = chart;
        } else {
            return;
        }
        BufferedImage image = new BufferedImage(
                targetComponent.getWidth(), 
                targetComponent.getHeight(), 
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setColor(targetComponent.getBackground());
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        targetComponent.paint(g2d);
        try {
            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            DataHandler dh = new DataHandler(baos.toByteArray(), "image/png");
            c.setContents(dh, null);
        } catch (IOException ex) {
            Logger.getLogger(CopyAction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
