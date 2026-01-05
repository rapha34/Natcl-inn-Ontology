package natclinn.util;

import java.util.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Primitive Jena personnalisée : GetCategoriesToProduct
 * 
 * Récupère toutes les catégories intermédiaires entre une catégorie racine et une catégorie feuille
 * dans la hiérarchie OFF, et rattache les catégories intermédiaires au produit.
 * 
 * Utilisation en règle :
 *   GetCategoriesToProduct(?product, ?rootCategory, ?leafCategory)
 * 
 * Cette primitive :
 * 1. Prend un produit avec ses propriétés hasRootCategory et hasLeafCategory
 * 2. Traverse la hiérarchie OFF (hasChildIngredient) entre root et leaf
 * 3. Identifie les catégories intermédiaires
 * 4. Ajoute la relation hasIntermediaryCategory entre le produit et chaque catégorie intermédiaire
 */
public class GetCategoriesToProduct extends BaseBuiltin {

    private static final String ncl;
    private static final String rdf;
    private static final Node HAS_INTERMEDIARY_CATEGORY;
    private static final Node HAS_FIRST_CATEGORY;
    private static final Node HAS_LAST_CATEGORY;
    private static final Node HAS_CHILD_INGREDIENT;
    private static final Node HAS_CHILD_CATEGORY;
    private static final Node OFF_CATEGORY_ID;
    private static final Node OFF_INGREDIENT_ID;
    private static final Node RDF_TYPE;
    private static final Node TAXO_CATEGORY;
    private static final Node TAXO_INGREDIENT;

    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        rdf = NatclinnConf.rdf;
        HAS_INTERMEDIARY_CATEGORY = NodeFactory.createURI(ncl + "hasIntermediaryCategory");
        HAS_FIRST_CATEGORY = NodeFactory.createURI(ncl + "hasFirstCategory");
        HAS_LAST_CATEGORY = NodeFactory.createURI(ncl + "hasLastCategory");
        HAS_CHILD_INGREDIENT = NodeFactory.createURI(ncl + "hasChildIngredient");
        HAS_CHILD_CATEGORY = NodeFactory.createURI(ncl + "hasChildCategory");
        OFF_CATEGORY_ID = NodeFactory.createURI(ncl + "offCategoryId");
        OFF_INGREDIENT_ID = NodeFactory.createURI(ncl + "offIngredientId");
        RDF_TYPE = NodeFactory.createURI(rdf + "type");
        TAXO_CATEGORY = NodeFactory.createURI(ncl + "TaxonomyCategory");
        TAXO_INGREDIENT = NodeFactory.createURI(ncl + "TaxonomyIngredient");
    }

    @Override
    public String getName() {
        return "GetCategoriesToProduct";
    }

    @Override
    public int getArgLength() {
        return 3;  // ?product, ?rootCategory, ?leafCategory
    }

    /**
     * Exécute la primitive pour trouver et créer les relations de catégories intermédiaires.
     * 
     * @param args : [?product, ?rootCategory, ?leafCategory]
     * @param context : le contexte de l'inférence Jena
     * @return true si au moins une catégorie intermédiaire a été trouvée
     */
    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        
        // Vérifier que tous les arguments sont liés (ground)
        if (!args[0].isConcrete() || !args[1].isConcrete() || !args[2].isConcrete()) {
            return false;
        }

        Node productNode = args[0];
        Node rootCategoryNode = args[1];
        Node leafCategoryNode = args[2];

        try {
            Graph graph = context.getGraph();

            // Résoudre root/leaf: si littéraux, mapper vers les ressources de taxonomie via offCategoryId/offIngredientId
            Node resolvedRoot = resolveTaxonomyResource(graph, rootCategoryNode);
            Node resolvedLeaf = resolveTaxonomyResource(graph, leafCategoryNode);
            if (resolvedRoot == null || resolvedLeaf == null) {
                return false;
            }

            // Ajouter les relations hasFirstCategory et hasLastCategory au produit
            Triple triple = Triple.create(productNode, HAS_FIRST_CATEGORY, resolvedRoot);
            context.add(triple);
            triple = Triple.create(productNode, HAS_LAST_CATEGORY, resolvedLeaf);
            context.add(triple);

            // Choisir la bonne propriété hiérarchique en fonction du type ou des arcs sortants
            Node childProp = chooseHierarchyProperty(graph, resolvedRoot);

            // Trouver le chemin exact (root -> leaf)
            List<Node> path = shortestPath(graph, resolvedRoot, resolvedLeaf, childProp);
            if (path.isEmpty()) {
                return false;
            }

            // Ajouter les relations hasIntermediaryCategory au produit
            boolean added = false;
            // Ajouter les nœuds internes du chemin (exclure extrémités)
            for (int i = 1; i < path.size() - 1; i++) {
                Node intermediary = path.get(i);
                triple = Triple.create(productNode, HAS_INTERMEDIARY_CATEGORY, intermediary);
                // Utiliser context.add() pour éviter ConcurrentModificationException pendant l'inférence
                context.add(triple);
                added = true;
            }

            return added;

        } catch (Exception e) {
            System.err.println("Erreur dans GetCategoriesToProduct : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Résout un nœud racine/feuille potentiel (littéral ou URI) vers une ressource de taxonomie
    private static Node resolveTaxonomyResource(Graph g, Node n) {
        if (n.isURI() || n.isBlank()) {
            return n; // déjà une ressource
        }
        // Essayer via offCategoryId
        ExtendedIterator<Triple> it = g.find(Node.ANY, OFF_CATEGORY_ID, n);
        if (it.hasNext()) {
            Node subj = it.next().getSubject();
            it.close();
            return subj;
        }
        it.close();
        // Sinon via offIngredientId
        it = g.find(Node.ANY, OFF_INGREDIENT_ID, n);
        if (it.hasNext()) {
            Node subj = it.next().getSubject();
            it.close();
            return subj;
        }
        it.close();
        return null;
    }

    // Choisit la propriété hiérarchique pertinente pour la ressource (catégorie vs ingrédient)
    private static Node chooseHierarchyProperty(Graph g, Node root) {
        // Si typée explicitement
        boolean isCat = g.find(root, RDF_TYPE, TAXO_CATEGORY).hasNext();
        boolean isIng = g.find(root, RDF_TYPE, TAXO_INGREDIENT).hasNext();
        if (isCat && !isIng) return HAS_CHILD_CATEGORY;
        if (isIng && !isCat) return HAS_CHILD_INGREDIENT;
        // Sinon heuristique: regarder les arcs sortants
        if (g.find(root, HAS_CHILD_CATEGORY, Node.ANY).hasNext()) return HAS_CHILD_CATEGORY;
        if (g.find(root, HAS_CHILD_INGREDIENT, Node.ANY).hasNext()) return HAS_CHILD_INGREDIENT;
        // Par défaut, tenter catégories
        return HAS_CHILD_CATEGORY;
    }

    // Calcule le plus court chemin root->leaf et renvoie la liste ordonnée des nœuds
    private static List<Node> shortestPath(Graph g, Node root, Node leaf, Node childProp) {
        Map<Node, Node> parent = new HashMap<>();
        Queue<Node> q = new LinkedList<>();
        Set<Node> visited = new HashSet<>();
        q.add(root);
        visited.add(root);
        boolean found = false;
        while (!q.isEmpty()) {
            Node cur = q.poll();
            if (cur.equals(leaf)) { found = true; break; }
            ExtendedIterator<Triple> it = g.find(cur, childProp, Node.ANY);
            while (it.hasNext()) {
                Node child = it.next().getObject();
                if (!visited.contains(child)) {
                    visited.add(child);
                    parent.put(child, cur);
                    q.add(child);
                }
            }
            it.close();
        }
        if (!found) return Collections.emptyList();
        // Reconstruire le chemin
        LinkedList<Node> path = new LinkedList<>();
        Node step = leaf;
        while (step != null) {
            path.addFirst(step);
            step = parent.get(step);
        }
        // parent(root) est null, donc root sera inclus en tête
        return path;
    }
}
