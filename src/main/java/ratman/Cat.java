package ratman;

/** One cat (ghost). When eaten it turns into "eyes" that find their way back to the cage. */
final class Cat {
    /** Each cat hunts differently, like the ghosts in Pac-Man. */
    enum Personality {
        /** Orange eyes: follows the shortest path straight to the player. */
        CHASER,
        /** Yellow eyes: aims a few tiles ahead of where the player is heading, to cut them off. */
        AMBUSHER,
        /** Green eyes: aims opposite the Chaser around the player, to box them in. */
        FLANKER
    }

    final int index;      // 0..2, also selects the sprite colour
    final Personality personality;
    final int homeX;
    final int homeY;
    final int startDelay; // ticks before leaving the cage at the start of a life

    int x;
    int y;
    int releaseTimer;
    int dirX, dirY;       // last move, so the cat doesn't turn straight back
    boolean eyes;
    boolean frightened;   // set by a power pellet; only frightened cats can be eaten
    int respawnTimer;

    /** Fractional movement: speed (tiles per tick) is added each tick; a step happens per whole tile. */
    double moveCredit;

    // For smooth drawing: where the cat came from, when the step started, and how many ticks it lasts
    int prevX, prevY;
    long moveTick;
    double moveTicks = 1;

    Cat(int index, Personality personality, int homeX, int homeY, int startDelay) {
        this.index = index;
        this.personality = personality;
        this.homeX = homeX;
        this.homeY = homeY;
        this.startDelay = startDelay;
        reset();
    }

    void reset() {
        x = homeX;
        y = homeY;
        snap();
        releaseTimer = startDelay;
        dirX = dirY = 0;
        eyes = false;
        frightened = false;
        respawnTimer = 0;
        moveCredit = 0;
    }

    /** Teleport: no sliding animation from the old position. */
    void snap() {
        prevX = x;
        prevY = y;
    }

    boolean isAt(int px, int py) {
        return x == px && y == py;
    }
}
