package model.dto;

import java.io.Serializable;

public class SpriteDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public final float posicionY;
    public final float velocidadX;
    public final float velocidadY;

    public SpriteDTO(float posicionY, float velocidadX, float velocidadY) {
        this.posicionY = posicionY;
        this.velocidadX = velocidadX;
        this.velocidadY = velocidadY;
    }
}