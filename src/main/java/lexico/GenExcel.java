package lexico;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class GenExcel {

    public static void export(List<Tokens> tokens,
            List<ErroresLex> errors,
            Map<String, Integer> contadores,
            List<ErroresSin> sintaxisErrors,
            Map<String, Integer> visitas,
            Map<Integer, Integer> erroresAmbito,
            List<EntidadSimbolo> simbolos,
            List<Semantica.ConteoTemporales> conteosSemantica,
            String filePath) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle evenRow = wb.createCellStyle();
            CellStyle errorStyle = wb.createCellStyle();
            XSSFFont errorFont = wb.createFont();
            errorFont.setBold(true);
            errorStyle.setFont(errorFont);

            CellStyle boldStyle = wb.createCellStyle();
            XSSFFont boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            XSSFSheet sheetTokens = wb.createSheet("TOKENS");
            sheetTokens.setDisplayGridlines(false);

            String[] tokenHeaders = { "Estado", "Lexema", "Linea" };
            Row th = sheetTokens.createRow(0);
            for (int i = 0; i < tokenHeaders.length; i++) {
                Cell cell = th.createCell(i);
                cell.setCellValue(tokenHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Tokens t : tokens) {
                Row row = sheetTokens.createRow(rowIdx);
                row.createCell(0).setCellValue(t.getTipo());
                row.createCell(1).setCellValue(t.getLexema());
                row.createCell(2).setCellValue(t.getLinea());
                if (rowIdx % 2 == 0) {
                    for (int c = 0; c < 3; c++) {
                        row.getCell(c).setCellStyle(evenRow);
                    }
                }
                rowIdx++;
            }

            sheetTokens.setColumnWidth(0, 8000);
            sheetTokens.setColumnWidth(1, 6000);
            sheetTokens.setColumnWidth(2, 2000);

            XSSFSheet sheetErrors = wb.createSheet("Errores");
            sheetErrors.setDisplayGridlines(false);

            String[] errHeaders = { "Codigo", "Descripcion", "Lexema", "Tipo de error", "Linea" };
            Row eh = sheetErrors.createRow(0);
            for (int i = 0; i < errHeaders.length; i++) {
                Cell cell = eh.createCell(i);
                cell.setCellValue(errHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int eIdx = 1;
            for (ErroresLex e : errors) {
                Row row = sheetErrors.createRow(eIdx++);
                row.createCell(0).setCellValue(e.getToken());
                row.createCell(1).setCellValue(e.getDescripcion());
                row.createCell(2).setCellValue(e.getLexema());
                row.createCell(3).setCellValue(e.getTipoError());
                row.createCell(4).setCellValue(e.getLinea());
            }

            if (sintaxisErrors != null) {
                for (ErroresSin se : sintaxisErrors) {
                    Row row = sheetErrors.createRow(eIdx++);
                    row.createCell(0).setCellValue(se.getCodigo());
                    row.createCell(1).setCellValue(se.getDescripcion());
                    row.createCell(2).setCellValue(se.getTerminal());
                    
                    String codeStr = se.getCodigo();
                    int codeNum = -1;
                    try {
                        if (codeStr != null) {
                            codeNum = Integer.parseInt(codeStr.trim());
                        }
                    } catch (Exception ignored) {
                    }

                    String tipo = "Error Sintaxis";
                    if (codeNum == 900 || codeNum == 901) {
                        tipo = "Error Ambito";
                    } else if (codeNum >= 903 || "Semantica".equalsIgnoreCase(se.getNoTerminal())) {
                        tipo = "Error Semantico";
                    }
                    row.createCell(3).setCellValue(tipo);
                    row.createCell(4).setCellValue(se.getLinea());
                }
            }

            sheetErrors.setColumnWidth(0, 3000);
            sheetErrors.setColumnWidth(1, 7000);
            sheetErrors.setColumnWidth(2, 12000);
            sheetErrors.setColumnWidth(3, 5000);
            sheetErrors.setColumnWidth(4, 5000);
            sheetErrors.setColumnWidth(5, 2000);

            XSSFSheet sheetCount = wb.createSheet("CONTADORES");
            sheetCount.setDisplayGridlines(false);
            Row header1 = sheetCount.createRow(0);

            header1.createCell(0).setCellValue("Errores");
            sheetCount.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 1, 0, 0));

            header1.createCell(1).setCellValue("Identificadores");
            sheetCount.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 1, 10));

            header1.createCell(11).setCellValue("Palabras reservadas");
            sheetCount.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 1, 11, 11));

            header1.createCell(12).setCellValue("Constantes");
            sheetCount.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 12, 20));

            header1.createCell(21).setCellValue("Operadores");
            sheetCount.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 21, 32));

            Row header2 = sheetCount.createRow(1);

            String[] subHeaders = {
                    null,
                    "cadena",
                    "Binario",
                    "Decimal",
                    "Octal",
                    "Hexadecimal",
                    "Real",
                    "Exponencial",
                    "Booleanas",
                    "Registro",
                    "Comentarios",
                    null,
                    "cadena",
                    "Binario",
                    "Decimal",
                    "Octal",
                    "Hexadecimal",
                    "Real",
                    "Exponencial",
                    "Booleanas",
                    "nula",
                    "postfix",
                    "logicos binarios",
                    "control",
                    "matematicos",
                    "exponente",
                    "turno",
                    "relacionales",
                    "sin igualdad",
                    "logicos",
                    "ternario",
                    "asignacion",
                    "agrupamiento"
            };

            for (int i = 0; i < subHeaders.length; i++) {
                if (subHeaders[i] != null) {
                    header2.createCell(i).setCellValue(subHeaders[i]);
                }
            }

            Row dataRow = sheetCount.createRow(2);

            String[] orden = {
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

            int col = 0;
            for (String key : orden) {
                dataRow.createCell(col++).setCellValue(contadores.getOrDefault(key, 0));
            }

            for (int i = 0; i < subHeaders.length; i++) {
                sheetCount.autoSizeColumn(i);
            }

            XSSFSheet sheetSintaxis = wb.createSheet("Sintaxis");
            sheetSintaxis.setDisplayGridlines(false);

            String[] sintaxisHeaders = {
                    "Errores",
                    "PROGRAMA",
                    "LISTA DE PARAMETROS",
                    "EXP PAS",
                    "CONSTANTE S/SIGNO",
                    "CONST NUMÃ‰RICA",
                    "OR",
                    "AND",
                    "DECLARACION",
                    "FACTOR",
                    "ELEVACION",
                    "TERMINO PASCAL",
                    "Simple Exp Pascal",
                    "STATU",
                    "Funcion",
                    "ASIG",
                    "ARR"
            };

            CellStyle plainHeaderStyle = wb.createCellStyle();
            XSSFFont plainFont = wb.createFont();
            plainFont.setBold(false);
            plainFont.setFontHeightInPoints((short) 11);
            plainHeaderStyle.setFont(plainFont);
            plainHeaderStyle.setAlignment(HorizontalAlignment.CENTER);

            Row sintaxisHeaderRow = sheetSintaxis.createRow(0);
            for (int i = 0; i < sintaxisHeaders.length; i++) {
                Cell cell = sintaxisHeaderRow.createCell(i);
                cell.setCellValue(sintaxisHeaders[i]);
                cell.setCellStyle(plainHeaderStyle);
            }

            String[] ntKeys = {
                    null,
                    "PROGRAMA",
                    "LISTA DE PARAMETROS",
                    "EXP PAS",
                    "CONSTANTE S/SIGNO",
                    "CONST NUMÃ‰RICA",
                    "OR",
                    "AND",
                    "DECLARACION",
                    "FACTOR",
                    "ELEVACION",
                    "TERMINO PASCAL",
                    "Simple Exp Pascal",
                    "STATU",
                    "FUNCION",
                    "ASIG",
                    "ARR"
            };
            int[] sintaxisCounts = new int[sintaxisHeaders.length];
            sintaxisCounts[0] = (sintaxisErrors != null) ? sintaxisErrors.size() : 0;
            if (visitas != null) {
                for (int i = 1; i < ntKeys.length; i++) {
                    if (ntKeys[i] != null) {
                        sintaxisCounts[i] = visitas.getOrDefault(ntKeys[i], 0);
                    }
                }
            }

            Row sintaxisDataRow = sheetSintaxis.createRow(1);
            for (int i = 0; i < sintaxisCounts.length; i++) {
                sintaxisDataRow.createCell(i).setCellValue(sintaxisCounts[i]);
            }

            for (int i = 0; i < sintaxisHeaders.length; i++) {
                sheetSintaxis.setColumnWidth(i, 4500);
            }

            XSSFSheet sheetAmbito = wb.createSheet("Tabla de simbolos");
            sheetAmbito.setDisplayGridlines(false);
            String[] ambitoHeaders = { "id", "tipo", "Clase", "amb", "Tarr", "DimArr", "NoPar", "TParr" };
            Row ambitoHeaderRow = sheetAmbito.createRow(0);
            for (int i = 0; i < ambitoHeaders.length; i++) {
                Cell cell = ambitoHeaderRow.createCell(i);
                cell.setCellValue(ambitoHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            String[][] rawAmbito;
            if (simbolos != null) {
                rawAmbito = new String[simbolos.size()][8];
                for (int i = 0; i < simbolos.size(); i++) {
                    EntidadSimbolo s = simbolos.get(i);
                    rawAmbito[i][0] = s.getIdentificador();
                    rawAmbito[i][1] = s.getTipoDato();
                    rawAmbito[i][2] = s.getClasificacion();
                    rawAmbito[i][3] = String.valueOf(s.getNivelAmbito());
                    rawAmbito[i][4] = s.getTipoArreglo();
                    rawAmbito[i][5] = s.getDimensionArreglo();
                    rawAmbito[i][6] = s.getNumeroParametros() != null ? String.valueOf(s.getNumeroParametros()) : "";
                    rawAmbito[i][7] = s.getTipoParametros();
                }
            } else {
                rawAmbito = new String[0][0];
            }
            int aIdx = 1;
            for (String[] rowData : rawAmbito) {
                Row row = sheetAmbito.createRow(aIdx++);
                for (int c = 0; c < Math.min(rowData.length, 8); c++) {
                    row.createCell(c).setCellValue(rowData[c] != null ? rowData[c] : "");
                }
                if (aIdx % 2 == 0) {
                    for (int c = 0; c < 8; c++) {
                        if (row.getCell(c) != null)
                            row.getCell(c).setCellStyle(evenRow);
                    }
                }
            }
            int[] ambitoWidths = { 5000, 4000, 4000, 3000, 4000, 4000, 3000, 5000 };
            for (int i = 0; i < ambitoWidths.length; i++) {
                sheetAmbito.setColumnWidth(i, ambitoWidths[i]);
            }

            XSSFSheet sheetEval = wb.createSheet("Ambito");
            sheetEval.setDisplayGridlines(false);

            String[] evalHeaders = { "Ambito", "Bin", "Dec", "Oct", "Hex", "Real", "exp", "Char", "Cadena", "Boolean",
                    "Errores", "total" };
            Row evHeaderRow = sheetEval.createRow(0);
            for (int i = 0; i < evalHeaders.length; i++) {
                Cell cell = evHeaderRow.createCell(i);
                cell.setCellValue(evalHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int maxAmbito = 0;
            for (String[] rowData : rawAmbito) {
                if (rowData == null || rowData.length < 4)
                    continue;
                try {
                    int numAmb = Integer.parseInt(rowData[3]);
                    if (numAmb > maxAmbito)
                        maxAmbito = numAmb;
                } catch (Exception e) {
                }
            }
            if (erroresAmbito != null) {
                for (Integer amb : erroresAmbito.keySet()) {
                    if (amb > maxAmbito)
                        maxAmbito = amb;
                }
            }

            int totalBin = 0, totalDec = 0, totalOct = 0, totalHex = 0, totalReal = 0;
            int totalExp = 0, totalChar = 0, totalCadena = 0, totalBool = 0, totalErr = 0;

            for (int i = 0; i <= maxAmbito; i++) {
                int cBin = 0, cDec = 0, cOct = 0, cHex = 0, cReal = 0;
                int cExp = 0, cChar = 0, cCadena = 0, cBool = 0;

                for (String[] rowData : rawAmbito) {
                    if (rowData == null || rowData.length < 4)
                        continue;
                    try {
                        int numAmb = Integer.parseInt(rowData[3]);
                        if (numAmb == i) {
                            String tipo = rowData[1] != null ? rowData[1].toLowerCase() : "";
                            switch (tipo) {
                                case "bin":
                                    cBin++;
                                    break;
                                case "decimal":
                                    cDec++;
                                    break;
                                case "oct":
                                    cOct++;
                                    break;
                                case "hex":
                                    cHex++;
                                    break;
                                case "real":
                                    cReal++;
                                    break;
                                case "exp":
                                    cExp++;
                                    break;
                                case "char":
                                    cChar++;
                                    break;
                                case "cadena":
                                    cCadena++;
                                    break;
                                case "bool":
                                    cBool++;
                                    break;
                            }
                        }
                    } catch (Exception e) {
                    }
                }

                int cErr = (erroresAmbito != null) ? erroresAmbito.getOrDefault(i, 0) : 0;

                int filaTotal = cBin + cDec + cOct + cHex + cReal + cExp + cChar + cCadena + cBool + cErr;

                Row r = sheetEval.createRow(i + 1);
                r.createCell(0).setCellValue(i);
                r.createCell(1).setCellValue(cBin);
                r.createCell(2).setCellValue(cDec);
                r.createCell(3).setCellValue(cOct);
                r.createCell(4).setCellValue(cHex);
                r.createCell(5).setCellValue(cReal);
                r.createCell(6).setCellValue(cExp);
                r.createCell(7).setCellValue(cChar);
                r.createCell(8).setCellValue(cCadena);
                r.createCell(9).setCellValue(cBool);
                r.createCell(10).setCellValue(cErr);
                r.createCell(11).setCellValue(filaTotal);

                totalBin += cBin;
                totalDec += cDec;
                totalOct += cOct;
                totalHex += cHex;
                totalReal += cReal;
                totalExp += cExp;
                totalChar += cChar;
                totalCadena += cCadena;
                totalBool += cBool;
                totalErr += cErr;
            }

            Row rTotal = sheetEval.createRow(maxAmbito + 2);
            rTotal.createCell(0).setCellValue("Total");
            rTotal.createCell(1).setCellValue(totalBin);
            rTotal.createCell(2).setCellValue(totalDec);
            rTotal.createCell(3).setCellValue(totalOct);
            rTotal.createCell(4).setCellValue(totalHex);
            rTotal.createCell(5).setCellValue(totalReal);
            rTotal.createCell(6).setCellValue(totalExp);
            rTotal.createCell(7).setCellValue(totalChar);
            rTotal.createCell(8).setCellValue(totalCadena);
            rTotal.createCell(9).setCellValue(totalBool);
            rTotal.createCell(10).setCellValue(totalErr);
            rTotal.createCell(11).setCellValue(totalBin + totalDec + totalOct + totalHex + totalReal + totalExp
                    + totalChar + totalCadena + totalBool + totalErr);

            for (int i = 0; i < evalHeaders.length; i++) {
                sheetEval.setColumnWidth(i, 3500);
            }

            XSSFSheet sheetSemantica = wb.createSheet("Semantica");
            sheetSemantica.setDisplayGridlines(true);

            String[] semHeaders = { "Linea", "TBin", "TDec", "TOct", "THex", "TReal", "Texp", "TCadena", "TBoolean", "TVariant", "Asignaciones", "Errores" };
            Row shHeaderRow = sheetSemantica.createRow(0);
            for (int i = 0; i < semHeaders.length; i++) {
                Cell cell = shHeaderRow.createCell(i);
                cell.setCellValue(semHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int semIdx = 1;
            int totBin = 0, totDec = 0, totOct = 0, totHex = 0, totReal = 0, totExp = 0, totCadena = 0, totBool = 0, totVariant = 0, totAsig = 0, totErr = 0;
            if (conteosSemantica != null) {
                for (Semantica.ConteoTemporales c : conteosSemantica) {
                    Row row = sheetSemantica.createRow(semIdx++);
                    row.createCell(0).setCellValue(c.linea);
                    row.createCell(1).setCellValue(c.tBin);
                    row.createCell(2).setCellValue(c.tDec);
                    row.createCell(3).setCellValue(c.tOct);
                    row.createCell(4).setCellValue(c.tHex);
                    row.createCell(5).setCellValue(c.tReal);
                    row.createCell(6).setCellValue(c.tExp);
                    row.createCell(7).setCellValue(c.tCadena);
                    row.createCell(8).setCellValue(c.tBoolean);
                    row.createCell(9).setCellValue(c.tVariant);
                    row.createCell(10).setCellValue(c.detalleAsignacion);
                    row.createCell(11).setCellValue(c.errores);

                    totBin += c.tBin;
                    totDec += c.tDec;
                    totOct += c.tOct;
                    totHex += c.tHex;
                    totReal += c.tReal;
                    totExp += c.tExp;
                    totCadena += c.tCadena;
                    totBool += c.tBoolean;
                    totVariant += c.tVariant;
                    totAsig += c.asignaciones;
                    totErr += c.errores;
                }
            }

            // Create some blank rows to match formatting from picture if needed, but not strictly necessary. Let's just create the total row.
            Row rowTot = sheetSemantica.createRow(semIdx + 1); // Leaving one empty row just in case.
            rowTot.createCell(0).setCellValue("Totales");
            rowTot.createCell(1).setCellValue(totBin);
            rowTot.createCell(2).setCellValue(totDec);
            rowTot.createCell(3).setCellValue(totOct);
            rowTot.createCell(4).setCellValue(totHex);
            rowTot.createCell(5).setCellValue(totReal);
            rowTot.createCell(6).setCellValue(totExp);
            rowTot.createCell(7).setCellValue(totCadena);
            rowTot.createCell(8).setCellValue(totBool);
            rowTot.createCell(9).setCellValue(totVariant);
            rowTot.createCell(10).setCellValue(totAsig);
            rowTot.createCell(11).setCellValue(totErr);

            for (int i = 0; i < semHeaders.length; i++) {
                sheetSemantica.setColumnWidth(i, 3500);
            }

            wb.setSheetOrder("Tabla de simbolos", wb.getNumberOfSheets() - 2);
            wb.setSheetOrder("Semantica", wb.getNumberOfSheets() - 1);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(filePath)) {
                wb.write(out);
            }
        }
    }
}
