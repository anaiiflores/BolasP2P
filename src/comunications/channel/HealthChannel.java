package comunications.channel;

public class HealthChannel implements Runnable {

    private final Channel channel;
    private volatile long ultimaRespuesta;

    private static final long TIMEOUT = 10_000;       // 10s
    private static final long CHECK_INTERVAL = 3_000; // cada 3s
    private static final long WAIT_PONG = 2_000;      // espera 2s la respuesta

    public HealthChannel(Channel channel) {
        this.channel = channel;
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (true) {

            // ✅ Si no hay canal válido, NO MUERAS: espera a que vuelva conexión
            if (channel == null || !channel.isValid()) {
                sleepSilently(300);
                // reset para que al reconectar no salte timeout inmediato
                ultimaRespuesta = System.currentTimeMillis();
                continue;
            }

            sleepSilently(CHECK_INTERVAL);

            // 1) ping
            channel.comprobarConexion();

            // 2) esperamos pong
            sleepSilently(WAIT_PONG);

            // 3) si no hay respuesta, cerramos pero seguimos vivos
            long tiempoSinRespuesta = System.currentTimeMillis() - ultimaRespuesta;
            if (tiempoSinRespuesta > TIMEOUT) {
                System.out.println("[Health] Sin respuesta " + (tiempoSinRespuesta / 1000) + "s -> cierro conexión y sigo esperando");
                channel.close();

                // 🔥 si tu Channel tiene notifyDown() (como te pasé), úsalo:
                // channel.notifyDown();

                // y seguimos el while: CC/SC reconectan y health continúa
                sleepSilently(500);
            }
        }
    }

    /** Se llama cuando llega el MsgDTO pong */
    public synchronized void notifyHealthy() {
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}