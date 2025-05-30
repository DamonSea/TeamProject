import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundPlayer {
    public static void playSound(String soundFileName) {

            try {
                File soundFile = new File(soundFileName);
                if (soundFile.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInput);

                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float range = gainControl.getMaximum() - gainControl.getMinimum();
                    float gain = (range * 0.7f) + gainControl.getMinimum();
                    gainControl.setValue(gain);

                    clip.start();
                } else {
                    System.out.println("Sound file not found: " + soundFileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}