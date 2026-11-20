package ratman;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Draws the game screen from the state in {@link GamePanel}. */
final class Renderer {
    private static final int T = GamePanel.TILE;
    private static final int LEADERBOARD_ROWS = 10;

    private static final Font HUD = new Font("Arial", Font.BOLD, 20);
    private static final Font BIG = new Font("Arial", Font.BOLD, 40);
    private static final Font READY = new Font("Arial", Font.BOLD, 24);
    private static final Font LIST = new Font("Arial", Font.BOLD, 18);
    private static final Font SMALL = new Font("Arial", Font.BOLD, 14);

    // Sprite sheets (one row of frames each)
    private final BufferedImage cats = Assets.image("/images/cats.png");            // 3 cats x 2 frames, 30px
    private final BufferedImage catsScared = Assets.image("/images/cats_scared.png"); // 3 cats x 2 frames, 64px
    private final BufferedImage catEyes = Assets.image("/images/cat_eyes.png");     // 64px frames
    private final BufferedImage boss = Assets.image("/images/Boss.png");            // 2 frames, 100px
    private final BufferedImage cage = Assets.image("/images/Cage.png");            // closed, open
    private final BufferedImage death = Assets.image("/images/rat_death.png");      // 6 frames, 64px
    private final BufferedImage cheese = Assets.image("/images/pelletcheese.png");

    private static final int DEATH_FRAMES = 6;

    private final GamePanel game;

    Renderer(GamePanel game) {
        this.game = game;
    }

    void draw(Graphics2D g) {
        drawMaze(g);
        drawCage(g);
        drawEnemies(g);

        if (game.dying) {
            drawDeath(g);
        } else {
            drawPlayer(g);
        }
        drawHud(g);

        if (game.gameWon) {
            drawLevelComplete(g);
        } else if (game.gameOver) {
            drawGameOver(g);
        } else if (game.waitingToStart) {
            text(g, "READY!", READY, Color.YELLOW, GamePanel.WIDTH / 2 - 50, GamePanel.HEIGHT / 2);
        }
    }

    // ---- world ----

    private void drawMaze(Graphics2D g) {
        Maze maze = game.maze;
        BufferedImage tiles = game.theme.tileSheet();
        int pelletSize = (int) (T * 0.8);
        int pelletOffset = (T - pelletSize) / 2;

        for (int y = 0; y < Maze.ROWS; y++) {
            for (int x = 0; x < Maze.COLS; x++) {
                int px = x * T, py = y * T;
                if (maze.isWall(x, y)) {
                    int sx = maze.tileVariant(x, y) * T;
                    g.drawImage(tiles, px, py, px + T, py + T, sx, 0, sx + T, T, null);
                } else if (maze.hasDot(x, y)) {
                    g.setColor(Color.WHITE);
                    g.fillOval(px + T / 3, py + T / 3, T / 3, T / 3);
                }
                if (maze.hasPellet(x, y)) {
                    g.drawImage(cheese, px + pelletOffset, py + pelletOffset, pelletSize, pelletSize, null);
                }
            }
        }
    }

    private void drawCage(Graphics2D g) {
        int fw = cage.getWidth() / 2;
        int sx = game.cageDoorTimer > 0 ? fw : 0;
        int cx = (Maze.CAGE_X - 1) * T, cy = (Maze.CAGE_Y - 1) * T;
        g.drawImage(cage, cx, cy, cx + 3 * T, cy + 3 * T, sx, 0, sx + fw, cage.getHeight(), null);
    }

    private void drawEnemies(Graphics2D g) {
        int frame = game.animTick % 2;
        if (game.isBossLevel()) {
            int fw = boss.getHeight();
            drawFrame(g, boss, frame, fw, game.bossX, game.bossY);
            return;
        }
        for (Cat cat : game.activeCats()) {
            if (cat.eyes) {
                drawFrame(g, catEyes, frame, catEyes.getHeight(), cat.x, cat.y);
            } else if (cat.frightened && !flashWarning()) {
                drawFrame(g, catsScared, cat.index * 2 + frame, catsScared.getHeight(), cat.x, cat.y);
            } else {
                drawFrame(g, cats, cat.index * 2 + frame, cats.getHeight(), cat.x, cat.y);
            }
        }
    }

