package natclinn.util;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import java.util.HashMap;
import java.util.Map;

/**
 * Primitive pour identifier le type d'emballage d'un produit via les matériaux
 * Usage: getTypePackaging(?materialLabel, ?packagingType)
 * Retourne: une ressource URI ncl:<type_emballage> (ex: ncl:emballage_plastique, ncl:emballage_verre)
 */
public class GetTypePackaging extends BaseBuiltin {
    
    private static final String ncl;
    
    // Cache pour éviter les calculs répétés
    private static Map<String, String> cache = new HashMap<>();
    
    // Mapping des matériaux vers les types d'emballage
    private static final Map<String, String> MATERIAL_TO_PACKAGING_TYPE = new HashMap<>();
    
    static {
        // Initialisation de la configuration
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        
        // Plastiques (moins naturel)
        MATERIAL_TO_PACKAGING_TYPE.put("plastic", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("plastique", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("pet", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("hdpe", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("pvc", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("ldpe", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("pp", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("ps", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("polypropylene", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("polypropylène", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("polyethylene", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("polyéthylène", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("polystyrene", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("polystyrène", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("film", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("sachet", "emballage_plastique");
        MATERIAL_TO_PACKAGING_TYPE.put("blister", "emballage_plastique");
        
        // Verre (naturel et recyclable)
        MATERIAL_TO_PACKAGING_TYPE.put("glass", "emballage_verre");
        MATERIAL_TO_PACKAGING_TYPE.put("verre", "emballage_verre");
        MATERIAL_TO_PACKAGING_TYPE.put("jar", "emballage_verre");
        MATERIAL_TO_PACKAGING_TYPE.put("bocal", "emballage_verre");
        MATERIAL_TO_PACKAGING_TYPE.put("bouteille en verre", "emballage_verre");
        
        // Métal (recyclable)
        MATERIAL_TO_PACKAGING_TYPE.put("metal", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("métal", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("aluminium", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("aluminum", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("steel", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("acier", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("tin", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("fer-blanc", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("can", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("boite", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("boîte", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("canette", "emballage_metal");
        MATERIAL_TO_PACKAGING_TYPE.put("conserve", "emballage_metal");
        
        // Carton/Papier (naturel et recyclable)
        MATERIAL_TO_PACKAGING_TYPE.put("cardboard", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("carton", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("paper", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("papier", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("paperboard", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("carton ondulé", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("corrugated", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("non-corrugated", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("baking paper", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("baking-paper", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("kraft", "emballage_carton");
        MATERIAL_TO_PACKAGING_TYPE.put("tetra pak", "emballage_composite");
        MATERIAL_TO_PACKAGING_TYPE.put("tetrapak", "emballage_composite");
        MATERIAL_TO_PACKAGING_TYPE.put("tetra brik", "emballage_composite");
        
        // Bois (naturel)
        MATERIAL_TO_PACKAGING_TYPE.put("wood", "emballage_bois");
        MATERIAL_TO_PACKAGING_TYPE.put("bois", "emballage_bois");
        MATERIAL_TO_PACKAGING_TYPE.put("wooden", "emballage_bois");
        MATERIAL_TO_PACKAGING_TYPE.put("crate", "emballage_bois");
        MATERIAL_TO_PACKAGING_TYPE.put("caisse", "emballage_bois");
        
        // Matériaux composites (mix)
        MATERIAL_TO_PACKAGING_TYPE.put("composite", "emballage_composite");
        MATERIAL_TO_PACKAGING_TYPE.put("multi-material", "emballage_composite");
        MATERIAL_TO_PACKAGING_TYPE.put("multi-matériau", "emballage_composite");
        MATERIAL_TO_PACKAGING_TYPE.put("multilayer", "emballage_composite");
        MATERIAL_TO_PACKAGING_TYPE.put("multicouche", "emballage_composite");
        
        // Matériaux biodégradables/compostables (naturel)
        MATERIAL_TO_PACKAGING_TYPE.put("biodegradable", "emballage_biodegradable");
        MATERIAL_TO_PACKAGING_TYPE.put("biodégradable", "emballage_biodegradable");
        MATERIAL_TO_PACKAGING_TYPE.put("compostable", "emballage_compostable");
        MATERIAL_TO_PACKAGING_TYPE.put("bio-based", "emballage_biosource");
        MATERIAL_TO_PACKAGING_TYPE.put("biosourcé", "emballage_biosource");
        MATERIAL_TO_PACKAGING_TYPE.put("pla", "emballage_biosource"); // Acide polylactique
        
        // Autres matériaux
        MATERIAL_TO_PACKAGING_TYPE.put("cork", "emballage_liege");
        MATERIAL_TO_PACKAGING_TYPE.put("liège", "emballage_liege");
        MATERIAL_TO_PACKAGING_TYPE.put("textile", "emballage_textile");
        MATERIAL_TO_PACKAGING_TYPE.put("tissu", "emballage_textile");
        MATERIAL_TO_PACKAGING_TYPE.put("jute", "emballage_textile");
        MATERIAL_TO_PACKAGING_TYPE.put("cotton", "emballage_textile");
        MATERIAL_TO_PACKAGING_TYPE.put("coton", "emballage_textile");
    }
    
    @Override
    public String getName() {
        return "getTypePackaging";
    }

    @Override
    public int getArgLength() {
        return 2; // materialLabel, packagingType
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        
        Node labelNode = getArg(0, args, context);
        
        if (labelNode.isLiteral()) {
            String label = labelNode.getLiteralLexicalForm().toLowerCase().trim();
            
            // Vérifier le cache
            if (cache.containsKey(label)) {
                Node packagingTypeNode = NodeFactory.createURI(ncl + cache.get(label));
                return context.getEnv().bind(args[1], packagingTypeNode);
            }
            
            String packagingType = identifyPackagingType(label);
            
            if (packagingType != null) {
                cache.put(label, packagingType);
                Node packagingTypeNode = NodeFactory.createURI(ncl + packagingType);
                return context.getEnv().bind(args[1], packagingTypeNode);
            }
        }
        return false;
    }
    
    private String identifyPackagingType(String label) {
        // Normaliser le label pour la comparaison
        String normalizedLabel = label
            .toLowerCase()
            .trim()
            .replaceAll("en:", "")  // Supprimer le préfixe Open Food Facts
            .replaceAll("fr:", "")  // Supprimer le préfixe français
            .replaceAll("[_-]", " "); // Remplacer underscores et tirets par espaces
        
        // Rechercher une correspondance exacte ou partielle
        for (Map.Entry<String, String> entry : MATERIAL_TO_PACKAGING_TYPE.entrySet()) {
            String keyword = entry.getKey();
            if (normalizedLabel.equals(keyword) || normalizedLabel.contains(keyword)) {
                return entry.getValue();
            }
        }
        
        // Si aucune correspondance trouvée, retourner null
        return null;
    }
}
