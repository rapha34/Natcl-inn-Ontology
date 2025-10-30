package natclinn.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NatclinnUtil {

	// Classe pour représenter un ingrédient
    public static class Ingredient {
        String name;
        Double percentage;
        String type = "ingredient"; // par défaut
        List<String> annotations = new ArrayList<>();
        List<Ingredient> subIngredients = new ArrayList<>();

        public Ingredient(String name) {
            this.name = name.trim();
        }

        public void setPercentage(Double pct) {
            this.percentage = pct;
        }

        public void setType(String type) {
            this.type = type.trim();
        }

        public void addAnnotation(String annotation) {
            this.annotations.add(annotation.trim());
        }

        public void addSubIngredient(Ingredient ing) {
            this.subIngredients.add(ing);
        }

		

        public String getName() {
			return name;
		}

		public Double getPercentage() {
			return percentage;
		}

		public String getType() {
			return type;
		}

		public List<String> getAnnotations() {
			return annotations;
		}

		public List<Ingredient> getSubIngredients() {
			return subIngredients;
		}

		@Override
        public String toString() {
            return toString(0);
        }

        private String toString(int indent) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < indent * 2; i++) sb.append(' ');
            sb.append("- ");
            if (type != null && !"ingredient".equals(type)) {
                sb.append(type).append(": ");
            }
            sb.append(name);
            if (percentage != null) {
                sb.append(" (").append(percentage).append("%)");
            }
            if (annotations.size() == 1) {
                sb.append(" [").append(annotations.get(0)).append("]");
            } else if (!annotations.isEmpty()) {
                sb.append(" [").append(String.join(", ", annotations)).append("]");
            }
            sb.append("\n");
            for (Ingredient sub : subIngredients) {
                sb.append(sub.toString(indent + 1));
            }
            return sb.toString();
        }
    }


    // private static void printIngredientTree(NatclinnUtil.Ingredient ing, int indent) {
    //     // Indentation visuelle
    //     for (int i = 0; i < indent; i++) System.out.print("  ");

    //     // Affichage des infos de l'ingrédient
    //     System.out.print("- ");
    //     if (ing.getType() != null) System.out.print(ing.getType() + ": ");
    //     System.out.print(ing.getName());
    //     if (ing.getPercentage() != null) System.out.print(" (" + ing.getPercentage() + "%)");
    //     if (!ing.getAnnotations().isEmpty()) System.out.print(" " + ing.getAnnotations());
    //     System.out.println();

    //     // Appel récursif pour les sous-ingrédients
    //     for (NatclinnUtil.Ingredient sub : ing.getSubIngredients()) {
    //         printIngredientTree(sub, indent + 1);
    //     }
    // }

	public static List<Ingredient> parse(String input) {
        input = removeOrphanClosingParentheses(input);
        input = truncateAfterPoint(input);
        input = input.replaceAll("\\{/?allergene\\}", "");
        input = input.replaceAll("(?i)origine\\s*:\\s*", "origine ");
        input = input.replaceAll("\\*", "");
        input = input.replaceAll("œ", "oe");
        input = input.replaceAll("Œ", "Oe");
        input = input.replaceAll("\\.$", "");
        input = input.replaceAll("_([^_]+)_","$1");
        input = replaceDash(input);
        input = input.replaceAll("(\\d+),(\\d+)\\s*%", "$1.$2%");
        // Suppression des points finaux
        input = input.replaceAll("\\.$", "");
        // Suppression des entités HTML courantes
        input = input.replaceAll("&quot", "");
        // Correction des apostrophes typographiques
        input = input.replaceAll("\\b([DdLlJjNnSsTtMm])’", "$1'");
        // Gestion des espaces insécables et des tirets dans "glucose - fructose"
        input = input.replaceAll("\\bglucose\\s*-\\s*fructose\\b", "glucose-fructose");
        input = input.replace('\u00A0', ' ').trim();
        List<String> partsList = splitRespectingParentheses(input);
        List<Ingredient> ingredients = new ArrayList<>();
        for (String part : partsList) {
            // Spécial "tomate" pour enrichir les ingrédients dérivés
            part = factorizeTomato(part);
            Ingredient ing = parseIngredientRecursive(part.trim());
            if (ing != null) ingredients.add(ing);
        }
        return ingredients;
    }

    private static String factorizeTomato(String input) {
        // Regex : capture "tomate" ou "tomates" + contenu entre parenthèses
        Pattern pattern = Pattern.compile("(tomates?)([^()]*)\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String base = matcher.group(1);   // "tomate" ou "tomates"
            String prefix = matcher.group(2); // ex. " 9,5% "
            String contenu = matcher.group(3); // ex. "concentrées, concassées et jus"

            // Liste des mots-clés à enrichir avec protection "pas déjà suivi de de tomate(s)"
            String[][] motsCles = {
                {"(?i)\\bconcentr[ée]?(?:e|es|s)?\\b(?!\\s+de\\s+tomates?)", "concentré de " + base},
                {"(?i)\\bconcas+[ée]?(?:e|es|s)?\\b(?!\\s+de\\s+tomates?)", "concassé de " + base},
                {"(?i)\\bjus\\b(?!\\s+de\\s+tomates?)", "jus de " + base},
                {"(?i)\\bpur[ée]s?\\b(?!\\s+de\\s+tomates?)", "purée de " + base},
                {"(?i)\\bcoulis\\b(?!\\s+de\\s+tomates?)", "coulis de " + base}
            };

            // Appliquer les remplacements
            for (String[] mc : motsCles) {
                contenu = contenu.replaceAll(mc[0], mc[1]);
            }

            // Transformer "et" en virgule
            contenu = contenu.replaceAll("\\s*et\\s*", ", ");

            // Nettoyage : enlever doubles virgules et espaces superflus
            contenu = contenu.replaceAll(",\\s*,", ", ");
            contenu = contenu.replaceAll("\\s+,", ", ");

            matcher.appendReplacement(sb, base + prefix + "(" + contenu + ")");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
 * Analyse récursive d'un ingrédient (ou sous-ingrédient) à partir d'une chaîne.
 * Gère les annotations, les pourcentages, les types (Additif, SubProduct), et les sous-ingrédients.
 */
    private static Ingredient parseIngredientRecursive(String part) {
        if (part.isEmpty()) return null;

        List<String> annotations = new ArrayList<>();

        // Extraction des annotations entre crochets [ ... ] (rare mais possible)
        Matcher bracketMatcher = Pattern.compile("\\[([^\\]]+)\\]").matcher(part);
        while (bracketMatcher.find()) {
            annotations.add(bracketMatcher.group(1).trim());
        }
        part = part.replaceAll("\\[[^\\]]+\\]", "").trim(); // Nettoyage des annotations extraites

        // Extraction du pourcentage (ex : 4%, 26%, etc.)
        Double pct = null;
        Matcher pctMatcher = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*%").matcher(part);
        if (pctMatcher.find()) {
            pct = Double.parseDouble(pctMatcher.group(1).replace(",", "."));
            part = part.replace(pctMatcher.group(), "").trim();
        }

        // Recherche des positions des parenthèses de haut niveau
        List<Integer> parens = findTopLevelParentheses(part);
        List<Integer> colon = findColons(part);
        Boolean colonBeforeParens = false;
        List<Ingredient> subIngredients = new ArrayList<>();
        String outerAnnotation = null;
        if (!parens.isEmpty() && !colon.isEmpty()) {
           if (colon.get(0) < parens.get(0) ) {
                // Si le premier double point est avant la première parenthèse on considère que c'est un additif
                colonBeforeParens = true;
           }
        }
        if (!colonBeforeParens) {
        if (parens.size() >= 4) {
            // Cas où il y a deux paires de parenthèses (ex : chocolat (origine UE) 4% (sucre, ...))
            int open1 = parens.get(0);
            int close1 = parens.get(1);
            int open2 = parens.get(2);
            int close2 = parens.get(3);

            String annotationPart = part.substring(open1 + 1, close1).trim(); // Ex : origine UE
            String subIngredientsPart = part.substring(open2 + 1, close2).trim(); // Ex : sucre, pâte de cacao, etc.
            String before = part.substring(0, open1).trim(); // Ex : chocolat

            outerAnnotation = annotationPart;

            // Découpe des sous-ingrédients internes
            List<String> subParts = splitRespectingParentheses(subIngredientsPart);
            for (String sub : subParts) {
                Ingredient subIng = parseIngredientRecursive(sub.trim());
                if (subIng != null) subIngredients.add(subIng);
            }

            part = before;
        } else if (parens.size() >= 2) {
            // Cas simple d'une seule paire de parenthèses (ex : poudre à lever (diphosphates, ...))
            int open1 = parens.get(0);
            int close1 = parens.get(1);
            String inside = part.substring(open1 + 1, close1).trim();
            String before = part.substring(0, open1).trim();

            if (looksLikeSubIngredientList(inside)) {
                // Ce sont des sous-ingrédients
                List<String> subParts = splitRespectingParentheses(inside);
                for (String sub : subParts) {
                    Ingredient subIng = parseIngredientRecursive(sub.trim());
                    if (subIng != null) subIngredients.add(subIng);
                }
            } else {
                // C'est une annotation normale (ex : "origine végétale")
                outerAnnotation = inside;
            }
            part = before;
        }
        }

        // Cas des additifs détectés par la présence de "nom : liste"
        if (part.contains(":")) { 
            String[] split = part.split(":", 2);
            if (split.length == 2) {
                String name = split[0].trim(); // ex : "émulsifiant", "épaississants"
                String content = split[1].trim(); // ex : "gomme xanthane (avoine) - gomme guar (agar-agar)"
                List<String> subParts = splitRespectingParenthesesAdditif(content);

                Ingredient ing = new Ingredient(name);
                ing.setType("Additif");

                for (String sub : subParts) {
                    String subTrimmed = sub.trim();

                    // Gestion spécifique des annotations pour sous-ingrédients d'additif
                    String subName = subTrimmed;
                    List<String> subAnnotations = new ArrayList<>();

                    Matcher subMatcher = Pattern.compile("(.+?)\\s*\\(([^\\)]+)\\)\\s*$").matcher(subTrimmed);
                    if (subMatcher.matches()) {
                        subName = subMatcher.group(1).trim();
                        subAnnotations.add(subMatcher.group(2).trim());
                    }

                    Ingredient subIng = new Ingredient(subName);
                    subIng.setType("ingredient"); // forcé à "ingredient"
                    subAnnotations.forEach(subIng::addAnnotation);

                    ing.addSubIngredient(subIng);
                }
                return ing;
            }
        }

        // Gestion d'un ingrédient simple (ou SubProduct si sous-ingrédients détectés)
        String name = part.trim();

        // Détection d'annotation sur l'ingrédient lui-même (ex : gomme guar (agar-agar))
        Matcher nameParen = Pattern.compile("(.+?)\\s*\\(([^\\)]+)\\)\\s*$").matcher(name);
        if (nameParen.matches()) {
            name = nameParen.group(1).trim();
            annotations.add(nameParen.group(2).trim());
        }

        Ingredient ing = new Ingredient(name);
        if (!subIngredients.isEmpty()) {
            ing.setType("SubProduct"); // Sous-ingrédients trouvés => SubProduct
        }  else {
            // Si aucun type spécifique n'est trouvé, on garde le type par défaut "ingredient"
            ing.setType("Ingredient"); // Ingrédient simple
        }
            if (name.contains("poudre à lever")) {
                ing.setType("Additif"); // Poudre à lever
            } else if (name.contains("arôme")) {
                ing.setType("Arome"); // Arôme
            } else if (name.contains("colorant")) {
                ing.setType("Additif"); // Colorant
            } else if (name.contains("conservateur")) {
                ing.setType("Additif"); // Conservateur
            } else if (name.contains("stabilisant")) {
                ing.setType("Additif"); // Stabilisant
            } else if (name.contains("émulsifiant")) {
                ing.setType("Additif"); // Émulsifiant
            } else if (name.contains("acidifiant")) {
                ing.setType("Additif"); // Acidifiant
            } else if (name.contains("antioxydant")) {
                ing.setType("Additif"); // Antioxydant
            } else if (name.contains("antiagglomérant")) {
                ing.setType("Additif"); // Anti-agglomérant
            }

        if (pct != null) ing.setPercentage(pct);
        if (outerAnnotation != null) annotations.add(outerAnnotation);
        annotations.forEach(ing::addAnnotation);
        subIngredients.forEach(ing::addSubIngredient);

        return ing;
    }

    /**
     * Sépare une chaîne en respectant les parenthèses.
     * Utilise des virgules et des points-virgules comme délimiteurs principaux.
     * Gère les parenthèses imbriquées pour éviter de couper à l'intérieur.
     */
    private static List<String> splitRespectingParentheses(String input) {
        List<String> result = new ArrayList<>();
        int level = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '(' || c == '[') level++;
            else if (c == ')' || c == ']') level--;
            if ((c == ',' || c == ';') && level == 0) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) result.add(sb.toString().trim());
        return result;
    }

    private static List<String> splitRespectingParenthesesAdditif(String input) {
        List<String> result = new ArrayList<>();
        int level = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '(' || c == '[') level++;
            else if (c == ')' || c == ']') level--;
            if ((c == ',' || c == ';' || c == '-') && level == 0) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        if (sb.length() > 0) result.add(sb.toString().trim());
		
        return result;
    }

    private static List<Integer> findTopLevelParentheses(String input) {
        List<Integer> positions = new ArrayList<>();
        int level = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') {
                if (level == 0) positions.add(i);
                level++;
            } else if (c == ')') {
                level--;
                if (level == 0) positions.add(i);
            }
        }
        return positions;
    }

    private static List<Integer> findColons(String input) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ':') {
                positions.add(i);
            } 
        }
        return positions;
    }

    private static boolean looksLikeSubIngredientList(String content) {
        return content.contains(",") || content.contains(":");
    }

    // public static String replaceDash(String input) {
        
    //     // Pattern pour capturer ce qui suit les deux points jusqu'à la virgule suivante
    //     Pattern pattern = Pattern.compile(":\\s*([^,]+)");
    //     Matcher matcher = pattern.matcher(input);
        
    //     // Collecter les segments trouvés
    //     List<String> segments = new ArrayList<>();
    //     while (matcher.find()) {
    //         String found = matcher.group(1).trim();
    //         segments.add(found);
    //     }
        
    //     // Remplacer les " - " par " , " seulement en dehors des segments trouvés
    //     String modifiedText = input;
    //     for (String segment : segments) {
    //         // Temporairement remplacer les segments par des placeholders
    //         modifiedText = modifiedText.replace(": " + segment, ": PLACEHOLDER_" + segments.indexOf(segment));
    //     }
        
    //     // Remplacer les " - " par " , " dans le texte sans les segments
    //     modifiedText = modifiedText.replace(" - ", " , ");
        
    //     // Restaurer les segments originaux
    //     for (int i = 0; i < segments.size(); i++) {
    //         modifiedText = modifiedText.replace(": PLACEHOLDER_" + i, ": " + segments.get(i));
    //     }

    //     return modifiedText.toString();
    // }
