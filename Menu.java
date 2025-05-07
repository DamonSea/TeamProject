import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Menu extends JPanel
{
    public Menu()
    {
        this.setBackground(new Color(200, 200, 200));
        this.setLayout(null);

        //button design start game
        JButton button = new JButton();
        button.setBounds(160, 170, 180, 60);
        button.setText("START GAME");
        button.setFocusable(false);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PacManGame.startGame();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        this.add(button);

        // Add Leaderboardwindow
        JButton leaderboardButton = new JButton("VIEW LEADERBOARD");
        leaderboardButton.setBounds(160, 250, 180, 60);
        leaderboardButton.setFocusable(false);
        leaderboardButton.addActionListener(e -> {
            // Open leaderboard window
            SwingUtilities.invokeLater(() -> {
                new LeaderboardWindow().setVisible(true);
            });
        });
        this.add(leaderboardButton);

        // Add key listener to handle 'S' and 'L' key presses
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

                // Start the game if 'S' is pressed
                if (key == KeyEvent.VK_S) {
                    try {
                        PacManGame.startGame();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                // Show the leaderboard if 'L' is pressed
                if (key == KeyEvent.VK_L) {
                    SwingUtilities.invokeLater(() -> {
                        new LeaderboardWindow().setVisible(true);
                    });
                }
            }
        });
        setFocusable(true);  // Make sure the panel listens to key events
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the main menu
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Welcome to the Game!", 150, 100);
        g.drawString("Press 'S' to Start", 150, 350);
        g.drawString("Press 'L' to View Leaderboard", 150, 380);
    }
}