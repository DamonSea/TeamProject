package ratman;

import java.util.List;

/**
 * Controller navigation for a screen of buttons laid out in a grid: the D-pad moves a white
 * focus ring between buttons, Cross/A presses the focused one, Circle/B goes back.
 */
final class PadMenu implements Gamepad.Target {
    private final List<UiStyle.RetroButton> buttons;
    private final int columns;
    private final Runnable back;
    private int focus = -1;

    PadMenu(int columns, Runnable back, List<UiStyle.RetroButton> buttons) {
        this.buttons = buttons;
        this.columns = Math.max(1, columns);
        this.back = back;
        // If a controller is already connected, show where the focus starts
        if (Gamepad.current().isConnected()) setFocus(0);
    }

    int focus() {
        return focus;
    }

    @Override
    public void padDirection(int dx, int dy) {
        if (focus < 0) {               // first press just shows the focus ring
            setFocus(0);
            return;
        }
        int rows = (buttons.size() + columns - 1) / columns;
        int col = focus % columns, row = focus / columns;
        col = Math.max(0, Math.min(columns - 1, col + dx));
        row = Math.max(0, Math.min(rows - 1, row + dy));
        int next = Math.min(buttons.size() - 1, row * columns + col);
        if (next != focus) {
            setFocus(next);
            SoundPlayer.play(SoundPlayer.Sound.CLICK);
        }
    }

    @Override
    public void padConfirm() {
        if (focus < 0) {
            setFocus(0);
            return;
        }
        buttons.get(focus).doClick();  // same as clicking it with the mouse
    }

    @Override
    public void padBack() {
        if (back != null) {
            SoundPlayer.play(SoundPlayer.Sound.CLICK);
            back.run();
        }
    }

    @Override
    public void padStart() {
        padConfirm();
    }

    private void setFocus(int index) {
        focus = index;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setPadFocus(i == index);
        }
    }
}
