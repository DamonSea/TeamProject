//https://github.com/jinput/jinput/releases
//You need the Jar file added to the project

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

public class XboxControllerReader {

    public static void main(String[] args) {
        Controller xboxController = null;

        // Find the Xbox controller
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller controller : controllers) {
            if (controller.getType() == Controller.Type.GAMEPAD) {
                xboxController = controller;
                break;
            }
        }

        if (xboxController == null) {
            System.out.println("Xbox Controller not found!");
            System.exit(0);
        }

        System.out.println("Found controller: " + xboxController.getName());

        // Polling loop
        while (true) {
            // Poll the controller
            xboxController.poll();

            // Get all events
            EventQueue queue = xboxController.getEventQueue();
            Event event = new Event();

            while (queue.getNextEvent(event)) {
                String componentName = event.getComponent().getName();
                float value = event.getValue();

                // Print button presses
                if (value == 1.0f) {
                    System.out.println("Button Pressed: " + componentName);
                }

                // Print joystick movement (optional)
                if (value != 0.0f && !componentName.contains("Button")) {
                    System.out.printf("Moved %s to %.2f%n", componentName, value);
                }
            }

            // Sleep briefly to reduce CPU usage
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
