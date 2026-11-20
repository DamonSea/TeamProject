package ratman;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * How the cats, their returning eyes, and the boss move.
 *
 * Cats work like Pac-Man's ghosts: they alternate between short SCATTER waves (each heads for its
 * own corner, giving the player a breather) and CHASE waves where each personality hunts
 * differently. Speeds are in tiles per tick; the player moves 1 tile per tick.
 */
final class CatAI {

    /** Ticks eaten-cat eyes wait in the cage before reviving. */
    static final int RESPAWN_DELAY = 20;
    /** Ticks a revived cat waits before leaving the cage again. */
    static final int RELEASE_AFTER_RESPAWN = 10;

    static final double EYES_SPEED = 1.0;
    static final double FRIGHTENED_FACTOR = 0.55;

    /** Within this many tiles every cat goes straight for the player. */
    private static final int CLOSE_RANGE = 3;
    private static final int AMBUSH_LOOKAHEAD = 4;

    /** Scatter/chase wave lengths in ticks: scatter, chase, scatter, chase, scatter, then chase for good. */
    private static final int[] WAVES = {50, 200, 40, 250, 30};

    enum Mode { SCATTER, CHASE }

    private static final int[] DXS = {0, -1, 0, 1}; // up, left, down, right (Pac-Man's tie-break order)
    private static final int[] DYS = {-1, 0, 1, 0};

    private CatAI() {}

    /** Normal cat speed for a level: half the player's speed on level 1, up to 80% by level 6. */
    static double catSpeed(int level) {
        return Math.min(0.8, 0.5 + 0.06 * (level - 1));
    }

    static Mode modeAt(int ticksIntoLife) {
        int t = ticksIntoLife;
        for (int i = 0; i < WAVES.length; i++) {
            if (t < WAVES[i]) return i % 2 == 0 ? Mode.SCATTER : Mode.CHASE;
            t -= WAVES[i];
        }
        return Mode.CHASE;
    }

    /** Called every tick. */
    static void moveCats(GamePanel game) {
        Mode mode = modeAt(game.ticksIntoLife);
        if (mode != game.catMode) {
            // Like the arcade: cats turn around when the wave changes, a hint to the player
            for (Cat cat : game.activeCats()) {
                if (!cat.eyes && !cat.frightened) {
                    cat.dirX = -cat.dirX;
                    cat.dirY = -cat.dirY;
                }
            }
            game.catMode = mode;
        }

        double speed = catSpeed(game.level);
        for (Cat cat : game.activeCats()) {
            if (cat.eyes && cat.isAt(Maze.CAGE_X, Maze.CAGE_Y)) {
                waitToRevive(game, cat);
                continue;
            }
            if (!cat.eyes && cat.releaseTimer > 0) {
                cat.releaseTimer--;
                continue;
            }
            double s = cat.eyes ? EYES_SPEED : cat.frightened ? speed * FRIGHTENED_FACTOR : speed;
            cat.moveCredit += s;
            while (cat.moveCredit >= 1) {
                cat.moveCredit -= 1;
                step(game, cat, s, mode);
                if (cat.eyes && cat.isAt(Maze.CAGE_X, Maze.CAGE_Y)) break;
            }
        }
    }

    private static void step(GamePanel game, Cat cat, double speed, Mode mode) {
        Maze maze = game.maze;
        boolean inCage = Maze.inCage(cat.x, cat.y);
        Point next;
        if (cat.eyes) {
            next = stepTowardCage(maze, cat.x, cat.y);
        } else if (cat.frightened) {
            next = toward(maze, cat, game.playerX, game.playerY, true, inCage);
        } else {
            Point target = target(game, cat, mode);
            if (cat.personality == Cat.Personality.CHASER && mode == Mode.CHASE) {
                next = shortestPathStep(maze, cat.x, cat.y, target.x, target.y, inCage);
            } else {
                next = toward(maze, cat, target.x, target.y, false, inCage);
            }
        }
        if (!cat.eyes && isOccupied(game, cat, next.x, next.y)) return;

        cat.prevX = cat.x;
        cat.prevY = cat.y;
        cat.moveTick = game.tickCount;
        cat.moveTicks = 1.0 / speed;
        cat.dirX = next.x - cat.x;
        cat.dirY = next.y - cat.y;
        cat.x = next.x;
        cat.y = next.y;
        if (!cat.eyes && inCage && !Maze.inCage(cat.x, cat.y)) {
            game.openCageDoor();
        }
    }

    /** Which tile a cat is heading for right now. */
    static Point target(GamePanel game, Cat cat, Mode mode) {
        int px = game.playerX, py = game.playerY;
        if (mode == Mode.SCATTER) return scatterCorner(cat);
        if (Math.abs(cat.x - px) + Math.abs(cat.y - py) <= CLOSE_RANGE) return new Point(px, py);

        int[] dir = game.playerDirection();
        return switch (cat.personality) {
            case CHASER -> new Point(px, py);
            case AMBUSHER -> new Point(px + dir[0] * AMBUSH_LOOKAHEAD, py + dir[1] * AMBUSH_LOOKAHEAD);
            case FLANKER -> {
                // Pivot two tiles ahead of the player, then double the vector from the Chaser to it
                Cat chaser = game.cats.get(0);
                int pivotX = px + dir[0] * 2, pivotY = py + dir[1] * 2;
                yield new Point(2 * pivotX - chaser.x, 2 * pivotY - chaser.y);
            }
        };
    }

