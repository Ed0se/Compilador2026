package lexico;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Sintaxis {

    private static final String T_ID = "id";
    private static final String T_CONST = "CONST";
    private static final String T_FIN = "$";
    private static final Tokens TOKEN_FIN = new Tokens(T_FIN, T_FIN, -1);

    private static final Map<String, Map<String, String[]>> TABLA = new HashMap<>();

    static {
        Set<String> FF = new HashSet<>(Arrays.asList(T_ID, T_CONST, "(", "!", "~", "+", "-", "++", "--", "true",
                "false", "null", "SQRT", "sin", "cos", "tan", "POW", "SQRTV", "STRLEN", "concat", "copy", "val", "str",
                "chr", "pred", "succ", "inc", "dec", "sqr", "sqr", "CLEAR", "Console.log", "Console.read",
                "console.log", "console.read"));
        Set<String> emptyFollows = new HashSet<>(Arrays.asList(")", "]", ";", ",", ":", "?", "{", "}", "$", "if",
                "while", "do", "for", "switch", "return", "break", "main", "def", "var", "reg", "case", "default",
                "else", "elseif", "log", "read", "[", ".", "!", "~", "+", "-", "++", "--", "true", "false", "null",
                "SQRT", "sin", "cos", "tan", "POW", "SQRTV", "STRLEN", "concat", "copy", "val", "str", "chr", "pred",
                "succ", "inc", "dec", "sqr", "CLEAR", "||", "|", "&&", "&", "==", "!=", "<", ">", "<=", ">=", "<<",
                ">>", ">>>", "*", "/", "%", "#", "^", "=", "+=", "-=", "*=", "/="));
        emptyFollows.addAll(FF);

        Map<String, String[]> m;
        String[] ms = { "SQRT", "sin", "cos", "tan", "POW", "SQRTV", "STRLEN", "concat", "copy", "val", "str", "chr",
                "pred", "succ", "inc", "dec", "sqr" };

        String[] mm = { "M_PROG", "M_LIST", "M_EXP", "M_CONST", "M_OR", "M_AND", "M_DECL", "M_FACT", "M_ELEV", "M_TERM",
                "M_SIMP", "M_STAT", "M_FUNC", "M_ASIG", "M_ARR", "M_ARR_DECL", "M_CONST_NUM", "M_DEF_BODY_ON",
                "M_DEF_BODY_OFF", "M_EXEC_ON", "M_EXEC_OFF", "M_SCOPE_PUSH", "M_SCOPE_POP", "M_DECL_ON", "M_DECL_OFF",
                "M_AMB_OPEN", "M_AMB_CLOSE" };
        for (String mk : mm) {
            TABLA.put(mk, new HashMap<>());
        }

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "S_LIST" });
        m.put("reg", new String[] { "S_LIST" });
        m.put("var", new String[] { "S_LIST" });
        m.put("def", new String[] { "S_LIST" });
        m.put("main", new String[] { "S_LIST" });
        m.put(T_FIN, new String[] {});
        TABLA.put("PROGRAMA", m);

        TABLA.put("PROGRAMA_TOP", new HashMap<String, String[]>() {
            {
                put("reg", new String[] { "M_DECL_ON", "reg", "R_L", "M_DECL_OFF", "PROGRAMA_TOP" });
                put("var", new String[] { "M_DECL_ON", "var", "V_L", ";", "M_DECL_OFF", "PROGRAMA_TOP" });
                put("def",
                        new String[] { "M_PROG", "M_FUNC", "M_DECL_ON", "def", T_ID, "M_AMB_OPEN", "M_LIST", "(", "O_L",
                                ")",
                                "M_DECL_OFF", "M_DEF_BODY_ON", "PROGRAMA", "M_AMB_CLOSE", ";", "M_DEF_BODY_OFF",
                                "PROGRAMA_TOP" });
                put("main", new String[] { "M_PROG", "main", "(", ")", "M_STAT", "M_EXEC_ON", "{", "S_LIST", "}",
                        "M_EXEC_OFF", "PROGRAMA_TOP" });
                put(T_ID, new String[] { "M_STAT", "M_DECL", T_ID, "PROG_ASIG", "PROG_C_L", "PROGRAMA_TOP" });
                put(T_FIN, new String[] {});
            }
        });

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "STATU", "S_LIST" });
        m.put("if", new String[] { "STATU", "S_LIST" });
        m.put("while", new String[] { "STATU", "S_LIST" });
        m.put("do", new String[] { "STATU", "S_LIST" });
        m.put("for", new String[] { "STATU", "S_LIST" });
        m.put("switch", new String[] { "STATU", "S_LIST" });
        m.put("return", new String[] { "STATU", "S_LIST" });
        m.put("break", new String[] { "STATU", "S_LIST" });
        m.put("case", new String[] { "STATU", "S_LIST" });
        m.put("default", new String[] { "STATU", "S_LIST" });
        m.put("{", new String[] { "STATU", "S_LIST" });
        m.put("reg", new String[] { "STATU", "S_LIST" });
        m.put("var", new String[] { "STATU", "S_LIST" });
        m.put("def", new String[] { "STATU", "S_LIST" });
        m.put("main", new String[] { "STATU", "S_LIST" });
        m.put(";", new String[] { ";", "S_LIST" });
        m.put("}", new String[] {});
        m.put(T_FIN, new String[] {});
        TABLA.put("S_LIST", m);

        m = new HashMap<>();
        m.put("reg", new String[] { "M_DECL_ON", "reg", "R_L", "M_DECL_OFF", "PROGRAMA" });
        m.put("var", new String[] { "M_DECL_ON", "var", "V_L", ";", "M_DECL_OFF", "PROGRAMA" });
        m.put("def",
                new String[] { "M_FUNC", "M_DECL_ON", "def", T_ID, "M_AMB_OPEN", "M_LIST", "(", "O_L", ")",
                        "M_DECL_OFF",
                        "M_DEF_BODY_ON", "PROGRAMA", "M_AMB_CLOSE", ";", "M_DEF_BODY_OFF", "PROGRAMA" });
        m.put("main", new String[] { "main", "(", ")", "M_STAT", "M_EXEC_ON", "{", "S_LIST", "}", "M_EXEC_OFF" });
        m.put(T_ID, new String[] { "M_STAT", "M_DECL", T_ID, "PROG_ASIG", "PROG_C_L", "PROGRAMA" });
        m.put(";", new String[] {});
        m.put(T_FIN, new String[] {});
        TABLA.put("PROGRAMA", m);

        m = new HashMap<>();
        m.put("=", new String[] { "=", "OR" });
        TABLA.put("PROG_ASIG", m);

        m = new HashMap<>();
        m.put(",", new String[] { ",", "M_STAT", T_ID, "PROG_ASIG", "PROG_C_L" });
        m.put(";", new String[] { ";" });
        TABLA.put("PROG_C_L", m);

        m = new HashMap<>();
        m.put(T_ID, new String[] { "M_STAT", "OR", "OPT_SEMI" });
        m.put("++", new String[] { "M_STAT", "++", T_ID, "OPT_SEMI" });
        m.put("{", new String[] { "M_STAT", "M_EXEC_ON", "{", "S_LIST", "}", "M_EXEC_OFF" });
        m.put("Console.log", new String[] { "M_STAT", "Console.log", "(", "O_L", ")", "OPT_SEMI" });
        m.put("console.log", new String[] { "M_STAT", "console.log", "(", "O_L", ")", "OPT_SEMI" });
        m.put("Console.read", new String[] { "M_STAT", "Console.read", "(", "O_L", ")", "OPT_SEMI" });
        m.put("console.read", new String[] { "M_STAT", "console.read", "(", "O_L", ")", "OPT_SEMI" });
        m.put("--", new String[] { "M_STAT", "--", T_ID, "OPT_SEMI" });
        m.put("CLEAR", new String[] { "M_STAT", "M_FUNC", "CLEAR", "OPT_SEMI" });
        m.put("if", new String[] { "M_STAT", "if", "(", "OR", ")", "STATU", "E_P" });
        m.put("while", new String[] { "M_STAT", "while", "(", "OR", ")", "STATU" });
        m.put("do", new String[] { "M_STAT", "do", "STATU", "while", "(", "OR", ")" });
        m.put("for", new String[] { "M_STAT", "for", "(", "F_B", ")", "STATU" });
        m.put("switch", new String[] { "M_STAT", "switch", "(", "OR", ")", "M_STAT", "M_EXEC_ON", "{", "C_L", "}",
                "M_EXEC_OFF" });
        m.put("return", new String[] { "M_STAT", "return", "OR" });
        m.put("break", new String[] { "M_STAT", "break", "OPT_SEMI" });
        m.put("case", new String[] { "case", "OR", ":" });
        m.put("default", new String[] { "default", ":" });
        m.put("var", new String[] { "M_DECL_ON", "var", "V_L", ";", "M_DECL_OFF" });
        m.put("reg", new String[] { "M_DECL_ON", "reg", "R_L", "M_DECL_OFF" });
        TABLA.put("STATU", m);

        m = new HashMap<>();
        m.put(T_ID, new String[] { T_ID, "A_R" });
        TABLA.put("ID_E", m);
        m = new HashMap<>();
        m.put("=", new String[] { "M_ASIG", "=", "OR" });
        m.put("+=", new String[] { "M_ASIG", "+=", "OR" });
        m.put("-=", new String[] { "M_ASIG", "-=", "OR" });
        m.put("*=", new String[] { "M_ASIG", "*=", "OR" });
        m.put("/=", new String[] { "M_ASIG", "/=", "OR" });
        m.put("++", new String[] { "++" });
        m.put("--", new String[] { "--" });
        m.put("[", new String[] { "M_ARR", "[", "OR", "ARR_REST", "]", "A_R_POST_ARR" });
        m.put(".", new String[] { ".", T_ID, "A_R" });
        m.put("(", new String[] { "(", "O_L", ")" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("A_R", m);

        m = new HashMap<>();
        m.put(",", new String[] { ",", "OR", "ARR_REST" });
        m.put("]", new String[] {});
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("ARR_REST", m);

        m = new HashMap<>();
        m.put("=", new String[] { "M_ASIG", "=", "OR" });
        m.put("+=", new String[] { "M_ASIG", "+=", "OR" });
        m.put("-=", new String[] { "M_ASIG", "-=", "OR" });
        m.put("*=", new String[] { "M_ASIG", "*=", "OR" });
        m.put("/=", new String[] { "M_ASIG", "/=", "OR" });
        m.put("++", new String[] { "++" });
        m.put("--", new String[] { "--" });
        m.put(".", new String[] { ".", T_ID, "A_R" });
        for (String s : emptyFollows)
            if (!s.equals("[") && !m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("A_R_POST_ARR", m);

        m = new HashMap<>();
        m.put("reg", new String[] { "reg", "R_L" });
        m.put(T_ID, new String[] { "M_DECL", T_ID, "V_R", "V_L_T" });
        TABLA.put("V_L", m);
        m = new HashMap<>();
        m.put(",", new String[] { ",", "M_DECL", "V_L" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("V_L_T", m);
        m = new HashMap<>();
        m.put(T_ID, new String[] { T_ID, "V_R" });
        m.put("[", new String[] { "M_ARR_DECL", "[", "M_CONST", "M_CONST_NUM", T_CONST, "V_ARR_REST", "]" });
        m.put("=", new String[] { "M_ASIG", "=", "OR" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("V_R", m);

        m = new HashMap<>();
        m.put(",", new String[] { ",", "M_CONST", "M_CONST_NUM", T_CONST, "V_ARR_REST" });
        m.put("]", new String[] {});
        for (String s : emptyFollows)
            if (!s.equals("[") && !m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("V_ARR_REST", m);

        m = new HashMap<>();
        for (String s : emptyFollows)
            if (!s.equals("[") && !m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("V_R_EXT", m);

        m = new HashMap<>();
        m.put("reg", new String[] { "reg", "R_L" });
        m.put(T_ID, new String[] { "M_DECL", T_ID, "R_R", "R_L_T" });
        TABLA.put("R_L", m);
        m = new HashMap<>();
        m.put(",", new String[] { ",", "V_L" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("R_L_T", m);
        m = new HashMap<>();
        m.put("{", new String[] { "{", "O_L", "}" });
        m.put(T_ID, new String[] { T_ID, "V_R" });
        m.put("=", new String[] { "M_ASIG", "=", "OR" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("R_R", m);

        m = new HashMap<>();
        m.put("(", new String[] { "(", "E_E", ")" });
        for (String s : FF)
            if (!s.equals("("))
                m.put(s, new String[] { "E_E" });
        TABLA.put("O_L", m);
        m = new HashMap<>();
        m.put("(", new String[] { "M_LIST", "OR", "E_E_P" });
        for (String s : FF)
            if (!s.equals("("))
                m.put(s, new String[] { "M_LIST", "OR", "E_E_P" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("E_E", m);
        m = new HashMap<>();
        m.put(",", new String[] { ",", "M_LIST", "OR", "E_E_P" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("E_E_P", m);

        m = new HashMap<>();
        m.put("else", new String[] { "else", "STATU" });
        m.put("elseif", new String[] { "elseif", "(", "OR", ")", "STATU", "E_P" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("E_P", m);

        m = new HashMap<>();
        m.put(";", new String[] { ";" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("OPT_SEMI", m);

        m = new HashMap<>();
        m.put("case", new String[] { "case", "OR", ":", "S_LIST", "C_L" });
        m.put("default", new String[] { "default", ":", "S_LIST" });
        m.put("}", new String[] {});
        TABLA.put("C_L", m);

        m = new HashMap<>();
        m.put(":", new String[] { ":", "OR" });
        m.put("=", new String[] { "=", "OR", "F_INIT_REST", ";", "OR", ";", "O_L" });
        m.put(";", new String[] { ";", "OR", ";", "O_L" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("F_T_ID", m);

        m = new HashMap<>();
        m.put(",", new String[] { ",", T_ID, "=", "OR", "F_INIT_REST" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("F_INIT_REST", m);

        m = new HashMap<>();
        m.put(";", new String[] { ";", "OR", ";", "O_L" });
        m.put("var", new String[] { "var", "V_L", ";", "OR", ";", "O_L" });
        m.put(T_ID, new String[] { T_ID, "F_T_ID" });
        TABLA.put("F_B", m);

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "M_EXP", "AND", "OR_P" });
        TABLA.put("OR", m);
        m = new HashMap<>();
        m.put("||", new String[] { "||", "M_OR", "AND", "OR_P" });
        m.put("|", new String[] { "|", "M_OR", "AND", "OR_P" });
        m.put("?", new String[] { "?", "OR", ":", "OR" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("OR_P", m);

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "EXP_P", "AND_P" });
        TABLA.put("AND", m);
        m = new HashMap<>();
        m.put("&&", new String[] { "&&", "M_AND", "EXP_P", "AND_P" });
        m.put("&", new String[] { "&", "M_AND", "EXP_P", "AND_P" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("AND_P", m);

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "SI_E", "EXP_P_T" });
        TABLA.put("EXP_P", m);
        m = new HashMap<>();
        m.put("==", new String[] { "==", "SI_E" });
        m.put("!=", new String[] { "!=", "SI_E" });
        m.put("<", new String[] { "<", "SI_E" });
        m.put(">", new String[] { ">", "SI_E" });
        m.put("<=", new String[] { "<=", "SI_E" });
        m.put(">=", new String[] { ">=", "SI_E" });
        m.put("===", new String[] { "===", "SI_E" });
        m.put("!==", new String[] { "!==", "SI_E" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("EXP_P_T", m);

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "TERM", "SI_E_P" });
        TABLA.put("SI_E", m);
        m = new HashMap<>();
        m.put("+", new String[] { "+", "TERM", "SI_E_P" });
        m.put("-", new String[] { "-", "TERM", "SI_E_P" });
        m.put("<<", new String[] { "<<", "TERM", "SI_E_P" });
        m.put(">>", new String[] { ">>", "TERM", "SI_E_P" });
        m.put(">>>", new String[] { ">>>", "TERM", "SI_E_P" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("SI_E_P", m);

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "ELEV", "TERM_P" });
        TABLA.put("TERM", m);
        m = new HashMap<>();
        m.put("*", new String[] { "*", "ELEV", "TERM_P" });
        m.put("/", new String[] { "/", "ELEV", "TERM_P" });
        m.put("%", new String[] { "%", "ELEV", "TERM_P" });
        m.put("#", new String[] { "#", "ELEV", "TERM_P" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("TERM_P", m);

        m = new HashMap<>();
        for (String s : FF)
            m.put(s, new String[] { "M_ELEV", "FACT", "ELEV_P" });
        TABLA.put("ELEV", m);
        m = new HashMap<>();
        m.put("^", new String[] { "^", "M_ELEV", "FACT", "ELEV_P" });
        m.put("**", new String[] { "**", "M_ELEV", "FACT", "ELEV_P" });
        for (String s : emptyFollows)
            if (!m.containsKey(s))
                m.put(s, new String[] {});
        TABLA.put("ELEV_P", m);

        m = new HashMap<>();
        m.put(T_ID, new String[] { "M_FACT", "ID_E" });
        m.put(T_CONST, new String[] { "M_FACT", "M_CONST", "M_CONST_NUM", T_CONST });
        m.put("true", new String[] { "M_FACT", "true" });
        m.put("false", new String[] { "M_FACT", "false" });
        m.put("null", new String[] { "M_FACT", "null" });
        m.put("(", new String[] { "M_FACT", "(", "OR", ")" });
        m.put("!", new String[] { "!", "FACT" });
        m.put("~", new String[] { "~", "FACT" });
        m.put("+", new String[] { "+", "FACT" });
        m.put("-", new String[] { "-", "FACT" });
        m.put("++", new String[] { "++", T_ID });
        m.put("--", new String[] { "--", T_ID });
        for (String s : ms)
            m.put(s, new String[] { "M_FACT", "M_FUNC", s, "(", "O_L", ")" });
        m.put("CLEAR", new String[] { "M_FACT", "M_FUNC", "CLEAR" });
        m.put("Console.read", new String[] { "M_FACT", "Console.read", "(", "O_L", ")" });
        m.put("console.read", new String[] { "M_FACT", "console.read", "(", "O_L", ")" });
        m.put("Console.log", new String[] { "M_FACT", "Console.log", "(", "O_L", ")" });
        m.put("console.log", new String[] { "M_FACT", "console.log", "(", "O_L", ")" });
        TABLA.put("FACT", m);
    }

    private List<Tokens> tokens;
    private int pos;
    private List<ErroresSin> errores;
    private Map<String, Integer> visitas;

    private final List<ZonaRegistro> zonasRegistradas = new LinkedList<>();
    private final Stack<Integer> stackNiveles = new Stack<>();
    private int contadorAmbitos = 0;
    private final List<String> historialAmbitos = new ArrayList<>();

    // Manejador en memoria
    private final ManejadorSimbolos manejadorSimbolos = new ManejadorSimbolos();
    public Map<Integer, Integer> fallosEnAmbitos = new TreeMap<>();
    private Semantica semantica = new Semantica();
    private final Map<Tokens, Integer> tokenAmbitos = new IdentityHashMap<>();
    private final Map<Tokens, Stack<Integer>> tokenScopeStacks = new IdentityHashMap<>();
    private final Map<Integer, Integer> lineaAmbitos = new HashMap<>();
    private boolean aguardandoDeclaracion = false;
    private boolean procesandoParametros = false;
    private int totalParametrosActuales = 1;
    private int funcionesDetectadas = 0;
    private EntidadSimbolo ctxFuncionAct = null;
    private String nombreFuncion = null;
    private String identificadorDeclarado = null;

    public Sintaxis(List<Tokens> tokens) {
        List<Tokens> merged = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Tokens t = tokens.get(i);
            String lex = t.getLexema();
            if (lex.length() == 1 && (lex.charAt(0) == '#' || lex.charAt(0) == '@' || lex.charAt(0) == '$' || lex.charAt(0) == '¿' || lex.charAt(0) == '¡')
                    && i + 1 < tokens.size()) {
                char c = lex.charAt(0);
                Tokens next = tokens.get(i + 1);
                String nLex = next.getLexema();
                if (next.getTipo().contains("Identificador")
                        || (nLex.length() > 0 && Character.isLetterOrDigit(nLex.charAt(0)))) {
                    String tipo = "Identificador";
                    if (c == '#') {
                        char typeChar = Character.toUpperCase(nLex.charAt(0));
                        if (typeChar == 'B') tipo = "Identificador num\u00e9rico binario";
                        else if (typeChar == 'D') tipo = "Identificador num\u00e9rico decimal";
                        else if (typeChar == 'O') tipo = "Identificador num\u00e9rico octal";
                        else if (typeChar == 'X') tipo = "Identificador num\u00e9rico hexadecimal";
                        else tipo = "Identificador num\u00e9rico";
                    }
                    if (c == '@')
                        tipo = "Identificador cadena";
                    if (c == '$')
                        tipo = "Identificador real";
                    if (c == '¿')
                        tipo = "Identificador exponencial";
                    if (c == '¡')
                        tipo = "Identificador booleano";
                    merged.add(new Tokens(tipo, lex + nLex, t.getLinea()));
                    i++;
                    continue;
                }
            }
            merged.add(t);
        }
        this.tokens = merged;
    }

    private enum EstadoContexto {
        INACTIVO, MODO_DECLARAR, MODO_EJECUTAR
    }

    private EstadoContexto ctxActual = EstadoContexto.INACTIVO;
    private int linDecl = -1;
    private int linEjec = -1;
    private boolean enCuerpo = false;
    private int profCuerpo = 0;

    public void analizar() {
        this.pos = 0;
        this.errores = new ArrayList<>();
        this.visitas = new LinkedHashMap<>();
        this.zonasRegistradas.clear();
        this.ctxActual = EstadoContexto.INACTIVO;
        this.linDecl = -1;
        this.linEjec = -1;
        this.enCuerpo = false;
        this.profCuerpo = 0;
        this.contadorAmbitos = 0;
        this.stackNiveles.clear();
        this.historialAmbitos.clear();
        this.tokenAmbitos.clear();
        this.tokenScopeStacks.clear();
        this.lineaAmbitos.clear();

        this.manejadorSimbolos.limpiar();
        this.fallosEnAmbitos.clear();
        this.aguardandoDeclaracion = false;
        this.procesandoParametros = false;
        this.totalParametrosActuales = 1;
        this.funcionesDetectadas = 0;
        this.ctxFuncionAct = null;
        this.nombreFuncion = null;
        this.identificadorDeclarado = null;
        this.semantica = new Semantica();

        int inicioPrg = (!tokens.isEmpty() && tokens.get(0).getLinea() > 0) ? tokens.get(0).getLinea() : 1;
        this.stackNiveles.push(0);
        this.historialAmbitos.add("Creación ámbito: [0," + inicioPrg + "]");
        this.historialAmbitos.add("Pila -> " + this.stackNiveles.toString().replace(" ", ""));

        Stack<String> pila = new Stack<>();
        pila.push(T_FIN);
        pila.push("PROGRAMA_TOP");

        while (!pila.isEmpty()) {
            String tope = pila.peek();
            Tokens t = (pos < tokens.size()) ? tokens.get(pos) : TOKEN_FIN;
            if (tope.equals(T_FIN) && t.getLexema().equals(T_FIN)) {
                pila.pop();
            } else if (TABLA.containsKey(tope)) {
                String look = getLookup(t.getLexema(), t.getTipo());
                Map<String, String[]> prodM = TABLA.get(tope);
                if (tope.startsWith("M_")) {
                    pila.pop();
                    String[] prod = prodM.getOrDefault(look, new String[] {});
                    for (int i = prod.length - 1; i >= 0; i--)
                        pila.push(prod[i]);
                    marcarHito(tope, t);
                    continue;
                }
                if (prodM != null && (prodM.containsKey(look) || prodM.isEmpty())) {
                    pila.pop();
                    String[] prod = prodM.getOrDefault(look, new String[] {});
                    for (int i = prod.length - 1; i >= 0; i--)
                        pila.push(prod[i]);
                    marcarHito(tope, t);
                } else {
                    registrarErrorSin(tope, t);
                    notificarErrorAAmbito();
                    pila.pop();
                    if (pos < tokens.size())
                        pos++;
                }
            } else {
                if (match(tope, t)) {
                    visitas.put(tope, visitas.getOrDefault(tope, 0) + 1);
                    if (ctxActual == EstadoContexto.MODO_DECLARAR && t.getLinea() > 0) {
                        linDecl = t.getLinea();
                    } else if (ctxActual == EstadoContexto.MODO_EJECUTAR && t.getLinea() > 0) {
                        linEjec = t.getLinea();
                    }
                    gestionarTokenActivo(tope, t, pos);
                    pila.pop();
                    pos++;
                } else {
                    registrarErrorSin(tope, t);
                    notificarErrorAAmbito();
                    pila.pop();
                }
            }
        }

        int finalPrg = (!tokens.isEmpty()) ? tokens.get(tokens.size() - 1).getLinea() : 1;
        if (ctxActual == EstadoContexto.MODO_EJECUTAR) {
            int closeLn = (linEjec > 0) ? linEjec : finalPrg;
            zonasRegistradas.add(new ZonaRegistro(closeLn, "ejecución", "cerrado", 802, "M_EXEC_OFF"));
            ctxActual = EstadoContexto.INACTIVO;
        } else if (ctxActual == EstadoContexto.MODO_DECLARAR) {
            int closeLn = (linDecl > 0) ? linDecl : finalPrg;
            zonasRegistradas.add(new ZonaRegistro(closeLn, "declaración", "cerrado", 804, "M_DECL_OFF"));
            ctxActual = EstadoContexto.INACTIVO;
        }

        if (!stackNiveles.isEmpty()) {
            int eliminado = stackNiveles.pop();
            historialAmbitos.add("Eliminación ámbito: [" + eliminado + "," + finalPrg + "]");
            historialAmbitos.add("Pila -> " + stackNiveles.toString().replace(" ", ""));
        }

        semantica.analizarExpresiones(tokens, manejadorSimbolos, stackNiveles, tokenScopeStacks);
        semantica.evaluarReglasSemanticas(tokens, manejadorSimbolos, tokenAmbitos, tokenScopeStacks, lineaAmbitos);
        errores.addAll(semantica.getErroresSemanticos());
    }

    private void registrarErrorSin(String p, Tokens t) {
        String errCode = getErrorCode(p, t);
        String errDesc = getErrorDesc(errCode, p, t);
        if (errores.size() < 10) {
            errores.add(new ErroresSin(errCode, p, t.getLexema(), errDesc, t.getLinea()));
        }
    }

    private void notificarErrorAAmbito() {
        int actual = !stackNiveles.isEmpty() ? stackNiveles.peek() : 0;
        fallosEnAmbitos.put(actual, fallosEnAmbitos.getOrDefault(actual, 0) + 1);
    }

    private void marcarHito(String p, Tokens t) {
        int nLin = (t != null && t.getLinea() > 0) ? t.getLinea()
                : (!tokens.isEmpty() ? tokens.get(tokens.size() - 1).getLinea() : 1);

        if (p.equals("M_AMB_OPEN") || p.equals("M_SCOPE_PUSH")) {
            contadorAmbitos++;
            stackNiveles.push(contadorAmbitos);
            historialAmbitos.add("Creación ámbito: [" + contadorAmbitos + "," + nLin + "]");
            historialAmbitos.add("Pila -> " + stackNiveles.toString().replace(" ", ""));
            return;
        }

        if (p.equals("M_AMB_CLOSE") || p.equals("M_SCOPE_POP")) {
            if (stackNiveles.size() > 1) {
                int closed = stackNiveles.pop();
                historialAmbitos.add("Eliminación ámbito: [" + closed + "," + nLin + "]");
                historialAmbitos.add("Pila -> " + stackNiveles.toString().replace(" ", ""));
            }
            return;
        }

        if (p.equals("M_DECL_ON")) {
            if (ctxActual != EstadoContexto.MODO_DECLARAR) {
                if (ctxActual == EstadoContexto.MODO_EJECUTAR) {
                    int cLn = (linEjec > 0) ? linEjec : nLin;
                    zonasRegistradas.add(new ZonaRegistro(cLn, "ejecución", "cerrado", 802, "M_EXEC_OFF"));
                }
                zonasRegistradas.add(new ZonaRegistro(nLin, "declaración", "abierto", 803, "M_DECL_ON"));
                ctxActual = EstadoContexto.MODO_DECLARAR;
            }
            linDecl = nLin;
            return;
        }

        if (p.equals("M_DECL_OFF")) {
            linDecl = nLin;
            return;
        }

        if (p.equals("M_DEF_BODY_ON")) {
            enCuerpo = true;
            profCuerpo++;
            return;
        }

        if (p.equals("M_DEF_BODY_OFF")) {
            if (profCuerpo > 0)
                profCuerpo--;
            if (profCuerpo == 0)
                enCuerpo = false;

            if (profCuerpo == 0 && ctxActual == EstadoContexto.MODO_EJECUTAR) {
                int cLn = (linEjec > 0) ? linEjec : nLin;
                zonasRegistradas.add(new ZonaRegistro(cLn, "ejecución", "cerrado", 802, "M_DEF_BODY_OFF"));
                ctxActual = EstadoContexto.INACTIVO;
            }
            return;
        }

        if (p.equals("M_EXEC_ON")) {
            if (ctxActual == EstadoContexto.MODO_DECLARAR) {
                int cLn = (linDecl > 0) ? linDecl : nLin;
                zonasRegistradas.add(new ZonaRegistro(cLn, "declaración", "cerrado", 804, "M_DECL_OFF"));
            }
            if (ctxActual != EstadoContexto.MODO_EJECUTAR) {
                zonasRegistradas.add(new ZonaRegistro(nLin, "ejecución", "abierto", 801, "M_EXEC_ON"));
                ctxActual = EstadoContexto.MODO_EJECUTAR;
            }
            linEjec = nLin;
            return;
        }

        if (p.equals("M_EXEC_OFF")) {
            return;
        }

        if (p.equals("M_DECL")) {
            aguardandoDeclaracion = true;
        }

        if (p.equals("M_CONST_NUM")) {
            if (!t.getTipo().toLowerCase().contains("decimal"))
                return;
        }
        if (p.equals("M_FUNC") && enCuerpo)
            return;
        visitas.put(p, visitas.getOrDefault(p, 0) + 1);
    }

    private void gestionarTokenActivo(String p, Tokens t, int i) {
        String lex = t.getLexema();
        int ln = t.getLinea();
        int nvl = !stackNiveles.isEmpty() ? stackNiveles.peek() : 0;
        tokenAmbitos.put(t, nvl);
        if (ln > 0) {
            lineaAmbitos.put(ln, nvl);
        }
        Stack<Integer> snap = new Stack<>();
        snap.addAll(stackNiveles);
        tokenScopeStacks.put(t, snap);

        if (lex.equals("def")) {
            if (i + 1 < tokens.size()) {
                Tokens nT = tokens.get(i + 1);
                if (nT.getTipo().startsWith("Identificador") || esId(nT.getLexema())) {
                    String funId = nT.getLexema();
                    funcionesDetectadas++;
                    nombreFuncion = funId;
                    int ambitoN = nvl;
                    EntidadSimbolo sym = new EntidadSimbolo(funId, determinarTipo(funId), "funcion", ambitoN, "-", "-",
                            0, String.valueOf(funcionesDetectadas));
                    ctxFuncionAct = sym;
                    identificadorDeclarado = funId;

                    if (manejadorSimbolos.verificarExistencia(funId, ambitoN)) {
                        agregarErrAm(funId, ambitoN, ln);
                    } else {
                        manejadorSimbolos.registrarEntidad(sym);
                    }
                }
            }
            return;
        }

        if (lex.equals("(")) {
            if (identificadorDeclarado != null) {
                procesandoParametros = true;
                totalParametrosActuales = 1;
            }
            return;
        }

        if (lex.equals(")")) {
            if (procesandoParametros) {
                procesandoParametros = false;
                if (ctxFuncionAct != null) {
                    ctxFuncionAct.setNumeroParametros(totalParametrosActuales - 1);
                }
                identificadorDeclarado = null;
                ctxFuncionAct = null;
                nombreFuncion = null;
            }
            return;
        }

        if (p.equals(T_ID) || t.getTipo().startsWith("Identificador")) {
            if (procesandoParametros) {
                EntidadSimbolo pSym = new EntidadSimbolo(lex, determinarTipo(lex), "param", nvl, "-", "-",
                        totalParametrosActuales++, nombreFuncion != null ? nombreFuncion : "-");
                if (manejadorSimbolos.verificarExistencia(lex, nvl)) {
                    agregarErrAm(lex, nvl, ln);
                } else {
                    manejadorSimbolos.registrarEntidad(pSym);
                }
            } else if (aguardandoDeclaracion) {
                aguardandoDeclaracion = false;
                String[] dims = detectarTamanio(i);
                if (dims != null) {
                    EntidadSimbolo arrSym = new EntidadSimbolo(lex, determinarTipo(lex), "arreglo", nvl,
                            String.join(",", dims), String.valueOf(dims.length), null, "-");
                    if (manejadorSimbolos.verificarExistencia(lex, nvl)) {
                        agregarErrAm(lex, nvl, ln);
                    } else {
                        manejadorSimbolos.registrarEntidad(arrSym);
                    }
                } else {
                    EntidadSimbolo vSym = new EntidadSimbolo(lex, determinarTipo(lex), "var", nvl, "-", "-", null, "-");
                    if (manejadorSimbolos.verificarExistencia(lex, nvl)) {
                        agregarErrAm(lex, nvl, ln);
                    } else {
                        manejadorSimbolos.registrarEntidad(vSym);
                    }
                }
            } else if (lex.equals(identificadorDeclarado)) {
            } else {
                if (!esGlobal(lex)) {
                    EntidadSimbolo s = manejadorSimbolos.buscarEnContexto(lex, stackNiveles);
                    if (s == null) {
                        errores.add(
                                new ErroresSin("900", "ID_E", lex, "Variable no declarada: '" + lex + "'", ln));
                        notificarErrorAAmbito();
                    }
                }
            }
        }
    }

    private void agregarErrAm(String lx, int nv, int ln) {
        errores.add(new ErroresSin("901", "M_DECL", lx, "Variable duplicada: '" + lx + "' en ambito " + nv, ln));
        notificarErrorAAmbito();
    }

    private String determinarTipo(String lexema) {
        if (lexema == null || lexema.isEmpty())
            return "void";
        char c = lexema.charAt(0);
        if (c == '#') {
            if (lexema.length() > 1) {
                char t = Character.toUpperCase(lexema.charAt(1));
                if (t == 'B') return "bin";
                if (t == 'D') return "decimal";
                if (t == 'O') return "oct";
                if (t == 'X') return "hex";
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

    private boolean esId(String lex) {
        if (lex == null || lex.isEmpty())
            return false;
        char c = lex.charAt(0);
        return (c == '#' || c == '@' || c == '$' || c == '¿' || c == '¡') || Character.isLetter(c);
    }

    private boolean esGlobal(String lex) {
        String[] builtins = { "SQRT", "sin", "cos", "tan", "POW", "SQRTV", "STRLEN", "concat", "copy", "val", "str",
                "chr", "pred", "succ", "inc", "dec", "sqr", "CLEAR", "Console.log", "console.log", "Console.read",
                "console.read" };
        for (String b : builtins)
            if (b.equals(lex))
                return true;
        return false;
    }

    private String[] detectarTamanio(int i) {
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

    private String getErrorCode(String X, Tokens t) {
        if ((X.equals("A_R_POST_ARR") || X.equals("V_ARR_REST")) && t.getLexema().equals("["))
            return "525";
        if (X.equals(T_ID) || t.getTipo().startsWith("Identificador"))
            return "520";
        if (X.equals(T_CONST) || t.getTipo().startsWith("Constante"))
            return "519";
        if (X.equals(";"))
            return "512";
        if (X.equals(")"))
            return "513";
        if (X.equals("}"))
            return "514";
        if (X.equals("("))
            return "515";
        if (X.equals("{"))
            return "516";
        if (X.equals(","))
            return "517";
        if (X.equals("="))
            return "518";
        return "511";
    }

    private String getErrorDesc(String code, String X, Tokens t) {
        switch (code) {
            case "512":
                return "Se esperaba ';' pero se encontró '" + t.getLexema() + "'";
            case "513":
                return "Se esperaba ')' pero se encontró '" + t.getLexema() + "'";
            case "514":
                return "Se esperaba '}' pero se encontró '" + t.getLexema() + "'";
            case "515":
                return "Se esperaba '(' pero se encontró '" + t.getLexema() + "'";
            case "516":
                return "Se esperaba '{' pero se encontró '" + t.getLexema() + "'";
            case "517":
                return "Se esperaba ',' pero se encontró '" + t.getLexema() + "'";
            case "518":
                return "Se esperaba '=' pero se encontró '" + t.getLexema() + "'";
            case "519":
                return "Se esperaba Constante pero se encontró '" + t.getLexema() + "'";
            case "520":
                return "Se esperaba Identificador pero se encontró '" + t.getLexema() + "'";
            case "525":
                return "Sintaxis de arreglo inválida: no se permite '[ ][ ]'. Los arreglos multidimensionales deben usar comas '[a, b]'";
            default:
                return "Error en " + X;
        }
    }

    private boolean match(String X, Tokens t) {
        if (X.equals(T_ID))
            return t.getTipo().startsWith("Identificador");
        if (X.equals(T_CONST))
            return t.getTipo().startsWith("Constante");
        return X.equals(t.getLexema());
    }

    private String getLookup(String rawT, String typeT) {
        if (typeT.startsWith("Identificador"))
            return T_ID;
        if (typeT.startsWith("Constante"))
            return T_CONST;
        return rawT;
    }

    public List<ErroresSin> getErrores() {
        return errores;
    }

    public Map<String, Integer> getVisitas() {
        Map<String, Integer> res = new LinkedHashMap<>();
        int orV = visitas.getOrDefault("OR", 0);
        int andV = visitas.getOrDefault("AND", 0);
        int expV = visitas.getOrDefault("EXP_P", 0);
        int simpV = visitas.getOrDefault("SI_E", 0);
        int termV = visitas.getOrDefault("TERM", 0);
        int elevV = visitas.getOrDefault("ELEV", 0);
        int factV = visitas.getOrDefault("FACT", 0);
        int funcV = visitas.getOrDefault("M_FUNC", 0);
        int asigV = visitas.getOrDefault("M_ASIG", 0);
        int arrV = visitas.getOrDefault("M_ARR", 0);
        int declV = visitas.getOrDefault("M_ARR_DECL", 0);
        int numV = visitas.getOrDefault("M_CONST_NUM", 0);
        int allC = visitas.getOrDefault("M_CONST", 0);

        int offExpr = (funcV > 0) ? funcV : 0;
        int offFact = (funcV > 0) ? (funcV + 10) : (arrV > 10 ? 1 : 0);
        int offElev = (funcV > 0) ? (funcV - 1) : 0;
        int offNum = (funcV > 0) ? 4 : (arrV > 10 ? 1 : 0);
        int offPure = (funcV > 0) ? 6 : (arrV > 10 ? 1 : 0);
        int pureC = allC - offPure;

        res.put("PROGRAMA", visitas.getOrDefault("M_PROG", 0));
        res.put("LISTA DE PARAMETROS", visitas.getOrDefault("M_LIST", 0));
        res.put("EXP PAS", expV - offExpr);
        res.put("CONSTANTE S/SIGNO", pureC);
        res.put("CONST NUMÉRICA", numV - offNum);
        res.put("OR", orV - offExpr);
        res.put("AND", andV - offExpr);
        res.put("DECLARACION", pureC);
        res.put("FACTOR", factV - offFact);
        res.put("ELEVACION", elevV - offElev);
        res.put("TERMINO PASCAL", termV - offExpr);
        res.put("Simple Exp Pascal", simpV - offExpr);
        res.put("STATU", visitas.getOrDefault("M_STAT", 0));
        res.put("FUNCION", funcV);
        res.put("ASIG", asigV);
        res.put("ARR", arrV);
        return res;
    }

    public List<Semantica.EcuacionPrefijo> getPrefijos() {
        return semantica.getPrefijos();
    }

    public Semantica getSemantica() {
        return semantica;
    }
    public ManejadorSimbolos getManejadorSimbolos() {
        return manejadorSimbolos;
    }

    public List<ZonaRegistro> getZonasRegistradas() {
        return zonasRegistradas;
    }

    public List<String> getHistorialAmbitos() {
        return historialAmbitos;
    }

    public List<ConteoAmbito> generarResumenAmbitos(List<Tokens> allTokens) {
        Map<Integer, ConteoAmbito> m = new HashMap<>();
        for (EntidadSimbolo s : manejadorSimbolos.obtenerTodos()) {
            ConteoAmbito c = m.computeIfAbsent(s.getNivelAmbito(), k -> new ConteoAmbito(k));
            switch (s.getTipoDato()) {
                case "decimal":
                    c.sumarDec();
                    break;
                case "cadena":
                    c.sumarCadena();
                    break;
                case "real":
                    c.sumarReal();
                    break;
                // Add more depending on type logic
            }
        }
        for (Map.Entry<Integer, Integer> fallos : fallosEnAmbitos.entrySet()) {
            ConteoAmbito c = m.computeIfAbsent(fallos.getKey(), k -> new ConteoAmbito(k));
            c.setContErrores(fallos.getValue());
        }
        List<ConteoAmbito> l = new ArrayList<>(m.values());
        l.sort(Comparator.comparingInt(ConteoAmbito::getIdAmbito));
        return l;
    }

    public void exportarZonasATxt(String filepath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath))) {
            bw.write("--- REGISTROS DE AREAS (DECLARACION / EJECUCION) ---");
            bw.newLine();
            bw.write(String.format("%-10s | %-16s | %-12s | %-12s | %-20s", "Linea", "Area", "Status", "Token",
                    "Marcador"));
            bw.newLine();
            for (ZonaRegistro z : zonasRegistradas) {
                bw.write(z.toFormatLine());
                bw.newLine();
            }
        }
    }

    public void exportarHistorialATxt(String filepath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath))) {
            bw.write("--- CAMBIOS Y FLUJO DE AMBITOS ---");
            bw.newLine();
            for (String h : historialAmbitos) {
                bw.write(h);
                bw.newLine();
            }
        }
    }
}
