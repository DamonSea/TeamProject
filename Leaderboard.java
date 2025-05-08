import java.io.*;
import java.util.*;

public class Leaderboard {
    private static final String FILE_NAME = "leaderboard.txt"; // File to store leaderboard
    private static final int MAX_ENTRIES = 20; // only top 20
    private static String lastAddedScore = ""; // Track last added score

    /**
     * Saves a player's score to the leaderboard.
     *
     * @param initials The player's initials (e.g., "ABC").
     * @param score    The player's final score.
     */
    public static void saveScore(String initials, int score) {
        List<String> scores = getLeaderboard(); // Read existing scores
        scores.add(initials + "," + score); // Add new score

        // Sort scores from highest to lowest
        scores.sort((a, b) -> {
            int scoreA = Integer.parseInt(a.split(",")[1]);
            int scoreB = Integer.parseInt(b.split(",")[1]);
            return Integer.compare(scoreB, scoreA);
        });

        // Keep only the top 10 scores
        if (scores.size() > MAX_ENTRIES) {
            scores = scores.subList(0, MAX_ENTRIES);
        }

        //Write Leaderboard file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, false))) { // 'false' overwrites the file
            for (String entry : scores) {
                writer.write(entry);
                writer.newLine();
            }
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
                // Highlight the latest score
                if (line.equals(lastAddedScore)) {
                    scores.add("* " + line + " *"); // Add markers around the latest score
                } else {
                    scores.add(line);
                }
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