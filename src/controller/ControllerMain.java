package controller;

import model.dto.BolaDTO;
import model.dto.SpriteDTO;

import comunications.Controller2;
import view.MainFrame;

import java.net.*;
import java.util.Enumeration;


public class ControllerMain {

    private static final String IP_EQUIPO_1 = "192.168.0.114";
    private static final String IP_EQUIPO_2 = "192.168.0.113";

    private static final int MAIN_PORT = 5000;
    private static final int AUX_PORT  = 5001;

    private final GameController controlador;
    private final Controller2 comunicaciones;

    public ControllerMain() {

        String miIP = obtenerMiIP();
        String ipRemota = obtenerIPRemota(miIP);

        if (ipRemota == null) {
            System.out.println("MODO LOCAL: usando localhost");
            ipRemota = "localhost";
        }

        System.out.println("Mi IP: " + miIP + " | IP remota: " + ipRemota);

        this.comunicaciones = new Controller2(this, ipRemota, MAIN_PORT, AUX_PORT);

        // 2️⃣ Esperar a saber en qué puerto escucha
        int listenPort = comunicaciones.getActualListenPort();

        // 3️⃣ Decidir label
        String label = (listenPort == MAIN_PORT) ? "A" : "B";

        // 4️⃣ Crear UI
        MainFrame frame = new MainFrame(
                "Bolas P2P - Peer " + label + " (listen " + listenPort + ")",
                720,
                420
        );
        frame.setVisible(true);

        // 5️⃣ Crear controller.GameController
        this.controlador = new GameController(frame, label, this);

        // 6️⃣ Movimiento inicial
        controlador.spawnLocalBall();
        controlador.spawnLocalBall();
    }


    private String obtenerIPRemota(String miIP) {
        if (miIP == null) return null;
        if (miIP.equals(IP_EQUIPO_1)) return IP_EQUIPO_2;
        if (miIP.equals(IP_EQUIPO_2)) return IP_EQUIPO_1;
        return null;
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

    /** Llamado por comunications.channel.Channel → ServerConnector */
    public void introducirBola(BolaDTO bolaDTO) {
        controlador.introducirBola(bolaDTO);
    }

    /** Llamado por controller.GameController cuando la bola sale */
    public void lanzarBola(BolaDTO bolaDTO) {
        comunicaciones.lanzarBola(bolaDTO);
    }
    public void introducirSprite(SpriteDTO dto) { controlador.introducirSprite(dto); }
    public void lanzarSprite(SpriteDTO dto) { comunicaciones.lanzarSprite(dto); }
}