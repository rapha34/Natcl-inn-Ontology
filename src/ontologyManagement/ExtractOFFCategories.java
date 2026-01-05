package ontologyManagement;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

import natclinn.util.NatclinnConf;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Extraction de la taxonomie des catégories depuis Open Food Facts
 * Télécharge le fichier JSON et génère un fichier Excel structuré
 */
public class ExtractOFFCategories {

    private static final String OFF_TAXONOMY_URL = "https://world.openfoodfacts.org/data/taxonomies/categories.json";
    private static final String USER_AGENT = "NatclinnOntology/1.0 (research project)";
    
    // Structure pour stocker les éléments de taxonomie
    private static class TaxonomyElement {
        String id;
        String nameEn;
        String nameFr;
        List<String> parents;
        List<String> children;
        
        TaxonomyElement(String id) {
            this.id = id;
            this.parents = new ArrayList<>();
            this.children = new ArrayList<>();
        }
    }
    
    private static Map<String, TaxonomyElement> taxonomy = new LinkedHashMap<>();

    public static void main(String[] args) {
        new NatclinnConf();
        try {
            System.out.println("=== Extraction de la taxonomie OFF (catégories) ===");
            System.out.println("Source: " + OFF_TAXONOMY_URL + "\n");
            
            // Télécharger et parser le fichier JSON
            System.out.println("Téléchargement de la taxonomie...");
            downloadAndParseTaxonomy();
            
            System.out.println("\n=== Total de catégories dans la taxonomie: " + taxonomy.size() + " ===\n");
            
            // Générer le fichier Excel
            String outputPath = NatclinnConf.folderForData + "/OFF_taxonomy_categories.xlsx";
            generateExcelFile(outputPath);
            
            System.out.println("Fichier Excel généré: " + outputPath);
            
            // Afficher un aperçu
            displaySummary();
            
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Télécharge et parse le fichier JSON de la taxonomie
     */
    private static void downloadAndParseTaxonomy() throws Exception {
        URL url = new URL(OFF_TAXONOMY_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(30000);  // 30s pour la connexion
        conn.setReadTimeout(60000);     // 60s pour la lecture
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Erreur HTTP: " + responseCode);
        }
        
        long startTime = System.currentTimeMillis();
        
        // Lire le contenu JSON
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }
        
        System.out.println("Téléchargement terminé (" + (jsonContent.length() / 1_000) + " KB)");
        System.out.println("Parsing du JSON...");
        
        // Parser le JSON
        JSONObject taxonomyJson = new JSONObject(jsonContent.toString());
        
        int processed = 0;
        for (String categoryId : taxonomyJson.keySet()) {
            processed++;
            
            if (processed % 500 == 0) {
                System.out.println("  Traité: " + processed + " catégories...");
            }
            
            try {
                JSONObject categoryData = taxonomyJson.getJSONObject(categoryId);
                TaxonomyElement element = new TaxonomyElement(categoryId);
                
                // Extraire les noms
                if (categoryData.has("name")) {
                    JSONObject names = categoryData.getJSONObject("name");
                    if (names.has("en")) {
                        element.nameEn = names.getString("en");
                    }
                    if (names.has("fr")) {
                        element.nameFr = names.getString("fr");
                    }
                }
                
                // Extraire les parents
                if (categoryData.has("parents")) {
                    JSONArray parents = categoryData.getJSONArray("parents");
                    for (int i = 0; i < parents.length(); i++) {
                        element.parents.add(parents.getString(i));
                    }
                }
                
                // Extraire les enfants
                if (categoryData.has("children")) {
                    JSONArray children = categoryData.getJSONArray("children");
                    for (int i = 0; i < children.length(); i++) {
                        element.children.add(children.getString(i));
                    }
                }
                
                taxonomy.put(categoryId, element);
                
            } catch (Exception e) {
                System.err.println("  ⚠ Erreur pour la catégorie: " + categoryId);
            }
        }
        
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Parsing terminé en " + elapsed + " secondes");
        System.out.println("  Total catégories traitées: " + processed);
    }
    
    /**
     * Génère un fichier Excel avec la taxonomie
     */
    private static void generateExcelFile(String outputPath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        
        // Feuille 1: Liste complète des catégories
        Sheet categoriesSheet = workbook.createSheet("Categories");
        createCategoriesSheet(categoriesSheet, workbook);
        
        // Feuille 2: Relations hiérarchiques (parent-enfant)
        Sheet hierarchySheet = workbook.createSheet("Hierarchy");
        createHierarchySheet(hierarchySheet, workbook);
        
        // Feuille 3: Statistiques
        Sheet statsSheet = workbook.createSheet("Statistics");
        createStatsSheet(statsSheet, workbook);
        
        // Écrire le fichier
        try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
            workbook.write(fileOut);
        }
        
        workbook.close();
    }
    
