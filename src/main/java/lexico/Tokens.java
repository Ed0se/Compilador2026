package lexico;

public class Tokens {
    private final String tipo;
    private final String lexema;
    private final int linea;

    public Tokens(String tipo, String lexema, int linea) {
        this.tipo   = tipo;
        this.lexema = lexema;
        this.linea  = linea;
    }

    public String getTipo()   { return tipo;   }
    public String getLexema() { return lexema; }
    public int    getLinea()  { return linea;  }

    @Override
    public String toString() {
        return String.format("L%d | %-40s | %s", linea, tipo, lexema);
    }
}


