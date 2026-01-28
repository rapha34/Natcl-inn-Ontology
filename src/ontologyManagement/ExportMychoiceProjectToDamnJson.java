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
        
        // Créer la structure JSON principale
        ObjectNode root = mapper.createObjectNode();
        
        // Métadonnées du projet
        root.put("id", "natclinn_damn_project");
        root.put("name", extractProjectName(projectUri));
        root.put("description", "Damn project from Natclinn project\n");
        root.put("semantic", "PDLwithoutTD");
        root.put("query", "");
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
        sb.append("%%% EXPORT MyChoice vers DAMN\n");
        sb.append("%%% Projet : ").append(projectUri).append("\n");
        sb.append("%%% Format : Defeasible Reasoning Tool for Multi-Agent Reasoning\n");
        sb.append("%%% Ce fichier contient les faits, règles et contraintes générés\n");
        sb.append("%%%\n");
        sb.append("\n");
        
        // Section FAITS
        sb.append("%%% === FAITS : Déclaration des arguments et leurs propriétés ===\n");
        sb.append("\n");
        
        int argumentIndex = 1;
        Map<String, String> argumentIdMap = new HashMap<>();
        
        for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
            String argId = "a" + argumentIndex;
            argumentIdMap.put(arg.uri, argId);
            
            // Déclaration de l'argument
            sb.append("argument(").append(argId).append(").\n");
            
            // Propriétés de base
            if (arg.stakeholder != null && !arg.stakeholder.name.isEmpty()) {
                sb.append("nameStakeHolder(").append(argId).append(",").append(format(arg.stakeholder.name)).append(").\n");
            }
            
            // Nom de l'alternative associée
            if (arg.alternativeName != null && !arg.alternativeName.isEmpty()) {
                sb.append("nameAlternative(").append(argId).append(",").append(format(arg.alternativeName)).append(").\n");
            } else if (!arg.value.isEmpty()) {
                sb.append("nameAlternative(").append(argId).append(",").append(format(arg.value)).append(").\n");
            }
            
            // Type pro/con
            String procon = arg.assertion.toLowerCase().contains("harm") ? "con" : "pro";
            sb.append("typeProCon(").append(argId).append(",").append(procon).append(").\n");
            
            // Critère
            if (!arg.nameCriterion.isEmpty()) {
                sb.append("nameCriterion(").append(argId).append(",").append(format(arg.nameCriterion)).append(").\n");
            }
            
            // Aim
            if (!arg.assertion.isEmpty()) {
                sb.append("aim(").append(argId).append(",").append(format(arg.assertion)).append(").\n");
            }
            
            // Property
            if (!arg.nameProperty.isEmpty()) {
                sb.append("nameProperty(").append(argId).append(",").append(format(arg.nameProperty)).append(").\n");
            }
            
            // Value
            if (!arg.value.isEmpty()) {
                sb.append("value(").append(argId).append(",").append(format(arg.value)).append(").\n");
            }
            
            // Assertion
            if (!arg.assertion.isEmpty()) {
                sb.append("assertion(").append(argId).append(",").append(format(arg.assertion)).append(").\n");
            }
            
            // Explication
            if (!arg.explanation.isEmpty()) {
                sb.append("explanation(").append(argId).append(",").append(format(arg.explanation)).append(").\n");
            }
            
            // Prospectif
            int prospective = arg.isProspective.isEmpty() ? 0 : Integer.parseInt(arg.isProspective);
            sb.append("isProspective(").append(argId).append(",").append(prospective).append(").\n");
            
            // Date
            if (!arg.date.isEmpty()) {
                sb.append("date(").append(argId).append(",").append(format(arg.date)).append(").\n");
            }
            
            // Sources
            if (arg.sources != null && !arg.sources.isEmpty()) {
                for (ExportMychoiceProjectToDamn.SourceData source : arg.sources) {
                    if (!source.name.isEmpty()) {
                        sb.append("nameSource(").append(argId).append(",").append(format(source.name)).append(").\n");
                    }
                    if (!source.typeSource.isEmpty()) {
                        sb.append("nameTypeSource(").append(argId).append(",").append(format(source.typeSource)).append(").\n");
                    }
                }
            }
            
            sb.append("\n");
            argumentIndex++;
        }
        
        // Section RÈGLES
        sb.append("%%% === RÈGLES : Raisonnement défaisable ===\n");
        sb.append("\n");
        sb.append("serves(ALT,AIM,PROP) <= argument(ARG), nameAlternative(ARG,ALT), typeProCon(ARG,pro), aim(ARG,AIM), nameProperty(ARG,PROP).\n");
        sb.append("harms(ALT,AIM,PROP) <= argument(ARG), nameAlternative(ARG,ALT), typeProCon(ARG,con), aim(ARG,AIM), nameProperty(ARG,PROP).\n");
        sb.append("hasCriterion(AIM,CRIT) <= argument(ARG), aim(ARG,AIM), nameCriterion(ARG,CRIT).\n");
        sb.append("serves(ALT,CRIT,PROP) <= serves(ALT,AIM,PROP), hasCriterion(AIM,CRIT).\n");
        sb.append("harms(ALT,CRIT,PROP) <= harms(ALT,AIM,PROP), hasCriterion(AIM,CRIT).\n");
        sb.append("support(ALT) <= serves(ALT,CRIT,PROP).\n");
        sb.append("avoid(ALT) <= harms(ALT,CRIT,PROP).\n");
        sb.append("\n");
        
        // Section CONTRAINTES
        sb.append("%%% === CONTRAINTES : Inconsistances et contradictions ===\n");
        sb.append("\n");
        sb.append("! :- serves(ALT,AIM,PROP), harms(ALT,AIM,PROP).\n");
        sb.append("! :- serves(ALT,CRIT,PROP), harms(ALT,CRIT,PROP).\n");
        sb.append("! :- support(ALT), avoid(ALT).\n");
        sb.append("\n");
        
        // Statistiques
        sb.append("%%% === STATISTIQUES ===\n");
        sb.append("%%% Arguments : ").append(arguments.size()).append("\n");
        sb.append("%%% Alternatives : ").append(alternatives.size()).append("\n");
        sb.append("%%% Règles : 7\n");
        sb.append("%%% Contraintes : 3\n");
        
        return sb.toString();
    }
}
