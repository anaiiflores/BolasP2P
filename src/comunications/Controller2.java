package comunications;

import controller.ControllerMain;
import comunications.channel.Channel;
import comunications.connectors.CC;
import comunications.connectors.SC;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;

import java.net.Socket;

public class Controller2 {

    private final ControllerMain master;

    private final Channel channel;
    private final CC clientConnector;
    private final SC serverConnector;

    private final int localPort1;
    private final int localPort2;

    public Controller2(ControllerMain master, String ipRemota, int mainPort, int auxPort) {
        this.master = master;

        this.localPort1 = mainPort;
        this.localPort2 = auxPort;

        this.channel = new Channel(ipRemota, this);
        this.serverConnector = new SC(this, mainPort, auxPort);
        this.clientConnector = new CC(this, mainPort, auxPort, ipRemota);

        inicializar();
    }

    private void inicializar() {
        new Thread(serverConnector, "ServerConnector").start();
        new Thread(clientConnector, "ClientConnector").start();
    }

    public boolean isValid() { return channel.isValid(); }

    // ✅ synchronized para que CC y SC no “ganen” a la vez
    public synchronized void setSocket(Socket socket) {
        channel.setSocket(socket);
    }

    public void onChannelDown() {
        // solo log (pero te sirve para saber que CC/SC deberían reintentar)
        System.out.println("[Controller2] Canal caído -> CC/SC reintentará");
    }

    public int getAvailablePort() {
        while (!serverConnector.isConected()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
        int actual = serverConnector.getActualPort();
        return (actual == localPort2) ? localPort1 : localPort2;
    }

    public int getActualListenPort() {
        while (!serverConnector.isConected()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
        return serverConnector.getActualPort();
    }

    // puente hacia juego
    public void introducirBola(BolaDTO bolaDTO) { master.introducirBola(bolaDTO); }
    public void lanzarBola(BolaDTO bolaDTO) { channel.lanzarBola(bolaDTO); }

    public void introducirSprite(SpriteDTO dto) { master.introducirSprite(dto); }
    public void lanzarSprite(SpriteDTO dto) { channel.lanzarSprite(dto); }
}