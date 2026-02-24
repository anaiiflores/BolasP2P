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

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private HealthChannel healthChannel;
    private Thread readerThread;
    private Thread healthThread;

    public Channel(String ipRemota, Controller2 com) {
        this.ipRemota = ipRemota;
        this.com = com;
    }

    // ==========================
    // ESTADO
    // ==========================
    public synchronized boolean isValid() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown()
                && in != null
                && out != null;
    }

    // ==========================
    // SOCKET
    // ==========================
    public synchronized void setSocket(Socket newSocket) {

        // Si ya hay conexión buena, cierro la nueva
        if (isValid()) {
            try { newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        // Limpieza total previa (CLAVE para reconectar)
        closeInternal();

        if (newSocket == null || newSocket.isClosed() || !newSocket.isConnected()) {
            System.out.println("[Channel] Socket inválido en setSocket()");
            return;
        }

        this.socket = newSocket;

        try {
            // ORDEN SEGURO
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            healthChannel = new HealthChannel(this);

            readerThread = new Thread(this, "ChannelReader");
            readerThread.start();

            healthThread = new Thread(healthChannel, "HealthChannel");
            healthThread.start();

            System.out.println("[Channel] ✅ Conectado a " + ipRemota);

        } catch (IOException e) {
            System.out.println("[Channel] Error creando streams: " + e.getMessage());
            closeInternal();
        }
    }

    // ==========================
    // ENVÍO
    // ==========================
    public void comprobarConexion() {
        send(new MsgDTO(1, null));
    }

    public void lanzarBola(BolaDTO dto) {
        send(new MsgDTO(0, dto));
    }

    public void lanzarSprite(SpriteDTO dto) {
        send(new MsgDTO(3, dto));
    }

    private synchronized void send(MsgDTO msg) {
        if (!isValid()) return;

        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("[Channel] Error enviando → reconectar");
            closeInternal();
        }
    }

    // ==========================
    // RECEPCIÓN
    // ==========================
    @Override
    public void run() {
        try {
            while (isValid()) {
                Object obj = in.readObject();
                if (!(obj instanceof MsgDTO msg)) continue;
                procesarMensaje(msg);
            }
        } catch (EOFException e) {
            System.out.println("[Channel] Conexión cerrada por el otro extremo");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Channel] Error leyendo: " + e.getMessage());
        } finally {
            // CLAVE: dejar el canal inválido para que CC/SC reconecten
            closeInternal();
            System.out.println("[Channel] Desconectado, esperando reconexión...");
        }
    }

    private void procesarMensaje(MsgDTO msg) {
        switch (msg.getHeader()) {

            case 0 -> com.introducirBola((BolaDTO) msg.getPayload());

            case 1 -> send(new MsgDTO(2, null)); // ping

            case 2 -> {
                if (healthChannel != null) healthChannel.notifyHealthy();
            }

            case 3 -> com.introducirSprite((SpriteDTO) msg.getPayload());

            default -> System.out.println("[Channel] Header desconocido: " + msg.getHeader());
        }
    }

    // ==========================
    // CIERRE
    // ==========================
    public synchronized void close() {
        closeInternal();
    }

    private synchronized void closeInternal() {

        if (readerThread != null) readerThread.interrupt();
        if (healthThread != null) healthThread.interrupt();

        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        in = null;
        out = null;
        socket = null;
        readerThread = null;
        healthThread = null;
        healthChannel = null;
    }
}