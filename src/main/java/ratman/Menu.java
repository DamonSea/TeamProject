package ratman;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/** Title screen with START and SKINS buttons. */
final class Menu extends JPanel {
    static final Color BUTTON_BLUE = new Color(120, 200, 255);

    Menu() {
        setPreferredSize(new Dimension(500, 470));
        setBackground(new Color(200, 200, 200));
        setLayout(null);

        JLabel title = new JLabel("Rat-Man");
        title.setFont(Assets.font("/fonts/GinkobRetroFont.ttf", 100f));
        title.setForeground(Color.BLACK);
        title.setBounds(50, 80, 500, 100);
        add(title);

        Font buttonFont = Assets.font("/fonts/RetroNakjoy-Regular.otf", 30f);
        add(button("START", buttonFont, 230, RatMan::startGame));
        add(button("SKINS", buttonFont, 310, RatMan::showSkins));
    }

    private static JButton button(String label, Font font, int y, Runnable action) {
        JButton b = new JButton(label);
        b.setBounds(160, y, 180, 60);
        b.setFont(font);
        b.setFocusable(false);
        b.setBackground(BUTTON_BLUE);
        b.setForeground(Color.WHITE);
        b.addActionListener(e -> action.run());
        return b;
    }
}
