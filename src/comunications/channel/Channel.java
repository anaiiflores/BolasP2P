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

    public synchronized boolean isValid() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null && in != null;
    }

    public synchronized void setSocket(Socket newSocket) {
        if (isValid()) {
            try { newSocket.close(); } catch (IOException ignored) {}
            return;
        }

        closeInternal();

        if (newSocket == null || newSocket.isClosed() || !newSocket.isConnected()) {
            System.out.println("[comunications.channel.Channel] Socket inválido en setSocket()");
            return;
        }

        this.socket = newSocket;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            healthChannel = new HealthChannel(this);

            readerThread = new Thread(this, "ChannelReader");
            readerThread.start();

            healthThread = new Thread(healthChannel, "comunications.channel.HealthChannel");
            healthThread.start();

            System.out.println("[comunications.channel.Channel] (IP remota: " + ipRemota + ")");

        } catch (IOException e) {
            System.out.println("[comunications.channel.Channel] Error creando streams: " + e.getMessage());
            closeInternal();
        }
    }


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
            System.out.println("[comunications.channel.Channel] Error enviando: " + e.getMessage());
            closeInternal();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                if (!isValid()) break;

                Object obj = in.readObject();
                if (!(obj instanceof MsgDTO msg)) continue;

                procesarMensaje(msg);

            } catch (EOFException e) {
                System.out.println("[comunications.channel.Channel] Conexión cerrada por el otro extremo");
                break;
            } catch (IOException e) {
                System.out.println("[comunications.channel.Channel] Error IO leyendo: " + e.getMessage());
                break;
            } catch (ClassNotFoundException e) {
                System.out.println("[comunications.channel.Channel] Clase no encontrada: " + e.getMessage());
                break;
            }
        }

        closeInternal();
        System.out.println("[comunications.channel.Channel] Thread lector terminado");
    }

    private void procesarMensaje(MsgDTO msg) {
        switch (msg.getHeader()) {

            case 0: { //
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

            case 3: {
                SpriteDTO dto = (SpriteDTO) msg.getPayload();
                com.introducirSprite(dto);
                break;
            }

            default:
                System.out.println("[comunications.channel.Channel] Header desconocido: " + msg.getHeader());
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

        readerThread = null;
        healthThread = null;
        healthChannel = null;
    }
}