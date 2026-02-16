package ontologyManagement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Exporte les données d'un projet MyChoice au format JSON compatible DAMN.
 * Structure compatible avec le format DAMN officiel incluant le champ dlgp.
 */
public class ExportMychoiceProjectToDamnJson {
    
    private static ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Extrait un nom lisible a partir de l'URI du projet.
     */
    private static String extractProjectName(String projectUri) {
        if (projectUri == null || projectUri.isEmpty()) {
            return "Natclinn Project";
        }
        int slash = projectUri.lastIndexOf('/');
        int hash = projectUri.lastIndexOf('#');
        int pos = Math.max(slash, hash);
        if (pos >= 0 && pos < projectUri.length() - 1) {
            return projectUri.substring(pos + 1);
        }
        return projectUri;
    }
    
    /**
     * Formate une chaîne pour qu'elle soit compatible avec la syntaxe DAMN.
     */
    private static String format(String str) {
        if (str == null || str.isEmpty()) return "";
        
        // Normalisation Unicode (décomposition des accents)
        str = Normalizer.normalize(str, Normalizer.Form.NFD)
            .toLowerCase()
            .replaceAll("'", "_")          // Remplace apostrophes par underscores
            .replaceAll("\\s", "_")        // Remplace espaces par underscores
            .replaceAll("[^a-z0-9_]", "")  // Supprime tous les autres caractères spéciaux
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", ""); // Supprime les diacritiques
        
        // Supprime les underscores en début
        while (!str.isEmpty() && str.charAt(0) == '_') {
            str = str.substring(1);
        }
        
        return str;
    }
    
    /**
     * Exporte le projet MyChoice en format JSON DAMN avec structure complète.
     */
    public static void exportToJson(
            Map<String, ExportMychoiceProjectToDamn.AlternativeData> alternatives,
            Map<String, ExportMychoiceProjectToDamn.ArgumentData> arguments,
            String projectUri,
            String outputFile) throws IOException {
        
        // Générer le texte DAMN
        String dlgpContent = generateDamnText(alternatives, arguments, projectUri);
        
        // Construire la query avec tous les produits
        StringBuilder queryBuilder = new StringBuilder();
        boolean first = true;
        for (ExportMychoiceProjectToDamn.AlternativeData alt : alternatives.values()) {
            if (!first) {
                queryBuilder.append(", ");
            }
            queryBuilder.append("hasGoodNaturalness(").append(format(alt.name)).append(")");
            first = false;
        }
        String query = queryBuilder.toString();
        
        // Créer la structure JSON principale
        ObjectNode root = mapper.createObjectNode();
        
        // Métadonnées du projet
        root.put("id", "natclinn_damn_project");
        root.put("name", extractProjectName(projectUri));
        root.put("description", "Damn project from Natclinn project\n");
        root.put("semantic", "PDLwithoutTD");
        root.put("query", query);
        root.put("creator_id", "natclinn_creator");
        
        // Contributors
        ArrayNode contributors = mapper.createArrayNode();
        contributors.add("natclinn_creator");
        root.set("contributors", contributors);
        
        // KBs (Knowledge Bases)
        ArrayNode kbs = mapper.createArrayNode();
        ObjectNode kb = mapper.createObjectNode();
        kb.put("id", "natclinn_kb_common");
        kb.put("source", "Common");
        kb.put("agent_id", "natclinn_creator");
        kb.put("dlgp", dlgpContent);
        kb.put("selected", true);
        kb.put("locked", false);
        kb.put("type", "common");
        
        ArrayNode editors = mapper.createArrayNode();
        editors.add("Common");
        kb.set("editors", editors);
        
        kbs.add(kb);
        root.set("kbs", kbs);
        
        root.put("public", false);
        
        // Écriture du fichier JSON
        try (FileWriter fw = new FileWriter(outputFile)) {
            fw.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        }
        
        System.out.println("Export JSON DAMN réussi : " + outputFile);
        System.out.println("  - " + arguments.size() + " arguments exportés");
    }
    
