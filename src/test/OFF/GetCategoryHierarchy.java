package test.OFF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;

/**
 * Génère un Excel avec la hiérarchie complète d'une catégorie donnée.
 * Affiche la catégorie racine et tous ses descendants avec indentation progressive.
 */
public class GetCategoryHierarchy {

    static {
        new NatclinnConf();
    }

    private static final String INPUT_FILE = NatclinnConf.folderForData + "/OFF_taxonomy_categories_fr.xlsx";
    private static final String OUTPUT_FILE = NatclinnConf.folderForData + "/category_hierarchy_";

    public static void main(String[] args) {
        String categoryId = args.length > 0 ? args[0] : "en:food-additives";
        
        try {
            System.out.println("Catégorie recherchée : " + categoryId);
            System.out.println("Lecture : " + INPUT_FILE);
            System.out.println("Génération de la hiérarchie...\n");

            generateHierarchyFile(categoryId);

            String output = OUTPUT_FILE + categoryId.replace(":", "_") + ".xlsx";
            System.out.println("\nFichier généré : " + output);
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateHierarchyFile(String rootCategoryId) throws IOException {
        File in = new File(INPUT_FILE);
        if (!in.exists()) {
            throw new IOException("Fichier introuvable : " + INPUT_FILE);
        }

        // Lire le fichier source
        Map<String, String> idToName = new HashMap<>();
        Map<String, List<String>> idToChildren = new HashMap<>();

        try (Workbook srcWb = new XSSFWorkbook(new FileInputStream(in))) {
            Sheet src = srcWb.getSheetAt(0);
            if (src == null) {
                throw new IOException("Aucune feuille trouvée");
            }

            // Identifier les colonnes
            Row header = src.getRow(0);
            int idOffCol = -1;
            int nomFrCol = -1;
            int enfantsCol = -1;

            for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String name = cell.getStringCellValue().toLowerCase().replace(" ", "");
                if (name.equals("idoff")) idOffCol = c;
                else if (name.equals("nomfr")) nomFrCol = c;
                else if (name.equals("enfants")) enfantsCol = c;
            }

            if (idOffCol < 0 || nomFrCol < 0 || enfantsCol < 0) {
                throw new IOException("Colonnes manquantes");
            }

            // Lire les données
            int lastRow = src.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row row = src.getRow(r);
                if (row == null) continue;

                String idOff = getString(row.getCell(idOffCol));
                String nomFr = getString(row.getCell(nomFrCol));
                String enfants = getString(row.getCell(enfantsCol));

                if (idOff != null && !idOff.isBlank()) {
                    idToName.put(idOff, nomFr);
                    if (enfants != null && !enfants.isBlank()) {
                        List<String> childList = new ArrayList<>();
                        for (String child : enfants.split(",")) {
                            String c = child.trim();
                            if (!c.isEmpty()) childList.add(c);
                        }
                        idToChildren.put(idOff, childList);
                    }
                }
            }
        }

        // Vérifier que la catégorie existe
        if (!idToName.containsKey(rootCategoryId)) {
            throw new IOException("Catégorie non trouvée : " + rootCategoryId);
        }

        // Générer l'Excel avec la hiérarchie
        try (Workbook outWb = new XSSFWorkbook()) {
            Sheet out = outWb.createSheet("Hiérarchie");

            CellStyle headerStyle = createHeaderStyle(outWb);
            CellStyle levelStyle = createLevelStyle(outWb);

            // En-tête
            Row headerRow = out.createRow(0);
            Cell c0 = headerRow.createCell(0);
            c0.setCellValue("Niveau");
            c0.setCellStyle(headerStyle);
            Cell c1 = headerRow.createCell(1);
            c1.setCellValue("Catégorie");
            c1.setCellStyle(headerStyle);

            // Générer la hiérarchie
            int rowIdx = 1;
            rowIdx = buildHierarchy(rootCategoryId, 0, out, rowIdx, idToName, idToChildren, levelStyle);

            // Ajuster les colonnes
            out.autoSizeColumn(0);
            out.setColumnWidth(1, 12000);

            // Écrire le fichier
            String outputFile = OUTPUT_FILE + rootCategoryId.replace(":", "_") + ".xlsx";
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                outWb.write(fos);
            }

            System.out.println("Total catégories affichées : " + (rowIdx - 1));
        }
    }

    private static int buildHierarchy(String categoryId, int level, Sheet sheet, int rowIdx,
                                       Map<String, String> idToName, Map<String, List<String>> idToChildren,
                                       CellStyle style) {
        // Afficher la catégorie actuelle
        Row row = sheet.createRow(rowIdx++);
        Cell levelCell = row.createCell(0);
        levelCell.setCellValue(level);
        levelCell.setCellStyle(style);
        
        Cell nameCell = row.createCell(1);
        String name = idToName.getOrDefault(categoryId, categoryId);
        String indentation = "  ".repeat(level);
        nameCell.setCellValue(indentation + name + " (" + categoryId + ")");
        nameCell.setCellStyle(style);

        // Récursivement afficher les enfants
        List<String> children = idToChildren.get(categoryId);
        if (children != null) {
            for (String child : children) {
                rowIdx = buildHierarchy(child, level + 1, sheet, rowIdx, idToName, idToChildren, style);
            }
        }

        return rowIdx;
    }

    private static String getString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return null;
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createLevelStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
