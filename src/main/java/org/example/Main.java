package org.example;


import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Launch the GUI on the Event Dispatch Thread for thread-safety
        SwingUtilities.invokeLater(() -> {
            MoodifyFrame frame = new MoodifyFrame();
            frame.setVisible(true);
        });
    }
}