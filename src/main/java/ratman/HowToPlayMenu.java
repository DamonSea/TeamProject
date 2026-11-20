package ratman;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Two-page guide: the basics, then an introduction to each cat. */
final class HowToPlayMenu extends JPanel {
    private static final int W = 560, H = 620;
    private static final int MARGIN = 28;
    private static final int ICON = 44;

    private static final Font HEADING = Assets.font("/fonts/GinkobRetroFont.ttf", 22f);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final Font BODY_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    /** One block on a page: an optional picture, a heading, and a paragraph. */
    private record Entry(BufferedImage icon, String heading, Color headingColor, String text) {}

    private final List<List<Entry>> pages = new ArrayList<>();
    private final String[] pageTitles = {"The Basics", "Meet the Cats", "Cat Tactics"};
    private int page;
    private final UiStyle.RetroButton prev;
    private final UiStyle.RetroButton next;

    HowToPlayMenu() {
        setPreferredSize(new Dimension(W, H));
        setLayout(null);
        buildPages();

        UiStyle.Title title = new UiStyle.Title("How to Play", Assets.font("/fonts/GinkobRetroFont.ttf", 52f));
        title.setBounds(0, 6, W, 80);
        add(title);

        Font buttonFont = Assets.font("/fonts/RetroNakjoy-Regular.otf", 22f);
        prev = UiStyle.RetroButton.cheese("< BACK", buttonFont);
        next = UiStyle.RetroButton.cheese("NEXT >", buttonFont);
        UiStyle.RetroButton menu = UiStyle.RetroButton.cheese("MENU", buttonFont);
        prev.setBounds(MARGIN, H - 66, 150, 50);
        menu.setBounds(W / 2 - 75, H - 66, 150, 50);
        next.setBounds(W - MARGIN - 150, H - 66, 150, 50);
        prev.addActionListener(e -> showPage(page - 1));
        next.addActionListener(e -> showPage(page + 1));
        menu.addActionListener(e -> RatMan.showMenu());
        add(prev);
        add(menu);
        add(next);
        showPage(0);
    }

    private void showPage(int p) {
        page = Math.max(0, Math.min(pages.size() - 1, p));
        prev.setVisible(page > 0);
        next.setVisible(page < pages.size() - 1);
        repaint();
    }

