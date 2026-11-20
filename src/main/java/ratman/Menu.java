package ratman;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Title screen with START and SKINS buttons. */
final class Menu extends JPanel {
    private static final int W = 500, H = 470;
    private static final int PATH_Y = 400; // little chase scene along the bottom

    Menu() {
        setPreferredSize(new Dimension(W, H));
        setLayout(null);

        UiStyle.Title title = new UiStyle.Title("Rat-Man", Assets.font("/fonts/GinkobRetroFont.ttf", 100f));
        title.setBounds(0, 25, W, 140);
        add(title);

        Font buttonFont = Assets.font("/fonts/RetroNakjoy-Regular.otf", 30f);
        UiStyle.RetroButton start = UiStyle.RetroButton.cheese("START", buttonFont);
        start.setBounds(125, 180, 250, 60);
        start.addActionListener(e -> RatMan.startGame());
        add(start);

        UiStyle.RetroButton skins = UiStyle.RetroButton.cheese("SKINS", buttonFont);
        skins.setBounds(125, 250, 250, 60);
        skins.addActionListener(e -> RatMan.showSkins());
        add(skins);

        UiStyle.RetroButton settings = UiStyle.RetroButton.cheese("SETTINGS", buttonFont);
        settings.setBounds(125, 320, 250, 60);
        settings.addActionListener(e -> RatMan.showSettings());
        add(settings);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        UiStyle.paintBackground(g, getWidth(), getHeight());
        paintChase(g);
    }

    /** A strip of path with dots: a cat chasing the selected skin toward a piece of cheese. */
    private void paintChase(Graphics2D g) {
        int t = GamePanel.TILE;
        g.setColor(Theme.OUTDOOR.background);
        g.fillRect(0, PATH_Y, getWidth(), t + 10);
        g.setColor(Color.WHITE);
        for (int x = 15; x < getWidth(); x += t) {
            if (x > 120 && x < 330) continue; // eaten already
            g.fillOval(x + 5, PATH_Y + 15, 10, 10);
        }
        int y = PATH_Y - 2;
        BufferedImage cats = Assets.image("/images/cats.png");
        g.drawImage(cats, 120, y, 162, y + 42, 60, 0, 90, 30, null); // orange cat
        g.drawImage(Skin.selected().icon(), 250, y, 42, 42, null);
        g.drawImage(Assets.image("/images/pelletcheese.png"), 420, PATH_Y + 2, 30, 30, null);
    }
}
