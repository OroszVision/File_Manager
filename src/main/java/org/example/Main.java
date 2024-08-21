package org.example;

import com.formdev.flatlaf.FlatDarculaLaf;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set the look and feel
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to apply FlatDarculaLaf: " + e.getMessage());
        }

        // Start the Swing application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            FileExplorerFrame frame = new FileExplorerFrame();
            frame.setVisible(true);
        });
    }
}
