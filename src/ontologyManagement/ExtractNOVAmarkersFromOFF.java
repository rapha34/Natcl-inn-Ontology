package ontologyManagement;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.json.JSONObject;

import natclinn.util.NatclinnConf;

import org.json.JSONArray;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.checkerframework.checker.units.qual.N;

/**
 * Extraction des marqueurs de groupes NOVA depuis Open Food Facts
 * Utilise le fichier JSONL complet pour respecter les limites de l'API
 */
public class ExtractNOVAmarkersFromOFF {

    private static final String OFF_JSONL_URL = "https://static.openfoodfacts.org/data/openfoodfacts-products.jsonl.gz";
	private static final String USER_AGENT = "NatclinnOntology/1.0 (research project)";
    
	// Plus de filtre par pays: on traite tous les produits    // Structure pour stocker les marqueurs
    private static class NOVAMarker {
        int group;
        String type;
        String value;
        
        NOVAMarker(int group, String type, String value) {
            this.group = group;
            this.type = type;
            this.value = value;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NOVAMarker)) return false;
            NOVAMarker other = (NOVAMarker) obj;
            return this.group == other.group && 
                   this.type.equals(other.type) && 
                   this.value.equals(other.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(group, type, value);
        }
    }
    
    private static Set<NOVAMarker> markers = new LinkedHashSet<>();

    public static void main(String[] args) {
        new NatclinnConf();
        try {
            System.out.println("=== Extraction des marqueurs NOVA depuis Open Food Facts ===");
            System.out.println("Source: Export JSONL complet (respecte les conditions d'utilisation)\n");
            // Aucun paramètre de pays: traitement global de l'export
            
            // Télécharger et parser le fichier JSONL
            System.out.println("Téléchargement du fichier JSONL complet...");
            System.out.println("URL: " + OFF_JSONL_URL);
            System.out.println("Cela peut prendre plusieurs minutes (~3 GB compressé)...\n");
            
            extractMarkersFromJSONL();
            
            System.out.println("\n=== Total de marqueurs extraits: " + markers.size() + " ===\n");
            
            // Générer le fichier Excel
            String outputPath = NatclinnConf.folderForData + "/NOVA_markers_OFF.xlsx";
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
     * Télécharge et parse le fichier JSONL complet d'Open Food Facts
     */
    private static void extractMarkersFromJSONL() throws Exception {
        URL url = new URL(OFF_JSONL_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(60000);  // 60s pour la connexion
        conn.setReadTimeout(300000);     // 5 minutes pour la lecture
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Erreur HTTP: " + responseCode);
        }
        
        long fileSize = conn.getContentLengthLong();
        System.out.println("Taille du fichier: " + (fileSize / 1_000_000) + " MB");
        System.out.println("Début du traitement...\n");
        
        // Plus de filtre par pays ou catégories: on traite tous les produits
        
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
                    System.out.println(String.format("  Traité: %,d produits | Correspondants: %,d | Marqueurs: %,d | Temps: %ds",
                        totalProcessed, matchingProducts, markers.size(), elapsed));
                }
                
                try {
                    JSONObject product = new JSONObject(line);
                    
                    // Vérifier si le produit correspond à nos critères
                    if (isTargetProduct(product)) {
                        matchingProducts++;
                        
                        // Aucun comptage par catégorie
                        
                        // Extraire les marqueurs NOVA
                        extractMarkersFromProduct(product);
                    }
                    
                } catch (Exception e) {
                    // Ignorer les lignes malformées
                    if (totalProcessed % 10000 == 0) {
                        System.err.println("  ⚠ Ligne ignorée (malformée): " + totalProcessed);
                    }
                }
            }
        }
        
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println(String.format("Traitement terminé en %d secondes (%d min)", elapsed, elapsed / 60));
        System.out.println(String.format("  Total produits analysés: %,d", totalProcessed));
        System.out.println(String.format("  Produits correspondants: %,d", matchingProducts));
        
        // Pas de répartition par catégorie (filtre par pays uniquement)
    }
    
    /**
     * Vérifie si un produit doit être traité (pas de filtre pays)
     */
    private static boolean isTargetProduct(JSONObject product) {
        // Traiter tous les produits
        return true;
    }
    
    /**
     * Extrait les marqueurs NOVA d'un produit
     */
    private static void extractMarkersFromProduct(JSONObject product) {
        if (!product.has("nova_groups_markers")) {
            return;
        }
        
        try {
            JSONObject markersObj = product.getJSONObject("nova_groups_markers");
            
            // Parcourir les groupes NOVA (1, 2, 3, 4)
            for (String groupKey : markersObj.keySet()) {
                try {
                    int novaGroup = Integer.parseInt(groupKey);
                    if (novaGroup < 1 || novaGroup > 4) continue;
                    
                    JSONArray groupMarkers = markersObj.getJSONArray(groupKey);
                    
                    // Chaque marqueur est un tableau [type, value]
                    for (int i = 0; i < groupMarkers.length(); i++) {
                        try {
                            JSONArray markerPair = groupMarkers.getJSONArray(i);
                            if (markerPair.length() >= 2) {
                                String type = markerPair.getString(0);    // "categories", "ingredients", "additives"
                                String value = markerPair.getString(1);   // "en:meals", "en:butter", "en:e14xx"
                                
                                // Exclure les marqueurs non significatifs
                                markers.add(new NOVAMarker(novaGroup, type, value));
                            }
                        } catch (Exception e) {
                            // Ignorer les marqueurs mal formés
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les clés qui ne sont pas des nombres
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs de parsing pour un produit individuel
        }
    }
    
    // (Anciennes sections supprimées)
    
    private static void generateExcelFile(String outputPath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("NOVA Markers");
        
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
        String[] headers = {"Groupe NOVA", "Type de Marqueur", "Valeur"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Données - Trier par groupe puis type
        List<NOVAMarker> sortedMarkers = new ArrayList<>(markers);
        sortedMarkers.sort((a, b) -> {
            int groupCompare = Integer.compare(a.group, b.group);
            if (groupCompare != 0) return groupCompare;
            int typeCompare = a.type.compareTo(b.type);
            if (typeCompare != 0) return typeCompare;
            return a.value.compareTo(b.value);
        });
        
        int rowNum = 1;
        for (NOVAMarker marker : sortedMarkers) {
            Row row = sheet.createRow(rowNum++);
            
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(marker.group);
            cell0.setCellStyle(dataStyle);
            
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(marker.type);
            cell1.setCellStyle(dataStyle);
            
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(marker.value);
            cell2.setCellStyle(dataStyle);
        }
        
        // Auto-dimensionner les colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
        
        // Écrire le fichier
        try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
            workbook.write(fileOut);
        }
        
        workbook.close();
    }
    
    private static void displaySummary() {
        Map<Integer, Map<String, Integer>> summary = new TreeMap<>();
        
        for (NOVAMarker marker : markers) {
            summary.putIfAbsent(marker.group, new HashMap<>());
            Map<String, Integer> typeCounts = summary.get(marker.group);
            typeCounts.put(marker.type, typeCounts.getOrDefault(marker.type, 0) + 1);
        }
        
        System.out.println("\n=== Résumé des marqueurs par groupe ===");
        for (Map.Entry<Integer, Map<String, Integer>> entry : summary.entrySet()) {
            System.out.println("\nGroupe NOVA " + entry.getKey() + ":");
            for (Map.Entry<String, Integer> typeEntry : entry.getValue().entrySet()) {
                System.out.println("  - " + typeEntry.getKey() + ": " + typeEntry.getValue() + " marqueurs");
            }
        }
        
        // Afficher quelques exemples
        System.out.println("\n=== Exemples de marqueurs ===");
        int count = 0;
        for (NOVAMarker marker : markers) {
            if (count++ < 10) {
                System.out.println("Groupe " + marker.group + " | " + 
                                 marker.type + " | " + marker.value);
            } else {
                break;
            }
        }
        System.out.println("...");
    }
    
    // (supprimé)
    
    // Méthode equals et hashCode pour NOVAMarker (pour le Set)
    // (supprimé)
}
