package ratman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A 21x21 maze with dots, power pellets ("cheese") and the cat cage in the exact centre.
 * Either loaded from a hand-designed level file (src/main/resources/levels) or randomly generated.
 * Arrays are indexed [row][col]; public helpers take (x = col, y = row).
 */
public final class Maze {
    public static final int ROWS = 21;
    public static final int COLS = 21;

    /** The cage is the 3x3 block centred on (CAGE_X, CAGE_Y). */
    public static final int CAGE_X = 10;
    public static final int CAGE_Y = 10;

    /** Number of hand-designed levels (levels/level1.txt ... levelN.txt); level 4 is the boss arena. */
    public static final int DESIGNED_LEVELS = 4;

    /** Where the player starts each life (random mazes start top-left). */
    private int startX = 1;
    private int startY = 1;
    private String name = "Random";

    /** Rows with a wrap-around tunnel on the left/right edge. */
    private static final int[] TUNNEL_ROWS = {9, 10};

    private final boolean[][] walls = new boolean[ROWS][COLS];
    private final boolean[][] dots = new boolean[ROWS][COLS];
    private final boolean[][] pellets = new boolean[ROWS][COLS];
    /** Which wall tile image variant to draw at each cell (fixed per level). */
    private final int[][] tileVariant = new int[ROWS][COLS];

    private Maze() {}

    public static Maze generate(Random random, Theme theme) {
        Maze m = new Maze();
        m.carve(random);
        m.buildCage();
        m.removeUnreachableDots();
        m.pickTiles(random, theme);
        return m;
    }

    /** The hand-designed maze for a level number; levels cycle through the designs. */
    public static Maze designed(int level, Random random, Theme theme) {
        int n = (level - 1) % DESIGNED_LEVELS + 1;
        return load("/levels/level" + n + ".txt", random, theme);
    }

