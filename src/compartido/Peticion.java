package compartido;

import java.io.Serializable;

public class Peticion implements Serializable {
    private static final long serialVersionUID = 1L;

    public String comando;
    public String parametro;
    public String ipOrigen; // Atributo para simular X-Forwarded-For

    public Peticion(String comando, String parametro) {
        this.comando = comando;
        this.parametro = parametro;
        this.ipOrigen = "Desconocida";
    }
}
