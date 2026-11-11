package lexico;

public class ZonaRegistro {
    private final int numLinea;
    private final String categoria;
    private final String estado;
    private final int tokenID;
    private final String indicadorSec;

    public ZonaRegistro(int numLinea, String categoria, String estado, int tokenID, String indicadorSec) {
        this.numLinea = numLinea;
        this.categoria = categoria;
        this.estado = estado;
        this.tokenID = tokenID;
        this.indicadorSec = indicadorSec;
    }

    public int getNumLinea() {
        return numLinea;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getEstado() {
        return estado;
    }

    public int getTokenID() {
        return tokenID;
    }

    public String getIndicadorSec() {
        return indicadorSec;
    }

    @Override
    public String toString() {
        return "Linea: " + numLinea + " | " + categoria + " | " + estado + " | Codigo: " + tokenID + " | Marcador: "
                + indicadorSec;
    }
}
