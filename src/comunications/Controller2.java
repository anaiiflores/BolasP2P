package comunications;

import controller.ControllerMain;
import comunications.channel.Channel;
import comunications.connectors.CC;
import comunications.connectors.SC;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;

import java.net.Socket;

/**
 * Controller2 = "controlador de red" (la capa de comunicaciones).
 *
 * - Tiene un Channel: canal activo (socket + streams + reader thread + health)
 * - Tiene un SC: ServerConnector (ServerSocket accept -> recibe conexiones entrantes)
 * - Tiene un CC: ClientConnector (intenta conectar activamente al peer remoto)
 *
 * IDEA:
 * - SC y CC están en bucle.
 * - Cuando entra una conexión o se logra conectar, se llama channel.setSocket(socket)
 * - El Channel se encarga de leer MsgDTO y reenviar a ControllerMain (master).
 */
public class Controller2 {

    private final ControllerMain master;     // puente hacia la lógica del juego (GameController)
    private final Channel channel;           // canal real (socket + OIS/OOS)
    private final CC clientConnector;        // conecta hacia fuera (new Socket)
    private final SC serverConnector;        // escucha conexiones entrantes (ServerSocket)

    private final int localPort1;
    private final int localPort2;

    public Controller2(ControllerMain master, String ipRemota, int mainPort, int auxPort) {
        this.master = master;

        this.localPort1 = mainPort;
        this.localPort2 = auxPort;

        // Channel conoce la ip remota (por logs o lógica extra), y sabe hablar con Controller2
        this.channel = new Channel(ipRemota, this);

        // Conector servidor: intenta abrir mainPort y si falla auxPort
        this.serverConnector = new SC(this, mainPort, auxPort);

        // Conector cliente: intenta conectar a la IP remota por mainPort y auxPort
        this.clientConnector = new CC(this, mainPort, auxPort, ipRemota);

        inicializar();
    }

    /**
     * Arranca los dos hilos:
     * - SC: queda "escuchando" accept()
     * - CC: queda intentando conectar si no hay canal válido
     */
    private void inicializar() {
        new Thread(serverConnector, "ServerConnector").start();
        new Thread(clientConnector, "ClientConnector").start();
    }

    /**
     * isValid = ¿hay un canal listo para enviar/recibir?
     * Depende de channel.isValid().
     */
    public boolean isValid() {
        return channel.isValid();
    }

    /**
     * setSocket:
     * - lo llaman SC o CC cuando consiguen un socket conectado.
     * - Channel decide si lo acepta o lo rechaza.
     */
    public void setSocket(Socket socket) {
        channel.setSocket(socket);
    }

    /**
     * onChannelDown:
     * Este método lo llamará el Channel cuando detecte que se ha caído la conexión
     * (EOFException / IOException en readObject()).
     *
     * No hace falta reconectar manualmente aquí, porque:
     * - CC ya está en bucle y, cuando isValid() sea false, reintentará conectar.
     * - SC ya está escuchando accept() para aceptar reconexiones entrantes.
     *
     * Aun así, viene genial para:
     * - logs
     * - métricas
     * - actualizar UI si quisieras (no lo haces aquí porque es clase de red)
     */
    public void onChannelDown() {
        System.out.println("[Controller2] Canal caído. CC reintentará conectar automáticamente.");
    }

    /**
     * getAvailablePort:
     * Solo para modo localhost.
     * Espera a que el serverConnector haya abierto puerto.
     * Si escuchas en localPort2, devuelves localPort1 (y viceversa),
     * para evitar que intentes conectar al mismo puerto que tú estás usando.
     */
    public int getAvailablePort() {
        while (!serverConnector.isConected()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
        int actual = serverConnector.getActualPort();
        return (actual == localPort2) ? localPort1 : localPort2;
    }


    // llega de red -> se lo paso al ControllerMain -> GameController
    public void introducirBola(BolaDTO bolaDTO) { master.introducirBola(bolaDTO); }

    // juego quiere enviar -> lo mando por el Channel
    public void lanzarBola(BolaDTO bolaDTO) { channel.lanzarBola(bolaDTO); }

    public void introducirSprite(SpriteDTO dto) { master.introducirSprite(dto); }
    public void lanzarSprite(SpriteDTO dto) { channel.lanzarSprite(dto); }
}