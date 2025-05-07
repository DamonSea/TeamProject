// GameManager.java
public class GameManager {
    public static final int SCATTER_DURATION = 10;

    private boolean gameWon = false;
    private boolean gameOver = false;
    private boolean playerDied = false;

    public boolean isGameWon() {
        return gameWon;
    }

    public void setGameWon(boolean gameWon) {
        this.gameWon = gameWon;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void triggerDeath() {
        playerDied = true;
        gameOver = true;
    }

    public boolean hasPlayerDied() {
        return playerDied;
    }

    public void reset() {
        gameWon = false;
        gameOver = false;
        playerDied = false;
    }
}
