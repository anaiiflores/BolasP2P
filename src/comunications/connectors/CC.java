package comunications.connectors;

import comunications.Controller2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

/**
 * CC = ClientConnector
 * - Intenta conectar ACTIVAMENTE al peer remoto SOLO si no hay canal válido.
 * - Backoff para no reventar al server con reconexiones.
 */
public class CC implements Runnable {

    private final Controller2 comController;
    private final int mainPort, auxPort;
    private final String HOST;

    // tiempos
    private static final int CONNECT_TIMEOUT_MS = 1500;
    private static final long BASE_WAIT_MS = 400;
    private static final long MAX_WAIT_MS  = 8000;

    public CC(Controller2 comController, int mainPort, int auxPort, String ip) {
        this.comController = comController;
        this.mainPort = mainPort;
        this.auxPort = auxPort;
        this.HOST = ip;
    }

    @Override
    public void run() {

        long wait = BASE_WAIT_MS;

        while (true) {

            // Si ya hay canal válido, no hago nada
            if (comController.isValid()) {
                wait = BASE_WAIT_MS;
                sleep(300);
                continue;
            }

            Socket socket = null;

            try {
                // 1) intento mainPort
                socket = tryConnect(HOST, mainPort);
                if (socket != null) {
                    System.out.println("[ClientConnector] Conectado a " + HOST + ":" + mainPort);
                } else {
                    // 2) intento auxPort
                    socket = tryConnect(HOST, auxPort);
                    if (socket != null) {
                        System.out.println("[ClientConnector] Conectado a " + HOST + ":" + auxPort);
                    }
                }

                if (socket != null && socket.isConnected()) {
                    // IMPORTANTE: antes de setSocket, vuelvo a comprobar por si SC ya conectó
                    if (!comController.isValid()) {
                        comController.setSocket(socket);
                        // si setSocket fallara internamente, él mismo llamará onChannelDown()
                    } else {
                        try { socket.close(); } catch (IOException ignored) {}
                    }

                    // tras un intento (haya ido bien o no), espero un poco para estabilizar
                    wait = BASE_WAIT_MS;
                    sleep(400);
                } else {
                    // no conectó a ninguno
                    System.out.println("[ClientConnector] No conecta. Reintento en " + wait + "ms...");
                    sleep(wait);
                    wait = Math.min(wait * 2, MAX_WAIT_MS);
                }

            } catch (Exception e) {
                // por si acaso
                if (socket != null) try { socket.close(); } catch (IOException ignored) {}
                System.out.println("[ClientConnector] Error: " + e.getMessage() + " | Reintento en " + wait + "ms...");
                sleep(wait);
                wait = Math.min(wait * 2, MAX_WAIT_MS);
            }
        }
    }

    private Socket tryConnect(String host, int port) {
        // localhost/127.0.0.1 no necesita lógica rara: elige port directamente
        String target = (Objects.equals(host, "localhost") ? "127.0.0.1" : host);

        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(target, port), CONNECT_TIMEOUT_MS);
            // Pequeña mejora: reduce delays
            s.setTcpNoDelay(true);
            return s;
        } catch (IOException e) {
            try { s.close(); } catch (IOException ignored) {}
            return null;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}