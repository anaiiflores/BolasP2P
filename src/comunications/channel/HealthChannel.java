package comunications.channel;

public class HealthChannel implements Runnable {

    private final Channel channel;

    // Última vez que recibí CUALQUIER cosa (ping/bola/sprite/pong)
    private volatile long lastActivity;

    // Última vez que recibí un PONG específicamente
    private volatile long lastPong;

    private volatile boolean running = true;

    // Ajustes
    private static final long TIMEOUT_MS = 10_000;     // si no hay vida en 10s -> caer
    private static final long CHECK_EVERY_MS = 3_000;  // cada cuánto compruebo
    private static final long WAIT_PONG_MS = 2_000;    // cuánto espero el pong tras ping

    // Como en el ejemplo: primer “aviso” manda ping, si vuelve a pasar -> mata
    private volatile boolean killArmed = false;

    public HealthChannel(Channel channel) {
        this.channel = channel;
        long now = System.currentTimeMillis();
        this.lastActivity = now;
        this.lastPong = now;
    }

    public void stop() {
        running = false;
    }

    /** Llamar cuando llega cualquier mensaje */
    public void notifyHealthy() {
        lastActivity = System.currentTimeMillis();
        // si hay actividad, desarmo la “muerte”
        killArmed = false;
    }

    /** Llamar específicamente cuando llega PONG */
    public void notifyPong() {
        long now = System.currentTimeMillis();
        lastPong = now;
        lastActivity = now;
        killArmed = false;
    }

    @Override
    public void run() {
        // mini-delay para dejar arrancar el reader (igual que el ejemplo)
        sleep(1000);

        while (running) {

            if (!channel.isValid()) break;

            sleep(CHECK_EVERY_MS);
            if (!running || !channel.isValid()) break;

            long now = System.currentTimeMillis();
            long inactiveFor = now - lastActivity;

            // Si ha habido actividad reciente, no hago nada
            if (inactiveFor <= TIMEOUT_MS) {
                continue;
            }

            // Si ya estaba armado y sigue sin actividad -> cierro
            if (killArmed) {
                System.out.println("[Health] ❌ Sin actividad > " + (TIMEOUT_MS/1000) + "s (2º aviso) -> cierro canal");
                channel.close();
                break;
            }

            // 1er aviso: mando ping y armo
            System.out.println("[Health] ⚠️ Inactivo " + inactiveFor + "ms -> mando PING");
            killArmed = true;
            channel.comprobarConexion(); // envía header 1

            // espero pong
            sleep(WAIT_PONG_MS);

            long sincePong = System.currentTimeMillis() - lastPong;
            if (sincePong <= TIMEOUT_MS) {
                // llegó pong / hubo vida -> desarmo
                killArmed = false;
            }
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}