package controller;

import comunications.Controller2;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;
import view.MainFrame;

import java.net.*;
import java.util.Enumeration;

public class ControllerMain {

    // IPs de los peers
    private static final String IP_PEER_A = "10.162.201.235";
    private static final String IP_PEER_B = "10.162.201.74";

    // Puertos de comunicación
    private static final int PRIMARY_PORT   = 5000;
    private static final int SECONDARY_PORT = 5001;

    private final GameController gameController;
    private final Controller2 networkController;

    public ControllerMain() {

        String localIp = detectLocalIp();

        String peerId;
        String remoteIp;

        if (localIp != null && localIp.equals(IP_PEER_A)) {
            peerId = "A";
            remoteIp = IP_PEER_B;

        } else if (localIp != null && localIp.equals(IP_PEER_B)) {
            peerId = "B";
            remoteIp = IP_PEER_A;

        } else {
            peerId = "A";
            remoteIp = "localhost";
            System.out.println("[ControllerMain] MODO LOCAL: usando localhost");
        }

        System.out.println(
                "[ControllerMain] Local IP: " + localIp +
                        " | Peer: " + peerId +
                        " | Remote IP: " + remoteIp
        );

        //  UI
        MainFrame frame = new MainFrame(
                "Bolas P2P - Peer " + peerId + " (port " + PRIMARY_PORT + ")",
                720,
                420
        );
        frame.setVisible(true);

        // Lógica de juego
        this.gameController = new GameController(frame, peerId, this);

        // Comunicaciones P2P
        this.networkController =
                new Controller2(this, remoteIp, PRIMARY_PORT, SECONDARY_PORT);

        //  Estado inicial
        gameController.spawnLocalBall();
        gameController.spawnLocalBall();
    }

    // Detección IP local
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


    public void introducirBola(BolaDTO bolaDTO) {
        if (gameController == null) return;
        gameController.introducirBola(bolaDTO);
    }

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