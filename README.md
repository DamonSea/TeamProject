# Rat-Man

A Pac-Man-style maze game in Java Swing. You're a rat collecting cheese while cats chase you.
Grab the big cheese to power up and eat the cats. Every 4th level is a boss level.

## Run it

Requires **Java 17+** and **Maven**.

```bash
mvn compile exec:java
```

Or open the folder in IntelliJ as a Maven project, run `mvn compile` once (this unpacks the
controller libraries into `target/natives`), then run `ratman.RatMan`.

To build a jar: `mvn package` → `target/rat-man-1.1.0.jar`.

## Controls

| Action | Keyboard | Xbox controller |
|---|---|---|
| Move | Arrow keys or WASD | D-pad or left stick |
| Next level / restart | R or Enter | Start (Button 6) |
| Enter initials | Type 3 letters, Enter to save, Esc to skip | Start skips |

Debug mode (999 lives; press `0` to clear a level):
`mvn compile exec:java -Dratman.debug=true`

## Levels

- Levels cycle through three themes: **Outdoor → Classroom → Planets**
- Levels 1–3 have 1–3 cats; level 4 is the **boss** (fast and can't be eaten); then the cycle repeats
- Mazes are randomly generated every level
- Skins: Rat, Squirrel, Joe Oakes

## Project layout

```
src/main/java/ratman/
  RatMan.java          entry point, window + screen switching
  GamePanel.java       game state, game loop, input
  Renderer.java        all drawing
  CatAI.java           cat chase, eaten-cat "eyes" pathfinding, boss
  Cat.java             one cat's state
  Maze.java            random maze generation
  Theme.java, Skin.java
  Leaderboard.java     high scores in leaderboard.txt
  SoundPlayer.java, Assets.java, ControllerInput.java
  Menu.java, SkinsMenu.java
  tools/ControllerTester.java   prints raw gamepad events (for debugging controllers)
src/main/resources/    images, sounds, fonts, maze themes
```

High scores are saved to `~/.ratman/leaderboard.txt` (your home folder), so playing doesn't create Git changes.
The first run copies in the team's original scores.
