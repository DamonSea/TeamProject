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
    static final int CAT_MOVE_DELAY = 5;     // cats move every 5 ticks
    static final int POWER_TICKS = 40;       // 4 s of power-up
    static final int DEATH_TICKS = 20;       // death animation length
    static final int READY_TICKS = 20;       // "READY!" pause
    static final int CAGE_DOOR_TICKS = 10;
    static final int START_LIVES = 3;
    static final long WAKA_COOLDOWN_MS = 1000;

    static final int DOT_POINTS = 10;
    static final int PELLET_POINTS = 50;
    static final int CAT_POINTS = 100;

    private final Random random = new Random();
    private final Timer timer = new Timer(TICK_MS, this);
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
    int moveDx, moveDy;       // current direction
    int queuedDx, queuedDy;   // buffered turn, applied as soon as it's possible
    Facing facing = Facing.RIGHT; // which way the player sprite points

    /** Sprites are drawn facing right; the renderer mirrors or rotates them for the others. */
    enum Facing { RIGHT, LEFT, UP, DOWN }
    boolean poweredUp;
    int powerTimer;

    // ---- enemies ----
    final List<Cat> cats = List.of(
            new Cat(0, Maze.CAGE_X - 1, Maze.CAGE_Y, 0),
            new Cat(1, Maze.CAGE_X, Maze.CAGE_Y, 20),
            new Cat(2, Maze.CAGE_X + 1, Maze.CAGE_Y, 40));
    int bossX, bossY;
    int bossDirX, bossDirY;
    int catMoveCounter;
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
    private long lastWakaTime;

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
        startLevel();
        SoundPlayer.play(Sound.INTRO);
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

        gameWon = false;
        gameOver = false;
        enteringName = false;
        paused = false;
        dying = false;
        initials = "";
        animTick = 0;
        catMoveCounter = 0;
        resetPositions();
        repaint();
    }

    /** Puts everyone back at their start positions (new level or after losing a life). */
    void resetPositions() {
        playerX = maze.startX();
        playerY = maze.startY();
        moveDx = moveDy = queuedDx = queuedDy = 0;
        facing = Facing.RIGHT;
        poweredUp = false;
        powerTimer = 0;

        cats.forEach(Cat::reset);
        bossX = Maze.CAGE_X;
        bossY = Maze.CAGE_Y;
        bossDirX = bossDirY = 0;
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
        tick();
        repaint();
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

        if (dying) {
            if (--deathTimer <= 0) finishDeath();
            return;
        }
        if (gameOver || gameWon) return;

        if (waitingToStart) {
            if (--readyTimer <= 0) waitingToStart = false;
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
        } else if (++catMoveCounter >= CAT_MOVE_DELAY) {
            catMoveCounter = 0;
            CatAI.moveCats(this);
        }
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

        int nx = wrapX(playerX + moveDx);
        int ny = playerY + moveDy;
        if (!maze.isWalkable(nx, ny)) return;

        playerX = nx;
        playerY = ny;
        facing = moveDx > 0 ? Facing.RIGHT : moveDx < 0 ? Facing.LEFT : moveDy < 0 ? Facing.UP : Facing.DOWN;

        if (maze.hasDot(playerX, playerY)) {
            maze.eatDot(playerX, playerY);
            score += DOT_POINTS;
            long now = System.currentTimeMillis();
            if (now - lastWakaTime >= WAKA_COOLDOWN_MS) {
                SoundPlayer.play(Sound.WAKA);
                lastWakaTime = now;
            }
        }
        if (maze.hasPellet(playerX, playerY)) {
            maze.eatPellet(playerX, playerY);
            score += PELLET_POINTS;
            poweredUp = true;
            powerTimer = POWER_TICKS;
            frightenCats();
            SoundPlayer.play(Sound.POWER_UP);
        }

        if (maze.isCleared()) {
            gameWon = true;
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
        SoundPlayer.play(Sound.EAT_CAT);
    }

    private void killPlayer() {
        dying = true;
        deathTimer = DEATH_TICKS;
        SoundPlayer.play(Sound.DEATH);
    }

    private void finishDeath() {
        dying = false;
        lives--;
        if (lives <= 0) {
            gameOver = true;
            enteringName = true;
            initials = "";
        } else {
            resetPositions();
        }
    }

    void openCageDoor() {
        cageDoorTimer = CAGE_DOOR_TICKS;
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
        repaint();
    }

    void resume() {
        paused = false;
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
