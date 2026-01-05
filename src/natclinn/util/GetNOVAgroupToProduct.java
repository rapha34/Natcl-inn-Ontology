package natclinn.util;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Primitive Jena personnalisée : GetNOVAgroupToProduct
 * 
 * Rattache les informations de marqueurs NOVA aux produits en se basant sur leurs catégories.
 * 
 * Utilisation en règle :
 *   GetNOVAgroupToProduct(?product)
 * 
 * Cette primitive :
 * 1. Prend un produit en entrée
 * 2. Récupère toutes ses catégories via hasCategory
 * 3. Pour chaque catégorie, vérifie si elle correspond à un marqueur NOVA
 * 4. Si match trouvé, ajoute la relation hasNOVAmarkerInfo avec la valeur "Groupe<N>_<type>_<valeur>"
 * 5. Explore la hiérarchie taxonomique si pas de correspondance directe
 */
public class GetNOVAgroupToProduct extends BaseBuiltin {

    private static final String ncl;
    private static final Node HAS_CATEGORY;
    private static final Node HAS_NOVA_MARKER_INFO;
    private static final Node RDF_TYPE;
    private static final Node NOVA_MARKER_CLASS;
    private static final Node MARKER_VALUE;
    private static final Node MARKER_TYPE;
    private static final Node BELONGS_TO_NOVA_GROUP;
    private static final Node GROUP_NUMBER;
    private static final Node TAXONOMY_CATEGORY_CLASS;
    private static final Node OFF_CATEGORY_ID;
    private static final Node HAS_PARENT_CATEGORY;

    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        
        HAS_CATEGORY = NodeFactory.createURI(ncl + "hasCategory");
        HAS_NOVA_MARKER_INFO = NodeFactory.createURI(ncl + "hasNOVAmarkerInfo");
        RDF_TYPE = NodeFactory.createURI(org.apache.jena.vocabulary.RDF.type.getURI());
        NOVA_MARKER_CLASS = NodeFactory.createURI(ncl + "NOVAMarker");
        MARKER_VALUE = NodeFactory.createURI(ncl + "markerValue");
        MARKER_TYPE = NodeFactory.createURI(ncl + "markerType");
        BELONGS_TO_NOVA_GROUP = NodeFactory.createURI(ncl + "belongsToNOVAGroup");
        GROUP_NUMBER = NodeFactory.createURI(ncl + "groupNumber");
        TAXONOMY_CATEGORY_CLASS = NodeFactory.createURI(ncl + "TaxonomyCategory");
        OFF_CATEGORY_ID = NodeFactory.createURI(ncl + "offCategoryId");
        HAS_PARENT_CATEGORY = NodeFactory.createURI(ncl + "hasParentCategory");
    }

    @Override
    public String getName() {
        return "GetNOVAgroupToProduct";
    }

    @Override
    public int getArgLength() {
        return 1;  // ?product
    }

    /**
     * Exécute la primitive pour trouver et créer les relations hasNOVAmarkerInfo.
     * 
     * @param args : [?product]
     * @param context : le contexte de l'inférence Jena
     * @return true si au moins un marqueur NOVA a été trouvé
     */
    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        
        // Vérifier que l'argument est lié (ground)
        if (!args[0].isConcrete()) {
            return false;
        }

        Node productNode = args[0];

        try {
            Graph graph = context.getGraph();
            boolean added = false;

            // Récupérer toutes les catégories du produit via hasCategory
            ExtendedIterator<Triple> categories = graph.find(productNode, HAS_CATEGORY, Node.ANY);
            try {
                while (categories.hasNext()) {
                    Node category = categories.next().getObject();
                    
                    // Chercher un marqueur NOVA pour cette catégorie
                    String novaInfo = findNOVAMarkerForCategory(graph, category);
                    if (novaInfo != null) {
                        // Créer le triple product hasNOVAmarkerInfo "Groupe<N>_<type>_<valeur>"
                        Node novaInfoLiteral = NodeFactory.createLiteral(novaInfo);
                        Triple triple = Triple.create(productNode, HAS_NOVA_MARKER_INFO, novaInfoLiteral);
                        // Utiliser context.add() pour éviter ConcurrentModificationException
                        context.add(triple);
                        added = true;
                    }
                }
            } finally {
                categories.close();
            }

            return added;

        } catch (Exception e) {
            System.err.println("Erreur dans GetNOVAgroupToProduct : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Trouve un marqueur NOVA correspondant à une catégorie
     * @param g Le graphe RDF
     * @param category Le nœud représentant la catégorie
     * @return Une chaîne "Groupe<N>_<type>_<valeur>" ou null si pas de match
     */
    private String findNOVAMarkerForCategory(Graph g, Node category) {
        // 1. Récupérer l'identifiant OFF de la catégorie
        String categoryId = getCategoryIdentifier(g, category);
        if (categoryId == null || categoryId.isEmpty()) {
            return null;
        }

        // 2. Vérifier correspondance directe avec les marqueurs NOVA
        String directMatch = findDirectNOVAMatch(g, categoryId);
        if (directMatch != null) {
            return directMatch;
        }

        // 3. Si pas de correspondance directe, explorer la hiérarchie taxonomique
        return findNOVAMatchViaHierarchy(g, categoryId);
    }

    /**
     * Récupère l'identifiant OFF d'une catégorie
     */
    private String getCategoryIdentifier(Graph g, Node category) {
        // Si la catégorie est un littéral (ex: "en:snacks"), retourner directement
        if (category.isLiteral()) {
            return category.getLiteralLexicalForm();
        }
        
        // Sinon, chercher la propriété offCategoryId
        return getPropertyValue(g, category, OFF_CATEGORY_ID);
    }

    /**
     * Recherche une correspondance directe entre la catégorie et un marqueur NOVA
     */
    private String findDirectNOVAMatch(Graph g, String categoryId) {
        ExtendedIterator<Triple> markers = g.find(Node.ANY, RDF_TYPE, NOVA_MARKER_CLASS);
        try {
            while (markers.hasNext()) {
                Node marker = markers.next().getSubject();
                String markerVal = getPropertyValue(g, marker, MARKER_VALUE);
                if (markerVal == null) continue;
                
                if (categoryId.equals(markerVal)) {
                    return buildNOVAInfoString(g, marker, markerVal);
                }
            }
        } finally {
            markers.close();
        }
        return null;
    }

    /**
     * Recherche un marqueur NOVA via la hiérarchie taxonomique des catégories
     */
    private String findNOVAMatchViaHierarchy(Graph g, String categoryId) {
        // Trouver la catégorie dans la taxonomie OFF
        Node taxonomyCategory = findTaxonomyCategory(g, categoryId);
        if (taxonomyCategory == null) {
            return null;
        }

        // Explorer les parents jusqu'à trouver un match avec un marqueur NOVA
        return exploreParentHierarchy(g, taxonomyCategory, 0, 10);
    }

    /**
     * Trouve une TaxonomyCategory par son offCategoryId
     */
    private Node findTaxonomyCategory(Graph g, String categoryId) {
        ExtendedIterator<Triple> it = g.find(Node.ANY, RDF_TYPE, TAXONOMY_CATEGORY_CLASS);
        try {
            while (it.hasNext()) {
                Node taxoCat = it.next().getSubject();
                String offId = getPropertyValue(g, taxoCat, OFF_CATEGORY_ID);
                if (categoryId.equals(offId)) {
                    return taxoCat;
                }
            }
        } finally {
            it.close();
        }
        return null;
    }

    /**
     * Explore récursivement la hiérarchie des parents pour trouver un marqueur NOVA
     */
    private String exploreParentHierarchy(Graph g, Node taxonomyCategory, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return null; // Limite de profondeur atteinte
        }

        // Récupérer les parents de cette catégorie
        ExtendedIterator<Triple> parents = g.find(taxonomyCategory, HAS_PARENT_CATEGORY, Node.ANY);
        try {
            while (parents.hasNext()) {
                Node parent = parents.next().getObject();
                
                // Récupérer l'offCategoryId du parent
                String parentOffId = getPropertyValue(g, parent, OFF_CATEGORY_ID);
                if (parentOffId != null) {
                    // Vérifier si ce parent correspond à un marqueur NOVA
                    String match = findDirectNOVAMatch(g, parentOffId);
                    if (match != null) {
                        return match; // Match trouvé !
                    }
                    
                    // Sinon, continuer à remonter la hiérarchie
                    String ancestorMatch = exploreParentHierarchy(g, parent, depth + 1, maxDepth);
                    if (ancestorMatch != null) {
                        return ancestorMatch;
                    }
                }
            }
        } finally {
            parents.close();
        }

        return null;
    }

    /**
     * Construit la chaîne d'information NOVA pour un marqueur donné
     */
    private String buildNOVAInfoString(Graph g, Node marker, String markerVal) {
        String markerType = getPropertyValue(g, marker, MARKER_TYPE);
        
        // Récupérer le groupe NOVA via belongsToNOVAGroup
        Node groupNode = getPropertyNode(g, marker, BELONGS_TO_NOVA_GROUP);
        if (groupNode != null) {
            String groupNum = getPropertyValue(g, groupNode, GROUP_NUMBER);
            
            if (groupNum != null && markerType != null && markerVal != null) {
                return "Groupe" + groupNum + "_" + markerType + "_" + markerVal;
            }
        }
        return null;
    }

    /**
     * Récupère la valeur littérale d'une propriété
     */
    private String getPropertyValue(Graph g, Node subject, Node property) {
        ExtendedIterator<Triple> it = g.find(subject, property, Node.ANY);
        try {
            if (it.hasNext()) {
                Node obj = it.next().getObject();
                if (obj.isLiteral()) {
                    return obj.getLiteralLexicalForm();
                } else if (obj.isURI()) {
                    return obj.getURI();
                }
            }
        } finally {
            it.close();
        }
        return null;
    }

    /**
     * Récupère le nœud objet d'une propriété
     */
    private Node getPropertyNode(Graph g, Node subject, Node property) {
        ExtendedIterator<Triple> it = g.find(subject, property, Node.ANY);
        try {
            if (it.hasNext()) {
                return it.next().getObject();
            }
        } finally {
            it.close();
        }
        return null;
    }
}
