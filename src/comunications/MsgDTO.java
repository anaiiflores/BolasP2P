package comunications;

import java.io.Serializable;

public class MsgDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int header;              // 0 bola, 1 ping, 2 pong, 3 sprite
    private final Serializable payload;    // model.dto.BolaDTO o model.dto.SpriteDTO o null

    public MsgDTO(int header, Serializable payload) {
        this.header = header;
        this.payload = payload;
    }

    public int getHeader() {
        return header;
    }

    public Serializable getPayload() {
        return payload;
    }
}