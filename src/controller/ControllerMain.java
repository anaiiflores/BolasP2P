package controller;

import comunications.Controller2;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;
import view.MainFrame;

import java.net.*;
import java.util.Enumeration;
public class ControllerMain {

    // IPs reales de 2 PCs (cuando uses 2 ordenadores)
    private static final String IP_PEER_A = "ipfalsa";
    private static final String IP_PEER_B = "ipfalsa";

    private static final int PRIMARY_PORT   = 5000;
    private static final int SECONDARY_PORT = 5001;

    private final GameController gameController;
    private final Controller2 networkController;

    public ControllerMain() {

        String localIp = detectLocalIp();

        String peerId;
        String remoteIp;

        // --- Caso 2 PCs (por IP) ---
        if (localIp != null && localIp.equals(IP_PEER_A)) {
            peerId = "A";
            remoteIp = IP_PEER_B;

        } else if (localIp != null && localIp.equals(IP_PEER_B)) {
            peerId = "B";
            remoteIp = IP_PEER_A;

        } else {
            // --- Caso LOCALHOST (1 PC, 2 ventanas) ---
            remoteIp = "localhost";
            System.out.println("[ControllerMain] MODO LOCAL: usando localhost");

            // 1) primero red (para saber en qué puerto escucha)
            this.networkController = new Controller2(this, remoteIp, PRIMARY_PORT, SECONDARY_PORT);

            int listenPort = networkController.getActualListenPort();
            peerId = (listenPort == PRIMARY_PORT) ? "A" : "B";

            System.out.println("[ControllerMain] Local IP: " + localIp +
                    " | Peer: " + peerId +
                    " | listenPort: " + listenPort +
                    " | Remote IP: " + remoteIp);

            // 2) UI con puerto REAL
            MainFrame frame = new MainFrame(
                    "Bolas P2P - Peer " + peerId + " (listen " + listenPort + ")",
                    720, 420
            );
            frame.setVisible(true);

            // 3) juego
            this.gameController = new GameController(frame, peerId, this);

            // estado inicial (si quieres SOLO en A, te digo abajo)
            gameController.spawnLocalBall();
            gameController.spawnLocalBall();

            return; // <- IMPORTANTÍSIMO: ya terminamos el constructor aquí
        }

        // --- Caso 2 PCs: aquí sí seguimos normal ---
        System.out.println("[ControllerMain] Local IP: " + localIp +
                " | Peer: " + peerId +
                " | Remote IP: " + remoteIp);

        MainFrame frame = new MainFrame(
                "Bolas P2P - Peer " + peerId + " (ports " + PRIMARY_PORT + "/" + SECONDARY_PORT + ")",
                720,
                420
        );
        frame.setVisible(true);

        this.gameController = new GameController(frame, peerId, this);
        this.networkController = new Controller2(this, remoteIp, PRIMARY_PORT, SECONDARY_PORT);

        gameController.spawnLocalBall();
        gameController.spawnLocalBall();
    }



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