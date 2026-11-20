package ratman;

import java.util.Random;

/**
 * A randomly generated 20x20 maze with dots, power pellets ("cheese") and the cat cage.
 * Arrays are indexed [row][col]; public helpers take (x = col, y = row).
 */
public final class Maze {
    public static final int ROWS = 20;
    public static final int COLS = 20;

    /** The cage is the 3x3 block centred on (CAGE_X, CAGE_Y). */
    public static final int CAGE_X = 10;
    public static final int CAGE_Y = 10;

    /** Where the player starts each life. */
    public static final int START_X = 1;
    public static final int START_Y = 1;

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
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                m.tileVariant[r][c] = theme.pickVariant(random.nextDouble());
            }
        }
        return m;
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

    /** Solid 3x3 cage with the centre open and doors above and below it. */
    private void buildCage() {
        for (int r = CAGE_Y - 1; r <= CAGE_Y + 1; r++) {
            for (int c = CAGE_X - 1; c <= CAGE_X + 1; c++) {
                walls[r][c] = true;
                dots[r][c] = false;
                pellets[r][c] = false;
            }
        }
        walls[CAGE_Y][CAGE_X] = false;
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
        reached[START_Y][START_X] = true;
        queue.add(new int[]{START_X, START_Y});
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
