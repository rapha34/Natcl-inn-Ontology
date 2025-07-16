package ontologyManagement;

import java.io.File;
import java.io.FileOutputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import natclinn.util.NatclinnConf;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;

public class OFFCategoriesKeywordSearchExcel {

    public static void main(String[] args) throws Exception {

        // Chargement configuration
        new NatclinnConf();
        String sourceFilePath = NatclinnConf.folderForData + "/categories.json";
        String excelFilePath = NatclinnConf.folderForData + "/categories_keywords.xlsx";

        // Chargement du JSON
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = (ObjectNode) mapper.readTree(new File(sourceFilePath));

        Map<String, JsonNode> allCategories = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        Map<String, List<String>> parentToChildren = new HashMap<>();
        Map<String, List<String>> childToParents = new HashMap<>();

        // Construction des relations hiérarchiques
        for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            String id = it.next();
            JsonNode node = root.get(id);
            allCategories.put(id, node);

            String fr = node.path("name").path("fr").asText();
            String en = node.path("name").path("en").asText();
            String label = !fr.isEmpty() ? fr : (!en.isEmpty() ? en : id);
            labels.put(id, label);

            JsonNode parents = node.path("parents");
            if (parents.isArray()) {
                for (JsonNode p : parents) {
                    String parentId = p.asText();
                    parentToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
                    childToParents.computeIfAbsent(id, k -> new ArrayList<>()).add(parentId);
                }
            }
        }

        // Recherche des racines et construction des chemins complets
        Set<String> allIds = allCategories.keySet();
        Set<String> children = childToParents.keySet();
        List<String> rootIds = allIds.stream().filter(id -> !children.contains(id)).collect(Collectors.toList());

        List<List<String>> allPaths = new ArrayList<>();
        for (String rootId : rootIds) {
            dfs(rootId, new ArrayList<>(), parentToChildren, allPaths);
        }

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet mainSheet = workbook.createSheet("Recherche mots-clés");
        XSSFSheet dataSheet = workbook.createSheet("Catégories");

        Set<String> keywordSet = new TreeSet<>();
        Map<String, List<String>> keywordToPaths = new HashMap<>();

