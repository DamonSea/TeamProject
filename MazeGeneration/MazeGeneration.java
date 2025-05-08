package MazeGeneration;

import java.util.Random;

public class MazeGeneration
{
    private static final int TILE_SIZE = 30;
    private static final int ROWS = 20;
    private static final int COLS = 20;

    private static final int[][] maze = new int[ROWS][COLS];
    private static final boolean[][] dots = new boolean[ROWS][COLS];
    private static final boolean[][] powerPellets = new boolean[ROWS][COLS];

    public static void generateRandomMaze(int level) {
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
        powerPellets[1][1] = true;
        powerPellets[1][18] = true;
        powerPellets[18][1] = true;
        powerPellets[18][18] = true;
    }

    public static void generateRandomMaze()
    {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++)
            {
                    maze[i][j] = 1;
                    dots[i][j] = false;
                    powerPellets[i][j] = false;
            }
        }

        for (int i = 1; i <= ROWS-2; i++)
        {
            maze[1][i] = 0;
            dots[1][i] = true;
            maze[i][1] = 0;
            dots[i][1] = true;
            maze[ROWS-2][i] = 0;
            dots[ROWS-2][i] = true;
            maze[i][ROWS-2] = 0;
            dots[i][ROWS-2] = true;
        }

        Random random = new Random();
        for (int i = 0; i < 7; i++)
        {
            int randomX = random.nextInt(4, COLS - 7);
            int randomY = random.nextInt(4, ROWS - 7);

            for (int j = randomX; j < randomX+3; j++) {
                for (int k = randomY; k < randomY+3; k++)
                {
                    maze[j][k] = 0;
                    dots[j][k] = true;
                }
            }

            maze[randomX+1][randomY+1] = 1;
            dots[randomX+1][randomY+1] = false;

            if (Math.random() >= 0.5)
            {
                for (int j = 2; j < ROWS-2; j++)
                {
                    maze[randomX][j] = 0;
                    dots[randomX][j] = true;
                }
            }
            else
            {
                for (int j = 2; j < COLS-2; j++)
                {
                    maze[j][randomY] = 0;
                    dots[j][randomY] = true;
                }
            }
        }
        //powerPellets[1][1] = true;
        powerPellets[1][COLS-2] = true;
        powerPellets[ROWS-2][1] = true;
        powerPellets[ROWS-2][COLS-2] = true;

        maze[9][0] = 0;
        maze[10][0] = 0;
        maze[9][19] = 0;
        maze[10][19] = 0;

        for (int i = 6; i < 9; i++)
        {
            maze[10][i] = 0;
            maze[i][10] = 0;
            powerPellets[i][10] = false;
        }
    }

    public static int[][] getMaze()
    {
        return maze;
    }

    public static boolean[][] getDots()
    {
        return dots;
    }

    public static boolean[][] getPowerPellets()
    {
        return powerPellets;
    }

}
