package org.example.ui;

import javax.swing.*;
import java.awt.*;

public class MoodifyFrame extends JFrame {
    private final JLabel moodLabel;

    public MoodifyFrame() {
        super("Moodify - Simple GUI");

        // Optional: use the system look and feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(480, 300));

        // Main content
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Title / question
        JLabel title = new JLabel("How are you feeling today?");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        content.add(title, BorderLayout.NORTH);

        // Mood label in the center
        moodLabel = new JLabel("Neutral", SwingConstants.CENTER);
        moodLabel.setOpaque(true);
        moodLabel.setBackground(new Color(245, 245, 245));
        moodLabel.setFont(moodLabel.getFont().deriveFont(Font.PLAIN, 24f));
        moodLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        content.add(moodLabel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        JButton happyBtn = new JButton("Happy");
        JButton calmBtn = new JButton("Calm");
        JButton sadBtn = new JButton("Sad");
        JButton resetBtn = new JButton("Reset");

        happyBtn.addActionListener(e -> setMood("Happy", new Color(255, 243, 176), new Color(199, 152, 12)));
        calmBtn.addActionListener(e -> setMood("Calm", new Color(197, 232, 255), new Color(13, 71, 161)));
        sadBtn.addActionListener(e -> setMood("Sad", new Color(230, 230, 250), new Color(81, 81, 81)));
        resetBtn.addActionListener(e -> setMood("Neutral", new Color(245, 245, 245), new Color(60, 60, 60)));

        buttons.add(happyBtn);
        buttons.add(calmBtn);
        buttons.add(sadBtn);
        buttons.add(resetBtn);
        content.add(buttons, BorderLayout.SOUTH);

        // Status bar
        JLabel status = new JLabel("Welcome to Moodify");
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(content, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null); // center on screen
    }

    private void setMood(String text, Color background, Color textColor) {
        moodLabel.setText(text);
        moodLabel.setBackground(background);
        moodLabel.setForeground(textColor);
    }
}

