package compartido;

import java.io.Serializable;

public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    public String username;
    public String password;
    public String plan;
    public boolean suscripcionActiva;

    public Usuario(String username, String password, String plan, boolean suscripcionActiva) {
        this.username = username;
        this.password = password;
        this.plan = plan;
        this.suscripcionActiva = suscripcionActiva;
    }

    @Override
    public String toString() {
        String estado = suscripcionActiva ? "Activa" : "Inactiva";
        return "Usuario: " + username + " | Plan: " + plan + " | Suscripción: " + estado;
    }
}
