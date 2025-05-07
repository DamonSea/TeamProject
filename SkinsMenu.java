import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SkinsMenu extends JPanel
{
    BufferedImage ratSprite;
    public SkinsMenu() throws IOException {
        this.setLayout(null);
        ratSprite = ImageIO.read(new File("Rat.png"));
        ImageIcon ratIcon = new ImageIcon(ratSprite.getSubimage(0, 0, 64, 64)
                .getScaledInstance(250, 250, Image.SCALE_SMOOTH));
        JButton button1 = new JButton(ratIcon);
        button1.setBounds(0,0,250,250);
        this.add(button1);

        JButton button2 = new JButton(ratIcon);
        button2.setBounds(250,0,250,250);
        button2.setBackground(Color.gray);
        this.add(button2);

        JButton button3 = new JButton(ratIcon);
        button3.setBounds(0,250,250,250);
        button3.setBackground(Color.gray);
        this.add(button3);

        JButton button4 = new JButton(ratIcon);
        button4.setBounds(250,250,250,250);
        button4.setBackground(Color.gray);
        this.add(button4);

    }
}
