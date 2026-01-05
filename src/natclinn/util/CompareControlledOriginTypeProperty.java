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
 * Primitive pour lier les produits aux arguments consommateurs via les origines contrôlées des ingrédients.
 * Crée des instances LinkToArgument pour établir la relation Product -> LinkToArgument -> ProductArgument.
 *
 * Signature: compareOriginProperty(?product, ?productArgument)
 * - ?product : ressource Product avec ingrédients et/ou vérifications d'absence d'origine
 * - ?productArgument : ressource ProductArgument (données consommateurs)
 *
 * Retourne true si les origines des ingrédients du produit matchent
 * avec les propriétés du ProductArgument via les métadonnées d'origine.
 * Crée alors un LinkToArgument pour établir le lien.
 */
public class CompareControlledOriginTypeProperty extends BaseBuiltin {

    private static final String ncl;
    private static final Node HAS_CONTROLLED_ORIGIN_TYPE;
    private static final Node HAS_ORIGIN_CHECK;
    private static final Node PRODUCT_ARG_NAME_PROPERTY;
    private static final Node BINDING_AGENT_NAME_PROPERTY;
    private static final Node ORIGIN_REQUIRED;
    private static final Node ABOUT_ORIGIN;
    private static final Node HAS_LINK_TO_ARGUMENT;
    private static final Node HAS_REFERENCE_PRODUCT_ARGUMENT;
    private static final Node INITIATOR;
    private static final Node LINK_SUPPORT_TYPE;
    private static final Node LINK_NAME_PROPERTY;
    private static final Node LINK_VALUE_PROPERTY;

    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        HAS_CONTROLLED_ORIGIN_TYPE = NodeFactory.createURI(ncl + "hasControlledOriginType");
        HAS_ORIGIN_CHECK = NodeFactory.createURI(ncl + "hasOriginCheck");
        PRODUCT_ARG_NAME_PROPERTY = NodeFactory.createURI(ncl + "nameProperty");
        BINDING_AGENT_NAME_PROPERTY = NodeFactory.createURI(ncl + "bindingAgentNameProperty");
        ORIGIN_REQUIRED = NodeFactory.createURI(ncl + "originRequired");
        ABOUT_ORIGIN = NodeFactory.createURI(ncl + "aboutOrigin");
        HAS_LINK_TO_ARGUMENT = NodeFactory.createURI(ncl + "hasLinkToArgument");
        HAS_REFERENCE_PRODUCT_ARGUMENT = NodeFactory.createURI(ncl + "hasReferenceProductArgument");
        INITIATOR = NodeFactory.createURI(ncl + "initiator");
        LINK_SUPPORT_TYPE = NodeFactory.createURI(ncl + "linkSupportType");
        LINK_NAME_PROPERTY = NodeFactory.createURI(ncl + "LinkNameProperty");
        LINK_VALUE_PROPERTY = NodeFactory.createURI(ncl + "LinkValueProperty");
    }

    @Override
    public String getName() {
        return "compareControlledOriginTypeProperty";
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

        // 1) Récupérer toutes les origines contrôlées du produit via ncl:hasControlledOriginType
        Set<Node> originTypeNodes = new HashSet<>();
        ExtendedIterator<Triple> itOrigin = g.find(productNode, HAS_CONTROLLED_ORIGIN_TYPE, Node.ANY);
        while (itOrigin.hasNext()) {
            Node originType = itOrigin.next().getObject();
            if (originType.isURI()) {
                originTypeNodes.add(originType);
            }
        }
        itOrigin.close();

        // 1b) Récupérer aussi les origines via ncl:hasOriginCheck (pour les règles de détection).
        Set<Node> originCheckNodes = new HashSet<>();
        ExtendedIterator<Triple> itCheck = g.find(productNode, HAS_ORIGIN_CHECK, Node.ANY);
        while (itCheck.hasNext()) {
            Node val = itCheck.next().getObject();
            if (val.isURI()) {
                originCheckNodes.add(val);
            }
        }
        itCheck.close();

        if (originTypeNodes.isEmpty() && originCheckNodes.isEmpty()) return false;

        // 2) Récupérer les propriétés du ProductArgument
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

        if (argProperties.isEmpty()) return false;

        // 3) Comparer via les métadonnées ControlledOriginTypeArgumentBinding
        // Vérifier d'abord les hasControlledOriginType
        for (Node originTypeNode : originTypeNodes) {
            if (checkOriginBinding(g, productNode, argumentNode, originTypeNode, argProperties, context)) {
                return true;
            }
        }
        
        // Vérifier aussi les hasOriginCheck
        for (Node originCheckNode : originCheckNodes) {
            if (checkOriginBinding(g, productNode, argumentNode, originCheckNode, argProperties, context)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean checkOriginBinding(Graph g, Node productNode, Node argumentNode, Node originNode, Set<String> argProperties, RuleContext context) {
        // Chercher les ControlledOriginTypeArgumentBinding qui ont aboutOrigin = originNode
        ExtendedIterator<Triple> itBinding = g.find(Node.ANY, ABOUT_ORIGIN, originNode);
        while (itBinding.hasNext()) {
            Node bindingNode = itBinding.next().getSubject();
            
            // Récupérer bindingAgentNameProperty de ce binding
            ExtendedIterator<Triple> itBindingProp = g.find(bindingNode, BINDING_AGENT_NAME_PROPERTY, Node.ANY);
            while (itBindingProp.hasNext()) {
                Node val = itBindingProp.next().getObject();
                if (!val.isLiteral()) continue;
                
                String bindingProp = val.getLiteralLexicalForm();
                if (bindingProp == null) continue;
                String bp = bindingProp.trim();
                
                // Comparer avec les propriétés de l'argument
                for (String argProp : argProperties) {
                    boolean match = false;
                    if (argProp.equalsIgnoreCase(bp)) {
                        match = true;
                    } else if (normalizeString(argProp).equals(normalizeString(bp))) {
                        match = true;
                    }
                    
                    if (match) {
                        // Vérifier si originRequired = "Oui"
                        boolean required = isOriginRequired(g, bindingNode);
                        if (required) {
                            // Créer un LinkToArgument instance
                            createLinkToArgument(g, productNode, argumentNode, originNode, bp, context);
                            itBindingProp.close();
                            itBinding.close();
                            return true;
                        }
                    }
                }
            }
            itBindingProp.close();
        }
        itBinding.close();
        return false;
    }

    /**
     * Crée un LinkToArgument pour établir le lien Product -> LinkToArgument -> ProductArgument
     * @param g le graphe
     * @param productNode le noeud Product
     * @param argumentNode le noeud ProductArgument
     * @param originNode le noeud ControlledOriginType
     * @param propertyName la propriété qui a matché
     * @param context le contexte de la règle
     */
    private void createLinkToArgument(Graph g, Node productNode, Node argumentNode, Node originNode, String propertyName, RuleContext context) {
        // Créer un nouvel identifiant unique pour le LinkToArgument
        String linkId = "LinkToArgument_" + System.currentTimeMillis() + "_" + Math.abs((productNode.toString() + argumentNode.toString()).hashCode());
        Node linkNode = NodeFactory.createURI(ncl + linkId);
        
        // Créer les triples pour le LinkToArgument
        // Product -> hasLinkToArgument -> LinkToArgument
        Triple t1 = Triple.create(productNode, HAS_LINK_TO_ARGUMENT, linkNode);
        g.add(t1);
        
        // LinkToArgument -> hasReferenceProductArgument -> ProductArgument
        Triple t2 = Triple.create(linkNode, HAS_REFERENCE_PRODUCT_ARGUMENT, argumentNode);
        g.add(t2);
        
        // LinkToArgument -> initiator -> "ControlledOriginLabel"
        Node initiatorValue = NodeFactory.createLiteral("ControlledOriginLabel");
        Triple t3 = Triple.create(linkNode, INITIATOR, initiatorValue);
        g.add(t3);
        
        // LinkToArgument -> linkSupportType -> "For" (par défaut)
        Node supportTypeValue = NodeFactory.createLiteral("For");
        Triple t4 = Triple.create(linkNode, LINK_SUPPORT_TYPE, supportTypeValue);
        g.add(t4);
        
        // LinkToArgument -> LinkNameProperty -> propertyName
        Node nameValue = NodeFactory.createLiteral(propertyName);
        Triple t5 = Triple.create(linkNode, LINK_NAME_PROPERTY, nameValue);
        g.add(t5);
        
        // LinkToArgument -> LinkValueProperty -> origin URI
        String originUri = originNode.isURI() ? originNode.getURI() : originNode.toString();
        Node valueNode = NodeFactory.createLiteral(originUri);
        Triple t6 = Triple.create(linkNode, LINK_VALUE_PROPERTY, valueNode);
        g.add(t6);
    }

    private void collectOrigins_UNUSED(Graph g, Node ingredient, Set<String> out) {
        ExtendedIterator<Triple> itFunc = g.find(ingredient, HAS_CONTROLLED_ORIGIN_TYPE, Node.ANY);
        while (itFunc.hasNext()) {
            Node val = itFunc.next().getObject();
            if (val.isURI()) {
                String uri = val.getURI();
                String originName = uri.substring(uri.lastIndexOf('/') + 1);
                out.add(originName.toLowerCase().trim());
            }
        }
        itFunc.close();
    }

    private boolean isOriginRequired(Graph g, Node bindingNode) {
        ExtendedIterator<Triple> itRequired = g.find(bindingNode, ORIGIN_REQUIRED, Node.ANY);
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
