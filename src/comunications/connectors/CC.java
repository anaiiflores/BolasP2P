package comunications.connectors;

import comunications.Controller2;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 * CC = ClientConnector
 *
 * - Hilo que intenta conectar ACTIVAMENTE al peer remoto.
 * - Solo intenta conectar cuando NO hay canal válido (comController.isValid() == false)
 * - Si conecta, llama comController.setSocket(socket)
 */
public class CC implements Runnable {

    private final Controller2 comController;
    private final int mainPort, auxPort;
    private final String HOST;

    public CC(Controller2 comController, int mainPort, int auxPort, String ip) {
        this.comController = comController;
        this.mainPort = mainPort;
        this.auxPort = auxPort;
        this.HOST = ip;
    }

    @Override
    public void run() {
        Socket socket;

        while (true) {

            // Solo conectar si NO hay conexión válida
            if (!comController.isValid()) {

                socket = null;

                // Modo 1 PC (localhost): elige el puerto "contrario" al que está escuchando el server
                if (Objects.equals(HOST, "localhost") || Objects.equals(HOST, "127.0.0.1")) {

                    int port = comController.getAvailablePort();
                    try {
                        socket = new Socket("127.0.0.1", port);
                        System.out.println("[ClientConnector] Conectado a localhost:" + port);
                    } catch (IOException e) {
                        System.out.println("[ClientConnector] Error conectando a localhost: " + e.getMessage());
                        sleep(5000);
                        continue;
                    }

                } else {
                    // Modo 2 PCs: intenta mainPort y si falla intenta auxPort
                    try {
                        socket = new Socket(HOST, mainPort);
                        System.out.println("[ClientConnector] Conectado a " + HOST + ":" + mainPort);
                    } catch (IOException e1) {
                        try {
                            socket = new Socket(HOST, auxPort);
                            System.out.println("[ClientConnector] Conectado a " + HOST + ":" + auxPort);
                        } catch (IOException e2) {
                            System.out.println("[ClientConnector] No conecta. Reintento en 5s...");
                            sleep(5000);
                            continue;
                        }
                    }
                }

                // Si conectó, pasa el socket al Channel (vía controller)
                if (socket != null && socket.isConnected()) {
                    comController.setSocket(socket);
                }

            } else {
                // Si ya hay conexión, duerme un poco
                sleep(3000);
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}