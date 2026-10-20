package lexico;

public class ErroresLex {

    private final String token;
    private final String descripcion;
    private final String lexema;
    private final String tipoError;
    private final int linea;

    public ErroresLex(String token, String descripcion,
            String lexema, String tipoError, int linea) {
        this.token = token;
        this.descripcion = descripcion;
        this.lexema = lexema;
        this.tipoError = tipoError;
        this.linea = linea;
    }

    public String getToken() {
        return token;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLexema() {
        return lexema;
    }

    public String getTipoError() {
        return tipoError;
    }

    public int getLinea() {
        return linea;
    }
}


