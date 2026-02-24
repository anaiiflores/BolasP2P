package view;

import model.Ball;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

public class GameView extends JPanel {

    private List<Ball> balls = Collections.emptyList();

    private java.awt.image.BufferedImage spriteFrame = null;
    private int spriteX = 0;
    private int spriteY = 0;

    public GameView(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(255, 236, 245)); // rosa pastel suave
        setDoubleBuffered(true);
    }

    public void setBalls(List<Ball> balls) {
        this.balls = (balls != null) ? balls : Collections.emptyList();
    }

    public void setSprite(java.awt.image.BufferedImage frame, int x, int y) {
        this.spriteFrame = frame;
        this.spriteX = x;
        this.spriteY = y;
    }

    public Rectangle2D.Float getWorldBounds() {
        return new Rectangle2D.Float(0, 0, getWidth(), getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // borde suave
            g2.setColor(new Color(0, 0, 0, 25));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            Color bolaColor = new Color(231, 111, 170);
            g2.setColor(bolaColor);

            for (Ball b : balls) {
                float r = b.getR();
                float x = b.getX() - r;
                float y = b.getY() - r;

                Shape circle = new Ellipse2D.Float(x, y, r * 2f, r * 2f);
                g2.fill(circle);

                g2.setColor(new Color(0, 0, 0, 40));
                g2.draw(circle);
                g2.setColor(bolaColor);
            }

            if (spriteFrame != null) {
                g2.drawImage(spriteFrame, spriteX, spriteY, null);
            }



        } finally {
            g2.dispose();
        }
    }
}