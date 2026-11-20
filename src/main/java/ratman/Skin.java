package ratman;

import java.awt.image.BufferedImage;

/** Playable character skins. Each sprite sheet is one row of square animation frames. */
public enum Skin {
    RAT("Rat", "Rat.png", 4),
    SQUIRREL("Squirrel", "Squirrel.png", 4),
    JOE_OAKES("Joe Oakes", "Joe_Oakes.png", 7);

    private static Skin selected = RAT;

    public final String displayName;
    public final int frameCount;
    private final String path;

    Skin(String displayName, String file, int frameCount) {
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

    public static Skin selected() {
        return selected;
    }

    public static void select(Skin skin) {
        selected = skin;
    }
}
