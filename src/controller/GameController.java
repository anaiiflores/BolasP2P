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

        frame.setModeText(label);
        frame.setConnText("Conectando...");
        frame.getGamePanel().setBalls(balls);

        // ✅ SOLO para que empiece en A (si quieres que empiece en B, cambia esto)
        if ("A".equals(label)) {
            crearSpriteLocal(50, 50, 4, 3);
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
        // 1) Update bolas (igual)
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
        // 2) Update sprite (y pasarlo al otro peer)
        // ======================
        if (anim != null) {
            anim.update();
            moveSprite(world);

            // ✅ Si sale por la derecha => LO MANDO AL OTRO PEER y desaparece aquí
            int spriteW = anim.getFrameWidth();
//modificarlo!!!
// ✅ mandar cuando ya SALIÓ entero por la derecha
            if (spriteX >= world.width) {
                SpriteDTO dto = new SpriteDTO(spriteY, spriteVx, spriteVy);
                master.lanzarSprite(dto);

                anim = null;
                frame.getGamePanel().setSprite(null, 0, 0);
            } else {
                frame.getGamePanel().setSprite(anim.getCurrentFrame(), spriteX, spriteY);
            }
        }

        // ✅ repaint al final
        frame.getGamePanel().repaint();
    }

    /**
     * Mueve el sprite y rebota con paredes ARRIBA/ABAJO e IZQUIERDA,
     * pero NO rebota en DERECHA porque ahí lo enviamos al otro peer.
     */
    private void moveSprite(Rectangle2D.Float world) {
        spriteX += spriteVx;
        spriteY += spriteVy;

        int spriteW = anim.getFrameWidth();
        int spriteH = anim.getFrameHeight();

        int panelW = (int) world.width;
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

        // ✅ No hacemos rebote derecha aquí (porque derecha = mandar)
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

        // pintar ya (por si acaso)
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

    public void spawnLocalBall() {
        Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
        if (world.width <= 10 || world.height <= 10) return;

        float r = 10 + rnd.nextInt(14);
        float x = r + 2;
        float y = r + rnd.nextFloat() * (world.height - 2 * r);

        float vx = 120 + rnd.nextInt(160);
        float vy = -80 + rnd.nextInt(161);

        balls.add(new Ball(x, y, r, vx, vy));
    }

    /** Cuando llega bola desde el otro peer */
    public void introducirBola(BolaDTO dto) {
        if (dto == null) return;

        SwingUtilities.invokeLater(() -> {
            Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
            if (world.width <= 10 || world.height <= 10) return;

            float r = dto.radio;
            float x = r + 2;
            float y = clamp(dto.posicionY, r, world.height - r);

            float vx = Math.max(90f, Math.abs(dto.velocidadX));
            float vy = dto.velocidadY;

            balls.add(new Ball(x, y, r, vx, vy));
        });
    }

    /** ✅ Cuando llega sprite desde el otro peer */
    public void introducirSprite(SpriteDTO dto) {
        if (dto == null) return;

        SwingUtilities.invokeLater(() -> {
            Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
            if (world.width <= 10 || world.height <= 10) return;

            int x = -SPRITE_FRAME_W; // ✅ fuera de pantalla para entrar suave
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
    }}