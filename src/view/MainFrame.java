package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class MainFrame extends JFrame {

    private final JButton btnSpawn = new JButton("LANZA");
    private final GameView gamePanel;

    public MainFrame(String title, int w, int h) {
        super(title);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Oculta la ventana pero deja la app viva (threads + reconexión)
                setVisible(false);
            }
        });

        setLayout(new BorderLayout());

        // ===== Barra superior =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        top.setBackground(new Color(255, 243, 181)); // 🌼 amarillo pastel

        btnSpawn.setFocusPainted(false);
        btnSpawn.setBackground(new Color(255, 214, 102));
        btnSpawn.setFont(btnSpawn.getFont().deriveFont(Font.BOLD, 14f));
        btnSpawn.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));

        top.add(btnSpawn);

        // ===== Panel principal =====
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

    public void setConnText(String txt) { }
}