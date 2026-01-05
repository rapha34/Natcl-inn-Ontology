package test.OFF;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.json.JSONObject;
import org.json.JSONArray;

import natclinn.util.NatclinnConf;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Analyse les produits du groupe 4 qui ont 'wheatflour' dans nova_groups_markers
 * Permet de comprendre pourquoi wheatflour est classé comme marqueur du groupe 4
 */
public class AnalyzeWheatFlourInGroup4 {

    private static final String OFF_JSONL_URL = "https://static.openfoodfacts.org/data/openfoodfacts-products.jsonl.gz";
    private static final String USER_AGENT = "NatclinnOntology/1.0 (research project)";
    
    private static class ProductWithWheatFlour {
        String code;
        String productName;
        int novaGroup;
        List<String> wheatflourMarkerTypes;  // Types où wheatflour apparaît (categories, ingredients, additives)
        List<String> allGroup4Markers;  // Tous les marqueurs du groupe 4
        
        ProductWithWheatFlour(String code, String productName, int novaGroup) {
            this.code = code;
            this.productName = productName;
            this.novaGroup = novaGroup;
            this.wheatflourMarkerTypes = new ArrayList<>();
            this.allGroup4Markers = new ArrayList<>();
        }
    }
    
    private static List<ProductWithWheatFlour> productsWithWheatFlour = new ArrayList<>();

    public static void main(String[] args) {
        new NatclinnConf();
        try {
            System.out.println("=== Analyse des produits groupe 4 avec 'wheatflour' dans nova_groups_markers ===\n");
            
            System.out.println("Téléchargement du fichier JSONL complet...");
            System.out.println("URL: " + OFF_JSONL_URL);
            System.out.println("Cela peut prendre plusieurs minutes (~3 GB compressé)...\n");
            
            analyzeWheatFlourInGroup4();
            
            System.out.println("\n=== Produits trouvés avec wheatflour en groupe 4: " + productsWithWheatFlour.size() + " ===\n");
            
            // Générer le fichier Excel
            String outputPath = NatclinnConf.folderForData + "/WheatFlour_in_Group4_Analysis.xlsx";
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
     * Télécharge et analyse le fichier JSONL pour trouver les produits groupe 4 avec wheatflour
     */
    private static void analyzeWheatFlourInGroup4() throws Exception {
        URL url = new URL(OFF_JSONL_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(300000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Erreur HTTP: " + responseCode);
        }
        
        long fileSize = conn.getContentLengthLong();
        System.out.println("Taille du fichier: " + (fileSize / 1_000_000) + " MB");
        System.out.println("Début du traitement...\n");
        
        int totalProcessed = 0;
        int matchingProducts = 0;
        long startTime = System.currentTimeMillis();
        
        try (InputStream in = conn.getInputStream();
             GZIPInputStream gzipIn = new GZIPInputStream(in);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIn, "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                totalProcessed++;
                
                // Afficher la progression tous les 100 000 produits
                if (totalProcessed % 100000 == 0) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    System.out.println(String.format("  Traité: %,d produits | Correspondants: %,d | Temps: %ds",
                        totalProcessed, matchingProducts, elapsed));
                }
                
                try {
                    JSONObject product = new JSONObject(line);
                    
                    // Chercher les produits groupe 4 avec wheatflour
                    if (product.has("nova_group") && product.getInt("nova_group") == 4 && 
                        product.has("nova_groups_markers")) {
                        
                        JSONObject markersObj = product.getJSONObject("nova_groups_markers");
                        
                        // Chercher le groupe 4
                        if (markersObj.has("4")) {
                            JSONArray group4Markers = markersObj.getJSONArray("4");
                            
                            // Chercher wheatflour dans ce groupe
                            boolean hasWheatFlour = false;
                            List<String> wheatflourTypes = new ArrayList<>();
                            List<String> allMarkersInGroup4 = new ArrayList<>();
                            
                            for (int i = 0; i < group4Markers.length(); i++) {
                                try {
                                    JSONArray markerPair = group4Markers.getJSONArray(i);
                                    if (markerPair.length() >= 2) {
                                        String type = markerPair.getString(0);
                                        String value = markerPair.getString(1);
                                        
                                        allMarkersInGroup4.add(type + ": " + value);
                                        
                                        if ("en:wheat-flour".equals(value)) {
                                            hasWheatFlour = true;
                                            wheatflourTypes.add(type);
                                        }
                                    }
                                } catch (Exception e) {
                                    // Ignorer les marqueurs mal formés
                                }
                            }
                            
                            if (hasWheatFlour) {
                                matchingProducts++;
                                String code = product.has("_id") ? product.getString("_id") : "unknown";
                                String productName = product.has("product_name") ? product.getString("product_name") : "unknown";
                                ProductWithWheatFlour p = new ProductWithWheatFlour(code, productName, 4);
                                p.wheatflourMarkerTypes = wheatflourTypes;
                                p.allGroup4Markers = allMarkersInGroup4;
                                productsWithWheatFlour.add(p);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    // Ignorer les lignes malformées
                }
            }
        }
        
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println(String.format("Traitement terminé en %d secondes (%d min)", elapsed, elapsed / 60));
        System.out.println(String.format("  Total produits analysés: %,d", totalProcessed));
        System.out.println(String.format("  Produits groupe 4 avec wheatflour: %,d", matchingProducts));
    }
    
    private static void generateExcelFile(String outputPath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("WheatFlour in Group 4");
        
        // Styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Code OFF", "Nom Produit", "WheatFlour Types", "Tous les marqueurs groupe 4"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Données
        int rowNum = 1;
        for (ProductWithWheatFlour p : productsWithWheatFlour) {
            Row row = sheet.createRow(rowNum++);
            
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(p.code);
            cell0.setCellStyle(dataStyle);
            
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(p.productName);
            cell1.setCellStyle(dataStyle);
            
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(String.join(", ", p.wheatflourMarkerTypes));
            cell2.setCellStyle(dataStyle);
            
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(String.join(" | ", p.allGroup4Markers));
            cell3.setCellStyle(dataStyle);
        }
        
        // Auto-dimensionner les colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1000, 20000));
        }
        
        // Écrire le fichier
        try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
            workbook.write(fileOut);
        }
        
        workbook.close();
    }
    
    private static void displaySummary() {
        System.out.println("\n=== Aperçu des produits avec wheatflour en groupe 4 ===");
        int count = 0;
        for (ProductWithWheatFlour p : productsWithWheatFlour) {
            if (count++ < 10) {
                System.out.println("\nCode: " + p.code);
                System.out.println("  Produit: " + p.productName);
                System.out.println("  WheatFlour types: " + String.join(", ", p.wheatflourMarkerTypes));
                System.out.println("  Tous marqueurs groupe 4: ");
                for (String marker : p.allGroup4Markers) {
                    System.out.println("    - " + marker);
                }
            } else {
                break;
            }
        }
        if (productsWithWheatFlour.size() > 10) {
            System.out.println("\n... et " + (productsWithWheatFlour.size() - 10) + " autres produits");
        }
        System.out.println("...");
    }
}
