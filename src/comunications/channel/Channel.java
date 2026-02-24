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

    // ✅ MÁS ESTRICTO: si el socket se cayó, esto debe ser false
    public synchronized boolean isValid() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown()
                && out != null
                && in != null;
    }

    public synchronized void setSocket(Socket newSocket) {
        // Si ya tengo un canal válido, cierro el nuevo y listo
        if (isValid()) {
            try { newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        // ✅ limpiar todo lo anterior (por si quedó medio roto)
        closeInternal();

        if (newSocket == null || newSocket.isClosed() || !newSocket.isConnected()) {
            System.out.println("[Channel] Socket inválido en setSocket()");
            return;
        }

        this.socket = newSocket;

        try {
            // ✅ ORDEN SEGURO: primero OUT + flush, luego IN
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Health (opcional, pero lo tienes)
            healthChannel = new HealthChannel(this);

            // Reader
            readerThread = new Thread(this, "ChannelReader");
            readerThread.start();

            // Health thread
            healthThread = new Thread(healthChannel, "HealthChannel");
            healthThread.start();

            System.out.println("[Channel] ✅ Listo (IP remota: " + ipRemota + ")");

        } catch (IOException e) {
            System.out.println("[Channel] Error creando streams: " + e.getMessage());
            closeInternal();
        }
    }

    // ==========================
    // ENVÍO
    // ==========================

    public void comprobarConexion() {
        send(new MsgDTO(1, null)); // ping
    }

    public void lanzarBola(BolaDTO bolaDTO) {
        send(new MsgDTO(0, bolaDTO));
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
            System.out.println("[Channel] Error enviando: " + e.getMessage());
            closeInternal(); // ✅ CLAVE: dejar listo para reconectar
        }
    }

    // ==========================
    // RECEPCIÓN
    // ==========================

    @Override
    public void run() {
        while (true) {
            try {
                if (!isValid()) break;

                Object obj = in.readObject();
                if (!(obj instanceof MsgDTO msg)) continue;

                procesarMensaje(msg);

            } catch (EOFException e) {
                System.out.println("[Channel] Conexión cerrada por el otro extremo");
                break;
            } catch (IOException e) {
                System.out.println("[Channel] Error IO leyendo: " + e.getMessage());
                break;
            } catch (ClassNotFoundException e) {
                System.out.println("[Channel] Clase no encontrada: " + e.getMessage());
                break;
            }
        }

        // ✅ IMPORTANTÍSIMO: limpiar para que CC/SC detecten isValid() == false
        closeInternal();
        System.out.println("[Channel] desconectado, esperando reconexión...");
    }

    private void procesarMensaje(MsgDTO msg) {
        switch (msg.getHeader()) {

            case 0: { // bola
                BolaDTO bola = (BolaDTO) msg.getPayload();
                com.introducirBola(bola);
                break;
            }

            case 1: // ping
                send(new MsgDTO(2, null));
                break;

            case 2: // pong
                if (healthChannel != null) healthChannel.notifyHealthy();
                break;

            case 3: { // sprite
                SpriteDTO dto = (SpriteDTO) msg.getPayload();
                com.introducirSprite(dto);
                break;
            }

            default:
                System.out.println("[Channel] Header desconocido: " + msg.getHeader());
        }
    }

    // ==========================
    // CIERRE
    // ==========================

    public synchronized void close() {
        closeInternal();
    }

    private synchronized void closeInternal() {
        // ✅ parar threads (si existen) para que no se queden colgando
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