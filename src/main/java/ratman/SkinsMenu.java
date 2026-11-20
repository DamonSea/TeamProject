package ratman;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.EnumMap;
import java.util.Map;

/** 2x2 grid: three skins plus a Back button. The chosen skin gets a yellow border. */
final class SkinsMenu extends JPanel implements Gamepad.Target {
    private static final int SIZE = 500;
    private static final int GAP = 16;
    private static final int CELL = (SIZE - 3 * GAP) / 2;
    private static final int ICON = 150;

    private final Map<Skin, UiStyle.RetroButton> buttons = new EnumMap<>(Skin.class);
    private final PadMenu pad;

    @Override
    public void padDirection(int dx, int dy) {
        pad.padDirection(dx, dy);
    }

    @Override
    public void padConfirm() {
        pad.padConfirm();
    }

    @Override
    public void padBack() {
        pad.padBack();
    }

    SkinsMenu() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setLayout(null);

        Font labelFont = Assets.font("/fonts/RetroNakjoy-Regular.otf", 24f);
        Skin[] skins = Skin.values();
        for (int i = 0; i < skins.length; i++) {
            Skin skin = skins[i];
            Image icon = skin.icon().getScaledInstance(ICON, ICON, Image.SCALE_FAST); // keep pixels crisp
            UiStyle.RetroButton b = new UiStyle.RetroButton(skin.displayName, new ImageIcon(icon),
                    UiStyle.PANEL, UiStyle.PANEL_HOVER, UiStyle.PANEL, Color.WHITE);
            b.setFont(labelFont);
            b.setVerticalTextPosition(SwingConstants.BOTTOM);
            b.setHorizontalTextPosition(SwingConstants.CENTER);
            b.setBounds(cellX(i), cellY(i), CELL, CELL);
            b.addActionListener(e -> {
                Skin.select(skin);
                updateSelection();
            });
            buttons.put(skin, b);
            add(b);
        }

        UiStyle.RetroButton back = UiStyle.RetroButton.cheese("BACK", Assets.font("/fonts/RetroNakjoy-Regular.otf", 34f));
        back.setBounds(cellX(3), cellY(3), CELL, CELL);
        back.addActionListener(e -> RatMan.showMenu());
        add(back);

        java.util.List<UiStyle.RetroButton> order = new java.util.ArrayList<>(buttons.values());
        order.add(back);
        pad = new PadMenu(2, RatMan::showMenu, order);

        updateSelection();
    }

    private static int cellX(int i) {
        return GAP + (i % 2) * (CELL + GAP);
    }

    private static int cellY(int i) {
        return GAP + (i / 2) * (CELL + GAP);
    }

    private void updateSelection() {
        buttons.forEach((skin, b) -> b.setChosen(skin == Skin.selected()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        UiStyle.paintBackground((Graphics2D) g, getWidth(), getHeight());
    }
}
