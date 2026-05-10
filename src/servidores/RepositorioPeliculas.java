package servidores;

import compartido.Pelicula;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositorioPeliculas {
    private final Map<String, Pelicula> peliculas = new HashMap<>();
    private final Map<String, String> usuarios = new HashMap<>();

    public RepositorioPeliculas() {
        peliculas.put("matrix", new Pelicula("Matrix", 10));
        peliculas.put("inception", new Pelicula("Inception", 15));
        peliculas.put("interstellar", new Pelicula("Interstellar", 7));

        usuarios.put("vicente", "Plan Premium - Chile");
    }

    public synchronized List<Pelicula> getLista() {
        return new ArrayList<>(peliculas.values());
    }

    public synchronized Pelicula buscar(String titulo) {
        if (titulo == null) {
            return null;
        }

        return peliculas.get(titulo.toLowerCase());
    }

    public synchronized String getPerfil(String usuario) {
        if (usuario == null) {
            return "Usuario inexistente";
        }

        return usuarios.getOrDefault(usuario.toLowerCase(), "Usuario inexistente");
    }
}