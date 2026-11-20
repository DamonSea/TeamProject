package ratman;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;

/** Movement for the cats, the returning eyes, and the boss. */
final class CatAI {

    /** Cat-move steps an eaten cat waits in the cage before reviving. */
    static final int RESPAWN_DELAY = 10;
    /** Cat-move steps a revived cat waits before leaving the cage again. */
    static final int RELEASE_AFTER_RESPAWN = 10;

    private static final int[] DXS = {-1, 1, 0, 0};
    private static final int[] DYS = {0, 0, -1, 1};

    private CatAI() {}

    static void moveCats(GamePanel game) {
        for (Cat cat : game.activeCats()) {
            if (cat.eyes) {
                moveEyes(game, cat);
                continue;
            }
            if (cat.releaseTimer > 0) {
                cat.releaseTimer--;
                continue;
            }

            Point next = greedyStep(game.maze, cat.x, cat.y, game.playerX, game.playerY);
            if (isOccupied(game, cat, next.x, next.y)) continue;

            boolean wasInCage = Maze.inCage(cat.x, cat.y);
            cat.x = next.x;
            cat.y = next.y;
            if (wasInCage && !Maze.inCage(cat.x, cat.y)) {
                game.openCageDoor();
            }
        }
    }

    /** The boss chases every tick and can't be eaten. */
    static void moveBoss(GamePanel game) {
        Point next = greedyStep(game.maze, game.bossX, game.bossY, game.playerX, game.playerY);
        game.bossX = next.x;
        game.bossY = next.y;
    }

    /**
     * Original chase logic: step to whichever open neighbour is closest (Manhattan distance)
     * to the target. Stays put if boxed in.
     */
    static Point greedyStep(Maze maze, int x, int y, int targetX, int targetY) {
        int bestX = x, bestY = y, best = Integer.MAX_VALUE;
        for (int d = 0; d < 4; d++) {
            int tx = x + DXS[d], ty = y + DYS[d];
            if (!maze.isOpen(tx, ty)) continue;
            int dist = Math.abs(tx - targetX) + Math.abs(ty - targetY);
            if (dist < best) {
                best = dist;
                bestX = tx;
                bestY = ty;
            }
        }
        return new Point(bestX, bestY);
    }

    private static boolean isOccupied(GamePanel game, Cat self, int x, int y) {
        for (Cat other : game.activeCats()) {
            if (other != self && other.isAt(x, y)) return true;
        }
        return false;
    }

    /** Eyes path-find back to the cage centre, wait, then revive at their home spot. */
    private static void moveEyes(GamePanel game, Cat cat) {
        if (cat.isAt(Maze.CAGE_X, Maze.CAGE_Y)) {
            if (++cat.respawnTimer >= RESPAWN_DELAY) {
                cat.eyes = false;
                cat.respawnTimer = 0;
                cat.x = cat.homeX;
                cat.y = cat.homeY;
                cat.releaseTimer = RELEASE_AFTER_RESPAWN;
            }
            return;
        }
        Point step = stepTowardCage(game.maze, cat.x, cat.y);
        cat.x = step.x;
        cat.y = step.y;
    }

    /**
     * Breadth-first search from the cage centre outward; returns the first step on the shortest
     * path from (x, y) to the cage. Eyes may pass through cage walls. (The old version moved
     * diagonally and could get stuck behind a wall forever.)
     */
    static Point stepTowardCage(Maze maze, int x, int y) {
        int[][] dist = new int[Maze.ROWS][Maze.COLS];
        for (int[] row : dist) java.util.Arrays.fill(row, -1);

        Deque<Point> queue = new ArrayDeque<>();
        dist[Maze.CAGE_Y][Maze.CAGE_X] = 0;
        queue.add(new Point(Maze.CAGE_X, Maze.CAGE_Y));
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            for (int d = 0; d < 4; d++) {
                int nx = p.x + DXS[d], ny = p.y + DYS[d];
                if (!Maze.inBounds(nx, ny) || dist[ny][nx] >= 0) continue;
                if (!maze.isOpen(nx, ny) && !Maze.inCage(nx, ny)) continue;
                dist[ny][nx] = dist[p.y][p.x] + 1;
                queue.add(new Point(nx, ny));
            }
        }

        Point best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int d = 0; d < 4; d++) {
            int nx = x + DXS[d], ny = y + DYS[d];
            if (!Maze.inBounds(nx, ny) || dist[ny][nx] < 0) continue;
            if (dist[ny][nx] < bestDist) {
                bestDist = dist[ny][nx];
                best = new Point(nx, ny);
            }
        }
        // Unreachable (shouldn't happen): just warp home rather than get stuck.
        return best != null ? best : new Point(Maze.CAGE_X, Maze.CAGE_Y);
    }
}
