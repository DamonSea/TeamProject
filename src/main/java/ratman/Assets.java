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

    /** Frame number {@code frameIndex} of a sheet with {@code frameW}-px frames, cropped square to its drawing. */
    public static BufferedImage cropToArt(BufferedImage sheet, int frameIndex, int frameW) {
        BufferedImage f = sheet.getSubimage(frameIndex * frameW, 0, frameW, Math.min(frameW, sheet.getHeight()));
        int minX = f.getWidth(), minY = f.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < f.getHeight(); y++) {
            for (int x = 0; x < f.getWidth(); x++) {
                if ((f.getRGB(x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
        }
        if (maxX < 0) return f;
        int w = maxX - minX + 1, h = maxY - minY + 1, size = Math.max(w, h);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        out.createGraphics().drawImage(f.getSubimage(minX, minY, w, h), (size - w) / 2, (size - h) / 2, null);
        return out;
    }

    private static InputStream open(String path) {
        InputStream in = Assets.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Missing resource: " + path);
        }
        return in;
    }
}
