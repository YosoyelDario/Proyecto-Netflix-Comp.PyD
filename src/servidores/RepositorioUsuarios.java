package servidores;

import compartido.Usuario;
import java.util.HashMap;
import java.util.Map;

public class RepositorioUsuarios {
    private final Map<String, Usuario> usuarios = new HashMap<>();

    public RepositorioUsuarios() {
        usuarios.put("vicente", new Usuario("vicente", "1234", "Plan Premium - Chile", true));
        usuarios.put("ana", new Usuario("ana", "abcd", "Plan Básico - Chile", true));
        usuarios.put("moroso", new Usuario("moroso", "1111", "Plan Premium - Chile", false));
    }

    public synchronized Usuario autenticar(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        Usuario usuario = usuarios.get(username.toLowerCase());

        if (usuario == null) {
            return null;
        }

        if (!usuario.password.equals(password)) {
            return null;
        }

        return usuario;
    }
}