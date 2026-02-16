package ontologyManagement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exporte les données d'un projet MyChoice au format texte compatible DAMN.
 * Génère un fichier texte par produit (alternative).
 */
public class ExportMychoiceProjectToDamnTextByAlternative {
    
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
     * Exporte le projet MyChoice en format texte DAMN V3 - un fichier par produit.
     */
    public static void exportToText(
            Map<String, ExportMychoiceProjectToDamn.AlternativeData> alternatives,
            Map<String, ExportMychoiceProjectToDamn.ArgumentData> arguments,
            String projectUri,
            String outputFileBase) throws IOException {
        
        // Pour chaque alternative (produit), créer un fichier séparé
        for (ExportMychoiceProjectToDamn.AlternativeData alternative : alternatives.values()) {
            String productName = format(alternative.name);
            
            // Filtrer les arguments concernant ce produit
            List<ExportMychoiceProjectToDamn.ArgumentData> productArguments = new ArrayList<>();
            for (ExportMychoiceProjectToDamn.ArgumentData arg : arguments.values()) {
                if (arg.alternativeName != null && arg.alternativeName.equals(alternative.name)) {
                    productArguments.add(arg);
                }
            }
            
            // Créer le nom de fichier avec le nom du produit
            String outputFile = outputFileBase.replace("_damn_by_alternative.txt", "-" + productName + "_damn.txt");
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                
                // En-tête
                writer.write("%%% === EXPORT MyChoice vers DAMN ===\n");
                writer.write("%%% Projet : " + projectUri + "\n");
                writer.write("%%% Scénario pour le produit : " + alternative.name + "\n");
                writer.write("%%%\n\n");
                
                // FAITS STRICTS - Produit
                writer.write("%%% === FAITS STRICTS - Produit ===\n\n");
                writer.write("product(" + format(alternative.name) + ").\n");
                writer.write("\n");
                
                // FAITS STRICTS - Arguments
                writer.write("%%% === FAITS STRICTS - Arguments ===\n\n");
                
                int argumentIndex = 1;
                
                for (ExportMychoiceProjectToDamn.ArgumentData arg : productArguments) {
                    String argId = "arg" + argumentIndex;
                    
                    writer.write("argument(" + argId + ").\n");
                    writer.write("argProduct(" + argId + "," + format(alternative.name) + ").\n");
                    
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
                        writer.write("argTag(" + argId + "," + tag + ").\n");
                    }
                    
                    // Polarité
                    if (arg.polarity != null && !arg.polarity.isEmpty()) {
                        String polarity = arg.polarity.equals("-") ? "con" : "pro";
                        writer.write("argPolarity(" + argId + "," + polarity + ").\n");
                    }
                    
                    // Fiabilité
                    if (arg.sources != null && !arg.sources.isEmpty()) {
                        for (ExportMychoiceProjectToDamn.SourceData source : arg.sources) {
                            if (source.typeSourceFiability > 0) {
                                writer.write("argFiability(" + argId + "," + source.typeSourceFiability + ").\n");
                                break;
                            }
                        }
                    }
                    
                    writer.write("\n");
                    argumentIndex++;
                }
                writer.write("\n");
                
                // REGLES STRICTS
                writer.write("%%% === REGLES STRICTS  ===\n\n");
                writer.write("%%% Niveaux de force des arguments basés sur la fiabilité\n");
                writer.write("%%% 1: très fiable, 2: fiable, 3: moins fiable, 4: peu fiable, 5: très peu fiable\n\n");
                writer.write("veryStrongArgument(Arg) :- argFiability(Arg,1).\n");
                writer.write("strongArgument(Arg) :- argFiability(Arg,2).\n");
                writer.write("moderateArgument(Arg) :- argFiability(Arg,3).\n");
                writer.write("weakArgument(Arg) :- argFiability(Arg,4).\n");
                writer.write("veryWeakArgument(Arg) :- argFiability(Arg,5).\n\n");
                
                writer.write("hasVeryStrongPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), veryStrongArgument(Arg).\n");
                writer.write("hasStrongPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), strongArgument(Arg).\n");
                writer.write("hasModeratePro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), moderateArgument(Arg).\n");
                writer.write("hasWeakPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), weakArgument(Arg).\n");
                writer.write("hasVeryWeakPro(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,pro), veryWeakArgument(Arg).\n\n");
                
                writer.write("hasVeryStrongCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), veryStrongArgument(Arg).\n");
                writer.write("hasStrongCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), strongArgument(Arg).\n");
                writer.write("hasModerateCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), moderateArgument(Arg).\n");
                writer.write("hasWeakCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), weakArgument(Arg).\n");
                writer.write("hasVeryWeakCon(Product) :- argument(Arg), argProduct(Arg,Product), argPolarity(Arg,con), veryWeakArgument(Arg).\n\n");
                
                // REGLES DEFAISABLES
                writer.write("%%% === REGLES DEFAISABLES ===\n\n");
                writer.write("[r1] hasGoodNaturalness(X) <= hasVeryStrongPro(X).\n");
                writer.write("[r2] hasGoodNaturalness(X) <= hasStrongPro(X).\n");
                writer.write("[r3] hasGoodNaturalness(X) <= hasModeratePro(X).\n");
                writer.write("[r4] hasGoodNaturalness(X) <= hasWeakPro(X).\n");
                writer.write("[r5] hasGoodNaturalness(X) <= hasVeryWeakPro(X).\n\n");
                
                writer.write("[r6] hasNoGoodNaturalness(X) <= hasVeryStrongCon(X).\n");
                writer.write("[r7] hasNoGoodNaturalness(X) <= hasStrongCon(X).\n");
                writer.write("[r8] hasNoGoodNaturalness(X) <= hasModerateCon(X).\n");
                writer.write("[r9] hasNoGoodNaturalness(X) <= hasWeakCon(X).\n");
                writer.write("[r10] hasNoGoodNaturalness(X) <= hasVeryWeakCon(X).\n\n\n");
                
                // PRIORISATION DES REGLES
                writer.write("%%% === PRIORISATION DES REGLES ===\n\n");
                writer.write("%%% Les arguments con forts l'emportent sur les arguments pro\n\n");
                writer.write("r6 >> r1.\n");
                writer.write("r6 >> r2.\n");
                writer.write("r6 >> r3.\n");
                writer.write("r6 >> r4.\n");
                writer.write("r6 >> r5.\n\n");
                
                writer.write("r1 >> r8.\n");
                writer.write("r1 >> r9.\n");
                writer.write("r1 >> r10.\n\n");
                
                writer.write("r2 >> r8.\n");
                writer.write("r2 >> r9.\n");
                writer.write("r2 >> r10.\n\n");
                
                // CONTRAINTES
                writer.write("%%% === CONTRAINTES ===\n\n");
                writer.write("! :- hasGoodNaturalness(X), hasNoGoodNaturalness(X).\n\n\n");
                
                // STATISTIQUES
                writer.write("%%% === STATISTIQUES ===\n");
                writer.write("%%% Arguments : " + productArguments.size() + "\n");
                writer.write("%%% Produit : " + alternative.name + "\n");
                writer.write("%%% Règles défaisables : 10\n");
                writer.write("%%% Priorités : 11\n");
                writer.write("%%% Contraintes : 1\n");
                writer.write("\n\n");
                
                // EXEMPLES DE QUERY
                writer.write("%%% === EXEMPLES DE QUERY ===\n\n");
                writer.write("%%% Queries sur les Arguments :\n");
                writer.write("%%% argument(Arg), argProduct(Arg, " + productName + ").\n");
                writer.write("%%% argument(Arg), argProduct(Arg, X), argPolarity(Arg, pro).\n");
                writer.write("%%% argument(Arg), argProduct(Arg, X), argPolarity(Arg, con).\n");
                writer.write("%%% argument(Arg), argFiability(Arg, 1).\n");
                writer.write("%%% argument(Arg), argTag(Arg, Tag).\n\n");
                
                writer.write("%%% Queries sur les Niveaux de Force :\n");
                writer.write("%%% hasVeryStrongPro(" + productName + ").\n");
                writer.write("%%% hasVeryStrongCon(" + productName + ").\n");
                writer.write("%%% hasStrongPro(" + productName + ").\n");
                writer.write("%%% hasStrongCon(" + productName + ").\n");
                writer.write("%%% argument(Arg), argProduct(Arg, " + productName + "), argPolarity(Arg, pro), veryStrongArgument(Arg).\n\n");
                
                writer.write("%%% Queries sur la Naturalité :\n");
                writer.write("%%% hasGoodNaturalness(" + productName + ").\n");
                writer.write("%%% hasNoGoodNaturalness(" + productName + ").\n");
                writer.write("%%% hasGoodNaturalness(" + productName + "), hasNoGoodNaturalness(" + productName + ").\n\n");
                
                writer.write("%%% Queries Complexes :\n");
                writer.write("%%% hasStrongPro(" + productName + "), hasStrongCon(" + productName + ").\n");
                writer.write("%%% argument(Arg1), argument(Arg2), argProduct(Arg1, " + productName + "), argProduct(Arg2, " + productName + "), argFiability(Arg1, 1), argFiability(Arg2, 1), argPolarity(Arg1, pro), argPolarity(Arg2, con).\n\n");
                
                writer.write("%%% Queries avec Exclusions :\n");
                writer.write("%%% argument(Arg), argProduct(Arg, " + productName + "), argPolarity(Arg, pro), \\+ (argument(Arg2), argProduct(Arg2, " + productName + "), argPolarity(Arg2, con)).\n");
            }
            
            System.out.println("Export texte DAMN by Alternative réussi : " + outputFile);
            System.out.println("  - Produit : " + alternative.name);
            System.out.println("  - " + productArguments.size() + " arguments exportés");
        }
    }
}
