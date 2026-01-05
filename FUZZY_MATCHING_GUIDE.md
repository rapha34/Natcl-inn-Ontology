# Primitive FindIngredientInTaxonomy - Documentation

## Vue d'ensemble

La primitive `FindIngredientInTaxonomy` permet de chercher et matcher fuzzy les labels d'ingrédients dans la taxonomie RDF d'ingrédients, même quand ceux-ci ne disposent pas d'un code OFF.

## Architecture

### 1. Méthodes heuristiques dans `NatclinnUtil.java`

Toutes les méthodes sont statiques et réutilisables pour d'autres contextes:

```java
// Normalization et nettoyage
normalizeLabel(String label)        // Minuscules, accents, ponctuation, stopwords
singularize(String word)            // Singularise (tomates → tomate)
getTokens(String label)             // Divise en tokens

// Métriques de similarité
jaccardSimilarity(Set, Set)         // Jaccard coefficient sur tokens
diceSimilarity(Set, Set)            // Dice coefficient sur tokens
tokenSortRatio(String, String)      // Tokens triés + Levenshtein
levenshteinDistance(String, String) // Distance d'édition brute
levenshteinSimilarity(String, String) // Distance normalisée (0-1)

// Règles linguistiques
applyFrenchRules(String, String)    // Détecte inversions, accords de genre

// Scoring composite
computeMatchScore(String, String)   // Score pondéré: 0.4×Jaccard + 0.4×TokenSort + 0.2×Levenshtein
                                     // + bonus français (+0.05)

// Classe utilitaire
IngredientMatch                      // {label, score, uri}

// Recherche dans la taxonomie
findIngredientInTaxonomy(String, List<Map>) // Cherche le meilleur match (score >= 0.78)
```

### 2. Primitive SPARQL dans `FindIngredientInTaxonomy.java`

Classe `FunctionBase` pour enregistrement dans Jena:

```java
// URI SPARQL
natclinn:findIngredientMatch(?label) → URI ou ""
natclinn:findIngredientMatch(?label, ?namespace) → idem
```

**Comportement:**
- Charge la taxonomie complète d'ingrédients (SELECT ?ingredient ?label)
- Cherche le meilleur match avec `NatclinnUtil.findIngredientInTaxonomy()`
- Retourne l'URI du match si score >= 0.78
- Retourne "" si aucun match >= 0.78
- Log "BORDERLINE" pour matches 0.70-0.78 (revue manuelle recommandée)

### 3. Requête de test dans `NatclinnQueryStatistics.java`

Query "Test - Fuzzy matching ingrédients (FindIngredientInTaxonomy)":

```sparql
PREFIX natclinn: <http://natclinn/sparql/function/>
SELECT ?ingredient ?ingredientLabel ?matchedUri ?matchedLabel 
WHERE { 
  ?ingredient rdf:type ncl:Ingredient . 
  ?ingredient skos:prefLabel ?ingredientLabel . 
  FILTER NOT EXISTS { ?ingredient ncl:hasIdIngredientOFF ?offId } 
  BIND(natclinn:findIngredientMatch(?ingredientLabel) AS ?matchUri) 
  OPTIONAL { 
    ?matchedUri rdf:type ncl:TaxonomyIngredient . 
    ?matchedUri skos:prefLabel ?matchedLabel . 
    FILTER(STR(?matchUri) != '' && ?matchUri = STR(?matchedUri)) 
  } 
} LIMIT 100
```

## Algorithme de matching

### 1. Normalisation (NatclinnUtil.normalizeLabel)

```
Entrée: "Concentré de Tomates"
  ↓ minuscules
"concentré de tomates"
  ↓ accents (é→e)
"concentre de tomates"
  ↓ stopwords (de)
"concentre tomates"
  ↓ singularisation (tomates→tomate)
"concentre tomate"
```

### 2. Scoring composite

Pour chaque candidat de la taxonomie:

1. **Jaccard** (40%): Intersection/Union des tokens singularisés
   - "concentre tomate" vs "tomate concentree" → ~0.67 (2 tokens communs, 3 différents)

2. **Token Sort Ratio** (40%): Tokens triés puis distance Levenshtein
   - Sorted1: "concentre tomate"
   - Sorted2: "concentre tomate" → Distance = 0 → Ratio = 1.0

3. **Levenshtein normalisé** (20%): Distance d'édition (0-1)
   - normalize("concentré de tomates") vs normalize("concentre tomate")
   - Distance brute petit (accents, "de") → Ratio ~0.95

4. **Règles français** (+5% bonus si match)
   - Détecte: "tomate concentree" vs "concentre de tomate" (même tokens)

### 3. Décision

```
Score final = (0.4 × Jaccard) + (0.4 × TokenSort) + (0.2 × Levenshtein) + bonus
```

**Seuils:**
- **Score >= 0.78**: ✅ Acceptance automatique → URI retournée
- **0.70 <= Score < 0.78**: ⚠️ BORDERLINE → Log "MANUAL REVIEW NEEDED"
- **Score < 0.70**: ❌ Rejection → "" retourné

## Exemples d'utilisation

### Cas 1: "concentré de tomates" → "Concentré de tomate"

