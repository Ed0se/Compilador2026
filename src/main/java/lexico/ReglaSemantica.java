package lexico;

public class ReglaSemantica {
    private final int linea;
    private final int regla;
    private final String clave;
    private final String descripcion;
    private final String situacion;
    private final String lexema;
    private final int ambito;

    public ReglaSemantica(int linea, int regla, String clave, String descripcion, String situacion, String lexema,
            int ambito) {
        this.linea = linea;
        this.regla = regla;
        this.clave = clave;
        this.descripcion = descripcion;
        this.situacion = situacion;
        this.lexema = lexema;
        this.ambito = ambito;
    }

    public int getLinea() {
        return linea;
    }

    public int getRegla() {
        return regla;
    }

    public String getClave() {
        return clave;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getSituacion() {
        return situacion;
    }

    public String getLexema() {
        return lexema;
    }

    public int getAmbito() {
        return ambito;
    }

    public String toTableLine() {
        return String.format("%-6d | %-5d | %-65s | %-9s | %-15s | %d",
                linea, regla, descripcion, situacion, lexema, ambito);
    }

    @Override
    public String toString() {
        return toTableLine();
    }
}
