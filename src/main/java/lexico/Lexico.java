package lexico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Lexico {

    private String input;
    private int pos;
    private int linea;
    private final List<Tokens> tokens = new ArrayList<>();
    private final List<ErroresLex> errors = new ArrayList<>();
    private final Map<String, Integer> contadores = new LinkedHashMap<>();
    private static final Set<String> PALABRAS_RESERVADAS = new LinkedHashSet<>(Arrays.asList(
            "reg", "var", "def", "main", "int", "float", "bool", "char", "string",
            "read", "write", "println", "true", "false", "null",
            "console.read", "console.log", "console", "log", "if", "elseif", "else", "while", "do", "return", "for",
            "switch", "case", "break", "default",
            "forEach", "continue", "let", "const", "undefined",
            "interface", "typeof", "any", "set", "get", "class",
            "toLowerCase", "toUpperCase", "length", "trim", "charAt",
            "startsWith", "endsWith", "indexOf", "includes", "slice",
            "replace", "split", "push", "shift", "in", "of", "splice",
            "concat", "copy", "val", "str", "sin", "cos", "tan", "chr",
            "pred", "succ", "inc", "dec", "sqr", "ofsort", "reverse", "find", "findIndex", "filter", "map", "CLEAR",
            "SQRT", "POW", "SQRTV", "STRLEN"));

    private static final String[] CATEGORIAS = {
            "Errores",
            "Identificadores cadena",
            "Identificadores numericos binarios",
            "Identificadores numericos decimales",
            "Identificadores numericos octales",
            "Identificadores numericos hexadecimales",
            "Identificadores reales",
            "Identificadores exponenciales",
            "Identificadores booleanos",
            "Identificadores de registro",
            "Comentarios",
            "Palabras reservadas",
            "Constantes cadena",
            "Constantes numérica binario",
            "Constantes numérica decimal",
            "Constantes numérica octal",
            "Constantes numérica hexadecimal",
            "Constantes real",
            "Constantes exponencial",
            "Constantes booleanas",
            "Constantes nula",
            "Operadores postfix",
            "Operadores lógicos binarios",
            "Operadores de control",
            "Operadores matemáticos",
            "Operador exponente",
            "Operadores de turno",
            "Operadores relacionales",
            "Operadores sin igualdad de conversión de tipo",
            "Operadores lógicos",
            "Operador ternario",
            "Operadores de asignación",
            "Operadores de agrupamiento"
    };

    public void analyze(String code) {
        input = code;
        pos = 0;
        linea = 1;
        tokens.clear();
        errors.clear();
        initContadores();

        while (pos < input.length()) {
            char c = peek(0);
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
                continue;
            }
            if (c == '\n') {
                linea++;
                pos++;
                continue;
            }
            if (!nextToken()) {
                addError("500",
                        "Carácter '" + c + "' no reconocido por el analizador léxico",
                        String.valueOf(c), "Error léxico");
                inc("Errores");
                pos++;
            }
        }
    }

    public List<Tokens> getTokens() {
        return tokens;
    }

    public List<ErroresLex> getErrors() {
        return errors;
    }

    public Map<String, Integer> getContadores() {
        return contadores;
    }

    private boolean nextToken() {
        char c = peek(0);
        if (c == '\u00C2') {
            pos++;
            return true;
        }
        if (c == '/' && peek(1) == '/') {
            return lineComment();
        }
        if (c == '/' && peek(1) == '*') {
            return blockComment();
        }
        if (c == '"' || c == '\'') {
            return stringConst();
        }

        if ((c == 'x' || c == 'X') && (peek(1) == 'b' || peek(1) == 'B')) {
            return binaryConst();
        }
        if ((c == 'x' || c == 'X') && (peek(1) == 'o' || peek(1) == 'O')) {
            return octalConst();
        }
        if ((c == 'x' || c == 'X') && (peek(1) == 'l' || peek(1) == 'L')) {
            return hexConst();
        }

        if (c == '@') {
            return identCadena();
        }
        if (c == '#') {
            char next = peek(1);
            if (next == 'b' || next == 'B' || next == 'd' || next == 'D' || next == 'o' || next == 'O' || next == 'x'
                    || next == 'X') {
                return identNumerico();
            }
        }
        if (c == '$') {
            return identReal();
        }
        if (c == '\u00BF') {
            return identExponencial();
        }
        if (c == '\u00A1') {
            return identBooleano();
        }

        if (Character.isDigit(c)) {
            return numberConst();
        }

        if (Character.isLetter(c) || c == '_') {
            return wordToken();

        }

        return operatorToken();
    }

    private boolean lineComment() {
        int start = pos;
        pos += 2;
        while (pos < input.length() && peek(0) != '\n') {
            pos++;
        }
        addToken("Comentario lineal", input.substring(start, pos));
        inc("Comentarios");
        return true;
    }

    private boolean blockComment() {
        int start = pos;
        int startLine = linea;
        pos += 2;
        while (pos < input.length()) {
            if (peek(0) == '*' && peek(1) == '/') {
                pos += 2;
                addToken("Comentario grupal", input.substring(start, pos), startLine);
                inc("Comentarios");
                return true;
            }
            if (peek(0) == '\n') {
                linea++;
            }
            pos++;
        }
        addError("502",
                "Comentario de bloque no cerrado; se esperaba '*/' antes del fin del archivo",
                input.substring(start, pos), "Error léxico", startLine);
        inc("Errores");
        return true;
    }

    private boolean stringConst() {
        char q = peek(0);
        int start = pos;
        pos++;
        while (pos < input.length() && peek(0) != q && peek(0) != '\n') {
            pos++;
        }
        if (pos < input.length() && peek(0) == q) {
            pos++;
            addToken("Constante cadena", input.substring(start, pos));
            inc("Constantes cadena");
        } else {
            addError("503",
                    "Cadena no cerrada; se esperaba '" + q + "' antes del salto de línea",
                    input.substring(start, pos), "Error léxico");
            inc("Errores");
        }
        return true;
    }

    private boolean binaryConst() {
        int start = pos;
        pos += 2;
        if (pos >= input.length() || (peek(0) != '0' && peek(0) != '1')) {
            addError("504",
                    "Binario mal formado: se esperaba 0 o 1 después de xb",
                    input.substring(start, pos),
                    "Error léxico");
            inc("Errores");
            return true;
        }
        while (pos < input.length()) {
            char c = peek(0);
            if (c == '0' || c == '1') {
                pos++;
            } else if (Character.isDigit(c)) {
                addError("504",
                        "Binario invalido: solo se permiten 0 y 1",
                        input.substring(start, pos + 1),
                        "Error léxico");
                inc("Errores");
                pos++;
                return true;
            } else {
                break;
            }
        }
        addToken("Constante numerica binario", input.substring(start, pos));
        inc("Constantes numérica binario");
        return true;
    }

    private boolean octalConst() {
        int start = pos;
        pos += 2;
        if (pos >= input.length() || peek(0) < '0' || peek(0) > '7') {
            addError("505",
                    "Octal mal formado: solo se permiten dígitos 0-7",
                    input.substring(start, pos),
                    "Error léxico");
            inc("Errores");
            return true;
        }
        while (pos < input.length()) {
            char c = peek(0);
            if (c >= '0' && c <= '7') {
                pos++;
            } else if (Character.isDigit(c)) {
                addError("505",
                        "Octal invalido: contiene dígitos fuera de 0-7",
                        input.substring(start, pos + 1),
                        "Error léxico");
                inc("Errores");
                pos++;
                return true;
            } else {
                break;
            }
        }
        addToken("Constante numerica octal", input.substring(start, pos));
        inc("Constantes numérica octal");
        return true;
    }

    private boolean hexConst() {
        int start = pos;
        pos += 2;
        if (pos >= input.length() || !isHex(peek(0))) {
            addError("506",
                    "Hexadecimal mal formado: se esperaba 0-9 o A-F",
                    input.substring(start, pos),
                    "Error léxico");
            inc("Errores");
            return true;
        }
        while (pos < input.length()) {
            char c = peek(0);
            if (isHex(c)) {
                pos++;
            } else if (Character.isLetterOrDigit(c)) {
                addError("506",
                        "Hexadecimal invalido: contiene caracteres no válidos",
                        input.substring(start, pos + 1),
                        "Error léxico");
                inc("Errores");
                pos++;
                return true;
            } else {
                break;
            }
        }
        addToken("Constante numérica hexadecimal", input.substring(start, pos));
        inc("Constantes numérica hexadecimal");
        return true;
    }

    private boolean numberConst() {
        int start = pos;

        while (pos < input.length() && Character.isDigit(peek(0))) {
            pos++;
        }

        if (pos < input.length() && Character.isLetter(peek(0))) {
            int errorStart = start;
            while (pos < input.length()
                    && (Character.isLetterOrDigit(peek(0)) || peek(0) == '_')) {
                pos++;
            }
            addError("507",
                    "Identificador inválido: no puede iniciar con número",
                    input.substring(errorStart, pos),
                    "Error léxico");
            inc("Errores");
            return true;
        }

        if (pos < input.length() && peek(0) == '.') {
            if (pos + 1 < input.length() && Character.isDigit(peek(1))) {
                pos++;
                while (pos < input.length() && Character.isDigit(peek(0))) {
                    pos++;
                }
            } else {
                addError("508",
                        "Número real mal formado: se esperaba dígito después del punto",
                        input.substring(start, pos + 1),
                        "Error léxico");
                inc("Errores");
                pos++;
                return true;
            }
        }

        if (pos < input.length() && peek(0) == '^') {
            pos++;

            if (peek(0) == '^') {
                addError("509",
                        "Exponencial mal formado: operador '^' duplicado",
                        input.substring(start, pos + 1),
                        "Error léxico");
                inc("Errores");
                pos++;
                return true;
            }

            if (peek(0) == '+' || peek(0) == '-') {
                pos++;
            }

            if (pos >= input.length() || !Character.isDigit(peek(0))) {
                addError("509",
                        "Exponencial mal formado: se esperaba número después de '^'",
                        input.substring(start, pos),
                        "Error léxico");
                inc("Errores");
                return true;
            }

            while (pos < input.length() && Character.isDigit(peek(0))) {
                pos++;
            }

            addToken("Constante exponencial", input.substring(start, pos));
            inc("Constantes exponencial");
            return true;
        }

        if (input.substring(start, pos).contains(".")) {
            addToken("Constante real", input.substring(start, pos));
            inc("Constantes real");
        } else {
            addToken("Constante numérica decimal", input.substring(start, pos));
            inc("Constantes numérica decimal");
        }

        return true;
    }

    private boolean wordToken() {
        String inputLower = input.toLowerCase();
        if (inputLower.startsWith("console.log", pos)) {
            pos += "console.log".length();
            addToken("Palabra reservada", "console.log");
            inc("Palabras reservadas");

            return true;
        }
        if (inputLower.startsWith("console.read", pos)) {
            pos += "console.read".length();
            addToken("Palabra reservada", "console.read");
            inc("Palabras reservadas");
            return true;
        }
        int start = pos;
        while (pos < input.length()
                && (Character.isLetterOrDigit(peek(0)) || peek(0) == '_')) {
            pos++;
        }

        String word = input.substring(start, pos);
        if (word.equals("any") && pos < input.length() && peek(0) == '.') {
            int tempPos = pos + 1;
            while (tempPos < input.length()
                    && (Character.isLetterOrDigit(input.charAt(tempPos)) || input.charAt(tempPos) == '_')) {
                tempPos++;
            }

            String nextWord = input.substring(pos + 1, tempPos);
            pos = tempPos;
            String full = input.substring(start, pos);
            if (PALABRAS_RESERVADAS.contains(nextWord)) {
                addError("510",
                        "Error: se esperaba una palabra reservada",
                        full,
                        "Error léxico");
            }

            inc("Errores");
            return true;
        }

        if (Character.isDigit(word.charAt(0))) {
            addError("507",
                    "Identificador inválido: no puede iniciar con número",
                    word,
                    "Error léxico");
            inc("Errores");
            return true;
        }

        if (word.equals("true") || word.equals("false")) {
            addToken("Constante booleana", word);
            inc("Constantes booleanas");
        } else if (word.equals("null")) {
            addToken("Constante nula", word);
            inc("Constantes nula");
        } else if (PALABRAS_RESERVADAS.contains(word)) {
            addToken("Palabra reservada", word);
            inc("Palabras reservadas");
        } else {
            addToken("Identificador registro", word);
            inc("Identificadores de registro");
        }

        return true;
    }

    private boolean identCadena() {
        int start = pos;
        pos++;
        while (pos < input.length()
                && (Character.isLetterOrDigit(peek(0)) || peek(0) == '_')) {
            pos++;
        }

        addToken("Identificador cadena", input.substring(start, pos));
        inc("Identificadores cadena");
        return true;
    }

    private boolean identNumerico() {
        int start = pos;
        pos++;
        if (pos >= input.length()) {
            identError("515", "#", start);
            return true;
        }
        char t = peek(0);
        String tipo, cat;
        switch (Character.toUpperCase(t)) {
            case 'B':
                tipo = "Identificador numérico binario";
                cat = "Identificadores numericos binarios";
                break;
            case 'D':
                tipo = "Identificador numérico decimal";
                cat = "Identificadores numericos decimales";
                break;
            case 'O':
                tipo = "Identificador numérico octal";
                cat = "Identificadores numericos octales";
                break;
            case 'X':
                tipo = "Identificador numérico hexadecimal";
                cat = "Identificadores numericos hexadecimales";
                break;
            default:
                addError("507",
                        "Tipo inválido '" + t + "' después de '#'; se esperaba B, D, O o X",
                        "#" + t, "Error léxico");
                inc("Errores");
                pos++;
                return true;
        }
        pos++;
        if (!hasIdentChars()) {
            identError(tipo.substring(14), input.substring(start, pos), start);
            return true;
        }
        while (pos < input.length() && isIdent(peek(0))) {
            pos++;
        }
        addToken(tipo, input.substring(start, pos));
        inc(cat);
        return true;
    }

    private boolean identReal() {
        int start = pos;
        pos++;
        if (!Character.isLetter(peek(0)) && peek(0) != '_') {
            addError("501",
                    "Error: se esperaba [a-zA-Z-Z0-9_]",
                    input.substring(start, pos + 1),
                    "Error léxico");
            inc("Errores");
            return true;
        }
        if (!hasIdentChars()) {
            identError("real", "$", start);
            return true;
        }
        while (pos < input.length() && isIdent(peek(0))) {
            pos++;
        }
        addToken("Identificador real", input.substring(start, pos));
        inc("Identificadores reales");
        return true;
    }

    private boolean identExponencial() {
        int start = pos;
        pos++;
        if (!hasIdentChars()) {
            identError("exponencial", "¿", start);
            return true;
        }
        while (pos < input.length() && isIdent(peek(0))) {
            pos++;
        }
        addToken("Identificador exponencial", input.substring(start, pos));
        inc("Identificadores exponenciales");
        return true;
    }

    private boolean identBooleano() {
        int start = pos;
        pos++;
        if (!hasIdentChars()) {
            identError("booleano", "¡", start);
            return true;
        }
        while (pos < input.length() && isIdent(peek(0))) {
            pos++;
        }
        addToken("Identificador booleano", input.substring(start, pos));
        inc("Identificadores booleanos");
        return true;
    }

    private void identError(String tipo, String found, int start) {
        addError("501",
                "Error: se esperaba [a-zA-Z-Z0-9_]",
                input.substring(start, pos), "Error léxico");
        inc("Errores");
    }

    private boolean operatorToken() {
        char c = peek(0);
        if (c == ':' && peek(1) == ':') {
            emit("Operador de control", "::", "Operadores de control", 2);
            return true;
        }

        if (c == '+') {
            if (peek(1) == '+') {
                emit("Operador postfix", "++", "Operadores postfix", 2);
                return true;
            }
            if (peek(1) == '=') {
                emit("Operador de asignación", "+=", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador matemático", "+", "Operadores matemáticos", 1);
            return true;
        }
        if (c == '-') {
            if (peek(1) == '-') {
                emit("Operador postfix", "--", "Operadores postfix", 2);
                return true;
            }
            if (peek(1) == '=') {
                emit("Operador de asignación", "-=", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador matemático", "-", "Operadores matemáticos", 1);
            return true;
        }
        if (c == '*') {
            if (peek(1) == '*') {
                emit("Operador exponente", "**", "Operador exponente", 2);
                return true;
            }
            if (peek(1) == '=') {
                emit("Operador de asignación", "*=", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador matemático", "*", "Operadores matemáticos", 1);
            return true;
        }
        if (c == '/') {
            if (peek(1) == '=') {
                emit("Operador de asignación", "/=", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador matemático", "/", "Operadores matemáticos", 1);
            return true;
        }
        if (c == '%') {
            if (peek(1) == '=') {
                emit("Operador de asignación", "%=", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador matemático", "%", "Operadores matemáticos", 1);
            return true;
        }
        if (c == '^') {
            if (peek(1) == '=') {
                emit("Operador de asignación", "^=", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador lógico binario", "^", "Operadores lógicos binarios", 1);
            return true;
        }
        if (c == '#') {
            emit("Operador matemático", "#", "Operadores matemáticos", 1);
            return true;
        }
        if (c == '~') {
            emit("Operador lógico binario", "~", "Operadores lógicos binarios", 1);
            return true;
        }
        if (c == '|') {
            if (peek(1) == '|') {
                emit("Operador lógico", "||", "Operadores lógicos", 2);
                return true;
            }
            emit("Operador lógico binario", "|", "Operadores lógicos binarios", 1);
            return true;
        }
        if (c == '&') {
            if (peek(1) == '&') {
                emit("Operador lógico", "&&", "Operadores lógicos", 2);
                return true;
            }
            if (peek(1) == '=') {
                emit("Operador de asignación", "&=", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador lógico binario", "&", "Operadores lógicos binarios", 1);
            return true;
        }

        if (c == '<') {
            if (peek(1) == '<') {
                if (peek(2) == '=') {
                    emit("Operador de asignación", "<<=", "Operadores de asignación", 3);
                    return true;
                }
                emit("Operador de turno", "<<", "Operadores de turno", 2);
                return true;
            }
            if (peek(1) == '=') {
                emit("Operador relacional", "<=", "Operadores relacionales", 2);
                return true;
            }
            if (peek(1) == '>') {
                emit("Operador relacional", "<>", "Operadores relacionales", 2);
                return true;
            }
            emit("Operador relacional", "<", "Operadores relacionales", 1);
            return true;
        }
        if (c == '>') {
            if (peek(1) == '>') {
                if (peek(2) == '>') {
                    if (peek(3) == '=') {
                        emit("Operador de asignación", ">>>=", "Operadores de asignación", 4);
                        return true;
                    }
                    emit("Operador de turno", ">>>", "Operadores de turno", 3);
                    return true;
                }
                if (peek(2) == '=') {
                    emit("Operador de asignación", ">>=", "Operadores de asignación", 3);
                    return true;
                }
                emit("Operador de turno", ">>", "Operadores de turno", 2);
                return true;
            }
            if (peek(1) == '=') {
                emit("Operador relacional", ">=", "Operadores relacionales", 2);
                return true;
            }
            emit("Operador relacional", ">", "Operadores relacionales", 1);
            return true;
        }
        if (c == '=') {
            if (peek(1) == '=') {
                if (peek(2) == '=') {
                    emit("Operador sin igualdad tipo", "===", "Operadores sin igualdad de conversión de tipo", 3);
                    return true;
                }
                emit("Operador relacional", "==", "Operadores relacionales", 2);
                return true;
            }
            if (peek(1) == '>') {
                emit("Operador de asignación", "=>", "Operadores de asignación", 2);
                return true;
            }
            emit("Operador de asignación", "=", "Operadores de asignación", 1);
            return true;
        }
        if (c == '!') {
            if (peek(1) == '=') {
                if (peek(2) == '=') {
                    emit("Operador sin igualdad tipo", "!==", "Operadores sin igualdad de conversión de tipo", 3);
                    return true;
                }
                emit("Operador relacional", "!=", "Operadores relacionales", 2);
                return true;
            }
            emit("Operador lógico", "!", "Operadores lógicos", 1);
            return true;
        }
        if (c == '?') {
            emit("Operador ternario", "?", "Operador ternario", 1);
            return true;
        }
        if (c == ',') {
            emit("Operador de control", ",", "Operadores de control", 1);
            return true;
        }
        if (c == '.') {
            emit("Operador de control", ".", "Operadores de control", 1);
            return true;
        }
        if (c == ';') {
            emit("Operador de control", ";", "Operadores de control", 1);
            return true;
        }
        if (c == ':') {
            emit("Operador de control", ":", "Operadores de control", 1);
            return true;
        }
        if (c == '{' || c == '}' || c == '[' || c == ']' || c == '(' || c == ')') {
            emit("Operador de agrupamiento", String.valueOf(c), "Operadores de agrupamiento", 1);
            return true;
        }

        addError("500",
                "El carácter '" + c + "' no es válido en el lenguaje",
                String.valueOf(c),
                "Error léxico");

        inc("Errores");
        pos++;
        return true;
    }

    private char peek(int offset) {
        int i = pos + offset;
        return (i < input.length()) ? input.charAt(i) : '\0';
    }

    private void emit(String tipo, String lexema, String categoria, int len) {
        addToken(tipo, lexema);
        inc(categoria);
        pos += len;
    }

    private boolean isHex(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isIdent(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private boolean hasIdentChars() {
        return pos < input.length() && isIdent(peek(0));
    }

    private void addToken(String tipo, String lexema) {
        tokens.add(new Tokens(tipo, lexema, linea));
    }

    private void addToken(String tipo, String lexema, int l) {
        tokens.add(new Tokens(tipo, lexema, l));
    }

    private void addError(String cod, String desc, String lex, String tipo) {
        errors.add(new ErroresLex(cod, desc, lex, tipo, linea));
    }

    private void addError(String cod, String desc, String lex, String tipo, int l) {
        errors.add(new ErroresLex(cod, desc, lex, tipo, l));
    }

    private void initContadores() {
        contadores.clear();
        for (String cat : CATEGORIAS) {
            contadores.put(cat, 0);
        }
    }

    private void inc(String cat) {
        contadores.put(cat, contadores.getOrDefault(cat, 0) + 1);
    }
}