    static Point scatterCorner(Cat cat) {
        return switch (cat.personality) {
            case CHASER -> new Point(Maze.COLS - 2, 1);
            case AMBUSHER -> new Point(1, 1);
            case FLANKER -> new Point(Maze.COLS - 2, Maze.ROWS - 2);
        };
    }

    // ---- the boss ----

    static final int POUNCE_WINDUP = 8;      // ticks crouched, glowing red, before it dashes
    static final int POUNCE_SPEED = 2;       // tiles per tick while dashing (the player does 1)
    static final int POUNCE_MAX = 12;        // longest dash
    static final int DIZZY_TICKS = 12;       // stunned after slamming into a wall
    private static final int POUNCE_MIN_RANGE = 3, POUNCE_MAX_RANGE = 10;

    /**
     * The boss chases every tick and can't be eaten. When it lines up with the player down a
     * clear corridor it winds up, then POUNCES at double speed until it hits a wall (and gets dizzy).
     */
    static void moveBoss(GamePanel game) {
        game.bossPrevX = game.bossX;
        game.bossPrevY = game.bossY;
        game.bossMoveTick = game.tickCount;

        switch (game.bossState) {
            case WINDUP -> {
                if (--game.bossStateTimer <= 0) {
                    game.bossState = GamePanel.BossState.DASH;
                    game.dashTravelled = 0;
                    SoundPlayer.play(SoundPlayer.Sound.BOSS_DASH);
                }
            }
            case DASH -> dash(game);
            case DIZZY -> {
                if (--game.bossStateTimer <= 0) {
                    game.bossState = GamePanel.BossState.CHASE;
                    game.bossCooldown = 60 + java.util.concurrent.ThreadLocalRandom.current().nextInt(50);
                }
            }
            case CHASE -> {
                boolean inCage = Maze.inCage(game.bossX, game.bossY);
                if (game.bossCooldown > 0) {
                    game.bossCooldown--;
                } else if (!inCage && lineUpForPounce(game)) {
                    game.bossState = GamePanel.BossState.WINDUP;
                    game.bossStateTimer = POUNCE_WINDUP;
                    SoundPlayer.play(SoundPlayer.Sound.BOSS_WINDUP);
                    return;
                }
                int[] dir = chooseDirection(game.maze, game.bossX, game.bossY, game.bossDirX, game.bossDirY,
                        game.playerX, game.playerY, false, inCage);
                game.bossX += dir[0];
                game.bossY += dir[1];
                game.bossDirX = dir[0];
                game.bossDirY = dir[1];
            }
        }
    }

    /** True (and sets the dash direction) if the player is straight ahead down a clear corridor. */
    private static boolean lineUpForPounce(GamePanel game) {
        int dx = Integer.signum(game.playerX - game.bossX), dy = Integer.signum(game.playerY - game.bossY);
        if (dx != 0 && dy != 0) return false;            // not in the same row or column
        int dist = Math.abs(game.playerX - game.bossX) + Math.abs(game.playerY - game.bossY);
        if (dist < POUNCE_MIN_RANGE || dist > POUNCE_MAX_RANGE) return false;
        for (int i = 1; i <= dist; i++) {
            if (!passable(game.maze, game.bossX + dx * i, game.bossY + dy * i, false)) return false;
        }
        game.dashDx = dx;
        game.dashDy = dy;
        return true;
    }

    private static void dash(GamePanel game) {
        for (int i = 0; i < POUNCE_SPEED; i++) {
            int nx = game.bossX + game.dashDx, ny = game.bossY + game.dashDy;
            if (!passable(game.maze, nx, ny, false)) {
                // SLAM into the wall: dizzy for a moment
                game.bossState = GamePanel.BossState.DIZZY;
                game.bossStateTimer = DIZZY_TICKS;
                game.effects.shake(0.35, 5);
                SoundPlayer.play(SoundPlayer.Sound.BOSS_THUD);
                return;
            }
            game.bossX = nx;
            game.bossY = ny;
            game.bossDirX = game.dashDx;
            game.bossDirY = game.dashDy;
            if (game.bossX == game.playerX && game.bossY == game.playerY) return; // got you
            if (++game.dashTravelled >= POUNCE_MAX) {
                game.bossState = GamePanel.BossState.CHASE;
                game.bossCooldown = 60;
                return;
            }
        }
    }

    private static Point toward(Maze maze, Cat cat, int tx, int ty, boolean flee, boolean allowCage) {
        int[] d = chooseDirection(maze, cat.x, cat.y, cat.dirX, cat.dirY, tx, ty, flee, allowCage);
        return new Point(cat.x + d[0], cat.y + d[1]);
    }

