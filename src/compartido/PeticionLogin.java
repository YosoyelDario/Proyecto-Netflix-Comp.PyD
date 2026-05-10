package compartido;
import java.io.Serializable;
/**
 * Representa una solicitud de inicio de sesión.
 */
public class PeticionLogin implements Serializable {
    private static final long serialVersionUID = 1L;
    public String usuario;
    public String password;

    public PeticionLogin(String usuario, String password) {
        this.usuario = usuario;
        this.password = password;
    }
}