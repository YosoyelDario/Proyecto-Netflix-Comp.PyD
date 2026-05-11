package servidores;

import compartido.Pelicula;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RepositorioPeliculas {
    private final Map<Integer, Pelicula> peliculas = new ConcurrentHashMap<>();
    private final String archivoRuta;

    public RepositorioPeliculas(String archivoRuta) {
        this.archivoRuta = archivoRuta;
        cargarDesdeArchivo();
    }

    /**
     * Carga películas desde el archivo de texto (BD Metadatos).
     * Formato: id|titulo|fragmentos|sinopsis
     */
    private void cargarDesdeArchivo() {
        try (BufferedReader br = new BufferedReader(new FileReader(archivoRuta))) {
            String linea;

            while ((linea = br.readLine()) != null) {
                linea = linea.trim();

                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                String[] partes = linea.split("\\|");

                if (partes.length < 4) {
                    System.err.println("[BD-PELICULAS] Línea mal formada: " + linea);
                    continue;
                }

                int id = Integer.parseInt(partes[0].trim());
                String titulo = partes[1].trim();
                int fragmentos = Integer.parseInt(partes[2].trim());
                String sinopsis = partes[3].trim();

                peliculas.put(id, new Pelicula(id, titulo, fragmentos, sinopsis));
            }

            System.out.println("[BD-PELICULAS] " + peliculas.size() + " películas cargadas desde: " + archivoRuta);

        } catch (IOException e) {
            System.err.println("[BD-PELICULAS] Error al leer archivo: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("[BD-PELICULAS] Error de formato numérico: " + e.getMessage());
        }
    }

    /**
     * Devuelve la lista completa del catálogo (Fase 2 - Carga del Catálogo).
     */
    public synchronized List<Pelicula> getLista() {
        return new ArrayList<>(peliculas.values());
    }

    /**
     * Busca una película por título exacto (para ELEGIR_PELICULA / streaming).
     */
    public synchronized Pelicula buscarExacto(String titulo) {
        if (titulo == null) {
            return null;
        }

        for (Pelicula p : peliculas.values()) {
            if (p.titulo.equalsIgnoreCase(titulo)) {
                return p;
            }
        }

        return null;
    }

    /**
     * Búsqueda parcial tipo LIKE "%query%" (Fase 3 - Búsqueda de Título).
     * Retorna lista de coincidencias.
     */
    public synchronized List<Pelicula> buscarPorQuery(String query) {
        List<Pelicula> resultados = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return resultados;
        }

        String queryLower = query.toLowerCase();

        for (Pelicula p : peliculas.values()) {
            if (p.titulo.toLowerCase().contains(queryLower)) {
                resultados.add(p);
            }
        }

        return resultados;
    }

    /**
     * Busca una película por su ID numérico.
     */
    public synchronized Pelicula buscarPorId(int id) {
        return peliculas.get(id);
    }
}