    /** In the last second of a power-up, scared cats flash back to normal as a warning. */
    private boolean flashWarning() {
        return game.powerTimer <= 10 && (game.powerTimer / 2) % 2 == 0;
    }

    private void drawPlayer(Graphics2D g) {
        Skin skin = Skin.selected();
        BufferedImage sheet = skin.sheet();
        int fw = sheet.getWidth() / skin.frameCount;
        drawFrame(g, sheet, game.animTick % skin.frameCount, fw, game.playerX, game.playerY);
    }

    private void drawDeath(Graphics2D g) {
        int fw = death.getWidth() / DEATH_FRAMES;
        int elapsed = GamePanel.DEATH_TICKS - game.deathTimer;
        int phase = Math.min(DEATH_FRAMES - 1, Math.max(0, elapsed * DEATH_FRAMES / GamePanel.DEATH_TICKS));
        drawFrame(g, death, phase, fw, game.playerX, game.playerY);
    }

    /** Draws frame {@code index} (each {@code frameWidth} px wide) of a sheet, scaled into tile (x, y). */
    private static void drawFrame(Graphics2D g, BufferedImage sheet, int index, int frameWidth, int x, int y) {
        int sx = index * frameWidth;
        int px = x * T, py = y * T;
        g.drawImage(sheet, px, py, px + T, py + T, sx, 0, sx + frameWidth, sheet.getHeight(), null);
    }

    // ---- HUD & overlays ----

    private void drawHud(Graphics2D g) {
        text(g, "Lives: " + game.lives, HUD, Color.WHITE, 20, 30);
        text(g, "Level: " + game.level, HUD, Color.WHITE, 490, 30);
        text(g, "Score: " + game.score, HUD, Color.WHITE, 10, 590);
    }

    private void drawLevelComplete(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRoundRect(150, 200, 300, 140, 20, 20);
        text(g, "Level Clear!", BIG, Color.GREEN, 185, 250);
        String next = (game.level + 1) % 4 == 0 ? "Boss level next!" : "Score: " + game.score;
        text(g, next, HUD, Color.WHITE, 220, 285);
        text(g, "Press 'R' to Advance", HUD, Color.YELLOW, 190, 320);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        text(g, "Game Over", BIG, Color.RED, 200, 250);
        text(g, "Final score: " + game.score, HUD, Color.WHITE, 215, 285);

        if (game.enteringName) {
            int cx = GamePanel.WIDTH / 2;
            text(g, "Enter Your Initials:", HUD, Color.WHITE, cx - 95, 340);
            String shown = (game.initials + "___").substring(0, 3);
            text(g, shown, BIG, Color.YELLOW, cx - 40, 390);
            text(g, "ENTER to submit  ·  BACKSPACE to delete  ·  ESC to skip", SMALL, Color.LIGHT_GRAY, cx - 195, 425);
            return;
        }

        text(g, "Press 'R' to Restart", HUD, Color.YELLOW, 200, 320);
        drawLeaderboard(g);
    }

    private void drawLeaderboard(Graphics2D g) {
        text(g, "Leaderboard", LIST, Color.WHITE, 20, 60);
        List<Leaderboard.Entry> entries = Leaderboard.load();
        Leaderboard.Entry latest = Leaderboard.lastAdded();
        boolean highlighted = false;
        int y = 90;
        for (int i = 0; i < Math.min(LEADERBOARD_ROWS, entries.size()); i++) {
            Leaderboard.Entry e = entries.get(i);
            boolean isNew = !highlighted && e.equals(latest);
            highlighted |= isNew;
            String line = String.format("%2d. %-3s %6d", i + 1, e.initials(), e.score());
            text(g, line, LIST, isNew ? Color.YELLOW : Color.WHITE, 20, y);
            y += 25;
        }
    }

    private static void text(Graphics2D g, String s, Font font, Color color, int x, int y) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(s, x, y);
    }
}
