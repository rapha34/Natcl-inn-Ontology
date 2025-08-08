package ontologyManagement;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import natclinn.util.NatclinnConf;

import java.io.*;

public class ExcelMergerBack {

    public static void main(String[] args) throws Exception {
        new NatclinnConf();
        String sourceFilePath = NatclinnConf.folderForData + "/NatclinnProductAbox.xlsx";
        backAddExcelSheet(sourceFilePath);
    }

    public static void backAddExcelSheet(String sourceFilePath) throws IOException {
        ZipSecureFile.setMinInflateRatio(0.001);
        try (FileInputStream fis = new FileInputStream(sourceFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getSheetName().equals("Categories")) {
                    continue; // Ignore la feuille cachée Categories
                }
                clearGreenCellsAndRemoveBackground(sheet, workbook);
            }

            // Recalcule toutes les formules via Apache POI
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            try (FileOutputStream fos = new FileOutputStream(sourceFilePath)) {
                workbook.write(fos);
            }

            System.out.println("Suppression des cellules vertes terminée dans : " + sourceFilePath);
        }
    }

    /**
     * Efface uniquement le contenu et la couleur des cellules vertes.
     */
    private static void clearGreenCellsAndRemoveBackground(Sheet sheet, Workbook workbook) {
        int lastRowWithId = findLastUsedRowInColumn(sheet, 0);
        CellStyle noColorStyle = workbook.createCellStyle();

        for (int rowIndex = 1; rowIndex <= lastRowWithId; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            for (Cell cell : row) {
                if (isCellLightGreen(cell)) {
                    cell.setBlank();
                    cell.setCellStyle(noColorStyle);
                }
            }
        }
    }

    /**
     * Teste si une cellule possède un fond vert clair spécifique.
     */
    private static boolean isCellLightGreen(Cell cell) {
        if (cell == null) return false;
        CellStyle style = cell.getCellStyle();
        if (style == null) return false;
        Color color = style.getFillForegroundColorColor();
        if (color instanceof XSSFColor) {
            XSSFColor xssfColor = (XSSFColor) color;
            byte[] rgb = xssfColor.getRGB();
            if (rgb != null) {
                int r = rgb[0] & 0xFF;
                int g = rgb[1] & 0xFF;
                int b = rgb[2] & 0xFF;
                if (r == 220 && g == 255 && b == 220) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Recherche la dernière ligne avec du contenu dans la colonne spécifiée.
     */
    private static int findLastUsedRowInColumn(Sheet sheet, int columnIndex) {
        int lastRowWithData = -1;
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && cell.getCellType() != CellType.BLANK && !extractCellValueAsString(cell).trim().isEmpty()) {
                    lastRowWithData = rowIndex;
                }
            }
        }
        return lastRowWithData;
    }

    private static String extractCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
