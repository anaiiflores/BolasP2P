package controller;

import comunications.Controller2;
import model.dto.BolaDTO;
import model.dto.SpriteDTO;
import view.MainFrame;

import java.net.*;
import java.util.Enumeration;

public class ControllerMain {

    private final GameController controlador;
    private final Controller2 comunicaciones;

    // Tus IPs reales (las dos máquinas)
    private static final String IP_EQUIPO_1 = "10.162.201.235";
    private static final String IP_EQUIPO_2 = "10.162.201.74";

    private static final int MAIN_PORT = 5000;
    private static final int AUX_PORT  = 5001;

    public ControllerMain() {
        String miIP = obtenerMiIP();
        String ipRemota = obtenerIPRemota(miIP);
        String label = obtenerLabel(miIP);

        System.out.println("Mi IP: " + miIP + " | Soy: " + label + " | Remota: " + ipRemota);

        // 1) UI primero
        MainFrame frame = new MainFrame("Bolas P2P - Peer " + label + " (port " + MAIN_PORT + ")", 720, 420);
        frame.setVisible(true);

        // 2) Controller del juego ANTES de comunicaciones (para que no llegue red sin controlador)
        this.controlador = new GameController(frame, label, this);

        // 3) Arrancar comunicaciones (ChannelReader, sockets, etc.)
        this.comunicaciones = new Controller2(this, ipRemota, MAIN_PORT, AUX_PORT);

        // 4) movimiento inicial
        controlador.spawnLocalBall();
        controlador.spawnLocalBall();
    }

    /** Decide la IP remota en P2P como tu MasterController */
    private String obtenerIPRemota(String miIP) {
        if (miIP == null) {
            System.out.println("MODO DESARROLLO: no pude detectar mi IP, usando localhost");
            return "localhost";
        }

        if (miIP.equals(IP_EQUIPO_1)) {
            return IP_EQUIPO_2;
        } else if (miIP.equals(IP_EQUIPO_2)) {
            return IP_EQUIPO_1;
        } else {
            System.out.println("MODO DESARROLLO: IP no reconocida (" + miIP + "), usando localhost");
            return "localhost";
        }
    }

    /** Solo para mostrar bonito en el título / logs */
    private String obtenerLabel(String miIP) {
        if (miIP == null) return "A";
        if (miIP.equals(IP_EQUIPO_1)) return "A";
        if (miIP.equals(IP_EQUIPO_2)) return "B";
        return "A";
    }

    /**
     * Detecta mi IP IPv4 "privada".
     * Igual que el ejemplo bueno, pero sin fijar una subred concreta.
     * Si quieres ser como el ejemplo bueno al 100%, filtra por el prefijo real de tu red (p.ej. "10.162.")
     */
    private String obtenerMiIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Saltar interfaces inactivas/loopback (puedes añadir iface.isVirtual() si quieres)
                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();

                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();

                        // IPs privadas típicas
                        if (ip.startsWith("10.") || ip.startsWith("172.16.") || ip.startsWith("192.168.")) {
                            return ip;
                        }
                    }
                }
            }

            System.out.println("No se encontró IP privada típica, usando getLocalHost()");
            return InetAddress.getLocalHost().getHostAddress();

        } catch (SocketException e) {
            System.err.println("Error obteniendo interfaces de red: " + e.getMessage());
            return "localhost";
        } catch (UnknownHostException e) {
            System.err.println("Error obteniendo host local: " + e.getMessage());
            return "localhost";
        }
    }

    // ===== puente =====

    public void introducirBola(BolaDTO bolaDTO) {
        if (controlador == null) return;
        controlador.introducirBola(bolaDTO);
    }

    public void lanzarBola(BolaDTO bolaDTO) {
        if (comunicaciones == null) return;
        comunicaciones.lanzarBola(bolaDTO);
    }

    public void introducirSprite(SpriteDTO dto) {
        if (controlador == null) return;
        controlador.introducirSprite(dto);
    }

    public void lanzarSprite(SpriteDTO dto) {
        if (comunicaciones == null) return;
        comunicaciones.lanzarSprite(dto);
    }
}