package view;

import javax.swing.*;
import java.awt.*;

/**
 * MainFrame (vista):
 * - Contiene el GameView y una barra superior simple.
 * - NO contiene lógica de red/juego.
 */
public class MainFrame extends JFrame {

    private final JButton btnSpawn = new JButton("LANZA");
    private final GameView gamePanel;

    public MainFrame(String title, int w, int h) {
        super(title);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        top.setBackground(new Color(255, 243, 181)); // 🌼 amarillo pastel

        btnSpawn.setFocusPainted(false);
        btnSpawn.setBackground(new Color(255, 214, 102));
        btnSpawn.setFont(btnSpawn.getFont().deriveFont(Font.BOLD, 14f));
        btnSpawn.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));

        top.add(btnSpawn);

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


}