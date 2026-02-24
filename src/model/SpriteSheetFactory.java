package model;

import java.awt.image.BufferedImage;

/**
 * Controla una animación basada en un spritesheet.
 * - Divide el spritesheet en frames.
 * - Avanza frames según una cadencia en milisegundos.
 */
public class SpriteSheetFactory {

    private final int frameWidth;
    private final int frameHeight;
    private final int totalFrames;
    private final int cadenceMs;

    private BufferedImage spriteSheet;
    private BufferedImage[] frames;
    private int currentFrame = 0;
    private long lastFrameTime = 0;
    private boolean active = false;

    // ✅ Nuevo: si loop=true, la animación nunca se apaga al llegar al final
    private boolean loop = true;

    public SpriteSheetFactory(BufferedImage sheet, int frameWidth, int frameHeight, int totalFrames, int cadenceMs) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.totalFrames = totalFrames;
        this.cadenceMs = cadenceMs;

        loadSpriteSheet(sheet);
        splitSpriteSheet();
    }

    private void loadSpriteSheet(BufferedImage sheet) {
        this.spriteSheet = sheet;
    }

    /**
     * Divide la imagen grande en imágenes pequeñas (frames).
     * Asume que el spritesheet es una cuadrícula (filas y columnas).
     */
    private void splitSpriteSheet() {
        frames = new BufferedImage[totalFrames];

        int cols = spriteSheet.getWidth() / frameWidth;
        int rows = spriteSheet.getHeight() / frameHeight;
        int index = 0;

        for (int r = 0; r < rows && index < totalFrames; r++) {
            for (int c = 0; c < cols && index < totalFrames; c++) {
                frames[index++] = spriteSheet.getSubimage(
                        c * frameWidth,
                        r * frameHeight,
                        frameWidth,
                        frameHeight
                );
            }
        }
    }

    /**
     * Avanza al siguiente frame si ya pasó la cadencia.
     */
    public void update() {
        if (!active || frames == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= cadenceMs) {
            currentFrame = (currentFrame + 1) % frames.length;
            lastFrameTime = currentTime;

            // ✅ Si no está en loop, se apaga al completar una vuelta
            if (!loop && currentFrame == 0) {
                active = false;
            }
        }
    }

    public BufferedImage getCurrentFrame() {
        if (frames == null || frames.length == 0 || currentFrame >= frames.length) return null;
        return frames[currentFrame];
    }

    public void start() {
        active = true;
        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public void reset() {
        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();
        active = true;
    }

    public void stop() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public int getFrameWidth() { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }
    public int getCurrentFrameIndex() { return currentFrame; }

    // ✅ Nuevo: activar/desactivar loop
    public void setLoop(boolean loop) { this.loop = loop; }
    public boolean isLoop() { return loop; }
}