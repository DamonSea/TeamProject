

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

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

    private final int[][] maze = new int[ROWS][COLS];
    private final boolean[][] dots = new boolean[ROWS][COLS];
    private final boolean[][] powerPellets = new boolean[ROWS][COLS];

    private boolean poweredUp = false;
    private int powerTimer = 0;

    private int score = 0;
    private boolean gameWon = false;
    private boolean gameOver = false;

    private final Random random = new Random();

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        initMaze();

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

    private void initMaze() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (i == 0 || j == 0 || i == ROWS - 1 || j == COLS - 1 || (i % 2 == 0 && j % 2 == 0)) {
                    maze[i][j] = 1;
                    dots[i][j] = false;
                    powerPellets[i][j] = false;
                } else {
                    maze[i][j] = 0;
                    dots[i][j] = true;
                    powerPellets[i][j] = false;
                }
            }
        }
        dots[pacmanY][pacmanX] = false;
        powerPellets[1][1] = true;
        powerPellets[1][18] = true;
        powerPellets[18][1] = true;
        powerPellets[18][18] = true;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMaze(g);
        drawDots(g);
        drawPowerPellets(g);
        drawPacman(g);
        drawGhost(g);
        drawScore(g);
        if (gameWon) {
            drawWinMessage(g);
            drawRestartMessage(g);
        }
        if (gameOver) {
            drawGameOverMessage(g);
            drawRestartMessage(g);
        }
    }

    private void drawMaze(Graphics g) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (maze[row][col] == 1) {
                    g.setColor(Color.BLUE);
                    g.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void drawDots(Graphics g) {
        g.setColor(Color.WHITE);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (dots[row][col]) {
                    g.fillOval(col * TILE_SIZE + TILE_SIZE / 2 - 3, row * TILE_SIZE + TILE_SIZE / 2 - 3, 6, 6);
                }
            }
        }
    }

    private void drawPowerPellets(Graphics g) {
        g.setColor(Color.PINK);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (powerPellets[row][col]) {
                    g.fillOval(col * TILE_SIZE + TILE_SIZE / 2 - 6, row * TILE_SIZE + TILE_SIZE / 2 - 6, 12, 12);
                }
            }
        }
    }

    private void drawPacman(Graphics g) {
        g.setColor(poweredUp ? Color.CYAN : Color.YELLOW);
        if (mouthOpen) {
            g.fillArc(pacmanX * TILE_SIZE, pacmanY * TILE_SIZE, TILE_SIZE, TILE_SIZE, 30, 300);
        } else {
            g.fillOval(pacmanX * TILE_SIZE, pacmanY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawGhost(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(ghostX * TILE_SIZE, ghostY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 590);
    }

    private void drawWinMessage(Graphics g) {
        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("You Win!", 200, 250);
    }

    private void drawGameOverMessage(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Game Over", 180, 250);
    }

    private void drawRestartMessage(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Press 'R' to Restart", 180, 300);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        movePacman();
        moveGhost();
        mouthOpen = !mouthOpen;
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

                if (checkWin()) {
                    gameWon = true;
                    timer.stop();
                }
            }

            if (powerPellets[pacmanY][pacmanX]) {
                powerPellets[pacmanY][pacmanX] = false;
                poweredUp = true;
                powerTimer = 50;
            }

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
        initMaze();
        timer.start();
    }
}
