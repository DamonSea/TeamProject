package ratman;

import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerUnpluggedException;

/**
 * Game controller support (PS5 DualSense, PS4, Xbox, Switch Pro, and most others) via Jamepad,
 * which wraps SDL, the same controller library most PC games use. SDL maps every controller to
 * one layout, so on a PlayStation pad Cross = A, Circle = B and Options = Start.
 *
 * {@link #poll()} is called ~60 times a second and turns button presses into simple actions that
 * the current screen handles through {@link Target}.
 */
final class Gamepad {

    /** A screen that can be driven with a controller. */
    interface Target {
        /** D-pad or left stick: fires once when pressed, then repeats while held. */
        void padDirection(int dx, int dy);

        /** Cross (PlayStation) / A (Xbox). */
        default void padConfirm() {}

        /** Circle (PlayStation) / B (Xbox). */
        default void padBack() {}

        /** Options (PlayStation) / Start (Xbox). */
        default void padStart() {}
    }

    /** The raw controller readings, so the logic below can be tested without hardware. */
    interface Source {
        boolean connected();

        boolean pressed(ControllerButton button);

        /** -1..1; for Y, up is positive (Jamepad's convention). */
        float axis(ControllerAxis axis);

        String name();

        void rumble(float left, float right, int millis);
    }

    private static final float STICK_DEADZONE = 0.5f;
    private static final long REPEAT_DELAY_NANOS = 350_000_000L;
    private static final long REPEAT_RATE_NANOS = 120_000_000L;

    /** The controller in use, or a do-nothing one if there's none (or the native library failed). */
    private static Gamepad current = new Gamepad(null);

    private final Source source;
    private int heldDx, heldDy;
    private long heldSince, lastRepeat;
    private boolean confirmWas, backWas, startWas;

    Gamepad(Source source) {
        this.source = source;
    }

    static Gamepad current() {
        return current;
    }

    static void install(Gamepad pad) {
        current = pad;
    }

    /** Starts SDL. Returns a gamepad that does nothing if controller support can't load. */
    static Gamepad start() {
        try {
            JamepadSource s = new JamepadSource();
            System.out.println("Controller support ready (plug in or pair a controller any time).");
            return new Gamepad(s);
        } catch (Throwable t) {
            // UnsatisfiedLinkError, IllegalStateException from SDL init, etc.
            System.err.println("Controller support unavailable: " + t);
            return new Gamepad(null);
        }
    }

    boolean isConnected() {
        return source != null && source.connected();
    }

    /** True for PlayStation controllers, so prompts can say Cross/Circle instead of A/B. */
    boolean isPlayStation() {
        if (!isConnected()) return false;
        String n = source.name();
        if (n == null) return false;
        n = n.toLowerCase();
        return n.contains("ps3") || n.contains("ps4") || n.contains("ps5") || n.contains("playstation")
                || n.contains("dualsense") || n.contains("dualshock") || n.contains("sony");
    }

    String confirmLabel() {
        return isPlayStation() ? "Cross" : "A";
    }

    String backLabel() {
        return isPlayStation() ? "Circle" : "B";
    }

    String startLabel() {
        return isPlayStation() ? "Options" : "Start";
    }

    /** Short rumble; does nothing without a controller (or if it can't vibrate). */
    static void rumble(float left, float right, int millis) {
        Gamepad pad = current;
        if (pad.isConnected() && Settings.rumbleOn()) {
            try {
                pad.source.rumble(left, right, millis);
            } catch (Throwable ignored) {
                // not every controller supports rumble
            }
        }
    }

    /** Reads the controller and sends actions to the target (if any). */
    void poll(Target target) {
        poll(target, System.nanoTime());
    }

