package comunications.channel;

import comunications.Controller2;
import comunications.MsgDTO;
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

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private HealthChannel healthChannel;
    private Thread readerThread;
    private Thread healthThread;

    // canal operativo
    private volatile boolean running = false;

    public Channel(String ipRemota, Controller2 com) {
        this.ipRemota = ipRemota;
        this.com = com;
    }

    public synchronized boolean isValid() {
        return running
                && socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && in != null
                && out != null;
    }

    /** Llamado por SC o CC cuando consiguen un socket conectado */
    public synchronized void setSocket(Socket newSocket) {

        // Si ya tengo canal válido, rechazo el socket nuevo
        if (isValid()) {
            try { newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        // Limpio restos anteriores
        closeInternal();

        if (newSocket == null || newSocket.isClosed() || !newSocket.isConnected()) {
            System.out.println("[Channel] Socket inválido en setSocket()");
            return;
        }

        this.socket = newSocket;

        try {
            // IMPORTANTE: primero OUT + flush, luego IN (evita deadlocks con ObjectStreams)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            running = true;

            // Reader permanente: se crea 1 vez y se queda esperando reconexiones
            if (readerThread == null) {
                readerThread = new Thread(this, "ChannelReader-" + ipRemota);
                readerThread.start();
            }

            // Health nuevo por cada conexión
            healthChannel = new HealthChannel(this);
            healthThread = new Thread(healthChannel, "HealthChannel-" + ipRemota);
            healthThread.start();

            System.out.println("[Channel] ✅ Conectado (IP remota: " + ipRemota + ")");

        } catch (IOException e) {
            System.out.println("[Channel] Error creando streams: " + e.getMessage());
            closeInternal();
            com.onChannelDown();
        }
    }

    // ===== ENVÍO =====
    public void comprobarConexion() { send(new MsgDTO(1, null)); }        // PING
    public void lanzarBola(BolaDTO bolaDTO) { send(new MsgDTO(0, bolaDTO)); }
    public void lanzarSprite(SpriteDTO dto) { send(new MsgDTO(3, dto)); }

    /** Envío seguro: usa copia local de out para evitar null en mitad */
    private void send(MsgDTO msg) {
        ObjectOutputStream localOut;

        synchronized (this) {
            if (!isValid()) return;
            localOut = out;
        }

        try {
            localOut.writeObject(msg);
            localOut.flush();
        } catch (IOException e) {
            System.out.println("[Channel] Error enviando: " + e.getMessage());
            // baja el canal y notifica
            closeInternal();
            com.onChannelDown();
        }
    }

    // ===== RECEPCIÓN =====
    @Override
    public void run() {
        while (true) {

            ObjectInputStream localIn;

            synchronized (this) {
                // sin conexión: me duermo y espero a que setSocket() lo monte otra vez
                if (!running || !isValid()) {
                    localIn = null;
                } else {
                    localIn = in;
                }
            }

            if (localIn == null) {
                sleepSilently(200);
                continue;
            }

            try {
                Object obj = localIn.readObject(); // bloqueante
                if (obj instanceof MsgDTO msg) {
                    procesarMensaje(msg);
                }

            } catch (EOFException e) {
                // el otro cerró limpio
                closeInternal();
                com.onChannelDown();

            } catch (IOException e) {
                // red caída / socket cerrado / etc.
                closeInternal();
                com.onChannelDown();

            } catch (ClassNotFoundException e) {
                System.out.println("[Channel] Clase no encontrada: " + e.getMessage());
            }
        }
    }

    private void procesarMensaje(MsgDTO msg) {
        // ✅ IMPORTANTÍSIMO: si llega CUALQUIER cosa, hay actividad -> canal vivo
        if (healthChannel != null) healthChannel.notifyHealthy();

        switch (msg.getHeader()) {
            case 0 -> com.introducirBola((BolaDTO) msg.getPayload());

            case 1 -> {
                // me llegó ping -> respondo pong
                send(new MsgDTO(2, null));
            }

            case 2 -> {
                // me llegó pong -> vivo
                if (healthChannel != null) healthChannel.notifyPong();
            }

            case 3 -> com.introducirSprite((SpriteDTO) msg.getPayload());

            default -> System.out.println("[Channel] Header desconocido: " + msg.getHeader());
        }
    }

    public synchronized void close() {
        closeInternal();
    }

    private synchronized void closeInternal() {
        running = false;

        // ✅ Para health de verdad (no solo interrupt)
        if (healthChannel != null) {
            healthChannel.stop();
        }
        if (healthThread != null) {
            healthThread.interrupt();
        }

        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        in = null;
        out = null;
        socket = null;

        healthThread = null;
        healthChannel = null;
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}