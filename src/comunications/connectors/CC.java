package comunications.connectors;

import comunications.Controller2;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

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
        while (true) {
            if (!comController.isValid()) {
                Socket socket = null;

                if (Objects.equals(HOST, "localhost") || Objects.equals(HOST, "127.0.0.1")) {
                    int port = comController.getAvailablePort();
                    try {
                        socket = new Socket("127.0.0.1", port);
                        System.out.println("[ClientConnector] Conectado a localhost:" + port);
                    } catch (IOException e) {
                        System.out.println("[ClientConnector] Error localhost: " + e.getMessage());
                        sleep(500);
                        continue;
                    }

                } else {
                    try {
                        socket = new Socket(HOST, mainPort);
                        System.out.println("[ClientConnector] Conectado a " + HOST + ":" + mainPort);
                    } catch (IOException e1) {
                        try {
                            socket = new Socket(HOST, auxPort);
                            System.out.println("[ClientConnector] Conectado a " + HOST + ":" + auxPort);
                        } catch (IOException e2) {
                            System.out.println("[ClientConnector] No conecta. Reintento...");
                            sleep(1000);
                            continue;
                        }
                    }
                }

                if (socket != null && socket.isConnected()) {
                    comController.setSocket(socket);
                }

            } else {
                sleep(1000);
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}