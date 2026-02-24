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

    // Ventana (UI) de este peer
    private final MainFrame frame;

    // "master" = controlador principal que tiene la comunicación (red)
    private final ControllerMain master;

    // Label "A" o "B" para saber quién es este peer
    private final String label;

    // Random para posiciones/velocidades aleatorias
    private final Random rnd = new Random();


    private final List<Ball> balls = Collections.synchronizedList(new ArrayList<>());


    private final Timer loopTimer;

    // Para calcular deltaTime real (tiempo entre frames) en segundos
    private long lastNanos = System.nanoTime();

    private static final float BALL_R = 16f;

    // Datos del spritesheet (ruta y configuración de frames)
    private static final String SPRITE_PATH = "C:\\Users\\anair\\Documents\\BolasP2P\\src\\Resources\\fuegosheet.jpeg";
    private static final int SPRITE_FRAME_W = 150;
    private static final int SPRITE_FRAME_H = 150;
    private static final int SPRITE_TOTAL_FRAMES = 29;
    private static final int SPRITE_CADENCE_MS = 60;


    private SpriteSheetFactory anim = null;

    // Posición y velocidad del sprite actual (si existe)
    private int spriteX = 50;
    private int spriteY = 50;
    private int spriteVx = 4;
    private int spriteVy = 3;


    public GameController(MainFrame frame, String label, ControllerMain master) {
        this.frame = frame;
        this.master = master;
        this.label = label;

        // El panel dibuja la lista de bolas que controla este GameController
        frame.getGamePanel().setBalls(balls);

        /**
         * Solo A crea sprites automáticamente.
         * B normalmente los recibe desde red y los lanza en pantalla.
         */
        if ("A".equals(label)) {
            crearSpriteLocal(50, 50, 4, 3);

            // Timer cada 5s: si no hay sprite activo, crea uno nuevo
            new Timer(5000, e -> {
                // si no hay sprite activo, crear otro
                if (anim == null) {
                    crearSpriteLocal(0, 50 + rnd.nextInt(120), 4, 3);
                }
            }).start();
        }

        // Botón: crear una bola local
        frame.getBtnSpawn().addActionListener(this::onSpawnClicked);

        // Loop principal: tick cada 16ms (aprox 60fps)
        loopTimer = new Timer(16, e -> tick());
        loopTimer.start();
    }

 //esto hace que se repita
    private void tick() {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1_000_000_000f; // nanosegundos -> segundos
        lastNanos = now;

        // Mundo (área visible) del panel
        Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
        if (world.width <= 10 || world.height <= 10) return;

        // -------------------------
        // 1) UPDATE BOLAS
        // -------------------------
        synchronized (balls) {
            for (int i = balls.size() - 1; i >= 0; i--) {
                Ball b = balls.get(i);

                // Actualiza posición y rebotes (depende de cómo esté Ball.update)
                b.update(dt, world);

                /**
                 * Si la bola sale por la derecha:
                 * - la convertimos a DTO (datos mínimos)
                 * - la mandamos al otro peer con master.lanzarBola(dto)
                 * - la quitamos de esta pantalla
                 */
                if (b.isOutRight(world)) {
                    BolaDTO dto = new BolaDTO(
                            b.getY(),                               // posición vertical
                            b.getR(),                               // radio (por si varía)
                            Math.max(90f, Math.abs(b.getVx())),     // velocidad X positiva (mínimo 90)
                            b.getVy()                               // velocidad Y tal cual
                    );

                    // enviar por red
                    master.lanzarBola(dto);

                    // eliminar localmente (ya "pasó" a la otra pantalla)
                    balls.remove(i);
                }
            }
        }

        // -------------------------
        // 2) UPDATE SPRITE
        // -------------------------
        if (anim != null) {
            // Avanza el frame de la animación
            anim.update();

            // Mueve sprite por la pantalla y rebota en bordes
            moveSprite(world);

            /**
             * Si el sprite sale por la derecha del mundo:
             * - enviamos DTO al otro peer
             * - destruimos el sprite aquí (anim = null)
             * - quitamos el sprite del panel (setSprite null)
             */
            if (spriteX >= world.width) {
                SpriteDTO dto = new SpriteDTO(spriteY, spriteVx, spriteVy);
                master.lanzarSprite(dto);

                anim = null;
                frame.getGamePanel().setSprite(null, 0, 0);

            } else {
                // Si no ha salido, lo dibujamos en el panel con el frame actual
                frame.getGamePanel().setSprite(anim.getCurrentFrame(), spriteX, spriteY);
            }
        }

        // repintar panel
        frame.getGamePanel().repaint();
    }

    /**
     * moveSprite:
     * - actualiza posición con velocidades
     * - rebota en bordes (izquierda, arriba, abajo)
     *
     * Nota: el rebote en derecha NO está porque tú lo usas para "transferir" al otro peer.
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

    /**
     * crearSpriteLocal:
     * - carga imagen del spritesheet desde disco
     * - convierte blanco a transparente (para que se vea bonito)
     * - crea SpriteSheetFactory con dimensiones y frames
     * - arranca animación
     * - pone posición y velocidad
     * - manda el frame actual al panel para dibujar
     */
    private void crearSpriteLocal(int x, int y, int vx, int vy) {
        BufferedImage sheet = loadSpriteFromDisk(SPRITE_PATH);

        // Hace transparente el blanco del spritesheet (si tu método lo hace así)
        sheet = SpriteSheetFactory.makeWhiteTransparent(sheet);

        // Crea la animación por frames
        anim = new SpriteSheetFactory(sheet, SPRITE_FRAME_W, SPRITE_FRAME_H, SPRITE_TOTAL_FRAMES, SPRITE_CADENCE_MS);
        anim.setLoop(true);
        anim.start();

        spriteX = x;
        spriteY = y;
        spriteVx = vx;
        spriteVy = vy;

        // Inicializa el sprite en el panel
        frame.getGamePanel().setSprite(anim.getCurrentFrame(), spriteX, spriteY);
    }

    /**
     * loadSpriteFromDisk:
     * - carga una imagen desde ruta absoluta (tu PC)
     * - si falla, lanza RuntimeException (para no seguir sin sprite)
     */
    private BufferedImage loadSpriteFromDisk(String filePath) {
        try {
            return ImageIO.read(new File(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Error cargando sprite desde disco: " + filePath, e);
        }
    }

    // Listener del botón
    private void onSpawnClicked(ActionEvent e) {
        spawnLocalBall();
    }

    /**
     * spawnLocalBall:
     * - crea bola en el lado izquierdo
     * - con posición Y aleatoria y velocidad fija hacia la derecha
     */
    public void spawnLocalBall() {
        Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
        if (world.width <= 10 || world.height <= 10) return;

        float r = BALL_R;
        float x = r + 2;
        float y = r + rnd.nextFloat() * (world.height - 2 * r);

        float vx = 160f;                 // hacia la derecha
        float vy = -60 + rnd.nextInt(121); // componente vertical random

        balls.add(new Ball(x, y, r, vx, vy));
    }

    /**
     * introducirBola(dto):
     * Esta función la llama ControllerMain cuando llega una bola por red.
     *
     * - Se ejecuta en el hilo de red, por eso usamos invokeLater:
     *   para tocar la UI / lista desde el hilo de Swing.
     * - Crea la bola entrando por la izquierda para que se vea que "entra".
     */
    public void introducirBola(BolaDTO dto) {
        if (dto == null) return;

        SwingUtilities.invokeLater(() -> {
            Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
            if (world.width <= 10 || world.height <= 10) return;

            float r = BALL_R;
            float x = r + 2;
            float y = clamp(dto.posicionY, r, world.height - r);

            float vx = Math.max(90f, Math.abs(dto.velocidadX)); // asegurar hacia la derecha
            float vy = dto.velocidadY;

            balls.add(new Ball(x, y, r, vx, vy));
        });
    }

    /**
     * introducirSprite(dto):
     * La llama ControllerMain cuando llega un sprite por red.
     * - Lo coloca empezando fuera a la izquierda (x = -frameW) para que "entre suave".
     * - Usa velocidades del dto (ajustando para que vx sea positivo).
     * - Crea el sprite local con los datos recibidos.
     */
    public void introducirSprite(SpriteDTO dto) {
        if (dto == null) return;

        SwingUtilities.invokeLater(() -> {
            Rectangle2D.Float world = frame.getGamePanel().getWorldBounds();
            if (world.width <= 10 || world.height <= 10) return;

            int x = -SPRITE_FRAME_W;
            int y = (int) clamp(dto.posicionY, 0, world.height - SPRITE_FRAME_H);

            int vx = (int) dto.velocidadX;
            int vy = (int) dto.velocidadY;

            // asegurar que se mueve a la derecha (para entrar en pantalla)
            if (vx < 0) vx = -vx;
            if (vx == 0) vx = 4;

            crearSpriteLocal(x, y, vx, vy);
        });
    }

    // clamp = limita un valor entre min y max
    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * shutdown:
     * - para el timer del juego (cuando cierras ventana o sales)
     */
    public void shutdown() {
        loopTimer.stop();
    }
}