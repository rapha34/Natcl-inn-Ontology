package ontologyManagement;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

/**
 * Programme Java permettant de télécharger et extraire la liste
 * des additifs alimentaires autorisés dans l'Union européenne
 * (Règlement CE n°1333/2008) et de la sauvegarder dans un fichier Excel.
 *
 * Dépendances :
 * - Apache POI (poi-ooxml)
 * - Jsoup (pour le parsing HTML)
 *
 * Auteur : ChatGPT - GPT-5
 */
public class EUFoodAdditivesExporter {

    /** URL de la page officielle (Annexe II du Règlement 1333/2008) */
    private static final String EU_LEGISLATION_URL =
            "https://www.legislation.gov.uk/eur/2008/1333/annex/II";

    /** Nom du fichier Excel de sortie */
    private static final String OUTPUT_FILE = "EU_FoodAdditives.xlsx";

    public static void main(String[] args) {
        try {
            System.out.println("Téléchargement de la liste des additifs depuis la source européenne...");
            String html = downloadPage(EU_LEGISLATION_URL);

            System.out.println("Extraction des additifs...");
            List<Additive> additives = extractAdditives(html);

            System.out.println("Création du fichier Excel...");
            writeExcel(additives, OUTPUT_FILE);

            System.out.println("✅ Fichier généré : " + OUTPUT_FILE);
            System.out.println("Nombre total d'additifs extraits : " + additives.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Télécharge le contenu HTML d'une page.
     */
    private static String downloadPage(String url) throws IOException {
        StringBuilder sb = new StringBuilder();
        URLConnection connection = new URL(url).openConnection();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Extraction simplifiée des additifs à partir du HTML.
     * NB : pour un usage scientifique, il est préférable d’utiliser Jsoup
     * pour parser la structure complète du tableau de l’annexe.
     */
    private static List<Additive> extractAdditives(String html) {
        List<Additive> list = new ArrayList<>();

        // Extraction basique : recherche de motifs "E " suivis d'un nombre et du nom
        // Exemple : "E 100 Curcumine"
        String regex = "E\\s?([0-9]{3,4}[a-zA-Z]?)\\s+([^<]*)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(html);

        while (m.find()) {
            String code = "E" + m.group(1).trim();
            String name = m.group(2).replaceAll("\\s+", " ").trim();
            if (!name.isEmpty()) {
                list.add(new Additive(code, name, "", ""));
            }
        }
        return list;
    }

    /**
     * Écrit la liste des additifs dans un fichier Excel.
     */
    private static void writeExcel(List<Additive> additives, String fileName) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Additifs autorisés UE");

            // En-têtes
            String[] headers = {"Code E", "Nom de l'additif", "Catégorie", "Conditions d'utilisation"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Contenu
            int rowIdx = 1;
            for (Additive a : additives) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.code);
                row.createCell(1).setCellValue(a.name);
                row.createCell(2).setCellValue(a.category);
                row.createCell(3).setCellValue(a.conditions);
            }

            // Ajustement des colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Sauvegarde
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }
        }
    }

    /** Petite classe interne pour représenter un additif */
    private static class Additive {
        String code;
        String name;
        String category;
        String conditions;

        Additive(String code, String name, String category, String conditions) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.conditions = conditions;
        }
    }
}
