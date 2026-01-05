package test.OFF;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import natclinn.util.NatclinnConf;

/**
 * Calcule les cooccurrences ingredient_tag x categories_tag depuis le JSONL OFF.
 * Sortie: Excel avec ingredient, category, count (limité à 1000 paires top)
 */
public class IngredientCategoryCooccurrence {
    private static final int LIMIT_ROWS = 1000000;
    private static final String OFF_JSONL_URL = "https://static.openfoodfacts.org/data/openfoodfacts-products.jsonl.gz";
    private static final String USER_AGENT = "NatclinnOntology/1.0 (research project)";

    private static class PairCount {
        String ingredient;
        String category;
        int count;          // nombre de cooccurrences produit
        PairCount(String i, String c, int cnt) { this.ingredient = i; this.category = c; this.count = cnt; }
    }

    public static void main(String[] args) {
        new NatclinnConf();
        try {
            // Choix de la source: local si dispo, sinon téléchargement streaming.
            File local = new File(NatclinnConf.folderForData + "/openfoodfacts-products.jsonl.gz");
            InputStream baseStream;
            if (local.exists()) {
                System.out.println("Lecture du fichier local: " + local.getAbsolutePath());
                baseStream = new FileInputStream(local);
            } else {
                System.out.println("Téléchargement streaming depuis OFF (pas d'écriture disque)...");
                baseStream = openRemoteStream();
            }

            Map<String, Map<String, Integer>> counts = computeCooccurrences(baseStream);

            // Tri et export Excel (limité à 1000000 paires top)
            String outPath = NatclinnConf.folderForData + "/Ingredient_Category_Cooccurrences.xlsx";
            exportExcel(counts, outPath);
            System.out.println("Fichier Excel généré (max " + LIMIT_ROWS + " lignes): " + outPath);

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static InputStream openRemoteStream() throws Exception {
        URL url = new URL(OFF_JSONL_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(300000);
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IllegalStateException("HTTP " + code);
        }
        return new BufferedInputStream(conn.getInputStream());
    }

    private static Map<String, Map<String, Integer>> computeCooccurrences(InputStream baseStream) throws Exception {
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        long total = 0;
        long matched = 0;
        long start = System.currentTimeMillis();

        try (GZIPInputStream gzip = new GZIPInputStream(baseStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                if (total % 100_000 == 0) {
                    long elapsed = (System.currentTimeMillis() - start) / 1000;
                    System.out.printf("Traité: %,d produits | Paires cumulées: %,d | Temps: %ds%n", total, matched, elapsed);
                }
                try {
                    JSONObject product = new JSONObject(line);

                    JSONArray ingTags = product.optJSONArray("ingredients_tags");
                    JSONArray catTags = product.optJSONArray("categories_tags");
                    if (ingTags == null || catTags == null) continue;

                    // Filtrer en:/fr: et dédoublonner par produit
                    Set<String> ingredients = new HashSet<>();
                    for (int i = 0; i < ingTags.length(); i++) {
                        String tag = ingTags.optString(i, "");
                        if (tag.startsWith("en:") || tag.startsWith("fr:")) {
                            ingredients.add(tag);
                        }
                    }
                    if (ingredients.isEmpty()) continue;

                    Set<String> categories = new HashSet<>();
                    for (int i = 0; i < catTags.length(); i++) {
                        String tag = catTags.optString(i, "");
                        if (tag.startsWith("en:") || tag.startsWith("fr:")) {
                            categories.add(tag);
                        }
                    }
                    if (categories.isEmpty()) continue;

                    // Cross-product pour ce produit (une fois par paire unique)
                    for (String ing : ingredients) {
                        Map<String, Integer> inner = counts.computeIfAbsent(ing, k -> new HashMap<>());
                        for (String cat : categories) {
                            inner.merge(cat, 1, Integer::sum);
                            matched++;
                        }
                    }
                } catch (Exception ignore) {
                    // ligne malformée: on ignore
                }
            }
        }

        System.out.printf("Terminé. Produits lus: %,d | Paires comptées: %,d%n", total, matched);
        return counts;
    }

    private static void exportExcel(Map<String, Map<String, Integer>> counts, String outPath) throws Exception {
        // Aplatir et trier par count décroissant
        List<PairCount> rows = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> eIng : counts.entrySet()) {
            String ing = eIng.getKey();
            for (Map.Entry<String, Integer> eCat : eIng.getValue().entrySet()) {
                rows.add(new PairCount(ing, eCat.getKey(), eCat.getValue()));
            }
        }
        rows.sort(Comparator.comparingInt((PairCount pc) -> pc.count).reversed());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ingredient x Category");

            CellStyle header = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);

            CellStyle data = wb.createCellStyle();
            data.setBorderBottom(BorderStyle.THIN);
            data.setBorderTop(BorderStyle.THIN);
            data.setBorderLeft(BorderStyle.THIN);
            data.setBorderRight(BorderStyle.THIN);

            // Header
            Row h = sheet.createRow(0);
            String[] cols = {"ingredient_tag", "category_tag", "count"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(header);
            }

            // Data - limité à LIMIT_ROWS
            int r = 1;
            for (int i = 0; i < rows.size() && r <= LIMIT_ROWS; i++) {
                PairCount pc = rows.get(i);
                Row row = sheet.createRow(r);
                Cell c0 = row.createCell(0); c0.setCellValue(pc.ingredient); c0.setCellStyle(data);
                Cell c1 = row.createCell(1); c1.setCellValue(pc.category);   c1.setCellStyle(data);
                Cell c2 = row.createCell(2); c2.setCellValue(pc.count);      c2.setCellStyle(data);
                r++;
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1000, 20000));
            }

            try (FileOutputStream out = new FileOutputStream(outPath)) {
                wb.write(out);
            }
        }
    }
}
