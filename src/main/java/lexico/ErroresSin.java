package lexico;

public class ErroresSin {

    private final String codigo;
    private final String noTerminal;  
    private final String terminal;    
    private final String descripcion;
    private final int linea;

    public ErroresSin(String codigo, String noTerminal, String terminal, String descripcion, int linea) {
        this.codigo      = codigo;
        this.noTerminal  = noTerminal;
        this.terminal    = terminal;
        this.descripcion = descripcion;
        this.linea       = linea;
    }

    public String getCodigo()      { return codigo;      }
    public String getNoTerminal()  { return noTerminal;  }
    public String getTerminal()    { return terminal;    }
    public String getDescripcion() { return descripcion; }
    public int    getLinea()       { return linea;       }

    @Override
    public String toString() {
        return "[" + codigo + " - Línea " + linea + "] " + descripcion
             + " (NT=" + noTerminal + ", terminal='" + terminal + "')";
    }
}


