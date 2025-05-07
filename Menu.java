import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Menu extends JPanel
{
    public Menu()
    {
        this.setBackground(new Color(200, 200, 200));
        this.setLayout(null);
        JButton button = new JButton();
        button.setBounds(160, 170, 180, 60);
        button.setText("START GAME");
        button.setFocusable(false);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PacManGame.startGame();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        this.add(button);
    }
}
