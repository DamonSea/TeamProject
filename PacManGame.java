import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.Point;
import java.util.Random;
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

        //Main Menu
        menuPanel = new Menu();
        frame.add(menuPanel);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
    }

    public static void showSkins() throws IOException {
        frame.remove(menuPanel);
        SkinsMenu skinsMenu = new SkinsMenu();
        frame.add(skinsMenu);
        frame.revalidate();
    }

    public static void startGame() throws IOException {
        frame.remove(menuPanel);
        GamePanel panel = new GamePanel();
        frame.add(panel);
        frame.setSize(610, 635);
        panel.requestFocusInWindow();
    }

    // Method to show the leaderboard window
    public static void showLeaderboard() {
        SwingUtilities.invokeLater(() -> {
            new LeaderboardWindow().setVisible(true);
        });
    }
}