    /**
     * Génère le contenu texte DAMN avec tous les faits, règles et contraintes.
     */
    private static String generateDamnText(
            Map<String, ExportMychoiceProjectToDamn.AlternativeData> alternatives,
            Map<String, ExportMychoiceProjectToDamn.ArgumentData> arguments,
            String projectUri) {
        
        StringBuilder sb = new StringBuilder();
        
        // En-tête
        sb.append("%%% === EXPORT MyChoice vers DAMN ===\n");
        sb.append("%%% Projet : ").append(projectUri).append("\n");
        sb.append("%%%\n\n");
        
        // FAITS STRICTS - Produits (toutes les alternatives)
        sb.append("%%% === FAITS STRICTS - Produits ===\n\n");
        for (ExportMychoiceProjectToDamn.AlternativeData alt : alternatives.values()) {
            sb.append("product(").append(format(alt.name)).append(").\n");
        }
        sb.append("\n");
        
        // FAITS STRICTS - Arguments
        sb.append("%%% === FAITS STRICTS - Arguments ===\n\n");
        
        int argumentIndex = 1;
        for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
            String argId = "arg" + argumentIndex;
            
            sb.append("argument(").append(argId).append(").\n");
            sb.append("argProduct(").append(argId).append(",").append(format(arg.alternativeName)).append(").\n");
            
            // Tag
            String tag = "";
            if (!arg.nameProperty.isEmpty() && !arg.value.isEmpty()) {
                tag = format(arg.nameProperty + "_" + arg.value);
            } else if (!arg.nameProperty.isEmpty()) {
                tag = format(arg.nameProperty);
            } else if (!arg.nameCriterion.isEmpty()) {
                tag = format(arg.nameCriterion);
            } else if (!arg.tagInitiator.isEmpty()) {
                tag = format(arg.tagInitiator);
            } else if (!arg.value.isEmpty()) {
                tag = format(arg.value);
            }
            if (!tag.isEmpty()) {
                sb.append("argTag(").append(argId).append(",").append(tag).append(").\n");
            }
            
            // Polarité
            if (arg.polarity != null && !arg.polarity.isEmpty()) {
                String polarity = arg.polarity.equals("-") ? "con" : "pro";
                sb.append("argPolarity(").append(argId).append(",").append(polarity).append(").\n");
            }
            
            // Fiabilité
            if (arg.sources != null && !arg.sources.isEmpty()) {
                for (ExportMychoiceProjectToDamn.SourceData source : arg.sources) {
                    if (source.typeSourceFiability > 0) {
                        sb.append("argFiability(").append(argId).append(",").append(source.typeSourceFiability).append(").\n");
                        break;
                    }
                }
            }
            
            sb.append("\n");
            argumentIndex++;
        }
        sb.append("\n");
        
        // REGLES STRICTS
        sb.append("%%% === REGLES STRICTS  ===\n\n");
        sb.append("%%% Niveaux de force des arguments basés sur la fiabilité\n");
        sb.append("%%% 1: très fiable, 2: fiable, 3: moins fiable, 4: peu fiable, 5: très peu fiable\n\n");
        sb.append("veryStrongArgument(Arg) :- argFiability(Arg,1).\n");
        sb.append("strongArgument(Arg) :- argFiability(Arg,2).\n");
        sb.append("moderateArgument(Arg) :- argFiability(Arg,3).\n");
        sb.append("weakArgument(Arg) :- argFiability(Arg,4).\n");
        sb.append("veryWeakArgument(Arg) :- argFiability(Arg,5).\n\n");
        
        sb.append("hasVeryStrongPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), veryStrongArgument(Arg).\n");
        sb.append("hasStrongPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), strongArgument(Arg).\n");
        sb.append("hasModeratePro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), moderateArgument(Arg).\n");
        sb.append("hasWeakPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), weakArgument(Arg).\n");
        sb.append("hasVeryWeakPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), veryWeakArgument(Arg).\n\n");
        
        sb.append("hasVeryStrongCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), veryStrongArgument(Arg).\n");
        sb.append("hasStrongCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), strongArgument(Arg).\n");
        sb.append("hasModerateCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), moderateArgument(Arg).\n");
        sb.append("hasWeakCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), weakArgument(Arg).\n");
        sb.append("hasVeryWeakCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), veryWeakArgument(Arg).\n\n");
        
        // REGLES DEFAISABLES
        sb.append("%%% === REGLES DEFAISABLES ===\n\n");
        sb.append("[r1] hasGoodNaturalness(X) <= hasVeryStrongPro(X).\n");
        sb.append("[r2] hasGoodNaturalness(X) <= hasStrongPro(X).\n");
        sb.append("[r3] hasGoodNaturalness(X) <= hasModeratePro(X).\n");
        sb.append("[r4] hasGoodNaturalness(X) <= hasWeakPro(X).\n");
        sb.append("[r5] hasGoodNaturalness(X) <= hasVeryWeakPro(X).\n\n");
        
