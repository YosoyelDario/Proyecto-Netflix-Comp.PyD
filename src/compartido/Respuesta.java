package compartido;

import java.io.Serializable;

public class Respuesta implements Serializable {
    private static final long serialVersionUID = 1L;

    public String codigo;
    public Object payload;

    public Respuesta(String codigo, Object payload) {
        this.codigo = codigo;
        this.payload = payload;
    }
}
