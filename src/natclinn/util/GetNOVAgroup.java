package natclinn.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Builtin pour déterminer le groupe NOVA d'un ingrédient.
 * Usage dans les règles: getNOVAgroup(?ingredient, ?novaInfo)
 * 
 * Nouvelle heuristique basée sur l'ontologie des marqueurs NOVA:
 * - En entrée: un ingrédient
 * - En sortie: une chaîne de caractères concaténant "Groupe<N>_<type>_<valeur>"
 * 
 * Processus:
 * 1. Récupère la valeur de hasIngredientOFF de l'ingrédient (ex: "en:olive-oil")
 * 2. Compare avec les markerValue des instances de NOVAMarker
 * 3. Si match trouvé, retourne: "Groupe<N>_<markerType>_<markerValue>"
 *    où N est le numéro du groupe NOVA (1-4)
 * 
 * But: identifier pour chaque ingrédient le groupe NOVA dans lequel l'ingrédient place son produit
 */
public class GetNOVAgroup extends BaseBuiltin {

    private static final String ncl;
    private static final Node HAS_INGREDIENT_OFF;
    private static final Node RDF_TYPE;
    private static final Node NOVA_MARKER_CLASS;
    private static final Node MARKER_VALUE;
    private static final Node MARKER_TYPE;
    private static final Node BELONGS_TO_NOVA_GROUP;
    private static final Node GROUP_NUMBER;
    private static final Node TAXONOMY_INGREDIENT_CLASS;
    private static final Node OFF_INGREDIENT_ID;
    private static final Node HAS_PARENT_INGREDIENT;

    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        
        HAS_INGREDIENT_OFF = NodeFactory.createURI(ncl + "hasIdIngredientOFF");
        RDF_TYPE = NodeFactory.createURI(org.apache.jena.vocabulary.RDF.type.getURI());
        NOVA_MARKER_CLASS = NodeFactory.createURI(ncl + "NOVAMarker");
        MARKER_VALUE = NodeFactory.createURI(ncl + "markerValue");
        MARKER_TYPE = NodeFactory.createURI(ncl + "markerType");
        BELONGS_TO_NOVA_GROUP = NodeFactory.createURI(ncl + "belongsToNOVAGroup");
        GROUP_NUMBER = NodeFactory.createURI(ncl + "groupNumber");
        TAXONOMY_INGREDIENT_CLASS = NodeFactory.createURI(ncl + "TaxonomyIngredient");
        OFF_INGREDIENT_ID = NodeFactory.createURI(ncl + "offIngredientId");
        HAS_PARENT_INGREDIENT = NodeFactory.createURI(ncl + "hasParentIngredient");
    }

    @Override
    public String getName() {
        return "getNOVAgroup";
    }

    @Override
    public int getArgLength() {
        return 2; // ingredient, novaInfo
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);

        Node ingredient = getArg(0, args, context);
        if (!(ingredient.isURI() || ingredient.isBlank())) return false;

        try {
            String novaInfo = findNOVAMarkerMatch(context.getGraph(), ingredient);
            if (novaInfo != null) {
                Node novaInfoNode = NodeFactory.createLiteral(novaInfo);
                return context.getEnv().bind(args[1], novaInfoNode);
            }
        } catch (Exception e) {
            System.err.println("GetNOVAgroup error pour " + ingredient + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Trouve un marqueur NOVA correspondant à l'ingrédient
     * @param g Le graphe RDF
     * @param ingredient Le nœud représentant l'ingrédient
     * @return Une chaîne "Groupe<N>_<type>_<valeur>" ou null si pas de match
     */
    private String findNOVAMarkerMatch(Graph g, Node ingredient) {
        // 1. Récupérer la valeur hasIngredientOFF de l'ingrédient
        String ingredientOFF = getPropertyValue(g, ingredient, HAS_INGREDIENT_OFF);
        if (ingredientOFF == null || ingredientOFF.isEmpty()) {
            return null;
        }

        // 2. Vérifier correspondance directe avec les marqueurs NOVA
        String directMatch = findDirectNOVAMatch(g, ingredientOFF);
        if (directMatch != null) {
            return directMatch;
        }

        // 3. Si pas de correspondance directe, explorer la hiérarchie taxonomique
        return findNOVAMatchViaHierarchy(g, ingredientOFF);
    }

    /**
     * Recherche une correspondance directe entre l'ingrédient et un marqueur NOVA
     */
    private String findDirectNOVAMatch(Graph g, String ingredientOFF) {
        ExtendedIterator<Triple> markers = g.find(Node.ANY, RDF_TYPE, NOVA_MARKER_CLASS);
        try {
            while (markers.hasNext()) {
                Node marker = markers.next().getSubject();
                String markerVal = getPropertyValue(g, marker, MARKER_VALUE);
                if (markerVal == null) continue;
                
                if (ingredientOFF.equals(markerVal)) {
                    return buildNOVAInfoString(g, marker, markerVal);
                }
            }
        } finally {
            markers.close();
        }
        return null;
    }

    /**
     * Recherche un marqueur NOVA via la hiérarchie taxonomique des ingrédients
     * Explore TOUS les parents et retourne celui avec le groupe NOVA le plus élevé
     */
    private String findNOVAMatchViaHierarchy(Graph g, String ingredientOFF) {
        // Trouver l'ingrédient dans la taxonomie OFF
        Node taxonomyIngredient = findTaxonomyIngredient(g, ingredientOFF);
        if (taxonomyIngredient == null) {
            return null;
        }

        // Explorer tous les parents et collecter tous les matches
        List<NOVAMatch> allMatches = new ArrayList<>();
        exploreAllParents(g, taxonomyIngredient, allMatches, 0, 10);

        // Retourner le match avec le groupe NOVA le plus élevé
        return selectHighestNOVAGroup(allMatches);
    }

    /**
     * Classe interne pour stocker un match NOVA avec son groupe et sa profondeur
     */
    private static class NOVAMatch {
        String infoString;
        int groupNumber;
        int depth; // Profondeur dans la hiérarchie (plus petit = plus proche de la racine)

        NOVAMatch(String infoString, int groupNumber, int depth) {
            this.infoString = infoString;
            this.groupNumber = groupNumber;
            this.depth = depth;
        }
    }

    /**
     * Trouve un TaxonomyIngredient par son offIngredientId
     */
    private Node findTaxonomyIngredient(Graph g, String ingredientId) {
        ExtendedIterator<Triple> it = g.find(Node.ANY, RDF_TYPE, TAXONOMY_INGREDIENT_CLASS);
        try {
            while (it.hasNext()) {
                Node taxoIng = it.next().getSubject();
                String offId = getPropertyValue(g, taxoIng, OFF_INGREDIENT_ID);
                if (ingredientId.equals(offId)) {
                    return taxoIng;
                }
            }
        } finally {
            it.close();
        }
        return null;
    }

    /**
     * Explore récursivement TOUS les parents pour collecter tous les marqueurs NOVA possibles
     */
    private void exploreAllParents(Graph g, Node taxonomyIngredient, List<NOVAMatch> matches, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return; // Limite de profondeur atteinte
        }

        // Récupérer tous les parents de cet ingrédient
        ExtendedIterator<Triple> parents = g.find(taxonomyIngredient, HAS_PARENT_INGREDIENT, Node.ANY);
        try {
            while (parents.hasNext()) {
                Node parent = parents.next().getObject();
                
                // Récupérer l'offIngredientId du parent
                String parentOffId = getPropertyValue(g, parent, OFF_INGREDIENT_ID);
                if (parentOffId != null) {
                    // Vérifier si ce parent correspond à un marqueur NOVA
                    String matchInfo = findDirectNOVAMatch(g, parentOffId);
                    if (matchInfo != null) {
                        // Extraire le numéro de groupe depuis la chaîne "Groupe<N>_..."
                        int groupNum = extractGroupNumber(matchInfo);
                        if (groupNum > 0) {
                            matches.add(new NOVAMatch(matchInfo, groupNum, depth));
                        }
                    }
                    
                    // Continuer à remonter la hiérarchie pour ce parent
                    exploreAllParents(g, parent, matches, depth + 1, maxDepth);
                }
            }
        } finally {
            parents.close();
        }
    }

    /**
     * Extrait le numéro de groupe depuis une chaîne "Groupe<N>_type_valeur"
     */
    private int extractGroupNumber(String novaInfo) {
        if (novaInfo == null || !novaInfo.startsWith("Groupe")) {
            return 0;
        }
        try {
            int underscorePos = novaInfo.indexOf('_');
            if (underscorePos > 6) {
                String numStr = novaInfo.substring(6, underscorePos);
                return Integer.parseInt(numStr);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }

    /**
     * Sélectionne le match avec le groupe NOVA le plus élevé (4 > 3 > 2 > 1)
     * En cas d'égalité, prend le parent le plus élevé dans la hiérarchie (depth le plus faible)
     */
    private String selectHighestNOVAGroup(List<NOVAMatch> matches) {
        if (matches.isEmpty()) {
            return null;
        }

        NOVAMatch highest = matches.get(0);
        for (NOVAMatch match : matches) {
            // Priorité 1: Groupe NOVA le plus élevé
            if (match.groupNumber > highest.groupNumber) {
                highest = match;
            } 
            // Priorité 2: En cas d'égalité, parent le plus élevé (depth minimal)
            else if (match.groupNumber == highest.groupNumber && match.depth < highest.depth) {
                highest = match;
            }
        }
        return highest.infoString;
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
