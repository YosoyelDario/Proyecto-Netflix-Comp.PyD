package compartido; 
import java.util.*;
/**
 * Capa de Almacenamiento independiente para Usuarios
 */
public class RepositorioUsuarios {
    private final Map<String, String> credenciales = new HashMap<>();
    private final Map<String, Usuario> perfiles = new HashMap<>();

    public RepositorioUsuarios() {
        // Simulando datos de inicio de sesión y perfiles
        credenciales.put("vicente", "1234");
        perfiles.put("vicente", new Usuario("Vicente", "Plan Premium - Chile", Arrays.asList("Matrix", "Interstellar")));
        perfiles.put("ana", new Usuario("Ana", "Plan Básico - Chile", Arrays.asList("Inception")));
        perfiles.put("luis", new Usuario("Luis", "Plan Estándar - Chile", Arrays.asList("Matrix", "Inception")));
        perfiles.put("araya", new Usuario("Araya", "Plan Premium - Chile", Arrays.asList("Interstellar")));
        perfiles.put("maria", new Usuario("Maria", "Plan Básico - Chile", Arrays.asList("Matrix")));
        perfiles.put("jose", new Usuario("Jose", "Plan Estándar - Chile", Arrays.asList("Inception", "Interstellar"))) ;
    }

    public synchronized boolean validarLogin(String usuario, String password) {
        String pwd = credenciales.get(usuario.toLowerCase());
        return pwd != null && pwd.equals(password);
    }

    public synchronized Usuario getPerfil(String user) {
        return perfiles.get(user.toLowerCase());
    }
}