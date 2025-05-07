import java.io.*;
import java.util.*;

public class Leaderboard {
    private static final String FILE_NAME = "leaderboard.txt"; // File to store leaderboard

    /**
     * Saves a player's score to the leaderboard.
     *
     * @param initials The player's initials (e.g., "ABC").
     * @param score    The player's final score.
     */
    public static void saveScore(String initials, int score) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            writer.write(initials + "," + score);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving score: " + e.getMessage());
        }
    }

    /**
     * Reads and returns the leaderboard history.
     *
     * @return A list of leaderboard entries sorted by highest score.
     */
    public static List<String> getLeaderboard() {
        List<String> scores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                scores.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading leaderboard: " + e.getMessage());
        }

        // Sort scores from highest to lowest
        scores.sort((a, b) -> {
            int scoreA = Integer.parseInt(a.split(",")[1]);
            int scoreB = Integer.parseInt(b.split(",")[1]);
            return Integer.compare(scoreB, scoreA);
        });

        return scores;
    }
}