import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class Menu extends JPanel
{
    public Menu() throws IOException, FontFormatException {
        //Font credit: https://www.1001fonts.com/ginkob-retro-font-font.html
        //
        File titleFont = new File("fonts/GinkobRetroFont.ttf");
        File buttonFont = new File("fonts/RetroNakjoy-Regular.otf");

        this.setBackground(new Color(220, 220, 220));
        this.setLayout(null);
        JButton startButton = new JButton();
        startButton.setBounds(160, 230, 180, 60);
        startButton.setText("START");
        startButton.setFont(Font.createFont(Font.TRUETYPE_FONT, buttonFont).deriveFont(30f));
        startButton.setFocusable(false);
        startButton.setBackground(new Color(120, 200, 255));
        startButton.setForeground(Color.white);

        this.setBackground(new Color(200, 200, 200));
        this.setLayout(null);
        JButton skinsButton = new JButton();
        skinsButton.setBounds(160, 310, 180, 60);
        skinsButton.setText("SKINS");
        skinsButton.setFont(Font.createFont(Font.TRUETYPE_FONT, buttonFont).deriveFont(30f));
        skinsButton.setFocusable(false);
        skinsButton.setBackground(new Color(120, 200, 255));
        skinsButton.setForeground(Color.white);
        this.add(skinsButton);

        JLabel ratManText = new JLabel();
        ratManText.setFont(Font.createFont(Font.TRUETYPE_FONT, titleFont).deriveFont(100f));
        ratManText.setBounds(50, 80, 500, 100);
        ratManText.setText("Rat-Man");
        ratManText.setForeground(Color.BLACK);
        this.add(ratManText);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PacManGame.startGame();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        skinsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PacManGame.showSkins();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        this.add(startButton);
    }
}
