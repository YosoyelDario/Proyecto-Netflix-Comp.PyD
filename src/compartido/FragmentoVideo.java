package compartido;

import java.io.Serializable;

/**
 * Representa un fragmento (chunk) de video transmitido por UDP.
 *
 * Usa serialización (marshalling) para enviar datos estructurados
 * complejos entre el Servidor de Streaming y el Cliente,
 * en lugar de simples Strings.
 *
 * Incluye número de secuencia para detección de pérdida de paquetes.
 */
public class FragmentoVideo implements Serializable {
    private static final long serialVersionUID = 1L;

    public int numeroSecuencia;
    public int totalFragmentos;
    public String tituloPelicula;
    public byte[] datos;
    public int tamanioDatos;
    public boolean esFin;

    /**
     * Constructor para un fragmento de video normal.
     */
    public FragmentoVideo(int numeroSecuencia, int totalFragmentos, String tituloPelicula, byte[] datos) {
        this.numeroSecuencia = numeroSecuencia;
        this.totalFragmentos = totalFragmentos;
        this.tituloPelicula = tituloPelicula;
        this.datos = datos;
        this.tamanioDatos = datos != null ? datos.length : 0;
        this.esFin = false;
    }

    /**
     * Constructor para la señal de fin de streaming.
     */
    public FragmentoVideo(String tituloPelicula) {
        this.numeroSecuencia = -1;
        this.totalFragmentos = 0;
        this.tituloPelicula = tituloPelicula;
        this.datos = null;
        this.tamanioDatos = 0;
        this.esFin = true;
    }

    @Override
    public String toString() {
        if (esFin) {
            return "[FIN] " + tituloPelicula;
        }

        return "[FRAG " + numeroSecuencia + "/" + totalFragmentos + "] " +
               tituloPelicula + " (" + tamanioDatos + " bytes)";
    }
}