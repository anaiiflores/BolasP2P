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
    private volatile boolean running = false;

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
            System.out.println("[comunications.channel.Channel] Error enviando: " + e.getMessage());
            closeInternal();
        }
    }

    @Override
    public void run() {
        while (true) {

            ObjectInputStream localIn;

            synchronized (this) {
                if (!isValid()) break;
                localIn = in; // copia local segura
            }

            try {
                Object obj = localIn.readObject();
                if (obj instanceof MsgDTO msg) {
                    procesarMensaje(msg);
                }
            } catch (EOFException e) {
                break;
            } catch (IOException | ClassNotFoundException e) {
                break;
            }
        }

        com.onChannelDown();
        closeInternal();
        System.out.println("[Channel] Thread lector terminado");
    }

    private void procesarMensaje(MsgDTO msg) {
        switch (msg.getHeader()) {

            case 0: {
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
        running = false;

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