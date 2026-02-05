package ontologyManagement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Exporte les données d'un projet MyChoice au format texte DAMN.
 * Utilise la syntaxe DAMN avec prédicats binaires (format compatible avec DamnArgument.java).
 * 
 * Format de sortie :
 * 1. Faits : déclarations des arguments et leurs propriétés
 * 2. Règles : règles de raisonnement défaisable
 * 3. Contraintes : inconsistances et contradictions
 */
public class ExportMychoiceProjectToDamnText {
    
    /**
     * Formate une chaîne pour qu'elle soit compatible avec la syntaxe DAMN.
     * - Conversion en minuscules
     * - Suppression des accents et caractères spéciaux
     * - Remplacement des espaces par des underscores
     * - Remplacement des apostrophes par des underscores
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
     * Exporte le projet MyChoice en format texte DAMN.
     */
    public static void exportToText(
            Map<String, ExportMychoiceProjectToDamn.AlternativeData> alternatives,
            Map<String, ExportMychoiceProjectToDamn.ArgumentData> arguments,
            String projectUri,
            String outputFile) throws IOException {
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            
            // En-tête
            bw.write("%%% EXPORT MyChoice vers DAMN\n");
            bw.write("%%% Projet : " + projectUri + "\n");
            bw.write("%%% Format : Defeasible Reasoning Tool for Multi-Agent Reasoning\n");
            bw.write("%%% Ce fichier contient les faits, règles et contraintes générés\n");
            bw.write("%%%\n");
            bw.write("\n");
            
            // Section FAITS
            bw.write("%%% === FAITS : Déclaration des arguments et leurs propriétés ===\n");
            bw.write("\n");
            
            int argumentIndex = 1;
            Map<String, String> argumentIdMap = new HashMap<>(); // Mapping URI -> a1, a2, etc.
            
            for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
                String argId = "a" + argumentIndex;
                argumentIdMap.put(arg.uri, argId);
                
                // Déclaration de l'argument
                bw.write("argument(" + argId + ").\n");
                
                // Propriétés de base
                if (arg.stakeholder != null && !arg.stakeholder.name.isEmpty()) {
                    bw.write("nameStakeHolder(" + argId + "," + format(arg.stakeholder.name) + ").\n");
                }
                
                // Utiliser le nom de l'alternative associée à cet argument
                if (arg.alternativeName != null && !arg.alternativeName.isEmpty()) {
                    bw.write("nameAlternative(" + argId + "," + format(arg.alternativeName) + ").\n");
                } else if (!arg.value.isEmpty()) {
                    // Fallback sur value si alternativeName n'est pas disponible
                    bw.write("nameAlternative(" + argId + "," + format(arg.value) + ").\n");
                }
                
                // Type pro/con - déterminé par la présence de "harms" ou "serves" dans l'assertion
                String procon = arg.assertion.toLowerCase().contains("harm") ? "con" : "pro";
                bw.write("typeProCon(" + argId + "," + procon + ").\n");
                
                // Critère - utiliser nameCriterion s'il existe
                if (!arg.nameCriterion.isEmpty()) {
                    bw.write("nameCriterion(" + argId + "," + format(arg.nameCriterion) + ").\n");
                }
                
                // Aim (objectif) - extrait de l'assertion ou du value
                if (!arg.assertion.isEmpty()) {
                    bw.write("aim(" + argId + "," + format(arg.assertion) + ").\n");
                }
                
                // Property - utiliser nameProperty s'il existe
                if (!arg.nameProperty.isEmpty()) {
                    bw.write("nameProperty(" + argId + "," + format(arg.nameProperty) + ").\n");
                }
                
                // Valeur
                if (!arg.value.isEmpty()) {
                    bw.write("value(" + argId + "," + format(arg.value) + ").\n");
                }
                
                // Assertion
                if (!arg.assertion.isEmpty()) {
                    bw.write("assertion(" + argId + "," + format(arg.assertion) + ").\n");
                }
                
                // Explication
                if (!arg.explanation.isEmpty()) {
                    bw.write("explanation(" + argId + "," + format(arg.explanation) + ").\n");
                }
                
                // Prospectif
                int prospective = arg.isProspective.isEmpty() ? 0 : Integer.parseInt(arg.isProspective);
                bw.write("isProspective(" + argId + "," + prospective + ").\n");
                
                // Date
                if (!arg.date.isEmpty()) {
                    bw.write("date(" + argId + "," + format(arg.date) + ").\n");
                }
                
                // Sources
                if (arg.sources != null && !arg.sources.isEmpty()) {
                    for (ExportMychoiceProjectToDamn.SourceData source : arg.sources) {
                        if (!source.name.isEmpty()) {
                            bw.write("nameSource(" + argId + "," + format(source.name) + ").\n");
                        }
                        if (!source.typeSource.isEmpty()) {
                            bw.write("nameTypeSource(" + argId + "," + format(source.typeSource) + ").\n");
                        }
                        if (source.typeSourceFiability > 0) {
                            bw.write("typeSourceFiability(" + argId + "," + source.typeSourceFiability + ").\n");
                        }
                    }
                }
                
                bw.write("\n");
                argumentIndex++;
            }
            
            // Section RÈGLES
            bw.write("%%% === RÈGLES : Raisonnement défaisable ===\n");
            bw.write("\n");
            
            bw.write("serves(ALT,AIM,PROP) <= argument(ARG), nameAlternative(ARG,ALT), typeProCon(ARG,pro), aim(ARG,AIM), nameProperty(ARG,PROP).\n");
            bw.write("harms(ALT,AIM,PROP) <= argument(ARG), nameAlternative(ARG,ALT), typeProCon(ARG,con), aim(ARG,AIM), nameProperty(ARG,PROP).\n");
            bw.write("hasCriterion(AIM,CRIT) <= argument(ARG), aim(ARG,AIM), nameCriterion(ARG,CRIT).\n");
            bw.write("serves(ALT,CRIT,PROP) <= serves(ALT,AIM,PROP), hasCriterion(AIM,CRIT).\n");
            bw.write("harms(ALT,CRIT,PROP) <= harms(ALT,AIM,PROP), hasCriterion(AIM,CRIT).\n");
            bw.write("support(ALT) <= serves(ALT,CRIT,PROP).\n");
            bw.write("avoid(ALT) <= harms(ALT,CRIT,PROP).\n");
            bw.write("\n");
            
            // Section CONTRAINTES
            bw.write("%%% === CONTRAINTES : Inconsistances et contradictions ===\n");
            bw.write("\n");
            
            bw.write("! :- serves(ALT,AIM,PROP), harms(ALT,AIM,PROP).\n");
            bw.write("! :- serves(ALT,CRIT,PROP), harms(ALT,CRIT,PROP).\n");
            bw.write("! :- support(ALT), avoid(ALT).\n");
            bw.write("\n");
            
            // Statistiques en commentaire
            bw.write("%%% === STATISTIQUES ===\n");
            bw.write("%%% Arguments : " + arguments.size() + "\n");
            bw.write("%%% Alternatives : " + alternatives.size() + "\n");
            bw.write("%%% Règles : 7\n");
            bw.write("%%% Contraintes : 3\n");
            
        }
        
        System.out.println("Export texte DAMN réussi : " + outputFile);
        System.out.println("  - " + arguments.size() + " arguments exportés");
        System.out.println("  - 7 règles incluses");
        System.out.println("  - 3 contraintes incluses");
    }
}
