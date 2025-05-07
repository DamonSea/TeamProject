// Player.java
import java.awt.*;
import java.util.List;
import java.util.Random;

public class Player {
    private int x = 1, y = 1;
    private int dx = 0, dy = 0;
    private int lastDX = 0, lastDY = 0;
    private boolean mouthOpen = true;
    private int frame = 0;
    private final int frameCount = 4;

    private boolean poweredUp = false;
    private int powerTimer = 0;
    private long lastWakaTime = 0;

    private int score = 0;
    private final Random random = new Random();

    public void resetPosition() {
        x = 1;
        y = 1;
        dx = dy = 0;
        lastDX = lastDY = 0;
        poweredUp = false;
        powerTimer = 0;
        score = 0;
    }

    public void updateAnimation() {
        mouthOpen = !mouthOpen;
        frame = (frame + 1) % frameCount;
    }

    public void move(int[][] maze, boolean[][] dots, boolean[][] pellets, List<Point> catPositions, boolean[] catScattering,
                     int[] catScatterTimer, int[] catRespawnDelay, int[] scatterDx, int[] scatterDy,
                     GameManager gm) {
        if (gm.isGameOver() || gm.isGameWon()) return;

        // Wraparound
        if (x == 0) x = 18;
        else if (x == 19) x = 1;

        int newX = dx != 0 || dy != 0 ? x + dx : x + lastDX;
        int newY = dx != 0 || dy != 0 ? y + dy : y + lastDY;

        // Prevent cage entry
        if ((newY >= 9 && newY <= 11 && newX >= 9 && newX <= 11) &&
            !(newY == 9 && (newX == 8 || newX == 12)) &&
            !(newY == 11 && (newX == 8 || newX == 12)) &&
            !(newX == 8 && (newY == 9 || newY == 11)) &&
            !(newX == 12 && (newY == 9 || newY == 11))) {
            return;
        }

        if (newX < 0 || newX >= maze[0].length || newY < 0 || newY >= maze.length || maze[newY][newX] == 1) {
            return;
        }

        x = newX;
        y = newY;
        lastDX = dx;
        lastDY = dy;

        // Eat dot
        if (dots[y][x]) {
            dots[y][x] = false;
            score += 10;
            long now = System.currentTimeMillis();
            if (now - lastWakaTime >= 1000) {
                SoundPlayer.playSound("sounds/waka.wav");
                lastWakaTime = now;
            }
            if (checkWin(dots, pellets)) {
                gm.setGameWon(true);
            }
        }

        // Eat pellet
        if (pellets[y][x]) {
            pellets[y][x] = false;
            poweredUp = true;
            powerTimer = 40;
            score += 50;
            SoundPlayer.playSound("sounds/powerup.wav");
        }

        // Ghost collisions
        for (int i = 0; i < catPositions.size(); i++) {
            Point cat = catPositions.get(i);
            if (cat.x == x && cat.y == y) {
                if (poweredUp && !catScattering[i]) {
                    catScattering[i] = true;
                    catScatterTimer[i] = GameManager.SCATTER_DURATION;
                    catRespawnDelay[i] = 0;
                    scatterDx[i] = Integer.compare(10, cat.x);
                    scatterDy[i] = Integer.compare(10, cat.y);
                    score += 100;
                    SoundPlayer.playSound("sounds/eatghost.wav");
                } else if (!poweredUp && !catScattering[i]) {
                    gm.triggerDeath();
                    return;
                }
            }
        }
    }

    public void tickPowerUp() {
        if (poweredUp) {
            powerTimer--;
            if (powerTimer <= 0) {
                poweredUp = false;
            }
        }
    }

    public boolean checkWin(boolean[][] dots, boolean[][] pellets) {
        for (int r = 0; r < dots.length; r++) {
            for (int c = 0; c < dots[0].length; c++) {
                if (dots[r][c] || pellets[r][c]) return false;
            }
        }
        return true;
    }

    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setDirection(int dx, int dy) { this.dx = dx; this.dy = dy; }
    public boolean isMouthOpen() { return mouthOpen; }
    public int getFrame() { return frame; }
    public boolean isPoweredUp() { return poweredUp; }
    public int getScore() { return score; }


}
