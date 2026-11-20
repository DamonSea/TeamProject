package ratman;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * High-score table stored as "INITIALS,score" lines in ~/.ratman/leaderboard.txt.
 * It used to live in the project folder, so every game played showed up as a change in Git.
 * On first run it's seeded from an existing ./leaderboard.txt (old location) or the bundled
 * default scores in src/main/resources/leaderboard.txt.
 */
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
    private static Path file = Path.of(System.getProperty("user.home"), ".ratman", "leaderboard.txt");
    private static Entry lastAdded;
    private static List<Entry> cache;

    private Leaderboard() {}

    /** For tests. */
    static void useFile(Path p) {
        file = p;
        lastAdded = null;
        cache = null;
    }

    /** Creates the score file on first run so existing scores carry over. */
    private static void ensureFile() {
        if (Files.exists(file)) return;
        try {
            Files.createDirectories(file.getParent());
            Path old = Path.of("leaderboard.txt");
            if (Files.exists(old)) {
                Files.copy(old, file);
            } else {
                try (var in = Leaderboard.class.getResourceAsStream("/leaderboard.txt")) {
                    if (in != null) Files.copy(in, file);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not create leaderboard file: " + e.getMessage());
        }
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
        cache = List.copyOf(entries);

        List<String> lines = entries.stream().map(Entry::toLine).toList();
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error saving score: " + e.getMessage());
        }
    }

    /** All entries, highest score first. Read from disk once, then cached (it's drawn every frame). */
    public static List<Entry> load() {
        if (cache != null) return cache;
        ensureFile();
        List<Entry> entries = new ArrayList<>();
        if (!Files.exists(file)) return cache = List.of();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Entry e = Entry.parse(line);
                if (e != null) entries.add(e);
            }
        } catch (IOException e) {
            System.err.println("Error reading leaderboard: " + e.getMessage());
        }
        entries.sort(Comparator.comparingInt(Entry::score).reversed());
        return cache = List.copyOf(entries);
    }

    /** The entry saved most recently this session (highlighted on screen), or null. */
    public static Entry lastAdded() {
        return lastAdded;
    }
}
