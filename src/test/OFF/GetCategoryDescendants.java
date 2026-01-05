package test.OFF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Génère un Excel avec les descendants de chaque catégorie parent.
 * Structure : Catégorie | Enfant1 | Enfant2 | Enfant3 | ...
 */
public class GetCategoryDescendants {

    static {
        new NatclinnConf();
    }

    private static final String INPUT_FILE = NatclinnConf.folderForData + "/OFF_taxonomy_categories_fr.xlsx";
    private static final String OUTPUT_FILE = NatclinnConf.folderForData + "/category_descendants.xlsx";

    // Catégories parent à analyser (base fixe)
    private static final Map<String, String> BASE_PARENT_CATEGORIES = new HashMap<String, String>() {{
        put("Additifs alimentaires", "en:food-additives");
        put("Aides culinaires", "en:cooking-helpers");
        put("Aliments et boissons à base de végétaux", "en:plant-based-foods-and-beverages");
        put("Aliments festifs", "en:festive-foods");
        put("Aliments lyophilisées", "en:freeze-dried-foods");
        put("Aliments pour bébé", "en:baby-foods");
        put("Alternatives à la viande", "en:meat-alternatives");
        put("Assortiments d'aliments", "en:variety-packs");
        put("Biscuits sucrés & biscuits apéritifs", "en:biscuits-and-crackers");
        put("Boissons et préparations de boissons", "en:beverages-and-beverages-preparations");
        put("Bouillons", "en:broths");
        put("Brochettes", "en:skewers");
        put("Cacao et dérivés", "en:cocoa-and-its-products");
        put("Chips et frites", "en:chips-and-fries");
        put("Compléments alimentaires", "en:dietary-supplements");
        put("Condiments", "en:condiments");
        put("Conserves", "en:canned-foods");
        put("Crêpes et galettes", "en:crepes-and-galettes");
        put("Décorations alimentaires", "en:food-decorations");
        put("Desserts", "en:desserts");
        put("Édulcorants", "en:sweeteners");
        put("Farces", "en:stuffing");
        put("Frais", "en:fresh-foods");
        put("Fritures", "en:fried-foods");
        put("Fromages blancs - petit suisses et skyr", "en:fromages-blancs-petit-suisses-and-skyr");
        put("Kits repas", "en:meal-kits");
        put("Matières grasses", "en:fats");
        put("Mauvais product type", "en:incorrect-product-type");
        put("Petit-déjeuners", "en:breakfasts");
        put("Pizzas tartes salées et quiches", "en:pizzas-pies-and-quiches");
        put("Plats préparés", "en:meals");
        put("Poissons et viandes et oeufs", "en:fish-and-meat-and-eggs");
        put("Produits à tartiner", "en:spreads");
        put("Produits artisanaux", "en:artisan-products");
        put("Produits de la mer", "en:seafood");
        put("Produits de la ruche", "en:bee-products");
        put("Produits de montagne", "en:mountain-products");
        put("Produits d'élevages", "en:farming-products");
        put("Produits déshydratés", "en:dried-products");
        put("Produits fermentés", "en:fermented-foods");
        put("Produits laitiers", "en:dairies");
        put("Produits non pasteurisés", "en:unpasteurised-products");
        put("Produits panés", "en:breaded-products");
        put("Produits pasteurisés", "en:pasteurised-products");
        put("Produits veganes", "en:vegan-products");
        put("Purées", "en:purees");
        put("Réfrigérés", "en:refrigerated-foods");
        put("Sandwichs", "en:sandwiches");
        put("Sirops", "en:syrups");
        put("Snacks", "en:snacks");
        put("Substituts du caviar", "en:caviar-substitutes");
        put("Surgelés", "en:frozen-foods");
        put("Tartes", "en:pies");
        put("Tartes sucrées", "en:sweet-pies");
        put("Terrines", "en:terrines");
        put("Viandes et dérivés", "en:meats-and-their-products");
        put("Vrac", "en:bulk");
    }};