        // Liste des petits mots à exclure des mots-clés
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "pour", "aux", "des", "les", "avec", "dans", "sur", "du", "de", "au", "à", "en", "et", "ou",
            "cru", "la", "le", "l'", "un", "une", "ce", "ces", "cette", "cet", "qui", "que","AOC", "AOP", "IGP", "BIO", "certifié", "certifiée",
            "certifiés", "certifiées", "label", "labels", "officielle", "officielles", "officiel", "officiels", "officielle", "officielles"
        ));

        // Création des mots-clés à partir des derniers labels
        for (List<String> path : allPaths) {
            List<String> labelPath = path.stream().map(id -> labels.getOrDefault(id, id)).collect(Collectors.toList());
            String fullPath = String.join(",", labelPath);

            String lastLabel = labelPath.get(labelPath.size() - 1);
            for (String word : lastLabel.toLowerCase().split("[\\s\\-/,()]+")) {
                if (word.length() >= 3 && 
                    word.matches("[a-zA-Zéèàùêîôâçœæ]+") &&
                    !stopWords.contains(word)) {
                    keywordSet.add(word);
                    keywordToPaths.computeIfAbsent(word, k -> new ArrayList<>()).add(fullPath);
                }
            }
        }

        List<String> keywords = new ArrayList<>(keywordSet);
        // Colonne A : mots-clés uniques
        for (int i = 0; i < keywords.size(); i++) {
            Row row = dataSheet.getRow(i);
            if (row == null) row = dataSheet.createRow(i);
            row.createCell(0).setCellValue(keywords.get(i));
        }

        // Colonne B : mots-clés sanitisés
        for (int i = 0; i < keywords.size(); i++) {
            Row row = dataSheet.getRow(i);
            if (row == null) row = dataSheet.createRow(i); // En pratique inutile car déjà fait ci-dessus
            row.createCell(1).setCellValue(sanitizeForRangeName(keywords.get(i)));
        }

        // Plage nommée pour les mots-clés
        Name kwRange = workbook.createName();
        kwRange.setNameName("KeywordList");
        kwRange.setRefersToFormula("Catégories!$A$1:$A$" + keywords.size());

        // Plage nommée pour les mots-clés sanitizés
        Name kwRangeS = workbook.createName();
        kwRangeS.setNameName("KeywordSanitizedList");
        kwRangeS.setRefersToFormula("Catégories!$B$1:$B$" + keywords.size());

        // Écriture des chemins associés en colonne C à partir de ligne 1 
        int pathStartRow = 0; // Ligne de départ pour les chemins
        int pathColumn = 2; // Colonne C
        Set<String> alreadyCreatedNames = new HashSet<>();

        for (String kw : keywords) {
            List<String> paths = keywordToPaths.getOrDefault(kw, Collections.emptyList());
            int startRow = pathStartRow;
            for (String path : paths) {
                Row row = dataSheet.getRow(pathStartRow);
                if (row == null) {
                    row = dataSheet.createRow(pathStartRow);
                }
                row.createCell(pathColumn).setCellValue(path);
                pathStartRow++;
            }
            String rangeName = "Paths_" + sanitizeForRangeName(kw);
            if (!alreadyCreatedNames.contains(rangeName) && startRow < pathStartRow) {
                Name name = workbook.createName();
                name.setNameName(rangeName);
                name.setRefersToFormula("Catégories!$C$" + (startRow + 1) + ":$C$" + pathStartRow);
                alreadyCreatedNames.add(rangeName);
            }
        }

        // Feuille principale
        Row header = mainSheet.createRow(0);
        header.createCell(0).setCellValue("Mot-clé");
        header.createCell(1).setCellValue("Choix de chemin");

        DataValidationHelper dvHelper = mainSheet.getDataValidationHelper();

        // Liste déroulante sur colonne A
        CellRangeAddressList addrListA = new CellRangeAddressList(1, 10000, 0, 0);
        DataValidationConstraint constraintA = dvHelper.createFormulaListConstraint("KeywordList");
        DataValidation validationA = dvHelper.createValidation(constraintA, addrListA);
        mainSheet.addValidationData(validationA);

        // Liste dépendante via INDIRECT colonne C masquée
        for (int i = 1; i <= 10000; i++) {
            Row row = mainSheet.getRow(i);
            if (row == null) row = mainSheet.createRow(i);

            Cell hiddenCell = row.createCell(2);
            hiddenCell.setCellFormula(
                "IF(A" + (i + 1) + "=\"\",\"\",\"Paths_\"&VLOOKUP(A" + (i + 1) + ",Catégories!A:C,2,FALSE))"
            );

            CellRangeAddressList addrListB = new CellRangeAddressList(i, i, 1, 1);
            DataValidationConstraint constraintB = dvHelper.createFormulaListConstraint("INDIRECT(C" + (i + 1) + ")");
            DataValidation validationB = dvHelper.createValidation(constraintB, addrListB);
            mainSheet.addValidationData(validationB);
        }

        mainSheet.setColumnHidden(2, true); // Cache colonne C
        mainSheet.autoSizeColumn(1);

        try (FileOutputStream out = new FileOutputStream(excelFilePath)) {
            workbook.write(out);
        }

        workbook.close();
        System.out.println("Fichier généré : " + excelFilePath);
        System.out.println("Mots-clés générés : " + keywords.size());
    }

    private static void dfs(String currentId, List<String> currentPath,
                            Map<String, List<String>> parentToChildren,
                            List<List<String>> allPaths) {
        currentPath.add(currentId);
        List<String> children = parentToChildren.get(currentId);
        if (children == null || children.isEmpty()) {
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            for (String child : children) {
                dfs(child, new ArrayList<>(currentPath), parentToChildren, allPaths);
            }
        }
    }

    private static String sanitizeForRangeName(String keyword) {
        String sanitized = Normalizer.normalize(keyword, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("[^a-zA-Z0-9_]", "_");
        if (sanitized.isEmpty()) sanitized = "empty";
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "KW_" + sanitized;
        }
        return sanitized;
    }
}
