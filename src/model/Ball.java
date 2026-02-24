package model;

import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicLong;

/**
 * model.Ball (modelo):
 * - Guarda estado de una bola (posición, velocidad, radio).
 * - Se actualiza con dt (delta time) en segundos.
 * - Rebota en límites superior/inferior.
 * - Cuando sale por la derecha, el controller la "teletransporta" (la manda al otro peer).
 */
public class Ball {

    private static final AtomicLong IDS = new AtomicLong(1);

    private final long id;
    private float x, y;
    private float vx, vy;
    private float r;

    public Ball(float x, float y, float r, float vx, float vy) {
        this.id = IDS.getAndIncrement();
        this.x = x;
        this.y = y;
        this.r = r;
        this.vx = vx;
        this.vy = vy;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getR() { return r; }

    public float getVx() { return vx; }
    public float getVy() { return vy; }


    /**
     * Avanza la física.
     * @param dtSeconds tiempo transcurrido en segundos
     * @param bounds área jugable (panel)
     */
    public void update(float dtSeconds, Rectangle2D.Float bounds) {
        x += vx * dtSeconds;
        y += vy * dtSeconds;

        // Rebote arriba/abajo
        float top = bounds.y;
        float bottom = bounds.y + bounds.height;

        if (y - r < top) {
            y = top + r;
            vy = -vy * 0.98f;
        } else if (y + r > bottom) {
            y = bottom - r;
            vy = -vy * 0.98f;
        }
    }

    /** True si la bola ya salió totalmente por la derecha del área */
    public boolean isOutRight(Rectangle2D.Float bounds) {
        return (x - r) > (bounds.x + bounds.width);
    }


}