import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class Menu extends JPanel {
    public Menu() throws IOException, FontFormatException {
        File titleFont = new File("fonts/GinkobRetroFont.ttf");
        File buttonFont = new File("fonts/RetroNakjoy-Regular.otf");

        this.setBackground(new Color(200, 200, 200));
        this.setLayout(null);

        JLabel ratManText = new JLabel();
        ratManText.setFont(Font.createFont(Font.TRUETYPE_FONT, titleFont).deriveFont(100f));
        ratManText.setBounds(50, 80, 500, 100);
        ratManText.setText("Rat-Man");
        ratManText.setForeground(Color.BLACK);
        this.add(ratManText);

        JButton startButton = new JButton("START");
        startButton.setBounds(160, 230, 180, 60);
        startButton.setFont(Font.createFont(Font.TRUETYPE_FONT, buttonFont).deriveFont(30f));
        startButton.setFocusable(false);
        startButton.setBackground(new Color(120, 200, 255));
        startButton.setForeground(Color.WHITE);
        this.add(startButton);

        JButton skinsButton = new JButton("SKINS");
        skinsButton.setBounds(160, 310, 180, 60);
        skinsButton.setFont(Font.createFont(Font.TRUETYPE_FONT, buttonFont).deriveFont(30f));
        skinsButton.setFocusable(false);
        skinsButton.setBackground(new Color(120, 200, 255));
        skinsButton.setForeground(Color.WHITE);
        this.add(skinsButton);

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
    }
}
