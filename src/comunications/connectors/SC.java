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

            if (!comController.isValid()) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("[ServerConnector] Conexión entrante: " + socket.getInetAddress());

                    // Si todavía no hay socket válido en comunications.channel.Channel, lo usamos
                    if (!comController.isValid()) {
                        comController.setSocket(socket);
                    } else {
                        socket.close();
                    }

                } catch (IOException e) {
                    System.out.println("[ServerConnector] Error en ServerSocket: " + e.getMessage());

                    // si el ServerSocket se rompió, lo reiniciamos
                    try {
                        if (serverSocket != null) serverSocket.close();
                    } catch (IOException ignored) {}
                    serverSocket = null;
                }
            } else {
                // Ya conectado: no hace falta aceptar cada milisegundo
                sleep(3000);
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
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}