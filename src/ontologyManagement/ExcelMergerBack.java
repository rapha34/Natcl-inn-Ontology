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
        ZipSecureFile.setMinInflateRatio(0.001); // ou 0.0 pour désactiver la sécurité
        try (FileInputStream fis = new FileInputStream(sourceFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                removeGreenRows(sheet);
            }

            // Réécriture du même fichier (en écrasant)
            try (FileOutputStream fos = new FileOutputStream(sourceFilePath)) {
                workbook.write(fos);
            }

            System.out.println("Suppression terminée dans : " + sourceFilePath);
        }
    }

    private static void removeGreenRows(Sheet sheet) {
        for (int rowIndex = sheet.getLastRowNum(); rowIndex >= 1; rowIndex--) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            if (isRowLightGreen(row)) {
                sheet.removeRow(row);
                if (rowIndex < sheet.getLastRowNum()) {
                    sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
                }
            }
        }
    }

    private static boolean isRowLightGreen(Row row) {
        for (Cell cell : row) {
            CellStyle style = cell.getCellStyle();
            if (style == null) continue;

            Color color = style.getFillForegroundColorColor();
            if (color instanceof XSSFColor) {
                XSSFColor xssfColor = (XSSFColor) color;
                byte[] rgb = xssfColor.getRGB();
                if (rgb != null) {
                    int r = rgb[0] & 0xFF;
                    int g = rgb[1] & 0xFF;
                    int b = rgb[2] & 0xFF;

                    // Détection du vert très clair 
                    if (r == 220 && g == 255 && b == 220) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
