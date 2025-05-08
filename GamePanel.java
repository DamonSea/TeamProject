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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class GamePanel extends JPanel implements ActionListener {

    public final boolean DEBUG = false;

    // Game constants
    public static final int TILE_SIZE = 30;
    public static final int ROWS = 20;
    public static final int COLS = 20;
    public static Timer timer;

    // Pacman position and direction
    public static int pacmanX = 1;
    public static int pacmanY = 1;
    public static int dx = 0;
    public static int dy = 0;
    public int lastDXMove;
    public int lastDYMove;

    public boolean mouthOpen = true;

    // Maze and collectibles
    public static int[][] maze = new int[ROWS][COLS];
    public boolean[][] dots = new boolean[ROWS][COLS];
    public boolean[][] powerPellets = new boolean[ROWS][COLS];

    // Power-up state
    public static boolean poweredUp = false;
    public int powerTimer = 0;

    // Score and game state
    public static int score = 0;
    public boolean gameWon = false;
    public static boolean gameOver = false;
    public boolean usingPreviousMap = false;
    public long lastWakaTime = 0;
    public final Random random = new Random();
    public boolean enteringName = false; // Input variable
    public String playerInitials = "";
    public boolean leaderboardShown = false;

    // Animation variables
    public Image ratSprite;
    public Image catSpriteSheet;
    public static Image bossSpriteSheet;
    public Image catScaredSpriteSheet;
    public Image catEyesSpriteSheet;
    public Image cageSpriteSheet;
    public int frame = 0;
    public final int frameCount = 4; // Number of frames for rat animation
    public int catFrame = 0;
    public final int CAT_FRAME_COUNT = 2; // Two frames per cat

    // Cat movement and animation
    public static java.util.List<Point> catPositions = new ArrayList<>();
    public static int[] catReleaseTimers = {0, 20, 40};
    public static int[] catRespawnDelay;
    public static final int RESPAWN_DELAY_TICKS = 10; // 1 second = 10 ticks
    public int catMoveCounter = 0;
    public final int CAT_MOVE_DELAY = 5; // Move every 5 ticks
    public static Point bossPosition = new Point(10,10);
    

    // per-ghost scatter state
    public static boolean[] catScattering;
    public static int[] catScatterTimer;
    public static int[] scatterDx;
    public static int[] scatterDy;
    public final int SCATTER_DURATION = 10;

    public boolean cageDoorOpen = false;
    public int cageDoorTimer = 0;
    public static final int CAGE_DOOR_OPEN_TIME = 10; // ticks (adjust if needed)

    // Lives
    public static int lives = 3;
    // timer delay
    public static boolean waitingToStart = false;
    public static int readyTimer = 0;
    Controller xboxController = null;

    public boolean dying = false;
    public int deathTimer = 0;
    public static final int DEATH_DURATION = 20;
    public Image deathSprite;
    public static final int DEATH_FRAMES   = 6;


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
        else if (DrawComponents.theme.equals("classroom"))
        {
            setBackground(new Color(100,130,130));
        }
        else
        {
            setBackground(Color.BLACK);
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
        ratSprite = new ImageIcon(SkinSelector.getSkinPath()).getImage();
        catSpriteSheet = new ImageIcon("Images/cats.png").getImage();
        bossSpriteSheet = new ImageIcon("Images/boss.png").getImage();
        catScaredSpriteSheet = new ImageIcon("Images/cats_scared.png").getImage();
        catEyesSpriteSheet = new ImageIcon("Images/cat_eyes.png").getImage();
        deathSprite = new ImageIcon("Images/rat_death.png").getImage();
        cageSpriteSheet = new ImageIcon("Images/Cage.png").getImage();



        // Add keyboard controls
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

                if (DEBUG && key == KeyEvent.VK_0)
                {
                    gameWon = true;
                    resetGame();
                }

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
                        //SwingUtilities.invokeLater(() -> {
                        //    new LeaderboardWindow().setVisible(true);
                        //});

                        // No immediate reset here; the user will view the leaderboard first
                        return;
                    }

                    if (key == KeyEvent.VK_BACK_SPACE && !playerInitials.isEmpty()) {
                        playerInitials = playerInitials.substring(0, playerInitials.length() - 1);
                    }
                    // Allow adding characters to initials if it's less than 3 characters
                    else if (playerInitials.length() < 3 && Character.isLetterOrDigit(e.getKeyChar())) {
                        playerInitials += e.getKeyChar();
                    }
                } else if (gameOver && !enteringName) {
                    if (key == KeyEvent.VK_ENTER) {
                        // Restart game when ENTER is pressed after leaderboard is shown
                        resetGame();
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

                DrawComponents.drawPowerPellets(g, powerPellets, TILE_SIZE, ROWS, COLS);
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
        if (level%4 != 0) // Not boss level
        {
            for (int i = 0; i < level%4; i++) {
                Point p = catPositions.get(i);
                drawCat(g, i, catFrame, p.x, p.y);
            }
        }
        else
        {
            DrawComponents.drawBoss(g, bossPosition.x,bossPosition.y, frame);
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
            DrawComponents.drawScore(g, score);
            g.setColor(Color.WHITE);
            g.drawString("Lives: " + lives, 20, 30);
            return;  // nothing else should overpaint it
        }

        int frameCount = switch (SkinSelector.selectedSkin) {
            case "Joe_Oakes" -> 7;
            case "Squirrel" -> 4;
            default -> 4;
        };

        // 4) normal Pac‑Man
        DrawComponents.drawPacman(g, ratSprite, this,
                frame, frameCount,
                TILE_SIZE, pacmanX, pacmanY,
                poweredUp, mouthOpen
        );

        // 5) UI, level, messages
        DrawComponents.drawScore(g, score);
        g.setColor(Color.WHITE);
        g.drawString("Lives: " + lives, 20, 30);
        DrawComponents.drawLevel(g, level);

        if (gameWon) {
            DrawComponents.drawWinMessage(g);
            DrawComponents.drawAdvanceLevelMessage(g);
        } else if (gameOver) {
            // Clear the screen for a clean transition
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            DrawComponents.drawGameOverMessage(g);
            DrawComponents.drawRestartMessage(g);

            // Immortalize player Initial
            if (enteringName) {
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.drawString("Enter Your Initials:", getWidth() / 2 - 100, getHeight() / 2 + 40);
                g.drawString(playerInitials, getWidth() / 2 - 20, getHeight() / 2 + 70);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("(Press ENTER to submit)", getWidth() / 2 - 80, getHeight() / 2 + 95);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("(Press Backspace to Delete)", getWidth() / 2 - 80, getHeight() / 2 + 115);
            } else {
            // Draw leaderboard
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            //g.drawString("Leaderboard:", getWidth() / 2 - 60, getHeight() / 2 + 30); //center
            g.drawString("Leaderboard:", 20, 70); // top left

            //Retrieve Leaderboard score
            List<String> scores = Leaderboard.getLeaderboard();
            //int y = getHeight() / 2 + 50; // Center position for leaderboard entries
                int y = 100; // Start position for leaderboard entries

            for (String score : scores) {
                if (score.startsWith("* ")) {
                    g.setColor(Color.YELLOW); // Highlight new score in yellow
                    score = score.replace("* ", "").replace(" *", ""); // Remove markers for display
                } else {
                    g.setColor(Color.WHITE);
                }
                //g.drawString(score, getWidth() / 2 - 60, y);//center
                g.drawString(score, 20, y); // Move to top left under LeaderBoard
                y += 25; // Move down for each entry
            }
            }
        }


        if (waitingToStart) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("READY!", getWidth()/2 - 50, getHeight()/2);
        }
    }

    public void drawCat(Graphics g, int catIndex, int frame, int x, int y) {
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

                // Check if the game is over OR game is won, then check for reset button
                if (gameOver || gameWon) {
                    if (componentName.equals("Button 6") && value == 1.0f) {
                        resetGame();
                        return;
                    }
                }

                if (!gameOver && !gameWon && !dying && !waitingToStart && !enteringName) {
                    if (componentName.equals("Hat Switch")) {
                        if (value == 0.25f) { dx = 0; dy = -1; } // Up
                        else if (value == 0.50f) { dx = 1; dy = 0; } // Right
                        else if (value == 0.75f) { dx = 0; dy = 1; } // Down
                        else if (value == 1.00f) { dx = -1; dy = 0; } // Left
                    }

                    if (comp.getIdentifier() == net.java.games.input.Component.Identifier.Axis.X) {
                        if (joyValue < -0.5f) { dx = -1; dy = 0; }  // move left
                        else if (joyValue > 0.5f) { dx = 1; dy = 0; }  // move right
                    } else if (comp.getIdentifier() == net.java.games.input.Component.Identifier.Axis.Y) {
                        if (joyValue < -0.5f) { dx = 0; dy = -1; }  // move up
                        else if (joyValue > 0.5f) { dx = 0; dy = 1; }  // move down
                    }
                } else if (gameOver && enteringName) {

                }
            }
        }

        if (dying) {
            if (--deathTimer <= 0) {
                dying = false;
                lives--;

                if (lives <= 0) {
                    gameOver = true;
                    enteringName = true;
                    repaint();
                } else {
                    resetPositions(); // Reset positions for next life
                    waitingToStart = true;
                    readyTimer = 20;
                }
            }
            repaint();
            return;
        }

        if (gameOver || gameWon) {
            repaint();
            return;
        }



        if (waitingToStart) {
            readyTimer--;
            if (readyTimer <= 0) {
                waitingToStart = false;
            }
            repaint();
            return;
        }

        if (!gameOver && !gameWon && checkWin()) {
            gameWon = true;
            repaint();
            return;
        }

        movePacman();

        // Animate Pacman's mouth
        mouthOpen = !mouthOpen;
        frame = (frame + 1) % frameCount;

        // Animate cats
        catFrame = (catFrame + 1) % CAT_FRAME_COUNT;

        // Handle power-up timer
        if (poweredUp) {
            powerTimer--;
            if (powerTimer <= 0) {
                poweredUp = false;
            }
        }

        // Throttle cat movement
        if (level%4 != 0) {
            catMoveCounter++;
            if (catMoveCounter >= CAT_MOVE_DELAY) {
                moveGhost(); // Has its own "if (gameWon || gameOver) return;"
                catMoveCounter = 0;
            }
        }
        else
        {
            Movement.moveBoss();
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

    public void movePacman() {
        if (gameWon || gameOver) return;

        if (pacmanX == 0)
        {
            pacmanX = 18;
        }
        else if (pacmanX == 19)
        {
            pacmanX = 1;
        }

        // 1) store old Pac-Man pos
        int prevPX = pacmanX, prevPY = pacmanY;
        int newX = 0;
        int newY = 0;

        // 2) compute next cell
        newX = pacmanX + dx;
        newY = pacmanY + dy;
        if (dx != 0 || dy != 0)
        {
            newX = pacmanX + dx;
            newY = pacmanY + dy;

            if (!(newX < 0 || newX >= COLS || newY < 0 || newY >= ROWS || maze[newY][newX] == 1))
            {
                lastDXMove = dx;
                lastDYMove = dy;
                dx=0;
                dy=0;
            }
            else
            {
                newX = newX - dx + lastDXMove;
                newY = newY - dy + lastDYMove;
            }

        }
        else
        {
            newX = pacmanX + lastDXMove;
            newY = pacmanY + lastDYMove;
        }


        // 3) only move if not a wall
        if (newX < 0 || newX >= COLS || newY < 0 || newY >= ROWS || maze[newY][newX] == 1)
        {
            return;
        }

        pacmanX = newX;
        pacmanY = newY;

        // 4) eat dots
        if (dots[pacmanY][pacmanX]) {
            dots[pacmanY][pacmanX] = false;
            score += 10;
            long now = System.currentTimeMillis();
            if (now - lastWakaTime >= 1000) {
                SoundPlayer.playSound("sounds/waka.wav");
                lastWakaTime = now;
            }
            if (checkWin()) {
                gameWon = true;
                timer.stop();
                return;
            }
        }

        // 5) eat power pellet
        if (powerPellets[pacmanY][pacmanX]) {
            powerPellets[pacmanY][pacmanX] = false;
            poweredUp = true;
            powerTimer = 40;
            score += 50;
            SoundPlayer.playSound("sounds/powerup.wav");
        }

        // 6) now handle any ghost collisions
        for (int i = 0; i < catPositions.size(); i++) {
            Point cat = catPositions.get(i);

            if (cat.x == pacmanX && cat.y == pacmanY) {
                if (poweredUp && !catScattering[i]) {
                    // trigger scatter
                    catScattering[i] = true;
                    catScatterTimer[i] = SCATTER_DURATION;
                    catRespawnDelay[i] = 0; // start delay counter from zero
                    score += 100;
                    SoundPlayer.playSound("sounds/eatghost.wav");
                    scatterDx[i] = Integer.compare(10, cat.x);
                    scatterDy[i] = Integer.compare(10, cat.y);

                    // set target direction TOWARD cage center (10,10)
                    int dxToCage = Integer.compare(10, cat.x);
                    int dyToCage = Integer.compare(10, cat.y);
                    scatterDx[i] = dxToCage;
                    scatterDy[i] = dyToCage;
                }
                else if (!poweredUp && !catScattering[i] && !dying) {
                    // start death animation instead of instantly resetting
                    dying= true;
                    deathTimer = DEATH_DURATION;
                    // play the death sound
                    SoundPlayer.playSound("sounds/pacman_death.wav");
                    return;

                }
            }
        }
    }

    public static void resetPositions() {
        pacmanX = 1;
        pacmanY = 1;
        dx = 0;
        dy = 0;

        catPositions.clear();
        catPositions.add(new Point(9, 10));
        catPositions.add(new Point(10, 10));
        catPositions.add(new Point(11, 10));

        bossPosition = new Point(10,10);

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

    public void moveGhost() {
        if (gameWon || gameOver) return;
        if (level % 4 != 0)
        {
            Movement.moveCats();
        }
        else
        {
            Movement.moveBoss();
        }

    }
    

    public boolean checkWin() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (dots[row][col] || powerPellets[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int level = 1; // start from level one

    public void resetGame() {
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
        mouthOpen = true;
        poweredUp = false;
        usingPreviousMap = false;
        powerTimer = 0;
        frame = 0;
        catFrame = 0;
        catMoveCounter = 0;
        lastDXMove = 0;
        lastDYMove = 0;
        bossPosition = new Point(10,10);
        if (DEBUG)
        {
            lives = 999;
        }
        else
        {
            lives = 3;
        }
        enteringName = false;
        playerInitials = "";
        leaderboardShown = false;

        if (level%3 == 1)
        {
            DrawComponents.theme = "outdoor";
        }
        else if (level%3 == 2)
        {
            DrawComponents.theme = "classroom";
        }
        else
        {
            DrawComponents.theme = "planets";
        }
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
