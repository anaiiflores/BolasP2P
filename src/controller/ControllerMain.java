package controller;

import comunications.Controller2;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;
import view.MainFrame;

import java.net.*;
import java.util.Enumeration;

public class ControllerMain {

    private static final String IP_EQUIPO_1 = "192.168.1.137";
    private static final String IP_EQUIPO_2 = "192.168.1.217";

    private static final int MAIN_PORT = 5000;
    private static final int AUX_PORT  = 5001;

    private final GameController controlador;
    private final Controller2 comunicaciones;

    public ControllerMain() {

        String miIP = obtenerMiIP();

        String label;
        String ipRemota;

        if (miIP != null && miIP.equals(IP_EQUIPO_1)) {
            label = "A";
            ipRemota = IP_EQUIPO_2;
        } else if (miIP != null && miIP.equals(IP_EQUIPO_2)) {
            label = "B";
            ipRemota = IP_EQUIPO_1;
        } else {
            label = "A";
            ipRemota = "localhost";
            System.out.println("MODO LOCAL: usando localhost");
        }

        System.out.println("Mi IP: " + miIP + " | Soy: " + label + " | Remota: " + ipRemota);

        // 1) UI primero
        MainFrame frame = new MainFrame("Bolas P2P - Peer " + label + " (port " + MAIN_PORT + ")", 720, 420);
        frame.setVisible(true);

        // 2) Controller del juego listo ANTES de que llegue nada por red
        this.controlador = new GameController(frame, label, this);

        // 3) Ahora sí: comunicaciones (arranca ChannelReader, etc.)
        this.comunicaciones = new Controller2(this, ipRemota, MAIN_PORT, AUX_PORT);

        // movimiento inicial
        controlador.spawnLocalBall();
        controlador.spawnLocalBall();
    }

    private String obtenerMiIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("172.16.") || ip.startsWith("10.")) {
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

    // ===== puente =====

    public void introducirBola(BolaDTO bolaDTO) {
        if (controlador == null) return;
        controlador.introducirBola(bolaDTO);
    }

    public void lanzarBola(BolaDTO bolaDTO) {
        comunicaciones.lanzarBola(bolaDTO);
    }

    public void introducirSprite(SpriteDTO dto) {
        if (controlador == null) return;

        controlador.introducirSprite(dto);
    }

    public void lanzarSprite(SpriteDTO dto) {
        comunicaciones.lanzarSprite(dto);
    }
}