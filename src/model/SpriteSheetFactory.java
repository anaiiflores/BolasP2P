package model;

import java.awt.image.BufferedImage;

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

//metodo para cortar frames
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


    public void update() {
        if (!active || frames == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= cadenceMs) {
            currentFrame = (currentFrame + 1) % frames.length;
            lastFrameTime = currentTime;

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

    public void setLoop(boolean loop) { this.loop = loop; }
    public boolean isLoop() { return loop; }

//nuevo metodo para hacerlo transparente


    public static BufferedImage makeWhiteTransparent(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        BufferedImage transparent =
                new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = img.getRGB(x, y);

                int r = (rgba >> 16) & 0xff;
                int g = (rgba >> 8) & 0xff;
                int b = rgba & 0xff;

                // Si es casi blanco → transparente
                if (r > 240 && g > 240 && b > 240) {
                    transparent.setRGB(x, y, 0x00000000);
                } else {
                    transparent.setRGB(x, y, rgba);
                }
            }
        }
        return transparent;
    }

}