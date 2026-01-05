package ontologyManagement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;

/**
 * Génère OFF_taxonomy_categories_fr.xlsx à partir de OFF_taxonomy_categories.xlsx
 * en ne conservant que les catégories qui possèdent un nom français (Nom FR non vide)
 * ET en excluant en:wines, en:beers et tous leurs descendants.
 *
 * Structure attendue :
 * ID OFF | Nom EN | Nom FR | Nb Parents | Nb Enfants | Parents | Enfants
 *
 * Seules les lignes avec "Nom FR" non vide sont conservées.
 * Les catégories ayant en:wines ou en:beers comme parent (directement ou indirectement) sont exclues.
 */
public class ExtractOFFTaxonomyFr {

    static {
        new NatclinnConf();
    }

    private static final String INPUT_FILE = NatclinnConf.folderForData + "/OFF_taxonomy_categories.xlsx";
    private static final String OUTPUT_FILE = NatclinnConf.folderForData + "/OFF_taxonomy_categories_fr.xlsx";

    public static void main(String[] args) {
        try {
            System.out.println("Lecture : " + INPUT_FILE);
            System.out.println("Écriture : " + OUTPUT_FILE);

            filterWithFrenchLabel();

            System.out.println("Terminé : seules les lignes avec Nom FR non vide ont été conservées.");
            System.out.println("Exclusion appliquée : en:wines, en:beers et tous leurs descendants ont été supprimés.");
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void filterWithFrenchLabel() throws IOException {
        File in = new File(INPUT_FILE);
        if (!in.exists()) {
            throw new IOException("Fichier introuvable : " + INPUT_FILE);
        }

        try (Workbook srcWb = new XSSFWorkbook(new FileInputStream(in));
             Workbook outWb = new XSSFWorkbook()) {

            Sheet src = srcWb.getSheetAt(0);
            if (src == null) {
                throw new IOException("Aucune feuille trouvée dans " + INPUT_FILE);
            }

            Sheet out = outWb.createSheet(src.getSheetName());

            // Identifier la ligne d'en-tête
            Row header = src.getRow(0);
            if (header == null) {
                throw new IOException("Ligne d'en-tête manquante (ligne 0)");
            }

            Map<Integer, String> headers = new HashMap<>();
            int nomFrCol = -1;
            int idOffCol = -1;
            int parentsCol = -1;
            for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String name = cell.getStringCellValue();
                if (name == null) continue;
                headers.put(c, name);

                String norm = name.toLowerCase(Locale.ROOT).replace(" ", "");
                if (norm.equals("nomfr")) {
                    nomFrCol = c;
                } else if (norm.equals("idoff")) {
                    idOffCol = c;
                } else if (norm.equals("parents")) {
                    parentsCol = c;
                }
            }

            if (nomFrCol < 0) {
                throw new IOException("Colonne 'Nom FR' introuvable dans l'en-tête.");
            }
            if (idOffCol < 0) {
                throw new IOException("Colonne 'ID OFF' introuvable dans l'en-tête.");
            }
            if (parentsCol < 0) {
                throw new IOException("Colonne 'Parents' introuvable dans l'en-tête.");
            }

            // Première passe : identifier tous les IDs à exclure (en:wines et descendants)
            Set<String> excludedIds = identifyExcludedCategories(src, idOffCol, parentsCol);

            // Copier l'en-tête
            Row outHeader = out.createRow(0);
            for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                Cell cell = outHeader.createCell(entry.getKey());
                cell.setCellValue(entry.getValue());
            }

            int outRowIdx = 1;
            int lastRow = src.getLastRowNum();
            int filteredCount = 0;
            int alcoholExcludedCount = 0;
            for (int r = 1; r <= lastRow; r++) {
                Row row = src.getRow(r);
                if (row == null) continue;
                
                String idOff = getString(row.getCell(idOffCol));
                if (excludedIds.contains(idOff)) {
                    alcoholExcludedCount++;
                    continue; // exclure en:wines, en:beers et leurs enfants
                }
                
                Cell frCell = row.getCell(nomFrCol);
                String fr = getString(frCell);
                if (fr == null || fr.isBlank()) {
                    filteredCount++;
                    continue; // pas de nom fr → ignorer
                }

                Row outRow = out.createRow(outRowIdx++);
                for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                    int c = entry.getKey();
                    Cell srcCell = row.getCell(c);
                    Cell dstCell = outRow.createCell(c);
                    copyCellValue(srcCell, dstCell);
                }
            }

            // Ajuster les colonnes utilisées
            for (int c : headers.keySet()) {
                out.autoSizeColumn(c);
            }

            try (FileOutputStream fos = new FileOutputStream(OUTPUT_FILE)) {
                outWb.write(fos);
            }
            
            System.out.println("Lignes conservées : " + (outRowIdx - 1));
            System.out.println("Lignes filtrées (sans Nom FR) : " + filteredCount);
            System.out.println("Lignes exclues (en:wines, en:beers et descendants) : " + alcoholExcludedCount);
        }
    }

    private static Set<String> identifyExcludedCategories(Sheet src, int idOffCol, int parentsCol) {
        Set<String> excluded = new HashSet<>();
        Map<String, String> parentsList = new HashMap<>();
        
        // Première passe : construire la map ID -> Parents
        int lastRow = src.getLastRowNum();
        for (int r = 1; r <= lastRow; r++) {
            Row row = src.getRow(r);
            if (row == null) continue;
            String id = getString(row.getCell(idOffCol));
            String parents = getString(row.getCell(parentsCol));
            if (id != null && !id.isBlank()) {
                parentsList.put(id, parents != null ? parents : "");
            }
        }
        
        // Ajouter en:wines et en:beers à l'ensemble d'exclusion
        excluded.add("en:wines");
        excluded.add("en:beers");
        
        // Deuxième passe : trouver tous les descendants (itérativement)
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, String> entry : parentsList.entrySet()) {
                String id = entry.getKey();
                if (!excluded.contains(id)) {
                    String parentStr = entry.getValue();
                    if (parentStr != null && !parentStr.isBlank()) {
                        // Vérifier si cette catégorie a un parent dans excluded
                        String[] parents = parentStr.split(",");
                        for (String parent : parents) {
                            String p = parent.trim();
                            if (excluded.contains(p)) {
                                excluded.add(id);
                                changed = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return excluded;
    }

    private static void copyCellValue(Cell src, Cell dst) {
        if (src == null) {
            dst.setBlank();
            return;
        }
        CellType type = src.getCellType();
        switch (type) {
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
            default:
                dst.setBlank();
        }
    }

    private static String getString(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        switch (type) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); } catch (Exception e) { return cell.getCellFormula(); }
            default: return null;
        }
    }
}
