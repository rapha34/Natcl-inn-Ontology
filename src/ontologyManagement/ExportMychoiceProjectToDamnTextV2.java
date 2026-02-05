package ontologyManagement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Exporte les données d'un projet MyChoice au format texte compatible DAMN V2.
 * Version améliorée avec poids catégorisés, crédibilité des stakeholders et sévérité des tags.
 */
public class ExportMychoiceProjectToDamnTextV2 {
    
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
     * Exporte le projet MyChoice en format texte DAMN V2.
     */
    public static void exportToText(
            Map<String, ExportMychoiceProjectToDamn.AlternativeData> alternatives,
            Map<String, ExportMychoiceProjectToDamn.ArgumentData> arguments,
            String projectUri,
            String outputFile) throws IOException {
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            
            // En-tête
            writer.write("%%% === EXPORT MyChoice vers DAMN V2 ===\n");
            writer.write("%%% Projet : " + projectUri + "\n");
            writer.write("%%% Version avec poids catégorisés et crédibilité stakeholder\n");
            writer.write("%%%\n\n");
            
            // FAITS STRICTS - Produits
            writer.write("%%% === FAITS STRICTS - Produits ===\n\n");
            for (ExportMychoiceProjectToDamn.AlternativeData alt : alternatives.values()) {
                writer.write("product(" + format(alt.name) + ").\n");
            }
            writer.write("\n");
            
            // FAITS STRICTS - Arguments
            writer.write("%%% === FAITS STRICTS - Arguments ===\n\n");
            
            int argumentIndex = 1;
            Map<String, String> argumentIdMap = new HashMap<>();
            
            for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
                String argId = "arg" + argumentIndex;
                argumentIdMap.put(arg.uri, argId);
                
                writer.write("argument(" + argId + ").\n");
                
                // Alternative associée
                if (arg.alternativeName != null && !arg.alternativeName.isEmpty()) {
                    writer.write("argProduct(" + argId + "," + format(arg.alternativeName) + ").\n");
                }
                
                // Tag
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
                    writer.write("argTag(" + argId + "," + tag + ").\n");
                }
                
                // Polarité
                String polarity = "";
                if (arg.polarity != null && !arg.polarity.isEmpty()) {
                    String pol = arg.polarity.toLowerCase();
                    polarity = pol.contains("con") || pol.contains("neg") ? "con" : "pro";
                } else {
                    polarity = arg.assertion.toLowerCase().contains("harm") ? "con" : "pro";
                }
                writer.write("argPolarity(" + argId + "," + polarity + ").\n");
                
                // Stakeholder
                if (arg.stakeholder != null && !arg.stakeholder.name.isEmpty()) {
                    String stakeholder = format(arg.stakeholder.name);
                    writer.write("argStakeholder(" + argId + "," + stakeholder + ").\n");
                }
                
                // Fiabilité du type de source (si disponible)
                if (arg.sources != null && !arg.sources.isEmpty()) {
                    for (ExportMychoiceProjectToDamn.SourceData source : arg.sources) {
                        if (source.typeSourceFiability > 0) {
                            writer.write("argFiability(" + argId + "," + source.typeSourceFiability + ").\n");
                            break; // Une seule fiabilité par argument
                        }
                    }
                }
                
                writer.write("\n");
                argumentIndex++;
            }
            
            // FAITS STRICTS - Poids catégorisés
            writer.write("%%% === FAITS STRICTS - Poids catégorisés ===\n\n");
            argumentIndex = 1;
            for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
                String argId = "arg" + argumentIndex;
                
                // Catégorisation basée sur la présence de propriétés
                boolean hasStrongIndicators = (!arg.nameProperty.isEmpty() && !arg.nameCriterion.isEmpty());
                
                if (hasStrongIndicators) {
                    writer.write("highWeight(" + argId + ").\n");
                } else {
                    writer.write("mediumWeight(" + argId + ").\n");
                }
                
                argumentIndex++;
            }
            writer.write("\n");
            
            // FAITS STRICTS - Crédibilité stakeholders
            writer.write("%%% === FAITS STRICTS - Crédibilité stakeholders ===\n\n");
            writer.write("highCredibility(scientist).\n");
            writer.write("highCredibility(expert).\n");
            writer.write("mediumCredibility(journalist).\n");
            writer.write("mediumCredibility(consumer).\n");
            writer.write("lowCredibility(producer).\n");
            writer.write("lowCredibility(marketer).\n");
            writer.write("\n");
            
            // FAITS STRICTS - Sévérité des tags
            writer.write("%%% === FAITS STRICTS - Sévérité des tags ===\n\n");
            writer.write("criticalTag(conservateur_artificiel).\n");
            writer.write("criticalTag(additif_synthetique).\n");
            writer.write("highSeverityTag(colorant_artificiel).\n");
            writer.write("highSeverityTag(aromatisant_artificiel).\n");
            writer.write("mediumSeverityTag(sans_conservateur).\n");
            writer.write("mediumSeverityTag(sans_colorant).\n");
            writer.write("\n");
            
            // RÈGLES DÉFAISABLES
            writer.write("%%% === REGLES DEFAISABLES ===\n\n");
            
            writer.write("strongArgument(Arg) <= highWeight(Arg).\n");
            writer.write("strongArgument(Arg) <= argStakeholder(Arg,Stakeholder), highCredibility(Stakeholder).\n");
            writer.write("\n");
            
            writer.write("criticalArgument(Arg) <= argTag(Arg,Tag), criticalTag(Tag).\n");
            writer.write("\n");
            
            writer.write("hasStrongPro(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), strongArgument(Arg).\n");
            writer.write("hasStrongCon(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), strongArgument(Arg).\n");
            writer.write("hasCriticalCon(Product) <= argument(Arg), argProduct(Arg,Product), criticalArgument(Arg).\n");
            writer.write("\n");
            
            writer.write("hasPro(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro).\n");
            writer.write("hasCon(Product) <= argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con).\n");
            writer.write("\n");
            
            writer.write("naturalProduct(Product) <= hasPro(Product).\n");
            writer.write("notNaturalProduct(Product) <= hasStrongCon(Product).\n");
            writer.write("notNaturalProduct(Product) <= hasCriticalCon(Product).\n");
            writer.write("\n");
            
            writer.write("support(Product) <= naturalProduct(Product).\n");
            writer.write("avoid(Product) <= notNaturalProduct(Product).\n");
            writer.write("\n");
            
            // CONTRAINTES
            writer.write("%%% === CONTRAINTES ===\n\n");
            writer.write("! :- naturalProduct(X), notNaturalProduct(X).\n");
            writer.write("! :- support(X), avoid(X).\n");
            writer.write("\n");
            
            // STATISTIQUES
            writer.write("%%% === STATISTIQUES ===\n");
            writer.write("%%% Arguments : " + arguments.size() + "\n");
            writer.write("%%% Alternatives : " + alternatives.size() + "\n");
            writer.write("%%% Règles défaisables : 11\n");
            writer.write("%%% Contraintes : 2\n");
        }
        
        System.out.println("Export texte DAMN V2 réussi : " + outputFile);
        System.out.println("  - " + arguments.size() + " arguments exportés");
    }
}
