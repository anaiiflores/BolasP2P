package comunications;

import controller.ControllerMain;
import comunications.channel.Channel;
import comunications.connectors.CC;
import comunications.connectors.SC;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;

import java.net.Socket;

/**
 * Controller2 = capa de comunicaciones.
 *
 * - SC: acepta conexiones entrantes (ServerSocket.accept)
 * - CC: intenta conectar activamente (new Socket)
 * - Channel: gestiona socket+streams+reader+health
 */
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

    public boolean isValid() {
        return channel.isValid();
    }

    /**
     * IMPORTANTE: synchronized para evitar carreras:
     * - SC acepta y llama setSocket()
     * - CC conecta y llama setSocket()
     * Si entran “a la vez”, uno debe ganar y el otro cerrar su socket.
     */
    public synchronized void setSocket(Socket socket) {
        channel.setSocket(socket);
    }

    public void onChannelDown() {
        System.out.println("[Controller2] Canal caído. CC reintentará conectar automáticamente.");
    }

    /**
     * Solo para modo localhost:
     * espera a que SC haya abierto un puerto y devuelve el “otro” para no conectar
     * al mismo puerto que está escuchando.
     */
    public int getAvailablePort() {
        while (!serverConnector.isConected()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
        int actual = serverConnector.getActualPort();
        return (actual == localPort2) ? localPort1 : localPort2;
    }

    // ===== Puente juego <-> red =====

    public void introducirBola(BolaDTO bolaDTO) {
        master.introducirBola(bolaDTO);
    }

    public void lanzarBola(BolaDTO bolaDTO) {
        channel.lanzarBola(bolaDTO);
    }

    public void introducirSprite(SpriteDTO dto) {
        master.introducirSprite(dto);
    }

    public void lanzarSprite(SpriteDTO dto) {
        channel.lanzarSprite(dto);
    }
}