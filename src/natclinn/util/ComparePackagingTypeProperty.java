package natclinn.util;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.HashSet;
import java.util.Set;

/**
 * Primitive pour lier les produits aux arguments consommateurs via les métadonnées de types d'emballage.
 * 
 * Architecture:
 * - Product → hasTypePackaging → PackagingType (présence de types d'emballage)
 * - Product → hasPackagingCheck → sans_plastique / sans_emballage / etc. (absence/vérifications)
 * - PackagingTypeArgumentBinding → aboutPackagingType → PackagingType (métadonnées: bindingAgentNameProperty, etc.)
 * - ProductArgument (données enquêtes consommateurs: nameProperty, nameCriterion, etc.)
 * 
 * Signature: comparePackagingTypeProperty(?product, ?productArgument)
 * - ?product : ressource Product avec types d'emballage et/ou vérifications d'absence
 * - ?productArgument : ressource ProductArgument (données consommateurs)
 * 
 * Retourne true si les types d'emballage du produit (présence ou absence) matchent
 * avec les propriétés du ProductArgument via les métadonnées PackagingTypeArgumentBinding.
 */
public class ComparePackagingTypeProperty extends BaseBuiltin {

    private static final String ncl;
    private static final Node HAS_TYPE_PACKAGING;
    private static final Node HAS_PACKAGING_CHECK;
    // Propriété sur ProductArgument (données consommateurs)
    private static final Node PRODUCT_ARG_NAME_PROPERTY;
    // Propriété sur PackagingTypeArgumentBinding (métadonnées type d'emballage)
    private static final Node BINDING_AGENT_NAME_PROPERTY;
    private static final Node PACKAGING_TYPE_REQUIRED;
    private static final Node ABOUT_PACKAGING_TYPE;

    static {
        // Initialisation de la configuration
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        HAS_TYPE_PACKAGING = NodeFactory.createURI(ncl + "hasTypePackaging");
        HAS_PACKAGING_CHECK = NodeFactory.createURI(ncl + "hasPackagingCheck");
        PRODUCT_ARG_NAME_PROPERTY = NodeFactory.createURI(ncl + "nameProperty");
        BINDING_AGENT_NAME_PROPERTY = NodeFactory.createURI(ncl + "bindingAgentNameProperty");
        PACKAGING_TYPE_REQUIRED = NodeFactory.createURI(ncl + "packagingTypeRequired");
        ABOUT_PACKAGING_TYPE = NodeFactory.createURI(ncl + "aboutPackagingType");
    }

    @Override
    public String getName() {
        return "comparePackagingTypeProperty";
    }

    @Override
    public int getArgLength() {
        return 2; // product, argument
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);

        Node productNode = getArg(0, args, context);
        Node argumentNode = getArg(1, args, context);

        if (!productNode.isURI() && !productNode.isBlank()) return false;
        if (!argumentNode.isURI() && !argumentNode.isBlank()) return false;

        Graph g = context.getGraph();

        // 1) Récupérer tous les types d'emballage du produit.
        //    On collecte les noms locaux des types (e.g., "emballage_plastique", "emballage_verre")
        //    depuis les ressources ncl:hasTypePackaging attachées au produit.
        Set<String> packagingTypes = new HashSet<>();

        ExtendedIterator<Triple> itType = g.find(productNode, HAS_TYPE_PACKAGING, Node.ANY);
        while (itType.hasNext()) {
            Node val = itType.next().getObject();
            if (val.isURI()) {
                String uri = val.getURI();
                String typeName = uri.substring(uri.lastIndexOf('/') + 1);
                packagingTypes.add(typeName.toLowerCase().trim());
            }
        }
        itType.close();

        // 1b) Récupérer les vérifications d'emballage via ncl:hasPackagingCheck.
        //     Ces vérifications indiquent l'ABSENCE ou des caractéristiques particulières
        //     (e.g., "sans_plastique", "sans_emballage", "emballage_naturel", "emballage_biodegradable").
        ExtendedIterator<Triple> itCheck = g.find(productNode, HAS_PACKAGING_CHECK, Node.ANY);
        while (itCheck.hasNext()) {
            Node val = itCheck.next().getObject();
            if (val.isURI()) {
                String uri = val.getURI();
                String checkName = uri.substring(uri.lastIndexOf('/') + 1);
                packagingTypes.add(checkName.toLowerCase().trim());
            }
        }
        itCheck.close();

        // Si aucun type d'emballage détecté, le produit ne peut être lié à cet argument.
        if (packagingTypes.isEmpty()) return false;

        // 2) Récupérer les propriétés du ProductArgument (données enquêtes consommateurs).
        //    Les ProductArgument définissent leurs caractéristiques via ncl:nameProperty, ncl:nameCriterion, etc.
        //    On collecte toutes les valeurs pour comparaison avec les métadonnées des types d'emballage.
        Set<String> argProperties = new HashSet<>();
        ExtendedIterator<Triple> itProp = g.find(argumentNode, PRODUCT_ARG_NAME_PROPERTY, Node.ANY);
        while (itProp.hasNext()) {
            Node val = itProp.next().getObject();
            if (val.isLiteral()) {
                String txt = val.getLiteralLexicalForm().trim();
                if (!txt.isEmpty()) argProperties.add(txt);
            }
        }
        itProp.close();

        // Si l'argument n'a aucune propriété définie, aucun lien ne peut être établi.
        if (argProperties.isEmpty()) return false;

