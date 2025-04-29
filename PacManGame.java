import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import MazeGeneration.MazeGeneration;

public class PacManGame {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple Pacman");
        GamePanel panel = new GamePanel();

        frame.add(panel);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener {

    private final int TILE_SIZE = 30;
    private final int ROWS = 20;
    private final int COLS = 20;
    private Timer timer;

    private int pacmanX = 1;
    private int pacmanY = 1;
    private int dx = 0;
    private int dy = 0;

    private int ghostX = 18;
    private int ghostY = 18;

    private boolean mouthOpen = true;

    private int[][] maze = new int[ROWS][COLS];
    private boolean[][] dots = new boolean[ROWS][COLS];
    private boolean[][] powerPellets = new boolean[ROWS][COLS];

    private boolean poweredUp = false;
    private int powerTimer = 0;

    private int score = 0;
    private boolean gameWon = false;
    private boolean gameOver = false;

    private long lastWakaTime = 0;

    private final Random random = new Random();

    private Image ratSprite;
    private int frame = 0;
    private final int frameCount = 4; // Change this if you have more/less frames

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        //MazeGeneration.initMaze();
        MazeGeneration.generateRandomMaze();

        maze = MazeGeneration.getMaze();
        dots = MazeGeneration.getDots();
        powerPellets = MazeGeneration.getPowerPellets();


        SoundPlayer.playSound("sounds/intro.wav");

        ratSprite = new ImageIcon("Rat.png").getImage();

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

        timer = new Timer(100, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        DrawComponents.drawMaze(g, maze, TILE_SIZE, ROWS, COLS);
        DrawComponents.drawDots(g, dots, TILE_SIZE, ROWS, COLS);
        DrawComponents.drawPowerPellets(g, powerPellets, TILE_SIZE, ROWS, COLS);
        DrawComponents.drawPacman(g, ratSprite, this, frame, frameCount, TILE_SIZE, pacmanX, pacmanY, poweredUp, mouthOpen);
        DrawComponents.drawGhost(g, ghostX, ghostY, TILE_SIZE);
        DrawComponents.drawScore(g, score);

        if (gameWon) {
            DrawComponents.drawWinMessage(g);
            DrawComponents.drawRestartMessage(g);
        }
        if (gameOver) {
            DrawComponents.drawGameOverMessage(g);
            DrawComponents.drawRestartMessage(g);
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        movePacman();
        moveGhost();
        mouthOpen = !mouthOpen;
        frame = (frame + 1) % frameCount;
        if (poweredUp) {
            powerTimer--;
            if (powerTimer <= 0) {
                poweredUp = false;
            }
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

            if (pacmanX == ghostX && pacmanY == ghostY) {
                if (poweredUp) {
                    ghostX = 18;
                    ghostY = 18;
                    score += 100;
                    SoundPlayer.playSound("sounds/eatghost.wav");
                } else {
                    gameOver = true;
                    timer.stop();
                }
            }
        }
    }

    private void moveGhost() {
        if (gameWon || gameOver) return;

        int bestDx = 0;
        int bestDy = 0;
        int minDistance = Integer.MAX_VALUE;

        int[] dxs = {-1, 1, 0, 0};
        int[] dys = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int newGhostX = ghostX + dxs[i];
            int newGhostY = ghostY + dys[i];
            if (newGhostX >= 0 && newGhostX < COLS && newGhostY >= 0 && newGhostY < ROWS && maze[newGhostY][newGhostX] == 0) {
                int distance = Math.abs(newGhostX - pacmanX) + Math.abs(newGhostY - pacmanY);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestDx = dxs[i];
                    bestDy = dys[i];
                }
            }
        }

        ghostX += bestDx;
        ghostY += bestDy;

        if (pacmanX == ghostX && pacmanY == ghostY) {
            if (poweredUp) {
                ghostX = 18;
                ghostY = 18;
                score += 100;
            } else {
                gameOver = true;
                timer.stop();
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
        ghostX = 18;
        ghostY = 18;
        dx = 0;
        dy = 0;
        score = 0;
        gameWon = false;
        gameOver = false;
        mouthOpen = true;
        poweredUp = false;
        powerTimer = 0;
        frame = 0;
        //MazeGeneration.initMaze();
        MazeGeneration.generateRandomMaze();
        maze = MazeGeneration.getMaze();
        dots = MazeGeneration.getDots();
        powerPellets = MazeGeneration.getPowerPellets();
        timer.start();
    }
}
