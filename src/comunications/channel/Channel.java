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

    private volatile Socket socket;
    private volatile ObjectInputStream in;
    private volatile ObjectOutputStream out;

    private volatile HealthChannel healthChannel;
    private volatile Thread readerThread;
    private volatile Thread healthThread;

    public Channel(String ipRemota, Controller2 com) {
        this.ipRemota = ipRemota;
        this.com = com;
    }

    // ✅ estricto: si algo está medio muerto, esto debe ser false
    public synchronized boolean isValid() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown()
                && in != null
                && out != null;
    }

    public synchronized void setSocket(Socket newSocket) {
        // Si ya tengo uno válido, cierro el nuevo
        if (isValid()) {
            try { newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        // limpiar restos
        closeInternal();

        if (newSocket == null || newSocket.isClosed() || !newSocket.isConnected()) {
            System.out.println("[Channel] Socket inválido en setSocket()");
            return;
        }

        this.socket = newSocket;

        try {
            // ✅ ORDEN SEGURO (evita deadlock ObjectStream)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // health
            healthChannel = new HealthChannel(this);

            // reader
            readerThread = new Thread(this, "ChannelReader");
            readerThread.start();

            // health thread
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
        send(new MsgDTO(0, bolaDTO)); // bola
    }

    public void lanzarSprite(SpriteDTO dto) {
        send(new MsgDTO(3, dto)); // sprite
    }

    private synchronized void send(MsgDTO msg) {
        if (!isValid()) return;

        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("[Channel] Error enviando: " + e.getMessage());
            closeInternal();          // ✅ deja listo para reconectar
            com.onChannelDown();      // opcional (log)
        }
    }

    // ==========================
    // LECTURA
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
            } catch (Exception e) {
                System.out.println("[Channel] Error inesperado: " + e.getMessage());
                break;
            }
        }

        closeInternal();          // ✅ CLAVE
        com.onChannelDown();
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
                send(new MsgDTO(2, null)); // pong
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
        // parar hilos si existen
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