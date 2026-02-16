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
            bw.write("%%% === EXPORT MyChoice vers DAMN ===\n");
            bw.write("%%% Projet : " + projectUri + "\n");
            bw.write("%%%\n\n");
            
            // FAITS STRICTS - Produits (toutes les alternatives)
            bw.write("%%% === FAITS STRICTS - Produits ===\n\n");
            for (ExportMychoiceProjectToDamn.AlternativeData alt : alternatives.values()) {
                bw.write("product(" + format(alt.name) + ").\n");
            }
            bw.write("\n");
            
            // FAITS STRICTS - Arguments
            bw.write("%%% === FAITS STRICTS - Arguments ===\n\n");
            
            int argumentIndex = 1;
            for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
                String argId = "arg" + argumentIndex;
                
                bw.write("argument(" + argId + ").\n");
                bw.write("argProduct(" + argId + "," + format(arg.alternativeName) + ").\n");
                
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
                    bw.write("argTag(" + argId + "," + tag + ").\n");
                }
                
                // Polarité
                if (arg.polarity != null && !arg.polarity.isEmpty()) {
                    String polarity = arg.polarity.equals("-") ? "con" : "pro";
                    bw.write("argPolarity(" + argId + "," + polarity + ").\n");
                }
                
                // Fiabilité
                if (arg.sources != null && !arg.sources.isEmpty()) {
                    for (ExportMychoiceProjectToDamn.SourceData source : arg.sources) {
                        if (source.typeSourceFiability > 0) {
                            bw.write("argFiability(" + argId + "," + source.typeSourceFiability + ").\n");
                            break;
                        }
                    }
                }
                
                bw.write("\n");
                argumentIndex++;
            }
            bw.write("\n");
            
            // REGLES STRICTS
            bw.write("%%% === REGLES STRICTS  ===\n\n");
            bw.write("%%% Niveaux de force des arguments basés sur la fiabilité\n");
            bw.write("%%% 1: très fiable, 2: fiable, 3: moins fiable, 4: peu fiable, 5: très peu fiable\n\n");
            bw.write("veryStrongArgument(Arg) :- argFiability(Arg,1).\n");
            bw.write("strongArgument(Arg) :- argFiability(Arg,2).\n");
            bw.write("moderateArgument(Arg) :- argFiability(Arg,3).\n");
            bw.write("weakArgument(Arg) :- argFiability(Arg,4).\n");
            bw.write("veryWeakArgument(Arg) :- argFiability(Arg,5).\n\n");
            
            bw.write("hasVeryStrongPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), veryStrongArgument(Arg).\n");
            bw.write("hasStrongPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), strongArgument(Arg).\n");
            bw.write("hasModeratePro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), moderateArgument(Arg).\n");
            bw.write("hasWeakPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), weakArgument(Arg).\n");
            bw.write("hasVeryWeakPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), veryWeakArgument(Arg).\n\n");
            
            bw.write("hasVeryStrongCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), veryStrongArgument(Arg).\n");
            bw.write("hasStrongCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), strongArgument(Arg).\n");
            bw.write("hasModerateCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), moderateArgument(Arg).\n");
            bw.write("hasWeakCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), weakArgument(Arg).\n");
            bw.write("hasVeryWeakCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), veryWeakArgument(Arg).\n\n");
            
            // REGLES DEFAISABLES
            bw.write("%%% === REGLES DEFAISABLES ===\n\n");
            bw.write("[r1] hasGoodNaturalness(X) <= hasVeryStrongPro(X).\n");
            bw.write("[r2] hasGoodNaturalness(X) <= hasStrongPro(X).\n");
            bw.write("[r3] hasGoodNaturalness(X) <= hasModeratePro(X).\n");
            bw.write("[r4] hasGoodNaturalness(X) <= hasWeakPro(X).\n");
            bw.write("[r5] hasGoodNaturalness(X) <= hasVeryWeakPro(X).\n\n");
            
            bw.write("[r6] hasNoGoodNaturalness(X) <= hasVeryStrongCon(X).\n");
            bw.write("[r7] hasNoGoodNaturalness(X) <= hasStrongCon(X).\n");
            bw.write("[r8] hasNoGoodNaturalness(X) <= hasModerateCon(X).\n");
            bw.write("[r9] hasNoGoodNaturalness(X) <= hasWeakCon(X).\n");
            bw.write("[r10] hasNoGoodNaturalness(X) <= hasVeryWeakCon(X).\n\n\n");
            
            // PRIORISATION DES REGLES
            bw.write("%%% === PRIORISATION DES REGLES ===\n\n");
            bw.write("%%% Les arguments con forts l'emportent sur les arguments pro\n\n");
            bw.write("r6 >> r1.\n");
            bw.write("r6 >> r2.\n");
            bw.write("r6 >> r3.\n");
            bw.write("r6 >> r4.\n");
            bw.write("r6 >> r5.\n\n");
            
            bw.write("r1 >> r8.\n");
            bw.write("r1 >> r9.\n");
            bw.write("r1 >> r10.\n\n");
            
            bw.write("r2 >> r8.\n");
            bw.write("r2 >> r9.\n");
            bw.write("r2 >> r10.\n\n");
            
            // CONTRAINTES
            bw.write("%%% === CONTRAINTES ===\n\n");
            bw.write("! :- hasGoodNaturalness(X), hasNoGoodNaturalness(X).\n\n\n");
            
            // STATISTIQUES
            bw.write("%%% === STATISTIQUES ===\n");
            bw.write("%%% Arguments : " + arguments.size() + "\n");
            bw.write("%%% Produits : " + alternatives.size() + "\n");
            bw.write("%%% Règles défaisables : 10\n");
            bw.write("%%% Priorités : 11\n");
            bw.write("%%% Contraintes : 1\n");
            bw.write("\n\n");
            
            // EXEMPLES DE QUERY
            bw.write("%%% === EXEMPLES DE QUERY ===\n\n");
            bw.write("%%% Queries sur les Arguments :\n");
            bw.write("%%% argument(Arg), argProduct(Arg, petites_madeleines).\n");
            bw.write("%%% argument(Arg), argProduct(Arg, X), argPolarity(Arg, pro).\n");
            bw.write("%%% argument(Arg), argProduct(Arg, X), argPolarity(Arg, con).\n");
            bw.write("%%% argument(Arg), argFiability(Arg, 1).\n");
            bw.write("%%% argument(Arg), argTag(Arg, Tag).\n\n");
            
            bw.write("%%% Queries sur les Niveaux de Force :\n");
            bw.write("%%% hasVeryStrongPro(Product).\n");
            bw.write("%%% hasVeryStrongCon(Product).\n");
            bw.write("%%% hasStrongPro(Product).\n");
            bw.write("%%% hasStrongCon(Product).\n");
            bw.write("%%% argument(Arg), argProduct(Arg, Product), argPolarity(Arg, pro), veryStrongArgument(Arg).\n\n");
            
            bw.write("%%% Queries sur la Naturalité :\n");
            bw.write("%%% hasGoodNaturalness(X).\n");
            bw.write("%%% hasNoGoodNaturalness(X).\n");
            bw.write("%%% hasGoodNaturalness(X), hasNoGoodNaturalness(X).\n");
            bw.write("%%% product(X), \\+ hasGoodNaturalness(X), \\+ hasNoGoodNaturalness(X).\n\n");
            
            bw.write("%%% Queries Comparatives :\n");
            bw.write("%%% product(X), product(Y), X < Y, hasStrongPro(X), hasStrongPro(Y).\n");
            bw.write("%%% argument(Arg), argProduct(Arg, Product), argPolarity(Arg, pro).\n");
            bw.write("%%% argument(Arg), argProduct(Arg, Product), argPolarity(Arg, con).\n\n");
            
            bw.write("%%% Queries Complexes :\n");
            bw.write("%%% hasStrongPro(Product), hasStrongCon(Product).\n");
            bw.write("%%% argument(Arg1), argument(Arg2), argProduct(Arg1, Product), argProduct(Arg2, Product), argFiability(Arg1, 1), argFiability(Arg2, 1), argPolarity(Arg1, pro), argPolarity(Arg2, con).\n\n");
            
            bw.write("%%% Queries avec Exclusions :\n");
            bw.write("%%% product(Product), argument(Arg), argProduct(Arg, Product), argPolarity(Arg, pro), \\+ (argument(Arg2), argProduct(Arg2, Product), argPolarity(Arg2, con)).\n");
            
        }
        
        System.out.println("Export texte DAMN réussi : " + outputFile);
        System.out.println("  - " + arguments.size() + " arguments exportés");
        System.out.println("  - 10 règles défaisables incluses");
        System.out.println("  - 11 priorités incluses");
        System.out.println("  - 1 contrainte incluse");
    }
}
