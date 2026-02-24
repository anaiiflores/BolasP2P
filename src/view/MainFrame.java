package view;

import javax.swing.*;
import java.awt.*;

/**
 * view.MainFrame (vista):
 * - Contiene el GamePanel y un pequeño panel superior con estado.
 * - No contiene lógica de red/bolas (eso va en el controller).
 */

public class MainFrame extends JFrame {

    private final JLabel lblMode = new JLabel("Modo: -");
    private final JLabel lblConn = new JLabel("Conexión: -");
    private final JButton btnSpawn = new JButton("Spawn local");

    private final GameView gamePanel;

    public MainFrame(String title, int w, int h) {
        super(title);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Barra superior
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(lblMode);
        left.add(lblConn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnSpawn);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        // Panel principal
        gamePanel = new GameView(w, h);

        add(top, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public GameView getGamePanel() {
        return gamePanel;
    }

    public JButton getBtnSpawn() {
        return btnSpawn;
    }

    public void setModeText(String txt) {
        lblMode.setText("Modo: " + txt);
    }

    public void setConnText(String txt) {
        lblConn.setText("Conexión: " + txt);
    }
}