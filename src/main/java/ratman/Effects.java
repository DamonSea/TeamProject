package ratman;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Purely visual effects: pixel-particle explosions and screen shake. Game logic triggers them;
 * the renderer advances them every frame using real time, so they stay smooth at 60 fps.
 */
final class Effects {

    private static final class Particle {
        double x, y, vx, vy, life, maxLife, size, spin;
        Color color;
    }

    private static final double GRAVITY = 420; // px/s²
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private double shakeTime;
    private double shakeTotal;
    private double shakeStrength;

    /**
     * Breaks an image into chunky pixels that fly outward from (cx, cy).
     * {@code displaySize} is how big the image appears on screen; {@code speed} scales the blast.
     */
    void burst(BufferedImage image, double cx, double cy, int displaySize, double speed) {
        int w = image.getWidth(), h = image.getHeight();
        int step = Math.max(1, Math.max(w, h) / 12);       // ~12x12 chunks
        double scale = displaySize / (double) Math.max(w, h);
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) < 128) continue;
                Particle p = new Particle();
                p.x = cx + (x - w / 2.0) * scale;
                p.y = cy + (y - h / 2.0) * scale;
                double angle = Math.atan2(p.y - cy, p.x - cx) + (random.nextDouble() - 0.5) * 0.8;
                double v = (90 + random.nextDouble() * 160) * speed;
                p.vx = Math.cos(angle) * v;
                p.vy = Math.sin(angle) * v - 120 * speed;
                p.maxLife = p.life = 0.6 + random.nextDouble() * 0.6;
                p.size = Math.max(3, step * scale);
                p.spin = (random.nextDouble() - 0.5) * 10;
                p.color = new Color(argb, true);
                particles.add(p);
            }
        }
    }

    /** A ring of coloured sparks, e.g. for a big explosion. */
    void sparks(double cx, double cy, int count, Color... colors) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = cx;
            p.y = cy;
            double angle = random.nextDouble() * Math.PI * 2;
            double v = 120 + random.nextDouble() * 260;
            p.vx = Math.cos(angle) * v;
            p.vy = Math.sin(angle) * v - 80;
            p.maxLife = p.life = 0.5 + random.nextDouble() * 0.9;
            p.size = 3 + random.nextInt(4);
            p.color = colors[random.nextInt(colors.length)];
            particles.add(p);
        }
    }

    void shake(double seconds, double strength) {
        if (strength >= shakeStrength || shakeTime <= 0) {
            shakeTime = shakeTotal = seconds;
            shakeStrength = strength;
        }
    }

    void clear() {
        particles.clear();
        shakeTime = 0;
    }

    boolean isActive() {
        return !particles.isEmpty() || shakeTime > 0;
    }

    void update(double dt) {
        dt = Math.min(dt, 0.05); // don't jump after a hiccup
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            p.vy += GRAVITY * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 1 - 0.8 * dt;
        }
        if (shakeTime > 0) shakeTime -= dt;
    }

    /** Current camera offset in pixels for the screen shake (eases out). */
    int[] shakeOffset() {
        if (shakeTime <= 0) return new int[]{0, 0};
        double k = shakeStrength * (shakeTime / shakeTotal);
        return new int[]{(int) Math.round((random.nextDouble() * 2 - 1) * k),
                (int) Math.round((random.nextDouble() * 2 - 1) * k)};
    }

    void draw(Graphics2D g) {
        for (Particle p : particles) {
            float alpha = (float) Math.min(1, p.life / (p.maxLife * 0.5));
            Color c = p.color;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * alpha)));
            int s = (int) Math.round(p.size);
            g.fillRect((int) Math.round(p.x - s / 2.0), (int) Math.round(p.y - s / 2.0), s, s);
        }
    }

    int particleCount() {
        return particles.size();
    }
}
