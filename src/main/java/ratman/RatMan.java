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
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Rat-Man");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            showMenu();
            frame.setLocationRelativeTo(null);
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
        frame.revalidate();
        frame.repaint();
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
