package ratman;

import ratman.SoundPlayer.Sound;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Random;

/**
 * The game screen: holds all game state, runs the 100 ms game loop, and handles input.
 * Drawing lives in {@link Renderer}; cat movement lives in {@link CatAI}.
 */
final class GamePanel extends JPanel implements ActionListener {

    /** Run with -Dratman.debug=true for 999 lives and the "0" key to clear a level. */
    static final boolean DEBUG = Boolean.getBoolean("ratman.debug");

    static final int TILE = 30;
    static final int WIDTH = Maze.COLS * TILE;
    static final int HEIGHT = Maze.ROWS * TILE;

    static final int TICK_MS = 100;
    static final int POWER_TICKS = 40;       // 4 s of power-up (HUD meter drains over this)
    static final int DEATH_TICKS = 20;       // death animation length
    static final int READY_TICKS = 20;       // "READY!" pause
    static final int CAGE_DOOR_TICKS = 10;
    static final int START_LIVES = 3;

    static final int DOT_POINTS = 10;
    static final int PELLET_POINTS = 50;
    static final int CAT_POINTS = 100;

    private final Random random = new Random();
    /**
     * The game logic still steps every 100 ms (TICK_MS), but the screen redraws ~60 times a second
     * and characters slide smoothly between tiles in between steps.
     */
    static final int FRAME_MS = 16;
    private static final long TICK_NANOS = TICK_MS * 1_000_000L;
    private final Timer timer = new Timer(FRAME_MS, this);
    private long lastTickNanos;
    long tickCount;
    private final ControllerInput controller;
    private final Renderer renderer = new Renderer(this);

    // ---- world ----
    Maze maze;
    Theme theme;
    int level = 1;
    int score;
    int lives;

    // ---- player ----
    int playerX, playerY;
    int prevX, prevY;          // where the player was before the last step (for smooth drawing)
    long playerMoveTick;
    int moveDx, moveDy;       // current direction
    int queuedDx, queuedDy;   // buffered turn, applied as soon as it's possible
    Facing facing = Facing.RIGHT; // which way the player sprite points

    /** Sprites are drawn facing right; the renderer mirrors or rotates them for the others. */
    enum Facing { RIGHT, LEFT, UP, DOWN }
    boolean poweredUp;
    int powerTimer;

    // ---- enemies ----
    final List<Cat> cats = List.of(
            new Cat(0, Cat.Personality.CHASER, Maze.CAGE_X - 1, Maze.CAGE_Y, 0),
            new Cat(1, Cat.Personality.AMBUSHER, Maze.CAGE_X, Maze.CAGE_Y, 30),
            new Cat(2, Cat.Personality.FLANKER, Maze.CAGE_X + 1, Maze.CAGE_Y, 60));
    int bossX, bossY;
    int bossDirX, bossDirY;

    /** The boss chases, but every so often lines up and POUNCES down a corridor. */
    enum BossState { CHASE, WINDUP, DASH, DIZZY }
    static final int BOSS_INTRO_TICKS = 45;    // cinematic entrance (instead of the normal READY)
    static final int BOSS_DEFEAT_TICKS = 30;   // flash, spin, explode, then Level Clear
    static final int BOSS_EXPLODE_AT = 12;     // ticks left in the defeat sequence when it blows up
    static final int BOSS_BONUS = 1000;
    BossState bossState = BossState.CHASE;
    int bossStateTimer;
    int bossCooldown;
    int dashDx, dashDy, dashTravelled;
    boolean bossIntro;
    boolean bossDefeated;
    int bossDefeatTimer;

    /** Particles and screen shake (visual only). */
    final Effects effects = new Effects();
    boolean deathPopped;                       // skins without death art pop into pixels mid-animation
    int bossPrevX, bossPrevY;
    long bossMoveTick;
    int ticksIntoLife;                    // drives the cats' scatter/chase waves
    CatAI.Mode catMode = CatAI.Mode.SCATTER;
    int cageDoorTimer;

    // ---- flow ----
    boolean waitingToStart;
    int readyTimer;
    boolean dying;
    int deathTimer;
    boolean gameWon;
    boolean gameOver;
    boolean enteringName;
    String initials = "";

