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
 * Primitive pour lier les produits aux arguments consommateurs via les métadonnées de rôles.
 * 
 * Architecture:
 * - Product → hasIngredientR → Ingredient → hasRole → AdditiveRole (présence)
 * - Product → hasAdditiveRoleCheck → pas_de_<rôle> (absence)
 * - AdditiveRoleArgumentBinding → aboutRole → AdditiveRole (métadonnées: bindingAgentNameProperty, etc.)
 * - ProductArgument (données enquêtes consommateurs: nameProperty, nameCriterion, etc.)
 * 
 * Signature: compareRoleProperty(?product, ?productArgument)
 * - ?product : ressource Product avec ingrédients et/ou vérifications d'absence
 * - ?productArgument : ressource ProductArgument (données consommateurs)
 * 
 * Retourne true si les rôles des ingrédients du produit (présence ou absence) matchent
 * avec les propriétés du ProductArgument via les métadonnées AdditiveRoleArgumentBinding.
 */
public class CompareRoleProperty extends BaseBuiltin {

    private static final String ncl;
    private static final Node HAS_INGREDIENT_R;
    private static final Node HAS_ROLE;
    private static final Node HAS_ADDITIVE_ROLE_CHECK;
    // Propriété sur ProductArgument (données consommateurs)
    private static final Node PRODUCT_ARG_NAME_PROPERTY;
    // Propriété sur AdditiveRoleArgumentBinding (métadonnées rôle)
    private static final Node BINDING_AGENT_NAME_PROPERTY;
    private static final Node ROLE_REQUIRED;
    private static final Node ABOUT_ROLE;

    static {
        // Initialisation de la configuration
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        HAS_INGREDIENT_R = NodeFactory.createURI(ncl + "hasIngredientR");
        HAS_ROLE = NodeFactory.createURI(ncl + "hasRole");
        HAS_ADDITIVE_ROLE_CHECK = NodeFactory.createURI(ncl + "hasAdditiveRoleCheck");
        PRODUCT_ARG_NAME_PROPERTY = NodeFactory.createURI(ncl + "nameProperty");
        BINDING_AGENT_NAME_PROPERTY = NodeFactory.createURI(ncl + "bindingAgentNameProperty");
        ROLE_REQUIRED = NodeFactory.createURI(ncl + "roleRequired");
        ABOUT_ROLE = NodeFactory.createURI(ncl + "aboutRole");
    }

