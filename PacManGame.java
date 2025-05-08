import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import MazeGeneration.MazeGeneration;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

public class PacManGame {
    static JFrame frame;
    static Menu menuPanel;

    public static void main(String[] args) throws IOException, FontFormatException {

        // Set up the game window.
        frame = new JFrame("Rat-Man");

        // Main Menu
        menuPanel = new Menu();
        frame.add(menuPanel);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
    }

    public static void showMenu() throws IOException, FontFormatException {
        menuPanel = new Menu();
        frame.setContentPane(menuPanel);
        frame.setSize(500, 500);
        frame.revalidate();
        frame.repaint();
    }

    public static void showSkins() throws IOException {
        SkinsMenu skinsMenu = new SkinsMenu();
        frame.setContentPane(skinsMenu);
        frame.revalidate();
        frame.repaint();
    }

    public static void startGame() throws IOException {
        GamePanel panel = new GamePanel();
        frame.setContentPane(panel);
        frame.setSize(610, 635);
        panel.requestFocusInWindow();
        frame.revalidate();
        frame.repaint();
    }

    // Method to show the leaderboard window
    public static void showLeaderboard() {
        SwingUtilities.invokeLater(() -> {
            new LeaderboardWindow().setVisible(true);
        });
    }
}
