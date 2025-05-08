import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SkinsMenu extends JPanel {
    public SkinsMenu() throws IOException {
        this.setLayout(null);
        this.setBackground(Color.LIGHT_GRAY);

        // Load full sprite sheets
        BufferedImage ratSheet = ImageIO.read(new File("Images/Rat.png"));           // 64x256
        BufferedImage squirrelSheet = ImageIO.read(new File("Images/Squirrel.png")); // 30x120
        BufferedImage joeOakesSheet = ImageIO.read(new File("Images/Joe_Oakes.png"));// 30x210

        // Crop first frame only
        BufferedImage ratSprite = ratSheet.getSubimage(0, 0, 64, 64);
        BufferedImage squirrelSprite = squirrelSheet.getSubimage(0, 0, 30, 30);
        BufferedImage joeOakesSprite = joeOakesSheet.getSubimage(0, 0, 30, 30);

        // Scale to 250×250 for buttons
        ImageIcon ratIcon = new ImageIcon(ratSprite.getScaledInstance(250, 250, Image.SCALE_SMOOTH));
        ImageIcon squirrelIcon = new ImageIcon(squirrelSprite.getScaledInstance(250, 250, Image.SCALE_SMOOTH));
        ImageIcon joeOakesIcon = new ImageIcon(joeOakesSprite.getScaledInstance(250, 250, Image.SCALE_SMOOTH));

        // RAT BUTTON
        JButton ratButton = new JButton(ratIcon);
        ratButton.setBounds(0, 0, 250, 250);
        ratButton.addActionListener(e -> SkinSelector.selectedSkin = "Rat");
        this.add(ratButton);

        // SQUIRREL BUTTON
        JButton squirrelButton = new JButton(squirrelIcon);
        squirrelButton.setBounds(250, 0, 250, 250);
        squirrelButton.addActionListener(e -> SkinSelector.selectedSkin = "Squirrel");
        this.add(squirrelButton);

        // JOE OAKES BUTTON
        JButton joeOakesButton = new JButton(joeOakesIcon);
        joeOakesButton.setBounds(0, 250, 250, 250);
        joeOakesButton.addActionListener(e -> SkinSelector.selectedSkin = "Joe_Oakes");
        this.add(joeOakesButton);

        // BACK BUTTON
        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 24));
        backButton.setBounds(250, 250, 250, 250);
        backButton.setBackground(new Color(120, 200, 255));
        backButton.setForeground(Color.BLACK);
        backButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        backButton.addActionListener(e -> {
            try {
                PacManGame.showMenu();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        this.add(backButton);
    }
}
