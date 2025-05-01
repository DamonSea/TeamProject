import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import MazeGeneration.MazeGeneration;

public class PacManGame {

    public static void main(String[] args) throws IOException {
        // Set up the game window
        JFrame frame = new JFrame("Simple Pacman");
        GamePanel panel = new GamePanel();

        frame.add(panel);
        frame.setSize(600, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener {

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

    private boolean mouthOpen = true;

    // Maze and collectibles
    private int[][] maze = new int[ROWS][COLS];
    private boolean[][] dots = new boolean[ROWS][COLS];
    private boolean[][] powerPellets = new boolean[ROWS][COLS];

    // Power-up state
    private boolean poweredUp = false;
    private int powerTimer = 0;

    // Score and game state
    private int score = 0;
    private boolean gameWon = false;
    private boolean gameOver = false;

    private boolean usingPreviousMap = false;

    private long lastWakaTime = 0;

    private final Random random = new Random();

    // Animation variables
    private Image ratSprite;
    private Image catSpriteSheet;
    private Image catScaredSpriteSheet;
    private Image catEyesSpriteSheet;
    private int frame = 0;
    private final int frameCount = 4; // Number of frames for rat animation
    private int catFrame = 0;
    private final int CAT_FRAME_COUNT = 2; // Two frames per cat

    // Cat movement and animation
    private List<Point> catPositions = new ArrayList<>();
    private int[] catReleaseTimers = {0, 20, 40};
    private int catMoveCounter = 0;
    private final int CAT_MOVE_DELAY = 5; // Move every 5 ticks

    // per-ghost scatter state
    private boolean[] catScattering;
    private int[] catScatterTimer;
    private int[] scatterDx, scatterDy;
    private final int SCATTER_DURATION = 10;


    public GamePanel() throws IOException {
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
        ratSprite = new ImageIcon("Rat.png").getImage();
        catSpriteSheet = new ImageIcon("cats.png").getImage();
        catScaredSpriteSheet = new ImageIcon("cats_scared.png").getImage();
        catEyesSpriteSheet = new ImageIcon("cat_eyes.png").getImage();


        // Add keyboard controls
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if ((gameWon || gameOver) && key == KeyEvent.VK_R) {
                    resetGame();
                } else {
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
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw all game elements
        try {
            DrawComponents.drawMaze(g, maze, TILE_SIZE, ROWS, COLS, usingPreviousMap);
            usingPreviousMap = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DrawComponents.drawDots(g, dots, TILE_SIZE, ROWS, COLS);
        DrawComponents.drawPowerPellets(g, powerPellets, TILE_SIZE, ROWS, COLS);
        DrawComponents.drawPacman(g, ratSprite, this, frame, frameCount, TILE_SIZE, pacmanX, pacmanY, poweredUp, mouthOpen);

        // Draw each cat
        for (int i = 0; i < catPositions.size(); i++) {
            Point p = catPositions.get(i);
            drawCat(g, i, catFrame, p.x, p.y);
        }

        DrawComponents.drawScore(g, score);

        // Draw win/lose messages
        if (gameWon) {
            DrawComponents.drawWinMessage(g);
            DrawComponents.drawRestartMessage(g);
        }
        if (gameOver) {
            DrawComponents.drawGameOverMessage(g);
            DrawComponents.drawRestartMessage(g);
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
        int frameIndex = catIndex * CAT_FRAME_COUNT + frame;
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
        catMoveCounter++;
        if (catMoveCounter >= CAT_MOVE_DELAY) {
            moveGhost();
            catMoveCounter = 0;
        }

        repaint();
    }

    private void movePacman() {
        if (gameWon || gameOver) return;

        // 1) store old Pac-Man pos
        int prevPX = pacmanX, prevPY = pacmanY;

        // 2) compute next cell
        int newX = pacmanX + dx;
        int newY = pacmanY + dy;

        // 3) only move if not a wall
        if (newX < 0 || newX >= COLS || newY < 0 || newY >= ROWS || maze[newY][newX] == 1)
            return;

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
                    catScattering[i]   = true;
                    catScatterTimer[i] = SCATTER_DURATION;
                    score += 100;
                    SoundPlayer.playSound("sounds/eatghost.wav");

                    // compute vector away from *previous* Pac-Man pos
                    int ddx = cat.x - prevPX;
                    int ddy = cat.y - prevPY;
                    if (ddx == 0 && ddy == 0) {
                        // fallback: try all 4 directions, pick the one farthest from Pac-Man
                        int bestDist = -1;
                        int[] dxs = {-1,1,0,0}, dys = {0,0,-1,1};
                        for (int j = 0; j < 4; j++) {
                            int tx = cat.x + dxs[j], ty = cat.y + dys[j];
                            if (tx < 0 || tx >= COLS || ty < 0 || ty >= ROWS) continue;
                            if (maze[ty][tx] == 1) continue;
                            int dist = Math.abs(tx - pacmanX) + Math.abs(ty - pacmanY);
                            if (dist > bestDist) {
                                bestDist   = dist;
                                scatterDx[i] = dxs[j];
                                scatterDy[i] = dys[j];
                            }
                        }
                    } else {
                        // straight‐line signum vector
                        if (Math.abs(ddx) > Math.abs(ddy)) {
                            scatterDx[i] = Integer.signum(ddx);
                            scatterDy[i] = 0;
                        } else {
                            scatterDx[i] = 0;
                            scatterDy[i] = Integer.signum(ddy);
                        }
                    }
                }
                else if (!poweredUp && !catScattering[i]) {
                    // unpowered‐up collision → game over
                    gameOver = true;
                    timer.stop();
                    return;
                }
            }
        }
    }




    private void moveGhost() {
        if (gameWon || gameOver) return;

        for (int i = 0; i < catPositions.size(); i++) {
            // skip until this cat is released
            if (catReleaseTimers[i] > 0) {
                catReleaseTimers[i]--;
                continue;
            }

            Point cat = catPositions.get(i);

            // --- 1) scatter phase ---
            if (catScattering[i]) {
                // try to move in the precomputed run-away direction
                int nx = cat.x + scatterDx[i];
                int ny = cat.y + scatterDy[i];
                if (nx >= 0 && nx < COLS && ny >= 0 && ny < ROWS && maze[ny][nx] == 0) {
                    catPositions.set(i, new Point(nx, ny));
                }
                // count down scatter timer
                if (--catScatterTimer[i] <= 0) {
                    catScattering[i] = false;
                }
                continue;
            }

            // --- 2) normal chase AI ---
            int bestDx = 0, bestDy = 0, minDist = Integer.MAX_VALUE;
            int[] dxs = {-1, 1, 0, 0};
            int[] dys = {0, 0, -1, 1};

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

// --- 3) collision check & trigger scatter or game over ---
            if (nextX == pacmanX && nextY == pacmanY) {
                if (poweredUp) {
                    catScattering[i] = true;
                    catScatterTimer[i] = SCATTER_DURATION;
                    score += 100;
                    SoundPlayer.playSound("sounds/eatghost.wav");

                    int dx = cat.x - pacmanX;
                    int dy = cat.y - pacmanY;
                    if (Math.abs(dx) > Math.abs(dy)) {
                        scatterDx[i] = Integer.signum(dx);
                        scatterDy[i] = 0;
                    } else {
                        scatterDx[i] = 0;
                        scatterDy[i] = Integer.signum(dy);
                    }
                    return; // skip move this tick
                } else {
                    gameOver = true;
                    timer.stop();
                    return;
                }
            }

// --- 4) bounce logic if another ghost is in the way ---
            boolean occupied = false;
            for (int j = 0; j < catPositions.size(); j++) {
                if (j != i) {
                    Point otherCat = catPositions.get(j);
                    if (otherCat.x == nextX && otherCat.y == nextY) {
                        occupied = true;
                        break;
                    }
                }
            }

            if (!occupied) {
                catPositions.set(i, new Point(nextX, nextY));
            } else {
                // Try alternate directions in random order
                List<Integer> dirs = List.of(0, 1, 2, 3);
                List<Integer> shuffled = new ArrayList<>(dirs);
                java.util.Collections.shuffle(shuffled);

                for (int dir : shuffled) {
                    int altX = cat.x + dxs[dir];
                    int altY = cat.y + dys[dir];

                    if (altX < 0 || altX >= COLS || altY < 0 || altY >= ROWS) continue;
                    if (maze[altY][altX] == 1) continue;

                    boolean occupiedAlt = false;
                    for (int j = 0; j < catPositions.size(); j++) {
                        if (j != i && catPositions.get(j).x == altX && catPositions.get(j).y == altY) {
                            occupiedAlt = true;
                            break;
                        }
                    }

                    if (!occupiedAlt) {
                        catPositions.set(i, new Point(altX, altY));
                        break;
                    }
                }
            }
        }
    }
    public class SoundPlayer {
        public static void playSound(String soundFileName) {
            try {
                File soundFile = new File(soundFileName);
                if (soundFile.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInput);
                    clip.start();
                } else {
                    System.out.println("Sound file not found: " + soundFileName);
                }
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
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

    private void resetGame() {
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

        MazeGeneration.generateRandomMaze();
        maze = MazeGeneration.getMaze();
        dots = MazeGeneration.getDots();
        powerPellets = MazeGeneration.getPowerPellets();

        catPositions.clear();
        catPositions.add(new Point(10, 10));
        catPositions.add(new Point(10, 10));
        catPositions.add(new Point(10, 10));

        catReleaseTimers = new int[]{0, 20, 40};

        int n = catPositions.size();
        catScattering   = new boolean[n];
        catScatterTimer = new int   [n];
        scatterDx       = new int   [n];
        scatterDy       = new int   [n];


        timer.start();
    }
}
