package ratman;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Shared look for the menu screens: grass background, cheese-coloured buttons, outlined title. */
final class UiStyle {
    static final Color CHEESE = new Color(255, 200, 45);
    static final Color CHEESE_HOVER = new Color(255, 222, 110);
    static final Color CHEESE_PRESSED = new Color(225, 165, 20);
    static final Color DARK = new Color(45, 30, 10);
    static final Color PANEL = new Color(40, 34, 52);
    static final Color PANEL_HOVER = new Color(62, 54, 80);

    private UiStyle() {}

    /** Grass tiles from the Outdoor theme with a dark gradient over them so text stays readable. */
    static void paintBackground(Graphics2D g, int width, int height) {
        BufferedImage tiles = Theme.OUTDOOR.tileSheet();
        int t = GamePanel.TILE;
        Random random = new Random(7); // fixed seed: same pattern every time
        for (int y = 0; y < height; y += t) {
            for (int x = 0; x < width; x += t) {
                int variant = Theme.OUTDOOR.pickVariant(random.nextDouble());
                g.drawImage(tiles, x, y, x + t, y + t, variant * t, 0, variant * t + t, t, null);
            }
        }
        g.setPaint(new GradientPaint(0, 0, new Color(10, 20, 10, 150), 0, height, new Color(10, 10, 30, 210)));
        g.fillRect(0, 0, width, height);
    }

    /** Big title text with a thick dark outline and a drop shadow. */
    static final class Title extends JComponent {
        private final String text;
        private final Font font;

        Title(String text, Font font) {
            this.text = text;
            this.font = font;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g.getFontMetrics(font);
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            GlyphVector glyphs = font.createGlyphVector(g.getFontRenderContext(), text);

            g.translate(x + 6, y + 6); // shadow
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(glyphs.getOutline());
            g.translate(-6, -6);
            g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(DARK);
            g.draw(glyphs.getOutline());
            g.setColor(CHEESE);
            g.fill(glyphs.getOutline());
            g.dispose();
        }
    }

    /** Rounded button that paints its own colours, so it looks the same on macOS and Windows. */
    static final class RetroButton extends JButton {
        private final Color base;
        private final Color hover;
        private final Color pressed;
        private boolean selected;

        RetroButton(String text, Icon icon, Color base, Color hover, Color pressed, Color textColor) {
            super(text, icon);
            this.base = base;
            this.hover = hover;
            this.pressed = pressed;
            setForeground(textColor);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        static RetroButton cheese(String text, Font font) {
            RetroButton b = new RetroButton(text, null, CHEESE, CHEESE_HOVER, CHEESE_PRESSED, DARK);
            b.setFont(font);
            return b;
        }

        /** Yellow highlight border (used for the chosen skin). */
        void setChosen(boolean chosen) {
            this.selected = chosen;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getModel().isPressed() ? pressed : getModel().isRollover() ? hover : base;
            int w = getWidth(), h = getHeight(), arc = 18;
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRoundRect(3, 5, w - 4, h - 5, arc, arc);          // shadow
            g.setColor(fill);
            g.fillRoundRect(0, 0, w - 4, h - 5, arc, arc);
            g.setStroke(new BasicStroke(selected ? 6 : 3));
            g.setColor(selected ? CHEESE : DARK);
            int inset = selected ? 3 : 1;
            g.drawRoundRect(inset, inset, w - 4 - 2 * inset, h - 5 - 2 * inset, arc, arc);
            g.dispose();
            super.paintComponent(g0); // text and icon
        }
    }
}
