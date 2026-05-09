package shared;
import java.io.Serializable;

public class ConfigObj implements Serializable{
    private static final long serialVersionUID = 1L; //Se usa para evitar errores de version
    public String ipVideo;
    public int puertoVideo;
    public String ipSub;
    public int puertoSub;

    public ConfigObj(String iv, int pv, String is, int ps)
    {
        this.ipVideo = iv;
        this.puertoVideo = pv;
        this.ipSub = is;
        this.puertoSub = ps;
    }
}