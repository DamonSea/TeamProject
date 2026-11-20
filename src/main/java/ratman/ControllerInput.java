package ratman;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;

/**
 * Optional Xbox / gamepad support via JInput. If no controller (or no native library)
 * is found, {@link #poll} simply does nothing and the game is keyboard-only.
 */
final class ControllerInput {

    interface Listener {
        void onDirection(int dx, int dy);

        /** The Start/"Button 6" button: advance level or restart. */
        void onStart();
    }

    private static final float STICK_DEADZONE = 0.5f;

    private final Controller pad;

    private ControllerInput(Controller pad) {
        this.pad = pad;
    }

    static ControllerInput detect() {
        try {
            for (Controller c : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
                if (c.getType() == Controller.Type.GAMEPAD) {
                    System.out.println("Controller found: " + c.getName());
                    return new ControllerInput(c);
                }
            }
        } catch (Throwable t) {
            // UnsatisfiedLinkError (natives missing), NoClassDefFoundError, etc.
            System.err.println("Controller support unavailable: " + t);
        }
        System.out.println("No controller found. Running keyboard-only mode.");
        return new ControllerInput(null);
    }

    void poll(Listener listener) {
        if (pad == null || !pad.poll()) return;

        Event event = new Event();
        while (pad.getEventQueue().getNextEvent(event)) {
            Component comp = event.getComponent();
            String name = comp.getName();
            float value = event.getValue();

            if (name.equals("Button 6") && value == 1.0f) {
                listener.onStart();
            } else if (name.equals("Hat Switch")) {
                if (value == Component.POV.UP) listener.onDirection(0, -1);
                else if (value == Component.POV.RIGHT) listener.onDirection(1, 0);
                else if (value == Component.POV.DOWN) listener.onDirection(0, 1);
                else if (value == Component.POV.LEFT) listener.onDirection(-1, 0);
            } else if (comp.getIdentifier() == Component.Identifier.Axis.X) {
                if (value < -STICK_DEADZONE) listener.onDirection(-1, 0);
                else if (value > STICK_DEADZONE) listener.onDirection(1, 0);
            } else if (comp.getIdentifier() == Component.Identifier.Axis.Y) {
                if (value < -STICK_DEADZONE) listener.onDirection(0, -1);
                else if (value > STICK_DEADZONE) listener.onDirection(0, 1);
            }
        }
    }
}