    /** Cats that have left the cage don't wander back in (only eyes do). */
    private static boolean passable(Maze maze, int x, int y, boolean allowCage) {
        return maze.isOpen(x, y) && (allowCage || !Maze.inCage(x, y));
    }

    /**
     * Classic Pac-Man ghost rule: at each step pick the open neighbour closest to the target
     * (or farthest, when fleeing), but never turn straight back unless it's a dead end.
     *
     * @return {dx, dy}; {0, 0} if completely boxed in
     */
    static int[] chooseDirection(Maze maze, int x, int y, int lastDx, int lastDy,
                                 int targetX, int targetY, boolean flee, boolean allowCage) {
        int bestD = -1, bestScore = 0, reverseD = -1;
        for (int d = 0; d < 4; d++) {
            int tx = x + DXS[d], ty = y + DYS[d];
            if (!passable(maze, tx, ty, allowCage)) continue;
            boolean isReverse = (lastDx != 0 || lastDy != 0) && DXS[d] == -lastDx && DYS[d] == -lastDy;
            if (isReverse) {
                reverseD = d;
                continue;
            }
            int dist = (tx - targetX) * (tx - targetX) + (ty - targetY) * (ty - targetY);
            int score = flee ? -dist : dist;
            if (bestD < 0 || score < bestScore) {
                bestD = d;
                bestScore = score;
            }
        }
        if (bestD < 0) bestD = reverseD; // dead end: turning around is the only option
        return bestD < 0 ? new int[]{0, 0} : new int[]{DXS[bestD], DYS[bestD]};
    }

    /** Overload kept for callers that don't care about the cage. */
    static int[] chooseDirection(Maze maze, int x, int y, int lastDx, int lastDy,
                                 int targetX, int targetY, boolean flee) {
        return chooseDirection(maze, x, y, lastDx, lastDy, targetX, targetY, flee, true);
    }

    /** First step of a true shortest path (breadth-first search) from (x, y) to the target. */
    static Point shortestPathStep(Maze maze, int x, int y, int targetX, int targetY, boolean allowCage) {
        int[][] dist = distancesFrom(maze, targetX, targetY, allowCage);
        Point best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int d = 0; d < 4; d++) {
            int nx = x + DXS[d], ny = y + DYS[d];
            if (!Maze.inBounds(nx, ny) || dist[ny][nx] < 0) continue;
            if (!passable(maze, nx, ny, allowCage)) continue;
            if (dist[ny][nx] < bestDist) {
                bestDist = dist[ny][nx];
                best = new Point(nx, ny);
            }
        }
        return best != null ? best : new Point(x, y);
    }

    private static int[][] distancesFrom(Maze maze, int sx, int sy, boolean allowCage) {
        int[][] dist = new int[Maze.ROWS][Maze.COLS];
        for (int[] row : dist) Arrays.fill(row, -1);
        if (!Maze.inBounds(sx, sy)) return dist;
        Deque<Point> queue = new ArrayDeque<>();
        dist[sy][sx] = 0;
        queue.add(new Point(sx, sy));
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            for (int d = 0; d < 4; d++) {
                int nx = p.x + DXS[d], ny = p.y + DYS[d];
                if (!Maze.inBounds(nx, ny) || dist[ny][nx] >= 0) continue;
                if (!passable(maze, nx, ny, allowCage)) continue;
                dist[ny][nx] = dist[p.y][p.x] + 1;
                queue.add(new Point(nx, ny));
            }
        }
        return dist;
    }

    private static boolean isOccupied(GamePanel game, Cat self, int x, int y) {
        if (Maze.inCage(x, y)) return false; // cats can squeeze past each other inside the cage
        for (Cat other : game.activeCats()) {
            if (other != self && !other.eyes && other.isAt(x, y)) return true;
        }
        return false;
    }

    /** Eyes wait in the cage, then the cat revives at its home spot. */
    private static void waitToRevive(GamePanel game, Cat cat) {
        if (++cat.respawnTimer >= RESPAWN_DELAY) {
            cat.eyes = false;
            cat.frightened = false;
            cat.dirX = cat.dirY = 0;
            cat.respawnTimer = 0;
            cat.moveCredit = 0;
            cat.x = cat.homeX;
            cat.y = cat.homeY;
            cat.snap();
            cat.releaseTimer = RELEASE_AFTER_RESPAWN;
        }
    }

    /**
     * Breadth-first search from the cage centre outward; returns the first step on the shortest
     * path from (x, y) to the cage. Eyes may pass through cage walls.
     */
    static Point stepTowardCage(Maze maze, int x, int y) {
        int[][] dist = new int[Maze.ROWS][Maze.COLS];
        for (int[] row : dist) Arrays.fill(row, -1);

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
        return best != null ? best : new Point(Maze.CAGE_X, Maze.CAGE_Y);
    }
}
