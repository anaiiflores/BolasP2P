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
            // si el channel ya no es válido, paramos
            if (channel == null || !channel.isValid()) break;

            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                break;
            }

            // mando ping
            channel.comprobarConexion();

            // espero un poco a que llegue el pong
            try {
                Thread.sleep(WAIT_PONG);
            } catch (InterruptedException e) {
                break;
            }

            //si no hay respuesta, cierro
            long tiempoSinRespuesta = System.currentTimeMillis() - ultimaRespuesta;
            if (tiempoSinRespuesta > TIMEOUT) {
                System.out.println("[Health]  Sin respuesta " + (tiempoSinRespuesta / 1000) + "s -> cierro conexión");
                channel.close();
                break;
            }
        }

        System.out.println("[Health] Thread terminado");
    }

    public synchronized void notifyHealthy() {
        this.ultimaRespuesta = System.currentTimeMillis();
    }
}