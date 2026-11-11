package lexico;

public class ReglaSemantica {
    private final int linea;
    private final int regla;
    private final String clave;
    private final String funcion;
    private final String topePila;
    private final String valorReal;
    private final String descripcion;
    private final String situacion;
    private final String lexema;
    private final int ambito;

    public ReglaSemantica(int linea, int regla, String clave, String topePila, String valorReal, String descripcion,
            String situacion, String lexema, int ambito) {
        this(linea, regla, clave, "-", topePila, valorReal, descripcion, situacion, lexema, ambito);
    }

    public ReglaSemantica(int linea, int regla, String clave, String funcion, String topePila, String valorReal,
            String descripcion, String situacion, String lexema, int ambito) {
        this.linea = linea;
        this.regla = regla;
        this.clave = clave;
        this.funcion = (funcion != null && !funcion.isEmpty()) ? funcion : "-";
        this.topePila = topePila;
        this.valorReal = valorReal;
        this.descripcion = descripcion;
        this.situacion = situacion;
        this.lexema = lexema;
        this.ambito = ambito;
    }

    public String getFuncion() {
        return funcion;
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

    public String getTopePila() {
        return topePila;
    }

    public String getValorReal() {
        return valorReal;
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

    @Override
    public String toString() {
        return String.format("%s | %s | %s | L%d | %s | Amb%d",
                clave, topePila, valorReal, linea, situacion, ambito);
    }
}
