package natclinn.util;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import java.util.HashMap;
import java.util.Map;

/**
 * Primitive pour identifier l'origine contrôlée d'un ingrédient ou produit
 * Usage: getControledOrigineLabel(?originLabel, ?originResource)
 * Retourne: une ressource URI ncl:<origine> (ex: ncl:origine_france, ncl:origine_europe)
 */
public class GetControlledOriginType extends BaseBuiltin {
    private static final String ncl;
    private static Map<String, String> cache = new HashMap<>();
    private static final Map<String, String> ORIGIN_LABEL_TO_RESOURCE = new HashMap<>();

    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        // Mappings directs pour chaque label d'origine fourni (format CamelCase avec underscores)
        ORIGIN_LABEL_TO_RESOURCE.put("viande bovine origine france", "Viande_bovine_origine_France");
        ORIGIN_LABEL_TO_RESOURCE.put("viande de porc francaise", "Viande_de_porc_francaise");
        ORIGIN_LABEL_TO_RESOURCE.put("viande de volaille origine france", "Viande_de_volaille_origine_France");
        ORIGIN_LABEL_TO_RESOURCE.put("viande bovine francaise", "Viande_bovine_francaise");
        ORIGIN_LABEL_TO_RESOURCE.put("porc origine france", "Porc_origine_France");
        ORIGIN_LABEL_TO_RESOURCE.put("poulet origine france", "Poulet_origine_France");
        ORIGIN_LABEL_TO_RESOURCE.put("boeuf francais", "Boeuf_francais");
        ORIGIN_LABEL_TO_RESOURCE.put("boeuf origine france", "Boeuf_francais");
        ORIGIN_LABEL_TO_RESOURCE.put("agneau de france", "Agneau_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("veau francais", "Veau_francais");
        ORIGIN_LABEL_TO_RESOURCE.put("canard du sud ouest", "Canard_du_Sud_Ouest");
        ORIGIN_LABEL_TO_RESOURCE.put("porc de bretagne", "Porc_de_Bretagne");
        ORIGIN_LABEL_TO_RESOURCE.put("boeuf charolais origine france", "Boeuf_charolais_origine_France");
        ORIGIN_LABEL_TO_RESOURCE.put("viande de porc label rouge origine france", "Viande_de_porc_Label_Rouge_origine_France");
        ORIGIN_LABEL_TO_RESOURCE.put("lait origine france", "Lait_origine_France");
        ORIGIN_LABEL_TO_RESOURCE.put("oeufs de france", "Oeufs_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("fromage au lait de france", "Fromage_au_lait_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("pommes de terre de france", "Pommes_de_terre_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("tomates de france", "Tomates_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("farine de ble francais", "Farine_de_ble_francais");
        ORIGIN_LABEL_TO_RESOURCE.put("ble francais", "Ble_francais");
        ORIGIN_LABEL_TO_RESOURCE.put("fruits de france", "Fruits_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("legumes de france", "Legumes_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("miel de france", "Miel_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("huile de tournesol francaise", "Huile_de_tournesol_francaise");
        ORIGIN_LABEL_TO_RESOURCE.put("vin de france", "Vin_de_France");
        ORIGIN_LABEL_TO_RESOURCE.put("beurre de normandie", "Beurre_de_Normandie");
        ORIGIN_LABEL_TO_RESOURCE.put("sel de guerande", "Sel_de_Guerande");
        ORIGIN_LABEL_TO_RESOURCE.put("vanille de madagascar", "Vanille_de_Madagascar");
        ORIGIN_LABEL_TO_RESOURCE.put("cacao de cote d ivoire", "Cacao_de_Cote_d_Ivoire");
        ORIGIN_LABEL_TO_RESOURCE.put("saumon de norvege", "Saumon_de_Norvege");
        ORIGIN_LABEL_TO_RESOURCE.put("thon peche en atlantique", "Thon_peche_en_Atlantique");
    }

    @Override
    public String getName() {
        return "getControlledOriginType";
    }

    @Override
    public int getArgLength() {
        return 2; // originLabel, originResource
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        Node labelNode = getArg(0, args, context);
        if (labelNode.isLiteral()) {
            String label = labelNode.getLiteralLexicalForm().toLowerCase().trim();
            label = label.replaceAll("[éèêë]", "e")
                         .replaceAll("[àâä]", "a")
                         .replaceAll("[îï]", "i")
                         .replaceAll("[ôö]", "o")
                         .replaceAll("[ùûü]", "u")
                         .replaceAll("[ç]", "c")
                         .replaceAll("œ", "oe")
                         .replaceAll("[\\-_]", " ");
            // Vérifier le cache
            if (cache.containsKey(label)) {
                Node originNode = NodeFactory.createURI(ncl + cache.get(label));
                return context.getEnv().bind(args[1], originNode);
            }
            String originResource = identifyOriginResource(label);
            if (originResource != null) {
                cache.put(label, originResource);
                Node originNode = NodeFactory.createURI(ncl + originResource);
                return context.getEnv().bind(args[1], originNode);
            }
        }
        return false;
    }

    private String identifyOriginResource(String label) {
        // Recherche exacte ou partielle
        for (Map.Entry<String, String> entry : ORIGIN_LABEL_TO_RESOURCE.entrySet()) {
            String keyword = entry.getKey();
            if (label.equals(keyword) || label.contains(keyword)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
