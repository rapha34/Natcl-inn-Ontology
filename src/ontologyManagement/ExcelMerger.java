package ontologyManagement;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.awt.Color;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ExcelMerger {

    
    public static void main(String[] args) throws Exception {
        new NatclinnConf();
        String sourceFilePath = NatclinnConf.folderForData + "/OFF-export.xlsx";
        String destinationFilePath = NatclinnConf.folderForData + "/NatclinnProductAbox.xlsx";
        addExcelSheet(sourceFilePath, destinationFilePath);
    }

    public static void addExcelSheet(String sourceFilePath, String destinationFilePath) throws IOException {
        ZipSecureFile.setMinInflateRatio(0.001); // ou 0.0 pour désactiver la sécurité
        try (
            FileInputStream sourceFis = new FileInputStream(sourceFilePath);
            FileInputStream destFis = new FileInputStream(destinationFilePath);
            Workbook sourceWorkbook = new XSSFWorkbook(sourceFis);
            Workbook destWorkbook = new XSSFWorkbook(destFis)
        ) {
            for (int i = 0; i < sourceWorkbook.getNumberOfSheets(); i++) {
                Sheet sourceSheet = sourceWorkbook.getSheetAt(i);
                String sheetName = sourceSheet.getSheetName();
                Sheet destSheet = destWorkbook.getSheet(sheetName);
                if (destSheet == null) {
                    destSheet = destWorkbook.createSheet(sheetName);
                }

                // Récupération des IDs existants (colonne A, index 0)
                Set<String> existingIds = new HashSet<>();
                for (int r = 1; r <= destSheet.getLastRowNum(); r++) {
                    Row row = destSheet.getRow(r);
                    if (row != null) {
                        Cell idCell = row.getCell(0);
                        if (idCell != null && idCell.getCellType() == CellType.STRING) {
                            existingIds.add(idCell.getStringCellValue().trim());
                        } else if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                            existingIds.add(String.valueOf((long) idCell.getNumericCellValue()));
                        }
                    }
                }

                // Recherche du dernier index de ligne non vide dans la colonne A
                int lastRowWithId = -1;
                for (int r = 0; r <= destSheet.getLastRowNum(); r++) {
                    Row row = destSheet.getRow(r);
                    if (row != null) {
                        Cell idCell = row.getCell(0);
                        if (idCell != null && idCell.getCellType() != CellType.BLANK && !extractCellValueAsString(idCell).trim().isEmpty()) {
                            lastRowWithId = r;
                        }
                    }
                }
                int destRowIndex = lastRowWithId + 1;

                for (int r = 1; r <= sourceSheet.getLastRowNum(); r++) { // sauter l’en-tête
                    Row srcRow = sourceSheet.getRow(r);
                    if (srcRow == null) continue;

                    Cell idCell = srcRow.getCell(0);
                    if (idCell == null) continue;

                    String id = extractCellValueAsString(idCell).trim();
                    if (existingIds.contains(id)) {
                        // System.out.println("Doublon détecté (ignoré) : " + id);
                        // Ignorer doublon
                        continue;
                    }

                    Row dstRow = destSheet.getRow(destRowIndex);
                    if (dstRow == null) {
                        dstRow = destSheet.createRow(destRowIndex);
                    }
                    destRowIndex++;

                    for (int c = 0; c < srcRow.getLastCellNum(); c++) {
                        Cell srcCell = srcRow.getCell(c);
                        // On ne touche QUE les colonnes présentes dans srcRow
                        Cell dstCell = dstRow.getCell(c);
                        if (dstCell == null) {
                            dstCell = dstRow.createCell(c);
                        }
                        if (srcCell != null) {
                            copyCellValue(srcCell, dstCell);
                            CellStyle coloredStyle = cloneAndColorCellStyle(srcCell.getCellStyle(), destWorkbook);
                            dstCell.setCellStyle(coloredStyle);
                        }
                    }
                }
            }

            // Écriture finale
            
            // Recalcule toutes les formules via Apache POI
            FormulaEvaluator evaluator = destWorkbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            // Ou bien demander à Excel de recalculer à l'ouverture
            // if (destWorkbook instanceof XSSFWorkbook) {
            //     ((XSSFWorkbook) destWorkbook).setForceFormulaRecalculation(true);
            // }

            for (int i = 0; i < destWorkbook.getNumberOfSheets(); i++) {
                Sheet sourceSheet = destWorkbook.getSheetAt(i);
                // Ajuster automatiquement la largeur des colonnes sur toutes les feuilles
                autoSizeAllColumns(sourceSheet);
            }  

            try (FileOutputStream out = new FileOutputStream(destinationFilePath)) {
                destWorkbook.write(out);
                System.out.println("Fichier fusionné avec succès : " + destinationFilePath);
            }

        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ajuste automatiquement la largeur de toutes les colonnes d'une feuille
     * en fonction du contenu, avec une largeur maximale pour éviter les colonnes trop larges.
     */
    private static void autoSizeAllColumns(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                int lastCellNum = firstRow.getLastCellNum();
                for (int i = 0; i < lastCellNum; i++) {
                    sheet.autoSizeColumn(i);
                    // Limiter la largeur maximale à 100 caractères (environ 25600 unités)
                    int currentWidth = sheet.getColumnWidth(i);
                    int maxWidth = 25600; // ~100 caractères
                    if (currentWidth > maxWidth) {
                        sheet.setColumnWidth(i, maxWidth);
                    }
                    // Ajouter un peu de padding (5% de plus)
                    int newWidth = Math.min(sheet.getColumnWidth(i) + 512, maxWidth);
                    sheet.setColumnWidth(i, newWidth);
                }
            }
        }
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

    private static void copyCellValue(Cell src, Cell dst) {
        switch (src.getCellType()) {
            case STRING:
                dst.setCellValue(src.getStringCellValue());
                break;
            case NUMERIC:
                dst.setCellValue(src.getNumericCellValue());
                break;
            case BOOLEAN:
                dst.setCellValue(src.getBooleanCellValue());
                break;
            case FORMULA:
                dst.setCellFormula(src.getCellFormula());
                break;
            case BLANK:
                dst.setBlank();
                break;
            default:
                dst.setCellValue(src.toString());
        }
    }


    private static CellStyle cloneAndColorCellStyle(CellStyle originalStyle, Workbook workbook) {
        CellStyle newStyle = workbook.createCellStyle();
        newStyle.cloneStyleFrom(originalStyle);
        if (workbook instanceof XSSFWorkbook) {
            XSSFColor paleGreen = new XSSFColor(new Color(220, 255, 220), null);
            ((XSSFCellStyle) newStyle).setFillForegroundColor(paleGreen);
            newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return newStyle;
    }

}