    private void buildPages() {
        Color yellow = UiStyle.CHEESE;
        BufferedImage rat = Skin.selected().icon();
        BufferedImage cheese = crop(Assets.image("/images/pelletcheese.png"), 0, 64);
        BufferedImage cats = Assets.image("/images/cats.png");
        BufferedImage scared = Assets.image("/images/cats_scared.png");

        pages.add(List.of(
                new Entry(rat, "Eat every dot", yellow,
                        "Clear all the dots in the maze to finish the level. Move with the ARROW KEYS or WASD "
                                + "(or a controller). Press P or ESC to pause. If a cat catches you, you lose a life."),
                new Entry(cheese, "Grab the big cheese", yellow,
                        "Eating a big cheese powers you up: the cats turn blue and run away, and you can eat them "
                                + "for 100 points. The bar at the bottom shows how long it lasts, and scared cats "
                                + "flash just before it runs out. Eaten cats float back to the cage as eyes."),
                new Entry(crop(scared, 0, 64), "Points", yellow,
                        "Dot: 10.   Big cheese: 50.   Scared cat: 100.   Your score carries over from level to level."),
                new Entry(null, "Tunnels", yellow,
                        "Run off the left or right edge of the maze to pop out on the other side. The cats "
                                + "can't follow you through."),
                new Entry(crop(Assets.image("/images/Boss.png"), 0, 100), "Every 4th level: the boss", new Color(255, 120, 120),
                        "The Three-Headed Cat is as fast as you and can't be eaten. Watch for its POUNCE: "
                                + "when it lines up with you it glows red and the lane lights up, then it dashes at "
                                + "double speed. Step out of the lane! If it hits a wall, it's dizzy for a moment.")
        ));

        pages.add(List.of(
                new Entry(crop(cats, 0, 30), "Alpha, the Chaser  (orange eyes)", new Color(255, 160, 60),
                        "The leader of the pack. Confident and reserved, Alpha never wastes a move: he locks on "
                                + "and takes the shortest path straight to you, around every corner, and he never "
                                + "gets lost. He hunts alone on level 1, and the others follow his lead."),
                new Entry(crop(cats, 2, 30), "Explorer, the Ambusher  (yellow eyes)", new Color(255, 225, 70),
                        "The smallest and scrappiest of the three. Explorer doesn't bother following you: "
                                + "Explorer dashes 4 tiles ahead of where you're running and cuts you off. Joins on "
                                + "level 2. Turn around or change direction to throw this one off."),
                new Entry(crop(cats, 4, 30), "Yoda, the Flanker  (green eyes)", new Color(130, 230, 110),
                        "The oldest and wisest. Patient and calm, Yoda never rushes in. He watches where Alpha "
                                + "is and quietly slips around to your other side to box you in. Joins on level 3. "
                                + "Don't get caught between them!")
        ));

        pages.add(List.of(
                new Entry(null, "Scatter and chase", yellow,
                        "Every so often the cats give up and scatter to their own corners for a few seconds. "
                                + "Use the break to grab dots. When they all turn around at once, they're coming back. "
                                + "Up close, every cat goes straight for you."),
                new Entry(null, "They get faster", yellow,
                        "Cats start at half your speed and speed up every level. Scared cats are slower; "
                                + "the eyes of an eaten cat rush home."),
                new Entry(crop(cats, 0, 30), "Beating Alpha", new Color(255, 160, 60),
                        "Alpha always comes the shortest way, so you know where he'll be. Lead him around a "
                                + "loop of walls and he'll follow you all the way round."),
                new Entry(crop(cats, 2, 30), "Beating Explorer", new Color(255, 225, 70),
                        "Explorer aims where you're going, not where you are. Head toward Explorer, then "
                                + "double back at the last second."),
                new Entry(crop(cats, 4, 30), "Beating Yoda", new Color(130, 230, 110),
                        "Yoda's plan needs Alpha. Use a side tunnel to put the whole maze between you and "
                                + "Alpha, and Yoda's trap falls apart.")
        ));
    }

    private static BufferedImage crop(BufferedImage sheet, int frameIndex, int frameW) {
        return Assets.cropToArt(sheet, frameIndex, frameW);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        UiStyle.paintBackground(g, getWidth(), getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // page header with dots for page position
        String header = pageTitles[page];
        g.setFont(HEADING);
        FontMetrics hm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(header, (W - hm.stringWidth(header)) / 2, 104);
        for (int i = 0; i < pages.size(); i++) {
            g.setColor(i == page ? UiStyle.CHEESE : new Color(255, 255, 255, 90));
            g.fillOval(W / 2 - pages.size() * 8 + i * 16 + 2, 112, 8, 8);
        }

        // card behind the text
        int top = 130, bottom = H - 80;
        g.setColor(new Color(20, 16, 30, 200));
        g.fillRoundRect(MARGIN - 10, top, W - 2 * MARGIN + 20, bottom - top, 18, 18);

        int y = top + 14;
        int textX = MARGIN + ICON + 14;
        int textW = W - textX - MARGIN;
        for (Entry e : pages.get(page)) {
            int blockTop = y;
            g.setFont(BODY_BOLD);
            g.setColor(e.headingColor());
            g.drawString(e.heading(), textX, y + 13);
            y += 18;
            g.setFont(BODY);
            g.setColor(new Color(235, 235, 240));
            FontMetrics fm = g.getFontMetrics();
            for (String line : wrap(e.text(), fm, textW)) {
                g.drawString(line, textX, y + 13);
                y += 17;
            }
            if (e.icon() != null) {
                int iconY = blockTop + Math.max(0, (y - blockTop - ICON) / 2);
                g.drawImage(e.icon(), MARGIN, iconY, ICON, ICON, null);
            }
            y += 12;
        }
    }

    private static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
