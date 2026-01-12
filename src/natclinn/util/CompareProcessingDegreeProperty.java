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
 * Primitive pour lier les produits aux arguments via la propriété de transformation `hasProcessing`.
 * Fonctionne de manière analogue à ComparePackagingTypeProperty mais utilise les métadonnées
 * définies par CreateNatclinnProcessingOntology : aboutProcessing, processingNameProperty, processingRequired.
 *
 * Signature: compareProcessingTypeProperty(?product, ?processingArgument)
 */
public class CompareProcessingDegreeProperty extends BaseBuiltin {

    private static final String ncl;
    private static final Node HAS_PROCESSING;
    private static final Node PRODUCT_ARG_NAME_PROPERTY;
    private static final Node ABOUT_PROCESSING;
    private static final Node PROCESSING_REQUIRED;
    private static final Node HAS_LINK_TO_ARGUMENT;
    private static final Node HAS_REFERENCE_PRODUCT_ARGUMENT;
    private static final Node INITIATOR;
    private static final Node LINK_SUPPORT_TYPE;
    private static final Node LINK_NAME_PROPERTY;
    private static final Node LINK_VALUE_PROPERTY;

    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        HAS_PROCESSING = NodeFactory.createURI(ncl + "hasProcessing");
        PRODUCT_ARG_NAME_PROPERTY = NodeFactory.createURI(ncl + "nameProperty");
        ABOUT_PROCESSING = NodeFactory.createURI(ncl + "aboutProcessing");
        PROCESSING_REQUIRED = NodeFactory.createURI(ncl + "processingRequired");
        HAS_LINK_TO_ARGUMENT = NodeFactory.createURI(ncl + "hasLinkToArgument");
        HAS_REFERENCE_PRODUCT_ARGUMENT = NodeFactory.createURI(ncl + "hasReferenceProductArgument");
        INITIATOR = NodeFactory.createURI(ncl + "initiator");
        LINK_SUPPORT_TYPE = NodeFactory.createURI(ncl + "linkSupportType");
        LINK_NAME_PROPERTY = NodeFactory.createURI(ncl + "LinkNameProperty");
        LINK_VALUE_PROPERTY = NodeFactory.createURI(ncl + "LinkValueProperty");
    }

    @Override
    public String getName() {
        return "compareProcessingTypeProperty";
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

        // 1) Récupérer tous les types de transformation (processing) du produit.
        Set<String> processingTypes = new HashSet<>();
        ExtendedIterator<Triple> itProc = g.find(productNode, HAS_PROCESSING, Node.ANY);
        while (itProc.hasNext()) {
            Node val = itProc.next().getObject();
            if (val.isURI()) {
                String uri = val.getURI();
                String typeName = uri.substring(uri.lastIndexOf('/') + 1);
                processingTypes.add(typeName.toLowerCase().trim());
            }
        }
        itProc.close();

        if (processingTypes.isEmpty()) return false;
        System.out.println("Processing types for product " + productNode.toString() + ": " + processingTypes);

        // 2) Récupérer les propriétés de l'argument (processingNameProperty)
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

        // 3) Pour chaque processingType, trouver les ProcessingArgumentBinding liés via aboutProcessing
        for (String typeLocal : processingTypes) {
            Node processingUri = NodeFactory.createURI(ncl + typeLocal);
            ExtendedIterator<Triple> itBinding = g.find(Node.ANY, ABOUT_PROCESSING, processingUri);
            while (itBinding.hasNext()) {
                Node bindingNode = itBinding.next().getSubject();

                // Extraire processingNameProperty de ce binding
                ExtendedIterator<Triple> itBindingProp = g.find(bindingNode, PRODUCT_ARG_NAME_PROPERTY, Node.ANY);
                while (itBindingProp.hasNext()) {
                    Node val = itBindingProp.next().getObject();
                    if (!val.isLiteral()) continue;
                    String bindingProp = val.getLiteralLexicalForm();
                    if (bindingProp == null) continue;
                    String bp = bindingProp.trim();

                    for (String argProp : argProperties) {
                        boolean match = false;
                        if (argProp.equalsIgnoreCase(bp)) match = true;
                        else if (normalizeString(argProp).equals(normalizeString(bp))) match = true;
                        if (!match) continue;

                        // vérifier processingRequired
                        boolean required = isProcessingRequired(g, bindingNode);
                        if (required) {
                            createLinkToArgument(g, productNode, argumentNode, processingUri, bp, context);
                            itBindingProp.close();
                            itBinding.close();
                            return true;
                        }
                    }
                }
                itBindingProp.close();
            }
            itBinding.close();
        }

        return false;
    }

    private void createLinkToArgument(Graph g, Node productNode, Node argumentNode, Node processingNode, String propertyName, RuleContext context) {
        String linkId = "LinkToArgument_" + System.currentTimeMillis() + "_" + Math.abs((productNode.toString() + argumentNode.toString()).hashCode());
        Node linkNode = NodeFactory.createURI(ncl + linkId);

        g.add(Triple.create(productNode, HAS_LINK_TO_ARGUMENT, linkNode));
        g.add(Triple.create(linkNode, HAS_REFERENCE_PRODUCT_ARGUMENT, argumentNode));

        Node initiatorValue = NodeFactory.createLiteral("ProcessingType");
        g.add(Triple.create(linkNode, INITIATOR, initiatorValue));

        Node supportTypeValue = NodeFactory.createLiteral("For");
        g.add(Triple.create(linkNode, LINK_SUPPORT_TYPE, supportTypeValue));

        Node nameValue = NodeFactory.createLiteral(propertyName);
        g.add(Triple.create(linkNode, LINK_NAME_PROPERTY, nameValue));

        String procUri = processingNode.isURI() ? processingNode.getURI() : processingNode.toString();
        Node valueNode = NodeFactory.createLiteral(procUri);
        g.add(Triple.create(linkNode, LINK_VALUE_PROPERTY, valueNode));
    }

    private boolean isProcessingRequired(Graph g, Node bindingNode) {
        ExtendedIterator<Triple> itRequired = g.find(bindingNode, PROCESSING_REQUIRED, Node.ANY);
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
