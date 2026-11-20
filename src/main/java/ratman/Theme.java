package ratman;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Visual theme for a level. The wall tile sheet holds several 30x30 tile variants;
 * {@code variantOdds} are cumulative probabilities for picking each variant.
 */
public enum Theme {
    OUTDOOR("Outdoor.png", new Color(150, 110, 0), 0.82, 0.88, 0.94, 1.0),
    CLASSROOM("Classroom.png", new Color(100, 130, 130), 0.85, 1.0),
    PLANETS("Planets.png", Color.BLACK, 0.85, 1.0);

    public final Color background;
    private final String path;
    private final double[] variantOdds;

    Theme(String file, Color background, double... variantOdds) {
        this.path = "/themes/" + file;
        this.background = background;
        this.variantOdds = variantOdds;
    }

    /** Levels cycle Outdoor → Classroom → Planets. */
    public static Theme forLevel(int level) {
        return switch (level % 3) {
            case 1 -> OUTDOOR;
            case 2 -> CLASSROOM;
            default -> PLANETS;
        };
    }

    public BufferedImage tileSheet() {
        return Assets.image(path);
    }

    /** Maps a random number in [0,1) to a wall tile variant index. */
    public int pickVariant(double roll) {
        for (int i = 0; i < variantOdds.length; i++) {
            if (roll <= variantOdds[i]) return i;
        }
        return variantOdds.length - 1;
    }
}
