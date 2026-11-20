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

    // The game's own retro fonts (same as the menus); all text is drawn with a dark outline
    // (The menus' blocky RetroNakjoy font has no digits, so in-game text uses the bubbly one.)
    private static final String BUBBLY = "/fonts/GinkobRetroFont.ttf";
    private static final Font HUD = Assets.font(BUBBLY, 21f);
    private static final Font SCORE = Assets.font(BUBBLY, 23f);
    private static final Font BIG = Assets.font(BUBBLY, 46f);
    private static final Font READY = Assets.font(BUBBLY, 36f);
    private static final Font LIST = Assets.font(BUBBLY, 19f);
    private static final Font SMALL = Assets.font(BUBBLY, 14f);
    private static final Font POPUP = Assets.font(BUBBLY, 18f);

    private static final Color CHEESE_YELLOW = new Color(255, 205, 50);
    private static final Color HUD_BAR = new Color(15, 12, 25, 185);

    // Sprite sheets (one row of frames each)
    private final BufferedImage cats = Assets.image("/images/cats.png");            // 3 cats x 2 frames, 30px
    private final BufferedImage catsScared = Assets.image("/images/cats_scared.png"); // 3 cats x 2 frames, 64px
    private final BufferedImage catEyes = Assets.image("/images/cat_eyes.png");     // 64px frames
    private final BufferedImage boss = Assets.image("/images/Boss.png");            // 2 frames, 100px
    private final BufferedImage cage = Assets.image("/images/Cage.png");            // closed, open
    private final BufferedImage death = Assets.image("/images/rat_death.png");      // 6 frames, 64px
    private final BufferedImage cheese = Assets.image("/images/pelletcheese.png");

    private static final int DEATH_FRAMES = 6;

    /**
     * On-screen size of the visible artwork (sprite files have a lot of empty margin, so we crop
     * to the drawing first). Bigger than the 30px tile, centred on it; collisions still use tiles.
     */
    private static final int PLAYER_SIZE = 42;
    private static final int CAT_SIZE = 42;
    private static final int BOSS_WIDTH = 84;

    /** Square area (relative to each frame) that contains a sheet's actual drawing. */
    private record ArtBox(int x, int y, int size) {}
    private final java.util.Map<BufferedImage, ArtBox> artBoxes = new java.util.IdentityHashMap<>();
    private static final Color OUTLINE = new Color(20, 20, 20);

    /** Black silhouettes of sprite sheets, used to draw an outline around the player. */
    private final java.util.Map<BufferedImage, BufferedImage> silhouettes = new java.util.IdentityHashMap<>();

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

        if (game.paused) {
            drawPauseMenu(g);
        } else if (game.gameWon) {
            drawLevelComplete(g);
        } else if (game.gameOver) {
            drawGameOver(g);
        } else if (game.waitingToStart) {
            int y = (Maze.CAGE_Y + 3) * T - 4; // just below the cage
            centered(g, "READY!", READY, CHEESE_YELLOW, y + 4);
            centered(g, "Level " + game.level + "  -  " + game.maze.name(), SMALL, Color.WHITE, y + 26);
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
                if (maze.isWall(x, y) && !Maze.inCage(x, y)) { // the cage draws its own floor
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

    private static final Color CAGE_FLOOR = new Color(125, 105, 80);
    private static final Color CAGE_FRAME = new Color(40, 30, 20);

    private void drawCage(Graphics2D g) {
        int fw = cage.getWidth() / 2;
        int sx = game.cageDoorTimer > 0 ? fw : 0;
        int cx = (Maze.CAGE_X - 1) * T, cy = (Maze.CAGE_Y - 1) * T;
        int size = 3 * T;
        // The whole 3x3 cage is a solid room (no maze tiles inside) with a dark frame
        g.setColor(CAGE_FLOOR);
        g.fillRect(cx, cy, size, size);
        g.setColor(CAGE_FRAME);
        g.setStroke(new java.awt.BasicStroke(3));
        g.drawRect(cx + 1, cy + 1, size - 3, size - 3);
        g.setStroke(new java.awt.BasicStroke(1));
        // Cage.png: bars at x 1-28, y 10-25; the open frame's door swings up into y 2-9.
        // Crop x 1-29, y 2-26 and fit it to the room, bottom-aligned.
        int srcX = sx + 1, srcY = 2, srcW = 28, srcH = 24;
        int dw = size - 8, dh = dw * srcH / srcW;
        int dx = cx + 4, dy = cy + size - 4 - dh;
        g.drawImage(cage, dx, dy, dx + dw, dy + dh, srcX, srcY, srcX + srcW, srcY + srcH, null);
    }

    private void drawEnemies(Graphics2D g) {
        int frame = game.animTick % 2;
        if (game.isBossLevel()) {
            int fw = boss.getHeight();
            ArtBox box = artBox(boss, fw);
            int cx = (int) Math.round(game.slide(game.bossPrevX, game.bossX, game.bossMoveTick, 1) * T) + T / 2;
            int cy = (int) Math.round(game.slide(game.bossPrevY, game.bossY, game.bossMoveTick, 1) * T) + T / 2;
            int sx = frame * fw + box.x();
            g.drawImage(boss, cx - BOSS_WIDTH / 2, cy - BOSS_WIDTH / 2, cx + BOSS_WIDTH / 2, cy + BOSS_WIDTH / 2,
                    sx, box.y(), sx + box.size(), box.y() + box.size(), null);
            return;
        }
        for (Cat cat : game.activeCats()) {
            // pixel centre of the cat, sliding smoothly between tiles
            int cx = (int) Math.round(game.slide(cat.prevX, cat.x, cat.moveTick, cat.moveTicks) * T) + T / 2;
            int cy = (int) Math.round(game.slide(cat.prevY, cat.y, cat.moveTick, cat.moveTicks) * T) + T / 2;
            if (cat.eyes) {
                // The eyes sheet is just eyes, so cropping it would blow them up. Draw the whole frame
                // at the same pixel scale as the scared-cat sheet (same 64px frames) so they line up.
                int fullFrame = CAT_SIZE * catsScared.getHeight() / artBox(catsScared, catsScared.getHeight()).size();
                int px = cx - fullFrame / 2, py = cy - fullFrame / 2;
                int fw = catEyes.getHeight(), sx = frame * fw;
                g.drawImage(catEyes, px, py, px + fullFrame, py + fullFrame, sx, 0, sx + fw, fw, null);
            } else if (cat.frightened && !flashWarning()) {
                drawSprite(g, catsScared, cat.index * 2 + frame, catsScared.getHeight(), cx, cy, CAT_SIZE);
            } else {
                drawSprite(g, cats, cat.index * 2 + frame, cats.getHeight(), cx, cy, CAT_SIZE);
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
        drawFacing(g, sheet, game.animTick % skin.frameCount, fw);
    }

    private void drawDeath(Graphics2D g) {
        int fw = death.getWidth() / DEATH_FRAMES;
        int elapsed = GamePanel.DEATH_TICKS - game.deathTimer;
        int phase = Math.min(DEATH_FRAMES - 1, Math.max(0, elapsed * DEATH_FRAMES / GamePanel.DEATH_TICKS));
        drawFacing(g, death, phase, fw);
    }

    /** Draws frame {@code index} of a sheet, cropped to its artwork, {@code size} px square, centred on pixel (cx, cy). */
    private void drawSprite(Graphics2D g, BufferedImage sheet, int index, int frameWidth, int cx, int cy, int size) {
        ArtBox box = artBox(sheet, frameWidth);
        int sx = index * frameWidth + box.x();
        int px = cx - size / 2, py = cy - size / 2;
        g.drawImage(sheet, px, py, px + size, py + size, sx, box.y(), sx + box.size(), box.y() + box.size(), null);
    }

    /**
     * Draws a player frame turned to face the direction of travel. Sprites face right,
     * so left is a mirror image and up/down are 90-degree rotations.
     */
    private void drawFacing(Graphics2D g, BufferedImage sheet, int index, int frameWidth) {
        java.awt.geom.AffineTransform saved = g.getTransform();
        double px = game.dying ? game.playerX : game.slide(game.prevX, game.playerX, game.playerMoveTick, 1);
        double py = game.dying ? game.playerY : game.slide(game.prevY, game.playerY, game.playerMoveTick, 1);
        g.translate(px * T + T / 2.0, py * T + T / 2.0);
        switch (game.facing) {
            case LEFT -> g.scale(-1, 1);
            case UP -> g.rotate(-Math.PI / 2);
            case DOWN -> g.rotate(Math.PI / 2);
            default -> { }
        }
        int half = PLAYER_SIZE / 2;
        ArtBox box = artBox(sheet, frameWidth);
        int sx = index * frameWidth + box.x(), sy = box.y(), ex = sx + box.size(), ey = sy + box.size();

        // Outline: the sprite's black silhouette nudged 2px in each direction, then the sprite on top
        BufferedImage outline = silhouette(sheet);
        int o = 2;
        for (int[] d : new int[][]{{-o, 0}, {o, 0}, {0, -o}, {0, o}}) {
            g.drawImage(outline, -half + d[0], -half + d[1], half + d[0], half + d[1], sx, sy, ex, ey, null);
        }
        g.drawImage(sheet, -half, -half, half, half, sx, sy, ex, ey, null);
        g.setTransform(saved);
    }

    /** Finds the smallest square (same for every frame) that holds the sheet's visible pixels. */
    private ArtBox artBox(BufferedImage sheet, int frameWidth) {
        return artBoxes.computeIfAbsent(sheet, src -> {
            int frames = src.getWidth() / frameWidth, h = src.getHeight();
            int minX = frameWidth, minY = h, maxX = -1, maxY = -1;
            for (int f = 0; f < frames; f++) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < frameWidth; x++) {
                        if ((src.getRGB(f * frameWidth + x, y) >>> 24) == 0) continue;
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < 0) return new ArtBox(0, 0, Math.min(frameWidth, h));
            int size = Math.max(maxX - minX + 1, maxY - minY + 1);
            // centre the square on the drawing, kept inside the frame
            int x = Math.max(0, Math.min(frameWidth - size, (minX + maxX + 1 - size) / 2));
            int y = Math.max(0, Math.min(h - size, (minY + maxY + 1 - size) / 2));
            return new ArtBox(x, y, Math.min(size, Math.min(frameWidth, h)));
        });
    }

    private BufferedImage silhouette(BufferedImage sheet) {
        return silhouettes.computeIfAbsent(sheet, src -> {
            BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int rgb = OUTLINE.getRGB() & 0xFFFFFF;
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int alpha = src.getRGB(x, y) >>> 24;
                    if (alpha > 0) out.setRGB(x, y, (alpha << 24) | rgb);
                }
            }
            return out;
        });
    }

    // ---- HUD & overlays ----

    /**
     * Top bar: lives as little player icons, level in the middle, score with a cheese on the right.
     * Bottom bar: best score, power-up meter, maze name. Bars sit over the outer wall rows.
     */
    private void drawHud(Graphics2D g) {
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        int w = GamePanel.WIDTH, h = GamePanel.HEIGHT;
        g.setColor(HUD_BAR);
        g.fillRect(0, 0, w, T);
        g.fillRect(0, h - T, w, T);

        // lives
        BufferedImage icon = Skin.selected().icon();
        int shown = Math.min(game.lives, 5);
        for (int i = 0; i < shown; i++) {
            g.drawImage(icon, 8 + i * 27, 3, 24, 24, null);
        }
        if (game.lives > 5) text(g, "x" + game.lives, HUD, Color.WHITE, 8 + 5 * 27 + 2, 23);

        // level
        centered(g, "LEVEL " + game.level, HUD, CHEESE_YELLOW, 23);

        // score, arcade style with leading zeros, counting up as points come in
        String score = String.format("%06d", game.displayedScore);
        int sw = g.getFontMetrics(SCORE).stringWidth(score);
        text(g, score, SCORE, Color.WHITE, w - 10 - sw, 24);
        g.drawImage(cheese, w - 10 - sw - 30, 3, 25, 25, null);

        // bottom bar: best score (yours if you're beating it)
        List<Leaderboard.Entry> top = Leaderboard.load();
        int best = top.isEmpty() ? 0 : top.get(0).score();
        String bestText = game.score > best ? "BEST  YOU  " + game.score
                : "BEST  " + (top.isEmpty() ? "---" : top.get(0).initials()) + "  " + best;
        text(g, bestText, SMALL, game.score > best ? CHEESE_YELLOW : Color.LIGHT_GRAY, 10, h - 9);

        String mazeName = game.maze.name().toUpperCase();
        text(g, mazeName, SMALL, Color.LIGHT_GRAY, w - 10 - g.getFontMetrics(SMALL).stringWidth(mazeName), h - 9);

        // power-up meter: a cheese-coloured bar that drains (and blinks near the end)
        if (game.poweredUp) {
            int barW = 140, barX = CX - barW / 2, barY = h - T + 10;
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(barX - 2, barY - 2, barW + 4, 14, 8, 8);
            boolean blink = game.powerTimer <= 10 && (game.powerTimer / 2) % 2 == 0;
            g.setColor(blink ? Color.WHITE : CHEESE_YELLOW);
            g.fillRoundRect(barX, barY, barW * game.powerTimer / GamePanel.POWER_TICKS, 10, 6, 6);
        }

        // floating "+100" popups that rise and fade
        for (GamePanel.Popup p : game.popups) {
            float life = 1f - (float) p.age() / GamePanel.POPUP_TICKS;
            java.awt.Composite old = g.getComposite();
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0f, life)));
            int px = p.x() * T + T / 2, py = p.y() * T - p.age() * 2;
            centeredAt(g, p.text(), POPUP, CHEESE_YELLOW, px, py);
            g.setComposite(old);
        }
    }

    private void drawPauseMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);
        int top = CY - 140;
        box(g, 260, 260, top, new Color(20, 20, 20, 230));
        centered(g, "PAUSED", BIG, CHEESE_YELLOW, top + 62);
        for (int i = 0; i < GamePanel.PAUSE_OPTIONS.length; i++) {
            boolean selected = i == game.pauseSelection;
            String label = selected ? "> " + GamePanel.PAUSE_OPTIONS[i] + " <" : GamePanel.PAUSE_OPTIONS[i];
            centered(g, label, HUD, selected ? Color.YELLOW : Color.WHITE, top + 115 + i * 40);
        }
        centered(g, "Arrows + Enter  /  P to resume", SMALL, Color.LIGHT_GRAY, top + 240);
    }

    private void drawLevelComplete(Graphics2D g) {
        int top = CY - 85;
        box(g, 300, 140, top, new Color(0, 0, 0, 190));
        centered(g, "Level Clear!", BIG, Color.GREEN, top + 50);
        String next = (game.level + 1) % 4 == 0 ? "Boss level next!" : "Score: " + game.score;
        centered(g, next, HUD, Color.WHITE, top + 85);
        centered(g, "Press 'R' to Advance", HUD, Color.YELLOW, top + 120);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        if (game.enteringName) {
            centered(g, "Game Over", BIG, Color.RED, CY - 50);
            centered(g, "Final score: " + game.score, HUD, Color.WHITE, CY - 15);
            centered(g, "Enter Your Initials:", HUD, Color.WHITE, CY + 40);
            centered(g, (game.initials + "___").substring(0, 3), BIG, Color.YELLOW, CY + 90);
            centered(g, "Enter = save   Backspace = delete   Esc = skip", SMALL, Color.LIGHT_GRAY, CY + 125);
            return;
        }

        // Leaderboard on the left, result on the right so they never overlap
        int rx = GamePanel.WIDTH * 2 / 3;
        centeredAt(g, "Game Over", BIG, Color.RED, rx, CY - 50);
        centeredAt(g, "Final score: " + game.score, HUD, Color.WHITE, rx, CY - 15);
        centeredAt(g, "Press 'R' to Restart", HUD, Color.YELLOW, rx, CY + 20);
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
            Color c = isNew ? Color.YELLOW : Color.WHITE;
            // columns: rank, initials, right-aligned score (the font isn't monospaced)
            String rank = (i + 1) + ".";
            text(g, rank, LIST, c, 52 - g.getFontMetrics(LIST).stringWidth(rank), y);
            text(g, e.initials(), LIST, c, 62, y);
            String sc = Integer.toString(e.score());
            text(g, sc, LIST, c, 200 - g.getFontMetrics(LIST).stringWidth(sc), y);
            y += 26;
        }
    }

    private static final int CX = GamePanel.WIDTH / 2;
    private static final int CY = GamePanel.HEIGHT / 2;

    private static void centered(Graphics2D g, String s, Font font, Color color, int y) {
        centeredAt(g, s, font, color, CX, y);
    }

    private static void centeredAt(Graphics2D g, String s, Font font, Color color, int cx, int y) {
        text(g, s, font, color, cx - g.getFontMetrics(font).stringWidth(s) / 2, y);
    }

    private static void box(Graphics2D g, int w, int h, int top, Color color) {
        g.setColor(color);
        g.fillRoundRect(CX - w / 2, top, w, h, 22, 22);
    }

    /**
     * Text with a dark outline so it reads on any background. Characters the font doesn't have
     * are drawn with a standard bold font instead of showing up as empty boxes.
     */
    private static void text(Graphics2D g, String s, Font font, Color color, int x, int y) {
        java.awt.geom.Path2D.Float shape = new java.awt.geom.Path2D.Float();
        float cursor = x;
        java.awt.font.FontRenderContext frc = g.getFontRenderContext();
        Font fallback = new Font(Font.DIALOG, Font.BOLD, Math.round(font.getSize2D()));
        int i = 0;
        while (i < s.length()) {
            boolean ok = font.canDisplay(s.charAt(i));
            int j = i;
            while (j < s.length() && font.canDisplay(s.charAt(j)) == ok) j++;
            Font f = ok ? font : fallback;
            java.awt.font.GlyphVector glyphs = f.createGlyphVector(frc, s.substring(i, j));
            shape.append(glyphs.getOutline(cursor, y), false);
            cursor += (float) glyphs.getLogicalBounds().getWidth();
            i = j;
        }
        Object aa = g.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);
        java.awt.Stroke oldStroke = g.getStroke();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new java.awt.BasicStroke(Math.max(2f, font.getSize2D() / 7f),
                java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.setColor(OUTLINE);
        g.draw(shape);
        g.setColor(color);
        g.fill(shape);
        g.setStroke(oldStroke);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, aa);
    }
}
