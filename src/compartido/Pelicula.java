package compartido;

import java.io.Serializable;

public class Pelicula implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String titulo;
    public int fragmentos;
    public String sinopsis;

    public Pelicula(int id, String titulo, int fragmentos, String sinopsis) {
        this.id = id;
        this.titulo = titulo;
        this.fragmentos = fragmentos;
        this.sinopsis = sinopsis;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titulo + " (" + (fragmentos * 2) + "s) - " + sinopsis;
    }

    public String toStringCorto() {
        return "[" + id + "] " + titulo + " (" + (fragmentos * 2) + " segundos)";
    }
}
