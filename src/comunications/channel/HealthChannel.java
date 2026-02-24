package comunications.channel;

public class HealthChannel implements Runnable {

    private final Channel channel;

    private volatile long ultimaRespuesta;

    private static final long TIMEOUT = 10_000;       // 10s sin pong => cerrar
    private static final long CHECK_INTERVAL = 3_000; // cada 3s
    private static final long WAIT_PONG = 2_000;      // espera 2s la respuesta

    private volatile boolean running = true;

    public HealthChannel(Channel channel) {
        this.channel = channel;
        this.ultimaRespuesta = System.currentTimeMillis();
    }

    /** Permite parar el hilo desde Channel.closeInternal() */
    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {

            // Si el channel ya no es válido, terminamos el health de este socket.
            // OJO: si luego reconectas, Channel creará un HealthChannel nuevo.
            if (!channel.isValid()) break;

            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                break;
            }

            // Si justo en este momento se cayó, no intentes enviar ping
            if (!channel.isValid()) break;

            // mando ping
            channel.comprobarConexion();

            // espero un poco a que llegue el pong
            try {
                Thread.sleep(WAIT_PONG);
            } catch (InterruptedException e) {
                break;
            }

            // si no hay respuesta, cierro (esto forzará a CC a reconectar)
            long tiempoSinRespuesta = System.currentTimeMillis() - ultimaRespuesta;
            if (tiempoSinRespuesta > TIMEOUT) {
                System.out.println("[Health] Sin respuesta " + (tiempoSinRespuesta / 1000) + "s -> cierro conexión");
                channel.close(); // cierra el socket (y ChannelReader saldrá)
                break;
            }
        }

        System.out.println("[Health] Thread terminado");
    }

    /** Lo llama Channel cuando recibe un PONG (header 2) */
    public void notifyHealthy() {
        ultimaRespuesta = System.currentTimeMillis();
    }
}