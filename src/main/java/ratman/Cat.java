package ratman;

/** One cat (ghost). When eaten it turns into "eyes" that find their way back to the cage. */
final class Cat {
    final int index;      // 0..2, also selects the sprite colour
    final int homeX;
    final int homeY;
    final int startDelay; // cat-move steps before leaving the cage

    int x;
    int y;
    int releaseTimer;
    int dirX, dirY;       // last move, so the cat doesn't turn straight back
    boolean eyes;
    boolean frightened;   // set by a power pellet; only frightened cats can be eaten
    int respawnTimer;

    Cat(int index, int homeX, int homeY, int startDelay) {
        this.index = index;
        this.homeX = homeX;
        this.homeY = homeY;
        this.startDelay = startDelay;
        reset();
    }

    void reset() {
        x = homeX;
        y = homeY;
        releaseTimer = startDelay;
        dirX = dirY = 0;
        eyes = false;
        frightened = false;
        respawnTimer = 0;
    }

    boolean isAt(int px, int py) {
        return x == px && y == py;
    }
}
