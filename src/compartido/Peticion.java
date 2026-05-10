package compartido;
import java.io.Serializable;

/**
 * Representa la solicitud del cliente.
 */
public class Peticion implements Serializable {
    private static final long serialVersionUID = 1L;
    public String comando;
    public String parametro;

    public Peticion(String comando, String parametro) {
        this.comando = comando;
        this.parametro = parametro;
    }
}







