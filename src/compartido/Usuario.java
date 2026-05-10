package compartido;
import java.io.Serializable;
import java.util.List;

/**
 * Entidad que representa a un usuario, su plan y su lista de películas.
 */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    public String nombre;
    public String plan;
    public List<String> miLista;

    public Usuario(String nombre, String plan, List<String> miLista) {
        this.nombre = nombre;
        this.plan = plan;
        this.miLista = miLista;
    }

    @Override
    public String toString() {
        return "Usuario: " + nombre + " | Plan: " + plan + " | Mi Lista: " + miLista;
    }
}