    /**
     * Reads a level file: 21 rows of 21 characters (see the comment at the top of each file).
     * The cage is always rebuilt in the centre so a typo in the file can't break the cats.
     */
    static Maze load(String path, Random random, Theme theme) {
        List<String> rows = new ArrayList<>();
        String name = path;
        try (InputStream in = Maze.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing level file " + path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.startsWith(";")) continue;
                if (line.startsWith("name:")) {
                    name = line.substring(5).trim();
                    continue;
                }
                if (line.isEmpty() && rows.isEmpty()) continue;
                rows.add(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (rows.size() < ROWS) {
            throw new IllegalStateException(path + " needs " + ROWS + " rows, found " + rows.size());
        }

        Maze m = new Maze();
        m.name = name;
        boolean foundStart = false;
        for (int r = 0; r < ROWS; r++) {
            String row = rows.get(r);
            for (int c = 0; c < COLS; c++) {
                char ch = c < row.length() ? row.charAt(c) : '#';
                m.walls[r][c] = ch == '#';
                m.dots[r][c] = ch == '.';
                m.pellets[r][c] = ch == 'o';
                if (ch == 'P') {
                    m.startX = c;
                    m.startY = r;
                    foundStart = true;
                }
            }
        }
        if (!foundStart) throw new IllegalStateException(path + " has no player start (P)");
        m.buildCage();
        m.removeUnreachableDots();
        m.pickTiles(random, theme);
        return m;
    }

    private void pickTiles(Random random, Theme theme) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                tileVariant[r][c] = theme.pickVariant(random.nextDouble());
            }
        }
    }

    public int startX() {
        return startX;
    }

    public int startY() {
        return startY;
    }

    public String name() {
        return name;
    }

    /** Same generation algorithm as the original game: border ring, random 3x3 rooms, long corridors. */
    private void carve(Random random) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                walls[r][c] = true;
            }
        }

        // Outer ring corridor
        for (int i = 1; i <= ROWS - 2; i++) {
            open(1, i);
            open(i, 1);
            open(ROWS - 2, i);
            open(i, COLS - 2);
        }

        // Seven random 3x3 rooms with a pillar, each with a full-length corridor
        for (int n = 0; n < 7; n++) {
            int r0 = random.nextInt(4, ROWS - 7);
            int c0 = random.nextInt(4, COLS - 7);

            for (int r = r0; r < r0 + 3; r++) {
                for (int c = c0; c < c0 + 3; c++) {
                    open(r, c);
                }
            }
            walls[r0 + 1][c0 + 1] = true;
            dots[r0 + 1][c0 + 1] = false;

            if (random.nextBoolean()) {
                for (int c = 2; c < COLS - 2; c++) open(r0, c);
            } else {
                for (int r = 2; r < ROWS - 2; r++) open(r, c0);
            }
        }

        pellets[1][COLS - 2] = true;
        pellets[ROWS - 2][1] = true;
        pellets[ROWS - 2][COLS - 2] = true;

        // Side tunnels (no dots)
        for (int r : TUNNEL_ROWS) {
            walls[r][0] = false;
            walls[r][COLS - 1] = false;
        }

        // Corridors from the outer ring to the cage (no dots)
        for (int i = 1; i < 9; i++) {
            walls[CAGE_Y][i] = false;
            walls[i][CAGE_X] = false;
        }
    }

    /** 3x3 cage: the middle row is open floor for the cats, with doors above and below the centre. */
    private void buildCage() {
        for (int r = CAGE_Y - 1; r <= CAGE_Y + 1; r++) {
            for (int c = CAGE_X - 1; c <= CAGE_X + 1; c++) {
                walls[r][c] = true;
                dots[r][c] = false;
                pellets[r][c] = false;
            }
        }
        for (int c = CAGE_X - 1; c <= CAGE_X + 1; c++) walls[CAGE_Y][c] = false;
        walls[CAGE_Y + 1][CAGE_X] = false;
        walls[CAGE_Y - 1][CAGE_X] = false;
    }

    /**
     * Occasionally the random rooms and the cage wall off a pocket of dots the player can't get to,
     * which made about 1 in 250 levels impossible to finish. Flood-fill from the start and
     * drop any dot or pellet the player can't reach.
     */
    private void removeUnreachableDots() {
        boolean[][] reached = new boolean[ROWS][COLS];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        reached[startY][startX] = true;
        queue.add(new int[]{startX, startY});
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] d : steps) {
                int nx = Math.floorMod(p[0] + d[0], COLS), ny = p[1] + d[1];
                if (isWalkable(nx, ny) && !reached[ny][nx]) {
                    reached[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!reached[r][c]) {
                    dots[r][c] = false;
                    pellets[r][c] = false;
                }
            }
        }
    }

    private void open(int r, int c) {
        walls[r][c] = false;
        dots[r][c] = true;
    }

    // ---- queries, (x = column, y = row) ----

    public static boolean inBounds(int x, int y) {
        return x >= 0 && x < COLS && y >= 0 && y < ROWS;
    }

    public static boolean inCage(int x, int y) {
        return Math.abs(x - CAGE_X) <= 1 && Math.abs(y - CAGE_Y) <= 1;
    }

    public boolean isOpen(int x, int y) {
        return inBounds(x, y) && !walls[y][x];
    }

    /** Tiles the player may stand on: open and not inside the cats' cage. */
    public boolean isWalkable(int x, int y) {
        return isOpen(x, y) && !inCage(x, y);
    }

    public boolean isWall(int x, int y) {
        return inBounds(x, y) && walls[y][x];
    }

    public boolean hasDot(int x, int y) {
        return dots[y][x];
    }

    public boolean hasPellet(int x, int y) {
        return pellets[y][x];
    }

    public void eatDot(int x, int y) {
        dots[y][x] = false;
    }

    public void eatPellet(int x, int y) {
        pellets[y][x] = false;
    }

    public int tileVariant(int x, int y) {
        return tileVariant[y][x];
    }

    public boolean isCleared() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (dots[r][c] || pellets[r][c]) return false;
            }
        }
        return true;
    }

    /** Debug helper: removes every dot and pellet. */
    void clearAll() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                dots[r][c] = false;
                pellets[r][c] = false;
            }
        }
    }
}
