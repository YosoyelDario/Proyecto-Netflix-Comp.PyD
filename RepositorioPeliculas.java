import java.util.*;

public class RepositorioPeliculas {
    // Simula la base de datos física
    private final Map<String, Pelicula> baseDeDatos = new HashMap<>();

    public RepositorioPeliculas() {
        // Carga inicial simulando lectura de disco/BD
        baseDeDatos.put("matrix", new Pelicula("Matrix", 10));
        baseDeDatos.put("inception", new Pelicula("Inception", 15));
        baseDeDatos.put("interstellar", new Pelicula("Interstellar", 7));
    }

    public synchronized List<Pelicula> obtenerTodas() {
        return new ArrayList<>(baseDeDatos.values());
    }

    public synchronized boolean existe(String titulo) {
        return baseDeDatos.containsKey(titulo.toLowerCase());
    }
    
    public synchronized Pelicula buscar(String titulo) {
        return baseDeDatos.get(titulo.toLowerCase());
    }
}