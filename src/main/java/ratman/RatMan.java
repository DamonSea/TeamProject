package ratman;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.io.File;

/** Entry point: owns the window and swaps between the menu, skins and game screens. */
public final class RatMan {
    private static JFrame frame;
    private static GamePanel game;

    private RatMan() {}

    public static void main(String[] args) {
        configureControllerNatives();
        useSameLookOnEveryOs();
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Rat-Man");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            showMenu();
            frame.setVisible(true);
        });
    }

    static void showMenu() {
        show(new Menu());
    }

    static void showSkins() {
        show(new SkinsMenu());
    }

    static void startGame() {
        game = new GamePanel();
        show(game);
        game.start();
    }

    private static void show(JPanel panel) {
        if (game != null && panel != game) {
            game.stop();
            game = null;
        }
        frame.setContentPane(panel);
        frame.pack(); // size the window to the panel, so nothing is cut off by the title bar
        frame.setLocationRelativeTo(null); // re-centre, since the game screen is bigger than the menu
        frame.revalidate();
        frame.repaint();
    }

    /**
     * On macOS, Java's default button style ignores custom background colours, so the menu's
     * white button text was drawn on white buttons (blank START/SKINS). Using Java's own
     * cross-platform style makes the menus look the same on Mac and Windows.
     */
    private static void useSameLookOnEveryOs() {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }
    }

    /**
     * JInput needs its native libraries. `mvn compile` unpacks them to target/natives;
     * point JInput there when running from an IDE so controllers work without extra flags.
     */
    private static void configureControllerNatives() {
        String key = "net.java.games.input.librarypath";
        File natives = new File("target/natives");
        if (System.getProperty(key) == null && natives.isDirectory()) {
            System.setProperty(key, natives.getAbsolutePath());
        }
    }
}
