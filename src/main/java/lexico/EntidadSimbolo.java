package lexico;

public class EntidadSimbolo {
    private String identificador;
    private String tipoDato;
    private String clasificacion;
    private int nivelAmbito;
    private String tipoArreglo;
    private String dimensionArreglo;
    private Integer numeroParametros;
    private String tipoParametros;

    public EntidadSimbolo() {
    }

    public EntidadSimbolo(String identificador, String tipoDato, String clasificacion, int nivelAmbito,
            String tipoArreglo, String dimensionArreglo, Integer numeroParametros, String tipoParametros) {
        this.identificador = identificador;
        this.tipoDato = tipoDato;
        this.clasificacion = clasificacion;
        this.nivelAmbito = nivelAmbito;
        this.tipoArreglo = (tipoArreglo != null && !tipoArreglo.isEmpty()) ? tipoArreglo : "-";
        this.dimensionArreglo = (dimensionArreglo != null && !dimensionArreglo.isEmpty()) ? dimensionArreglo : "-";
        this.numeroParametros = numeroParametros;
        this.tipoParametros = (tipoParametros != null && !tipoParametros.isEmpty()) ? tipoParametros : "-";
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }

    public int getNivelAmbito() {
        return nivelAmbito;
    }

    public void setNivelAmbito(int nivelAmbito) {
        this.nivelAmbito = nivelAmbito;
    }

    public String getTipoArreglo() {
        return tipoArreglo;
    }

    public void setTipoArreglo(String tipoArreglo) {
        this.tipoArreglo = tipoArreglo;
    }

    public String getDimensionArreglo() {
        return dimensionArreglo;
    }

    public void setDimensionArreglo(String dimensionArreglo) {
        this.dimensionArreglo = dimensionArreglo;
    }

    public Integer getNumeroParametros() {
        return numeroParametros;
    }

    public void setNumeroParametros(Integer numeroParametros) {
        this.numeroParametros = numeroParametros;
    }

    public String getTipoParametros() {
        return tipoParametros;
    }

    public void setTipoParametros(String tipoParametros) {
        this.tipoParametros = tipoParametros;
    }
}
