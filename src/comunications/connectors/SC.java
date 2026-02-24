package comunications.connectors;

import comunications.Controller2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SC implements Runnable {

    private final Controller2 comController;
    private ServerSocket serverSocket;

    private final int mainPort, auxPort;
    private int actualPort;

    public SC(Controller2 comController, int mainPort, int auxPort) {
        this.comController = comController;
        this.serverSocket = null;
        this.mainPort = mainPort;
        this.auxPort = auxPort;
    }

    @Override
    public void run() {
        while (true) {

            if (serverSocket == null) {
                conectarPuerto();
            }

            try {
                Socket socket = serverSocket.accept();
                System.out.println("[ServerConnector] Conexión entrante: " + socket.getInetAddress());

                // Si ya hay canal, cierro el socket entrante
                if (comController.isValid()) {
                    try { socket.close(); } catch (IOException ignored) {}
                    continue;
                }

                // canal aún no válido -> lo monto
                comController.setSocket(socket);

            } catch (IOException e) {
                System.out.println("[ServerConnector] Error en ServerSocket: " + e.getMessage());
                try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
                serverSocket = null;

                // descanso corto para no hacer loop infinito si algo va mal
                sleep(300);
            }
        }
    }

    private void conectarPuerto() {
        try {
            serverSocket = new ServerSocket(mainPort);
            actualPort = mainPort;
        } catch (IOException e) {
            try {
                serverSocket = new ServerSocket(auxPort);
                actualPort = auxPort;
            } catch (IOException ex) {
                throw new RuntimeException("No puedo abrir puertos " + mainPort + " ni " + auxPort, ex);
            }
        }

        System.out.println("[ServerConnector] Escuchando en " + actualPort);
    }

    public boolean isConected() {
        return serverSocket != null;
    }

    public int getActualPort() {
        return actualPort;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}