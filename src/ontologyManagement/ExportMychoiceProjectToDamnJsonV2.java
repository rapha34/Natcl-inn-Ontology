package ontologyManagement;

import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Exporte les données d'un projet MyChoice au format JSON compatible DAMN V2.
 * Version améliorée avec poids catégorisés, crédibilité des stakeholders et sévérité des tags.
 */
public class ExportMychoiceProjectToDamnJsonV2 {
    
    private static ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Extrait un nom lisible à partir de l'URI du projet.
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
        
        str = Normalizer.normalize(str, Normalizer.Form.NFD)
            .toLowerCase()
            .replaceAll("'", "_")
            .replaceAll("\\s", "_")
            .replaceAll("[^a-z0-9_]", "")
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        while (!str.isEmpty() && str.charAt(0) == '_') {
            str = str.substring(1);
        }
        
        return str;
    }
    
    /**
     * Exporte le projet MyChoice en format JSON DAMN V2.
     */
    public static void exportToJson(
            Map<String, ExportMychoiceProjectToDamn.AlternativeData> alternatives,
            Map<String, ExportMychoiceProjectToDamn.ArgumentData> arguments,
            String projectUri,
            String outputFile) throws IOException {
        
        // Générer le texte DAMN V2
        String dlgpContent = generateDamnTextV2(alternatives, arguments, projectUri);
        
        // Créer la structure JSON
        ObjectNode root = mapper.createObjectNode();
        
        root.put("id", "natclinn_damn_v2_project");
        root.put("name", extractProjectName(projectUri) + " (DAMN V2)");
        root.put("description", "Damn V2 project from Natclinn with weighted arguments\n");
        root.put("semantic", "PDLwithoutTD");
        root.put("query", "");
        root.put("creator_id", "natclinn_creator");
        
        ArrayNode contributors = mapper.createArrayNode();
        contributors.add("natclinn_creator");
        root.set("contributors", contributors);
        
        ArrayNode kbs = mapper.createArrayNode();
        ObjectNode kb = mapper.createObjectNode();
        kb.put("id", "natclinn_kb_v2");
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
        
        try (FileWriter fw = new FileWriter(outputFile)) {
            fw.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        }
        
        System.out.println("Export JSON DAMN V2 réussi : " + outputFile);
        System.out.println("  - " + arguments.size() + " arguments exportés");
    }
    
    /**
     * Génère le contenu texte DAMN V2 avec poids catégorisés et nouvelles règles.
     */
    private static String generateDamnTextV2(
            Map<String, ExportMychoiceProjectToDamn.AlternativeData> alternatives,
            Map<String, ExportMychoiceProjectToDamn.ArgumentData> arguments,
            String projectUri) {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("%%% === EXPORT MyChoice vers DAMN V2 ===\n");
        sb.append("%%% Projet : ").append(projectUri).append("\n");
        sb.append("%%% Version avec poids catégorisés et crédibilité stakeholder\n");
        sb.append("%%%\n\n");
        
        // FAITS STRICTS - Produits (alternatives)
        sb.append("%%% === FAITS STRICTS - Produits ===\n\n");
        for (ExportMychoiceProjectToDamn.AlternativeData alt : alternatives.values()) {
            sb.append("product(").append(format(alt.name)).append(").\n");
        }
        sb.append("\n");
        
        // FAITS STRICTS - Arguments
        sb.append("%%% === FAITS STRICTS - Arguments ===\n\n");
        
        int argumentIndex = 1;
        Map<String, String> argumentIdMap = new HashMap<>();
        
        for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
            String argId = "arg" + argumentIndex;
            argumentIdMap.put(arg.uri, argId);
            
            sb.append("argument(").append(argId).append(").\n");
            
            // Alternative associée
            if (arg.alternativeName != null && !arg.alternativeName.isEmpty()) {
                sb.append("argProduct(").append(argId).append(",").append(format(arg.alternativeName)).append(").\n");
            }
            
            // Tag (nameProperty ou nameCriterion)
            String tag = "";
            if (!arg.nameProperty.isEmpty()) {
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
            
            // Polarité pro/con
            String polarity = "";
            if (arg.polarity != null && !arg.polarity.isEmpty()) {
                String pol = arg.polarity.toLowerCase();
                polarity = pol.contains("con") || pol.contains("neg") ? "con" : "pro";
            } else {
                polarity = arg.assertion.toLowerCase().contains("harm") ? "con" : "pro";
            }
            sb.append("argPolarity(").append(argId).append(",").append(polarity).append(").\n");
            
            // Stakeholder
            if (arg.stakeholder != null && !arg.stakeholder.name.isEmpty()) {
                String stakeholder = format(arg.stakeholder.name);
                sb.append("argStakeholder(").append(argId).append(",").append(stakeholder).append(").\n");
            }
            
            // Fiabilité du type de source (si disponible)
            if (arg.sources != null && !arg.sources.isEmpty()) {
                for (ExportMychoiceProjectToDamn.SourceData source : arg.sources) {
                    if (source.typeSourceFiability > 0) {
                        sb.append("argFiability(").append(argId).append(",").append(source.typeSourceFiability).append(").\n");
                        break; // Une seule fiabilité par argument
                    }
                }
            }
            
            sb.append("\n");
            argumentIndex++;
        }
        
        // FAITS STRICTS - Poids catégorisés des arguments
        sb.append("%%% === FAITS STRICTS - Poids catégorisés ===\n\n");
        argumentIndex = 1;
        for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
            String argId = "arg" + argumentIndex;
            
            // Catégorisation simple basée sur la présence de propriétés
            boolean hasStrongIndicators = (!arg.nameProperty.isEmpty() && !arg.nameCriterion.isEmpty());
            
            if (hasStrongIndicators) {
                sb.append("highWeight(").append(argId).append(").\n");
            } else {
                sb.append("mediumWeight(").append(argId).append(").\n");
            }
            
            argumentIndex++;
        }
        sb.append("\n");
        
        // FAITS STRICTS - Crédibilité stakeholders
        sb.append("%%% === FAITS STRICTS - Crédibilité stakeholders ===\n\n");
        sb.append("highCredibility(scientist).\n");
        sb.append("highCredibility(expert).\n");
        sb.append("mediumCredibility(journalist).\n");
        sb.append("mediumCredibility(consumer).\n");
        sb.append("lowCredibility(producer).\n");
        sb.append("lowCredibility(marketer).\n");
        sb.append("\n");
        
        // FAITS STRICTS - Sévérité des tags
        sb.append("%%% === FAITS STRICTS - Sévérité des tags ===\n\n");
        sb.append("criticalTag(conservateur_artificiel).\n");
        sb.append("criticalTag(additif_synthetique).\n");
        sb.append("highSeverityTag(colorant_artificiel).\n");
        sb.append("highSeverityTag(aromatisant_artificiel).\n");
        sb.append("mediumSeverityTag(sans_conservateur).\n");
        sb.append("mediumSeverityTag(sans_colorant).\n");
        sb.append("\n");
        
        // RÈGLES DÉFAISABLES
        sb.append("%%% === REGLES DEFAISABLES ===\n\n");
        
        sb.append("strongArgument(Arg) <= highWeight(Arg).\n");
        sb.append("strongArgument(Arg) <= argStakeholder(Arg,Stakeholder), highCredibility(Stakeholder).\n");
        sb.append("\n");
        
        sb.append("criticalArgument(Arg) <= argTag(Arg,Tag), criticalTag(Tag).\n");
        sb.append("\n");
        
        sb.append("hasStrongPro(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), strongArgument(Arg).\n");
        sb.append("hasStrongCon(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), strongArgument(Arg).\n");
        sb.append("hasCriticalCon(Product) <= argument(Arg), argProduct(Arg,Product), criticalArgument(Arg).\n");
        sb.append("\n");
        
        sb.append("hasPro(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro).\n");
        sb.append("hasCon(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con).\n");
        sb.append("\n");
        
        sb.append("naturalProduct(Product) <= hasPro(Product).\n");
        sb.append("notNaturalProduct(Product) <= hasStrongCon(Product).\n");
        sb.append("notNaturalProduct(Product) <= hasCriticalCon(Product).\n");
        sb.append("\n");
        
        sb.append("support(Product) <= naturalProduct(Product).\n");
        sb.append("avoid(Product) <= notNaturalProduct(Product).\n");
        sb.append("\n");
        
        // CONTRAINTES
        sb.append("%%% === CONTRAINTES ===\n\n");
        sb.append("! :- naturalProduct(X), notNaturalProduct(X).\n");
        sb.append("! :- support(X), avoid(X).\n");
        sb.append("\n");
        
        // STATISTIQUES
        sb.append("%%% === STATISTIQUES ===\n");
        sb.append("%%% Arguments : ").append(arguments.size()).append("\n");
        sb.append("%%% Alternatives : ").append(alternatives.size()).append("\n");
        sb.append("%%% Règles défaisables : 11\n");
        sb.append("%%% Contraintes : 2\n");
        
        return sb.toString();
    }
}
