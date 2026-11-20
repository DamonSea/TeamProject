package ratman;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads images, fonts and sound bytes from the classpath (src/main/resources).
 * Loading from the classpath instead of the working directory means the game
 * works no matter where it is launched from, and file names are matched exactly
 * on every OS (the old code broke on Linux because of "boss.png" vs "Boss.png").
 */
public final class Assets {
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    private static final Map<String, Font> FONTS = new HashMap<>();
    private static final Map<String, byte[]> BYTES = new HashMap<>();

    private Assets() {}

    public static BufferedImage image(String path) {
        return IMAGES.computeIfAbsent(path, p -> {
            try (InputStream in = open(p)) {
                return ImageIO.read(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read image " + p, e);
            }
        });
    }

    public static Font font(String path, float size) {
        Font base = FONTS.computeIfAbsent(path, p -> {
            try (InputStream in = open(p)) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (IOException | FontFormatException e) {
                throw new IllegalStateException("Could not read font " + p, e);
            }
        });
        return base.deriveFont(size);
    }

    public static byte[] bytes(String path) {
        return BYTES.computeIfAbsent(path, p -> {
            try (InputStream in = open(p)) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + p, e);
            }
        });
    }

    private static InputStream open(String path) {
        InputStream in = Assets.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Missing resource: " + path);
        }
        return in;
    }
}
