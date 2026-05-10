package compartido;
import java.util.*;


public class RepositorioPeliculas {
    // Simula la base de datos física
    private final Map<String, Pelicula> baseDeDatos = new HashMap<>();

    public RepositorioPeliculas() {
        // Carga inicial simulando lectura de disco/BD
        baseDeDatos.put("matrix", new Pelicula("Matrix", 10));
        baseDeDatos.put("inception", new Pelicula("Inception", 15));
        baseDeDatos.put("interstellar", new Pelicula("Interstellar", 7));
        baseDeDatos.put("avatar", new Pelicula("Avatar", 12));
        baseDeDatos.put("titanic", new Pelicula("Titanic", 20));
        baseDeDatos.put("gladiator", new Pelicula("Gladiator", 18));
        baseDeDatos.put("avengers", new Pelicula("Avengers", 25));
        baseDeDatos.put("joker", new Pelicula("Joker", 8));
        baseDeDatos.put("parasite", new Pelicula("Parasite", 9));
        baseDeDatos.put("godfather", new Pelicula("Godfather", 22));
        baseDeDatos.put("pulpfiction", new Pelicula("Pulp Fiction", 14));
        baseDeDatos.put("shawshank", new Pelicula("Shawshank Redemption", 16));
        baseDeDatos.put("darkknight", new Pelicula("Dark Knight", 19));
        baseDeDatos.put("fightclub", new Pelicula("Fight Club", 13));
        baseDeDatos.put("forrestgump", new Pelicula("Forrest Gump", 17));
        baseDeDatos.put("incredibles", new Pelicula("Incredibles", 11));
        baseDeDatos.put("up", new Pelicula("Up", 9));
        baseDeDatos.put("wall-e", new Pelicula("Wall-E", 26));
        baseDeDatos.put("oppenheimer", new Pelicula("Oppenheimer", 21));
        baseDeDatos.put("dune", new Pelicula("Dune", 24));
        baseDeDatos.put("blackpanther", new Pelicula("Black Panther", 23));
        baseDeDatos.put("theflash", new Pelicula("The Flash", 27)); 
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