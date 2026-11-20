package ratman;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Settings screen: maze style and sound, each a button that flips between two values. */
final class SettingsMenu extends JPanel implements Gamepad.Target {
    private static final int W = 500, H = 470;

    SettingsMenu() {
        setPreferredSize(new Dimension(W, H));
        setLayout(null);

        UiStyle.Title title = new UiStyle.Title("Settings", Assets.font("/fonts/GinkobRetroFont.ttf", 70f));
        title.setBounds(0, 20, W, 110);
        add(title);

        Font font = Assets.font("/fonts/RetroNakjoy-Regular.otf", 24f);
        UiStyle.RetroButton mazes = toggle("MAZES", "DESIGNED", "RANDOM", Settings::randomMazes, Settings::setRandomMazes, font, 125);
        UiStyle.RetroButton music = toggle("MUSIC", "ON", "OFF", () -> !Settings.musicOn(), off -> {
            Settings.setMusicOn(!off);
            SoundPlayer.musicSettingChanged(SoundPlayer.Track.MENU);
        }, font, 190);
        UiStyle.RetroButton sfx = toggle("SOUND FX", "ON", "OFF", () -> !Settings.sfxOn(), off -> Settings.setSfxOn(!off), font, 255);
        UiStyle.RetroButton rumble = toggle("RUMBLE", "ON", "OFF", () -> !Settings.rumbleOn(), off -> {
            Settings.setRumbleOn(!off);
            if (!off) Gamepad.rumble(0.6f, 0.6f, 250); // a little buzz so you can feel it's on
        }, font, 320);
        for (UiStyle.RetroButton b : new UiStyle.RetroButton[]{mazes, music, sfx, rumble}) add(b);

        UiStyle.RetroButton back = UiStyle.RetroButton.cheese("BACK", Assets.font("/fonts/RetroNakjoy-Regular.otf", 30f));
        back.setBounds(150, 392, 200, 60);
        back.addActionListener(e -> RatMan.showMenu());
        add(back);

        pad = new PadMenu(1, RatMan::showMenu, java.util.List.of(mazes, music, sfx, rumble, back));
    }

    private final PadMenu pad;

    @Override
    public void padDirection(int dx, int dy) {
        pad.padDirection(0, dy);
    }

    @Override
    public void padConfirm() {
        pad.padConfirm();
    }

    @Override
    public void padBack() {
        pad.padBack();
    }

    /**
     * A wide button reading e.g. "MAZES: DESIGNED"; clicking flips it.
     * {@code isSecond} tells whether the setting currently has its second value.
     */
    private static UiStyle.RetroButton toggle(String label, String first, String second,
                                              BooleanSupplier isSecond, Consumer<Boolean> setSecond,
                                              Font font, int y) {
        UiStyle.RetroButton b = new UiStyle.RetroButton("", null,
                UiStyle.PANEL, UiStyle.PANEL_HOVER, UiStyle.PANEL, Color.WHITE);
        b.setFont(font);
        Runnable refresh = () -> b.setText(label + ":  " + (isSecond.getAsBoolean() ? second : first));
        refresh.run();
        b.setBounds(70, y, 360, 56);
        b.addActionListener(e -> {
            setSecond.accept(!isSecond.getAsBoolean());
            refresh.run();
        });
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        UiStyle.paintBackground((Graphics2D) g, getWidth(), getHeight());
    }
}