    public static void main(String[] args) {
        try {
            System.out.println("Lecture : " + INPUT_FILE);
            System.out.println("Génération des descendants...\n");

            generateDescendantsFile();

            System.out.println("\nFichier généré : " + OUTPUT_FILE);
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateDescendantsFile() throws IOException {
        File in = new File(INPUT_FILE);
        if (!in.exists()) {
            throw new IOException("Fichier introuvable : " + INPUT_FILE);
        }

        // Lire le fichier source et construire les mappings
        Map<String, String> idToName = new HashMap<>();
        Map<String, List<String>> idToChildren = new HashMap<>();
        Map<String, String> idToParent = new HashMap<>();

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
            int parentsCol = -1;

            for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String name = cell.getStringCellValue().toLowerCase().replace(" ", "");
                if (name.equals("idoff")) idOffCol = c;
                else if (name.equals("nomfr")) nomFrCol = c;
                else if (name.equals("enfants")) enfantsCol = c;
                else if (name.equals("parents")) parentsCol = c;
            }

            if (idOffCol < 0 || nomFrCol < 0 || enfantsCol < 0 || parentsCol < 0) {
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
                String parents = getString(row.getCell(parentsCol));

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
                    if (parents != null && !parents.isBlank()) {
                        String[] parentArr = parents.split(",");
                        if (parentArr.length > 0) {
                            idToParent.put(idOff, parentArr[0].trim());
                        }
                    }
                }
            }
        }