    /**
     * Crée la feuille avec la liste des catégories
     */
    private static void createCategoriesSheet(Sheet sheet, Workbook workbook) {
        // Styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID OFF", "Nom EN", "Nom FR", "Nb Parents", "Nb Enfants", "Parents", "Enfants"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Données - Trier par ID
        List<TaxonomyElement> sortedElements = new ArrayList<>(taxonomy.values());
        sortedElements.sort(Comparator.comparing(e -> e.id));
        
        int rowNum = 1;
        for (TaxonomyElement element : sortedElements) {
            Row row = sheet.createRow(rowNum++);
            
            createCell(row, 0, element.id, dataStyle);
            createCell(row, 1, element.nameEn != null ? element.nameEn : "", dataStyle);
            createCell(row, 2, element.nameFr != null ? element.nameFr : "", dataStyle);
            createCell(row, 3, element.parents.size(), dataStyle);
            createCell(row, 4, element.children.size(), dataStyle);
            createCell(row, 5, String.join(", ", element.parents), dataStyle);
            createCell(row, 6, String.join(", ", element.children), dataStyle);
        }
        
        // Auto-dimensionner les colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (i < 5) {
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }
        }
    }
    
    /**
     * Crée la feuille avec les relations hiérarchiques
     */
    private static void createHierarchySheet(Sheet sheet, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Catégorie", "Nom EN", "Parent", "Nom Parent EN", "Niveau"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Données - Relations parent-enfant
        List<String[]> relations = new ArrayList<>();
        for (TaxonomyElement element : taxonomy.values()) {
            if (!element.parents.isEmpty()) {
                for (String parentId : element.parents) {
                    TaxonomyElement parent = taxonomy.get(parentId);
                    relations.add(new String[] {
                        element.id,
                        element.nameEn != null ? element.nameEn : "",
                        parentId,
                        parent != null && parent.nameEn != null ? parent.nameEn : "",
                        String.valueOf(calculateLevel(element.id))
                    });
                }
            }
        }
        
        // Trier par catégorie puis parent
        relations.sort((a, b) -> {
            int cmp = a[0].compareTo(b[0]);
            return cmp != 0 ? cmp : a[2].compareTo(b[2]);
        });
        
        int rowNum = 1;
        for (String[] relation : relations) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < relation.length; i++) {
                if (i == 4) {
                    try {
                        createCell(row, i, Integer.parseInt(relation[i]), dataStyle);
                    } catch (NumberFormatException e) {
                        createCell(row, i, relation[i], dataStyle);
                    }
                } else {
                    createCell(row, i, relation[i], dataStyle);
                }
            }
        }
        
        // Auto-dimensionner
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }
    
    /**
     * Crée la feuille avec les statistiques
     */
    private static void createStatsSheet(Sheet sheet, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        int rowNum = 0;
        
        // Statistiques générales
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Statistiques Générales");
        titleCell.setCellStyle(headerStyle);
        
        sheet.createRow(rowNum++);
        
        addStatRow(sheet, rowNum++, "Total catégories", taxonomy.size(), headerStyle, dataStyle);
        
        int withNames = (int) taxonomy.values().stream()
            .filter(e -> e.nameEn != null || e.nameFr != null).count();
        addStatRow(sheet, rowNum++, "Avec nom", withNames, headerStyle, dataStyle);
        
        int withParents = (int) taxonomy.values().stream()
            .filter(e -> !e.parents.isEmpty()).count();
        addStatRow(sheet, rowNum++, "Avec parents", withParents, headerStyle, dataStyle);
        
        int withChildren = (int) taxonomy.values().stream()
            .filter(e -> !e.children.isEmpty()).count();
        addStatRow(sheet, rowNum++, "Avec enfants", withChildren, headerStyle, dataStyle);
        
        int roots = (int) taxonomy.values().stream()
            .filter(e -> e.parents.isEmpty()).count();
        addStatRow(sheet, rowNum++, "Racines (sans parent)", roots, headerStyle, dataStyle);
        
        int leaves = (int) taxonomy.values().stream()
            .filter(e -> e.children.isEmpty()).count();
        addStatRow(sheet, rowNum++, "Feuilles (sans enfant)", leaves, headerStyle, dataStyle);
        
        rowNum += 2;
        
        // Top 10 des catégories avec le plus d'enfants
        Row titleRow2 = sheet.createRow(rowNum++);
        Cell titleCell2 = titleRow2.createCell(0);
        titleCell2.setCellValue("Top 10 - Plus d'enfants");
        titleCell2.setCellStyle(headerStyle);
        
        sheet.createRow(rowNum++);
        
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "Catégorie", headerStyle);
        createCell(headerRow, 1, "Nom EN", headerStyle);
        createCell(headerRow, 2, "Nb Enfants", headerStyle);
        
        List<TaxonomyElement> topChildren = new ArrayList<>(taxonomy.values());
        topChildren.sort((a, b) -> Integer.compare(b.children.size(), a.children.size()));
        
        for (int i = 0; i < Math.min(10, topChildren.size()); i++) {
            TaxonomyElement element = topChildren.get(i);
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, element.id, dataStyle);
            createCell(row, 1, element.nameEn != null ? element.nameEn : "", dataStyle);
            createCell(row, 2, element.children.size(), dataStyle);
        }
        
        // Auto-dimensionner
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0) + 2000);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1) + 2000);
    }
    
    /**
     * Calcule le niveau hiérarchique d'une catégorie (profondeur depuis la racine)
     */
    private static int calculateLevel(String categoryId) {
        TaxonomyElement element = taxonomy.get(categoryId);
        if (element == null || element.parents.isEmpty()) {
            return 0;
        }
        
        int maxParentLevel = 0;
        for (String parentId : element.parents) {
            maxParentLevel = Math.max(maxParentLevel, calculateLevel(parentId));
        }
        
        return maxParentLevel + 1;
    }
    
    /**
     * Affiche un résumé dans la console
     */
    private static void displaySummary() {
        System.out.println("\n=== Résumé de la taxonomie ===");
        System.out.println("Total catégories: " + taxonomy.size());
        
        long withNames = taxonomy.values().stream()
            .filter(e -> e.nameEn != null || e.nameFr != null).count();
        System.out.println("Avec nom: " + withNames);
        
        long withParents = taxonomy.values().stream()
            .filter(e -> !e.parents.isEmpty()).count();
        System.out.println("Avec parents: " + withParents);
        
        long withChildren = taxonomy.values().stream()
            .filter(e -> !e.children.isEmpty()).count();
        System.out.println("Avec enfants: " + withChildren);
        
        long roots = taxonomy.values().stream()
            .filter(e -> e.parents.isEmpty()).count();
        System.out.println("Racines (sans parent): " + roots);
        
        // Afficher quelques exemples de hiérarchies
        System.out.println("\n=== Exemples de hiérarchies ===");
        
        // Chercher "en:meals"
        if (taxonomy.containsKey("en:meals")) {
            System.out.println("\nExemple: en:meals");
            displayHierarchy("en:meals", 0);
        }
        
        // Chercher "en:moussaka"
        if (taxonomy.containsKey("en:moussaka")) {
            System.out.println("\nExemple: en:moussaka");
            displayHierarchy("en:moussaka", 0);
        }
        
        // Chercher "en:cheeses"
        if (taxonomy.containsKey("en:cheeses")) {
            System.out.println("\nExemple: en:cheeses");
            displayHierarchy("en:cheeses", 0);
        }
    }
    
    /**
     * Affiche la hiérarchie d'une catégorie
     */
    private static void displayHierarchy(String categoryId, int level) {
        TaxonomyElement element = taxonomy.get(categoryId);
        if (element == null) return;
        
        String indent = "  ".repeat(level);
        String name = element.nameEn != null ? element.nameEn : 
                     (element.nameFr != null ? element.nameFr : "");
        System.out.println(indent + categoryId + " (" + name + ")");
        
        if (!element.parents.isEmpty() && level < 5) {
            for (String parentId : element.parents) {
                System.out.println(indent + "  ↑ parent:");
                displayHierarchy(parentId, level + 2);
            }
        }
    }
    
    // Méthodes utilitaires pour Excel
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    private static void createCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    private static void addStatRow(Sheet sheet, int rowNum, String label, int value, 
                                   CellStyle headerStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, label, headerStyle);
        createCell(row, 1, value, dataStyle);
    }
}