```
Entrée SPARQL: "concentré de tomates"
Normalize: "concentre tomate" (s supprimé via singularize)

Candidat: "Concentré de tomate"
Normalize: "concentre tomate"

Jaccard: {concentre, tomate} ∩ {concentre, tomate} / {concentre, tomate} = 1.0
TokenSort: sorted strings identiques → 1.0
Levenshtein: 0 distance → 1.0
Bonus français: oui (+0.05)

Score: (0.4×1.0) + (0.4×1.0) + (0.2×1.0) + 0.05 = 1.05 → capped 1.0
✅ RESULT: Excellent match (score 1.0)
```

### Cas 2: "concassé de tomates" → "Tomate concassée"

```
Entrée: "concassé de tomates"
Normalize: "concasse tomate"

Candidat: "Tomate concassée"
Normalize: "tomate concasse"

Jaccard: {concasse, tomate} vs {tomate, concasse} = 1.0
TokenSort: after sort: "concasse tomate" vs "concasse tomate" = 1.0
Levenshtein: identical = 1.0
Bonus: oui (inversion word order detected)

Score: 1.0
✅ RESULT: Inversion word order handled correctly
```

### Cas 3: "sirop de glucose-fructose" → "Glucose-fructose sirop"

```
Entrée: "sirop de glucose-fructose"
Normalize: "sirop glucose-fructose" (après suppression "de")
Tokenize: {sirop, glucose, fructose}

Candidat: "Glucose-fructose sirop"
Normalize: "glucose-fructose sirop"
Tokenize: {glucose, fructose, sirop}

Jaccard: {g,f,s} = 1.0
TokenSort: "fructose glucose sirop" vs "fructose glucose sirop" = 1.0
```

### Cas 4: "concentre tomate" → "ail" (mauvaise taxonomie)

```
Jaccard: 0
TokenSort: très faible (~0.1)
Levenshtein: faible (~0.3)

Score: ~0.15
❌ RESULT: Rejection (< 0.78)
```

## Performance et optimisations

- **Première requête SPARQL** (load taxonomy): Unique, puis cached en mémoire
- **Pour chaque ingrédient**: O(n) comparaisons (n = taille taxonomie)
- **Complexity par ingrédient**: O(m) où m = longueur moyenne labels

**Optimisation possible:**
- Cache de résultats (HashMap<ingredientLabel, IngredientMatch>)
- Index inversé des tokens (faster filtering)
- Parent-based filtering (si ingredient parent connu)

## Intégration future

### 1. Utiliser dans une SPARQL CONSTRUCT

```sparql
CONSTRUCT {
  ?ingredient ncl:hasIdIngredientOFF ?matchedUri
}
WHERE {
  ?ingredient rdf:type ncl:Ingredient .
  ?ingredient skos:prefLabel ?label .
  FILTER NOT EXISTS { ?ingredient ncl:hasIdIngredientOFF ?any }
  BIND(natclinn:findIngredientMatch(?label) AS ?matchUri)
  ?matchedUri rdfs:type ncl:TaxonomyIngredient .
  FILTER(STR(?matchUri) != '')
}
```

### 2. Batch processing

```java
// Réutiliser les méthodes pour traitement batch
for (String ingredientLabel : unmappedLabels) {
  double score = NatclinnUtil.computeMatchScore(ingredientLabel, taxonomyLabel);
  // Log pour analyse
}
```

### 3. Amélioration avec synonymes

```java
// Charger altLabel et synonymes de la taxonomie
String sparqlQuery = "SELECT ?ingredient ?label ?altLabel WHERE { ... }";
// Ajouter altLabel à la liste des candidats dans findIngredientInTaxonomy
```

## Configuration (NatclinnConf.java)

Aucune configuration requise - utilise la taxonomie existante d'ingrédients.

## Logs et debugging

### Mode verbeux (console)

```
✅ FUZZY MATCH SUCCESS: 'concentré de tomates' => 'Concentré de tomate' (score: 0.998)
⚠️  FUZZY MATCH BORDERLINE: 'jus tomate' ~= 'Concentré de tomate' (score: 0.715) - MANUAL REVIEW NEEDED
❌ NO MATCH: 'xyz ingredient' (no taxonomy entry scored >= 0.78)
❌ Model is null in FindIngredientInTaxonomy
⚠️ Error loading ingredient taxonomy: Connection refused
```

## Fichiers modifiés

1. **src/natclinn/util/NatclinnUtil.java** (~280 lignes ajoutées)
   - Méthodes heuristiques (public static)
   - Classe IngredientMatch

2. **src/natclinn/util/FindIngredientInTaxonomy.java** (nouveau fichier, ~150 lignes)
   - Primitive SPARQL FunctionBase
   - Chargement taxonomie SPARQL
   - Interface exec()

3. **src/inferencesAndQueries/NatclinnQueryStatistics.java** (modifications mineures)
   - Ajout "FindIngredientInTaxonomy" à listPrimitives
   - Ajout requête de test (Query #???)

## Prochaines étapes

1. **Tester sur P-3564700423196** avec les ingrédients sans code OFF
2. **Affiner seuil 0.78** si trop strict/laxiste
3. **Ajouter caching** pour performance sur grands datasets
4. **Implémenter disambiguation UI** pour borderline cases (0.70-0.78)
5. **Exporter résultats** vers CONSTRUCT SPARQL pour mise à jour RDF

---

**Dernière mise à jour**: Dec 20, 2025
**Status**: ✅ Implémentée et prête au test
