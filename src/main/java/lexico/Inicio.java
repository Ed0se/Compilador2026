package lexico;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;

public class Inicio extends JFrame {

    private static final Color BG_EDITOR = new Color(0x1E, 0x1E, 0x1E);
    private static final Color BG_PANEL = new Color(0x25, 0x25, 0x26);
    private static final Color BG_TITLEBAR = new Color(0x32, 0x32, 0x32);
    private static final Color BG_TAB_ACTIVE = new Color(0x1E, 0x1E, 0x1E);
    private static final Color BG_TAB_IDLE = new Color(0x2D, 0x2D, 0x2D);
    private static final Color FG_DEFAULT = new Color(0xD4, 0xD4, 0xD4);
    private static final Color ACCENT_BLUE = new Color(0x00, 0x7A, 0xCC);
    private static final Color ACCENT_TEAL = new Color(0x00, 0x69, 0x6E);

    private static final Color BORDER = new Color(0x3E, 0x3E, 0x42);
    private static final Color ROW_EVEN = new Color(0x25, 0x25, 0x26);
    private static final Color ROW_ODD = new Color(0x1E, 0x1E, 0x1E);
    private static final Color STATUS_OK = new Color(0x38, 0x8E, 0x3C);
    private static final Color STATUS_ERR = new Color(0xC6, 0x28, 0x28);
    private static final Font FONT_CODE = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_GUTTER = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_UI_B = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_TAB = new Font("Segoe UI", Font.PLAIN, 12);
    private JTextArea codeArea;
    private JTextArea lineNumbers;
    private JLabel statusLabel;
    private File currentFile;

    private DefaultTableModel tokenTableModel;
    private DefaultTableModel errorTableModel;
    private DefaultTableModel counterTableModel;
    private DefaultTableModel scopeTableModel;
    private DefaultTableModel simbolosTableModel;
    private DefaultTableModel semanticaTableModel;
    private DefaultTableModel reglasTableModel;

    private final Lexico lex = new Lexico();
    private Sintaxis parser;
    private JButton btnCompilar;