        // Générer l'Excel avec les descendants
        try (Workbook outWb = new XSSFWorkbook()) {
            Sheet out = outWb.createSheet("Descendants");

            CellStyle headerStyle = createHeaderStyle(outWb);
            CellStyle dataStyle = createDataStyle(outWb);

            // Construire la liste finale des catégories parent : base + toutes les catégories ayant >1 enfant
            Map<String, String> parentCategories = buildParentCategories(idToChildren, idToName);

            // En-tête
            Row headerRow = out.createRow(0);
            Cell c0 = headerRow.createCell(0);
            c0.setCellValue("Parent niveau -8");
            c0.setCellStyle(headerStyle);
            Cell c1 = headerRow.createCell(1);
            c1.setCellValue("Parent niveau -7");
            c1.setCellStyle(headerStyle);
            Cell c2 = headerRow.createCell(2);
            c2.setCellValue("Parent niveau -6");
            c2.setCellStyle(headerStyle);
            Cell c3 = headerRow.createCell(3);
            c3.setCellValue("Parent niveau -5");
            c3.setCellStyle(headerStyle);
            Cell c4 = headerRow.createCell(4);
            c4.setCellValue("Parent niveau -4");
            c4.setCellStyle(headerStyle);
            Cell c5 = headerRow.createCell(5);
            c5.setCellValue("Parent niveau -3");
            c5.setCellStyle(headerStyle);
            Cell c6 = headerRow.createCell(6);
            c6.setCellValue("Parent niveau -2");
            c6.setCellStyle(headerStyle);
            Cell c7 = headerRow.createCell(7);
            c7.setCellValue("Parent niveau -1");
            c7.setCellStyle(headerStyle);
            Cell c8 = headerRow.createCell(8);
            c8.setCellValue("Parent direct");
            c8.setCellStyle(headerStyle);
            Cell c9 = headerRow.createCell(9);
            c9.setCellValue("Catégorie Parent");
            c9.setCellStyle(headerStyle);

            int maxDescendants = 0;

            // Calculer le nombre maximum de descendants
            for (String parentId : parentCategories.values()) {
                Set<String> descendants = getAllDescendants(parentId, idToChildren);
                maxDescendants = Math.max(maxDescendants, descendants.size());
            }

            // Créer les en-têtes pour les descendants
            for (int d = 1; d <= maxDescendants; d++) {
                Cell c = headerRow.createCell(d + 9);
                c.setCellValue("Enfant " + d);
                c.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowIdx = 1;
            for (Map.Entry<String, String> entry : parentCategories.entrySet()) {
                String parentName = entry.getKey();
                String parentId = entry.getValue();

                Set<String> descendants = getAllDescendants(parentId, idToChildren);

                Row dataRow = out.createRow(rowIdx++);
                
                // Colonne 0 : Parent niveau -8
                Cell great7GrandParentCell = dataRow.createCell(0);
                String great7GrandParentId = getAncestor(parentId, 8, idToParent);
                if (great7GrandParentId != null) {
                    String great7GrandParentName = idToName.getOrDefault(great7GrandParentId, great7GrandParentId);
                    great7GrandParentCell.setCellValue(great7GrandParentName + " (" + great7GrandParentId + ")");
                }
                great7GrandParentCell.setCellStyle(dataStyle);
                
                // Colonne 1 : Parent niveau -7
                Cell great6GrandParentCell = dataRow.createCell(1);
                String great6GrandParentId = getAncestor(parentId, 7, idToParent);
                if (great6GrandParentId != null) {
                    String great6GrandParentName = idToName.getOrDefault(great6GrandParentId, great6GrandParentId);
                    great6GrandParentCell.setCellValue(great6GrandParentName + " (" + great6GrandParentId + ")");
                }
                great6GrandParentCell.setCellStyle(dataStyle);
                
                // Colonne 2 : Parent niveau -6
                Cell great5GrandParentCell = dataRow.createCell(2);
                String great5GrandParentId = getAncestor(parentId, 6, idToParent);
                if (great5GrandParentId != null) {
                    String great5GrandParentName = idToName.getOrDefault(great5GrandParentId, great5GrandParentId);
                    great5GrandParentCell.setCellValue(great5GrandParentName + " (" + great5GrandParentId + ")");
                }
                great5GrandParentCell.setCellStyle(dataStyle);
                
                // Colonne 3 : Parent niveau -5
                Cell great4GrandParentCell = dataRow.createCell(3);
                String great4GrandParentId = getAncestor(parentId, 5, idToParent);
                if (great4GrandParentId != null) {
                    String great4GrandParentName = idToName.getOrDefault(great4GrandParentId, great4GrandParentId);
                    great4GrandParentCell.setCellValue(great4GrandParentName + " (" + great4GrandParentId + ")");
                }
                great4GrandParentCell.setCellStyle(dataStyle);
                
                // Colonne 4 : Parent niveau -4
                Cell great3GrandParentCell = dataRow.createCell(4);
                String great3GrandParentId = getAncestor(parentId, 4, idToParent);
                if (great3GrandParentId != null) {
                    String great3GrandParentName = idToName.getOrDefault(great3GrandParentId, great3GrandParentId);
                    great3GrandParentCell.setCellValue(great3GrandParentName + " (" + great3GrandParentId + ")");
                }
                great3GrandParentCell.setCellStyle(dataStyle);
                
                // Colonne 5 : Parent niveau -3
                Cell great2GrandParentCell = dataRow.createCell(5);
                String great2GrandParentId = getAncestor(parentId, 3, idToParent);
                if (great2GrandParentId != null) {
                    String great2GrandParentName = idToName.getOrDefault(great2GrandParentId, great2GrandParentId);
                    great2GrandParentCell.setCellValue(great2GrandParentName + " (" + great2GrandParentId + ")");
                }
                great2GrandParentCell.setCellStyle(dataStyle);
                
                // Colonne 6 : Parent niveau -2 (arrière-grand-parent)
                Cell greatGrandParentCell = dataRow.createCell(6);
                String greatGrandParentId = getAncestor(parentId, 2, idToParent);
                if (greatGrandParentId != null) {
                    String greatGrandParentName = idToName.getOrDefault(greatGrandParentId, greatGrandParentId);
                    greatGrandParentCell.setCellValue(greatGrandParentName + " (" + greatGrandParentId + ")");
                }
                greatGrandParentCell.setCellStyle(dataStyle);
                
                // Colonne 7 : Parent niveau -1 (grand-parent)
                Cell grandParentCell = dataRow.createCell(7);
                String grandParentId = getAncestor(parentId, 1, idToParent);
                if (grandParentId != null) {
                    String grandParentName = idToName.getOrDefault(grandParentId, grandParentId);
                    grandParentCell.setCellValue(grandParentName + " (" + grandParentId + ")");
                }
                grandParentCell.setCellStyle(dataStyle);
                
                // Colonne 8 : Parent direct
                Cell parentDirectCell = dataRow.createCell(8);
                String parentDirectId = idToParent.get(parentId);
                if (parentDirectId != null) {
                    String parentDirectName = idToName.getOrDefault(parentDirectId, parentDirectId);
                    parentDirectCell.setCellValue(parentDirectName + " (" + parentDirectId + ")");
                }
                parentDirectCell.setCellStyle(dataStyle);
                
                // Colonne 9 : Catégorie Parent
                Cell parentCell = dataRow.createCell(9);
                parentCell.setCellValue(parentName + " (" + parentId + ")");
                parentCell.setCellStyle(dataStyle);

                // Colonnes suivantes : descendants
                int col2 = 10;
                for (String descendantId : descendants) {
                    Cell descendantCell = dataRow.createCell(col2++);
                    String descendantName = idToName.getOrDefault(descendantId, descendantId);
                    descendantCell.setCellValue(descendantName + " (" + descendantId + ")");
                    descendantCell.setCellStyle(dataStyle);
                }
            }

            // Ajuster les colonnes
            out.autoSizeColumn(0);
            out.autoSizeColumn(1);
            out.autoSizeColumn(2);
            out.autoSizeColumn(3);
            out.autoSizeColumn(4);
            out.autoSizeColumn(5);
            out.autoSizeColumn(6);
            out.autoSizeColumn(7);
            out.autoSizeColumn(8);
            out.autoSizeColumn(9);
            for (int i = 10; i <= maxDescendants + 9; i++) {
                out.setColumnWidth(i, 8000);
            }

            // Écrire le fichier
            try (FileOutputStream fos = new FileOutputStream(OUTPUT_FILE)) {
                outWb.write(fos);
            }

            System.out.println("Total catégories parent (base + ajoutées) : " + parentCategories.size());
            System.out.println("Descendants max par catégorie : " + maxDescendants);
        }
    }

