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
    private int frame = 0;
    private final int frameCount = 4; // Number of frames for rat animation
    private int catFrame = 0;
    private final int CAT_FRAME_COUNT = 2; // Two frames per cat

    // Cat movement and animation
    private List<Point> catPositions = new ArrayList<>();
    private int[] catReleaseTimers = {0, 20, 40};
    private int catMoveCounter = 0;
    private final int CAT_MOVE_DELAY = 5; // Move every 5 ticks

    public GamePanel() throws IOException {
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

        DrawComponents.loadSprites();

        // Play intro sound
        SoundPlayer.playSound("sounds/intro.wav");

        // Load sprites
        ratSprite = new ImageIcon("Rat.png").getImage();
        catSpriteSheet = new ImageIcon("cats.png").getImage();

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
        int sourceWidth = 30;
        int sourceHeight = 30;

        // Calculate the correct frame index (0 to 5)
        int frameIndex = catIndex * CAT_FRAME_COUNT + frame;
        int sx = frameIndex * sourceWidth;
        int sy = 0;

        // Draw the frame directly into the 30x30 tile area
        g.drawImage(catSpriteSheet,
                x * TILE_SIZE, y * TILE_SIZE,
                x * TILE_SIZE + TILE_SIZE, y * TILE_SIZE + TILE_SIZE,
                sx, sy, sx + sourceWidth, sy + sourceHeight,
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

        int newX = pacmanX + dx;
        int newY = pacmanY + dy;

        if (newX >= 0 && newX < COLS && newY >= 0 && newY < ROWS && maze[newY][newX] == 0) {
            pacmanX = newX;
            pacmanY = newY;

            if (dots[pacmanY][pacmanX]) {
                dots[pacmanY][pacmanX] = false;
                score += 10;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastWakaTime >= 1000) {
                    SoundPlayer.playSound("sounds/waka.wav");
                    lastWakaTime = currentTime;
                }
                if (checkWin()) {
                    gameWon = true;
                    timer.stop();
                }
            }

            if (powerPellets[pacmanY][pacmanX]) {
                powerPellets[pacmanY][pacmanX] = false;
                poweredUp = true;
                powerTimer = 50;
                SoundPlayer.playSound("sounds/powerup.wav");
            }
        }
    }

    private void moveGhost() {
        if (gameWon || gameOver) return;

        for (int i = 0; i < catPositions.size(); i++) {
            if (catReleaseTimers[i] > 0) {
                catReleaseTimers[i]--;
                continue;
            }

            Point cat = catPositions.get(i);
            int catX = cat.x;
            int catY = cat.y;

            int bestDx = 0;
            int bestDy = 0;
            int minDistance = Integer.MAX_VALUE;

            int[] dxs = {-1, 1, 0, 0};
            int[] dys = {0, 0, -1, 1};

            for (int j = 0; j < 4; j++) {
                int newX = catX + dxs[j];
                int newY = catY + dys[j];
                if (newX >= 0 && newX < COLS && newY >= 0 && newY < ROWS && maze[newY][newX] == 0) {
                    int distance = Math.abs(newX - pacmanX) + Math.abs(newY - pacmanY);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestDx = dxs[j];
                        bestDy = dys[j];
                    }
                }
            }

            int nextX = catX + bestDx;
            int nextY = catY + bestDy;

            catPositions.set(i, new Point(nextX, nextY));

            if (pacmanX == nextX && pacmanY == nextY) {
                if (poweredUp) {
                    catPositions.set(i, new Point(10, 10));
                    score += 100;
                    SoundPlayer.playSound("sounds/eatghost.wav");
                } else {
                    gameOver = true;
                    timer.stop();
                }
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

        timer.start();
    }
}