        // 3) Comparer via les métadonnées PackagingTypeArgumentBinding.
        //    Pour chaque type d'emballage détecté dans le produit :
        //      a) Construire l'URI de la ressource PackagingType (e.g., ncl:emballage_plastique).
        //      b) Trouver tous les PackagingTypeArgumentBinding liés à ce type (via aboutPackagingType).
        //      c) Récupérer les bindingAgentNameProperty de ces PackagingTypeArgumentBinding.
        //      d) Comparer avec les nameProperty du ProductArgument.
        //         Si correspondance (exacte ou normalisée) → le produit peut être lié à cet argument.
        for (String typeLocal : packagingTypes) {
            Node packagingTypeUri = NodeFactory.createURI(ncl + typeLocal);
            
            // Rechercher tous les PackagingTypeArgumentBinding ayant ncl:aboutPackagingType pointant vers packagingTypeUri
            ExtendedIterator<Triple> itBinding = g.find(Node.ANY, ABOUT_PACKAGING_TYPE, packagingTypeUri);
            while (itBinding.hasNext()) {
                Node bindingNode = itBinding.next().getSubject();

                // Extraire les bindingAgentNameProperty de ce PackagingTypeArgumentBinding
                ExtendedIterator<Triple> itBindingProp = g.find(bindingNode, BINDING_AGENT_NAME_PROPERTY, Node.ANY);
                while (itBindingProp.hasNext()) {
                    Node val = itBindingProp.next().getObject();
                    if (!val.isLiteral()) continue;
                    String bindingProp = val.getLiteralLexicalForm();
                    if (bindingProp == null) continue;
                    String bp = bindingProp.trim();

                    // Comparer bindingAgentNameProperty (métadonnées) avec nameProperty (ProductArgument)
                    for (String argProp : argProperties) {
                        boolean match = false;
                        
                        // Comparaison exacte (insensible à la casse)
                        if (argProp.equalsIgnoreCase(bp)) {
                            match = true;
                        }
                        // Comparaison normalisée (suppression accents et caractères spéciaux)
                        else if (normalizeString(argProp).equals(normalizeString(bp))) {
                            match = true;
                        }
                        
                        // Si pas de correspondance nameProperty → rejet immédiat
                        if (!match) continue;
                        
                        // Si correspondance trouvée, vérifier packagingTypeRequired
                        boolean required = isPackagingTypeRequired(g, bindingNode);
                        
                        // Si packagingTypeRequired="oui" (ou variantes) → lien accepté
                        if (required) {
                            itBindingProp.close();
                            itBinding.close();
                            return true;
                        }
                        // Si packagingTypeRequired n'est pas "oui" → rejet
                        // (on continue la boucle pour vérifier d'autres bindings)
                    }
                }
                itBindingProp.close();
            }
            itBinding.close();
        }

        return false;
    }

    /**
     * Vérifie si la propriété packagingTypeRequired d'un PackagingTypeArgumentBinding
     * indique que le type d'emballage est obligatoire (valeurs: Oui, OUI, oui, Yes, yes, OK, ok, Ok).
     * 
     * Logique de matching :
     * 1. Vérifier d'abord la correspondance nameProperty (bindingAgentNameProperty vs nameProperty)
     * 2. Si pas de correspondance → rejet (return false)
     * 3. Si correspondance ET packagingTypeRequired="oui" → lien créé (return true)
     * 4. Si correspondance ET packagingTypeRequired≠"oui" → rejet (return false)
     * 
     * Le flag packagingTypeRequired filtre donc les liens : seuls les bindings avec packagingTypeRequired="oui"
     * ET une correspondance nameProperty créent effectivement un lien Product-ProductArgument.
     * 
     * @param g Le graphe RDF contenant les données.
     * @param bindingNode Le nœud PackagingTypeArgumentBinding à vérifier.
     * @return true si packagingTypeRequired indique "oui", false sinon.
     */
    private boolean isPackagingTypeRequired(Graph g, Node bindingNode) {
        ExtendedIterator<Triple> itRequired = g.find(bindingNode, PACKAGING_TYPE_REQUIRED, Node.ANY);
        while (itRequired.hasNext()) {
            Node val = itRequired.next().getObject();
            if (val.isLiteral()) {
                String required = val.getLiteralLexicalForm();
                if (required != null) {
                    String req = required.trim().toLowerCase();
                    if (req.equals("oui") || req.equals("yes") || req.equals("ok")) {
                        itRequired.close();
                        return true;
                    }
                }
            }
        }
        itRequired.close();
        return false;
    }

    /**
     * Normalise une chaîne de caractères en :
     * - Convertissant en minuscules.
     * - Remplaçant les caractères accentués par leur équivalent non accentué.
     * - Supprimant tous les caractères qui ne sont ni alphanumériques ni tirets/underscores.
     * 
     * Cette normalisation permet une comparaison insensible aux accents et à la ponctuation
     * pour détecter des correspondances sémantiques entre:
     * - ProductArgument.nameProperty (données consommateurs)
     * - PackagingTypeArgumentBinding.bindingAgentNameProperty (métadonnées types d'emballage)
     * 
     * @param str La chaîne à normaliser.
     * @return La chaîne normalisée.
     */
    private String normalizeString(String str) {
        if (str == null) return "";
        return str.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ýÿ]", "y")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
}
