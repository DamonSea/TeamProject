package ratman;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

/** Plays short sound effects. Each clip is closed when it finishes so audio lines aren't leaked. */
public final class SoundPlayer {

    public enum Sound {
        INTRO("intro.wav"),
        WAKA("waka.wav"),
        POWER_UP("powerup.wav"),
        EAT_CAT("eatghost.wav"),
        DEATH("pacman_death.wav");

        final String path;

        Sound(String file) {
            this.path = "/sounds/" + file;
        }
    }

    private static final float VOLUME = 0.7f; // 0..1 of the gain range
    private static boolean enabled = true;

    private SoundPlayer() {}

    /** Turns sound off (used by tests / machines without audio). */
    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static void play(Sound sound) {
        if (!enabled || !Settings.soundOn()) return;
        try {
            byte[] data = Assets.bytes(sound.path);
            AudioInputStream audio = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new ByteArrayInputStream(data)));
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) clip.close();
            });
            clip.open(audio);

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gain.getMaximum() - gain.getMinimum();
                gain.setValue(gain.getMinimum() + range * VOLUME);
            }
            clip.start();
        } catch (Exception e) {
            // Missing audio device etc. — the game should keep running silently.
            System.err.println("Could not play " + sound + ": " + e.getMessage());
        }
    }
}