    public Inicio() {
        super("Compilador A16130755");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 780);
        setMinimumSize(new Dimension(960, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_EDITOR);
        setLayout(new BorderLayout(0, 0));
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JSplitPane buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildEditorPanel(), buildOutputPanel());
        split.setDividerLocation(580);
        split.setDividerSize(3);
        split.setBorder(null);
        split.setBackground(BORDER);
        split.putClientProperty("JSplitPane.style", "line");
        return split;
    }

    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_EDITOR);
        panel.add(buildEditorTabHeader(), BorderLayout.NORTH);

        codeArea = new JTextArea();
        codeArea.setFont(FONT_CODE);
        codeArea.setBackground(BG_EDITOR);
        codeArea.setForeground(FG_DEFAULT);
        codeArea.setCaretColor(Color.WHITE);
        codeArea.setSelectionColor(new Color(0x26, 0x4F, 0x78));
        codeArea.setSelectedTextColor(FG_DEFAULT);
        codeArea.setTabSize(4);
        codeArea.setLineWrap(false);
        codeArea.setMargin(new Insets(4, 4, 4, 4));

        lineNumbers = new JTextArea("1");
        lineNumbers.setFont(FONT_GUTTER);
        lineNumbers.setBackground(new Color(0x1E, 0x1E, 0x1E));
        lineNumbers.setForeground(new Color(0x6E, 0x70, 0x66));
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setMargin(new Insets(4, 10, 4, 10));

        codeArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers();
            }
        });

        JScrollPane scroll = new JScrollPane(codeArea);
        scroll.setRowHeaderView(lineNumbers);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_EDITOR);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildEditorTabHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setBackground(BG_TAB_IDLE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setPreferredSize(new Dimension(0, 35));
        JLabel tab = new JLabel(" Archivo nuevo  ");
        tab.setFont(FONT_TAB);
        tab.setForeground(Color.WHITE);
        tab.setOpaque(true);
        tab.setBackground(BG_TAB_ACTIVE);
        tab.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 0, 0, ACCENT_BLUE),
                new EmptyBorder(6, 12, 6, 12)));
        header.add(tab);
        return header;
    }

    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PANEL);
        panel.add(buildResultTabs(), BorderLayout.CENTER);
        panel.add(buildButtonBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JTabbedPane buildResultTabs() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(FONT_TAB);
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_INSETS, new Insets(6, 14, 6, 14));
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS, true);
        tabs.addTab("Tokens", buildTokenTab());
        tabs.addTab("Errores", buildErrorTab());
        tabs.addTab("Contadores", buildCounterTab());
        tabs.addTab("Ámbitos", buildScopeTab());
        tabs.addTab("Símbolos", buildSimbolosTab());
        tabs.addTab("Semántica", buildSemanticaTab());
        tabs.addTab("Reglas Semánticas", buildReglasTab());
        return tabs;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(BG_PANEL);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));

        JButton btnAbrir = vscBtn("Cargar", ACCENT_BLUE);
        btnCompilar = vscBtn("Compilar", ACCENT_BLUE);
        JButton btnAvance = vscBtn("Generar Avance", ACCENT_TEAL);

        btnAbrir.addActionListener(e -> abrirArchivo());
        btnCompilar.addActionListener(e -> compilar());
        btnAvance.addActionListener(e -> generarAvance());

        bar.add(btnAbrir);
        bar.add(btnCompilar);
        bar.add(btnAvance);
        return bar;
    }

    private JButton vscBtn(String text, Color base) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? base.darker()
                        : getModel().isRollover() ? base.brighter() : base;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_UI);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 32));
        btn.setMargin(new Insets(0, 0, 0, 0));
        return btn;
    }

    private JScrollPane buildTokenTab() {
        tokenTableModel = new DefaultTableModel(new Object[] { "Tokens", "Lexema", "Linea" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        return styledTable(tokenTableModel, new int[] { 220, 200, 60 });
    }

    private JScrollPane buildErrorTab() {
        errorTableModel = new DefaultTableModel(new Object[] { "Codigo", "Descripcion", "Lexema", "Tipo", "Linea" },
                0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        return styledTable(errorTableModel, new int[] { 65, 260, 140, 90, 60 });
    }

    private JScrollPane buildCounterTab() {
        counterTableModel = new DefaultTableModel(new Object[] { "Categoria", "Cantidad" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        return styledTable(counterTableModel, new int[] { 320, 90 });
    }

    private JScrollPane buildScopeTab() {
        scopeTableModel = new DefaultTableModel(
                new Object[] { "Linea", "Tipo de Area", "Status", "Codigo Tok", "Marcador Semantico" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        return styledTable(scopeTableModel, new int[] { 60, 100, 80, 100, 150 });
    }

    private JScrollPane buildSimbolosTab() {
        simbolosTableModel = new DefaultTableModel(
                new Object[] { "ID", "Tipo", "Clase", "Nivel", "Tarr", "DimArr", "NoPar", "TPar" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        return styledTable(simbolosTableModel, new int[] { 130, 80, 90, 70, 80, 80, 60, 120 });
    }

    private JScrollPane buildSemanticaTab() {
        semanticaTableModel = new DefaultTableModel(new Object[] {
                "Linea", "TBin", "TDec", "TOct", "THex", "TReal", "Texp", "TCad", "TBool", "TVar", "Asig", "Err" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        return styledTable(semanticaTableModel, new int[] { 50, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45 });
    }

    private JScrollPane buildReglasTab() {
        reglasTableModel = new DefaultTableModel(
                new Object[] { "Linea", "Regla", "Descripción", "Situación", "Lexema", "Ámbito" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        return styledTable(reglasTableModel, new int[] { 50, 50, 320, 80, 100, 50 });
    }

    private JScrollPane styledTable(DefaultTableModel model, int[] widths) {
        JTable table = new JTable(model);
        table.setFont(FONT_UI);
        table.setRowHeight(26);
        table.setBackground(ROW_EVEN);
        table.setForeground(FG_DEFAULT);
        table.setGridColor(BORDER);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ACCENT_BLUE);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_TITLEBAR);
        header.setForeground(new Color(0xBB, 0xBB, 0xBB));
        header.setFont(FONT_UI_B);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setPreferredSize(new Dimension(0, 32));
        header.setReorderingAllowed(false);
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
                    int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setFont(FONT_UI);
                if (sel) {
                    setBackground(ACCENT_BLUE);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                    setForeground(FG_DEFAULT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 6));
                return this;
            }
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.getViewport().setBackground(ROW_EVEN);
        return sp;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setPreferredSize(new Dimension(0, 24));
        bar.setBackground(ACCENT_BLUE);
        statusLabel = new JLabel("  Listo");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(Color.WHITE);
        bar.add(statusLabel, BorderLayout.WEST);
        JLabel right = new JLabel("A16130755");
        right.setFont(FONT_SMALL);
        right.setForeground(new Color(0xBB, 0xDD, 0xFF));
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void abrirArchivo() {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filtroTxt = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        fc.setFileFilter(filtroTxt);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            if (selected == null || !selected.getName().toLowerCase().endsWith(".txt")) {
                JOptionPane.showMessageDialog(this,
                        "Solo se admiten archivos con formato .txt",
                        "Tipo de archivo no permitido",
                        JOptionPane.WARNING_MESSAGE);
                setStatus("  Solo se admiten archivos .txt", STATUS_ERR);
                return;
            }
            currentFile = selected;
            try {
                String content = new String(Files.readAllBytes(currentFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                codeArea.setText(content);
                setStatus("  " + currentFile.getName() + " cargado", ACCENT_BLUE);
            } catch (IOException ex) {
                setStatus("  Error al abrir archivo", STATUS_ERR);
            }
        }
    }

    private void compilar() {
        String code = codeArea.getText();
        if (code.trim().isEmpty()) {
            setStatus("El archivo esta vacio", STATUS_ERR);
            return;
        }

        tokenTableModel.setRowCount(0);
        errorTableModel.setRowCount(0);
        counterTableModel.setRowCount(0);
        scopeTableModel.setRowCount(0);
        simbolosTableModel.setRowCount(0);
        semanticaTableModel.setRowCount(0);
        if (reglasTableModel != null)
            reglasTableModel.setRowCount(0);
        parser = null;

        btnCompilar.setEnabled(false);
        setStatus("  Compilando...", ACCENT_BLUE);

        if (currentFile != null) {
            try {
                Files.write(currentFile.toPath(), code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException ex) {
                setStatus("  Error al guardar", STATUS_ERR);
            }
        }

        SwingWorker<Sintaxis, Void> worker = new SwingWorker<Sintaxis, Void>() {
            @Override
            protected Sintaxis doInBackground() {
                lex.analyze(code);
                java.util.List<Tokens> sinCom = new java.util.ArrayList<>();
                for (Tokens t : lex.getTokens())
                    if (!t.getTipo().startsWith("Comentario"))
                        sinCom.add(t);
                Sintaxis localParser = new Sintaxis(sinCom);
                localParser.analizar();
                return localParser;
            }

            @Override
            protected void done() {
                Sintaxis localParser;
                try {
                    localParser = get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Error interno: " + ex.getMessage(), STATUS_ERR);
                    btnCompilar.setEnabled(true);
                    return;
                }
                parser = localParser;

                for (Tokens t : lex.getTokens())
                    tokenTableModel.addRow(new Object[] { t.getTipo(), t.getLexema(), t.getLinea() });

                boolean hayErrores = !lex.getErrors().isEmpty() || !localParser.getErrores().isEmpty();
                if (hayErrores) {
                    for (ErroresLex e : lex.getErrors())
                        errorTableModel.addRow(new Object[] { e.getToken(), e.getDescripcion(),
                                e.getLexema(), e.getTipoError(), e.getLinea() });
                    for (ErroresSin se : localParser.getErrores()) {
                        String codeStr = se.getCodigo();
                        int codeNum = -1;
                        try {
                            if (codeStr != null) {
                                codeNum = Integer.parseInt(codeStr.trim());
                            }
                        } catch (Exception ignored) {
                        }

                        String tipoError = "Error Sintaxis";
                        if (codeNum == 900 || codeNum == 901) {
                            tipoError = "Error Ambito";
                        } else if (codeNum >= 903 || "Semantica".equalsIgnoreCase(se.getNoTerminal())) {
                            tipoError = "Error Semantico";
                        }
                        errorTableModel.addRow(new Object[] { se.getCodigo(), se.getDescripcion(),
                                se.getTerminal(), tipoError, se.getLinea() });
                    }
                    setStatus("  " + (lex.getErrors().size() + localParser.getErrores().size())
                            + " error(es)", STATUS_ERR);
                } else {
                    errorTableModel.addRow(new Object[] { "-", "Compilación correcta", "-", "OK", "-" });
                    setStatus("  Compilación exitosa", STATUS_OK);
                }

                for (Map.Entry<String, Integer> entry : lex.getContadores().entrySet())
                    counterTableModel.addRow(new Object[] { entry.getKey(), entry.getValue() });

                List<ZonaRegistro> zonas = localParser.getZonasRegistradas();
                if (zonas.isEmpty()) {
                    scopeTableModel.addRow(new Object[] { "-", "-", "-", "-", "Sin eventos" });
                } else {
                    for (ZonaRegistro z : zonas) {
                        scopeTableModel.addRow(new Object[] {
                                z.getNumLinea(), z.getCategoria(), z.getEstado(), z.getTokenID(), z.getIndicadorSec()
                        });
                    }
                }

                // Tabla de Símbolos en memoria
                List<EntidadSimbolo> simbolos = localParser.getManejadorSimbolos().obtenerTodos();
                if (simbolos.isEmpty()) {
                    simbolosTableModel.addRow(new Object[] { "-", "-", "-", "-", "-", "-", "-", "Sin Simbolos" });
                } else {
                    for (EntidadSimbolo s : simbolos) {
                        simbolosTableModel.addRow(new Object[] {
                                s.getIdentificador(),
                                s.getTipoDato(),
                                s.getClasificacion(),
                                s.getNivelAmbito(),
                                s.getTipoArreglo(),
                                s.getDimensionArreglo(),
                                s.getNumeroParametros() != null ? String.valueOf(s.getNumeroParametros()) : "-",
                                s.getTipoParametros()
                        });
                    }
                }

                List<Semantica.ConteoTemporales> conteos = localParser.getSemantica().getConteos();
                if (conteos != null && !conteos.isEmpty()) {
                    for (Semantica.ConteoTemporales c : conteos) {
                        String strAsig = c.detalleAsignacion.isEmpty() ? "-" : c.detalleAsignacion;
                        semanticaTableModel.addRow(new Object[] {
                                c.linea, c.tBin, c.tDec, c.tOct, c.tHex, c.tReal, c.tExp, c.tCadena, c.tBoolean,
                                c.tVariant, strAsig, c.errores
                        });
                    }
                } else {
                    semanticaTableModel
                            .addRow(new Object[] { "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-" });
                }

                List<ReglaSemantica> reglas = localParser.getSemantica().getReglasSemanticas();
                if (reglas != null && !reglas.isEmpty()) {
                    for (ReglaSemantica r : reglas) {
                        reglasTableModel.addRow(new Object[] {
                                r.getLinea(),
                                r.getRegla(),
                                r.getDescripcion(),
                                r.getSituacion(),
                                r.getLexema(),
                                r.getAmbito()
                        });
                    }
                }

                btnCompilar.setEnabled(true);
            }
        };
        worker.execute();
    }

    private void generarAvance() {
        if (parser == null) {
            JOptionPane.showMessageDialog(this, "No hay datos para generar el avance. Compile primero.",
                    "Sin datos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        fc.setFileFilter(filtro);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setSelectedFile(new File("Avance_Semantica1.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            if (!dest.getName().toLowerCase().endsWith(".txt")) {
                dest = new File(dest.getAbsolutePath() + ".txt");
            }
            try {
                parser.getSemantica().exportarAvanceTxt(dest.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Archivo de avance generado correctamente en:\n" + dest.getName(),
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                setStatus("  Avance generado: " + dest.getName(), ACCENT_BLUE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al generar avance: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unused")
    private void generarExcel() {
        if (parser == null) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar. Compile primero.",
                    "Sin datos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Sem1_JoseEduardoRamirez_1.xlsx"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                GenExcel.export(lex.getTokens(), lex.getErrors(), lex.getContadores(), parser.getErrores(),
                        parser.getVisitas(), parser.fallosEnAmbitos, parser.getManejadorSimbolos().obtenerTodos(),
                        parser.getSemantica().getConteos(),
                        fc.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Archivo Excel generado correctamente.", "Exito",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al generar Excel: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateLineNumbers() {
        int lines = codeArea.getLineCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++)
            sb.append(i).append('\n');
        lineNumbers.setText(sb.toString());
    }

    private void setStatus(String msg, Color bgColor) {
        statusLabel.setText(msg);
        statusLabel.getParent().setBackground(bgColor);
    }

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("TabbedPane.selectedBackground", BG_TAB_ACTIVE);
            UIManager.put("TabbedPane.underlineColor", ACCENT_BLUE);
            UIManager.put("TabbedPane.inactiveUnderlineColor", new Color(0, 0, 0, 0));
            UIManager.put("TabbedPane.background", BG_PANEL);
            UIManager.put("TabbedPane.tabAreaBackground", BG_TAB_IDLE);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.width", 8);
            UIManager.put("Button.arc", 5);
            UIManager.put("TextComponent.arc", 4);
            UIManager.put("Component.arc", 4);
            UIManager.put("defaultFont", FONT_UI);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new Inicio().setVisible(true));
    }
}
