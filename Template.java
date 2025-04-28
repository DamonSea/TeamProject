

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Template {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple Pacman");
        GamePanell panel = new GamePanell();

        frame.add(panel);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class GamePanell extends JPanel implements ActionListener {

    private final int TILE_SIZE = 30;
    private final int ROWS = 20;
    private final int COLS = 20;
    private Timer timer;

    private int pacmanX = 1;
    private int pacmanY = 1;
    private int dx = 0;
    private int dy = 0;

    private final int[][] maze = new int[ROWS][COLS];
    private final boolean[][] dots = new boolean[ROWS][COLS];
    private int score = 0;
    private boolean gameWon = false;

    public GamePanell() {
        setBackground(Color.BLACK);
        setFocusable(true);
        initMaze();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (gameWon && key == KeyEvent.VK_R) {
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
                } else {
                    maze[i][j] = 0;
                    dots[i][j] = true;
                }
            }
        }
        dots[pacmanY][pacmanX] = false;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMaze(g);
        drawDots(g);
        drawPacman(g);
        drawScore(g);
        if (gameWon) {
            drawWinMessage(g);
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

    private void drawPacman(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillOval(pacmanX * TILE_SIZE, pacmanY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
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

    private void drawRestartMessage(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Press 'R' to Restart", 180, 300);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        movePacman();
        repaint();
    }

    private void movePacman() {
        if (gameWon) return;

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
        }
    }

    private boolean checkWin() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (dots[row][col]) {
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
        initMaze();
        timer.start();
    }
}
