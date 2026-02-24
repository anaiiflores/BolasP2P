package comunications.channel;

public class HealthChannel implements Runnable {

    private final Channel channel;
    private volatile long ultimaRespuesta;

    private static final long TIMEOUT = 10_000;
    private static final long CHECK_INTERVAL = 3_000;
    private static final long WAIT_PONG = 2_000;

    public HealthChannel(Channel channel) {
        this.channel = channel;
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (true) {

            // ✅ Si no hay conexión, NO muere: espera
            if (channel == null || !channel.isValid()) {
                sleepSilently(300);
                continue;
            }

            sleepSilently(CHECK_INTERVAL);

            channel.comprobarConexion(); // ping
            sleepSilently(WAIT_PONG);

            long sinRespuesta = System.currentTimeMillis() - ultimaRespuesta;
            if (sinRespuesta > TIMEOUT) {
                System.out.println("[Health] Sin respuesta " + (sinRespuesta / 1000) + "s -> cierro y fuerzo reconexión");
                channel.close();
                channel.notifyDown();     // 🔥 clave
                sleepSilently(500);
            }
        }
    }

    public synchronized void notifyHealthy() {
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}