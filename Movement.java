import java.awt.*;

public class Movement {

    public static void moveCats()
    {
        for (int i = 0; i < GamePanel.level%4; i++) {
            boolean[] catScattering;
            if (i >= GamePanel.catScattering.length || i >= GamePanel.catRespawnDelay.length || i >= GamePanel.catReleaseTimers.length) continue;  // Prevent out-of-bounds error

            // cat scatter moves back to cage
            if (GamePanel.catScattering[i]) {
                Point cat = GamePanel.catPositions.get(i);
                int targetX = 10, targetY = 10; // position of cage

                // If eyes have reached the cage
                if (cat.x == targetX && cat.y == targetY) {
                    if (GamePanel.catRespawnDelay[i] < GamePanel.RESPAWN_DELAY_TICKS) {
                        GamePanel.catRespawnDelay[i]++;
                    } else {
                        GamePanel.catScattering[i] = false;
                        GamePanel.catRespawnDelay[i] = 0;
                        GamePanel.catReleaseTimers[i] = 10;
                        //catPositions.set(i, new Point(9, 10)); // just outside cage
                        // add cat position to move cats eye
                        if (i % 3 == 0) {
                            GamePanel.catPositions.set(i, new Point(9, 10)); // Left side of cage
                        } else if (i % 3 == 1) {
                            GamePanel.catPositions.set(i, new Point(10, 10)); // Center of cage
                        } else {
                            GamePanel.catPositions.set(i, new Point(11, 10)); // Right side of cage
                        }
                    }
                    continue;
                }

                // Move eyes toward cage
                int dx = Integer.compare(targetX, cat.x);
                int dy = Integer.compare(targetY, cat.y);
                int nx = cat.x + dx;
                int ny = cat.y + dy;

                if (nx >= 0 && nx < GamePanel.COLS && ny >= 0 && ny < GamePanel.ROWS &&
                        (GamePanel.maze[ny][nx] == 0 || (nx >= 9 && nx <= 11 && ny >= 9 && ny <= 11))) {
                    GamePanel.catPositions.set(i, new Point(nx, ny));
                }

                continue;
            }

            // Wait for release timer
            if (GamePanel.catReleaseTimers[i] > 0) {
                GamePanel.catReleaseTimers[i]--;
                continue;
            }

            // Normal chase logic
            Point cat = GamePanel.catPositions.get(i);
            int bestDx = 0, bestDy = 0, minDist = Integer.MAX_VALUE;
            int[] dxs = {-1, 1, 0, 0}, dys = {0, 0, -1, 1};

            int pacmanY;
            for (int j = 0; j < 4; j++) {
                int tx = cat.x + dxs[j];
                int ty = cat.y + dys[j];

                if (tx < 0 || tx >= GamePanel.COLS || ty < 0 || ty >= GamePanel.ROWS) continue;
                if (GamePanel.maze[ty][tx] == 1) continue;

                int dist = Math.abs(tx - GamePanel.pacmanX) + Math.abs(ty - GamePanel.pacmanY);
                if (dist < minDist) {
                    minDist = dist;
                    bestDx = dxs[j];
                    bestDy = dys[j];
                }
            }

            int nextX = cat.x + bestDx;
            int nextY = cat.y + bestDy;

            if (nextX == GamePanel.pacmanX && nextY == GamePanel.pacmanY) {
                if (GamePanel.poweredUp) {
                    GamePanel.catScattering[i] = true; //cat turns in to eyes
                    GamePanel.catRespawnDelay[i] = 0;
                    SoundPlayer.playSound("sounds/eatghost.wav");
                    GamePanel.score += 100;
                    continue;
                } else {
                    GamePanel.lives--;
                    if (GamePanel.lives > 0) GamePanel.resetPositions();
                    else {
                        GamePanel.gameOver = true;
                        GamePanel.timer.stop();
                    }
                    return;
                }
            }

            boolean occupied = false;
            for (int j = 0; j < GamePanel.catPositions.size(); j++) {
                if (j != i && GamePanel.catPositions.get(j).equals(new Point(nextX, nextY))) {
                    occupied = true;
                    break;
                }
            }

            if (!occupied) {
                GamePanel.catPositions.set(i, new Point(nextX, nextY));
            }
        }
    }

    public static void moveBoss() {
        // Normal chase logic
        Point cat = GamePanel.bossPosition;
        int bestDx = 0, bestDy = 0, minDist = Integer.MAX_VALUE;
        int[] dxs = {-1, 1, 0, 0}, dys = {0, 0, -1, 1};

        int pacmanY;
        for (int j = 0; j < 4; j++) {
            int tx = cat.x + dxs[j];
            int ty = cat.y + dys[j];

            if (tx < 0 || tx >= GamePanel.COLS || ty < 0 || ty >= GamePanel.ROWS) continue;
            if (GamePanel.maze[ty][tx] == 1) continue;

            int dist = Math.abs(tx - GamePanel.pacmanX) + Math.abs(ty - GamePanel.pacmanY);
            if (dist < minDist) {
                minDist = dist;
                bestDx = dxs[j];
                bestDy = dys[j];
            }
        }

        int nextX = cat.x + bestDx;
        int nextY = cat.y + bestDy;

        if (nextX == GamePanel.pacmanX && nextY == GamePanel.pacmanY) {
            GamePanel.lives--;
            if (GamePanel.lives > 0) GamePanel.resetPositions();
            else {
                GamePanel.gameOver = true;
                GamePanel.timer.stop();
            }
            return;

        }
        GamePanel.bossPosition = new Point(nextX, nextY);
    }
}
