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
        perfiles.put("vicente", new Usuario("Vicente", "1234", "Plan Premium - Chile", true));
        perfiles.put("ana", new Usuario("Ana", "5678", "Plan Básico - Chile", true));
        perfiles.put("luis", new Usuario("Luis", "9012", "Plan Estándar - Chile", true));
        perfiles.put("araya", new Usuario("Araya", "3456", "Plan Premium - Chile", true));
        perfiles.put("maria", new Usuario("Maria", "7890", "Plan Básico - Chile", true));
        perfiles.put("jose", new Usuario("Jose", "2345", "Plan Estándar - Chile", true));
    }

    public synchronized Usuario autenticar(String user, String password) {
        Usuario u = perfiles.get(user.toLowerCase());
        String pwd = credenciales.get(user.toLowerCase());
        if(u != null && pwd != null && pwd.equals(password)) { // verificar credenciales
            return u;
        }
        return null;
    }

    public synchronized Usuario getPerfil(String user) {
        return perfiles.get(user.toLowerCase());
    }
}

