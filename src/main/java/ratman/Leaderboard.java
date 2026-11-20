package ratman;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** High-score table stored as "INITIALS,score" lines in leaderboard.txt (in the working directory). */
public final class Leaderboard {

    public record Entry(String initials, int score) {
        String toLine() {
            return initials + "," + score;
        }

        static Entry parse(String line) {
            String[] parts = line.trim().split(",");
            if (parts.length != 2) return null;
            try {
                return new Entry(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException e) {
                return null; // skip malformed lines instead of crashing
            }
        }
    }

    private static final int MAX_ENTRIES = 20;
    private static Path file = Path.of("leaderboard.txt");
    private static Entry lastAdded;

    private Leaderboard() {}

    /** For tests. */
    static void useFile(Path p) {
        file = p;
        lastAdded = null;
    }

    public static void saveScore(String initials, int score) {
        Entry entry = new Entry(initials, score);
        List<Entry> entries = new ArrayList<>(load());
        entries.add(entry);
        entries.sort(Comparator.comparingInt(Entry::score).reversed());
        if (entries.size() > MAX_ENTRIES) {
            entries = entries.subList(0, MAX_ENTRIES);
        }
        lastAdded = entry;

        List<String> lines = entries.stream().map(Entry::toLine).toList();
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error saving score: " + e.getMessage());
        }
    }

    /** All entries, highest score first. */
    public static List<Entry> load() {
        List<Entry> entries = new ArrayList<>();
        if (!Files.exists(file)) return entries;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Entry e = Entry.parse(line);
                if (e != null) entries.add(e);
            }
        } catch (IOException e) {
            System.err.println("Error reading leaderboard: " + e.getMessage());
        }
        entries.sort(Comparator.comparingInt(Entry::score).reversed());
        return entries;
    }

    /** The entry saved most recently this session (highlighted on screen), or null. */
    public static Entry lastAdded() {
        return lastAdded;
    }
}
