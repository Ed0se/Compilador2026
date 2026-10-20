package lexico;

public class ConteoAmbito {
    private int idAmbito;
    private int contBin;
    private int contDec;
    private int contOct;
    private int contHex;
    private int contReal;
    private int contExp;
    private int contChar;
    private int contCadena;
    private int contBool;
    private int contErrores;

    public ConteoAmbito(int idAmbito) {
        this.idAmbito = idAmbito;
    }

    public int getIdAmbito() {
        return idAmbito;
    }

    public void setIdAmbito(int idAmbito) {
        this.idAmbito = idAmbito;
    }

    public int getContBin() {
        return contBin;
    }

    public void sumarBin() {
        this.contBin++;
    }

    public int getContDec() {
        return contDec;
    }

    public void sumarDec() {
        this.contDec++;
    }

    public int getContOct() {
        return contOct;
    }

    public void sumarOct() {
        this.contOct++;
    }

    public int getContHex() {
        return contHex;
    }

    public void sumarHex() {
        this.contHex++;
    }

    public int getContReal() {
        return contReal;
    }

    public void sumarReal() {
        this.contReal++;
    }

    public int getContExp() {
        return contExp;
    }

    public void sumarExp() {
        this.contExp++;
    }

    public int getContChar() {
        return contChar;
    }

    public void sumarChar() {
        this.contChar++;
    }

    public int getContCadena() {
        return contCadena;
    }

    public void sumarCadena() {
        this.contCadena++;
    }

    public int getContBool() {
        return contBool;
    }

    public void sumarBool() {
        this.contBool++;
    }

    public int getContErrores() {
        return contErrores;
    }

    public void sumarError() {
        this.contErrores++;
    }

    public void setContErrores(int errores) {
        this.contErrores = errores;
    }

    public int calcularTotal() {
        return contBin + contDec + contOct + contHex + contReal + contExp + contChar + contCadena + contBool
                + contErrores;
    }
}
