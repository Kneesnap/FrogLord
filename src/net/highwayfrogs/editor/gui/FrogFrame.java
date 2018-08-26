package net.highwayfrogs.editor.gui;

import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class FrogFrame extends JFrame {
    private static final long serialVersionUID = -2253148347090771214L; // Just in case we get serialized (and getting rid of warnings)
    private GridLayout sideBySide = new GridLayout(1, 2);
    private JPanel leftPanel;
    
    public FrogFrame() {
        setLayout(sideBySide);
        add(leftPanel);
        add(new FrogCanvas());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // Clean up, check for save
                System.exit(0);
            }
        });
    }
}
