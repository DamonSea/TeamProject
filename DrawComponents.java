import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DrawComponents
{
    static double[][] previousMap = new double[30][30];
    static BufferedImage mazeBlockSheet;
    static String theme = "outdoor";

    public static void loadSprites() throws IOException {
        mazeBlockSheet = ImageIO.read(new File("MazeGeneration/themes/Outdoor.png"));
    }

    public static void drawMaze(Graphics g, int[][] maze, int TILE_SIZE, int ROWS, int COLS, boolean usingPreviousMap) throws IOException {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (maze[row][col] == 1) {
                    if (theme.equals("standard"))
                    {
                        g.setColor(Color.BLUE);
                        g.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                    else if (theme.equals("outdoor"))
                    {
                        double randomValue;
                        if (usingPreviousMap)
                        {
                            randomValue = previousMap[col][row];
                        }
                        else
                        {
                            randomValue = Math.random();
                            previousMap[col][row] = randomValue;
                        }
                        if (randomValue <= 0.82)
                        {
                            g.drawImage(mazeBlockSheet.getSubimage(0, 0, TILE_SIZE, TILE_SIZE), col * TILE_SIZE, row * TILE_SIZE, null);
                        }
                        else if (randomValue <= 0.88)
                        {
                            g.drawImage(mazeBlockSheet.getSubimage(TILE_SIZE, 0, TILE_SIZE, TILE_SIZE), col * TILE_SIZE, row * TILE_SIZE, null);
                        }
                        else if (randomValue <= 0.94)
                        {
                            g.drawImage(mazeBlockSheet.getSubimage(TILE_SIZE*2, 0, TILE_SIZE, TILE_SIZE), col * TILE_SIZE, row * TILE_SIZE, null);
                        }
                        else
                        {
                            g.drawImage(mazeBlockSheet.getSubimage(TILE_SIZE*3, 0, TILE_SIZE, TILE_SIZE), col * TILE_SIZE, row * TILE_SIZE, null);
                        }
                    }
                }
            }
        }
    }

    public static void drawDots(Graphics g, boolean[][] dots, int TILE_SIZE, int ROWS, int COLS) {
        g.setColor(Color.WHITE);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (dots[row][col]) {
                    g.fillOval(col * TILE_SIZE + TILE_SIZE / 2 - 3, row * TILE_SIZE + TILE_SIZE / 2 - 3, 6, 6);
                }
            }
        }
    }

    public static void drawPowerPellets(Graphics g, boolean[][] powerPellets, int TILE_SIZE, int ROWS, int COLS) {
        g.setColor(Color.PINK);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (powerPellets[row][col]) {
                    g.fillOval(col * TILE_SIZE + TILE_SIZE / 2 - 6, row * TILE_SIZE + TILE_SIZE / 2 - 6, 12, 12);
                }
            }
        }
    }

    public static void drawPacman(Graphics g, Image ratSprite, JPanel panel, int frame, int frameCount, int TILE_SIZE, int pacmanX, int pacmanY, boolean poweredUp, boolean mouthOpen) {
        if (ratSprite != null) {
            int frameWidth = ratSprite.getWidth(panel) / frameCount;
            int frameHeight = ratSprite.getHeight(panel);
            g.drawImage(ratSprite,
                    pacmanX * TILE_SIZE, pacmanY * TILE_SIZE,
                    pacmanX * TILE_SIZE + TILE_SIZE, pacmanY * TILE_SIZE + TILE_SIZE,
                    frame * frameWidth, 0,
                    (frame + 1) * frameWidth, frameHeight,
                    panel);
        } else {
            g.setColor(poweredUp ? Color.CYAN : Color.YELLOW);
            if (mouthOpen) {
                g.fillArc(pacmanX * TILE_SIZE, pacmanY * TILE_SIZE, TILE_SIZE, TILE_SIZE, 30, 300);
            } else {
                g.fillOval(pacmanX * TILE_SIZE, pacmanY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    public static void drawBoss(Graphics g, int x, int y)
    {

    }


    public static void drawScore(Graphics g, int score) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 590);
    }

    public static void drawWinMessage(Graphics g) {
        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("You Win!", 200, 250);
    }

    public static void drawAdvanceLevelMessage(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Press 'R' to Advance", 180, 300);
    }

    public static void drawGameOverMessage(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Game Over", 180, 250);
    }

    public static void drawRestartMessage(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Press 'R' to Restart", 180, 300);
    }

    // method for draw level for current level as level progress
    public static void drawLevel(Graphics g, int level) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Level: " + level, 500, 30);
    }
}
