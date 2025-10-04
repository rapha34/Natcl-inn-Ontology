package ontologyManagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;
import natclinn.util.NatclinnUtil.Ingredient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.StreamSupport;

public class OpenFoodFactsToExcelExporterCopy {

    public static void main(String[] args) throws Exception {
        new NatclinnConf();
        // extractOFFToExcel("categories_tags_en", "madeleines");
        // extractOFFToExcel("code", "3835100000004");
        // 3270160865826 Saumon, duo de guinoa et orge, purée aux légumes verts, sauce vierge à la framboise
        // 3564700423196 Moussaka – Marque Repère – 300 g
        // 3250392814908 Moussaka – Monique ranou – 300 g
        // 3302740044786 La Moussaka Boeuf & Aubergines avec une touche de menthe douce – Fleury Michon – 300 g
        // extractOFFToExcel("code", "3270160865826");
        extractOFFToExcel("code", "3302740044786");
    }

    public static void extractOFFToExcel(String searchProperty, String searchPropertyString) throws Exception {

        String fieldParams = "&fields=code,product_name,brands,categories,nutriscore_grade,nova_groups,nova_groups_markers,ingredients_text_fr,ingredients,packagings,origins,labels";
        int pageSize = 100;
        String baseUrl = "https://world.openfoodfacts.net/api/v2/search?" + searchProperty + "=" + searchPropertyString + "&json=1&page_size=" + pageSize;

        String initialUrl = baseUrl + "&page=1&fields=product_name";
        String initialJson = getHttpContent(initialUrl);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(initialJson);
        int count = root.path("count").asInt();
        int totalPages = (count + pageSize - 1) / pageSize;

        // Feuilles Excel
        Workbook workbook = new XSSFWorkbook();
        Sheet productsSheet = workbook.createSheet("Products");
        Sheet ingredientsSheet = workbook.createSheet("Ingredients");
        Sheet compositionsSheet = workbook.createSheet("Compositions");
        Sheet packagingSheet = workbook.createSheet("Packaging");
        Sheet controlledOriginLabelSheet = workbook.createSheet("ControlledOriginLabel");
        Sheet cleanLabelSheet = workbook.createSheet("CleanLabel");
        Sheet manufacturingProcessSheet = workbook.createSheet("ManufacturingProcess");
        Sheet nutriScoreSheet = workbook.createSheet("NutriScore");
        Sheet novaSheet = workbook.createSheet("Nova");
        

        createHeaders(productsSheet, "IDProduit", "NomProduit", "EANCode", "TypeProduit", "Marque", "MotClefCategorie", "Categorie");
        createHeaders(ingredientsSheet, "IDIngredient", "NomIngredient", "Ciqual_food_code", "Ciqual_proxi_food_code");
        createHeaders(compositionsSheet, "IDProduit", "TypeComposant", "IDComposant", "Quantité", "Unité", "Pourcentage", "Rang");
        createHeaders(packagingSheet, "IDProduit", "Forme","Matériau");
        createHeaders(controlledOriginLabelSheet, "IDProduit", "ControlledOriginLabel");
        createHeaders(cleanLabelSheet, "IDProduit", "CleanLabel");
        createHeaders(manufacturingProcessSheet, "IDProduit", "ManufacturingProcess");
        createHeaders(nutriScoreSheet, "IDProduit", "NutriScore");
        createHeaders(novaSheet, "IDProduit", "GroupeNova", "Groupe1", "Groupe2", "Groupe3", "Groupe4");
        
        // Set to track unique ingredients and their IDs    

        Set<String> ingredientSet = new HashSet<>();
        Map<String, String> ingredientIdMap = new HashMap<>();
        int prodRowIdx = 1;
        int ingRowIdx = 1;
        int packRowIdx = 1;
        int ctrlRowIdx = 1;
        int cleanRowIdx = 1;
        int manuRowIdx = 1;
        int nutriRowIdx = 1;
        int novaRowIdx = 1;
        int compRowIdx = 1;
        int countError = 0;

        // Création d’un DataFormat et d’un style pour nombre sans décimale
        DataFormat format = workbook.createDataFormat();
        CellStyle integerStyle = workbook.createCellStyle();
        integerStyle.setDataFormat(format.getFormat("0")); // Format "0" = entier sans décimale

        // Pour toutes les pages de résultats
        for (int page = 1; page <= totalPages; page++) {
            String pageUrl = baseUrl + "&page=" + page + fieldParams;
            String json = getHttpContent(pageUrl);
            root = mapper.readTree(json);
            JsonNode products = root.path("products");
            // Pour tous les produits de la page
            for (JsonNode p : products) {
                String eanCode = p.path("code").asText("");
                String nameFr = p.path("product_name_fr").asText("");
                String name = p.path("product_name").asText("");
                String brand = p.path("brands").asText("");
                String categories = p.path("categories").asText("");
                Boolean compositeProduct = false;

                if (!nameFr.isEmpty()) name = nameFr; // Priorité au nom en français
                if (name.isEmpty()) continue;
                // Test à supprimer 
                // if (!name.equals("Madeleines coquilles aux oeufs")) continue;
                //if (!eanCode.equals("3178530402353")) continue;

                String productId = "" ;
                if (eanCode == null || eanCode.isEmpty()) {
						long counter = NatclinnUtil.getNextProductCounterValue();
						String paddedCounter = String.format("%013d", counter); // format 13 chiffres
						productId = "P-" + paddedCounter;
					} else {
						productId = "P-" + eanCode;
					}
                

                Row prodRow = productsSheet.createRow(prodRowIdx++);
                prodRow.createCell(0).setCellValue(productId);
                prodRow.createCell(1).setCellValue(name);
                // Si le code EAN est vide, on laisse la cellule vide
                if (eanCode != null && eanCode != "") {
                    // Sinon on le convertit en entier pour l'export Excel
                    Cell cell = prodRow.createCell(2);
                    cell.setCellValue(Long.parseLong(eanCode)); // Valeur numérique
                    cell.setCellStyle(integerStyle); // Formatage sans décimale
                }
                Cell cellTypeProduct = prodRow.createCell(3);
                cellTypeProduct.setCellValue("SimpleProduct");
                prodRow.createCell(4).setCellValue(brand);
                prodRow.createCell(5).setCellValue("");
                prodRow.createCell(6).setCellValue(categories);

                // Enregistrement du packaging
                JsonNode packagingNode = p.path("packagings");
                for (JsonNode packItem : packagingNode) {
                    String shape = packItem.path("shape").asText("");
                    String material = packItem.path("material").asText("");
                    if (!shape.isEmpty()) {
                        Row packRow = packagingSheet.createRow(packRowIdx++);
                        packRow.createCell(0).setCellValue(productId);
                        packRow.createCell(1).setCellValue(shape);
                        packRow.createCell(2).setCellValue(material);
                    }
                }
                // Enregistrement de l'étiquette d'origine contrôlée
                String controlledOriginLabel = p.path("origins").asText("");
                if (!controlledOriginLabel.isEmpty()) {
                    String[] origins = controlledOriginLabel.split("\\s*,\\s*"); // split sur la virgule avec espaces optionnels
                    for (String origin : origins) {
                        if (!origin.isEmpty()) {
                            Row ctrlRow = controlledOriginLabelSheet.createRow(ctrlRowIdx++);
                            ctrlRow.createCell(0).setCellValue(productId);
                            ctrlRow.createCell(1).setCellValue(origin);
                        }
                    }
                }
                // Enregistrement de l'étiquette Clean Label
                String cleanLabel = p.path("labels").asText("");
                if (!cleanLabel.isEmpty()) {
                    String[] labels = cleanLabel.split("\\s*,\\s*"); // split sur la virgule avec espaces optionnels
                    for (String label : labels) {
                        if (!label.isEmpty()) {
                        Row cleanRow = cleanLabelSheet.createRow(cleanRowIdx++);
                        cleanRow.createCell(0).setCellValue(productId);
                        cleanRow.createCell(1).setCellValue(label);
                        }
                    }
                }
                // Enregistrement du processus de fabrication
                String manufacturingProcess = p.path("manufacturing_process").asText("");
                if (!manufacturingProcess.isEmpty()) {
                    Row manuRow = manufacturingProcessSheet.createRow(manuRowIdx++);
                    manuRow.createCell(0).setCellValue(productId);
                    manuRow.createCell(1).setCellValue(manufacturingProcess);
                }
                // Enregistrement du Nutri-Score
                String nutriScore = p.path("nutriscore_grade").asText("");
                if (!nutriScore.isEmpty()) {
                    Row nutriRow = nutriScoreSheet.createRow(nutriRowIdx++);
                    nutriRow.createCell(0).setCellValue(productId);
                    nutriRow.createCell(1).setCellValue(nutriScore.toUpperCase());
                }

                // Enregistrement de la classification Nova
                String novaScore = p.path("nova_groups").asText("");
                // Récupération du node markers
                JsonNode markersNode = p.path("nova_groups_markers");
                if (!novaScore.isEmpty()) {
                    Row novaRow = novaSheet.createRow(novaRowIdx++);
                    novaRow.createCell(0).setCellValue(productId);
                    novaRow.createCell(1).setCellValue(novaScore);
                    // Parcours des groupes Nova 1 à 4
                    for (int g = 1; g <= 4; g++) {
                        JsonNode groupNode = markersNode.path(String.valueOf(g));

                        if (groupNode.isArray()) {
                            List<String> markers = new ArrayList<>();

                            for (JsonNode marker : groupNode) {
                                // Certains markers sont sous forme ["ingredients", "en:butter"]
                                if (marker.isArray() && marker.size() >= 2) {
                                    String type = marker.get(0).asText("");
                                    String value = marker.get(1).asText("");
                                    markers.add(type + ":" + value);
                                } else {
                                    // Sinon valeur brute
                                    markers.add(marker.asText(""));
                                }
                            }

                            // On concatène les markers séparés par une virgule
                            String joined = String.join(", ", markers);

                            // On écrit dans la colonne correspondante (1 = Groupe1, 2 = Groupe2…)
                            novaRow.createCell(g + 1).setCellValue(joined);
                        }
                    }
                }

                // Mapping des ingrédients structurés du champ "ingredients"
                JsonNode structuredIngredients = p.path("ingredients");
                Map<String, JsonNode> structuredMap = new HashMap<>();
                for (JsonNode ingNode : structuredIngredients) {
                    String label = ingNode.path("text").asText("");
                    label=typographicalCorrection(label);
                    if (!label.isEmpty()) {
                        structuredMap.put(label, ingNode);
                    }
                }

                // Composition via analyse texte
                String ingredientsText = p.path("ingredients_text_fr").asText("");
                List<Ingredient> parsed = NatclinnUtil.parse(ingredientsText);
                int rank = 1;
                for (Ingredient ing : parsed) {
                    if (ing.getName().isEmpty()) continue;
                    String labelIngredient = typographicalCorrection(ing.getName());
                System.out.println("labelIngredient : '" +  labelIngredient + "'");
                //hexViewer(labelIngredient);
                    String typeIngredient = ing.getType();
                    List<Ingredient> subIngredient = ing.getSubIngredients();
                    if (typeIngredient == "Ingredient" ) {   
                        // Si un ingrédient n’a pas encore d’identifiant dans la map, on lui en génère un nouveau et on l’y associe, sinon on récupère l’identifiant déjà existant.
                        String ingId = ingredientIdMap.computeIfAbsent(labelIngredient, l -> "I-" + String.format("%013d", NatclinnUtil.getNextIngredientCounterValue()));
                        
                        String ingredientLabel = "";
                        String ciqualCode = "";
                        String ciqualProxy = "";
                        String hasSubs = "";
                        
                        // System.out.println("labelIngredient : " +  labelIngredient);
                        // System.out.println("StructuredMap : " +  structuredMap);

                        JsonNode structured = structuredMap.get(labelIngredient);
                        if (structured != null) {
                            ingredientLabel = labelIngredient;    
                            ciqualCode = structured.path("ciqual_food_code").asText("");
                            ciqualProxy = structured.path("ciqual_proxy_food_code").asText("");
                            hasSubs = structured.path("has_sub_ingredients").asText("");
                        } else {
                            // === Tentative de correspondance approximative ===
                            Optional<JsonNode> approx = StreamSupport.stream(structuredIngredients.spliterator(), false)
                                .filter(node -> {
                                    String label = node.path("text").asText("");
                                    label=typographicalCorrection(label);
                                    // System.out.println("label : '" + label + "'");
                                    // System.out.println("labelIngredient : '" + labelIngredient + "'");
                                    return !label.isEmpty() && labelIngredient.startsWith(label);
                                })
                                .findFirst();

                            if (approx.isPresent()) {
                                structured = approx.get();
                                ingredientLabel = structured.path("text").asText("");
                                ingredientLabel = typographicalCorrection(ingredientLabel);
                                // System.out.println("Approximation : l'ingrédient '" + labelIngredient + "' est aproximé par '" + ingredientLabel + "' dans le produit " + productId + ".");
                                ingredientLabel = labelIngredient; // On garde le label original
                                ciqualCode = structured.path("ciqual_food_code").asText("");
                                ciqualProxy = structured.path("ciqual_proxy_food_code").asText("");
                                hasSubs = structured.path("has_sub_ingredients").asText("");
                            } else {
                                countError++;
                                ingredientLabel = labelIngredient; // On garde le label original
                                System.out.println("Incohérence : l'ingrédient '" + labelIngredient + "' est trouvé selon le parsing mais pas dans la liste d'ingrédients du produit "+ productId +".");
                            }
                        }

                        
                        if (ingredientSet.add(ingId)) {
                            Row ingRow = ingredientsSheet.createRow(ingRowIdx++);
                            ingRow.createCell(0).setCellValue(ingId);
                            ingRow.createCell(1).setCellValue(ingredientLabel);
                            // Si le code ciqual est vide, on laisse la cellule vide
                            if (!ciqualCode.isEmpty()) {
                                // Sinon on le convertit en entier pour l'export Excel
                                ingRow.createCell(2).setCellValue(Integer.parseInt(ciqualCode));   
                            }
                            // Si le code ciqualProxy est vide, on laisse la cellule vide
                            if (!ciqualProxy.isEmpty()) {
                                // Sinon on le convertit en entier pour l'export Excel
                                ingRow.createCell(3).setCellValue(Integer.parseInt(ciqualProxy));   
                            } 
                        }

                        // Détection d’incohérence
                        if ("no".equalsIgnoreCase(hasSubs) && !ing.getSubIngredients().isEmpty()) {
                            System.out.println("Incohérence : l'ingrédient '" + labelIngredient + "' est déclaré sans sous-ingrédients mais en contient selon le parsing.");
                        }
                        if ("yes".equalsIgnoreCase(hasSubs) && ing.getSubIngredients().isEmpty()) {
                            System.out.println("Incohérence : l'ingrédient '" + labelIngredient + "' est déclaré avec sous-ingrédients mais n'en contient pas selon le parsing.");
                        }

                        Row compRow = compositionsSheet.createRow(compRowIdx++);
                        compRow.createCell(0).setCellValue(productId);
                        compRow.createCell(1).setCellValue(typeIngredient);
                        compRow.createCell(2).setCellValue(ingId);
                        if (ing.getPercentage() != null)
                            compRow.createCell(5).setCellValue(ing.getPercentage());
                        compRow.createCell(6).setCellValue(rank++);
                    } else if (typeIngredient != null && typeIngredient.equals("SubProduct")) { 
                        compositeProduct = true;
                        // On place l'ingredient en temps que sous-produit dans les produits
                        long counter = NatclinnUtil.getNextProductCounterValue();
						String paddedCounter = String.format("%013d", counter); // format 13 chiffres
						String subProductId = "P-" + paddedCounter;
                        prodRow = productsSheet.createRow(prodRowIdx++);
                        prodRow.createCell(0).setCellValue(subProductId);
                        prodRow.createCell(1).setCellValue(labelIngredient);
                        prodRow.createCell(2).setCellValue("");
                        prodRow.createCell(3).setCellValue("SimpleProduct");
                        prodRow.createCell(4).setCellValue("");
                        prodRow.createCell(5).setCellValue("");

                        // On met à jour la composition pour le sous-produit
                        Row compRowsubProduct = compositionsSheet.createRow(compRowIdx++);
                        compRowsubProduct.createCell(0).setCellValue(productId);
                        compRowsubProduct.createCell(1).setCellValue("Product");
                        compRowsubProduct.createCell(2).setCellValue(subProductId);
                        if (ing.getPercentage() != null)
                            compRowsubProduct.createCell(5).setCellValue(ing.getPercentage());
                        compRowsubProduct.createCell(6).setCellValue(rank++);
                        
                        // On traite les ingrédients du sous-produit
                        int subRank = 1;
                        for (Ingredient sub : subIngredient) {
                            // on traite les sous-ingrédients
                             String labelSubIngredient = sub.getName();
                            if (labelSubIngredient.isEmpty()) continue;
                            String typeSubIngredient = sub.getType();
                            // Si un ingrédient n’a pas encore d’identifiant dans la map, on lui en génère un nouveau et on l’y associe, sinon on récupère l’identifiant déjà existant.
                            String ingId = ingredientIdMap.computeIfAbsent(labelSubIngredient, l -> "I-" + String.format("%013d", NatclinnUtil.getNextIngredientCounterValue()));
                            if (ingredientSet.add(ingId)) {
                                Row ingRow = ingredientsSheet.createRow(ingRowIdx++);
                                ingRow.createCell(0).setCellValue(ingId);
                                ingRow.createCell(1).setCellValue(labelSubIngredient);
                            }
                        
                            Row compRow = compositionsSheet.createRow(compRowIdx++);
                            compRow.createCell(0).setCellValue(subProductId);
                            compRow.createCell(1).setCellValue(typeSubIngredient);
                            compRow.createCell(2).setCellValue(ingId);
                            if (sub.getPercentage() != null)
                                compRow.createCell(5).setCellValue(sub.getPercentage());
                            compRow.createCell(6).setCellValue(subRank++);
                        }    
                    } else if (typeIngredient != null && typeIngredient.equals("Additif")) {
                        compositeProduct = true;
                        // Pour les additifs
                        // On place l'additif en temps que sous-produit dans les produits
                        long counter = NatclinnUtil.getNextProductCounterValue();
						String paddedCounter = String.format("%013d", counter); // format 13 chiffres
						String subProductId = "P-" + paddedCounter;
                        prodRow = productsSheet.createRow(prodRowIdx++);
                        prodRow.createCell(0).setCellValue(subProductId);
                        prodRow.createCell(1).setCellValue(labelIngredient);
                        prodRow.createCell(2).setCellValue("");
                        prodRow.createCell(3).setCellValue("AdditiveProduct");
                        prodRow.createCell(4).setCellValue("");
                        prodRow.createCell(5).setCellValue("");

                        // On met à jour la composition pour l'additif
                        Row compRowAdditiveProduct = compositionsSheet.createRow(compRowIdx++);
                        compRowAdditiveProduct.createCell(0).setCellValue(productId);
                        compRowAdditiveProduct.createCell(1).setCellValue("AdditiveProduct");
                        compRowAdditiveProduct.createCell(2).setCellValue(subProductId);
                        if (ing.getPercentage() != null)
                            compRowAdditiveProduct.createCell(5).setCellValue(ing.getPercentage());
                        compRowAdditiveProduct.createCell(6).setCellValue(rank++);

                        // On traite les ingrédients de l'additif si besoin
                        int subRank = 1;
                        for (Ingredient sub : subIngredient) {
                            // on traite les sous-ingrédients
                             String labelSubIngredient = sub.getName();
                            if (labelSubIngredient.isEmpty()) continue;
                            String typeSubIngredient = sub.getType();
                            // Si un ingrédient n’a pas encore d’identifiant dans la map, on lui en génère un nouveau et on l’y associe, sinon on récupère l’identifiant déjà existant.
                            String ingId = ingredientIdMap.computeIfAbsent(labelSubIngredient, l -> "I-" + String.format("%013d", NatclinnUtil.getNextIngredientCounterValue()));
                            if (ingredientSet.add(ingId)) {
                                Row ingRow = ingredientsSheet.createRow(ingRowIdx++);
                                ingRow.createCell(0).setCellValue(ingId);
                                ingRow.createCell(1).setCellValue(labelSubIngredient);
                            }
                            
                            Row compRow = compositionsSheet.createRow(compRowIdx++);
                            compRow.createCell(0).setCellValue(subProductId);
                            compRow.createCell(1).setCellValue(typeSubIngredient);
                            compRow.createCell(2).setCellValue(ingId);
                            if (sub.getPercentage() != null)
                                compRow.createCell(5).setCellValue(sub.getPercentage());
                            compRow.createCell(6).setCellValue(subRank++);
                        }    
                     } else if (typeIngredient != null && typeIngredient.equals("Arome")) {
                        compositeProduct = true;
                        // Pour les arômes
                        // On place l'arôme en temps que sous-produit dans les produits
                        long counter = NatclinnUtil.getNextProductCounterValue();
						String paddedCounter = String.format("%013d", counter); // format 13 chiffres
						String subProductId = "P-" + paddedCounter;
                        prodRow = productsSheet.createRow(prodRowIdx++);
                        prodRow.createCell(0).setCellValue(subProductId);
                        prodRow.createCell(1).setCellValue(labelIngredient);
                        prodRow.createCell(2).setCellValue("");
                        prodRow.createCell(3).setCellValue("AromaProduct");
                        prodRow.createCell(4).setCellValue("");
                        prodRow.createCell(5).setCellValue("");

                        // On met à jour la composition pour l'arôme
                        Row compRow = compositionsSheet.createRow(compRowIdx++);
                        compRow.createCell(0).setCellValue(productId);
                        compRow.createCell(1).setCellValue("Arôme");
                        compRow.createCell(2).setCellValue(subProductId);
                        if (ing.getPercentage() != null)
                            compRow.createCell(5).setCellValue(ing.getPercentage());
                        compRow.createCell(6).setCellValue(rank++);
                        

                        // On traite les ingrédients de l'arôme si besoin (rare)
                        int subRank = 1;
                        for (Ingredient sub : subIngredient) {
                            // on traite les sous-ingrédients
                             String labelSubIngredient = sub.getName();
                            if (labelSubIngredient.isEmpty()) continue;
                            String typeSubIngredient = sub.getType();
                            // Si un ingrédient n’a pas encore d’identifiant dans la map, on lui en génère un nouveau et on l’y associe, sinon on récupère l’identifiant déjà existant.
                            String ingId = ingredientIdMap.computeIfAbsent(labelSubIngredient, l -> "I-" + String.format("%013d", NatclinnUtil.getNextIngredientCounterValue()));
                            if (ingredientSet.add(ingId)) {
                                Row ingRow = ingredientsSheet.createRow(ingRowIdx++);
                                ingRow.createCell(0).setCellValue(ingId);
                                ingRow.createCell(1).setCellValue(labelSubIngredient);
                            }
                            compRow = compositionsSheet.createRow(compRowIdx++);
                            compRow.createCell(0).setCellValue(subProductId);
                            compRow.createCell(1).setCellValue(typeSubIngredient);
                            compRow.createCell(2).setCellValue(ingId);
                            if (sub.getPercentage() != null)
                                compRow.createCell(5).setCellValue(sub.getPercentage());
                            compRow.createCell(6).setCellValue(subRank++);
                        }    
                    } else if (typeIngredient != null) {
                        System.out.println("Type d'ingrédient inconnu : " + typeIngredient + " pour l'ingrédient " + labelIngredient);
                    } 
                    if (compositeProduct) {
                        cellTypeProduct.setCellValue("CompositeProduct");
                    }
                    
                }
            }
        }
        System.out.println("Nombre d'erreurs" + countError);

        // Écriture dans fichier Excel
        // Ajuster automatiquement la largeur des colonnes sur toutes les feuilles
        autoSizeAllColumns(productsSheet);
        autoSizeAllColumns(ingredientsSheet);
        autoSizeAllColumns(compositionsSheet);
        autoSizeAllColumns(packagingSheet);
        autoSizeAllColumns(controlledOriginLabelSheet);
        autoSizeAllColumns(cleanLabelSheet);
        autoSizeAllColumns(manufacturingProcessSheet);
        autoSizeAllColumns(nutriScoreSheet);

        String filePath = NatclinnConf.folderForData + "/OFF-export.xlsx";
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            workbook.write(out);
            System.out.println("Fichier exporté : " + filePath);
        }
        workbook.close();
    }

    private static String typographicalCorrection(String label) {
        label = label.toLowerCase();
        label = label.replaceAll("\\*", "");
        label = label.replaceAll("œ", "oe");
        label = label.replaceAll("_([^_]+)_", "$1");
        label = label.replaceAll("\"", "");
        label = label.replaceAll("\\b([DdLlJjNnSsTtMm])’", "$1'");
        label = label.trim();
        return label;
       
    }

    private static void createHeaders(Sheet sheet, String... headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            sheet.autoSizeColumn(i);
        }
    }

    private static void autoSizeAllColumns(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                int lastCellNum = firstRow.getLastCellNum();
                for (int i = 0; i < lastCellNum; i++) {
                    sheet.autoSizeColumn(i);
                }
            }
        }
    }

    private static String getHttpContent(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Java");
        try (Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    
    private static void hexViewer(String input)  {
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            String hex = String.format("%04x", (int) ch); // affichage sur 4 chiffres
            System.out.println("Char: '" + ch + "' | Code point: U+" + hex);
        }
    }
}
