package lexico;

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
            { BIN, ERR, ERR, ERR, ERR, ERR, CAD, ERR, VAR },
            { ERR, REAL, ERR, ERR, REAL, EXP, CAD, ERR, VAR },
            { ERR, ERR, OCT, ERR, ERR, ERR, CAD, ERR, VAR },
            { ERR, ERR, ERR, HEX, ERR, ERR, CAD, ERR, VAR },
            { ERR, REAL, ERR, ERR, REAL, EXP, CAD, ERR, VAR },
            { ERR, EXP, ERR, ERR, EXP, EXP, CAD, ERR, VAR },
            { CAD, CAD, CAD, CAD, CAD, CAD, CAD, ERR, CAD },
            { ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR, ERR },
            { VAR, VAR, VAR, VAR, VAR, VAR, CAD, ERR, VAR }
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

    public void analizarExpresiones(List<Tokens> tokens, ManejadorSimbolos manejadorSimbolos,
            Stack<Integer> stackNiveles, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Stack<Integer>> lineaScopeStacks) {
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

                    Stack<Integer> activeScopes = (tokenScopeStacks != null && tokenScopeStacks.containsKey(t))
                            ? tokenScopeStacks.get(t)
                            : (lineaScopeStacks != null && lineaScopeStacks.containsKey(lineaApertura)
                                    ? lineaScopeStacks.get(lineaApertura)
                                    : stackNiveles);
                    if (activeScopes == null || activeScopes.isEmpty()) {
                        activeScopes = new Stack<>();
                        activeScopes.push(0);
                    }

                    List<String> postfix = infixToPostfix(expr);
                    generarCuadruplosSemanticos(postfix, id, lex, lineaApertura,
                            manejadorSimbolos, activeScopes);
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
            } else if ((tk.getTipo().startsWith("Identificador") || esId(lex))
                    && !esPalabraClave(lex)
                    && i + 1 < rawTokens.size() && rawTokens.get(i + 1).getLexema().equals("(")) {
                int j = i + 1;
                int depth = 0;
                while (j < rawTokens.size()) {
                    Tokens tCall = rawTokens.get(j);
                    if (tCall.getLexema().equals("(")) {
                        depth++;
                    } else if (tCall.getLexema().equals(")")) {
                        depth--;
                        if (depth == 0) {
                            j++;
                            break;
                        }
                    }
                    j++;
                }
                res.add(new Tokens("Identificador", lex, tk.getLinea()));
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

    private boolean esPalabraClave(String s) {
        if (s == null)
            return false;
        String sl = s.toLowerCase();
        return sl.equals("var") || sl.equals("reg") || sl.equals("def") || sl.equals("main")
                || sl.equals("if") || sl.equals("elseif") || sl.equals("else")
                || sl.equals("while") || sl.equals("do") || sl.equals("for")
                || sl.equals("switch") || sl.equals("case") || sl.equals("break") || sl.equals("default")
                || sl.equals("return") || sl.equals("true") || sl.equals("false") || sl.equals("null")
                || sl.equals("console.log") || sl.equals("console.read")
                || sl.equals("console") || sl.equals("log") || sl.equals("read");
    }

    private boolean esGlobal(String lex) {
        if (lex == null)
            return false;
        String[] builtins = { "SQRT", "sin", "cos", "tan", "POW", "SQRTV", "STRLEN", "concat", "copy", "val", "str",
                "chr", "pred", "succ", "inc", "dec", "sqr", "CLEAR", "Console.log", "console.log", "Console.read",
                "console.read" };
        for (String b : builtins)
            if (b.equalsIgnoreCase(lex))
                return true;
        return false;
    }

    private String determinarTipo(String lexema) {
        if (lexema == null || lexema.isEmpty())
            return "void";
        char c = lexema.charAt(0);
        if (c == '#') {
            if (lexema.length() > 1) {
                char t = Character.toUpperCase(lexema.charAt(1));
                if (t == 'B')
                    return "bin";
                if (t == 'D')
                    return "decimal";
                if (t == 'O')
                    return "oct";
                if (t == 'X')
                    return "hex";
            }
            return "decimal";
        }
        if (c == '@')
            return "cadena";
        if (c == '$')
            return "real";
        if (c == '¿')
            return "exp";
        if (c == '¡')
            return "bool";
        return "void";
    }

    private EntidadSimbolo buscarSubrutina(ManejadorSimbolos ms, String funId, Stack<Integer> scopes) {
        if (ms == null || funId == null)
            return null;
        if (scopes != null && !scopes.isEmpty()) {
            EntidadSimbolo s = ms.buscarEnContexto(funId, scopes);
            if (s != null && ("funcion".equalsIgnoreCase(s.getClasificacion())
                    || "procedimiento".equalsIgnoreCase(s.getClasificacion()))) {
                return s;
            }
        }
        for (EntidadSimbolo s : ms.obtenerTodos()) {
            if (funId.equals(s.getIdentificador()) && ("funcion".equalsIgnoreCase(s.getClasificacion())
                    || "procedimiento".equalsIgnoreCase(s.getClasificacion()))) {
                return s;
            }
        }
        return null;
    }

    private List<EntidadSimbolo> obtenerParametrosSubrutina(ManejadorSimbolos ms, String funId) {
        List<EntidadSimbolo> params = new ArrayList<>();
        if (ms == null || funId == null)
            return params;
        Integer targetAmbito = null;
        for (EntidadSimbolo s : ms.obtenerTodos()) {
            if ("param".equalsIgnoreCase(s.getClasificacion()) && funId.equals(s.getTipoParametros())) {
                if (targetAmbito == null) {
                    targetAmbito = s.getNivelAmbito();
                }
                if (s.getNivelAmbito() == targetAmbito) {
                    params.add(s);
                }
            }
        }
        params.sort(Comparator.comparingInt(s -> s.getNumeroParametros() != null ? s.getNumeroParametros() : 0));
        return params;
    }

    private String formatTSem2(String t) {
        if (t == null)
            return "TVariant";
        if (t.equalsIgnoreCase("void"))
            return "TVoid";
        String n = normalizeType(t);
        switch (n) {
            case "bin":
                return "TBin";
            case "dec":
                return "TDec";
            case "oct":
                return "TOct";
            case "hex":
                return "THex";
            case "real":
                return "TReal";
            case "exp":
                return "TExp";
            case "cadena":
                return "TCadena";
            case "boolean":
                return "TBoolean";
            default:
                return "TVariant";
        }
    }

    private String deducirTipoSem2(String lexema, ManejadorSimbolos ms, Stack<Integer> niveles) {
        if (lexema == null || lexema.isEmpty())
            return "variant";
        if (lexema.contains("[")) {
            lexema = lexema.substring(0, lexema.indexOf('['));
        }
        if (lexema.equals("true") || lexema.equals("false"))
            return "boolean";
        if (lexema.equals("null"))
            return "variant";
        if (lexema.toLowerCase().startsWith("xb"))
            return "bin";
        if (lexema.toLowerCase().startsWith("xo"))
            return "oct";
        if (lexema.toLowerCase().startsWith("xl"))
            return "hex";
        if (lexema.contains("^") && Character.isDigit(lexema.charAt(0)))
            return "exp";
        if (lexema.matches("[0-9]+\\.[0-9]+"))
            return "real";
        if (lexema.matches("[0-9]+"))
            return "dec";
        if (lexema.startsWith("\"") || lexema.startsWith("'") || lexema.startsWith("\u201C")
                || lexema.startsWith("\u00AB"))
            return "cadena";

        if (ms != null) {
            EntidadSimbolo s = null;
            if (niveles != null && !niveles.isEmpty()) {
                s = ms.buscarEnContexto(lexema, niveles);
            }
            if (s == null) {
                s = ms.buscarEnNivel(lexema, 0);
            }
            if (s == null) {
                s = buscarSimboloGlobal(ms, lexema);
            }
            if (s != null && s.getTipoDato() != null && !s.getTipoDato().isEmpty()) {
                if (s.getTipoDato().equalsIgnoreCase("void"))
                    return "void";
                return normalizeType(s.getTipoDato());
            }
        }
        return "variant";
    }

    private boolean esTipoNumerico(String t) {
        String n = normalizeType(t);
        return n.equals("dec") || n.equals("bin") || n.equals("oct") || n.equals("hex") || n.equals("real")
                || n.equals("exp");
    }

    private static class ValTipo {
        final String valor;
        final String tipo;

        ValTipo(String valor, String tipo) {
            this.valor = valor;
            this.tipo = tipo;
        }
    }

    private static class DefCtx {
        final String id;
        final int linea;
        final int defAmb;
        int bodyAmb;
        final String tipoDato;
        final boolean esFunc;
        int mainBraceDepth = -1;
        boolean hasReturn = false;

        DefCtx(String id, int linea, int defAmb, int bodyAmb, String tipoDato, boolean esFunc) {
            this.id = id;
            this.linea = linea;
            this.defAmb = defAmb;
            this.bodyAmb = bodyAmb;
            this.tipoDato = tipoDato;
            this.esFunc = esFunc;
        }
    }

    private static class SwitchCtx {
        String topeCase;
        String expectedTipo;
        final int braceDepth;

        SwitchCtx(String topeCase, String expectedTipo, int braceDepth) {
            this.topeCase = topeCase;
            this.expectedTipo = expectedTipo;
            this.braceDepth = braceDepth;
        }
    }

    private static class ExprAtom {
        final boolean isOp;
        final String op;
        final ValTipo valTipo;

        ExprAtom(String op) {
            this.isOp = true;
            this.op = op;
            this.valTipo = null;
        }

        ExprAtom(ValTipo vt) {
            this.isOp = false;
            this.op = null;
            this.valTipo = vt;
        }
    }

    private int getAmbitoToken(Tokens t, Map<Tokens, Integer> tokenAmbitos, Map<Integer, Integer> lineaAmbitos) {
        if (t == null)
            return 0;
        if (tokenAmbitos != null && tokenAmbitos.containsKey(t))
            return tokenAmbitos.get(t);
        if (lineaAmbitos != null && lineaAmbitos.containsKey(t.getLinea()))
            return lineaAmbitos.get(t.getLinea());
        return 0;
    }

    private Stack<Integer> getScopesToken(Tokens t, int amb, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Stack<Integer>> lineaScopeStacks) {
        Stack<Integer> scopes = null;
        if (t != null && tokenScopeStacks != null && tokenScopeStacks.containsKey(t)) {
            scopes = tokenScopeStacks.get(t);
        } else if (t != null && lineaScopeStacks != null && lineaScopeStacks.containsKey(t.getLinea())) {
            scopes = lineaScopeStacks.get(t.getLinea());
        }
        Stack<Integer> res = new Stack<>();
        if (scopes != null && !scopes.isEmpty()) {
            res.addAll(scopes);
        }
        if (!res.contains(0)) {
            res.insertElementAt(0, 0);
        }
        if (amb > 0 && !res.contains(amb)) {
            res.push(amb);
        }
        return res;
    }

    private void emitReglaSem2(int linea, int numRegla, String clave, String topePila, String valorReal,
            boolean aceptada, int ambito) {
        String estado = aceptada ? "Acept" : "ERROR";
        String desc = "CVE " + clave + " (" + topePila + " -> " + valorReal + ")";
        reglasSemanticas.add(new ReglaSemantica(linea, numRegla, clave, topePila, valorReal, desc, estado, valorReal,
                ambito));
        if (!aceptada) {
            erroresSemanticos.add(new ErroresSin(clave, "Semantica", valorReal, desc, linea));
        }
    }

    private void emitRegla1100(Tokens idTk, ManejadorSimbolos ms, Map<Tokens, Integer> tokenAmbitos,
            Map<Tokens, Stack<Integer>> tokenScopeStacks, Map<Integer, Integer> lineaAmbitos,
            Map<Integer, Stack<Integer>> lineaScopeStacks) {
        if (idTk == null)
            return;
        String lex = idTk.getLexema();
        int ln = idTk.getLinea();
        int amb = getAmbitoToken(idTk, tokenAmbitos, lineaAmbitos);
        Stack<Integer> scopes = getScopesToken(idTk, amb, tokenScopeStacks, lineaScopeStacks);

        EntidadSimbolo sym = (ms != null) ? ms.buscarEnContexto(lex, scopes) : null;
        if (sym == null && ms != null) {
            sym = ms.buscarEnNivel(lex, 0);
        }
        if (sym == null && ms != null) {
            sym = buscarSimboloGlobal(ms, lex);
        }
        boolean declarada = (sym != null);
        emitReglaSem2(ln, 10, "1100", "id", lex, declarada, amb);
    }

    private List<Tokens> stripOuterParens(List<Tokens> list) {
        while (list != null && list.size() >= 2 && list.get(0).getLexema().equals("(")
                && list.get(list.size() - 1).getLexema().equals(")")) {
            int depth = 0;
            boolean matchesEnd = false;
            for (int i = 0; i < list.size(); i++) {
                String lx = list.get(i).getLexema();
                if (lx.equals("("))
                    depth++;
                else if (lx.equals(")")) {
                    depth--;
                    if (depth == 0) {
                        if (i == list.size() - 1)
                            matchesEnd = true;
                        break;
                    }
                }
            }
            if (matchesEnd) {
                list = list.subList(1, list.size() - 1);
            } else {
                break;
            }
        }
        return list;
    }

    private ValTipo evaluarExpresionSem2(List<Tokens> rawExpr, ManejadorSimbolos ms,
            Map<Tokens, Integer> tokenAmbitos, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Integer> lineaAmbitos, Map<Integer, Stack<Integer>> lineaScopeStacks,
            Map<String, Integer> tempTypeCounts) {
        List<Tokens> expr = stripOuterParens(rawExpr);
        if (expr == null || expr.isEmpty()) {
            return new ValTipo("-", "variant");
        }

        Tokens firstTk = expr.get(0);
        int ln = firstTk.getLinea();
        int amb = getAmbitoToken(firstTk, tokenAmbitos, lineaAmbitos);
        Stack<Integer> scopes = getScopesToken(firstTk, amb, tokenScopeStacks, lineaScopeStacks);

        if (expr.size() == 1) {
            String lx = firstTk.getLexema();
            if ((firstTk.getTipo().startsWith("Identificador") || (esId(lx) && !esPalabraClave(lx)))
                    && !firstTk.getTipo().startsWith("Constante")
                    && !firstTk.getTipo().startsWith("Palabra")
                    && !esGlobal(lx)) {
                emitRegla1100(firstTk, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos, lineaScopeStacks);
            }
            return new ValTipo(lx, deducirTipoSem2(lx, ms, scopes));
        }

        if (!hasTopLevelBinaryOp(expr)) {
            String firstLex = firstTk.getLexema();
            if (firstLex.equals("++") || firstLex.equals("--") || firstLex.equals("!") || firstLex.equals("~")) {
                List<Tokens> sub = stripOuterParens(expr.subList(1, expr.size()));
                ValTipo subVt = evaluarExpresionSem2(sub, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos,
                        lineaScopeStacks, tempTypeCounts);
                if (firstLex.equals("++") || firstLex.equals("--")) {
                    boolean ok = esTipoEntero(subVt.tipo) || subVt.tipo.equals("variant");
                    emitReglaSem2(ln, 7, "1070", "TDec", subVt.valor, ok, amb);
                    return new ValTipo(subVt.valor, ok ? subVt.tipo : "variant");
                } else if (firstLex.equals("!")) {
                    boolean ok = subVt.tipo.equals("boolean") || subVt.tipo.equals("variant");
                    emitReglaSem2(ln, 7, "1070", "TBoolean", subVt.valor, ok, amb);
                    return new ValTipo(subVt.valor, "boolean");
                } else {
                    boolean ok = esTipoEntero(subVt.tipo) || subVt.tipo.equals("variant");
                    emitReglaSem2(ln, 7, "1070", "TDec/TBin/TOct/THex", subVt.valor, ok, amb);
                    return new ValTipo(subVt.valor, ok ? subVt.tipo : "variant");
                }
            }
            String lastLex = expr.get(expr.size() - 1).getLexema();
            if (lastLex.equals("++") || lastLex.equals("--")) {
                List<Tokens> sub = stripOuterParens(expr.subList(0, expr.size() - 1));
                ValTipo subVt = evaluarExpresionSem2(sub, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos,
                        lineaScopeStacks, tempTypeCounts);
                boolean ok = esTipoEntero(subVt.tipo) || subVt.tipo.equals("variant");
                emitReglaSem2(ln, 7, "1070", "TDec", subVt.valor, ok, amb);
                return new ValTipo(subVt.valor, ok ? subVt.tipo : "variant");
            }

            if (expr.size() >= 3
                    && (firstTk.getTipo().startsWith("Identificador")
                            || (esId(firstLex) && !esPalabraClave(firstLex)))
                    && expr.get(1).getLexema().equals("(")
                    && findMatchingCloseParen(expr, 1) == expr.size() - 1) {
                return evaluarLlamadaFuncionSem2(firstTk, expr.subList(2, expr.size() - 1), ms, tokenAmbitos,
                        tokenScopeStacks, lineaAmbitos, lineaScopeStacks, tempTypeCounts);
            }

            if (expr.size() >= 4
                    && (firstTk.getTipo().startsWith("Identificador")
                            || (esId(firstLex) && !esPalabraClave(firstLex)))
                    && expr.get(1).getLexema().equals("[")
                    && findMatchingCloseBracket(expr, 1) == expr.size() - 1) {
                return evaluarAccesoArregloSem2(firstTk, expr.subList(2, expr.size() - 1), ms, tokenAmbitos,
                        tokenScopeStacks, lineaAmbitos, lineaScopeStacks, tempTypeCounts);
            }
        }

        List<ExprAtom> atoms = new ArrayList<>();
        int idx = 0;
        while (idx < expr.size()) {
            Tokens tk = expr.get(idx);
            String lx = tk.getLexema();

            if (lx.equals("(")) {
                int closeIdx = findMatchingCloseParen(expr, idx);
                if (closeIdx > idx) {
                    ValTipo subVt = evaluarExpresionSem2(expr.subList(idx + 1, closeIdx), ms, tokenAmbitos,
                            tokenScopeStacks, lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                    atoms.add(new ExprAtom(subVt));
                    idx = closeIdx + 1;
                    continue;
                }
            }

            if ((lx.equals("!") || lx.equals("~") || lx.equals("++") || lx.equals("--"))
                    && (atoms.isEmpty() || atoms.get(atoms.size() - 1).isOp)) {
                int opEnd = idx + 1;
                if (opEnd < expr.size()) {
                    if (expr.get(opEnd).getLexema().equals("(")) {
                        int cParen = findMatchingCloseParen(expr, opEnd);
                        opEnd = (cParen > opEnd) ? (cParen + 1) : (opEnd + 1);
                    } else if (opEnd + 1 < expr.size()
                            && (expr.get(opEnd + 1).getLexema().equals("[")
                                    || expr.get(opEnd + 1).getLexema().equals("("))) {
                        if (expr.get(opEnd + 1).getLexema().equals("[")) {
                            int cBr = findMatchingCloseBracket(expr, opEnd + 1);
                            opEnd = (cBr > opEnd + 1) ? (cBr + 1) : (opEnd + 1);
                        } else {
                            int cPr = findMatchingCloseParen(expr, opEnd + 1);
                            opEnd = (cPr > opEnd + 1) ? (cPr + 1) : (opEnd + 1);
                        }
                    } else {
                        opEnd = idx + 2;
                    }
                    ValTipo uVt = evaluarExpresionSem2(expr.subList(idx, opEnd), ms, tokenAmbitos, tokenScopeStacks,
                            lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                    atoms.add(new ExprAtom(uVt));
                    idx = opEnd;
                    continue;
                }
            }

            if (isOperator(lx)) {
                atoms.add(new ExprAtom(lx));
                idx++;
                continue;
            }

            if ((tk.getTipo().startsWith("Identificador") || (esId(lx) && !esPalabraClave(lx)))
                    && idx + 1 < expr.size()) {
                String nextLx = expr.get(idx + 1).getLexema();
                if (nextLx.equals("(")) {
                    int closeIdx = findMatchingCloseParen(expr, idx + 1);
                    if (closeIdx > idx + 1) {
                        ValTipo fnVt = evaluarLlamadaFuncionSem2(tk, expr.subList(idx + 2, closeIdx), ms, tokenAmbitos,
                                tokenScopeStacks, lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                        atoms.add(new ExprAtom(fnVt));
                        idx = closeIdx + 1;
                        continue;
                    }
                } else if (nextLx.equals("[")) {
                    int closeIdx = findMatchingCloseBracket(expr, idx + 1);
                    if (closeIdx > idx + 1) {
                        ValTipo arrVt = evaluarAccesoArregloSem2(tk, expr.subList(idx + 2, closeIdx), ms, tokenAmbitos,
                                tokenScopeStacks, lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                        atoms.add(new ExprAtom(arrVt));
                        idx = closeIdx + 1;
                        continue;
                    }
                }
            }

            ValTipo singleVt = evaluarExpresionSem2(Collections.singletonList(tk), ms, tokenAmbitos, tokenScopeStacks,
                    lineaAmbitos, lineaScopeStacks, tempTypeCounts);
            atoms.add(new ExprAtom(singleVt));
            idx++;
        }

        Stack<String> opStack = new Stack<>();
        List<ExprAtom> postfix = new ArrayList<>();
        for (ExprAtom atom : atoms) {
            if (atom.isOp) {
                while (!opStack.isEmpty() && precedencia(opStack.peek()) >= precedencia(atom.op)) {
                    postfix.add(new ExprAtom(opStack.pop()));
                }
                opStack.push(atom.op);
            } else {
                postfix.add(atom);
            }
        }
        while (!opStack.isEmpty()) {
            postfix.add(new ExprAtom(opStack.pop()));
        }

        Stack<ValTipo> evalStack = new Stack<>();
        for (ExprAtom atom : postfix) {
            if (atom.isOp) {
                ValTipo right = evalStack.isEmpty() ? new ValTipo("-", "variant") : evalStack.pop();
                ValTipo left = evalStack.isEmpty() ? new ValTipo("-", "variant") : evalStack.pop();
                String resTipo = resolverTipoOperacion(left.tipo, atom.op, right.tipo);
                if ("error".equals(resTipo)) {
                    resTipo = "variant";
                }
                String tName = formatTSem2(resTipo);
                int cnt = tempTypeCounts.getOrDefault(tName, 0) + 1;
                tempTypeCounts.put(tName, cnt);
                evalStack.push(new ValTipo(tName + cnt, resTipo));
            } else {
                evalStack.push(atom.valTipo);
            }
        }

        return evalStack.isEmpty() ? new ValTipo("-", "variant") : evalStack.pop();
    }

    private boolean hasTopLevelBinaryOp(List<Tokens> expr) {
        int pDepth = 0;
        int bDepth = 0;
        for (int i = 0; i < expr.size(); i++) {
            String lx = expr.get(i).getLexema();
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
            } else if (pDepth == 0 && bDepth == 0 && i > 0) {
                if (isOperator(lx) && !lx.equals("!") && !lx.equals("~")) {
                    return true;
                }
            }
        }
        return false;
    }

    private int findMatchingCloseParen(List<Tokens> list, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < list.size(); i++) {
            String lx = list.get(i).getLexema();
            if (lx.equals("("))
                depth++;
            else if (lx.equals(")")) {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return -1;
    }

    private int findMatchingCloseBracket(List<Tokens> list, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < list.size(); i++) {
            String lx = list.get(i).getLexema();
            if (lx.equals("["))
                depth++;
            else if (lx.equals("]")) {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return -1;
    }

    private ValTipo evaluarLlamadaFuncionSem2(Tokens funTk, List<Tokens> insideArgs, ManejadorSimbolos ms,
            Map<Tokens, Integer> tokenAmbitos, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Integer> lineaAmbitos, Map<Integer, Stack<Integer>> lineaScopeStacks,
            Map<String, Integer> tempTypeCounts) {
        String funId = funTk.getLexema();
        int ln = funTk.getLinea();
        int amb = getAmbitoToken(funTk, tokenAmbitos, lineaAmbitos);
        Stack<Integer> scopes = getScopesToken(funTk, amb, tokenScopeStacks, lineaScopeStacks);

        List<List<Tokens>> argTokensList = separarIndices(insideArgs);
        if (!esGlobal(funId)) {
            emitRegla1100(funTk, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos, lineaScopeStacks);
        }

        List<ValTipo> argVts = new ArrayList<>();
        for (List<Tokens> argTks : argTokensList) {
            if (!argTks.isEmpty()) {
                argVts.add(evaluarExpresionSem2(argTks, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos,
                        lineaScopeStacks, tempTypeCounts));
            }
        }

        if (!esGlobal(funId)) {
            List<EntidadSimbolo> declParams = obtenerParametrosSubrutina(ms, funId);
            EntidadSimbolo symSub = buscarSubrutina(ms, funId, scopes);
            if (symSub != null) {
                int m = declParams.size();
                int n = argVts.size();
                int max = Math.max(m, n);
                for (int k = 0; k < max; k++) {
                    if (k < m && k < n) {
                        EntidadSimbolo pSym = declParams.get(k);
                        ValTipo aVt = argVts.get(k);
                        emitReglaSem2(ln, 15, "1150", pSym.getIdentificador(), aVt.valor, true, amb);
                        String pTipo = normalizeType(pSym.getTipoDato());
                        boolean comp = esCompatibleAsignacion(pTipo, aVt.tipo) || pTipo.equals(aVt.tipo);
                        emitReglaSem2(ln, 16, "1160", formatTSem2(pTipo), aVt.valor, comp, amb);
                    } else if (k < m) {
                        EntidadSimbolo pSym = declParams.get(k);
                        emitReglaSem2(ln, 15, "1150", pSym.getIdentificador(), "-", false, amb);
                    } else {
                        ValTipo aVt = argVts.get(k);
                        emitReglaSem2(ln, 15, "1150", "-", aVt.valor, false, amb);
                    }
                }
            }
        }

        return new ValTipo(funId, deducirTipoSem2(funId, ms, scopes));
    }

    private ValTipo evaluarAccesoArregloSem2(Tokens arrTk, List<Tokens> insideBrackets, ManejadorSimbolos ms,
            Map<Tokens, Integer> tokenAmbitos, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Integer> lineaAmbitos, Map<Integer, Stack<Integer>> lineaScopeStacks,
            Map<String, Integer> tempTypeCounts) {
        String arrId = arrTk.getLexema();
        int ln = arrTk.getLinea();
        int amb = getAmbitoToken(arrTk, tokenAmbitos, lineaAmbitos);
        Stack<Integer> scopes = getScopesToken(arrTk, amb, tokenScopeStacks, lineaScopeStacks);

        emitRegla1100(arrTk, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos, lineaScopeStacks);

        List<List<Tokens>> indicesList = separarIndices(insideBrackets);
        List<ValTipo> idxVts = new ArrayList<>();
        for (List<Tokens> idxTks : indicesList) {
            idxVts.add(evaluarExpresionSem2(idxTks, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos, lineaScopeStacks,
                    tempTypeCounts));
        }

        EntidadSimbolo sym = (ms != null) ? ms.buscarEnContexto(arrId, scopes) : null;
        if (sym == null && ms != null) {
            sym = ms.buscarEnNivel(arrId, 0);
        }
        if (sym == null && ms != null) {
            sym = buscarSimboloGlobal(ms, arrId);
        }

        if (sym != null && "arreglo".equalsIgnoreCase(sym.getClasificacion())) {
            int dimEsperada = 1;
            try {
                dimEsperada = Integer.parseInt(sym.getDimensionArreglo());
            } catch (Exception e) {
            }
            String[] sizes = sym.getTipoArreglo().split(",");
            int dimUsada = indicesList.size();
            String topeDim = "Dim " + dimEsperada;

            int maxDim = Math.max(dimEsperada, dimUsada);
            for (int k = 0; k < maxDim; k++) {
                if (k < dimEsperada && k < dimUsada) {
                    emitReglaSem2(ln, 3, "1030", topeDim, arrId, true, amb);
                    ValTipo idxVt = idxVts.get(k);
                    boolean esEntero = esTipoEntero(idxVt.tipo);
                    emitReglaSem2(ln, 4, "1040", "TDec", idxVt.valor, esEntero, amb);
                    if (esEntero && k < sizes.length) {
                        int maxTam = 0;
                        try {
                            maxTam = Integer.parseInt(sizes[k].trim());
                        } catch (Exception e) {
                        }
                        String rangoStr = "0.." + (maxTam - 1);
                        Integer valConst = parsearConstanteEntera(indicesList.get(k));
                        boolean enRango = (valConst == null) || (valConst >= 0 && valConst < maxTam);
                        emitReglaSem2(ln, 5, "1050", rangoStr, idxVt.valor, enRango, amb);
                    }
                } else {
                    emitReglaSem2(ln, 3, "1030", topeDim, arrId, false, amb);
                }
            }
        }

        return new ValTipo(arrId, deducirTipoSem2(arrId, ms, scopes));
    }

    private void procesarAsignacionSem2(List<Tokens> stmtTokens, int asigIdx, ManejadorSimbolos ms,
            Map<Tokens, Integer> tokenAmbitos, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Integer> lineaAmbitos, Map<Integer, Stack<Integer>> lineaScopeStacks,
            Map<String, Integer> tempTypeCounts) {
        if (asigIdx <= 0 || asigIdx >= stmtTokens.size() - 1)
            return;
        List<Tokens> lhsTokens = stmtTokens.subList(0, asigIdx);
        Tokens opTk = stmtTokens.get(asigIdx);
        String op = opTk.getLexema();
        List<Tokens> rhsTokens = stmtTokens.subList(asigIdx + 1, stmtTokens.size());

        Tokens firstLhs = lhsTokens.get(0);
        String baseId = firstLhs.getLexema();
        int ln = opTk.getLinea();
        int amb = getAmbitoToken(firstLhs, tokenAmbitos, lineaAmbitos);
        Stack<Integer> scopes = getScopesToken(firstLhs, amb, tokenScopeStacks, lineaScopeStacks);

        if (lhsTokens.size() >= 4 && lhsTokens.get(1).getLexema().equals("[")
                && findMatchingCloseBracket(lhsTokens, 1) == lhsTokens.size() - 1) {
            evaluarAccesoArregloSem2(firstLhs, lhsTokens.subList(2, lhsTokens.size() - 1), ms, tokenAmbitos,
                    tokenScopeStacks, lineaAmbitos, lineaScopeStacks, tempTypeCounts);
        } else {
            emitRegla1100(firstLhs, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos, lineaScopeStacks);
        }

        EntidadSimbolo symDest = (ms != null) ? ms.buscarEnContexto(baseId, scopes) : null;
        if (symDest == null && ms != null) {
            symDest = ms.buscarEnNivel(baseId, 0);
        }
        if (symDest == null && ms != null) {
            symDest = buscarSimboloGlobal(ms, baseId);
        }
        boolean esValido1090;
        if (symDest == null) {
            esValido1090 = true;
        } else {
            String clasif = symDest.getClasificacion() != null ? symDest.getClasificacion().toLowerCase() : "";
            esValido1090 = clasif.equals("var") || clasif.equals("variable") || clasif.equals("param")
                    || clasif.equals("par") || clasif.equals("arreglo") || clasif.equals("arr");
        }
        emitReglaSem2(ln, 9, "1090", "Var/Par/Arr", baseId, esValido1090, amb);

        ValTipo rhsVt = evaluarExpresionSem2(rhsTokens, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos,
                lineaScopeStacks, tempTypeCounts);

        String tipoDest = deducirTipoSem2(baseId, ms, scopes);
        String clave = "1020";
        boolean valida;
        if (op.equals("=")) {
            clave = "1020";
            valida = esCompatibleAsignacion(tipoDest, rhsVt.tipo);
        } else if (op.equals("+=")) {
            clave = "1021";
            if (tipoDest.equals("variant") || rhsVt.tipo.equals("variant")) {
                valida = true;
            } else {
                String res = resolverTipoOperacion(tipoDest, "+", rhsVt.tipo);
                valida = !"error".equals(res) && esCompatibleAsignacion(tipoDest, res);
            }
        } else {
            clave = "1022";
            String binOp = op.substring(0, 1);
            if (tipoDest.equals("variant") || rhsVt.tipo.equals("variant")) {
                valida = true;
            } else {
                String res = resolverTipoOperacion(tipoDest, binOp, rhsVt.tipo);
                valida = !"error".equals(res) && (esCompatibleAsignacion(tipoDest, res)
                        || (binOp.equals("/") && esTipoNumerico(tipoDest) && esTipoNumerico(rhsVt.tipo)));
            }
        }
        emitReglaSem2(ln, 2, clave, formatTSem2(tipoDest), rhsVt.valor, valida, amb);
    }

    private int findTopLevelAssignment(List<Tokens> list) {
        int pDepth = 0;
        int bDepth = 0;
        for (int i = 0; i < list.size(); i++) {
            String lx = list.get(i).getLexema();
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
            } else if (pDepth == 0 && bDepth == 0) {
                if (lx.equals("=") || lx.equals("+=") || lx.equals("-=") || lx.equals("*=") || lx.equals("/=")) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void evaluarReglasSemanticas(List<Tokens> tokens, ManejadorSimbolos ms,
            Map<Tokens, Integer> tokenAmbitos, Map<Tokens, Stack<Integer>> tokenScopeStacks,
            Map<Integer, Integer> lineaAmbitos, Map<Integer, Stack<Integer>> lineaScopeStacks) {
        reglasSemanticas.clear();
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        Map<String, Integer> tempTypeCounts = new HashMap<>();
        Stack<Integer> doStack = new Stack<>();
        Stack<SwitchCtx> switchStack = new Stack<>();
        DefCtx currentDef = null;
        int braceDepth = 0;
        boolean inExecutableBody = false;

        for (int i = 0; i < tokens.size(); i++) {
            Tokens t = tokens.get(i);
            String lex = t.getLexema();
            int ln = t.getLinea();
            int amb = getAmbitoToken(t, tokenAmbitos, lineaAmbitos);
            Stack<Integer> scopes = getScopesToken(t, amb, tokenScopeStacks, lineaScopeStacks);

            if (lex.equals("{")) {
                braceDepth++;
                continue;
            }
            if (lex.equals("}")) {
                if (!switchStack.isEmpty() && switchStack.peek().braceDepth == braceDepth) {
                    switchStack.pop();
                }
                if (currentDef != null && currentDef.mainBraceDepth == braceDepth) {
                    if (!currentDef.hasReturn) {
                        if (currentDef.esFunc) {
                            emitReglaSem2(currentDef.linea, 13, "1130", "return", currentDef.id, false,
                                    currentDef.bodyAmb);
                        } else {
                            emitReglaSem2(currentDef.linea, 14, "1140", "TVoid", currentDef.id, true,
                                    currentDef.bodyAmb);
                        }
                    }
                    currentDef = null;
                    inExecutableBody = false;
                } else if (braceDepth == 1 && currentDef == null) {
                    inExecutableBody = false;
                }
                if (braceDepth > 0) {
                    braceDepth--;
                }
                continue;
            }
            if (lex.equals(";") || lex.equalsIgnoreCase("else") || lex.equalsIgnoreCase("break")) {
                continue;
            }

            if (lex.equalsIgnoreCase("var") || lex.equalsIgnoreCase("reg")) {
                int v = i + 1;
                while (v < tokens.size()) {
                    String clx = tokens.get(v).getLexema();
                    if (clx.equals(";") || clx.equalsIgnoreCase("main") || clx.equalsIgnoreCase("def")
                            || clx.equals("{")) {
                        break;
                    }
                    v++;
                }
                i = (v < tokens.size() && tokens.get(v).getLexema().equals(";")) ? v : (v - 1);
                continue;
            }

            if (lex.equalsIgnoreCase("def")) {
                if (i + 1 < tokens.size()) {
                    Tokens idTk = tokens.get(i + 1);
                    String defId = idTk.getLexema();
                    int defLn = idTk.getLinea();
                    int defAmb = getAmbitoToken(idTk, tokenAmbitos, lineaAmbitos);
                    String tipoRet = determinarTipo(defId);
                    boolean esFunc = !tipoRet.equals("void");
                    if (esFunc) {
                        emitReglaSem2(defLn, 12, "1120", "func", defId, true, defAmb);
                    } else {
                        emitReglaSem2(defLn, 11, "1110", "proc", defId, true, defAmb);
                    }

                    int p = i + 2;
                    int bodyAmb = defAmb;
                    if (p < tokens.size() && tokens.get(p).getLexema().equals("(")) {
                        int closeP = findMatchingCloseParen(tokens, p);
                        if (closeP > p) {
                            for (int k = p + 1; k < closeP; k++) {
                                int a = getAmbitoToken(tokens.get(k), tokenAmbitos, lineaAmbitos);
                                if (a > 0) {
                                    bodyAmb = a;
                                    break;
                                }
                            }
                            p = closeP + 1;
                        }
                    }
                    for (int k = p; k < tokens.size(); k++) {
                        String klx = tokens.get(k).getLexema();
                        int a = getAmbitoToken(tokens.get(k), tokenAmbitos, lineaAmbitos);
                        if (a > 0 && bodyAmb == defAmb) {
                            bodyAmb = a;
                        }
                        if (klx.equals("{")) {
                            int bAmb = getAmbitoToken(tokens.get(k), tokenAmbitos, lineaAmbitos);
                            if (bAmb > 0) {
                                bodyAmb = bAmb;
                            }
                            break;
                        }
                    }
                    currentDef = new DefCtx(defId, defLn, defAmb, bodyAmb, tipoRet, esFunc);
                    i = p - 1;
                }
                continue;
            }

            if (lex.equalsIgnoreCase("main")) {
                int p = i + 1;
                if (p < tokens.size() && tokens.get(p).getLexema().equals("(")) {
                    int closeP = findMatchingCloseParen(tokens, p);
                    if (closeP > p) {
                        p = closeP + 1;
                    }
                }
                if (p < tokens.size() && tokens.get(p).getLexema().equals("{")) {
                    braceDepth++;
                    inExecutableBody = true;
                    if (currentDef != null) {
                        currentDef.mainBraceDepth = braceDepth;
                        int mAmb = getAmbitoToken(tokens.get(p), tokenAmbitos, lineaAmbitos);
                        if (mAmb > 0) {
                            currentDef.bodyAmb = mAmb;
                        }
                    }
                    i = p;
                } else {
                    i = p - 1;
                }
                continue;
            }

            if (!inExecutableBody) {
                int sEnd = i;
                while (sEnd < tokens.size() && !tokens.get(sEnd).getLexema().equals(";")
                        && !tokens.get(sEnd).getLexema().equalsIgnoreCase("var")
                        && !tokens.get(sEnd).getLexema().equalsIgnoreCase("def")
                        && !tokens.get(sEnd).getLexema().equalsIgnoreCase("main")) {
                    sEnd++;
                }
                i = (sEnd < tokens.size() && tokens.get(sEnd).getLexema().equals(";")) ? sEnd : (sEnd - 1);
                continue;
            }

            if (lex.equalsIgnoreCase("do")) {
                doStack.push(i);
                continue;
            }

            if (lex.equalsIgnoreCase("if") || lex.equalsIgnoreCase("elseif") || lex.equalsIgnoreCase("while")) {
                String clave = "1010";
                if (lex.equalsIgnoreCase("elseif")
                        || (lex.equalsIgnoreCase("if") && i > 0 && tokens.get(i - 1).getLexema().equalsIgnoreCase("else"))) {
                    clave = "1011";
                } else if (lex.equalsIgnoreCase("while")) {
                    boolean isDoWhile = false;
                    if (!doStack.isEmpty() && i + 1 < tokens.size() && tokens.get(i + 1).getLexema().equals("(")) {
                        int closeP = findMatchingCloseParen(tokens, i + 1);
                        if (closeP > i + 1) {
                            int afterP = closeP + 1;
                            if (afterP >= tokens.size()
                                    || tokens.get(afterP).getLexema().equals(";")
                                    || tokens.get(afterP).getLexema().equals("}")
                                    || tokens.get(afterP).getLexema().equalsIgnoreCase("else")
                                    || tokens.get(afterP).getLexema().equalsIgnoreCase("elseif")) {
                                isDoWhile = true;
                            }
                        }
                    }
                    if (isDoWhile) {
                        doStack.pop();
                        clave = "1013";
                    } else {
                        clave = "1012";
                    }
                }

                if (i + 1 < tokens.size() && tokens.get(i + 1).getLexema().equals("(")) {
                    int closeP = findMatchingCloseParen(tokens, i + 1);
                    if (closeP > i + 2) {
                        List<Tokens> condExpr = tokens.subList(i + 2, closeP);
                        ValTipo condVt = evaluarExpresionSem2(condExpr, ms, tokenAmbitos, tokenScopeStacks,
                                lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                        boolean esLogica = condVt.tipo.equals("boolean") || condVt.tipo.equals("variant");
                        emitReglaSem2(ln, 1, clave, "TBoolean", condVt.valor, esLogica, amb);
                        i = closeP;
                        continue;
                    }
                }
            }

            if (lex.equalsIgnoreCase("switch")) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).getLexema().equals("(")) {
                    int closeP = findMatchingCloseParen(tokens, i + 1);
                    if (closeP > i + 2) {
                        List<Tokens> swExpr = tokens.subList(i + 2, closeP);
                        ValTipo swVt = evaluarExpresionSem2(swExpr, ms, tokenAmbitos, tokenScopeStacks,
                                lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                        boolean ok1060 = swVt.tipo.equals("dec") || swVt.tipo.equals("cadena")
                                || swVt.tipo.equals("variant");
                        emitReglaSem2(ln, 6, "1060", "TDec/TCadena", swVt.valor, ok1060, amb);

                        String topeCase;
                        String expectedTipo;
                        if (swVt.tipo.equals("dec")) {
                            topeCase = "TDec";
                            expectedTipo = "dec";
                        } else if (swVt.tipo.equals("cadena")) {
                            topeCase = "TCadena";
                            expectedTipo = "cadena";
                        } else {
                            topeCase = "TDec/TCadena";
                            expectedTipo = "any_dec_or_cadena";
                        }

                        int bIdx = closeP + 1;
                        while (bIdx < tokens.size() && !tokens.get(bIdx).getLexema().equals("{")) {
                            bIdx++;
                        }
                        if (bIdx < tokens.size() && tokens.get(bIdx).getLexema().equals("{")) {
                            braceDepth++;
                            switchStack.push(new SwitchCtx(topeCase, expectedTipo, braceDepth));
                            i = bIdx;
                        } else {
                            i = closeP;
                        }
                        continue;
                    }
                }
            }

            if (lex.equalsIgnoreCase("case")) {
                int cEnd = i + 1;
                int pD = 0;
                int bD = 0;
                while (cEnd < tokens.size()) {
                    String clx = tokens.get(cEnd).getLexema();
                    if (clx.equals("("))
                        pD++;
                    else if (clx.equals(")")) {
                        if (pD > 0)
                            pD--;
                    } else if (clx.equals("["))
                        bD++;
                    else if (clx.equals("]")) {
                        if (bD > 0)
                            bD--;
                    } else if (pD == 0 && bD == 0 && clx.equals(":")) {
                        break;
                    }
                    cEnd++;
                }
                if (cEnd > i + 1) {
                    List<Tokens> caseExpr = tokens.subList(i + 1, cEnd);
                    ValTipo caseVt = evaluarExpresionSem2(caseExpr, ms, tokenAmbitos, tokenScopeStacks,
                            lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                    if (!switchStack.isEmpty()) {
                        SwitchCtx swCtx = switchStack.peek();
                        boolean ok1061;
                        if (swCtx.expectedTipo.equals("dec")) {
                            ok1061 = caseVt.tipo.equals("dec") || caseVt.tipo.equals("variant");
                        } else if (swCtx.expectedTipo.equals("cadena")) {
                            ok1061 = caseVt.tipo.equals("cadena") || caseVt.tipo.equals("variant");
                        } else {
                            ok1061 = caseVt.tipo.equals("dec") || caseVt.tipo.equals("cadena")
                                    || caseVt.tipo.equals("variant");
                        }
                        emitReglaSem2(ln, 6, "1061", swCtx.topeCase, caseVt.valor, ok1061, amb);
                    } else {
                        emitReglaSem2(ln, 6, "1061", "TDec/TCadena", caseVt.valor, false, amb);
                    }
                }
                i = (cEnd < tokens.size()) ? cEnd : (cEnd - 1);
                continue;
            }

            if (lex.equalsIgnoreCase("default")) {
                int dEnd = i + 1;
                while (dEnd < tokens.size() && !tokens.get(dEnd).getLexema().equals(":")) {
                    dEnd++;
                }
                i = (dEnd < tokens.size()) ? dEnd : (dEnd - 1);
                continue;
            }

            if (lex.equalsIgnoreCase("for")) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).getLexema().equals("(")) {
                    int closeP = findMatchingCloseParen(tokens, i + 1);
                    if (closeP > i + 2) {
                        List<Tokens> insideFor = tokens.subList(i + 2, closeP);
                        int ambFor = getAmbitoToken(insideFor.get(0), tokenAmbitos, lineaAmbitos);
                        Stack<Integer> scopesFor = getScopesToken(insideFor.get(0), ambFor, tokenScopeStacks,
                                lineaScopeStacks);

                        int colonIdx = -1;
                        int curPDepth = 0;
                        int curBDepth = 0;
                        for (int k = 0; k < insideFor.size(); k++) {
                            String lx = insideFor.get(k).getLexema();
                            if (lx.equals("("))
                                curPDepth++;
                            else if (lx.equals(")")) {
                                if (curPDepth > 0)
                                    curPDepth--;
                            } else if (lx.equals("["))
                                curBDepth++;
                            else if (lx.equals("]")) {
                                if (curBDepth > 0)
                                    curBDepth--;
                            } else if (lx.equals(":") && curPDepth == 0 && curBDepth == 0) {
                                colonIdx = k;
                                break;
                            }
                        }

                        if (colonIdx != -1) {
                            List<Tokens> leftTokens = insideFor.subList(0, colonIdx);
                            List<Tokens> rightTokens = insideFor.subList(colonIdx + 1, insideFor.size());
                            String iterId = leftTokens.isEmpty() ? "-"
                                    : leftTokens.get(leftTokens.size() - 1).getLexema();
                            ValTipo colVt = evaluarExpresionSem2(rightTokens, ms, tokenAmbitos, tokenScopeStacks,
                                    lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                            String colId = colVt.valor;

                            EntidadSimbolo symCol = (ms != null) ? ms.buscarEnContexto(colId, scopesFor) : null;
                            if (symCol == null && ms != null) {
                                symCol = ms.buscarEnNivel(colId, 0);
                            }
                            if (symCol == null && ms != null) {
                                symCol = buscarSimboloGlobal(ms, colId);
                            }

                            boolean esArreglo = (symCol != null
                                    && "arreglo".equalsIgnoreCase(symCol.getClasificacion()));
                            String tipoCol = colVt.tipo;
                            boolean esCadena = tipoCol.equals("cadena") || colId.startsWith("\"")
                                    || colId.startsWith("'");
                            String tipoIter = deducirTipoSem2(iterId, ms, scopesFor);
                            if ("variant".equals(tipoIter) && !iterId.isEmpty()) {
                                String byPrefix = determinarTipo(iterId);
                                if (!"void".equals(byPrefix)) {
                                    tipoIter = normalizeType(byPrefix);
                                }
                            }

                            if (esCadena && !esArreglo) {
                                boolean ok1083 = esTipoEntero(tipoIter) || tipoIter.equals("variant");
                                emitReglaSem2(ln, 8, "1083", "TDec", iterId, ok1083, ambFor);
                            } else {
                                String topeArr = "Arr " + formatTSem2(tipoIter);
                                boolean ok1084;
                                if (esArreglo) {
                                    String tipoElem = normalizeType(symCol.getTipoDato());
                                    ok1084 = tipoIter.equals(tipoElem) || tipoIter.equals("variant")
                                            || tipoElem.equals("variant")
                                            || esCompatibleAsignacion(tipoIter, tipoElem);
                                } else {
                                    ok1084 = tipoCol.equals("variant");
                                }
                                emitReglaSem2(ln, 8, "1084", topeArr, colId, ok1084, ambFor);
                            }
                        } else {
                            List<List<Tokens>> parts = new ArrayList<>();
                            List<Tokens> currentPart = new ArrayList<>();
                            curPDepth = 0;
                            curBDepth = 0;
                            for (Tokens tk : insideFor) {
                                String lx = tk.getLexema();
                                if (lx.equals("("))
                                    curPDepth++;
                                else if (lx.equals(")")) {
                                    if (curPDepth > 0)
                                        curPDepth--;
                                } else if (lx.equals("["))
                                    curBDepth++;
                                else if (lx.equals("]")) {
                                    if (curBDepth > 0)
                                        curBDepth--;
                                }
                                if (lx.equals(";") && curPDepth == 0 && curBDepth == 0) {
                                    parts.add(new ArrayList<>(currentPart));
                                    currentPart.clear();
                                } else {
                                    currentPart.add(tk);
                                }
                            }
                            parts.add(currentPart);

                            // Part 1: 1080
                            List<Tokens> part1 = parts.size() > 0 ? parts.get(0) : Collections.emptyList();
                            boolean esAsig = false;
                            String initId = "-";
                            for (Tokens tk : part1) {
                                String lx = tk.getLexema();
                                if (initId.equals("-") && !lx.equalsIgnoreCase("var")
                                        && (tk.getTipo().startsWith("Identificador") || (esId(lx) && !esPalabraClave(lx)))) {
                                    initId = lx;
                                }
                                if (lx.equals("=") || lx.equals("+=") || lx.equals("-=") || lx.equals("*=")
                                        || lx.equals("/=")) {
                                    esAsig = true;
                                }
                            }
                            emitReglaSem2(ln, 8, "1080", "id", initId, esAsig, ambFor);

                            // Part 2: 1081
                            List<Tokens> part2 = parts.size() > 1 ? parts.get(1) : Collections.emptyList();
                            if (!part2.isEmpty()) {
                                ValTipo condVt = evaluarExpresionSem2(part2, ms, tokenAmbitos, tokenScopeStacks,
                                        lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                                boolean ok1081 = condVt.tipo.equals("boolean") || condVt.tipo.equals("variant");
                                emitReglaSem2(ln, 8, "1081", "TBoolean", condVt.valor, ok1081, ambFor);
                            } else {
                                emitReglaSem2(ln, 8, "1081", "TBoolean", "-", false, ambFor);
                            }

                            // Part 3: 1082
                            List<Tokens> part3 = parts.size() > 2 ? parts.get(2) : Collections.emptyList();
                            if (!part3.isEmpty()) {
                                int asigIdx = findTopLevelAssignment(part3);
                                if (asigIdx != -1) {
                                    procesarAsignacionSem2(part3, asigIdx, ms, tokenAmbitos, tokenScopeStacks,
                                            lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                                    String targetId = part3.get(0).getLexema();
                                    emitReglaSem2(ln, 8, "1082", "++/--", targetId, false, ambFor);
                                } else {
                                    ValTipo stepVt = evaluarExpresionSem2(part3, ms, tokenAmbitos, tokenScopeStacks,
                                            lineaAmbitos, lineaScopeStacks, tempTypeCounts);
                                    boolean hasIncDec = false;
                                    for (Tokens tk : part3) {
                                        String lx = tk.getLexema();
                                        if (lx.equals("++") || lx.equals("--")) {
                                            hasIncDec = true;
                                            break;
                                        }
                                    }
                                    emitReglaSem2(ln, 8, "1082", "++/--", stepVt.valor, hasIncDec, ambFor);
                                }
                            } else {
                                emitReglaSem2(ln, 8, "1082", "++/--", "-", false, ambFor);
                            }
                        }

                        i = closeP;
                        continue;
                    }
                }
            }

            if (lex.equalsIgnoreCase("return")) {
                int rEnd = i + 1;
                int pD = 0;
                int bD = 0;
                while (rEnd < tokens.size()) {
                    String rlx = tokens.get(rEnd).getLexema();
                    if (rlx.equals("("))
                        pD++;
                    else if (rlx.equals(")")) {
                        if (pD > 0)
                            pD--;
                    } else if (rlx.equals("["))
                        bD++;
                    else if (rlx.equals("]")) {
                        if (bD > 0)
                            bD--;
                    } else if (pD == 0 && bD == 0 && (rlx.equals(";") || rlx.equals("}")
                            || rlx.equalsIgnoreCase("else") || rlx.equalsIgnoreCase("elseif")
                            || rlx.equalsIgnoreCase("case") || rlx.equalsIgnoreCase("default")
                            || rlx.equalsIgnoreCase("break"))) {
                        break;
                    }
                    rEnd++;
                }
                List<Tokens> retExpr = (rEnd > i + 1) ? tokens.subList(i + 1, rEnd) : Collections.emptyList();
                ValTipo retVt = null;
                if (!retExpr.isEmpty()) {
                    retVt = evaluarExpresionSem2(retExpr, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos,
                            lineaScopeStacks, tempTypeCounts);
                }
                if (currentDef != null) {
                    currentDef.hasReturn = true;
                    if (currentDef.esFunc) {
                        if (retVt != null) {
                            emitReglaSem2(ln, 13, "1130", "return", currentDef.id, true, amb);
                            String fTipo = normalizeType(currentDef.tipoDato);
                            boolean comp = esCompatibleAsignacion(fTipo, retVt.tipo) || fTipo.equals(retVt.tipo);
                            emitReglaSem2(ln, 13, "1131", formatTSem2(fTipo), retVt.valor, comp, amb);
                        } else {
                            emitReglaSem2(ln, 13, "1130", "return", currentDef.id, false, amb);
                        }
                    } else {
                        if (retVt != null) {
                            emitReglaSem2(ln, 14, "1140", "TVoid", retVt.valor, false, amb);
                        } else {
                            emitReglaSem2(ln, 14, "1140", "TVoid", currentDef.id, true, amb);
                        }
                    }
                }
                i = rEnd - 1;
                continue;
            }

            // General statement (assignment, unary operation, function/procedure call)
            int stmtEnd = i;
            int pD = 0;
            int bD = 0;
            while (stmtEnd < tokens.size()) {
                String slx = tokens.get(stmtEnd).getLexema();
                if (slx.equals("("))
                    pD++;
                else if (slx.equals(")")) {
                    if (pD > 0)
                        pD--;
                } else if (slx.equals("["))
                    bD++;
                else if (slx.equals("]")) {
                    if (bD > 0)
                        bD--;
                } else if (pD == 0 && bD == 0) {
                    if (slx.equals(";") || slx.equals("{") || slx.equals("}")
                            || slx.equalsIgnoreCase("else") || slx.equalsIgnoreCase("elseif")
                            || slx.equalsIgnoreCase("case") || slx.equalsIgnoreCase("default")
                            || slx.equalsIgnoreCase("break")
                            || (slx.equalsIgnoreCase("while") && !doStack.isEmpty())) {
                        break;
                    }
                }
                stmtEnd++;
            }

            if (stmtEnd > i) {
                List<Tokens> stmtTokens = tokens.subList(i, stmtEnd);
                int asigIdx = findTopLevelAssignment(stmtTokens);
                if (asigIdx != -1) {
                    procesarAsignacionSem2(stmtTokens, asigIdx, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos,
                            lineaScopeStacks, tempTypeCounts);
                } else {
                    evaluarExpresionSem2(stmtTokens, ms, tokenAmbitos, tokenScopeStacks, lineaAmbitos,
                            lineaScopeStacks, tempTypeCounts);
                }
                i = stmtEnd - 1;
            }
        }
    }
}