    void poll(Target target, long now) {
        if (!isConnected()) {
            heldDx = heldDy = 0;
            confirmWas = backWas = startWas = false;
            return;
        }

        // direction: D-pad wins, otherwise the left stick's stronger axis
        int dx = 0, dy = 0;
        if (source.pressed(ControllerButton.DPAD_LEFT)) dx = -1;
        else if (source.pressed(ControllerButton.DPAD_RIGHT)) dx = 1;
        else if (source.pressed(ControllerButton.DPAD_UP)) dy = -1;
        else if (source.pressed(ControllerButton.DPAD_DOWN)) dy = 1;
        else {
            float sx = source.axis(ControllerAxis.LEFTX);
            float sy = source.axis(ControllerAxis.LEFTY);  // pushing up gives a negative value, same as the screen
            if (Math.max(Math.abs(sx), Math.abs(sy)) > STICK_DEADZONE) {
                if (Math.abs(sx) >= Math.abs(sy)) dx = sx > 0 ? 1 : -1;
                else dy = sy > 0 ? 1 : -1;
            }
        }

        if (dx != heldDx || dy != heldDy) {
            heldDx = dx;
            heldDy = dy;
            heldSince = lastRepeat = now;
            if ((dx != 0 || dy != 0) && target != null) target.padDirection(dx, dy);
        } else if ((dx != 0 || dy != 0) && now - heldSince >= REPEAT_DELAY_NANOS
                && now - lastRepeat >= REPEAT_RATE_NANOS) {
            lastRepeat = now;
            if (target != null) target.padDirection(dx, dy);
        }

        boolean confirm = source.pressed(ControllerButton.A);
        boolean back = source.pressed(ControllerButton.B);
        boolean start = source.pressed(ControllerButton.START);
        if (target != null) {
            if (confirm && !confirmWas) target.padConfirm();
            if (back && !backWas) target.padBack();
            if (start && !startWas) target.padStart();
        }
        confirmWas = confirm;
        backWas = back;
        startWas = start;
    }

    /** The real thing: reads the first connected controller through Jamepad / SDL. */
    private static final class JamepadSource implements Source {
        private final ControllerManager manager = new ControllerManager();
        private ControllerIndex active;
        private boolean lightBarSet;

        JamepadSource() {
            manager.initSDLGamepad();
        }

        @Override
        public boolean connected() {
            manager.update(); // pumps SDL events, picks up plugged / unplugged controllers
            if (active != null && active.isConnected()) return true;
            active = null;
            lightBarSet = false;
            for (int i = 0; i < 4; i++) {
                ControllerIndex c = manager.getControllerIndex(i);
                if (c.isConnected()) {
                    active = c;
                    announce(c);
                    return true;
                }
            }
            return false;
        }

        private void announce(ControllerIndex c) {
            try {
                System.out.println("Controller connected: " + c.getName());
            } catch (ControllerUnpluggedException ignored) {
                // gone again already
            }
            if (!lightBarSet) {
                lightBarSet = true;
                setLightBar(c, 255, 200, 40); // cheese yellow, on controllers with a light bar
            }
        }

        /** Light bar colour, if this Jamepad version offers it (looked up by name so it's optional). */
        private static void setLightBar(ControllerIndex c, int r, int g, int b) {
            for (String name : new String[]{"setLED", "setLedColor", "setLEDColor"}) {
                try {
                    c.getClass().getMethod(name, int.class, int.class, int.class).invoke(c, r, g, b);
                    return;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // try the next name
                }
            }
        }

        @Override
        public boolean pressed(ControllerButton button) {
            try {
                return active != null && active.isButtonPressed(button);
            } catch (ControllerUnpluggedException e) {
                active = null;
                return false;
            }
        }

        @Override
        public float axis(ControllerAxis axis) {
            try {
                return active == null ? 0 : active.getAxisState(axis);
            } catch (ControllerUnpluggedException e) {
                active = null;
                return 0;
            }
        }

        @Override
        public String name() {
            try {
                return active == null ? "" : active.getName();
            } catch (ControllerUnpluggedException e) {
                return "";
            }
        }

        @Override
        public void rumble(float left, float right, int millis) {
            try {
                if (active != null) active.doVibration(left, right, millis);
            } catch (ControllerUnpluggedException ignored) {
                // unplugged mid-rumble
            }
        }
    }
}