public static String replaceDash(String input) {

    // 1. Protéger les mots composés avec des tirets
    Map<String, String> protectedMap = new LinkedHashMap<>();
    protectedMap.put("glucose - fructose", "PROTECTED_GLUCOSE_FRUCTOSE");
    protectedMap.put("glucose-fructose", "PROTECTED_GLUCOSE_FRUCTOSE_2");
    protectedMap.put("sirop de glucose - fructose", "PROTECTED_SIROP");
    protectedMap.put("sirop de glucose-fructose", "PROTECTED_SIROP_2");
    // 🔁 Ajoute ici d'autres exceptions au besoin

    for (Map.Entry<String, String> entry : protectedMap.entrySet()) {
        input = input.replaceAll("(?i)\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
    }

    // 2. Identifier les zones protégées (après les ":") comme tu le fais déjà
    Pattern pattern = Pattern.compile(":\\s*([^,]+)");
    Matcher matcher = pattern.matcher(input);

    List<String> segments = new ArrayList<>();
    while (matcher.find()) {
        segments.add(matcher.group(1).trim());
    }

    String modifiedText = input;
    for (int i = 0; i < segments.size(); i++) {
        modifiedText = modifiedText.replace(": " + segments.get(i), ": PLACEHOLDER_" + i);
    }

    // 3. Remplacement global " - " ➝ " , " (hors zones protégées)
    modifiedText = modifiedText.replace(" - ", " , ");

    // 4. Restauration des segments protégés
    for (int i = 0; i < segments.size(); i++) {
        modifiedText = modifiedText.replace(": PLACEHOLDER_" + i, ": " + segments.get(i));
    }

    // 5. Restauration des expressions protégées (glucose-fructose, etc.)
    for (Map.Entry<String, String> entry : protectedMap.entrySet()) {
        modifiedText = modifiedText.replace(entry.getValue(), entry.getKey().replace(" - ", "-"));
    }

    return modifiedText;
}


public static String removeOrphanClosingParentheses(String text) {
        StringBuilder result = new StringBuilder();
        Stack<Integer> openParenStack = new Stack<>();
        
        // Première passe : identifier les parenthèses ouvrantes valides
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                openParenStack.push(i);
            } else if (c == ')') {
                if (!openParenStack.isEmpty()) {
                    openParenStack.pop(); // Paire valide trouvée
                }
            }
        }
        
        // Deuxième passe : reconstruire le texte sans les parenthèses fermantes orphelines
        openParenStack.clear();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                openParenStack.push(i);
                result.append(c);
            } else if (c == ')') {
                if (!openParenStack.isEmpty()) {
                    openParenStack.pop();
                    result.append(c); // Garder cette parenthèse fermante
                }
                // Ignorer les parenthèses fermantes orphelines
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }


    public static String truncateAfterPoint(String text) {
        // Regex pour un point qui n'est pas entouré de chiffres
        String regex = "(?<!\\d)\\.(?!\\d)";
        // (?<!\d) : lookbehind négatif - le caractère précédent n'est PAS un chiffre
        // \. : le point littéral
        // (?!\d) : lookahead négatif - le caractère suivant n'est PAS un chiffre
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            int firstPeriodIndex = matcher.start();
            return text.substring(0, firstPeriodIndex).trim();
        }
        return text; // Pas de point non-numérique trouvé, retourner le texte original
    }







	public static synchronized long getNextProductCounterValue() {
		String counterFile = NatclinnConf.mainFolderNatclinn + "/counterProduct.txt"; 
		long counter = 9990000000001L;

		File file = new File(counterFile);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line = reader.readLine();
				if (line != null) {
					counter = Long.parseLong(line.trim()) + 1; // utilisation de Long.parseLong
				}
			} catch (IOException | NumberFormatException e) {
				System.err.println("Erreur lecture compteur produit, démarrage à 9990000000001.");
			}
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(String.valueOf(counter));
		} catch (IOException e) {
			System.err.println("Erreur écriture compteur produit.");
		}

		return counter;
	}
	
	public static synchronized long getNextIngredientCounterValue() {
		String counterFile = NatclinnConf.mainFolderNatclinn + "/counterIngredient.txt"; 
		long counter = 9980000000001L;

		File file = new File(counterFile);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line = reader.readLine();
				if (line != null) {
					counter = Long.parseLong(line.trim()) + 1; // utilisation de Long.parseLong
				}
			} catch (IOException | NumberFormatException e) {
				System.err.println("Erreur lecture compteur ingrédient, démarrage à 9980000000001.");
			}
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(String.valueOf(counter));
		} catch (IOException e) {
			System.err.println("Erreur écriture compteur produit.");
		}

		return counter;
	}
	
	// Génère une URI "propre" à partir d’un label brut
    public static String makeURI(String base, String label) {
        if (label == null || label.trim().isEmpty()) {
            return null; // Pas de triplet si pas de label
        }

        try {
            String cleaned = label.trim().replaceAll(" ", "-"); // Optionnel : plus lisible
            String encoded = URLEncoder.encode(cleaned, StandardCharsets.UTF_8.name());
            return base + encoded;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	// Retourne un tableau contenant les noms de fichier à traiter	
	public static ArrayList<String> makeListFileName(String nameJsonFile) throws Exception {
		ArrayList<String> listFile = new ArrayList<String>();
		File file  = new File(nameJsonFile);
		if (file.exists()) {
		   //on récupère le noms des fichier à traiter dans le fichier JSON
			String jsonArray = NatclinnUtil.readFileAsString(nameJsonFile);

			ObjectMapper objectMapper = new ObjectMapper();

			List<FileName> fileNames = objectMapper.readValue(jsonArray, new TypeReference<List<FileName>>(){});
			
			fileNames.forEach(x -> listFile.add(x.getName()));
				
		} else {
			System.out.println("Le fichier " + nameJsonFile +  " est inexistant !"); 
		}
		
		return listFile;
	}

	// // Retourne un nom de fichier à traiter	
	// 	public static String makeFileName(String nameJsonFile) throws Exception {
	// 		String nameOfFile = null;
	// 		File file  = new File(nameJsonFile);
	// 		if (file.exists()) {
	// 		   //on récupère le noms des fichier à traiter dans le fichier JSON
	// 			String jsonArray = NatclinnUtil.readFileAsString(nameJsonFile);

	// 			ObjectMapper objectMapper = new ObjectMapper();

	// 			FileName fileName = objectMapper.readValue(jsonArray, new TypeReference<FileName>(){});
				
	// 			nameOfFile = fileName.getName();
	// 		} else {
	// 			System.out.println("Le fichier " + nameJsonFile +  " est inexistant !"); 
	// 		}
			
			
	// 		return nameOfFile ;
	// 	}

    // Retourne un nom de fichier à traiter
    public static String makeFileName(String nameJsonFile) throws Exception {
        String nameOfFile = null;
        File file = new File(nameJsonFile);

        if (file.exists()) {
            if (file.length() == 0) {
                System.err.println("⚠️ Le fichier " + nameJsonFile + " est vide !");
                return "resultsQueries_default.json";
            }

            String jsonArray = NatclinnUtil.readFileAsString(nameJsonFile);
            if (jsonArray == null || jsonArray.trim().isEmpty()) {
                System.err.println("⚠️ Le contenu du fichier " + nameJsonFile + " est vide ou invalide !");
                return "resultsQueries_default.json";
            }

            ObjectMapper objectMapper = new ObjectMapper();
            FileName fileName = objectMapper.readValue(jsonArray, new TypeReference<FileName>(){});
            nameOfFile = fileName.getName();
        } else {
            System.err.println("⚠️ Le fichier " + nameJsonFile + " est inexistant !");
            nameOfFile = "resultsQueries_default.json";
        }

        return nameOfFile;
    }
	
	// Retourne un top	
	public static String extractParameter(String nameJsonFile, String keyParameter) throws Exception {
		String valueParameter = "";
		File file  = new File(nameJsonFile);
		if (file.exists()) {
			//on récupère le contenu du fichier
			String jsonString = NatclinnUtil.readFileAsString(nameJsonFile);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(jsonString );
			valueParameter = jsonNode.get(keyParameter).toString();
			
		} else {
			System.out.println("Le fichier " + nameJsonFile +  " est inexistant !"); 
		}

		return valueParameter;
	}

	public static String readFileAsString(String file)throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(file)));
    }
	
	/**
	 * For a given subject resource and a given collection of (label/comment) properties this finds the most
	 * suitable value of either property for a given list of languages (usually from the current user's preferences).
	 * For example, if the user's languages are [ "en-AU" ] then the function will prefer "mate"@en-AU over
	 * "friend"@en and never return "freund"@de.  The function falls back to literals that have no language
	 * if no better literal has been found.
	 * @param resource  the subject resource
	 * @param langs  the allowed languages
	 * @param properties  the properties to check
	 * @return the best suitable value or null
	 */
	
	public static Literal getBestStringLiteral(Resource resource) {
		
		// Initialisation de la configuration
		// Chemin d'acc�s, noms fichiers...
		new NatclinnConf();  

		
		List<String> langs = Arrays.asList(NatclinnConf.listPreferredLanguages);
		List<Property> properties = new ArrayList<>();
		List<String> stringProperties = Arrays.asList(NatclinnConf.listLabelProperties);
		Property labelProperty = null ;
		for (String stringProperty : stringProperties) {
			labelProperty = ResourceFactory.createProperty(stringProperty);
			properties.add(labelProperty);
		}
		
		
		return getBestStringLiteral(resource, langs, properties);
	}
	

	
	public static Literal getBestStringLiteral(Resource resource, List<String> langs, Iterable<Property> properties) {
		return getBestStringLiteral(resource, langs, properties, (r,p) -> r.listProperties(p));
	}
	
	
	public static Literal getBestStringLiteral(Resource resource, List<String> langs, Iterable<Property> properties, BiFunction<Resource,Property,Iterator<Statement>> getter) {
		Literal label = null;
		int bestLang = -1;
		for(Property predicate : properties) {
			Iterator<Statement> it = getter.apply(resource, predicate);
			while(it.hasNext()) {
				RDFNode object = it.next().getObject();
				if(object.isLiteral()) {
					Literal literal = (Literal)object;
					String lang = literal.getLanguage();
					if(lang.length() == 0 && label == null) {
						label = literal;
					}
					else {
						// 1) Never use a less suitable language
						// 2) Never replace an already existing label (esp: skos:prefLabel) unless new lang is better
						// 3) Fall back to more special languages if no other was found (e.g. use en-GB if only "en" is accepted)
						int startLang = bestLang < 0 ? langs.size() - 1 : (label != null ? bestLang - 1 : bestLang);
						for(int i = startLang; i >= 0; i--) {
							String langi = langs.get(i);
							if(langi.equals(lang)) {
								label = literal;
								bestLang = i;
							}
							else if(lang.contains("-") && NodeFunctions.langMatches(lang, langi) && label == null) {
								label = literal;
							}
						}
					}
				}
			}
		}
		return label;
	}

	public static String decodeHexToStringUTF8(String hex){

        String[] list=hex.split("(?<=\\G.{2})");
        ByteBuffer buffer= ByteBuffer.allocate(list.length);
        for(String str: list)
            buffer.put(Byte.parseByte(str,16)); 
        String strUTF8 = "";
		try {
			strUTF8 = new String(buffer.array(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
        return strUTF8;

}
	

}