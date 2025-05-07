import MazeGeneration.MazeGeneration;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class GamePanel extends JPanel implements ActionListener {


    private Player player = new Player();
    private GameManager gameManager = new GameManager();

    // Game constants
    private final int TILE_SIZE = 30;
    private final int ROWS = 20;
    private final int COLS = 20;
    private Timer timer;

    // Pacman position and direction
    private int pacmanX = 1;
    private int pacmanY = 1;
    private int dx = 0;
    private int dy = 0;
    private int lastDXMove;
    private int lastDYMove;

//    private boolean mouthOpen = true;

    // Maze and collectibles
    private int[][] maze = new int[ROWS][COLS];
    private boolean[][] dots = new boolean[ROWS][COLS];
    private boolean[][] powerPellets = new boolean[ROWS][COLS];

    // Power-up state
    private boolean poweredUp = false;
//    private int powerTimer = 0;

    // Score and game state
    private int score = 0;
    private boolean gameWon = false;
    private boolean gameOver = false;
    private boolean usingPreviousMap = false;
//    private long lastWakaTime = 0;
    private final Random random = new Random();
    private boolean enteringName = false; // Input variable
    private String playerInitials = "";
    private boolean leaderboardShown = false;

    // Animation variables
    private Image ratSprite;
    private Image catSpriteSheet;
    private Image catScaredSpriteSheet;
    private Image catEyesSpriteSheet;
    private Image cageSpriteSheet;
    private int frame = 0;
    private final int frameCount = 4; // Number of frames for rat animation
    private int catFrame = 0;
    private final int CAT_FRAME_COUNT = 2; // Two frames per cat

    // Cat movement and animation
    private java.util.List<Point> catPositions = new ArrayList<>();
    private int[] catReleaseTimers = {0, 20, 40};
    private int[] catRespawnDelay;
    private static final int RESPAWN_DELAY_TICKS = 10; // 1 second = 10 ticks
    private int catMoveCounter = 0;
    private final int CAT_MOVE_DELAY = 5; // Move every 5 ticks

    // per-ghost scatter state
    private boolean[] catScattering;
    private int[] catScatterTimer;
    private int[] scatterDx, scatterDy;
    private final int SCATTER_DURATION = 10;

    private boolean cageDoorOpen = false;
    private int cageDoorTimer = 0;
    private static final int CAGE_DOOR_OPEN_TIME = 10; // ticks (adjust if needed)

    // Lives
    private int lives = 3;
    // timer delay
    private boolean waitingToStart = false;
    private int readyTimer = 0;
    Controller xboxController = null;

    private boolean dying = false;
    private int deathTimer = 0;
    private static final int DEATH_DURATION = 20;
    private Image deathSprite;
    private static final int DEATH_FRAMES   = 6;


    //constuctor
    public GamePanel() throws IOException {
        // Find the Xbox controller
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller controller : controllers) {
            if (controller.getType() == Controller.Type.GAMEPAD) {
                xboxController = controller;
                break;
            }
        }

        if (xboxController == null) {
            System.out.println("Xbox Controller not found! Running keyboard-only mode.");
        }

        DrawComponents.loadSprites();

        if (DrawComponents.theme.equals("standard"))
        {
            setBackground(Color.BLACK);
        }
        else if (DrawComponents.theme.equals("outdoor"))
        {
            setBackground(new Color(150,110,0));
        }
        setFocusable(true);

        // Generate initial maze
        MazeGeneration.generateRandomMaze();
        maze = MazeGeneration.getMaze();
        dots = MazeGeneration.getDots();
        powerPellets = MazeGeneration.getPowerPellets();

        // Play intro sound
        SoundPlayer.playSound("sounds/intro.wav");

        // Load sprites
        ratSprite = new ImageIcon("Images/Rat.png").getImage();
        catSpriteSheet = new ImageIcon("Images/cats.png").getImage();
        catScaredSpriteSheet = new ImageIcon("Images/cats_scared.png").getImage();
        catEyesSpriteSheet = new ImageIcon("Images/cat_eyes.png").getImage();
        deathSprite = new ImageIcon("Images/rat_death.png").getImage();
        cageSpriteSheet = new ImageIcon("Images/Cage.png").getImage();



        // Add keyboard controls
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

                //Advance to next level if won and reset game if over press R
                if ((gameWon || gameOver) && key == KeyEvent.VK_R) {
                    resetGame(); // Reset the game if game over or game won
                    return;
                }

                if (gameOver && enteringName) {
                    // Capture initials when Enter is pressed and initials are at least 3 characters long
                    if (!leaderboardShown && key == KeyEvent.VK_ENTER && playerInitials.length() >= 3) {
                        // Save score when Enter is pressed
                        Leaderboard.saveScore(playerInitials, score);
                        enteringName = false;  // Exit name entry mode
                        leaderboardShown = true; // prevent showing again

                        // Trigger repaint first to clear initials UI
                        repaint();

                        // Delay leaderboard window so it opens after repaint
                        SwingUtilities.invokeLater(() -> {
                            new LeaderboardWindow().setVisible(true);
                        });

                        // No immediate reset here; the user will view the leaderboard first
                        return;
                    }

                    if (key == KeyEvent.VK_BACK_SPACE && playerInitials.length() > 0) {
                        playerInitials = playerInitials.substring(0, playerInitials.length() - 1);
                    }
                    // Allow adding characters to initials if it's less than 3 characters
                    else if (playerInitials.length() < 3 && Character.isLetterOrDigit(e.getKeyChar())) {
                        playerInitials += e.getKeyChar();
                    }
                }

                // Game controls RatMan
                if (!enteringName) {
                    switch (key) {
                        case KeyEvent.VK_LEFT -> { dx = -1; dy = 0; }
                        case KeyEvent.VK_RIGHT -> { dx = 1; dy = 0; }
                        case KeyEvent.VK_UP -> { dx = 0; dy = -1; }
                        case KeyEvent.VK_DOWN -> { dx = 0; dy = 1; }
                    }
                }
            }
        });

        // Timer for game updates (100 ms)
        timer = new Timer(100, this);
        timer.start();
        resetGame();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1) background: maze, dots, pellets
        try {
            DrawComponents.drawMaze(g, maze, TILE_SIZE, ROWS, COLS, usingPreviousMap);
            usingPreviousMap = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                // Skip cage zone (3x3 center area)
                if (col >= 9 && col <= 11 && row >= 9 && row <= 11) continue;

                if (dots[row][col]) {
                    g.setColor(Color.WHITE);
                    g.fillOval(col * TILE_SIZE + TILE_SIZE / 3, row * TILE_SIZE + TILE_SIZE / 3, TILE_SIZE / 3, TILE_SIZE / 3);
                }

                if (powerPellets[row][col]) {
                    g.setColor(Color.YELLOW);
                    g.fillOval(col * TILE_SIZE + TILE_SIZE / 4, row * TILE_SIZE + TILE_SIZE / 4, TILE_SIZE / 2, TILE_SIZE / 2);
                }
            }
        }

        int cageFrameWidth = cageSpriteSheet.getWidth(null) / 2;
        int cageFrameHeight = cageSpriteSheet.getHeight(null);
        int sx = cageDoorOpen ? cageFrameWidth : 0;

        int cageX = 9 * TILE_SIZE;
        int cageY = 9 * TILE_SIZE;

        g.drawImage(cageSpriteSheet,
                cageX, cageY,
                cageX + TILE_SIZE * 3, cageY + TILE_SIZE * 3,
                sx, 0, sx + cageFrameWidth, cageFrameHeight,
                this
        );

        // 2) draw all ghosts
        for (int i = 0; i < catPositions.size(); i++) {
            Point p = catPositions.get(i);
            drawCat(g, i, catFrame, p.x, p.y);
        }

        // 3) death animation overlay
        if (dying) {
            int w     = deathSprite.getWidth(null)  / DEATH_FRAMES;
            int h     = deathSprite.getHeight(null);
            int phase = ((DEATH_DURATION - deathTimer) * DEATH_FRAMES) / DEATH_DURATION;
            phase = Math.max(0, Math.min(DEATH_FRAMES - 1, phase));

            // draw the strip at Pac‑Man’s last position
            g.drawImage(deathSprite,
                    pacmanX * TILE_SIZE,            pacmanY * TILE_SIZE,
                    pacmanX * TILE_SIZE + TILE_SIZE,pacmanY * TILE_SIZE + TILE_SIZE,
                    phase * w, 0, phase * w + w, h,
                    this
            );

            // draw UI on top
            DrawComponents.drawScore(g, player.getScore());
            g.setColor(Color.WHITE);
            g.drawString("Lives: " + lives, 20, 30);
            return;  // nothing else should overpaint it
        }

        // 4) normal Pac‑Man
        DrawComponents.drawPacman(g, ratSprite, this,
                player.getFrame(), 4,
                TILE_SIZE, player.getX(), player.getY(),
                player.isPoweredUp(), player.isMouthOpen());


        // 5) UI, level, messages
        DrawComponents.drawScore(g, player.getScore());
        g.setColor(Color.WHITE);
        g.drawString("Lives: " + lives, 20, 30);
        DrawComponents.drawLevel(g, level);

        if (gameWon) {
            DrawComponents.drawWinMessage(g);
            DrawComponents.drawAdvanceLevelMessage(g);
        } else if (gameOver) {
            DrawComponents.drawGameOverMessage(g);
            DrawComponents.drawRestartMessage(g);
            if (enteringName) {
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.drawString("Enter Your Initials:", getWidth() / 2 - 100, getHeight() / 2 + 40);
                g.drawString(playerInitials, getWidth() / 2 - 20, getHeight() / 2 + 70);
                g.setFont(new Font("Arial", Font.PLAIN, 14));
                g.drawString("(Press ENTER to submit)", getWidth() / 2 - 80, getHeight() / 2 + 95);
            }
        }

        if (waitingToStart) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("READY!", getWidth()/2 - 50, getHeight()/2);
        }
    }

    private void drawCat(Graphics g, int catIndex, int frame, int x, int y) {
        Image sprite;
        int frameWidth;
        int frameHeight;

        // Pick the correct ghost sprite depending on its state
        if (catScattering[catIndex]) {
            // Ghost was eaten — show just the eyes
            sprite = catEyesSpriteSheet;
            frameWidth = 64;   // this sprite sheet uses 64×64 frames
            frameHeight = 64;
        } else if (poweredUp) {
            // Pac-Man (the rat) ate a power pellet — ghost is scared (turn blue)
            sprite = catScaredSpriteSheet;
            frameWidth = 64;
            frameHeight = 64;
        } else {
            // Regular ghost chasing the player — use default ghost sprite
            sprite = catSpriteSheet;
            frameWidth = 30;   // default ghost sprites are 30×30
            frameHeight = 30;
        }

        // Pick which frame to show from the sprite sheet (each ghost has 2 frames for animation)
        int frameIndex = (catScattering[catIndex]) ? frame : catIndex * CAT_FRAME_COUNT + frame;
        int sx = frameIndex * frameWidth;
        int sy = 0;

        // Draw the selected sprite frame, scaled down to fit the 30×30 grid
        g.drawImage(sprite,
                x * TILE_SIZE, y * TILE_SIZE,
                x * TILE_SIZE + TILE_SIZE, y * TILE_SIZE + TILE_SIZE,
                sx, sy, sx + frameWidth, sy + frameHeight,
                this
        );
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (xboxController != null) {
            xboxController.poll();
            net.java.games.input.EventQueue queue = xboxController.getEventQueue();
            net.java.games.input.Event event = new net.java.games.input.Event();
            while (queue.getNextEvent(event)) {
                Component comp = event.getComponent();
                float joyValue = event.getValue();
                String componentName = event.getComponent().getName();
                float value = event.getValue();

                if (value == 1.0f) {
                    if (componentName.equals("Button 6")) {
                        resetGame();
                    }
                }

                if (componentName.equals("Hat Switch")) {
                    if (value == 0.25f) { // Up
                        dx = 0;
                        dy = -1;
                    } else if (value == 0.50f) { // Right
                        dx = 1;
                        dy = 0;
                    } else if (value == 0.75f) { // Down
                        dx = 0;
                        dy = 1;
                    } else if (value == 1.00f) { // Left
                        dx = -1;
                        dy = 0;
                    } else if (value == 0.0f) {}
                }

                if (comp.getIdentifier() == net.java.games.input.Component.Identifier.Axis.X) {
                    if (joyValue < -0.5f) { dx = -1; dy = 0; }  // move left
                    else if (joyValue > 0.5f) { dx = 1; dy = 0; }  // move right
                }
                else if (comp.getIdentifier() == net.java.games.input.Component.Identifier.Axis.Y) {
                    if (joyValue < -0.5f) { dx = 0; dy = -1; }  // move up
                    else if (joyValue > 0.5f) { dx = 0; dy = 1; }  // move down
                }

            }
        }

        if (dying) {
            if (--deathTimer <= 0) {
                dying = false;
                lives--;

                if (lives <= 0) {
                    gameOver = true;
                    enteringName = true; // prompt for initials input
                    //Leaderboard.saveScore(playerInitials, score); // Save the player's score when the game is over
                    timer.stop();
                    repaint();

                    //SwingUtilities.invokeLater(() -> {
                    //    new LeaderboardWindow().setVisible(true); // Show the leaderboard after the game ends
                    //});

                    return;
                }

                resetPositions();
                waitingToStart = true;
                readyTimer = 20;
            }

            repaint();
            return;
        }
        movePacman();

        if (waitingToStart) {
            readyTimer--;
            if (readyTimer <= 0) {
                waitingToStart = false;
            }
            repaint();
            return; // skip moving pacman/ghosts while waiting
        }

        if (!gameOver && !gameWon && checkWin()) {
            gameWon = true;
            timer.stop();
            repaint();
            return;
        }

        // Animate Pacman's mouth
        player.updateAnimation();


        // Animate cats
        catFrame = (catFrame + 1) % CAT_FRAME_COUNT;

        // Handle power-up timer
        player.tickPowerUp();


        // Throttle cat movement
        catMoveCounter++;
        if (catMoveCounter >= CAT_MOVE_DELAY) {
            moveGhost();
            catMoveCounter = 0;
        }

        // Cage door animation timer
        if (cageDoorOpen && cageDoorTimer > 0) {
            cageDoorTimer--;
            if (cageDoorTimer == 0) {
                cageDoorOpen = false;
            }
        }

        repaint();
    }

    // Move Pacman

    private void movePacman() {
        player.move(
                maze, dots, powerPellets,
                catPositions, catScattering,
                catScatterTimer, catRespawnDelay,
                scatterDx, scatterDy,
                gameManager
        );
    }

    private void resetPositions() {
        pacmanX = 1;
        pacmanY = 1;
        dx = 0;
        dy = 0;

        catPositions.clear();
        catPositions.add(new Point(9, 10));
        catPositions.add(new Point(10, 10));
        catPositions.add(new Point(11, 10));

        catReleaseTimers = new int[]{0, 20, 40};

        int n = catPositions.size();
        catRespawnDelay = new int[n];
        catScattering   = new boolean[n];
        catScatterTimer = new int   [n];
        scatterDx       = new int   [n];
        scatterDy       = new int   [n];

        waitingToStart = true;
        readyTimer = 20; // about 2 seconds if timer interval = 100ms

    }

    private void moveGhost() {
        if (gameWon || gameOver) return;

        for (int i = 0; i < catPositions.size(); i++) {
            if (i >= catScattering.length || i >= catRespawnDelay.length || i >= catReleaseTimers.length) continue;  // Prevent out-of-bounds error

            // cat scatter moves back to cage
            if (catScattering[i]) {
                Point cat = catPositions.get(i);
                int targetX = 10, targetY = 10; // position of cage

                // If eyes have reached the cage
                if (cat.x == targetX && cat.y == targetY) {
                    if (catRespawnDelay[i] < RESPAWN_DELAY_TICKS) {
                        catRespawnDelay[i]++;
                    } else {
                        catScattering[i] = false;
                        catRespawnDelay[i] = 0;
                        catReleaseTimers[i] = 10;
                        //catPositions.set(i, new Point(9, 10)); // just outside cage
                        // add cat position to move cats eye
                        if (i % 3 == 0) {
                            catPositions.set(i, new Point(9, 10)); // Left side of cage
                        } else if (i % 3 == 1) {
                            catPositions.set(i, new Point(10, 10)); // Center of cage
                        } else {
                            catPositions.set(i, new Point(11, 10)); // Right side of cage
                        }
                    }
                    continue;
                }

                // Move eyes toward cage
                int dx = Integer.compare(targetX, cat.x);
                int dy = Integer.compare(targetY, cat.y);
                int nx = cat.x + dx;
                int ny = cat.y + dy;

                if (nx >= 0 && nx < COLS && ny >= 0 && ny < ROWS &&
                        (maze[ny][nx] == 0 || (nx >= 9 && nx <= 11 && ny >= 9 && ny <= 11))) {
                    catPositions.set(i, new Point(nx, ny));
                }

                continue;
            }

            // Wait for release timer
            if (catReleaseTimers[i] > 0) {
                catReleaseTimers[i]--;
                continue;
            }

            // Normal chase logic
            Point cat = catPositions.get(i);
            int bestDx = 0, bestDy = 0, minDist = Integer.MAX_VALUE;
            int[] dxs = {-1, 1, 0, 0}, dys = {0, 0, -1, 1};

            for (int j = 0; j < 4; j++) {
                int tx = cat.x + dxs[j];
                int ty = cat.y + dys[j];

                if (tx < 0 || tx >= COLS || ty < 0 || ty >= ROWS) continue;
                if (maze[ty][tx] == 1) continue;

                int dist = Math.abs(tx - pacmanX) + Math.abs(ty - pacmanY);
                if (dist < minDist) {
                    minDist = dist;
                    bestDx = dxs[j];
                    bestDy = dys[j];
                }
            }

            int nextX = cat.x + bestDx;
            int nextY = cat.y + bestDy;

            if (nextX == pacmanX && nextY == pacmanY) {
                if (poweredUp) {
                    catScattering[i] = true; //cat turns in to eyes
                    catRespawnDelay[i] = 0;
                    SoundPlayer.playSound("sounds/eatghost.wav");
                    score += 100;
                    continue;
                } else {
                    lives--;
                    if (lives > 0) resetPositions();
                    else {
                        gameOver = true;
                        timer.stop();
                    }
                    return;
                }
            }

            boolean occupied = false;
            for (int j = 0; j < catPositions.size(); j++) {
                if (j != i && catPositions.get(j).equals(new Point(nextX, nextY))) {
                    occupied = true;
                    break;
                }
            }

            if (!occupied) {
                catPositions.set(i, new Point(nextX, nextY));
            }
        }
    }

    private boolean checkWin() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (dots[row][col] || powerPellets[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }

    private int level = 1; // start from level one

    private void resetGame() {
        if (gameWon) {
            level++; // Advance to next level
        } else {
            level = 1; // reset to level 1 if lost
        }
        pacmanX = 1;
        pacmanY = 1;
        dx = 0;
        dy = 0;
        score = 0;
        gameWon = false;
        gameOver = false;
//        mouthOpen = true;
        poweredUp = false;
        usingPreviousMap = false;
//        powerTimer = 0;
        frame = 0;
        catFrame = 0;
        catMoveCounter = 0;
        lives = 3;
        enteringName = false;
        playerInitials = "";
        leaderboardShown = false;


        //generates a new maze map based on the level
        MazeGeneration.generateRandomMaze();
        maze = MazeGeneration.getMaze();
        dots = MazeGeneration.getDots();
        powerPellets = MazeGeneration.getPowerPellets();
        // Make the 3x3 cage area solid and empty
        for (int row = 9; row <= 11; row++) {
            for (int col = 9; col <= 11; col++) {
                maze[row][col] = 1; // block the cage walls
                dots[row][col] = false;
                powerPellets[row][col] = false;
            }
        }

        //  Open the cage center AND one entrance (e.g., from below)
        maze[10][10] = 0; // center
        maze[11][10] = 0; // entrance from bottom
        maze[9][10] = 0; // allow ghost to exit upward after revive

        // Reset cat positions
        catPositions.clear(); // add to clear previous level cat positions
        catPositions.add(new Point(9, 10));
        catPositions.add(new Point(10, 10));
        catPositions.add(new Point(11, 10));

        catReleaseTimers = new int[]{0, 20, 40};

        int n = catPositions.size();
        catRespawnDelay = new int[n];
        catScattering   = new boolean[n];
        catScatterTimer = new int   [n];
        scatterDx       = new int   [n];
        scatterDy       = new int   [n];

        // reset the cat arrays
        Arrays.fill(catScattering, false);
        Arrays.fill(catRespawnDelay, 0);
        Arrays.fill(catScatterTimer, 0);
        Arrays.fill(scatterDx, 0);
        Arrays.fill(scatterDy, 0);

        timer.start();
        requestFocusInWindow();
        repaint();
    }
}
