package comunications.channel;

public class HealthChannel implements Runnable {

    private final Channel channel;
    private volatile long ultimaRespuesta;

    private static final long TIMEOUT = 10_000;       // 10s
    private static final long CHECK_INTERVAL = 3_000; // cada 3s
    private static final long WAIT_PONG = 2_000;      // espera 2s

    public HealthChannel(Channel channel) {
        this.channel = channel;
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (true) {
            if (Thread.currentThread().isInterrupted()) break;
            if (channel == null || !channel.isValid()) break;

            sleep(CHECK_INTERVAL);

            if (channel == null || !channel.isValid()) break;
            channel.comprobarConexion();

            sleep(WAIT_PONG);

            long sinRespuesta = System.currentTimeMillis() - ultimaRespuesta;
            if (sinRespuesta > TIMEOUT) {
                System.out.println("[Health] Sin respuesta " + (sinRespuesta / 1000) + "s -> cierro");
                channel.close();
                break;
            }
        }
        System.out.println("[Health] Thread terminado");
    }

    public synchronized void notifyHealthy() {
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}