package natclinn.util;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ClosableIterator;
import org.apache.jena.graph.Triple;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Primitive pour extraire les informations NOVA d'un produit pour un groupe spécifique
 * Usage: getNOVAgroupInfoToProduct(?product, ?groupNumber, ?result)
 * 
 * Pour un produit donné, récupère les valeurs ncl:hasNOVAmarkerInfo au format "GroupeX_markerType_markerVal"
 * où X correspond au groupNumber en entrée.
 * 
 * Sources des marqueurs NOVA:
 * 1. Marqueurs rattachés directement au produit (via GetNOVAgroupToProduct à partir des catégories)
 * 2. Marqueurs des ingrédients du produit (via GetNOVAgroup)
 * 
 * Retourne une chaîne: "markerType1:markerVal1, markerType2:markerVal2, ..."
 * 
 * Exemple:
 * - Entrée: Product P-123, groupNumber = 4
 * - Si le produit a: "Groupe4_categories_en:ultra-processed-foods"
 * - Et ses ingrédients ont: "Groupe4_ingredients_en:beef", "Groupe4_additives_en:e422"
 * - Retour: "categories:en:ultra-processed-foods, ingredients:en:beef, additives:en:e422"
 */
public class GetNOVAgroupInfoToProduct extends BaseBuiltin {
    
    private static final String ncl;
    private static final Pattern NOVA_PATTERN = Pattern.compile("^Groupe(\\d+)_(.+)$");
    
    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
    }
    
    @Override
    public String getName() {
        return "getNOVAgroupInfoToProduct";
    }

    @Override
    public int getArgLength() {
        return 3; // product, groupNumber, result
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        
        Node productNode = getArg(0, args, context);
        Node groupNumberNode = getArg(1, args, context);
        
        // Vérifier que le produit est une ressource URI
        if (!productNode.isURI()) {
            return false;
        }
        
        // Vérifier que groupNumber est un entier entre 1 et 4
        if (!groupNumberNode.isLiteral()) {
            return false;
        }
        
        int groupNumber;
        try {
            groupNumber = Integer.parseInt(groupNumberNode.getLiteralLexicalForm());
            if (groupNumber < 1 || groupNumber > 4) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        // Collecter les informations NOVA
        List<String> novaInfos = collectNOVAInfos(productNode, groupNumber, context);
        
        if (novaInfos.isEmpty()) {
            return false;
        }
        
        // Construire la chaîne résultat
        String result = String.join(", ", novaInfos);
        Node resultNode = NodeFactory.createLiteral(result);
        
        return context.getEnv().bind(args[2], resultNode);
    }
    
    /**
     * Collecte les informations NOVA pour un produit et un groupe spécifique
     */
    private List<String> collectNOVAInfos(Node productNode, int groupNumber, RuleContext context) {
        List<String> novaInfos = new ArrayList<>();
        Node hasIngredientR = NodeFactory.createURI(ncl + "hasIngredientR");
        Node hasNOVAmarkerInfo = NodeFactory.createURI(ncl + "hasNOVAmarkerInfo");
        
        // 1. Collecter les marqueurs NOVA rattachés directement au produit (via catégories)
        try {
            ClosableIterator<Triple> productNovaTriples = context.find(productNode, hasNOVAmarkerInfo, Node.ANY);
            try {
                while (productNovaTriples.hasNext()) {
                    Triple novaTriple = productNovaTriples.next();
                    Node novaInfoNode = novaTriple.getObject();
                    
                    if (novaInfoNode.isLiteral()) {
                        String novaInfoValue = novaInfoNode.getLiteralLexicalForm();
                        String extracted = extractNOVAInfo(novaInfoValue, groupNumber);
                        
                        if (extracted != null && !novaInfos.contains(extracted)) {
                            novaInfos.add(extracted);
                        }
                    }
                }
            } finally {
                productNovaTriples.close();
            }
        } catch (Exception e) {
            System.err.println("Erreur collecte marqueurs produit : " + e.getMessage());
        }
        
        // 2. Collecter les marqueurs NOVA des ingrédients du produit
        ClosableIterator<Triple> ingredientTriples = context.find(productNode, hasIngredientR, Node.ANY);
        
        try {
            while (ingredientTriples.hasNext()) {
                Triple ingredientTriple = ingredientTriples.next();
                Node ingredientNode = ingredientTriple.getObject();
                
                // Pour chaque ingrédient, trouver ses marqueurs NOVA
                ClosableIterator<Triple> novaTriples = context.find(ingredientNode, hasNOVAmarkerInfo, Node.ANY);
                
                try {
                    while (novaTriples.hasNext()) {
                        Triple novaTriple = novaTriples.next();
                        Node novaInfoNode = novaTriple.getObject();
                        
                        if (novaInfoNode.isLiteral()) {
                            String novaInfoValue = novaInfoNode.getLiteralLexicalForm();
                            String extracted = extractNOVAInfo(novaInfoValue, groupNumber);
                            
                            if (extracted != null && !novaInfos.contains(extracted)) {
                                novaInfos.add(extracted);
                            }
                        }
                    }
                } finally {
                    novaTriples.close();
                }
            }
        } finally {
            ingredientTriples.close();
        }
        
        return novaInfos;
    }
    
    /**
     * Extrait markerType:markerVal d'une chaîne "GroupeX_markerType_markerVal"
     * si X correspond au groupNumber
     */
    private String extractNOVAInfo(String novaInfoValue, int groupNumber) {
        Matcher matcher = NOVA_PATTERN.matcher(novaInfoValue);
        
        if (matcher.matches()) {
            int groupe = Integer.parseInt(matcher.group(1));
            
            if (groupe == groupNumber) {
                String remainder = matcher.group(2);
                // Convertir les underscores en deux-points pour le format final
                // "ingredients_en:beef" -> "ingredients:en:beef"
                return remainder.replace("_", ":");
            }
        }
        
        return null;
    }
}
