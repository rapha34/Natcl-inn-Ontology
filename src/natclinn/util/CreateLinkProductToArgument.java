package natclinn.util;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Primitive pour lier les produits aux arguments consommateurs via les métadonnées des tags produits.
 * Crée des instances LinkToArgument pour établir la relation Product -> LinkToArgument -> ProductArgument.
 *
 * Architecture:
 * - Product  hasTag  Tag (présence de types d'emballage)
 * - Product  hasTagCheck  sans_plastique / sans_emballage / etc. (absence/vérifications)
 * - TagArgumentBinding  aboutTag  Tag (métadonnées: bindingAgentNameProperty, bindingAgentKeywords, etc.)
 * - ProductArgument (données enquêtes consommateurs: nameProperty, aim, assertion, verbatim, valueProperty, etc.)
 *
 * Signature: createLinkProductToArgument(?product)
 * - ?product : ressource Product avec tags et/ou vérifications d'absence
 *
 * ALGORITHME DE SCORING :
 * La primitive utilise un système de scoring multi-critères pour sélectionner les meilleurs arguments :
 * 1. Pour chaque tag, récupère TOUS les mots-clés du TagArgumentBinding (keywordsStr complet)
 * 2. Pour chaque ProductArgument, calcule un score de matching global basé sur :
 *    - Couverture des keywords (% de keywords qui matchent avec argProperties)
 *    - Couverture des argProperties (% d'argProperties qui matchent avec keywords)
 *    - Qualité du matching (exact > normalisé > token overlap > containment)
 *    - Longueur des tokens matchés (mots longs > mots courts)
 * 3. Trie les arguments par score décroissant
 * 4. Ne crée des liens QUE pour les arguments avec score  seuil minimal (0.6)
 *
 * Ceci évite la création excessive de liens basés sur des matchings faibles ou isolés.
 */
public class CreateLinkProductToArgument extends BaseBuiltin {

    // ==================== CONSTANTES DE SCORING ====================

    /** Seuil minimal de score pour créer un lien (0.0 à 1.0) */
    private static final double SCORE_THRESHOLD = 0.47;

    /** Poids de la couverture des keywords dans le score final */
    private static final double WEIGHT_KEYWORD_COVERAGE = 0.4;

    /** Poids de la couverture des argProperties dans le score final */
    private static final double WEIGHT_PROPERTY_COVERAGE = 0.3;

    /** Poids de la qualité moyenne du matching dans le score final */
    private static final double WEIGHT_MATCH_QUALITY = 0.2;

    /** Poids du bonus de longueur des tokens dans le score final */
    private static final double WEIGHT_TOKEN_LENGTH = 0.1;

    /** Pondération pour match exact */
    private static final double QUALITY_EXACT = 1.0;

    /** Pondération pour match normalisé */
    private static final double QUALITY_NORMALIZED = 0.9;

    /** Pondération pour match par token overlap */
    private static final double QUALITY_TOKEN_OVERLAP = 0.7;

    /** Pondération pour match par containment */
    private static final double QUALITY_CONTAINMENT = 0.5;

    /** Longueur minimale pour considérer un token comme significatif */
    private static final int MIN_SIGNIFICANT_TOKEN_LENGTH = 5;

    private static final String ncl;
    private static final String rdf;
    private static final String skos;
    private static final Node HAS_RDF_TYPE;
    private static final Node HAS_TAG;
    private static final Node TAG_LABEL;
    private static final Node HAS_TAG_CHECK;
    // Propriété sur ProductArgument (données consommateurs)
    private static final Node PRODUCT_ARG_ATTRIBUT;
    private static final Node PRODUCT_ARG_ATTRIBUT_LABEL;
    private static final Node PRODUCT_ARG_DESCRIPTION;
    private static final Node PRODUCT_ARG_VERBATIM;

    // Propriété sur TagArgumentBinding (métadonnées tags)
    private static final Node TAG_NAME_PROPERTY;
    private static final Node TAG_VALUE_PROPERTY;
    private static final Node TAG_AIM;
    private static final Node TAG_ASSERTION;
    private static final Node TAG_BINDING_KEYWORDS;
    private static final Node ABOUT_TAG;
    private static final Node HAS_LINK_TO_ARGUMENT;
    private static final Node HAS_REFERENCE_PRODUCT_ARGUMENT;
    private static final Node HAS_TAG_INITIATOR;
    // Nouvelles propriétés pour la configuration depuis l'ontologie pivot
    private static final Node TAG_FILTERING_RULE;
    private static final Node TAG_SYNONYM_LABELS;
    private static final Node TAG_SELECTION_RULE;
    private static final Node TAG_ANTINOMY_PROPERTIES;
    private static final Set<String> STOPWORDS = new HashSet<>(java.util.Arrays.asList(
            "le", "la", "les", "un", "une", "des", "de", "du", "que", "qui", "quoi",
            "et", "ou", "mais", "donc", "car", "ni", "or",
            "ce", "cet", "cette", "ces", "se", "sa", "son", "ses",
            "me", "te", "nous", "vous", "lui", "leur",
            "pas", "plus", "non", "oui", "si", "bien",
            "tout", "tous", "toute", "toutes",
            "mon", "ton", "ma", "ta", "mes", "tes",
            "au", "aux", "en", "dans", "sur", "sous", "pour", "par", "avec", "sans",
            "je", "tu", "il", "elle", "on", "ils", "elles",
            "avoir", "être", "fait", "faire", "être", "ai", "as", "est", "sont", "été"
    ));

    static {
        // Initialisation de la configuration
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        rdf = NatclinnConf.rdf;
        skos = NatclinnConf.skos;
        HAS_RDF_TYPE = NodeFactory.createURI(rdf + "type");
        HAS_TAG = NodeFactory.createURI(ncl + "hasTag");
        HAS_TAG_CHECK = NodeFactory.createURI(ncl + "hasTagCheck");
        //
        TAG_LABEL = NodeFactory.createURI(skos + "prefLabel");
        TAG_NAME_PROPERTY = NodeFactory.createURI(ncl + "tagNameProperty");
        TAG_AIM = NodeFactory.createURI(ncl + "tagAim");
        TAG_ASSERTION = NodeFactory.createURI(ncl + "tagAssertion");
        TAG_VALUE_PROPERTY = NodeFactory.createURI(ncl + "tagValueProperty");
        TAG_BINDING_KEYWORDS = NodeFactory.createURI(ncl + "tagBindingKeywords");
        // Nouvelles propriétés de configuration
        TAG_FILTERING_RULE = NodeFactory.createURI(ncl + "tagFilteringRule");
        TAG_SYNONYM_LABELS = NodeFactory.createURI(ncl + "tagSynonymLabels");
        TAG_SELECTION_RULE = NodeFactory.createURI(ncl + "tagSelectionRule");
        TAG_ANTINOMY_PROPERTIES = NodeFactory.createURI(ncl + "tagAntinomyProperties");
        ABOUT_TAG = NodeFactory.createURI(ncl + "aboutTag");
        HAS_LINK_TO_ARGUMENT = NodeFactory.createURI(ncl + "hasLinkToArgument");
        HAS_REFERENCE_PRODUCT_ARGUMENT = NodeFactory.createURI(ncl + "hasReferenceProductArgument");
        HAS_TAG_INITIATOR = NodeFactory.createURI(ncl + "hasTagInitiator");

        PRODUCT_ARG_ATTRIBUT = NodeFactory.createURI(ncl + "hasAttribut");
        PRODUCT_ARG_ATTRIBUT_LABEL = NodeFactory.createURI(skos + "prefLabel");
        PRODUCT_ARG_DESCRIPTION = NodeFactory.createURI(skos + "prefLabel");
        PRODUCT_ARG_VERBATIM = NodeFactory.createURI(ncl + "verbatim");
    }

    @Override
    public String getName() {
        return "createLinkProductToArgument";
    }

    @Override
    public int getArgLength() {
        return 1; // product only (arguments retrieved internally)
    }

    // ==================== CLASSES INTERNES DE SUPPORT ====================

    /**
     * Stocke le résultat global de matching pour un ProductArgument.
     */
    private static class ArgumentMatch implements Comparable<ArgumentMatch> {
        /** Le noeud ProductArgument */
        Node argumentNode;
        /** Score global de matching (0.0 à 1.0) */
        double score;
        /** Nombre de keywords qui ont matché */
        int nbKeywordsMatched;
        /** Nombre total de keywords */
        int nbKeywordsTotal;
        /** Nombre de propriétés d'argument qui ont matché */
        int nbPropertiesMatched;
        /** Nombre total de propriétés d'argument */
        int nbPropertiesTotal;
        /** Paires (keyword  argProperty) matchées pour debug */
        List<String> matchedPairs;
        /** Somme des qualités de matching (pour moyenne) */
        double totalMatchQuality;
        /** Somme des longueurs des tokens matchés */
        int totalTokenLength;

        ArgumentMatch(Node argumentNode) {
            this.argumentNode = argumentNode;
            this.score = 0.0;
            this.nbKeywordsMatched = 0;
            this.nbKeywordsTotal = 0;
            this.nbPropertiesMatched = 0;
            this.nbPropertiesTotal = 0;
            this.matchedPairs = new ArrayList<>();
            this.totalMatchQuality = 0.0;
            this.totalTokenLength = 0;
        }

        /** Calcule le score final basé sur les critères collectés. */
        void calculateScore() {
            double keywordCoverage = nbKeywordsTotal > 0
                    ? (double) nbKeywordsMatched / nbKeywordsTotal
                    : 0.0;

            double propertyCoverage = nbPropertiesTotal > 0
                    ? (double) nbPropertiesMatched / nbPropertiesTotal
                    : 0.0;

            double avgMatchQuality = nbKeywordsMatched > 0
                    ? totalMatchQuality / nbKeywordsMatched
                    : 0.0;

            double avgTokenLength = nbKeywordsMatched > 0
                    ? (double) totalTokenLength / nbKeywordsMatched
                    : 0.0;
            double tokenLengthBonus = Math.min(1.0, avgTokenLength / MIN_SIGNIFICANT_TOKEN_LENGTH);

            this.score = (WEIGHT_KEYWORD_COVERAGE * keywordCoverage)
                    + (WEIGHT_PROPERTY_COVERAGE * propertyCoverage)
                    + (WEIGHT_MATCH_QUALITY * avgMatchQuality)
                    + (WEIGHT_TOKEN_LENGTH * tokenLengthBonus);
        }

        /** Tri décroissant sur le score. */
        @Override
        public int compareTo(ArgumentMatch other) {
            return Double.compare(other.score, this.score);
        }

        /** Représentation textuelle pour le logging. */
        @Override
        public String toString() {
            double avgQuality = nbKeywordsMatched > 0 ? totalMatchQuality / nbKeywordsMatched : 0.0;
            return String.format("Score=%.3f (%d/%d keywords, %d/%d properties, avgQuality=%.2f)",
                    score, nbKeywordsMatched, nbKeywordsTotal,
                    nbPropertiesMatched, nbPropertiesTotal, avgQuality);
        }
    }

    /** Résultat d'un matching individuel keyword  property. */
    private static class MatchResult {
        String matchedProperty;
        double quality;

        MatchResult(String matchedProperty, double quality) {
            this.matchedProperty = matchedProperty;
            this.quality = quality;
        }
    }

    // ==================== LOGIQUE PRINCIPALE ====================

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);

        Node productNode = getArg(0, args, context);
        //System.out.println("Comparing tag for product: " + productNode.toString());

        if (!productNode.isURI() && !productNode.isBlank()) {
            return false;
        }

        Graph g = context.getGraph();

        // 1) Récupérer tous les tags du produit (présence et vérifications d'absence)
        Set<String> tags = new HashSet<>();

        ExtendedIterator<Triple> itType = g.find(productNode, HAS_TAG, Node.ANY);
        while (itType.hasNext()) {
            Node val = itType.next().getObject();
            if (val.isURI()) {
                tags.add(val.getURI());
            }
        }
        itType.close();

        ExtendedIterator<Triple> itCheck = g.find(productNode, HAS_TAG_CHECK, Node.ANY);
        while (itCheck.hasNext()) {
            Node val = itCheck.next().getObject();
            if (val.isURI()) {
                tags.add(val.getURI());
            }
        }
        itCheck.close();

        if (tags.isEmpty()) {
            return false; // Aucun tag => aucun lien possible
        }

        // 2) Récupérer TOUS les ProductArgument
        List<Node> productArguments = new ArrayList<>();
        ExtendedIterator<Triple> itArgs = g.find(Node.ANY, HAS_RDF_TYPE, NodeFactory.createURI(ncl + "ProductArgument"));
        while (itArgs.hasNext()) {
            Node argNode = itArgs.next().getSubject();
            if (argNode.isURI() || argNode.isBlank()) {
                productArguments.add(argNode);
            }
        }
        itArgs.close();

        // 3) Pour chaque tag, calculer le score de TOUS les arguments et créer des liens au-dessus d'un seuil
        for (String tag : tags) {
            // 3.1) Récupérer les keywords du TagArgumentBinding pour ce tag
            String keywordsStr = getTagBindingKeywords(g, tag);
            if (keywordsStr == null || keywordsStr.trim().isEmpty()) {
                //System.out.println("   Aucun keyword trouvé pour ce tag - skip");
                continue;
            }

            String[] keywords = keywordsStr.split(",");
            List<String> keywordList = new ArrayList<>();
            for (String kw : keywords) {
                String cleaned = kw.trim();
                if (!cleaned.isEmpty()) {
                    keywordList.add(cleaned);
                }
            }

            if (keywordList.isEmpty()) {
                //System.out.println("   Aucun keyword valide après parsing - skip");
                continue;
            }

            // System.out.println("\n====================================================");
            // System.out.println(" Processing tag: " + tag);
            // System.out.println("====================================================");
            // System.out.println("  Keywords du tag: " + keywordList);
            // System.out.println("  Évaluation de " + productArguments.size() + " arguments...\n");

            List<ArgumentMatch> matches = new ArrayList<>();

            // 3.2) Calculer le score pour chaque argument
            for (Node productArgument : productArguments) {
                Set<String> argProperties = collectProductArgumentProperties(g, productArgument, tag);
                if (argProperties.isEmpty()) {
                    continue; // Argument non pertinent pour ce tag
                }

                ArgumentMatch match = new ArgumentMatch(productArgument);
                match.nbKeywordsTotal = keywordList.size();
                match.nbPropertiesTotal = argProperties.size();

                Set<String> matchedKeywords = new HashSet<>();
                Set<String> matchedProperties = new HashSet<>();

                for (String keyword : keywordList) {
                    MatchResult bestMatch = findBestMatch(keyword, argProperties);
                    if (bestMatch != null && bestMatch.quality > 0) {
                        matchedKeywords.add(keyword);
                        matchedProperties.add(bestMatch.matchedProperty);
                        match.totalMatchQuality += bestMatch.quality;
                        match.totalTokenLength += keyword.length();
                        match.matchedPairs.add(keyword + "  " + bestMatch.matchedProperty);
                    }
                }

                match.nbKeywordsMatched = matchedKeywords.size();
                match.nbPropertiesMatched = matchedProperties.size();
                match.calculateScore();

                if (match.nbKeywordsMatched > 0) {
                    matches.add(match);
                }
            }

            matches.sort(null); // Tri par compareTo (score décroissant)

            // System.out.println("  Résultats de scoring (" + matches.size() + " arguments matchés) :");
            // System.out.println("  " + repeat("-", 70));

            int linksCreated = 0;
            for (int i = 0; i < matches.size(); i++) {
                ArgumentMatch match = matches.get(i);
                String argumentUri = match.argumentNode.isURI() ? match.argumentNode.getURI() : match.argumentNode.toString();

                String status = match.score >= SCORE_THRESHOLD ? " ACCEPTÉ" : " REJETÉ";
                    // System.out.println("  [" + (i + 1) + "] " + status + " - " + match);
                    // System.out.println("      Argument: " + argumentUri);

                if (match.score >= SCORE_THRESHOLD && !match.matchedPairs.isEmpty()) {
                    // System.out.println("      Matchings:");
                    for (String pair : match.matchedPairs) {
                        // System.out.println("         " + pair);
                    }
                }

                if (match.score >= SCORE_THRESHOLD) {
                    String tagLabel = getTagLabel(g, tag);
                    createLinkToArgument(g, productNode, match.argumentNode, NodeFactory.createURI(tag), tagLabel, context);
                    linksCreated++;
                }
                // System.out.println();
            }

            // System.out.println("  ====================================================");
            // System.out.println("   " + linksCreated + " lien(s) créé(s) pour ce tag");
            // System.out.println("  ====================================================\n");
            
            // Affichage des tags sans lien créé
            if (linksCreated == 0) {
                String tagLabel = getTagLabel(g, tag);
                System.out.println("⚠ Tag sans lien avec argument : " + (tagLabel != null ? tagLabel : tag));
                System.out.println("   URI: " + tag);
                System.out.println("   Keywords: " + keywordList);
                System.out.println("   Arguments évalués: " + matches.size());
                if (!matches.isEmpty()) {
                    System.out.println("   Meilleur score obtenu: " + String.format("%.3f", matches.get(0).score) + " (seuil: " + SCORE_THRESHOLD + ")");
                }
                System.out.println();
            }
        }

        return true;
    }

    // ==================== MÉTHODES UTILITAIRES POUR LE SCORING ====================

    /**
     * Récupère les keywords d'un TagArgumentBinding pour un tag donné.
     */
    private String getTagBindingKeywords(Graph g, String tag) {
        Node tagUri = NodeFactory.createURI(tag);

        ExtendedIterator<Triple> itBinding = g.find(Node.ANY, ABOUT_TAG, tagUri);
        while (itBinding.hasNext()) {
            Node bindingNode = itBinding.next().getSubject();

            ExtendedIterator<Triple> itKeywords = g.find(bindingNode, TAG_BINDING_KEYWORDS, Node.ANY);
            while (itKeywords.hasNext()) {
                Node val = itKeywords.next().getObject();
                if (val.isLiteral()) {
                    String keywords = val.getLiteralLexicalForm();
                    if (keywords != null && !keywords.trim().isEmpty()) {
                        itKeywords.close();
                        itBinding.close();
                        return keywords;
                    }
                }
            }
            itKeywords.close();
        }
        itBinding.close();

        return null;
    }

    /**
     * Récupère les keywords d'un TagArgumentBinding pour un tag donné.
     */
    private String getTagLabel(Graph g, String tag) {
        Node tagUri = NodeFactory.createURI(tag);

        ExtendedIterator<Triple> itTag = g.find(tagUri, TAG_LABEL, Node.ANY);
        while (itTag.hasNext()) {
            Node val = itTag.next().getObject();
            if (val.isLiteral()) {
                String label = val.getLiteralLexicalForm();
                if (label != null && !label.trim().isEmpty()) {
                    itTag.close();
                    return label;
                }
            }
        }
        itTag.close();

        return null;
    }
    /**
     * Trouve le meilleur match pour un keyword parmi un ensemble de propriétés d'argument.
     * Retourne le match avec la meilleure qualité, ou null si aucun match.
     */
    private MatchResult findBestMatch(String keyword, Set<String> argProperties) {
        MatchResult bestMatch = null;
        double bestQuality = 0.0;

        for (String argProp : argProperties) {
            // 1. Exact (insensible à la casse)
            if (argProp.equalsIgnoreCase(keyword)) {
                if (QUALITY_EXACT > bestQuality) {
                    bestMatch = new MatchResult(argProp, QUALITY_EXACT);
                    bestQuality = QUALITY_EXACT;
                }
                continue;
            }

            // 2. Normalisé (suppression accents et caractères spéciaux)
            if (normalizeString(argProp).equals(normalizeString(keyword))) {
                if (QUALITY_NORMALIZED > bestQuality) {
                    bestMatch = new MatchResult(argProp, QUALITY_NORMALIZED);
                    bestQuality = QUALITY_NORMALIZED;
                }
                continue;
            }

            // 3. Token overlap (Jaccard-like)
            double overlapRatio = tokenOverlap(keyword, argProp);
            if (overlapRatio >= 0.5) { // Au moins 50% de tokens en commun
                double quality = QUALITY_TOKEN_OVERLAP * overlapRatio;
                if (quality > bestQuality) {
                    bestMatch = new MatchResult(argProp, quality);
                    bestQuality = quality;
                }
            }

            // 4. Containment
            String normKeyword = normalizeString(keyword);
            String normArg = normalizeString(argProp);
            if (!normKeyword.isEmpty() && !normArg.isEmpty() && (normKeyword.contains(normArg) || normArg.contains(normKeyword))) {
                double containmentRatio = Math.min(normKeyword.length(), normArg.length())
                        / (double) Math.max(normKeyword.length(), normArg.length());
                double quality = QUALITY_CONTAINMENT * containmentRatio;
                if (quality > bestQuality) {
                    bestMatch = new MatchResult(argProp, quality);
                    bestQuality = quality;
                }
            }
        }

        return bestMatch;
    }

    /**
     * Répète une chaîne `count` fois (compatibilité Java < 11).
     */
    private String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(count * s.length());
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    // ==================== UTILITAIRES EXISTANTS ====================

    /**
     * Collecte toutes les propriétés d'un ProductArgument pour un type d'emballage spécifique.
     * Inclut : description, verbatim, attribut.labels
     * Filtre par pertinence via isArgumentRelevantForTag.
     */
    private Set<String> collectProductArgumentProperties(Graph g, Node argumentNode, String tag) {
        if (!isArgumentRelevantForTag(g, argumentNode, tag)) {
            return new HashSet<>();
        }

        Set<String> properties = new HashSet<>();

        // Propriétés textuelles principales
        Node[] propertyNodes = {PRODUCT_ARG_DESCRIPTION, PRODUCT_ARG_VERBATIM};
        for (Node propNode : propertyNodes) {
            ExtendedIterator<Triple> it = g.find(argumentNode, propNode, Node.ANY);
            while (it.hasNext()) {
                Node val = it.next().getObject();
                if (val.isLiteral()) {
                    String txt = val.getLiteralLexicalForm().trim();
                    if (!txt.isEmpty()) {
                        properties.add(txt);
                        String[] tokens = txt.split("[\\s,;:]+");
                        for (String token : tokens) {
                            String cleaned = token.trim().toLowerCase();
                            if (!cleaned.isEmpty() && cleaned.length() > 3 && !STOPWORDS.contains(cleaned)) {
                                properties.add(cleaned);
                            }
                        }
                    }
                }
            }
            it.close();
        }

        // Attribut -> prefLabel
        ExtendedIterator<Triple> itAttr = g.find(argumentNode, PRODUCT_ARG_ATTRIBUT, Node.ANY);
        while (itAttr.hasNext()) {
            Node attribut = itAttr.next().getObject();
            if (attribut.isURI()) {
                ExtendedIterator<Triple> itAttr2 = g.find(attribut, PRODUCT_ARG_ATTRIBUT_LABEL, Node.ANY);
                while (itAttr2.hasNext()) {
                    Node labelVal = itAttr2.next().getObject();
                    if (labelVal.isLiteral()) {
                        String txt = labelVal.getLiteralLexicalForm().trim();
                        if (!txt.isEmpty()) {
                            properties.add(txt);
                            String[] tokens = txt.split("[\\s,;:]+");
                            for (String token : tokens) {
                                String cleaned = token.trim().toLowerCase();
                                if (!cleaned.isEmpty() && cleaned.length() > 3 && !STOPWORDS.contains(cleaned)) {
                                    properties.add(cleaned);
                                }
                            }
                        }
                    }
                }
                itAttr2.close();
            }
        }
        itAttr.close();

        return properties;
    }

    /**
     * Vérifie si un ProductArgument est pertinent pour un type d'emballage spécifique.
     * Si aucune règle de filtrage n'est configurée, tout est accepté.
     */
    private boolean isArgumentRelevantForTag(Graph g, Node argumentNode, String tag) {
        String filteringRule = getTagFilteringRule(g, tag);
        if (filteringRule == null || filteringRule.isEmpty()) {
            return true;
        }
        // TODO: appliquer la règle de filtrage si nécessaire
        return true;
    }

    /**
     * Récupère le mot-clé de filtrage pour un tag depuis l'ontologie.
     */
    private String getTagFilteringRule(Graph g, String tag) {
        Node tagUri = NodeFactory.createURI(ncl + tag);

        ExtendedIterator<Triple> itBinding = g.find(Node.ANY, ABOUT_TAG, tagUri);
        while (itBinding.hasNext()) {
            Node bindingNode = itBinding.next().getSubject();

            ExtendedIterator<Triple> itRule = g.find(bindingNode, TAG_FILTERING_RULE, Node.ANY);
            while (itRule.hasNext()) {
                Node val = itRule.next().getObject();
                if (val.isLiteral()) {
                    String rule = val.getLiteralLexicalForm();
                    if (rule != null) {
                        itRule.close();
                        itBinding.close();
                        return rule.trim();
                    }
                }
            }
            itRule.close();
        }
        itBinding.close();

        return null;
    }

    /**
     * Crée un LinkToArgument et ajoute les triples associés.
     */
    private void createLinkToArgument(Graph g, Node productNode, Node argumentNode, Node tagNode, String tagLabel, RuleContext context) {
        String linkId = "LinkToArgument_" + tagLabel + "_" + Math.abs((productNode.toString() + argumentNode.toString()).hashCode());
        Node linkNode = NodeFactory.createURI(ncl + linkId);

        Triple t0 = Triple.create(linkNode, HAS_RDF_TYPE, NodeFactory.createURI(ncl + "LinkToArgument"));
        context.add(t0);

        Triple t1 = Triple.create(productNode, HAS_LINK_TO_ARGUMENT, linkNode);
        context.add(t1);

        Triple t2 = Triple.create(linkNode, HAS_TAG_INITIATOR, tagNode);
        context.add(t2);

        Triple t3 = Triple.create(linkNode, HAS_REFERENCE_PRODUCT_ARGUMENT, argumentNode);
        context.add(t3);

        //System.out.println(" Created LinkToArgument: " + linkNode.toString() + " between Product: " + productNode.toString() + " and Argument: " + argumentNode.toString());
    }

    /**
     * Calcule le taux de chevauchement des tokens entre deux chaînes.
     */
    private double tokenOverlap(String s1, String s2) {
        Set<String> tokens1 = tokenize(normalizeString(s1));
        Set<String> tokens2 = tokenize(normalizeString(s2));

        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 1.0;
        }
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Tokenize une chaîne normalisée.
     */
    private Set<String> tokenize(String str) {
        Set<String> tokens = new HashSet<>();
        String[] parts = str.split("\\s+");
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty() && cleaned.length() > 1) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }

    /**
     * Normalise une chaîne : minuscules, suppression accents et caractères non alphanumériques.
     */
    private String normalizeString(String str) {
        if (str == null) {
            return "";
        }
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
