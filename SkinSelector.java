import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SkinSelector {
    public static String selectedSkin = "Rat"; // Default

    public static String getSkinPath() {
        switch (selectedSkin) {
            case "Squirrel":
                return "Images/Squirrel.png";
            case "Joe_Oakes":
                return "Images/Joe_Oakes.png";
            default:
                return "Images/Rat.png";
        }
    }

    public static BufferedImage getFirstFrame() throws IOException {
        BufferedImage spriteSheet = ImageIO.read(new File(getSkinPath()));
        return spriteSheet.getSubimage(0, 0, 30, 30);
    }
}
