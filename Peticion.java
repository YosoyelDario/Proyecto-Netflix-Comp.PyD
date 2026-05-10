import java.io.Serializable;
import java.util.List;

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

/**
 * Representa la respuesta del servidor. 
 * No es public para que pueda coexistir en Peticion.java.
 */
class Respuesta implements Serializable {
    private static final long serialVersionUID = 1L;
    public String codigo;
    public Object payload;

    public Respuesta(String codigo, Object payload) {
        this.codigo = codigo;
        this.payload = payload;
    }
}

/**
 * Entidad de datos del catálogo.
 */
class Pelicula implements Serializable {
    private static final long serialVersionUID = 1L;
    public String titulo;
    public int fragmentos;

    public Pelicula(String titulo, int fragmentos) {
        this.titulo = titulo;
        this.fragmentos = fragmentos;
    }

    @Override
    public String toString() {
        return titulo + " (" + (fragmentos * 2) + " segundos)";
    }
}