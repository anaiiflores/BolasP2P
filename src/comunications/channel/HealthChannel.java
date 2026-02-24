package comunications.channel;

public class HealthChannel implements Runnable {

    private final Channel channel;
    private volatile long ultimaRespuesta;

    private static final long TIMEOUT = 10_000;       // 10s
    private static final long CHECK_INTERVAL = 3_000; // cada 3s

    public HealthChannel(Channel channel) {
        this.channel = channel;
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            // si no hay conexión válida, paramos (el Channel se encargará de reconectar con CC/SC)
            if (!channel.isValid()) break;

            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                break;
            }

            // mando ping
            channel.comprobarConexion();

            // compruebo timeout (sin sleep extra fijo)
            long tiempoSinRespuesta = System.currentTimeMillis() - ultimaRespuesta;
            if (tiempoSinRespuesta > TIMEOUT) {
                System.out.println("[Health] Sin respuesta " + (tiempoSinRespuesta / 1000) + "s -> cierro conexión");
                channel.close();
                break;
            }
        }

        System.out.println("[Health] Thread terminado");
    }

    /** Se llama cuando llega el MsgDTO header=2 (pong). */
    public synchronized void notifyHealthy() {
        this.ultimaRespuesta = System.currentTimeMillis();
    }
}