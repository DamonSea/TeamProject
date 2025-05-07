import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LeaderboardWindow extends JFrame {

    public LeaderboardWindow() {
        // Set the window properties
        setTitle("Leaderboard");
        setSize(400, 400);
        setLocationRelativeTo(null);  // Center the window
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create a JTextArea to display the leaderboard
        JTextArea leaderboardText = new JTextArea();
        leaderboardText.setEditable(false);

        // Get the leaderboard entries
        List<String> scores = Leaderboard.getLeaderboard();

        // Format the leaderboard text
        StringBuilder leaderboardDisplay = new StringBuilder();
        leaderboardDisplay.append("Leaderboard:\n\n");
        for (String score : scores) {
            leaderboardDisplay.append(score).append("\n");
        }

        // Set the leaderboard text in the JTextArea
        leaderboardText.setText(leaderboardDisplay.toString());

        // Add the text area inside a scroll pane
        add(new JScrollPane(leaderboardText), BorderLayout.CENTER);
    }
}
