package controller;

import comunications.Controller2;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;
import view.MainFrame;

import java.net.*;
import java.util.Enumeration;

/**
 * ControllerMain = "orquestador":
 * - Detecta quién soy (A o B) según mi IP local
 * - Crea la UI
 * - Crea GameController (lógica de juego)
 * - Crea Controller2 (red)
 * - Hace de puente:
 *      juego -> networkController -> channel
 *      red -> networkController -> gameController
 */
public class ControllerMain {

    // IPs fijas (las de tus PCs)
    private static final String IP_PEER_A = "10.162.201.235";
    private static final String IP_PEER_B = "10.162.201.74";

    // puertos P2P
    private static final int PRIMARY_PORT   = 5000;
    private static final int SECONDARY_PORT = 5001;

    private final GameController gameController;
    private final Controller2 networkController;

    public ControllerMain() {

        // detectar ip local para decidir rol
        String localIp = detectLocalIp();

        String peerId;
        String remoteIp;

        // si soy la ip A, entonces yo soy peer A y mi remoto es B
        if (localIp != null && localIp.equals(IP_PEER_A)) {
            peerId = "A";
            remoteIp = IP_PEER_B;

            // si soy la ip B, entonces yo soy peer B y mi remoto es A
        } else if (localIp != null && localIp.equals(IP_PEER_B)) {
            peerId = "B";
            remoteIp = IP_PEER_A;

        } else {
            // fallback: modo local (1 PC)
            peerId = "A";
            remoteIp = "localhost";
            System.out.println("[ControllerMain] MODO LOCAL: usando localhost");
        }

        System.out.println(
                "[ControllerMain] Local IP: " + localIp +
                        " | Peer: " + peerId +
                        " | Remote IP: " + remoteIp
        );

        // UI
        MainFrame frame = new MainFrame(
                "Bolas P2P - Peer " + peerId + " (port " + PRIMARY_PORT + ")",
                720,
                420
        );
        frame.setVisible(true);

        // Lógica del juego (bolas/sprite y envío de DTOs)
        this.gameController = new GameController(frame, peerId, this);

        // Comunicaciones P2P (abre server, intenta cliente, crea channel)
        this.networkController =
                new Controller2(this, remoteIp, PRIMARY_PORT, SECONDARY_PORT);

        // Estado inicial: crea bolas
        gameController.spawnLocalBall();
        gameController.spawnLocalBall();
    }

    /**
     * detectLocalIp:
     * - recorre interfaces de red activas
     * - busca IPv4 privada (10.x, 172.16.x, 192.168.x)
     * - si no encuentra, intenta InetAddress.getLocalHost()
     */
    private String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();

                        if (ip.startsWith("10.")
                                || ip.startsWith("172.16.")
                                || ip.startsWith("192.168.")) {
                            return ip;
                        }
                    }
                }
            }

            return InetAddress.getLocalHost().getHostAddress();

        } catch (Exception e) {
            return null;
        }
    }

    // ----------------------------
    // PUENTE: red <-> juego
    // ----------------------------

    // Desde red hacia juego
    public void introducirBola(BolaDTO bolaDTO) {
        if (gameController == null) return;
        gameController.introducirBola(bolaDTO);
    }

    // Desde juego hacia red
    public void lanzarBola(BolaDTO bolaDTO) {
        networkController.lanzarBola(bolaDTO);
    }

    public void introducirSprite(SpriteDTO dto) {
        if (gameController == null) return;
        gameController.introducirSprite(dto);
    }

    public void lanzarSprite(SpriteDTO dto) {
        networkController.lanzarSprite(dto);
    }
}