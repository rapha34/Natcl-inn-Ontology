package natclinn.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.rulesys.BindingEnvironment;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Builtin Jena pour mapper un ingrédient sans code OFF via son label
 * en utilisant les heuristiques externalisées dans NatclinnUtil.
 *
 * Usage dans les règles:
 *   findIngredientOFFByLabel(?ingredient, ?offId)
 *   -> ajoute (ncl:hasIdIngredientOFF ?offId) si un match >= 0.78 est trouvé
 */
public class FindIngredientOFFByLabel extends BaseBuiltin {

    private static final String ncl;
    private static final String prefix;
    private static final Node RDF_TYPE;
    private static final Node INGREDIENT_CLASS;
    private static final Node HAS_ID_INGREDIENT_OFF;
    private static final Node TAXONOMY_INGREDIENT_CLASS;
    private static final Node OFF_INGREDIENT_ID;
    private static final Node RDFS_LABEL;

    static {
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
		prefix = NatclinnConf.queryPrefix;
        RDF_TYPE = NodeFactory.createURI(org.apache.jena.vocabulary.RDF.type.getURI());
        INGREDIENT_CLASS = NodeFactory.createURI(ncl + "Ingredient");
        HAS_ID_INGREDIENT_OFF = NodeFactory.createURI(ncl + "hasIdIngredientOFF");
        TAXONOMY_INGREDIENT_CLASS = NodeFactory.createURI(ncl + "TaxonomyIngredient");
        OFF_INGREDIENT_ID = NodeFactory.createURI(ncl + "offIngredientId");
        RDFS_LABEL = NodeFactory.createURI(org.apache.jena.vocabulary.RDFS.label.getURI());
    }

    @Override
    public String getName() {
        return "findIngredientOFFByLabel";
    }

    @Override
    public int getArgLength() {
        return 2; // ingredient, offId
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        Node ingredientNode = getArg(0, args, context);
        if (!(ingredientNode.isURI() || ingredientNode.isBlank())) return false;

        Model model = ModelFactory.createModelForGraph(context.getGraph());
        Resource ingredientRes = ingredientNode.isURI() ? model.createResource(ingredientNode.getURI()) : model.asRDFNode(ingredientNode).asResource();
        
        // Vérifier que c'est bien un ncl:Ingredient
        if (!hasType(model, ingredientRes, INGREDIENT_CLASS)) {
            return false;
        }

        // Si l'ingrédient a déjà un code OFF, ne rien faire
        if (hasProperty(model, ingredientRes, HAS_ID_INGREDIENT_OFF)) {
            return false;
        }

        // Récupérer le meilleur label
        Literal labelLit = NatclinnUtil.getBestStringLiteral(ingredientRes);
        if (labelLit == null) {
            return false;
        }
        String ingredientLabel = labelLit.getLexicalForm();
        // Charger la taxonomie des ingrédients avec offId et labels
        List<Map<String, Object>> taxonomy = loadIngredientTaxonomy(model);
        if (taxonomy.isEmpty()) {
            return false;
        }      
        // Matching flou via utilitaire
        NatclinnUtil.IngredientMatch match = NatclinnUtil.findIngredientInTaxonomy(ingredientLabel, taxonomy);
        if (match == null) {
            return false;
        }

        // Récupérer offId depuis la ressource taxonomique matchée
        String offId = getOffIdForUri(model, match.taxonomyUri);
        if (offId == null || offId.isEmpty()) {
            return false;
        }

        // Binder la sortie
        Node offIdNode = NodeFactory.createLiteral(offId);
        BindingEnvironment env = context.getEnv();
        return env.bind(args[1], offIdNode);
    }

    private boolean hasType(Model m, Resource r, Node typeNode) {
        ExtendedIterator<Triple> it = m.getGraph().find(r.asNode(), RDF_TYPE, typeNode);
        try {
            return it.hasNext();
        } finally {
            it.close();
        }
    }

    private boolean hasProperty(Model m, Resource r, Node property) {
        ExtendedIterator<Triple> it = m.getGraph().find(r.asNode(), property, Node.ANY);
        try {
            return it.hasNext();
        } finally {
            it.close();
        }
    }

    private String getOffIdForUri(Model model, String taxonomyUri) {
        if (taxonomyUri == null) return null;
        ExtendedIterator<Triple> it = model.getGraph().find(NodeFactory.createURI(taxonomyUri), OFF_INGREDIENT_ID, Node.ANY);
        try {
            if (it.hasNext()) {
                Node obj = it.next().getObject();
                if (obj.isLiteral()) {
                    return obj.getLiteralLexicalForm();
                }
            }
        } finally {
            it.close();
        }
        return null;
    }

    private List<Map<String, Object>> loadIngredientTaxonomy(Model model) {
        List<Map<String, Object>> taxonomy = new ArrayList<>();
        try {
            String sparqlQuery = prefix +
                "SELECT ?ingredient ?label ?offId WHERE {\n" +
                "  ?ingredient rdf:type ncl:TaxonomyIngredient .\n" +
                "  OPTIONAL { ?ingredient rdfs:label ?label } .\n" +
                "  OPTIONAL { ?ingredient ncl:offIngredientId ?offId } .\n" +
                "  FILTER (BOUND(?label) && lang(?label) = 'fr')\n" +
                "} LIMIT 20000";

            Query query = QueryFactory.create(sparqlQuery);
            try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    QuerySolution qs = rs.nextSolution();
                    String uri = qs.getResource("ingredient").getURI();
                    String label = qs.contains("label") ? qs.getLiteral("label").getString() : null;
                    String offId = qs.contains("offId") ? qs.getLiteral("offId").getString() : null;
                    if (label == null) continue;
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("uri", uri);
                    entry.put("label", label);
                    entry.put("offId", offId);
                    taxonomy.add(entry);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement taxonomie ingrédients: " + e.getMessage());
        }
        return taxonomy;
    }
}
