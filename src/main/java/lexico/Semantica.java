package lexico;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Semantica {

    public static class ResultadoSemantica {
        public String tipo;
        public boolean esError;
        public String mensajeError;

        public ResultadoSemantica(String tipo, boolean esError, String mensajeError) {
            this.tipo = tipo;
            this.esError = esError;
            this.mensajeError = mensajeError;
        }
    }

    public static class ConteoTemporales {
        public int linea;
        public int tBin = 0;
        public int tDec = 0;
        public int tOct = 0;
        public int tHex = 0;
        public int tReal = 0;
        public int tExp = 0;
        public int tCadena = 0;
        public int tBoolean = 0;
        public int tVariant = 0;
        public int asignaciones = 0;
        public String detalleAsignacion = "";
        public int errores = 0;

        public ConteoTemporales(int linea) {
            this.linea = linea;
        }
    }

    private List<ConteoTemporales> conteos = new ArrayList<>();
    private List<ErroresSin> erroresSemanticos = new ArrayList<>();
    private Map<Integer, ConteoTemporales> conteosPorLinea = new HashMap<>();
    private List<ReglaSemantica> reglasSemanticas = new ArrayList<>();

    public Semantica() {
    }

    public List<ReglaSemantica> getReglasSemanticas() {
        return reglasSemanticas;
    }

    public List<ErroresSin> getErroresSemanticos() {
        return erroresSemanticos;
    }

    public List<ConteoTemporales> getConteos() {
        List<ConteoTemporales> res = new ArrayList<>(conteosPorLinea.values());
        res.sort(Comparator.comparingInt(c -> c.linea));
        return res;
    }

    private ConteoTemporales getConteo(int linea) {
        return conteosPorLinea.computeIfAbsent(linea, ConteoTemporales::new);
    }

    private String normalizeType(String t) {
        if (t == null)
            return "variant";
        t = t.toLowerCase();
        if (t.equals("bin") || t.equals("binario"))
            return "bin";
        if (t.equals("dec") || t.equals("decimal"))
            return "dec";
        if (t.equals("oct") || t.equals("octal"))
            return "oct";
        if (t.equals("hex") || t.equals("hexa") || t.equals("hexadecimal"))
            return "hex";
        if (t.equals("real"))
            return "real";
        if (t.equals("exp") || t.equals("exponencial") || t.equals("expo"))
            return "exp";
        if (t.equals("cadena") || t.equals("string"))
            return "cadena";
        if (t.equals("bool") || t.equals("boolean") || t.equals("logica"))
            return "boolean";
        if (t.equals("variant"))
            return "variant";
        return "variant";
    }

    private String formatTipo(String t) {
        if (t == null)
            return "Variant";
        t = t.toLowerCase();
        if (t.equals("bin") || t.equals("binario"))
            return "Bin";
        if (t.equals("dec") || t.equals("decimal"))
            return "Dec";
        if (t.equals("oct") || t.equals("octal"))
            return "Oct";
        if (t.equals("hex") || t.equals("hexa") || t.equals("hexadecimal"))
            return "Hex";
        if (t.equals("real"))
            return "Real";
        if (t.equals("exp") || t.equals("exponencial") || t.equals("expo"))
            return "Exp";
        if (t.equals("cadena") || t.equals("string"))
            return "Cadena";
        if (t.equals("boolean") || t.equals("bool") || t.equals("logica"))
            return "Logica";
        return "Variant";
    }

    public ResultadoSemantica checkOperacionBinaria(String tipoIzq, String op, String tipoDer, int linea, String lexIzq,
            String lexDer) {
        tipoIzq = normalizeType(tipoIzq);
        tipoDer = normalizeType(tipoDer);

        String tipoRes = ERR;

        if (op.equals("+")) {
            tipoRes = evaluarMatriz(MATRIZ_SUMA, tipoIzq, tipoDer);
        } else if (op.equals("-") || op.equals("*")) {
            tipoRes = evaluarMatriz(MATRIZ_RESTA_MULT, tipoIzq, tipoDer);
        } else if (op.equals("/")) {
            tipoRes = evaluarMatriz(MATRIZ_DIV, tipoIzq, tipoDer);
        } else if (op.equals("%") || op.equals(">>>") || op.equals(">>") || op.equals("<<") || op.equals("^")
                || op.equals("**")) {
            tipoRes = evaluarMatriz(MATRIZ_BITWISE, tipoIzq, tipoDer);
        } else if (op.equals("==") || op.equals("!=") || op.equals("===") || op.equals("!==")) {
            tipoRes = evaluarMatriz(MATRIZ_IGUALDAD, tipoIzq, tipoDer);
        } else if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
            tipoRes = evaluarMatriz(MATRIZ_RELACIONAL, tipoIzq, tipoDer);
        } else if (op.equals("&&") || op.equals("||") || op.equals("&") || op.equals("|")) {
            tipoRes = evaluarMatriz(MATRIZ_LOGICA, tipoIzq, tipoDer);
        } else {
            tipoRes = ERR;
        }

        ConteoTemporales conteo = getConteo(linea);

        if (tipoRes.equals("error")) {
            conteo.errores++;
            conteo.tVariant++;
            String desc = "Incompatibilidad entre operandos: " + lexIzq + " (" + tipoIzq + ") " + op + " " + lexDer
                    + " (" + tipoDer + ")";
            erroresSemanticos.add(new ErroresSin("903", "Semantica", op, desc, linea));
            return new ResultadoSemantica("variant", true, desc);
        } else {
            incrementarTemporal(conteo, tipoRes);
            return new ResultadoSemantica(tipoRes, false, "");
        }
    }

    public ResultadoSemantica checkAsignacion(String tipoVar, String tipoExpr, int linea, String lexVar) {
        tipoVar = normalizeType(tipoVar);
        tipoExpr = normalizeType(tipoExpr);

        ConteoTemporales conteo = getConteo(linea);
        conteo.asignaciones++;

        String tipoFormat = formatTipo(tipoExpr);
        if (conteo.detalleAsignacion.isEmpty()) {
            conteo.detalleAsignacion = lexVar + " -> " + tipoFormat;
        } else {
            conteo.detalleAsignacion += ", " + lexVar + " -> " + tipoFormat;
        }

        if (tipoExpr.equals("variant") || tipoVar.equals("variant")) {
            return new ResultadoSemantica("variant", false, ""); // Evita cascada
        }

        boolean valida = tipoVar.equals(tipoExpr);

        // Reglas de Upcasting Numérico
        if (!valida) {
            if (tipoVar.equals("real")) {
                valida = tipoExpr.equals("dec") || tipoExpr.equals("bin") || tipoExpr.equals("oct")
                        || tipoExpr.equals("hex");
            } else if (tipoVar.equals("exp")) {
                valida = tipoExpr.equals("real") || tipoExpr.equals("dec") || tipoExpr.equals("bin")
                        || tipoExpr.equals("oct") || tipoExpr.equals("hex");
            } else if (tipoVar.equals("dec")) {
                valida = tipoExpr.equals("bin") || tipoExpr.equals("oct") || tipoExpr.equals("hex");
            }
        }

        if (!valida) {
            conteo.errores++;
            String desc = "Incompatibilidad en la asignación: no se puede asignar " + tipoExpr + " a la variable '"
                    + lexVar + "' de tipo " + tipoVar;
            erroresSemanticos.add(new ErroresSin("904", "Semantica", "=", desc, linea));
            return new ResultadoSemantica("variant", true, desc);
        }
        return new ResultadoSemantica(tipoVar, false, "");
    }

    private void incrementarTemporal(ConteoTemporales conteo, String tipo) {
        switch (tipo) {
            case "bin":
                conteo.tBin++;
                break;
            case "dec":
                conteo.tDec++;
                break;
            case "oct":
                conteo.tOct++;
                break;
            case "hex":
                conteo.tHex++;
                break;
            case "real":
                conteo.tReal++;
                break;
            case "exp":
                conteo.tExp++;
                break;
            case "cadena":
                conteo.tCadena++;
                break;
            case "boolean":
                conteo.tBoolean++;
                break;
            case "variant":
                conteo.tVariant++;
                break;
        }
    }

    private static final String BIN = "bin";
    private static final String DEC = "dec";
    private static final String OCT = "oct";
    private static final String HEX = "hex";
    private static final String REAL = "real";
    private static final String EXP = "exp";
    private static final String CAD = "cadena";
    private static final String BOL = "boolean";
    private static final String VAR = "variant";
    private static final String ERR = "error";

    private int getIndex(String t) {
        switch (t) {
            case BIN:
                return 0;
            case DEC:
                return 1;
            case OCT:
                return 2;
            case HEX:
                return 3;
            case REAL:
                return 4;
            case EXP:
                return 5;
            case CAD:
                return 6;
            case BOL:
                return 7;
            case VAR:
                return 8;
            default:
                return 9;
        }
    }

    private static final String[][] MATRIZ_SUMA = {
            // BIN, DEC, OCT, HEX, REAL, EXP, CAD, BOL, VAR
            { BIN, ERR, ERR, ERR, ERR, ERR, CAD, ERR, VAR }, // BIN
            { ERR, DEC, ERR, ERR, REAL, EXP, CAD, ERR, VAR }, // DEC
            { ERR, ERR, OCT, ERR, ERR, ERR, CAD, ERR, VAR }, // OCT
            { ERR, ERR, ERR, HEX, ERR, ERR, CAD, ERR, VAR }, // HEX
            { ERR, REAL, ERR, ERR, REAL, EXP, CAD, ERR, VAR }, // REAL
            { ERR, EXP, ERR, ERR, EXP, EXP, CAD, ERR, VAR }, // EXP
            { CAD, CAD, CAD, CAD, CAD, CAD, CAD, ERR, CAD }, // CAD
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR }, // BOL
            { BIN, DEC, OCT, HEX, REAL, EXP, CAD, ERR, VAR } // VAR
    };

    private static final String[][] MATRIZ_RESTA_MULT = {
            // BIN, DEC, OCT, HEX, REAL, EXP, CAD, BOL, VAR
            { BIN, ERR, ERR, ERR, ERR, ERR, ERR, ERR, VAR },
            { ERR, DEC, ERR, ERR, REAL, EXP, ERR, ERR, VAR },
            { ERR, ERR, OCT, ERR, ERR, ERR, ERR, ERR, VAR },
            { ERR, ERR, ERR, HEX, ERR, ERR, ERR, ERR, VAR },
            { ERR, REAL, ERR, ERR, REAL, EXP, ERR, ERR, VAR },
            { ERR, EXP, ERR, ERR, EXP, EXP, ERR, ERR, VAR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { VAR, VAR, VAR, VAR, VAR, VAR, ERR, ERR, VAR }
    };

    private static final String[][] MATRIZ_DIV = {
            // BIN, DEC, OCT, HEX, REAL, EXP, CAD, BOL, VAR
            { BIN, ERR, ERR, ERR, ERR, ERR, ERR, ERR, VAR },
            { ERR, REAL, ERR, ERR, REAL, EXP, ERR, ERR, VAR },
            { ERR, ERR, OCT, ERR, ERR, ERR, ERR, ERR, VAR },
            { ERR, ERR, ERR, HEX, ERR, ERR, ERR, ERR, VAR },
            { ERR, REAL, ERR, ERR, REAL, EXP, ERR, ERR, VAR },
            { ERR, EXP, ERR, ERR, EXP, EXP, ERR, ERR, VAR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { VAR, VAR, VAR, VAR, VAR, VAR, ERR, ERR, VAR }
    };

    private static final String[][] MATRIZ_BITWISE = {
            // BIN, DEC, OCT, HEX, REAL, EXP, CAD, BOL, VAR
            { BIN, ERR, ERR, ERR, ERR, ERR, ERR, ERR, VAR },
            { ERR, DEC, ERR, ERR, ERR, ERR, ERR, ERR, VAR },
            { ERR, ERR, OCT, ERR, ERR, ERR, ERR, ERR, VAR },
            { ERR, ERR, ERR, HEX, ERR, ERR, ERR, ERR, VAR },
            { ERR, ERR, ERR, ERR, REAL, ERR, ERR, ERR, VAR },
            { ERR, ERR, ERR, ERR, ERR, EXP, ERR, ERR, VAR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { BIN, REAL, OCT, HEX, EXP, EXP, ERR, ERR, VAR }
    };

    private static final String[][] MATRIZ_IGUALDAD = {
            // BIN, DEC, OCT, HEX, REAL, EXP, CAD, BOL, VAR
            { BOL, ERR, ERR, ERR, ERR, ERR, ERR, ERR, BOL },
            { ERR, BOL, ERR, ERR, BOL, BOL, ERR, ERR, BOL },
            { ERR, ERR, BOL, ERR, ERR, ERR, ERR, ERR, BOL },
            { ERR, ERR, ERR, BOL, ERR, ERR, ERR, ERR, BOL },
            { ERR, BOL, ERR, ERR, BOL, BOL, ERR, ERR, BOL },
            { ERR, BOL, ERR, ERR, BOL, BOL, ERR, ERR, BOL },
            { ERR, ERR, ERR, ERR, ERR, ERR, BOL, ERR, BOL },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, BOL, ERR },
            { BOL, BOL, BOL, BOL, BOL, BOL, BOL, ERR, BOL }
    };

    private static final String[][] MATRIZ_RELACIONAL = {
            // BIN, DEC, OCT, HEX, REAL, EXP, CAD, BOL, VAR
            { BOL, ERR, ERR, ERR, ERR, ERR, ERR, ERR, BOL },
            { ERR, BOL, ERR, ERR, BOL, BOL, ERR, ERR, BOL },
            { ERR, ERR, BOL, ERR, ERR, ERR, ERR, ERR, BOL },
            { ERR, ERR, ERR, BOL, ERR, ERR, ERR, ERR, BOL },
            { ERR, BOL, ERR, ERR, BOL, BOL, ERR, ERR, BOL },
            { ERR, BOL, ERR, ERR, BOL, BOL, ERR, ERR, BOL },
            { ERR, ERR, ERR, ERR, ERR, ERR, BOL, ERR, BOL },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { BOL, BOL, BOL, BOL, BOL, BOL, BOL, ERR, BOL }
    };

    private static final String[][] MATRIZ_LOGICA = {
            // BIN, DEC, OCT, HEX, REAL, EXP, CAD, BOL, VAR
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, BOL, BOL },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, BOL, VAR }
    };

    private String evaluarMatriz(String[][] matriz, String t1, String t2) {
        int i1 = getIndex(t1);
        int i2 = getIndex(t2);
        if (i1 == 9 || i2 == 9)
            return ERR;
        return matriz[i1][i2];
    }

    // Move logic from Sintaxis to Semantica: EcuacionPrefijo, etc.
    public static class EcuacionPrefijo {
        public int linea;
        public String original;
        public String prefijo;
        public List<String> cuadruplos;

        public EcuacionPrefijo(int linea, String original, String prefijo, List<String> cuadruplos) {
            this.linea = linea;
            this.original = original;
            this.prefijo = prefijo;
            this.cuadruplos = cuadruplos;
        }
    }

    private List<EcuacionPrefijo> prefijos = new ArrayList<>();

    public List<EcuacionPrefijo> getPrefijos() {
        return prefijos;
    }

    public void analizarExpresiones(List<Tokens> tokens, ManejadorSimbolos manejadorSimbolos,
            Stack<Integer> stackNiveles) {
        analizarExpresiones(tokens, manejadorSimbolos, stackNiveles, null);
    }

    public void analizarExpresiones(List<Tokens> tokens, ManejadorSimbolos manejadorSimbolos,
            Stack<Integer> stackNiveles, Map<Tokens, Stack<Integer>> tokenScopeStacks) {
        for (int i = 0; i < tokens.size(); i++) {
            Tokens t = tokens.get(i);
            String lex = t.getLexema();
            if (lex.equals("=") || lex.equals("+=") || lex.equals("-=") || lex.equals("*=") || lex.equals("/=")) {
                int lineaApertura = t.getLinea();
                String id = "";
                if (i > 0) {
                    if (tokens.get(i - 1).getLexema().equals("]")) {
                        int k = i - 1;
                        int depth = 0;
                        while (k >= 0) {
                            if (tokens.get(k).getLexema().equals("]")) {
                                depth++;
                            } else if (tokens.get(k).getLexema().equals("[")) {
                                depth--;
                                if (depth == 0) {
                                    k--;
                                    break;
                                }
                            }
                            k--;
                        }
                        if (k >= 0) {
                            StringBuilder sb = new StringBuilder();
                            for (int j = k; j < i; j++) {
                                sb.append(tokens.get(j).getLexema());
                            }
                            id = sb.toString();
                        } else {
                            id = tokens.get(i - 1).getLexema();
                        }
                    } else {
                        id = tokens.get(i - 1).getLexema();
                    }
                }

                int start = i + 1;
                int end = start;
                while (end < tokens.size() && !tokens.get(end).getLexema().equals(";")) {
                    end++;
                }

                if (end > start) {
                    List<Tokens> rawExpr = tokens.subList(start, end);
                    List<Tokens> expr = simplificarTokensArreglos(rawExpr);
                    String prefijo = infixToPrefix(expr);

                    Stack<Integer> activeScopes = (tokenScopeStacks != null && tokenScopeStacks.containsKey(t))
                            ? tokenScopeStacks.get(t) : stackNiveles;

                    List<String> postfix = infixToPostfix(expr);
                    List<String> cuadruplos = generarCuadruplosSemanticos(postfix, id, lex, lineaApertura,
                            manejadorSimbolos, activeScopes);

                    StringBuilder orig = new StringBuilder();
                    if (!id.isEmpty())
                        orig.append(id).append(" ");
                    orig.append(lex).append(" ");
                    for (Tokens tExpr : rawExpr) {
                        orig.append(tExpr.getLexema()).append(" ");
                    }

                    if (!prefijo.isEmpty() || !cuadruplos.isEmpty()) {
                        prefijos.add(new EcuacionPrefijo(lineaApertura, orig.toString().trim(),
                                lex + " " + id + " " + prefijo, cuadruplos));
                    }
                }
                i = end;
            }
        }
    }

    private int precedencia(String op) {
        switch (op) {
            case "||":
            case "|":
                return 1;
            case "&&":
            case "&":
                return 2;
            case "==":
            case "!=":
            case "===":
            case "!==":
                return 3;
            case "<":
            case "<=":
            case ">":
            case ">=":
                return 4;
            case "+":
            case "-":
            case "<<":
            case ">>":
            case ">>>":
                return 5;
            case "*":
            case "/":
            case "%":
            case "#":
                return 6;
            case "^":
            case "**":
                return 7;
            case "!":
            case "~":
                return 8;
        }
        return -1;
    }

    private boolean isOperator(String c) {
        return precedencia(c) > 0;
    }

    private String infixToPrefix(List<Tokens> expr) {
        List<String> reversed = new ArrayList<>();
        for (int i = expr.size() - 1; i >= 0; i--) {
            String lex = expr.get(i).getLexema();
            if (lex.equals("("))
                reversed.add(")");
            else if (lex.equals(")"))
                reversed.add("(");
            else
                reversed.add(lex);
        }

        Stack<String> stack = new Stack<>();
        List<String> postfix = new ArrayList<>();

        for (String c : reversed) {
            if (isOperator(c)) {
                while (!stack.isEmpty() && isOperator(stack.peek()) && precedencia(stack.peek()) > precedencia(c)) {
                    postfix.add(stack.pop());
                }
                stack.push(c);
            } else if (c.equals("(")) {
                stack.push(c);
            } else if (c.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    postfix.add(stack.pop());
                }
                if (!stack.isEmpty())
                    stack.pop();
            } else {
                postfix.add(c);
            }
        }
        while (!stack.isEmpty()) {
            postfix.add(stack.pop());
        }

        StringBuilder res = new StringBuilder();
        for (int i = postfix.size() - 1; i >= 0; i--) {
            res.append(postfix.get(i)).append(" ");
        }
        return res.toString().trim();
    }

    private List<String> infixToPostfix(List<Tokens> expr) {
        Stack<String> stack = new Stack<>();
        List<String> postfix = new ArrayList<>();

        for (Tokens t : expr) {
            String c = t.getLexema();
            if (isOperator(c)) {
                while (!stack.isEmpty() && isOperator(stack.peek()) && precedencia(stack.peek()) >= precedencia(c)) {
                    postfix.add(stack.pop());
                }
                stack.push(c);
            } else if (c.equals("(")) {
                stack.push(c);
            } else if (c.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    postfix.add(stack.pop());
                }
                if (!stack.isEmpty())
                    stack.pop();
            } else {
                postfix.add(c);
            }
        }
        while (!stack.isEmpty()) {
            postfix.add(stack.pop());
        }
        return postfix;
    }

    private List<String> generarCuadruplosSemanticos(List<String> postfix, String id, String opAsig, int linea,
            ManejadorSimbolos ms, Stack<Integer> niveles) {
        List<String> cuadruplos = new ArrayList<>();
        Stack<String> stackValor = new Stack<>();
        Stack<String> stackTipo = new Stack<>();
        int tempCont = 1;

        for (String c : postfix) {
            if (isOperator(c)) {
                if (c.equals("!") || c.equals("~")) {
                    String rightVal = stackValor.isEmpty() ? "" : stackValor.pop();
                    String rightTipo = stackTipo.isEmpty() ? "variant" : stackTipo.pop();
                    String temp = "T" + tempCont++;

                    String resTipo = "variant";
                    if (c.equals("!")) {
                        String n = normalizeType(rightTipo);
                        if (n.equals("boolean") || n.equals("variant")) {
                            resTipo = "boolean";
                        } else {
                            resTipo = "error";
                        }
                    } else if (c.equals("~")) {
                        String n = normalizeType(rightTipo);
                        if (esTipoEntero(n)) {
                            resTipo = n;
                        } else if (n.equals("variant")) {
                            resTipo = "variant";
                        } else {
                            resTipo = "error";
                        }
                    }

                    ConteoTemporales conteo = getConteo(linea);
                    if (resTipo.equals("error")) {
                        conteo.errores++;
                        conteo.tVariant++;
                        String desc = "Operando incompatible con operador unario " + c + ": " + rightVal + " ("
                                + rightTipo + ")";
                        erroresSemanticos.add(new ErroresSin("903", "Semantica", c, desc, linea));
                        stackTipo.push("variant");
                    } else {
                        incrementarTemporal(conteo, resTipo);
                        stackTipo.push(resTipo);
                    }

                    cuadruplos.add(c + "," + rightVal + ",," + temp);
                    stackValor.push(temp);
                } else {
                    String rightVal = stackValor.isEmpty() ? "" : stackValor.pop();
                    String rightTipo = stackTipo.isEmpty() ? "variant" : stackTipo.pop();
                    String leftVal = stackValor.isEmpty() ? "" : stackValor.pop();
                    String leftTipo = stackTipo.isEmpty() ? "variant" : stackTipo.pop();
                    String temp = "T" + tempCont++;

                    ResultadoSemantica rs = checkOperacionBinaria(leftTipo, c, rightTipo, linea, leftVal, rightVal);

                    cuadruplos.add(c + "," + leftVal + "," + rightVal + "," + temp);
                    stackValor.push(temp);
                    stackTipo.push(rs.tipo);
                }
            } else {
                stackValor.push(c);
                String tipo = deducirTipo(c, ms, niveles);
                stackTipo.push(tipo);
            }
        }

        String resFinalVal = stackValor.isEmpty() ? "" : stackValor.pop();
        String resFinalTipo = stackTipo.isEmpty() ? "variant" : stackTipo.pop();

        if (!resFinalVal.isEmpty()) {
            String tipoDestino = deducirTipo(id, ms, niveles);
            if (opAsig.equals("=")) {
                cuadruplos.add(opAsig + "," + id + "," + resFinalVal);
                checkAsignacion(tipoDestino, resFinalTipo, linea, id);
            } else {
                String binOp = opAsig.substring(0, opAsig.length() - 1);
                String tempComp = "T" + tempCont++;
                ResultadoSemantica rs = checkOperacionBinaria(tipoDestino, binOp, resFinalTipo, linea, id,
                        resFinalVal);
                cuadruplos.add(binOp + "," + id + "," + resFinalVal + "," + tempComp);
                cuadruplos.add("=," + id + "," + tempComp);
                checkAsignacion(tipoDestino, rs.tipo, linea, id);
            }
        }
        return cuadruplos;
    }

    private String deducirTipo(String lexema, ManejadorSimbolos ms, Stack<Integer> niveles) {
        if (lexema == null || lexema.isEmpty())
            return "variant";
        if (lexema.contains("[")) {
            lexema = lexema.substring(0, lexema.indexOf('['));
        }
        if (ms != null) {
            EntidadSimbolo s = null;
            if (niveles != null && !niveles.isEmpty()) {
                s = ms.buscarEnContexto(lexema, niveles);
            }
            if (s == null) {
                s = buscarSimboloGlobal(ms, lexema);
            }
            if (s != null && s.getTipoDato() != null && !s.getTipoDato().isEmpty() && !s.getTipoDato().equals("void")) {
                return s.getTipoDato();
            }
        }

        char c = lexema.charAt(0);
        if (c == '#') {
            if (lexema.length() > 1) {
                char t = Character.toUpperCase(lexema.charAt(1));
                if (t == 'B')
                    return "bin";
                if (t == 'D')
                    return "dec";
                if (t == 'O')
                    return "oct";
                if (t == 'X')
                    return "hex";
            }
            return "dec";
        }
        if (c == '@')
            return "cadena";
        if (c == '$')
            return "real";
        if (c == '¿')
            return "exp";
        if (c == '¡')
            return "bool";

        if (lexema.equals("true") || lexema.equals("false"))
            return "bool";
        if (lexema.toLowerCase().startsWith("xb"))
            return "bin";
        if (lexema.toLowerCase().startsWith("xo"))
            return "oct";
        if (lexema.toLowerCase().startsWith("xl"))
            return "hex";
        if (lexema.contains("^"))
            return "exp";
        if (lexema.matches("[0-9]+"))
            return "dec";
        if (lexema.matches("[0-9]+\\.[0-9]+"))
            return "real";
        if (lexema.startsWith("\"") || lexema.startsWith("'"))
            return "cadena";

        return "variant"; // fallback
    }

    private List<Tokens> simplificarTokensArreglos(List<Tokens> rawTokens) {
        List<Tokens> res = new ArrayList<>();
        for (int i = 0; i < rawTokens.size(); i++) {
            Tokens tk = rawTokens.get(i);
            String lex = tk.getLexema();
            if ((tk.getTipo().startsWith("Identificador") || esId(lex))
                    && i + 1 < rawTokens.size() && rawTokens.get(i + 1).getLexema().equals("[")) {
                StringBuilder sb = new StringBuilder();
                sb.append(lex);
                int j = i + 1;
                int depth = 0;
                while (j < rawTokens.size()) {
                    Tokens tArr = rawTokens.get(j);
                    sb.append(tArr.getLexema());
                    if (tArr.getLexema().equals("[")) {
                        depth++;
                    } else if (tArr.getLexema().equals("]")) {
                        depth--;
                        if (depth == 0) {
                            j++;
                            break;
                        }
                    }
                    j++;
                }
                res.add(new Tokens("Identificador", sb.toString(), tk.getLinea()));
                i = j - 1;
            } else {
                res.add(tk);
            }
        }
        return res;
    }

    private boolean esId(String lex) {
        if (lex == null || lex.isEmpty())
            return false;
        char c = lex.charAt(0);
        return (c == '#' || c == '@' || c == '$' || c == '¿' || c == '¡') || Character.isLetter(c);
    }

    public String evaluarTipoExpresion(List<Tokens> expr, ManejadorSimbolos ms, Stack<Integer> niveles) {
        if (expr == null || expr.isEmpty())
            return "variant";
        List<Tokens> simpl = simplificarTokensArreglos(expr);
        List<String> postfix = infixToPostfix(simpl);
        Stack<String> stackTipo = new Stack<>();

        for (String c : postfix) {
            if (isOperator(c)) {
                if (c.equals("!") || c.equals("~")) {
                    String rightTipo = stackTipo.isEmpty() ? "variant" : stackTipo.pop();
                    if (c.equals("!")) {
                        stackTipo.push(rightTipo.equals("boolean") || rightTipo.equals("bool") ? "boolean" : "variant");
                    } else {
                        stackTipo.push(rightTipo);
                    }
                } else {
                    String rightTipo = stackTipo.isEmpty() ? "variant" : stackTipo.pop();
                    String leftTipo = stackTipo.isEmpty() ? "variant" : stackTipo.pop();
                    String resTipo = resolverTipoOperacion(leftTipo, c, rightTipo);
                    stackTipo.push(resTipo);
                }
            } else {
                String tipo = deducirTipo(c, ms, niveles);
                stackTipo.push(tipo);
            }
        }
        return stackTipo.isEmpty() ? "variant" : stackTipo.pop();
    }

    public String resolverTipoOperacion(String tipoIzq, String op, String tipoDer) {
        tipoIzq = normalizeType(tipoIzq);
        tipoDer = normalizeType(tipoDer);

        if (op.equals("+")) {
            return evaluarMatriz(MATRIZ_SUMA, tipoIzq, tipoDer);
        } else if (op.equals("-") || op.equals("*")) {
            return evaluarMatriz(MATRIZ_RESTA_MULT, tipoIzq, tipoDer);
        } else if (op.equals("/")) {
            return evaluarMatriz(MATRIZ_DIV, tipoIzq, tipoDer);
        } else if (op.equals("%") || op.equals(">>>") || op.equals(">>") || op.equals("<<") || op.equals("^")
                || op.equals("**")) {
            return evaluarMatriz(MATRIZ_BITWISE, tipoIzq, tipoDer);
        } else if (op.equals("==") || op.equals("!=") || op.equals("===") || op.equals("!==")) {
            return evaluarMatriz(MATRIZ_IGUALDAD, tipoIzq, tipoDer);
        } else if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
            return evaluarMatriz(MATRIZ_RELACIONAL, tipoIzq, tipoDer);
        } else if (op.equals("&&") || op.equals("||") || op.equals("&") || op.equals("|")) {
            return evaluarMatriz(MATRIZ_LOGICA, tipoIzq, tipoDer);
        }
        return ERR;
    }

    public boolean esCompatibleAsignacion(String tipoVar, String tipoExpr) {
        tipoVar = normalizeType(tipoVar);
        tipoExpr = normalizeType(tipoExpr);

        if (tipoExpr.equals("variant") || tipoVar.equals("variant")) {
            return true;
        }
        if (tipoVar.equals(tipoExpr)) {
            return true;
        }
        if (tipoVar.equals("real")) {
            return tipoExpr.equals("dec") || tipoExpr.equals("bin") || tipoExpr.equals("oct")
                    || tipoExpr.equals("hex");
        } else if (tipoVar.equals("exp")) {
            return tipoExpr.equals("real") || tipoExpr.equals("dec") || tipoExpr.equals("bin")
                    || tipoExpr.equals("oct") || tipoExpr.equals("hex");
        } else if (tipoVar.equals("dec")) {
            return tipoExpr.equals("bin") || tipoExpr.equals("oct") || tipoExpr.equals("hex");
        }
        return false;
    }

    public boolean esTipoEntero(String tipo) {
        if (tipo == null)
            return false;
        String n = normalizeType(tipo);
        return n.equals("dec") || n.equals("bin") || n.equals("oct") || n.equals("hex");
    }

    private List<List<Tokens>> separarIndices(List<Tokens> tokensEntreCorchetes) {
        List<List<Tokens>> res = new ArrayList<>();
        List<Tokens> actual = new ArrayList<>();
        int pDepth = 0;
        int bDepth = 0;

        for (Tokens tk : tokensEntreCorchetes) {
            String lx = tk.getLexema();
            if (lx.equals("("))
                pDepth++;
            else if (lx.equals(")")) {
                if (pDepth > 0)
                    pDepth--;
            } else if (lx.equals("["))
                bDepth++;
            else if (lx.equals("]")) {
                if (bDepth > 0)
                    bDepth--;
            }

            if (lx.equals(",") && pDepth == 0 && bDepth == 0) {
                if (!actual.isEmpty()) {
                    res.add(new ArrayList<>(actual));
                    actual.clear();
                }
            } else {
                actual.add(tk);
            }
        }
        if (!actual.isEmpty()) {
            res.add(actual);
        }
        return res;
    }

    private String tokensToString(List<Tokens> list) {
        if (list == null || list.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (Tokens t : list) {
            sb.append(t.getLexema());
        }
        return sb.toString();
    }

    private Integer parsearConstanteEntera(List<Tokens> idxExpr) {
        if (idxExpr == null || idxExpr.isEmpty())
            return null;
        if (idxExpr.size() == 1) {
            Tokens tk = idxExpr.get(0);
            String lx = tk.getLexema();
            try {
                if (lx.toLowerCase().startsWith("xb")) {
                    return Integer.parseInt(lx.substring(2), 2);
                } else if (lx.toLowerCase().startsWith("xo")) {
                    return Integer.parseInt(lx.substring(2), 8);
                } else if (lx.toLowerCase().startsWith("xl")) {
                    return Integer.parseInt(lx.substring(2), 16);
                } else if (lx.matches("[0-9]+")) {
                    return Integer.parseInt(lx);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    private EntidadSimbolo buscarSimboloGlobal(ManejadorSimbolos ms, String id) {
        if (ms == null || id == null)
            return null;
        for (EntidadSimbolo s : ms.obtenerTodos()) {
            if (id.equals(s.getIdentificador())) {
                return s;
            }
        }
        return null;
    }

    private boolean esDeclaracion(List<Tokens> tokens, int idx) {
        for (int j = idx - 1; j >= 0; j--) {
            String lx = tokens.get(j).getLexema();
            if (lx.equals(";"))
                return false;
            if (lx.equals("var") || lx.equals("reg") || lx.equals("def"))
                return true;
        }
        return false;
    }

    private String[] detectarTamanioEnTokens(List<Tokens> tokens, int i) {
        List<String> ls = new ArrayList<>();
        int idx = i + 1;
        if (idx < tokens.size() && tokens.get(idx).getLexema().equals("[")) {
            idx++;
            while (idx < tokens.size() && !tokens.get(idx).getLexema().equals("]")) {
                Tokens tk = tokens.get(idx);
                if (tk.getTipo().startsWith("Constante")) {
                    ls.add(tk.getLexema());
                }
                idx++;
            }
        }
        return ls.isEmpty() ? null : ls.toArray(new String[0]);
    }

    public void evaluarReglasSemanticas(List<Tokens> tokens, ManejadorSimbolos ms,
            Map<Tokens, Integer> tokenAmbitos, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Integer> lineaAmbitos) {
        reglasSemanticas.clear();

        Stack<Integer> doStack = new Stack<>();

        for (int i = 0; i < tokens.size(); i++) {
            Tokens t = tokens.get(i);
            String lex = t.getLexema();
            int ln = t.getLinea();
            int amb = tokenAmbitos.getOrDefault(t, lineaAmbitos.getOrDefault(ln, 0));
            Stack<Integer> scopes = tokenScopeStacks.getOrDefault(t, new Stack<>());
            if (scopes.isEmpty())
                scopes.push(amb);

            // ==========================================
            // REGLA 1: EXP debe ser lógica
            // Claves: 1010 (if), 1011 (elseif), 1012 (while), 1013 (do-while)
            // ==========================================
            if (lex.equals("do")) {
                doStack.push(i);
            } else if (lex.equals("if") || lex.equals("elseif") || lex.equals("while")) {
                String clave = "1010";
                String nombreEst = "if";

                if (lex.equals("elseif")
                        || (lex.equals("if") && i > 0 && tokens.get(i - 1).getLexema().equals("else"))) {
                    clave = "1011";
                    nombreEst = "elseif";
                } else if (lex.equals("while")) {
                    boolean isDoWhile = false;
                    if (!doStack.isEmpty()) {
                        int scanDepth = 0;
                        int scanEnd = i + 1;
                        if (scanEnd < tokens.size() && tokens.get(scanEnd).getLexema().equals("(")) {
                            scanDepth = 1;
                            scanEnd++;
                            while (scanEnd < tokens.size() && scanDepth > 0) {
                                if (tokens.get(scanEnd).getLexema().equals("("))
                                    scanDepth++;
                                else if (tokens.get(scanEnd).getLexema().equals(")"))
                                    scanDepth--;
                                scanEnd++;
                            }
                            if (scanEnd < tokens.size() && (tokens.get(scanEnd).getLexema().equals(";")
                                    || tokens.get(scanEnd).getLexema().equals("}"))) {
                                isDoWhile = true;
                            }
                        }
                    }
                    if (isDoWhile) {
                        doStack.pop();
                        clave = "1013";
                        nombreEst = "do-while";
                    } else {
                        clave = "1012";
                        nombreEst = "while";
                    }
                }

                if (i + 1 < tokens.size() && tokens.get(i + 1).getLexema().equals("(")) {
                    int start = i + 2;
                    int depth = 1;
                    int end = start;
                    while (end < tokens.size()) {
                        if (tokens.get(end).getLexema().equals("("))
                            depth++;
                        else if (tokens.get(end).getLexema().equals(")")) {
                            depth--;
                            if (depth == 0)
                                break;
                        }
                        end++;
                    }
                    if (end > start) {
                        List<Tokens> condExpr = tokens.subList(start, end);
                        String tipoCond = evaluarTipoExpresion(condExpr, ms, scopes);
                        String tipoNorm = normalizeType(tipoCond);
                        boolean esLogica = tipoNorm.equals("boolean") || tipoNorm.equals("variant");
                        String situacion = esLogica ? "Acept" : "Error";
                        String desc;
                        if (esLogica) {
                            desc = "CVE " + clave + " Condicion " + nombreEst + " tipo Logica";
                        } else {
                            desc = "CVE " + clave + " Condicion " + nombreEst + " no es logica (tipo "
                                    + formatTipo(tipoNorm) + ")";
                            erroresSemanticos.add(new ErroresSin(clave, "Semantica", nombreEst, desc, ln));
                        }
                        reglasSemanticas.add(new ReglaSemantica(ln, 1, clave, desc, situacion, nombreEst, amb));
                    }
                }
            }

            // ==========================================
            // REGLA 2: Identificador del mismo tipo que EXP
            // Claves: 1020 (=), 1021 (+=), 1022 (-=, *=, /=)
            // ==========================================
            if (lex.equals("=") || lex.equals("+=") || lex.equals("-=") || lex.equals("*=") || lex.equals("/=")) {
                String clave = "1020";
                if (lex.equals("+=")) {
                    clave = "1021";
                } else if (!lex.equals("=")) {
                    clave = "1022";
                }

                String idCompleto = "";
                String baseId = "";
                if (i > 0) {
                    if (tokens.get(i - 1).getLexema().equals("]")) {
                        int k = i - 1;
                        int depth = 0;
                        while (k >= 0) {
                            if (tokens.get(k).getLexema().equals("]"))
                                depth++;
                            else if (tokens.get(k).getLexema().equals("[")) {
                                depth--;
                                if (depth == 0) {
                                    k--;
                                    break;
                                }
                            }
                            k--;
                        }
                        if (k >= 0) {
                            baseId = tokens.get(k).getLexema();
                            StringBuilder sb = new StringBuilder();
                            for (int j = k; j < i; j++) {
                                sb.append(tokens.get(j).getLexema());
                            }
                            idCompleto = sb.toString();
                        } else {
                            idCompleto = tokens.get(i - 1).getLexema();
                            baseId = idCompleto;
                        }
                    } else {
                        idCompleto = tokens.get(i - 1).getLexema();
                        baseId = idCompleto;
                    }
                }

                int start = i + 1;
                int end = start;
                while (end < tokens.size() && !tokens.get(end).getLexema().equals(";")) {
                    end++;
                }

                if (end > start) {
                    List<Tokens> rawExpr = tokens.subList(start, end);
                    String tipoDestino = deducirTipo(baseId, ms, scopes);
                    String tipoExpr = evaluarTipoExpresion(rawExpr, ms, scopes);

                    boolean valida = esCompatibleAsignacion(tipoDestino, tipoExpr);
                    String situacion = valida ? "Acept" : "Error";
                    String desc;
                    if (valida) {
                        desc = "CVE " + clave + " Asignacion " + lex + " tipo " + formatTipo(tipoDestino) + " con "
                                + formatTipo(tipoExpr);
                    } else {
                        desc = "CVE " + clave + " Asignacion incompatible " + lex + ": tipo " + formatTipo(tipoDestino)
                                + " con " + formatTipo(tipoExpr);
                        erroresSemanticos.add(new ErroresSin(clave, "Semantica", lex, desc, ln));
                    }
                    reglasSemanticas.add(new ReglaSemantica(ln, 2, clave, desc, situacion, idCompleto, amb));
                }
            }

            // ==========================================
            // REGLAS 3, 4, 5: Arreglos
            // Clave 1030: Dimensión del arreglo
            // Clave 1040: El índice debe ser entero
            // Clave 1050: Rango del arreglo
            // ==========================================
            if ((t.getTipo().startsWith("Identificador") || esId(lex))
                    && i + 1 < tokens.size() && tokens.get(i + 1).getLexema().equals("[")) {
                String arrId = lex;
                int startBracket = i + 1;
                int depth = 1;
                int endBracket = startBracket + 1;
                while (endBracket < tokens.size()) {
                    if (tokens.get(endBracket).getLexema().equals("["))
                        depth++;
                    else if (tokens.get(endBracket).getLexema().equals("]")) {
                        depth--;
                        if (depth == 0)
                            break;
                    }
                    endBracket++;
                }

                if (endBracket < tokens.size()) {
                    boolean esDecl = esDeclaracion(tokens, i);
                    if (esDecl) {
                        String[] dims = detectarTamanioEnTokens(tokens, i);
                        if (dims != null && dims.length > 0) {
                            String desc3 = "CVE 1030 Declaracion arreglo " + arrId + " con " + dims.length
                                    + " dimension(es)";
                            reglasSemanticas.add(new ReglaSemantica(ln, 3, "1030", desc3, "Acept", arrId, amb));
                            String desc5 = "CVE 1050 Dimensiones declaradas en " + arrId + ": ["
                                    + String.join(",", dims) + "]";
                            reglasSemanticas.add(new ReglaSemantica(ln, 5, "1050", desc5, "Acept", arrId, amb));
                        }
                    } else {
                        // Acceso a arreglo
                        List<List<Tokens>> indicesList = separarIndices(tokens.subList(startBracket + 1, endBracket));
                        StringBuilder accSb = new StringBuilder(arrId);
                        for (int j = startBracket; j <= endBracket; j++) {
                            accSb.append(tokens.get(j).getLexema());
                        }
                        String fullAccess = accSb.toString();

                        EntidadSimbolo sym = ms.buscarEnContexto(arrId, scopes);
                        if (sym == null)
                            sym = buscarSimboloGlobal(ms, arrId);

                        if (sym != null && sym.getClasificacion().equals("arreglo")) {
                            int dimEsperada = 1;
                            try {
                                dimEsperada = Integer.parseInt(sym.getDimensionArreglo());
                            } catch (Exception e) {
                            }
                            String[] sizes = sym.getTipoArreglo().split(",");
                            int dimUsada = indicesList.size();

                            // Regla 3: Dimensión del arreglo (1030)
                            if (dimUsada == dimEsperada) {
                                String desc3 = "CVE 1030 Dimension del arreglo " + arrId + " correcta (" + dimUsada
                                        + ")";
                                reglasSemanticas
                                        .add(new ReglaSemantica(ln, 3, "1030", desc3, "Acept", fullAccess, amb));
                            } else {
                                String desc3 = "CVE 1030 Dimension incorrecta en " + arrId + ": esperada " + dimEsperada
                                        + ", encontrada " + dimUsada;
                                erroresSemanticos.add(new ErroresSin("1030", "Semantica", fullAccess, desc3, ln));
                                reglasSemanticas
                                        .add(new ReglaSemantica(ln, 3, "1030", desc3, "Error", fullAccess, amb));
                            }

                            // Regla 4: El índice debe ser entero (1040)
                            boolean[] indicesEnteros = new boolean[dimUsada];
                            for (int k = 0; k < dimUsada; k++) {
                                List<Tokens> idxExpr = indicesList.get(k);
                                String tipoIdx = evaluarTipoExpresion(idxExpr, ms, scopes);
                                String idxStr = tokensToString(idxExpr);
                                boolean esEntero = esTipoEntero(tipoIdx);
                                indicesEnteros[k] = esEntero;
                                if (esEntero) {
                                    String desc4 = "CVE 1040 Indice " + (k + 1) + " entero valido en " + arrId
                                            + " (tipo " + formatTipo(tipoIdx) + ")";
                                    reglasSemanticas
                                            .add(new ReglaSemantica(ln, 4, "1040", desc4, "Acept", idxStr, amb));
                                } else {
                                    String desc4 = "CVE 1040 Indice " + (k + 1) + " no es entero en " + arrId
                                            + ": tipo " + formatTipo(tipoIdx);
                                    erroresSemanticos.add(new ErroresSin("1040", "Semantica", idxStr, desc4, ln));
                                    reglasSemanticas
                                            .add(new ReglaSemantica(ln, 4, "1040", desc4, "Error", idxStr, amb));
                                }
                            }

                            // Regla 5: Rango del arreglo (1050)
                            for (int k = 0; k < dimUsada; k++) {
                                if (k < sizes.length && indicesEnteros[k]) {
                                    int maxTam = 0;
                                    try {
                                        maxTam = Integer.parseInt(sizes[k].trim());
                                    } catch (Exception e) {
                                    }
                                    List<Tokens> idxExpr = indicesList.get(k);
                                    String idxStr = tokensToString(idxExpr);
                                    Integer valConst = parsearConstanteEntera(idxExpr);
                                    if (valConst != null) {
                                        if (valConst >= 0 && valConst < maxTam) {
                                            String desc5 = "CVE 1050 Indice " + valConst + " dentro de rango [0, "
                                                    + (maxTam - 1) + "] en " + arrId;
                                            reglasSemanticas.add(
                                                    new ReglaSemantica(ln, 5, "1050", desc5, "Acept", idxStr, amb));
                                        } else {
                                            String desc5 = "CVE 1050 Indice " + valConst + " fuera de rango [0, "
                                                    + (maxTam - 1) + "] en " + arrId;
                                            erroresSemanticos
                                                    .add(new ErroresSin("1050", "Semantica", idxStr, desc5, ln));
                                            reglasSemanticas.add(
                                                    new ReglaSemantica(ln, 5, "1050", desc5, "Error", idxStr, amb));
                                        }
                                    } else {
                                        String desc5 = "CVE 1050 Indice dinamico '" + idxStr + "' en " + arrId
                                                + " (rango [0, " + (maxTam - 1) + "])";
                                        reglasSemanticas
                                                .add(new ReglaSemantica(ln, 5, "1050", desc5, "Acept", idxStr, amb));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        reglasSemanticas.sort(Comparator.comparingInt(ReglaSemantica::getLinea)
                .thenComparingInt(ReglaSemantica::getRegla)
                .thenComparing(ReglaSemantica::getClave));
    }

    public void exportarAvanceTxt(String filepath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath, StandardCharsets.UTF_8))) {
            bw.write(
                    "=================================================================================================================================");
            bw.newLine();
            bw.write("                                                   AVANCE DE REGLAS SEMÁNTICAS");
            bw.newLine();
            bw.write(
                    "=================================================================================================================================");
            bw.newLine();
            bw.write(String.format("%-6s | %-5s | %-65s | %-9s | %-15s | %s", "Linea", "Regla", "Descripción",
                    "Situación", "Lexema", "Ámbito"));
            bw.newLine();
            bw.write(
                    "---------------------------------------------------------------------------------------------------------------------------------");
            bw.newLine();
            int aceptados = 0;
            int errores = 0;
            for (ReglaSemantica r : reglasSemanticas) {
                bw.write(r.toTableLine());
                bw.newLine();
                if ("Acept".equalsIgnoreCase(r.getSituacion())) {
                    aceptados++;
                } else {
                    errores++;
                }
            }
            bw.write(
                    "---------------------------------------------------------------------------------------------------------------------------------");
            bw.newLine();
            bw.write(String.format("TOTAL EVALUACIONES: %d | ACEPTADAS: %d | ERRORES: %d", reglasSemanticas.size(),
                    aceptados, errores));
            bw.newLine();
            bw.write(
                    "=================================================================================================================================");
            bw.newLine();
        }
    }
}
