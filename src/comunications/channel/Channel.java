package comunications.channel;

import comunications.MsgDTO;
import comunications.Controller2;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Channel implements Runnable {

    private final String ipRemota;
    private final Controller2 com;

    private volatile Socket socket;
    private volatile ObjectInputStream in;
    private volatile ObjectOutputStream out;

    private volatile HealthChannel healthChannel;
    private volatile Thread readerThread;
    private volatile Thread healthThread;

    private volatile boolean stopRequested = false;

    public Channel(String ipRemota, Controller2 com) {
        this.ipRemota = ipRemota;
        this.com = com;
    }

    public synchronized boolean isValid() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null && in != null;
    }

    public void notifyDown() {
        com.onChannelDown();
    }

    public synchronized void setSocket(Socket newSocket) {
        if (stopRequested) {
            try { if (newSocket != null) newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        // si ya hay conexión válida, no la pisamos
        if (isValid()) {
            try { if (newSocket != null) newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        closeInternal();

        if (newSocket == null || newSocket.isClosed() || !newSocket.isConnected()) {
            System.out.println("[Channel] Socket inválido en setSocket()");
            return;
        }

        this.socket = newSocket;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // reader: se arranca UNA vez y se queda vivo
            if (readerThread == null) {
                readerThread = new Thread(this, "ChannelReader");
                readerThread.start();
            }

            // health: se arranca UNA vez y se queda vivo
            if (healthThread == null) {
                healthChannel = new HealthChannel(this);
                healthThread = new Thread(healthChannel, "HealthChannel");
                healthThread.start();
            }

            System.out.println("[Channel] ✅ Listo (IP remota: " + ipRemota + ")");

        } catch (IOException e) {
            System.out.println("[Channel] Error creando streams: " + e.getMessage());
            closeInternal();
            notifyDown();
        }
    }

    // ===== envío =====
    public void comprobarConexion() { send(new MsgDTO(1, null)); }
    public void lanzarBola(BolaDTO bolaDTO) { send(new MsgDTO(0, bolaDTO)); }
    public void lanzarSprite(SpriteDTO dto) { send(new MsgDTO(3, dto)); }

    private synchronized void send(MsgDTO msg) {
        if (!isValid()) return;

        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("[Channel] Error enviando: " + e.getMessage());
            closeInternal();
            notifyDown();
        }
    }

    // ===== recepción =====
    @Override
    public void run() {
        while (!stopRequested) {
            try {
                // ✅ si no hay socket válido, espera; NO muere
                if (!isValid()) {
                    sleepSilently(200);
                    continue;
                }

                Object obj = in.readObject();
                if (!(obj instanceof MsgDTO msg)) continue;

                procesarMensaje(msg);

            } catch (EOFException e) {
                System.out.println("[Channel] Conexión cerrada por el otro extremo");
                closeInternal();
                notifyDown();
                sleepSilently(300);

            } catch (IOException e) {
                System.out.println("[Channel] Error IO leyendo: " + e.getMessage());
                closeInternal();
                notifyDown();
                sleepSilently(300);

            } catch (ClassNotFoundException e) {
                System.out.println("[Channel] Clase no encontrada: " + e.getMessage());
            }
        }

        closeInternal();
        System.out.println("[Channel] Thread lector detenido");
    }

    private void procesarMensaje(MsgDTO msg) {
        switch (msg.getHeader()) {
            case 0 -> com.introducirBola((BolaDTO) msg.getPayload());
            case 1 -> send(new MsgDTO(2, null)); // ping -> pong
            case 2 -> { if (healthChannel != null) healthChannel.notifyHealthy(); }
            case 3 -> com.introducirSprite((SpriteDTO) msg.getPayload());
            default -> System.out.println("[Channel] Header desconocido: " + msg.getHeader());
        }
    }

    public synchronized void close() {
        closeInternal();
    }

    private synchronized void closeInternal() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        in = null;
        out = null;
        socket = null;
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}