    private static Map<String, String> buildParentCategories(Map<String, List<String>> idToChildren, Map<String, String> idToName) {
        Map<String, String> parents = new LinkedHashMap<>(BASE_PARENT_CATEGORIES);

        for (Map.Entry<String, List<String>> entry : idToChildren.entrySet()) {
            String id = entry.getKey();
            List<String> children = entry.getValue();
            if (children != null && children.size() > 1 && !parents.containsValue(id)) {
                String name = idToName.get(id);
                String label = (name != null && !name.isBlank()) ? name : id;
                parents.put(label, id);
            }
        }

        return parents;
    }

    private static Set<String> getAllDescendants(String parentId, Map<String, List<String>> idToChildren) {
        Set<String> descendants = new HashSet<>();
        Set<String> visited = new HashSet<>();

        addDescendantsRecursive(parentId, idToChildren, descendants, visited);

        return descendants;
    }

    private static String getAncestor(String id, int levels, Map<String, String> idToParent) {
        String current = id;
        for (int i = 0; i < levels; i++) {
            current = idToParent.get(current);
            if (current == null) return null;
        }
        return current;
    }

    private static void addDescendantsRecursive(String id, Map<String, List<String>> idToChildren,
                                                  Set<String> descendants, Set<String> visited) {
        if (visited.contains(id)) return;
        visited.add(id);

        List<String> children = idToChildren.get(id);
        if (children != null) {
            // Si plus de deux enfants, on ajoute les enfants directs mais on ne descend pas plus loin
            if (children.size() > 10) {
                descendants.addAll(children);
                return;
            }

            // Sinon on descend récursivement
            for (String child : children) {
                descendants.add(child);
                addDescendantsRecursive(child, idToChildren, descendants, visited);
            }
        }
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

    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
