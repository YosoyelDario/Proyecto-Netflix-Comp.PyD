package compartido;
import java.io.Serializable;

public class Pelicula implements Serializable {
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