    // ---- pause menu ----
    static final String[] PAUSE_OPTIONS = {"Resume", "Restart", "Main Menu"};
    boolean paused;
    int pauseSelection;

    int animTick;
    private boolean munchToggle;   // alternates the two "nom" sounds

    // ---- HUD effects ----
    /** Floating "+100" style labels; drawn by the renderer, aged every tick. */
    record Popup(int x, int y, String text, int age) {}
    static final int POPUP_TICKS = 12;
    final java.util.List<Popup> popups = new java.util.ArrayList<>();
    int displayedScore;            // rolls up toward score so points "count in"

    GamePanel() {
        this(ControllerInput.detect());
    }

    GamePanel(ControllerInput controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode(), e.getKeyChar());
            }
        });
        // Pause automatically if the window loses focus (e.g. you click another app)
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (canPause()) pause();
            }
        });
        startNewGame();
    }

    void start() {
        lastTickNanos = 0;
        timer.start();
        requestFocusInWindow();
    }

    void stop() {
        timer.stop();
    }

    // =====================================================================
    // Game flow
    // =====================================================================

    void startNewGame() {
        level = 1;
        score = 0;
        displayedScore = 0;
        startLevel();
    }

    void nextLevel() {
        level++;
        startLevel();
    }

    private void startLevel() {
        lives = DEBUG ? 999 : START_LIVES;
        theme = Theme.forLevel(level);
        maze = Settings.randomMazes() ? Maze.generate(random, theme) : Maze.designed(level, random, theme);
        setBackground(theme.background);

        SoundPlayer.stopMusic();
        popups.clear();
        effects.clear();
        bossDefeated = false;
        bossDefeatTimer = 0;
        gameWon = false;
        gameOver = false;
        enteringName = false;
        paused = false;
        dying = false;
        initials = "";
        animTick = 0;
        resetPositions();
        if (isBossLevel()) {
            // Cinematic entrance: warning siren, then the boss rises out of the cage with a roar
            bossIntro = true;
            readyTimer = BOSS_INTRO_TICKS;
            SoundPlayer.play(Sound.BOSS_WARNING);
        } else {
            SoundPlayer.play(Sound.START);
        }
        repaint();
    }

    /** Puts everyone back at their start positions (new level or after losing a life). */
    void resetPositions() {
        playerX = maze.startX();
        playerY = maze.startY();
        prevX = playerX;
        prevY = playerY;
        moveDx = moveDy = queuedDx = queuedDy = 0;
        facing = Facing.RIGHT;
        poweredUp = false;
        powerTimer = 0;

        cats.forEach(Cat::reset);
        bossX = Maze.CAGE_X;
        bossY = Maze.CAGE_Y;
        bossPrevX = bossX;
        bossPrevY = bossY;
        ticksIntoLife = 0;
        catMode = CatAI.Mode.SCATTER;
        bossDirX = bossDirY = 0;
        bossState = BossState.CHASE;
        bossStateTimer = 0;
        bossCooldown = 40;
        bossIntro = false;
        deathPopped = false;
        cageDoorTimer = 0;

        waitingToStart = true;
        readyTimer = READY_TICKS;
    }

    boolean isBossLevel() {
        return level % 4 == 0;
    }

    /** Levels 1-3 have 1-3 cats, level 4 is the boss, then it repeats. */
    List<Cat> activeCats() {
        return isBossLevel() ? List.of() : cats.subList(0, level % 4);
    }

    boolean isPlaying() {
        return !gameOver && !gameWon && !dying && !waitingToStart;
    }

    // =====================================================================
    // Game loop
    // =====================================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        // Fixed-step game logic: run one tick per 100 ms of real time, redraw every frame
        long now = System.nanoTime();
        if (lastTickNanos == 0) lastTickNanos = now;
        int steps = 0;
        while (now - lastTickNanos >= TICK_NANOS && steps < 5) {
            tick();
            lastTickNanos += TICK_NANOS;
            steps++;
        }
        if (now - lastTickNanos >= TICK_NANOS) lastTickNanos = now; // fell far behind (e.g. laptop slept)
        repaint();
    }

    /** How far (0..1) real time is between the last tick and the next, for sliding sprites. */
    double frameAlpha() {
        if (lastTickNanos == 0 || paused) return 1;
        return Math.min(1, Math.max(0, (System.nanoTime() - lastTickNanos) / (double) TICK_NANOS));
    }

    /** Smoothed position (in tiles) between a previous and current tile. */
    double slide(int from, int to, long startTick, double lengthTicks) {
        if (Math.abs(to - from) > Maze.COLS / 2) return to; // tunnel wrap: don't slide across the whole screen
        double progress = (tickCount - startTick + frameAlpha()) / Math.max(1e-9, lengthTicks);
        progress = Math.min(1, Math.max(0, progress));
        return from + (to - from) * progress;
    }

    /** Direction the player is heading (or facing, if stopped). Used by the Ambusher and Flanker. */
    int[] playerDirection() {
        if (moveDx != 0 || moveDy != 0) return new int[]{moveDx, moveDy};
        return switch (facing) {
            case LEFT -> new int[]{-1, 0};
            case UP -> new int[]{0, -1};
            case DOWN -> new int[]{0, 1};
            default -> new int[]{1, 0};
        };
    }

    /** One 100 ms step of the game. */
    void tick() {
        controller.poll(new ControllerInput.Listener() {
            @Override
            public void onDirection(int dx, int dy) {
                if (paused) movePauseSelection(dy);
                else if (!gameOver && !gameWon) queueDirection(dx, dy);
            }

            @Override
            public void onStart() {
                handleStartButton();
            }
        });

        if (paused) return;
        tickCount++;
        updateHudEffects();

        if (dying) {
            --deathTimer;
            if (!Skin.selected().hasDeathArt && !deathPopped && deathTimer <= DEATH_TICKS * 45 / 100) {
                deathPopped = true;
                effects.burst(Skin.selected().icon(), playerX * TILE + TILE / 2.0, playerY * TILE + TILE / 2.0, 42, 1.0);
                effects.shake(0.25, 4);
                SoundPlayer.play(Sound.POP);
            }
            if (deathTimer <= 0) finishDeath();
            return;
        }
        if (bossDefeated && !gameWon) {
            updateBossDefeat();
            return;
        }
        if (gameOver || gameWon) return;

        if (waitingToStart) {
            if (bossIntro && readyTimer == BOSS_INTRO_TICKS - 15) {
                SoundPlayer.play(Sound.BOSS_ROAR);
                effects.shake(1.1, 7);
            }
            if (--readyTimer <= 0) {
                waitingToStart = false;
                bossIntro = false;
                SoundPlayer.playMusic(isBossLevel() ? SoundPlayer.Track.BOSS : SoundPlayer.Track.LEVEL);
            }
            return;
        }

        movePlayer();
        if (gameWon || dying) return;

        animTick++;

        if (poweredUp && --powerTimer <= 0) {
            poweredUp = false;
            cats.forEach(c -> c.frightened = false);
        }

        if (isBossLevel()) {
            CatAI.moveBoss(this);
        } else {
            CatAI.moveCats(this);
        }
        ticksIntoLife++;
        checkCollisions();

        if (cageDoorTimer > 0) cageDoorTimer--;
    }

    private void movePlayer() {
        // Take the buffered turn if that direction is open; otherwise keep going straight.
        if ((queuedDx != 0 || queuedDy != 0) && canEnter(playerX + queuedDx, playerY + queuedDy)) {
            moveDx = queuedDx;
            moveDy = queuedDy;
            queuedDx = queuedDy = 0;
        }

        prevX = playerX;
        prevY = playerY;
        playerMoveTick = tickCount;
        int nx = wrapX(playerX + moveDx);
        int ny = playerY + moveDy;
        if (!maze.isWalkable(nx, ny)) return;

        playerX = nx;
        playerY = ny;
        facing = moveDx > 0 ? Facing.RIGHT : moveDx < 0 ? Facing.LEFT : moveDy < 0 ? Facing.UP : Facing.DOWN;

        if (maze.hasDot(playerX, playerY)) {
            maze.eatDot(playerX, playerY);
            score += DOT_POINTS;
            munchToggle = !munchToggle;
            SoundPlayer.play(munchToggle ? Sound.MUNCH_A : Sound.MUNCH_B);
        }
        if (maze.hasPellet(playerX, playerY)) {
            maze.eatPellet(playerX, playerY);
            score += PELLET_POINTS;
            poweredUp = true;
            powerTimer = POWER_TICKS;
            frightenCats();
            popups.add(new Popup(playerX, playerY, "+" + PELLET_POINTS, 0));
            SoundPlayer.play(Sound.CHEESE);
        }

        if (maze.isCleared()) {
            SoundPlayer.stopMusic();
            if (isBossLevel()) {
                // Defeat sequence first; Level Clear comes when it finishes
                bossDefeated = true;
                bossDefeatTimer = BOSS_DEFEAT_TICKS;
                bossState = BossState.CHASE;
                SoundPlayer.play(Sound.BOSS_ROAR);
                effects.shake(0.6, 4);
            } else {
                gameWon = true;
                SoundPlayer.play(Sound.LEVEL_CLEAR);
            }
            return;
        }
        checkCollisions();
    }

    private boolean canEnter(int x, int y) {
        return maze.isWalkable(wrapX(x), y);
    }

    /** The side tunnels wrap around to the other edge. */
    private static int wrapX(int x) {
        return Math.floorMod(x, Maze.COLS);
    }

    /** Single place that decides what happens when the player and an enemy share a tile. */
    void checkCollisions() {
        if (dying) return;

        if (isBossLevel()) {
            if (bossX == playerX && bossY == playerY) killPlayer();
            return;
        }
        for (Cat cat : activeCats()) {
            if (!cat.isAt(playerX, playerY) || cat.eyes) continue;
            if (cat.frightened) {
                eatCat(cat);
            } else {
                killPlayer();
                return;
            }
        }
    }

    /** Every cat currently out (not eyes) gets scared and turns around, like in Pac-Man. */
    private void frightenCats() {
        for (Cat cat : activeCats()) {
            if (cat.eyes) continue;
            cat.frightened = true;
            cat.dirX = -cat.dirX;
            cat.dirY = -cat.dirY;
        }
    }

    private void eatCat(Cat cat) {
        cat.eyes = true;
        cat.frightened = false;
        cat.respawnTimer = 0;
        score += CAT_POINTS;
        popups.add(new Popup(cat.x, cat.y, cat.personality.catName.toUpperCase() + " +" + CAT_POINTS, 0));
        SoundPlayer.play(Sound.EAT_CAT);
    }

    /** For tests. */
    void killPlayerForTest() {
        killPlayer();
    }

    private void killPlayer() {
        dying = true;
        deathTimer = DEATH_TICKS;
        SoundPlayer.stopMusic();
        SoundPlayer.play(Sound.DEATH);
    }

    private void finishDeath() {
        dying = false;
        lives--;
        if (lives <= 0) {
            gameOver = true;
            enteringName = true;
            initials = "";
            SoundPlayer.play(Sound.GAME_OVER);
        } else {
            resetPositions();
        }
    }

    void openCageDoor() {
        cageDoorTimer = CAGE_DOOR_TICKS;
        SoundPlayer.play(Sound.CAT_FREE);
    }

    /** Boss defeat: it shakes and spins, explodes (bonus points!), then Level Clear. */
    private void updateBossDefeat() {
        bossDefeatTimer--;
        if (bossDefeatTimer == BOSS_EXPLODE_AT) {
            double cx = bossX * TILE + TILE / 2.0, cy = bossY * TILE + TILE / 2.0;
            effects.burst(Assets.cropToArt(Assets.image("/images/Boss.png"), 0, 100), cx, cy, 84, 1.6);
            effects.sparks(cx, cy, 60, new java.awt.Color(255, 220, 60), new java.awt.Color(255, 120, 40), java.awt.Color.WHITE);
            effects.shake(0.9, 10);
            score += BOSS_BONUS;
            popups.add(new Popup(bossX, bossY, "BOSS BONUS +" + BOSS_BONUS, 0));
            SoundPlayer.play(Sound.BOSS_EXPLODE);
        }
        if (bossDefeatTimer <= 0) {
            gameWon = true;
            SoundPlayer.play(Sound.BOSS_DEFEAT);
        }
    }

    private void updateHudEffects() {
        popups.replaceAll(p -> new Popup(p.x(), p.y(), p.text(), p.age() + 1));
        popups.removeIf(p -> p.age() > POPUP_TICKS);
        if (displayedScore > score) displayedScore = score;
        if (displayedScore < score) displayedScore += Math.max(1, (score - displayedScore) / 3);
    }

    // =====================================================================
    // Input
    // =====================================================================

    void queueDirection(int dx, int dy) {
        queuedDx = dx;
        queuedDy = dy;
    }

    void handleKey(int key, char ch) {
        if (enteringName) {
            handleInitialsKey(key, ch);
            return;
        }
        if (paused) {
            handlePauseKey(key);
            return;
        }
        if ((key == KeyEvent.VK_P || key == KeyEvent.VK_ESCAPE) && canPause()) {
            pause();
            return;
        }
        if (gameOver) {
            if (key == KeyEvent.VK_R || key == KeyEvent.VK_ENTER) startNewGame();
            return;
        }
        if (gameWon) {
            if (key == KeyEvent.VK_R || key == KeyEvent.VK_ENTER) nextLevel();
            return;
        }
        if (DEBUG && key == KeyEvent.VK_0) {
            maze.clearAll();
            gameWon = true;
            return;
        }
        switch (key) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> queueDirection(-1, 0);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> queueDirection(1, 0);
            case KeyEvent.VK_UP, KeyEvent.VK_W -> queueDirection(0, -1);
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> queueDirection(0, 1);
            default -> { }
        }
    }

    private void handleInitialsKey(int key, char ch) {
        if (key == KeyEvent.VK_ENTER && initials.length() == 3) {
            submitInitials();
        } else if (key == KeyEvent.VK_ESCAPE) {
            enteringName = false; // skip saving
        } else if (key == KeyEvent.VK_BACK_SPACE && !initials.isEmpty()) {
            initials = initials.substring(0, initials.length() - 1);
        } else if (initials.length() < 3 && Character.isLetterOrDigit(ch)) {
            initials += Character.toUpperCase(ch);
        }
    }

    private void submitInitials() {
        Leaderboard.saveScore(initials, score);
        enteringName = false;
    }

    // ---- pause menu ----

    /** Can pause during play or the READY countdown, but not on the death/win/game-over screens. */
    boolean canPause() {
        return !paused && !gameOver && !gameWon && !dying && !enteringName;
    }

    void pause() {
        paused = true;
        pauseSelection = 0;
        SoundPlayer.pauseMusic();
        SoundPlayer.play(Sound.PAUSE);
        repaint();
    }

    void resume() {
        paused = false;
        SoundPlayer.play(Sound.PAUSE);
        SoundPlayer.resumeMusic();
        requestFocusInWindow();
        repaint();
    }

    private void handlePauseKey(int key) {
        switch (key) {
            case KeyEvent.VK_P, KeyEvent.VK_ESCAPE -> resume();
            case KeyEvent.VK_UP, KeyEvent.VK_W -> movePauseSelection(-1);
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> movePauseSelection(1);
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> choosePauseOption();
            default -> { }
        }
        repaint();
    }

    private void movePauseSelection(int delta) {
        if (delta == 0) return;
        pauseSelection = Math.floorMod(pauseSelection + delta, PAUSE_OPTIONS.length);
        SoundPlayer.play(Sound.CLICK);
        repaint();
    }

    private void choosePauseOption() {
        switch (pauseSelection) {
            case 0 -> resume();
            case 1 -> {
                paused = false;
                startNewGame();
            }
            default -> {
                paused = false;
                RatMan.showMenu();
            }
        }
    }

    private void handleStartButton() {
        if (paused) {
            choosePauseOption();
            return;
        }
        if (canPause()) {
            pause();
            return;
        }
        if (enteringName) {
            if (initials.length() == 3) submitInitials();
            else enteringName = false;
        } else if (gameOver) {
            startNewGame();
        } else if (gameWon) {
            nextLevel();
        }
    }

    // =====================================================================
    // Drawing
    // =====================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.draw((Graphics2D) g);
    }
}
