package ratman;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.util.EnumMap;
import java.util.Map;

/** 2x2 grid: three skins plus a Back button. The selected skin gets a yellow border. */
final class SkinsMenu extends JPanel {
    private static final int CELL = 250;
    private final Map<Skin, JButton> buttons = new EnumMap<>(Skin.class);

    SkinsMenu() {
        setPreferredSize(new Dimension(2 * CELL, 2 * CELL));
        setLayout(null);
        setBackground(Color.LIGHT_GRAY);

        Skin[] skins = Skin.values();
        for (int i = 0; i < skins.length; i++) {
            Skin skin = skins[i];
            Image icon = skin.firstFrame().getScaledInstance(CELL, CELL, Image.SCALE_SMOOTH);
            JButton b = new JButton(new ImageIcon(icon));
            b.setToolTipText(skin.displayName);
            b.setBounds((i % 2) * CELL, (i / 2) * CELL, CELL, CELL);
            b.addActionListener(e -> {
                Skin.select(skin);
                updateSelection();
            });
            buttons.put(skin, b);
            add(b);
        }

        JButton back = new JButton("Back");
        back.setFont(new Font("Arial", Font.BOLD, 24));
        back.setBounds(CELL, CELL, CELL, CELL);
        back.setBackground(Menu.BUTTON_BLUE);
        back.setForeground(Color.BLACK);
        back.setOpaque(true);
        back.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        back.addActionListener(e -> RatMan.showMenu());
        add(back);

        updateSelection();
    }

    private void updateSelection() {
        buttons.forEach((skin, b) -> b.setBorder(skin == Skin.selected()
                ? BorderFactory.createLineBorder(Color.YELLOW, 6)
                : BorderFactory.createLineBorder(Color.DARK_GRAY, 1)));
    }
}
