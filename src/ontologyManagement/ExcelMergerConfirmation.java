package ontologyManagement;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;

import java.awt.Color;
import java.io.*;

public class ExcelMergerConfirmation {

    public static void main(String[] args) throws Exception {
        new NatclinnConf();
        String sourceFilePath = NatclinnConf.folderForData + "/NatclinnProductAbox.xlsx";
        removePaleGreenBackground(sourceFilePath);
    }

    public static void removePaleGreenBackground(String sourceFilePath) throws IOException {

        try (FileInputStream fis = new FileInputStream(sourceFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Couleur à supprimer : vert clair
            XSSFColor paleGreen = new XSSFColor(new Color(220, 255, 220), null);

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                removeGreenFromSheet(sheet, paleGreen);
            }

            try (FileOutputStream fos = new FileOutputStream(sourceFilePath)) {
                workbook.write(fos);
            }

            System.out.println("Fond vert clair supprimé dans : " + sourceFilePath);
        }
    }

    private static void removeGreenFromSheet(Sheet sheet, XSSFColor targetColor) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                CellStyle style = cell.getCellStyle();
                if (style instanceof XSSFCellStyle) {
                    XSSFCellStyle xssfStyle = (XSSFCellStyle) style;
                    XSSFColor fgColor = (XSSFColor) xssfStyle.getFillForegroundColorColor();
                    if (fgColor != null && fgColor.equals(targetColor)) {
                        XSSFCellStyle newStyle = ((XSSFWorkbook) sheet.getWorkbook()).createCellStyle();
                        newStyle.cloneStyleFrom(xssfStyle);
                        newStyle.setFillPattern(FillPatternType.NO_FILL);
                        newStyle.setFillForegroundColor(IndexedColors.AUTOMATIC.getIndex());
                        cell.setCellStyle(newStyle);
                    }
                }
            }
        }
    }
}