    @Override
    public String getName() {
        return "compareRoleProperty";
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

        // 1) Récupérer toutes les rôles d'ingrédients du produit.
        //    On collecte les noms locaux des rôles (e.g., "conservateur", "colorant")
        //    depuis les ressources ncl:hasRole attachées aux ingrédients du produit.
        //    Le produit doit avoir des relations ncl:hasIngredientR pointant vers ses ingrédients.
        Set<String> roles = new HashSet<>();

        ExtendedIterator<Triple> itIngR = g.find(productNode, HAS_INGREDIENT_R, Node.ANY);
        while (itIngR.hasNext()) {
            Node ing = itIngR.next().getObject();
            collectRoles(g, ing, roles);
        }
        itIngR.close();

        // 1b) Récupérer les rôles d'absence via ncl:hasAdditiveRoleCheck.
        //     Ces rôles indiquent l'ABSENCE d'additifs (e.g., "pas_de_colorant", "pas_de_conservateur").
        //     On collecte les noms locaux depuis les ressources URI directement attachées au produit.
        ExtendedIterator<Triple> itCheck = g.find(productNode, HAS_ADDITIVE_ROLE_CHECK, Node.ANY);
        while (itCheck.hasNext()) {
            Node val = itCheck.next().getObject();
            if (val.isURI()) {
                String uri = val.getURI();
                String roleName = uri.substring(uri.lastIndexOf('/') + 1);
                roles.add(roleName.toLowerCase().trim());
            }
        }
        itCheck.close();

        // Si aucune rôle (présence ou absence) détectée, le produit ne peut être lié à cet argument.
        if (roles.isEmpty()) return false;

        // 2) Récupérer les propriétés du ProductArgument (données enquêtes consommateurs).
        //    Les ProductArgument définissent leurs caractéristiques via ncl:nameProperty, ncl:nameCriterion, etc.
        //    On collecte toutes les valeurs pour comparaison avec les métadonnées des rôles.
        Set<String> argProperties = new HashSet<>();
        ExtendedIterator<Triple> itPropOld = g.find(argumentNode, PRODUCT_ARG_NAME_PROPERTY, Node.ANY);
        while (itPropOld.hasNext()) {
            Node val = itPropOld.next().getObject();
            if (val.isLiteral()) {
                String txt = val.getLiteralLexicalForm().trim();
                if (!txt.isEmpty()) argProperties.add(txt);
            }
        }
        itPropOld.close();

        // Si l'argument n'a aucune propriété définie, aucun lien ne peut être établi.
        if (argProperties.isEmpty()) return false;

        // 3) Comparer via les métadonnées AdditiveRoleArgumentBinding.
        //    Pour chaque rôle d'ingrédient détectée dans le produit :
        //      a) Construire l'URI de la ressource AdditiveRole (e.g., ncl:conservateur).
        //      b) Trouver tous les AdditiveRoleArgumentBinding liés à cette rôle (via aboutRole).
        //      c) Récupérer les bindingAgentNameProperty de ces AdditiveRoleArgumentBinding.
        //      d) Comparer avec les nameProperty du ProductArgument.
        //         Si correspondance (exacte ou normalisée) → le produit peut être lié à cet argument.
        for (String funcLocal : roles) {
            Node roleUri = NodeFactory.createURI(ncl + funcLocal);
            
            // Rechercher tous les AdditiveRoleArgumentBinding ayant ncl:aboutRole pointant vers roleUri
            ExtendedIterator<Triple> itBinding = g.find(Node.ANY, ABOUT_ROLE, roleUri);
            while (itBinding.hasNext()) {
                Node bindingNode = itBinding.next().getSubject();

                // Extraire les bindingAgentNameProperty de cet AdditiveRoleArgumentBinding
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
                        
                        // Si correspondance trouvée, vérifier roleRequired
                        boolean required = isRoleRequired(g, bindingNode);
                        
                        // Si roleRequired="oui" (ou variantes) → lien accepté
                        if (required) {
                            itBindingProp.close();
                            itBinding.close();
                            return true;
                        }
                        // Si roleRequired n'est pas "oui" → rejet
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
     * Collecte les noms locaux des rôles d'additifs associées à un ingrédient.
     * 
     * Pour chaque ingrédient, on cherche les triples (ingrédient, ncl:hasRole, ?rôle).
     * La propriété ncl:hasRole pointe maintenant vers une ressource URI (e.g., ncl:conservateur)
     * et non plus vers un litéral. On extrait le nom local de l'URI (dernière partie après le dernier '/').
     * 
     * @param g Le graphe RDF contenant les données.
     * @param ingredient Le nœud représentant l'ingrédient.
     * @param out L'ensemble de sortie où seront ajoutés les noms de rôles (en minuscules).
     */
    private void collectRoles(Graph g, Node ingredient, Set<String> out) {
        ExtendedIterator<Triple> itFunc = g.find(ingredient, HAS_ROLE, Node.ANY);
        while (itFunc.hasNext()) {
            Node val = itFunc.next().getObject();
            // ncl:hasRole pointe vers une ressource URI (ncl:<rôle>)
            if (val.isURI()) {
                String uri = val.getURI();
                // Extraire le nom de la rôle depuis l'URI (dernière partie après le dernier '/')
                String roleName = uri.substring(uri.lastIndexOf('/') + 1);
                out.add(roleName.toLowerCase().trim());
            }
        }
        itFunc.close();
    }

    /**
     * Vérifie si la propriété roleRequired d'un AdditiveRoleArgumentBinding
     * indique que la rôle est obligatoire (valeurs: Oui, OUI, oui, Yes, yes, OK, ok, Ok).
     * 
     * Logique de matching :
     * 1. Vérifier d'abord la correspondance nameProperty (bindingAgentNameProperty vs nameProperty)
     * 2. Si pas de correspondance → rejet (return false)
     * 3. Si correspondance ET roleRequired="oui" → lien créé (return true)
     * 4. Si correspondance ET roleRequired≠"oui" → rejet (return false)
     * 
     * Le flag roleRequired filtre donc les liens : seuls les bindings avec roleRequired="oui"
     * ET une correspondance nameProperty créent effectivement un lien Product-ProductArgument.
     * 
     * @param g Le graphe RDF contenant les données.
     * @param bindingNode Le nœud AdditiveRoleArgumentBinding à vérifier.
     * @return true si roleRequired indique "oui", false sinon.
     */
    private boolean isRoleRequired(Graph g, Node bindingNode) {
        ExtendedIterator<Triple> itRequired = g.find(bindingNode, ROLE_REQUIRED, Node.ANY);
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
     * - AdditiveRoleArgumentBinding.bindingAgentNameProperty (métadonnées rôles)
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
