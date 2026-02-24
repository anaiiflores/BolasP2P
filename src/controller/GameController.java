package controller;

import model.Ball;
import model.SpriteSheetFactory;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;
import view.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class GameController {

    private final MainFrame frame;
    private final ControllerMain master;
    private final String label;

    private final Random rnd = new Random();
    private final List<Ball> balls = Collections.synchronizedList(new ArrayList<>());

    private final Timer loopTimer;
    private long lastNanos = System.nanoTime();

    // ✅ bolas tamaño fijo
    private static final float BALL_R = 16f;

    // ===== SpriteSheet (pasa entre peers) =====
    private static final String SPRITE_PATH = "C:\\Users\\anair\\Documents\\BolasP2P\\src\\Resources\\pitufo.png";
    private static final int SPRITE_FRAME_W = 90;
    private static final int SPRITE_FRAME_H = 119;
    private static final int SPRITE_TOTAL_FRAMES = 4;
    private static final int SPRITE_CADENCE_MS = 120;

    private SpriteSheetFactory anim = null;
    private int spriteX = 50;
    private int spriteY = 50;
    private int spriteVx = 4;
    private int spriteVy = 3;

    public GameController(MainFrame frame, String label, ControllerMain master) {
        this.frame = frame;
        this.master = master;
        this.label = label;

        frame.setConnText("Conectando...");
        frame.getGamePanel().setBalls(balls);

        if ("A".equals(label)) {
            crearSpriteLocal(50, 50, 4, 3);

            new Timer(3000, e -> {
                // si no hay sprite activo, crear otro
                if (anim == null) {
                    crearSpriteLocal(0, 50 + rnd.nextInt(120), 4, 3);
                }
            }).start();
        }

        frame.getBtnSpawn().addActionListener(this::onSpawnClicked);

        loopTimer = new Timer(16, e -> tick());
        loopTimer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;

        Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
        if (world.width <= 10 || world.height <= 10) return;

        // ======================
        // 1) Update bolas
        // ======================
        synchronized (balls) {
            for (int i = balls.size() - 1; i >= 0; i--) {
                Ball b = balls.get(i);
                b.update(dt, world);

                if (b.isOutRight(world)) {
                    BolaDTO dto = new BolaDTO(
                            b.getY(),
                            b.getR(),
                            Math.max(90f, Math.abs(b.getVx())),
                            b.getVy()
                    );
                    master.lanzarBola(dto);
                    balls.remove(i);
                }
            }
        }

        // ======================
        // 2) Update sprite y enviarlo
        // ======================
        if (anim != null) {
            anim.update();
            moveSprite(world);

            // ✅ mandar cuando ya salió por la derecha (entrada suave en el otro)
            if (spriteX >= world.width) {
                SpriteDTO dto = new SpriteDTO(spriteY, spriteVx, spriteVy);
                master.lanzarSprite(dto);

                anim = null;
                frame.getGamePanel().setSprite(null, 0, 0);
            } else {
                frame.getGamePanel().setSprite(anim.getCurrentFrame(), spriteX, spriteY);
            }
        }

        frame.getGamePanel().repaint();
    }

    /**
     * Rebota izquierda, arriba y abajo.
     * Derecha = mandar al otro peer.
     */
    private void moveSprite(Rectangle2D.Float world) {
        spriteX += spriteVx;
        spriteY += spriteVy;

        int spriteH = anim.getFrameHeight();
        int panelH = (int) world.height;

        // Rebote izquierda
        if (spriteX <= 0) {
            spriteX = 0;
            spriteVx = -spriteVx;
        }

        // Rebote arriba/abajo
        if (spriteY <= 0) {
            spriteY = 0;
            spriteVy = -spriteVy;
        } else if (spriteY + spriteH >= panelH) {
            spriteY = panelH - spriteH;
            spriteVy = -spriteVy;
        }
    }

    private void crearSpriteLocal(int x, int y, int vx, int vy) {
        BufferedImage sheet = loadSpriteFromDisk(SPRITE_PATH);

        anim = new SpriteSheetFactory(sheet, SPRITE_FRAME_W, SPRITE_FRAME_H, SPRITE_TOTAL_FRAMES, SPRITE_CADENCE_MS);
        anim.setLoop(true);
        anim.start();

        spriteX = x;
        spriteY = y;
        spriteVx = vx;
        spriteVy = vy;

        frame.getGamePanel().setSprite(anim.getCurrentFrame(), spriteX, spriteY);
    }

    private BufferedImage loadSpriteFromDisk(String filePath) {
        try {
            return ImageIO.read(new File(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Error cargando sprite desde disco: " + filePath, e);
        }
    }

    private void onSpawnClicked(ActionEvent e) {
        spawnLocalBall();
    }

    // ✅ bolas tamaño fijo
    public void spawnLocalBall() {
        Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
        if (world.width <= 10 || world.height <= 10) return;

        float r = BALL_R;
        float x = r + 2;
        float y = r + rnd.nextFloat() * (world.height - 2 * r);

        float vx = 160f;
        float vy = -60 + rnd.nextInt(121);

        balls.add(new Ball(x, y, r, vx, vy));
    }

    public void introducirBola(BolaDTO dto) {
        if (dto == null) return;

        SwingUtilities.invokeLater(() -> {
            Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
            if (world.width <= 10 || world.height <= 10) return;

            float r = BALL_R; // ✅ fijo al llegar también
            float x = r + 2;
            float y = clamp(dto.posicionY, r, world.height - r);

            float vx = Math.max(90f, Math.abs(dto.velocidadX));
            float vy = dto.velocidadY;

            balls.add(new Ball(x, y, r, vx, vy));
        });
    }

    public void introducirSprite(SpriteDTO dto) {
        if (dto == null) return;

        SwingUtilities.invokeLater(() -> {
            Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
            if (world.width <= 10 || world.height <= 10) return;

            int x = -SPRITE_FRAME_W; // ✅ entra suave desde fuera
            int y = (int) clamp(dto.posicionY, 0, world.height - SPRITE_FRAME_H);

            int vx = (int) dto.velocidadX;
            int vy = (int) dto.velocidadY;

            if (vx < 0) vx = -vx;
            if (vx == 0) vx = 4;

            crearSpriteLocal(x, y, vx, vy);
        });
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public void shutdown() {
        loopTimer.stop();
    }
}