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

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;

/**
 * Génère un fichier Excel prêt à l'emploi pour sélectionner une catégorie OFF feuille.
 * - Onglet "Feuilles" : liste des Noms FR des catégories feuilles (sans enfants)
 * - Plage nommée "LeafNames" pointant sur Feuilles!A2:A{n}
 * - Onglet "Produit" : cellule B2 avec validation de données (liste) basée sur =LeafNames
 */
public class ExportLeafDropdownWorkbook {

    static { new NatclinnConf(); }

    private static final String INPUT = NatclinnConf.folderForData + "/OFF_taxonomy_categories_fr.xlsx";
    private static final String OUTPUT = NatclinnConf.folderForData + "/produit_categorie_feuille.xlsx";

    public static void main(String[] args) {
        try {
            System.out.println("Lecture source : " + INPUT);
            System.out.println("Création du classeur de sélection…\n");
            buildWorkbook();
            System.out.println("\nFichier prêt : " + OUTPUT);
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void buildWorkbook() throws IOException {
        File in = new File(INPUT);
        if (!in.exists()) throw new IOException("Fichier introuvable : " + INPUT);

        // Collecter la liste unique des Noms FR et ID OFF pour les feuilles (enfants vides)
        // leafMap conserve l'ordre, parentMap pour remonter aux racines
        LinkedHashMap<String, String> leafMap = new LinkedHashMap<>();
        Map<String, String> idToName = new HashMap<>();
        Map<String, List<String>> parentMap = new HashMap<>();

        try (Workbook src = new XSSFWorkbook(new FileInputStream(in))) {
            Sheet s = src.getSheetAt(0);
            if (s == null) throw new IOException("Aucune feuille trouvée dans la source");

            Row header = s.getRow(0);
            int colId = -1, colFr = -1, colEnfants = -1, colParents = -1;
            for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String name = cell.getStringCellValue();
                if (name == null) continue;
                String norm = name.toLowerCase().replace(" ", "");
                if ("idoff".equals(norm)) colId = c;
                else if ("nomfr".equals(norm)) colFr = c;
                else if ("enfants".equals(norm)) colEnfants = c;
                else if ("parents".equals(norm)) colParents = c;
            }
            if (colId < 0 || colFr < 0 || colEnfants < 0) throw new IOException("Colonnes manquantes (idOff/nomFr/enfants)");
            if (colParents < 0) throw new IOException("Colonne manquante (parents)");

            int last = s.getLastRowNum();
            for (int r = 1; r <= last; r++) {
                Row row = s.getRow(r);
                if (row == null) continue;
                String idOff = getString(row.getCell(colId));
                String nomFr = getString(row.getCell(colFr));
                String enfants = getString(row.getCell(colEnfants));
                String parents = getString(row.getCell(colParents));
                // Filtre: exclure catégories isolées (Nb Parents = 0 ET Nb Enfants = 0)
                boolean hasParents = parents != null && !parents.isBlank();
                boolean hasChildren = enfants != null && !enfants.isBlank();
                if (!hasParents && !hasChildren) {
                    continue; // on ignore complètement les noeuds isolés
                }

                if (idOff != null && !idOff.isBlank()) {
                    idToName.put(idOff.trim(), nomFr == null ? "" : nomFr.trim());
                    parentMap.put(idOff.trim(), parseList(parents));
                }
                boolean isLeaf = !hasChildren;
                if (isLeaf && nomFr != null && !nomFr.isBlank() && idOff != null && !idOff.isBlank()) {
                    leafMap.put(nomFr.trim(), idOff.trim());
                }
            }
        }

        // Trouver les racines (parents vides)
        Set<String> roots = new HashSet<>();
        for (Map.Entry<String, List<String>> e : parentMap.entrySet()) {
            if (e.getValue().isEmpty()) roots.add(e.getKey());
        }

        // Pour chaque feuille, déterminer une racine (première racine trouvée dans le graphe)
        Map<String, String> leafToRootId = new HashMap<>();
        Map<String, String> leafToRootName = new HashMap<>();
        Map<String, Integer> rootCounts = new HashMap<>();
        for (String leafId : leafMap.values()) {
            String rootId = findRoot(leafId, parentMap, roots);
            String rootName = rootId == null ? "" : idToName.getOrDefault(rootId, rootId);
            leafToRootId.put(leafId, rootId == null ? "" : rootId);
            leafToRootName.put(leafId, rootName);
            if (rootId != null && !rootId.isBlank()) {
                rootCounts.put(rootId, rootCounts.getOrDefault(rootId, 0) + 1);
            }
        }

        // Filtrer racines sans feuilles et trier par nom croissant
        List<String> sortedRoots = new ArrayList<>();
        for (String r : roots) {
            if (rootCounts.getOrDefault(r, 0) > 0) sortedRoots.add(r);
        }
        sortedRoots.sort((a, b) -> idToName.getOrDefault(a, a).compareToIgnoreCase(idToName.getOrDefault(b, b)));

        // Construire le classeur cible
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            CellStyle data = dataStyle(wb);

            // Onglet Feuilles avec Nom FR, ID OFF, Racine ID, Racine Nom
            Sheet feuilles = wb.createSheet("Feuilles");
            Row h = feuilles.createRow(0);
            Cell h0 = h.createCell(0); h0.setCellValue("Nom FR"); h0.setCellStyle(header);
            Cell idOffHeader = h.createCell(1); idOffHeader.setCellValue("ID OFF"); idOffHeader.setCellStyle(header);
            Cell racineIdHeader = h.createCell(2); racineIdHeader.setCellValue("Racine ID"); racineIdHeader.setCellStyle(header);
            Cell racineNomHeader = h.createCell(3); racineNomHeader.setCellValue("Racine Nom"); racineNomHeader.setCellStyle(header);

            int rowIdx = 1;
            for (Map.Entry<String, String> entry : leafMap.entrySet()) {
                Row r = feuilles.createRow(rowIdx++);
                String leafName = entry.getKey();
                String leafId = entry.getValue();
                Cell c0 = r.createCell(0); c0.setCellValue(leafName); c0.setCellStyle(data);
                Cell c1 = r.createCell(1); c1.setCellValue(leafId); c1.setCellStyle(data);
                Cell c2 = r.createCell(2); c2.setCellValue(leafToRootId.getOrDefault(leafId, "")); c2.setCellStyle(data);
                Cell c3 = r.createCell(3); c3.setCellValue(leafToRootName.getOrDefault(leafId, "")); c3.setCellStyle(data);
            }
            feuilles.autoSizeColumn(0);
            feuilles.autoSizeColumn(1);
            feuilles.autoSizeColumn(2);
            feuilles.autoSizeColumn(3);

            // Onglet Racines (top categories)
            Sheet racines = wb.createSheet("Racines");
            Row rh = racines.createRow(0);
            Cell rh0 = rh.createCell(0); rh0.setCellValue("Racine Nom"); rh0.setCellStyle(header);
            Cell rh1 = rh.createCell(1); rh1.setCellValue("Racine ID"); rh1.setCellStyle(header);
            int rRow = 1;
            for (String rootId : sortedRoots) {
                Row r = racines.createRow(rRow++);
                Cell rc0 = r.createCell(0); rc0.setCellValue(idToName.getOrDefault(rootId, rootId)); rc0.setCellStyle(data);
                Cell rc1 = r.createCell(1); rc1.setCellValue(rootId); rc1.setCellStyle(data);
            }
            racines.autoSizeColumn(0);
            racines.autoSizeColumn(1);

            // Plage nommée LeafNames (toutes les feuilles, non filtrée)
            int lastRow = Math.max(1, rowIdx - 1);
            if (lastRow >= 2) {
                wb.createName().setNameName("LeafNames");
                wb.getName("LeafNames").setRefersToFormula("Feuilles!$A$2:$A$" + lastRow);
            } else {
                // Au cas où aucune feuille détectée, référencer une cellule vide sûre
                wb.createName().setNameName("LeafNames");
                wb.getName("LeafNames").setRefersToFormula("Feuilles!$A$2:$A$2");
            }

            // Plage nommée TopRoots
            if (!sortedRoots.isEmpty()) {
                int lastRootRow = Math.max(2, sortedRoots.size() + 1);
                wb.createName().setNameName("TopRoots");
                wb.getName("TopRoots").setRefersToFormula("Racines!$A$2:$A$" + lastRootRow);
            }

            // Onglet Liste: zone Mot clef + résultats filtrés
            Sheet liste = wb.createSheet("Liste");
            Row l0 = liste.createRow(0);
            Cell la1 = l0.createCell(0); la1.setCellValue("Résultats"); la1.setCellStyle(header);

            // Générer une formule matricielle pour chaque ligne Liste
            if (lastRow >= 2) {
                String lastRef = Integer.toString(lastRow);
                String rangeNames = "Feuilles!$A$2:$A$" + lastRef;
                String rangeRoots = "Feuilles!$D$2:$D$" + lastRef;
                
                // Génération matrice 400 lignes × 300 colonnes
                // Chaque ligne Liste correspond à une ligne Produit et contient 300 résultats filtrés
                for (int r = 2; r <= 401; r++) {
                    Row row = liste.createRow(r - 1);
                    
                    // Références dynamiques : Produit G (mot clef) et F (racine) pour la ligne correspondante
                    String produitA = "Produit!$G$" + r;
                    String produitB = "Produit!$F$" + r;
                    String mask = "(IF(" + produitB + "=\"\",1,IF(" + rangeRoots + "=" + produitB + ",1,0))*IF(" + produitA + "=\"\",1,ISNUMBER(SEARCH(" + produitA + "," + rangeNames + "))))";
                    
                    // Créer la formule matricielle pour toute la ligne (colonnes A à KN = 300 colonnes)
                    // Une seule formule matricielle couvrant A:KN de cette ligne
                    String formulaBase = "IFERROR(INDEX(" + rangeNames + ",SMALL(IF(" + mask + ">0,ROW(" + rangeNames + ")-ROW(Feuilles!$A$2)+1),COLUMN())),\"\")";
                    CellRangeAddress arrRange = new CellRangeAddress(r - 1, r - 1, 0, 299); // Colonnes A à KN (0 à 299)
                    ((XSSFSheet) liste).setArrayFormula(formulaBase, arrRange);
                }
            }

            liste.autoSizeColumn(0);
            liste.setColumnWidth(0, 10000);

            // Créer des plages nommées dynamiques pour chaque ligne Liste (exclut les cellules vides)
            for (int r = 2; r <= 401; r++) {
                String rangeName = "ListeLigne" + r;
                // Formule OFFSET qui compte les cellules non vides sur la ligne r, colonnes A à KN
                String formula = "OFFSET(Liste!$A$" + r + ",0,0,1,COUNTIF(Liste!$A$" + r + ":$KN$" + r + ",\"?*\"))";
                org.apache.poi.ss.usermodel.Name nm = wb.createName();
                nm.setNameName(rangeName);
                nm.setRefersToFormula(formula);
            }

            // Onglet Produit avec bandeau en F:J
            Sheet produit = wb.createSheet("Produit");
            Row p0 = produit.createRow(0); // ligne 1
            // Bandeau demandé: CategorieRacine, MotClefCategorie, CategorieFeuille, CodeCategorieRacine, CodeCategorieFeuille
            Cell f1 = p0.createCell(5); f1.setCellValue("CategorieRacine"); f1.setCellStyle(header);
            Cell g1 = p0.createCell(6); g1.setCellValue("MotClefCategorie"); g1.setCellStyle(header);
            Cell h1 = p0.createCell(7); h1.setCellValue("CategorieFeuille"); h1.setCellStyle(header);
            Cell i1 = p0.createCell(8); i1.setCellValue("CodeCategorieRacine"); i1.setCellStyle(header);
            Cell j1 = p0.createCell(9); j1.setCellValue("CodeCategorieFeuille"); j1.setCellStyle(header);
            
            // Créer 400 lignes de saisie (lignes 2 à 401) sur F:J
            for (int rowNum = 2; rowNum <= 401; rowNum++) {
                Row pRow = produit.createRow(rowNum - 1); // 0-indexed
                Cell cellF = pRow.createCell(5); cellF.setCellValue(""); cellF.setCellStyle(data); // CategorieRacine (dropdown)
                Cell cellG = pRow.createCell(6); cellG.setCellValue(""); cellG.setCellStyle(data); // MotClefCategorie (texte)
                Cell cellH = pRow.createCell(7); cellH.setCellValue(""); cellH.setCellStyle(data); // CategorieFeuille (dropdown)
                Cell cellI = pRow.createCell(8); // CodeCategorieRacine (lookup)
                cellI.setCellFormula("IFERROR(INDEX(Racines!$B:$B,MATCH(F" + rowNum + ",Racines!$A:$A,0)),\"\")");
                cellI.setCellStyle(data);
                Cell cellJ = pRow.createCell(9); // CodeCategorieFeuille (lookup)
                cellJ.setCellFormula("IFERROR(INDEX(Feuilles!$B:$B,MATCH(H" + rowNum + ",Feuilles!$A:$A,0)),\"\")");
                cellJ.setCellStyle(data);
            }

            // Validation racine (TopRoots) en F2:F401
            if (!sortedRoots.isEmpty()) {
                DataValidationHelper dvRootHelper = produit.getDataValidationHelper();
                DataValidationConstraint dvRootConstraint = dvRootHelper.createFormulaListConstraint("TopRoots");
                CellRangeAddressList rootAddr = new CellRangeAddressList(1, 400, 5, 5); // F2:F401
                DataValidation dvRoot = dvRootHelper.createValidation(dvRootConstraint, rootAddr);
                dvRoot.setEmptyCellAllowed(true);
                produit.addValidationData(dvRoot);
            }

            // Validation catégorie: chaque ligne pointe sur la plage nommée dynamique correspondante
            // Produit H2 → ListeLigne2, H3 → ListeLigne3, etc.
            DataValidationHelper dvHelper = produit.getDataValidationHelper();
            for (int r = 2; r <= 401; r++) {
                String rangeName = "ListeLigne" + r;
                DataValidationConstraint dvConstraint = dvHelper.createFormulaListConstraint(rangeName);
                CellRangeAddressList addressList = new CellRangeAddressList(r - 1, r - 1, 7, 7); // H uniquement
                DataValidation dv = dvHelper.createValidation(dvConstraint, addressList);
                dv.setEmptyCellAllowed(true);
                produit.addValidationData(dv);
            }

            // Ajuster largeurs pour F:J
            produit.autoSizeColumn(5);
            produit.autoSizeColumn(6);
            produit.setColumnWidth(7, 10000);
            produit.autoSizeColumn(8);
            produit.autoSizeColumn(9);

            // Écrire
            try (FileOutputStream fos = new FileOutputStream(OUTPUT)) {
                wb.write(fos);
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

    // Parse une liste de parents séparés par ; ou , ; renvoie liste vide si null/blank
    private static List<String> parseList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split("[;,]")) {
            String p = part.trim();
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    // Trouve une racine atteignable depuis leafId; retourne null si aucune
    private static String findRoot(String leafId, Map<String, List<String>> parentMap, Set<String> roots) {
        Set<String> visited = new HashSet<>();
        return dfsRoot(leafId, parentMap, roots, visited);
    }

    private static String dfsRoot(String id, Map<String, List<String>> parentMap, Set<String> roots, Set<String> visited) {
        if (id == null) return null;
        if (roots.contains(id)) return id;
        if (!visited.add(id)) return null;
        List<String> parents = parentMap.getOrDefault(id, List.of());
        if (parents.isEmpty()) return null;
        for (String p : parents) {
            String r = dfsRoot(p, parentMap, roots, visited);
            if (r != null) return r;
        }
        return null;
    }

    private static CellStyle headerStyle(Workbook wb) {
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

    private static CellStyle dataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