        sb.append("[r6] hasNoGoodNaturalness(X) <= hasVeryStrongCon(X).\n");
        sb.append("[r7] hasNoGoodNaturalness(X) <= hasStrongCon(X).\n");
        sb.append("[r8] hasNoGoodNaturalness(X) <= hasModerateCon(X).\n");
        sb.append("[r9] hasNoGoodNaturalness(X) <= hasWeakCon(X).\n");
        sb.append("[r10] hasNoGoodNaturalness(X) <= hasVeryWeakCon(X).\n\n\n");
        
        // PRIORISATION DES REGLES
        sb.append("%%% === PRIORISATION DES REGLES ===\n\n");
        sb.append("%%% Les arguments con forts l'emportent sur les arguments pro\n\n");
        sb.append("r6 >> r1.\n");
        sb.append("r6 >> r2.\n");
        sb.append("r6 >> r3.\n");
        sb.append("r6 >> r4.\n");
        sb.append("r6 >> r5.\n\n");
        
        sb.append("r1 >> r8.\n");
        sb.append("r1 >> r9.\n");
        sb.append("r1 >> r10.\n\n");
        
        sb.append("r2 >> r8.\n");
        sb.append("r2 >> r9.\n");
        sb.append("r2 >> r10.\n\n");
        
        // CONTRAINTES
        sb.append("%%% === CONTRAINTES ===\n\n");
        sb.append("! :- hasGoodNaturalness(X), hasNoGoodNaturalness(X).\n\n\n");
        
        // STATISTIQUES
        sb.append("%%% === STATISTIQUES ===\n");
        sb.append("%%% Arguments : ").append(arguments.size()).append("\n");
        sb.append("%%% Produits : ").append(alternatives.size()).append("\n");
        sb.append("%%% Règles défaisables : 10\n");
        sb.append("%%% Priorités : 11\n");
        sb.append("%%% Contraintes : 1\n");
        sb.append("\n\n");
        
        // EXEMPLES DE QUERY
        sb.append("%%% === EXEMPLES DE QUERY ===\n\n");
        sb.append("%%% Queries sur les Arguments :\n");
        sb.append("%%% argument(Arg), argProduct(Arg, petites_madeleines).\n");
        sb.append("%%% argument(Arg), argProduct(Arg, X), argPolarity(Arg, pro).\n");
        sb.append("%%% argument(Arg), argProduct(Arg, X), argPolarity(Arg, con).\n");
        sb.append("%%% argument(Arg), argFiability(Arg, 1).\n");
        sb.append("%%% argument(Arg), argTag(Arg, Tag).\n\n");
        
        sb.append("%%% Queries sur les Niveaux de Force :\n");
        sb.append("%%% hasVeryStrongPro(Product).\n");
        sb.append("%%% hasVeryStrongCon(Product).\n");
        sb.append("%%% hasStrongPro(Product).\n");
        sb.append("%%% hasStrongCon(Product).\n");
        sb.append("%%% argument(Arg), argProduct(Arg, Product), argPolarity(Arg, pro), veryStrongArgument(Arg).\n\n");
        
        sb.append("%%% Queries sur la Naturalité :\n");
        sb.append("%%% hasGoodNaturalness(X).\n");
        sb.append("%%% hasNoGoodNaturalness(X).\n");
        sb.append("%%% hasGoodNaturalness(X), hasNoGoodNaturalness(X).\n");
        sb.append("%%% product(X), \\+ hasGoodNaturalness(X), \\+ hasNoGoodNaturalness(X).\n\n");
        
        sb.append("%%% Queries Comparatives :\n");
        sb.append("%%% product(X), product(Y), X < Y, hasStrongPro(X), hasStrongPro(Y).\n");
        sb.append("%%% argument(Arg), argProduct(Arg, Product), argPolarity(Arg, pro).\n");
        sb.append("%%% argument(Arg), argProduct(Arg, Product), argPolarity(Arg, con).\n\n");
        
        sb.append("%%% Queries Complexes :\n");
        sb.append("%%% hasStrongPro(Product), hasStrongCon(Product).\n");
        sb.append("%%% argument(Arg1), argument(Arg2), argProduct(Arg1, Product), argProduct(Arg2, Product), argFiability(Arg1, 1), argFiability(Arg2, 1), argPolarity(Arg1, pro), argPolarity(Arg2, con).\n\n");
        
        sb.append("%%% Queries avec Exclusions :\\n");
        sb.append("%%% product(Product), argument(Arg), argProduct(Arg, Product), argPolarity(Arg, pro), \\+ (argument(Arg2), argProduct(Arg2, Product), argPolarity(Arg2, con)).\n");
        
        return sb.toString();
    }
}
