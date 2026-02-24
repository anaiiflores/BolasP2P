package comunications.channel;

public class HealthChannel implements Runnable {

    private final Channel channel;
    private volatile long ultimaRespuesta;

    private static final long TIMEOUT = 10_000;
    private static final long CHECK_INTERVAL = 3_000;
    private static final long WAIT_PONG = 2_000;

    private volatile boolean running = true;

    public HealthChannel(Channel channel) {
        this.channel = channel;
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {

            if (!channel.isValid()) break;

            sleep(CHECK_INTERVAL);

            if (!running || !channel.isValid()) break;

            channel.comprobarConexion(); // ping

            sleep(WAIT_PONG);

            long sinRespuesta = System.currentTimeMillis() - ultimaRespuesta;
            if (sinRespuesta > TIMEOUT) {
                System.out.println("[Health] Sin pong " + (sinRespuesta / 1000) + "s -> cierro");
                channel.close();
                break;
            }
        }
    }

    public void notifyHealthy() {
        ultimaRespuesta = System.currentTimeMillis();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}