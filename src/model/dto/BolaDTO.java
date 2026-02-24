package model.dto;

import java.io.Serializable;


public class BolaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public final float posicionY;
    public final float radio;
    public final float velocidadX;
    public final float velocidadY;

    public BolaDTO(float posicionY, float radio, float velocidadX, float velocidadY) {
        this.posicionY = posicionY;
        this.radio = radio;
        this.velocidadX = velocidadX;
        this.velocidadY = velocidadY;
    }
}


