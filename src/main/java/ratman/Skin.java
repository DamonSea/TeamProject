package ratman;

import java.awt.image.BufferedImage;

/** Playable character skins. Each sprite sheet is one row of square animation frames. */
public enum Skin {
    RAT("Rat", "Rat.png", 4, true),
    SQUIRREL("Squirrel", "Squirrel.png", 4, false),
    JOE_OAKES("Joe Oakes", "Joe_Oakes.png", 7, false);

    private static Skin selected = RAT;

    public final String displayName;
    public final int frameCount;
    /** Only the rat has drawn death frames; the others spin, shrink and pop instead. */
    public final boolean hasDeathArt;
    private final String path;

    Skin(String displayName, String file, int frameCount, boolean hasDeathArt) {
        this.hasDeathArt = hasDeathArt;
        this.displayName = displayName;
        this.path = "/images/" + file;
        this.frameCount = frameCount;
    }

    public BufferedImage sheet() {
        return Assets.image(path);
    }

    /** First animation frame, used for the skin picker. */
    public BufferedImage firstFrame() {
        BufferedImage sheet = sheet();
        int size = sheet.getWidth() / frameCount;
        return sheet.getSubimage(0, 0, size, sheet.getHeight());
    }

    /** First frame cropped to a square around the actual drawing (no empty margins). */
    public BufferedImage icon() {
        BufferedImage f = firstFrame();
        int minX = f.getWidth(), minY = f.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < f.getHeight(); y++) {
            for (int x = 0; x < f.getWidth(); x++) {
                if ((f.getRGB(x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
        }
        if (maxX < 0) return f;
        int size = Math.max(maxX - minX + 1, maxY - minY + 1) + 2;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int ox = (size - (maxX - minX + 1)) / 2, oy = (size - (maxY - minY + 1)) / 2;
        out.createGraphics().drawImage(f.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1), ox, oy, null);
        return out;
    }

    public static Skin selected() {
        return selected;
    }

    public static void select(Skin skin) {
        selected = skin;
    }
}
