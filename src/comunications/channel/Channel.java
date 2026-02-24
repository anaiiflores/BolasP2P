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

    // indica si el canal está “montado” y operativo
    private volatile boolean running = false;

    public Channel(String ipRemota, Controller2 com) {
        this.ipRemota = ipRemota;
        this.com = com;
    }

    /** ¿Puedo usar el canal ahora mismo? */
    public synchronized boolean isValid() {
        return running
                && socket != null
                && !socket.isClosed()
                && in != null
                && out != null;
    }

    /** Llamado por SC o CC cuando consiguen un socket conectado */
    public synchronized void setSocket(Socket newSocket) {
        // Si ya estoy conectado, rechazo la conexión nueva
        if (isValid()) {
            try { newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        // Limpio cualquier resto anterior
        closeInternal();

        // Si el socket no es usable, fuera
        if (newSocket == null || newSocket.isClosed() || !newSocket.isConnected()) {
            System.out.println("[Channel] Socket inválido en setSocket()");
            return;
        }

        this.socket = newSocket;

        try {
            // IMPORTANTE: primero out y flush, luego in (para evitar deadlock)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            running = true;

            // Arranco reader solo una vez
            if (readerThread == null) {
                readerThread = new Thread(this, "ChannelReader");
                readerThread.start();
            }

            // Arranco health solo una vez por conexión
            healthChannel = new HealthChannel(this);
            healthThread = new Thread(healthChannel, "HealthChannel");
            healthThread.start();

            System.out.println("[Channel] ✅ Conectado (IP remota: " + ipRemota + ")");

        } catch (IOException e) {
            System.out.println("[Channel] Error creando streams: " + e.getMessage());
            closeInternal();
            com.onChannelDown();
        }
    }

    // ===== ENVÍO =====
    public void comprobarConexion() { send(new MsgDTO(1, null)); }
    public void lanzarBola(BolaDTO bolaDTO) { send(new MsgDTO(0, bolaDTO)); }
    public void lanzarSprite(SpriteDTO dto) { send(new MsgDTO(3, dto)); }

    /** Envío seguro sin NPE: uso copia local de out */
    private void send(MsgDTO msg) {
        ObjectOutputStream localOut;

        synchronized (this) {
            if (!isValid()) return;
            localOut = out; // copia local para evitar que se vuelva null a mitad
        }

        try {
            localOut.writeObject(msg);
            localOut.flush();
        } catch (IOException e) {
            System.out.println("[Channel] Error enviando: " + e.getMessage());
            closeInternal();
            com.onChannelDown();
        }
    }

    // ===== RECEPCIÓN =====
    @Override
    public void run() {
        while (true) {

            ObjectInputStream localIn;

            // si no hay canal válido, espero y sigo
            synchronized (this) {
                if (!running) {
                    // si nunca hay running, el CC/SC lo pondrán con setSocket
                    localIn = null;
                } else if (!isValid()) {
                    localIn = null;
                } else {
                    localIn = in; // copia local segura
                }
            }

            if (localIn == null) {
                sleepSilently(200);
                continue;
            }

            try {
                Object obj = localIn.readObject();
                if (obj instanceof MsgDTO msg) {
                    procesarMensaje(msg);
                }

            } catch (EOFException e) {
                // el otro cerró limpio
                closeInternal();
                com.onChannelDown();

            } catch (IOException e) {
                // se cayó la red o cerraron el socket
                closeInternal();
                com.onChannelDown();

            } catch (ClassNotFoundException e) {
                System.out.println("[Channel] Clase no encontrada: " + e.getMessage());
            }
        }
    }

    private void procesarMensaje(MsgDTO msg) {
        switch (msg.getHeader()) {
            case 0 -> com.introducirBola((BolaDTO) msg.getPayload());
            case 1 -> send(new MsgDTO(2, null)); // ping -> respondo pong
            case 2 -> { if (healthChannel != null) healthChannel.notifyHealthy(); } // pong
            case 3 -> com.introducirSprite((SpriteDTO) msg.getPayload());
            default -> System.out.println("[Channel] Header desconocido: " + msg.getHeader());
        }
    }

    public synchronized void close() {
        closeInternal();
    }

    /** Cierra TODO de esta conexión. No mata el hilo reader; el hilo espera reconexión. */
    private synchronized void closeInternal() {
        running = false;

        if (healthThread != null) healthThread.interrupt();

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