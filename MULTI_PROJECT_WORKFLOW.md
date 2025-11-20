# Workflow Multi-Projets MyChoice

## Vue d'ensemble

Le système permet maintenant de créer et traiter **plusieurs projets MyChoice en parallèle**, chacun générant son propre fichier XML. Le nom du fichier est dérivé automatiquement du nom du projet.

## Architecture en 2 phases

### Phase 1 : Pré-création du projet (INSERT queries)

Dans `NatclinnQueryStatistics.java`, vous pouvez injecter autant de projets que nécessaire via des requêtes INSERT :

```java
// Projet 1 : Produits Natclinn
stringQuery = prefix + 
    "INSERT DATA { " +
    "  mch:Project_NatclinnProducts rdf:type mch:Project . " +
    "  mch:Project_NatclinnProducts mch:projectName \"Choix de produits Natclinn\"@fr . " +
    "  mch:Project_NatclinnProducts mch:projectDescription \"Projet généré automatiquement...\"@fr . " +
    "  mch:Project_NatclinnProducts mch:projectImage \"https://cdn.pixabay.com/photo/2022/01/18/16/30/vegetables-6947444_960_720.jpg\" . " +
    "}";

// Alternatives du projet 1
stringQuery = prefix + 
    "INSERT { " +
    "  ?alternative rdf:type mch:Alternative . " +
    "  ... " +
    "  VALUES ?ean13 { \"3564700423196\" \"3564700573181\" \"3564700755211\" } " +
    "  ... " +
    "}";

// Projet 2 : Autre comparaison
stringQuery = prefix + 
    "INSERT DATA { " +
    "  mch:Project_ComparaisonBio rdf:type mch:Project . " +
    "  mch:Project_ComparaisonBio mch:projectName \"Produits Bio vs Conventionnels\"@fr . " +
    "  mch:Project_ComparaisonBio mch:projectDescription \"Comparaison bio/conventionnel\"@fr . " +
    "  mch:Project_ComparaisonBio mch:projectImage \"https://example.com/bio-image.jpg\" . " +
    "}";

// Alternatives du projet 2...
```

### Phase 2 : Enrichissement automatique

`CreateMychoiceProjectFromProducts.java` détecte **automatiquement tous les projets** de type `mch:Project` et :
1. Parcourt chaque projet trouvé
2. Récupère les alternatives existantes (créées par INSERT)
3. Ajoute les arguments inférés via les règles Jena
4. Génère un fichier XML par projet

## Propriétés supportées

### Propriétés de Projet

Toutes les propriétés de `MychoiceAbox.xml` sont supportées :

| Propriété | Obligatoire | Valeur par défaut |
|-----------|-------------|-------------------|
| `mch:projectName` | ✅ Oui | - |
| `mch:projectDescription` | ✅ Oui | - |
| `mch:projectImage` | ❌ Non | `https://cdn.pixabay.com/photo/2022/01/18/16/30/vegetables-6947444_960_720.jpg` |

Si `mch:projectImage` n'est pas spécifiée dans l'INSERT, le système ajoute automatiquement l'image par défaut.

### Propriétés d'Alternative

| Propriété | Source |
|-----------|--------|
| `mch:nameAlternative` | `ncl:name` du produit |
| `mch:alternativeDescription` | `ncl:description` du produit (ou nom si absent) |
| `mch:relatedToProduct` | URI du produit ncl:Product |

### Propriétés d'Argument

| Propriété | Source |
|-----------|--------|
| `mch:assertion` | `ncl:nameProperty` ou `skos:prefLabel` de l'argument |
| `mch:explanation` | `ncl:hasExplanation` de l'argument |
| `mch:relatedToNatclinnProductArgument` | URI (string) de l'argument ncl:ProductArgument |

## Génération des noms de fichiers

Le système normalise le nom du projet pour créer le nom de fichier :

```
Nom du projet : "Choix de produits Natclinn"
→ Fichier XML : "Choix_de_produits_Natclinn.xml"

Nom du projet : "Produits Bio vs Conventionnels"
→ Fichier XML : "Produits_Bio_vs_Conventionnels.xml"

Nom du projet : "NATCL'INN"
→ Fichier XML : "NATCLINN.xml"
```

**Règles de normalisation :**
- Suppression des caractères spéciaux (', ", etc.)
- Espaces → underscores
- Garde les lettres, chiffres, tirets

**Format des codes produits :**
- Les URIs des produits sont `ncl:P-{EAN13}` où `{EAN13}` est le code à 13 chiffres
- Dans les INSERT queries, utilisez uniquement le code EAN13 (sans le préfixe `P-`)
- Exemple : pour le produit `ncl:P-3564700423196`, utilisez `"3564700423196"` dans VALUES
- L'URI de l'alternative sera automatiquement générée : `mch:Alternative_P-3564700423196`

## Exemple complet : 2 projets

### INSERT queries dans NatclinnQueryStatistics.java

```java
// ============ PROJET 1 : Moussakas ============
titleQuery = "Création du projet Moussakas";
typeQuery = "INSERT";
stringQuery = prefix + 
    "INSERT DATA { " +
    "  mch:Project_Moussakas rdf:type mch:Project . " +
    "  mch:Project_Moussakas mch:projectName \"Comparaison Moussakas\"@fr . " +
    "  mch:Project_Moussakas mch:projectDescription \"3 moussakas du marché\"@fr . " +
    "  mch:Project_Moussakas mch:projectImage \"https://images.openfoodfacts.org/.../moussaka.jpg\" . " +
    "}";
listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

titleQuery = "Alternatives du projet Moussakas";
typeQuery = "INSERT";
stringQuery = prefix + 
    "INSERT { " +
    "  ?alternative rdf:type mch:Alternative . " +
    "  ?alternative mch:nameAlternative ?productName . " +
    "  ?alternative mch:relatedToProduct ?product . " +
    "  mch:Project_Moussakas mch:hasAlternative ?alternative . " +
    "} WHERE { " +
    "  VALUES ?ean13 { \"3564700423196\" \"3564700573181\" \"3564700755211\" } " +
    "  ?product ncl:hasEAN13 ?ean13 . " +
    "  ?product ncl:name ?productName . " +
    "  BIND(IRI(CONCAT(STR(mch:), \"Alternative_P-\", ?ean13)) AS ?alternative) " +
    "}";
listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

// ============ PROJET 2 : Plats cuisinés Bio ============
titleQuery = "Création du projet Bio";
typeQuery = "INSERT";
stringQuery = prefix + 
    "INSERT DATA { " +
    "  mch:Project_Bio rdf:type mch:Project . " +
    "  mch:Project_Bio mch:projectName \"Plats cuisinés Bio\"@fr . " +
    "  mch:Project_Bio mch:projectDescription \"Sélection de plats bio\"@fr . " +
    "  mch:Project_Bio mch:projectImage \"https://cdn.pixabay.com/photo/.../organic.jpg\" . " +
    "}";
listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

titleQuery = "Alternatives du projet Bio";
typeQuery = "INSERT";
stringQuery = prefix + 
    "INSERT { " +
    "  ?alternative rdf:type mch:Alternative . " +
    "  ?alternative mch:nameAlternative ?productName . " +
    "  ?alternative mch:relatedToProduct ?product . " +
    "  mch:Project_Bio mch:hasAlternative ?alternative . " +
    "} WHERE { " +
    "  VALUES ?ean13 { \"XXXXX\" \"YYYYY\" } " +
    "  ?product ncl:hasEAN13 ?ean13 . " +
    "  ?product ncl:name ?productName . " +
    "  BIND(IRI(CONCAT(STR(mch:), \"Alternative_P-\", ?ean13)) AS ?alternative) " +
    "}";
listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
```

### Résultat de l'exécution

```
=== Création de projets MyChoice depuis le modèle inféré ===
Recherche des projets MyChoice à traiter...

--- Traitement du projet 1 : Comparaison Moussakas ---
   - Projet MyChoice détecté : https://w3id.org/MCH/ontology/Project_Moussakas
   - Nom : Comparaison Moussakas
   - Description : 3 moussakas du marché
   - Enrichissement avec les arguments inférés...
   - Arguments ajoutés au projet : 15
   - Nouvelles alternatives créées : 0
Sauvegarde du projet dans : Comparaison_Moussakas.xml
   - Fichier sauvegardé : C:\var\www\natclinn\ontologies\Comparaison_Moussakas.xml

--- Traitement du projet 2 : Plats cuisinés Bio ---
   - Projet MyChoice détecté : https://w3id.org/MCH/ontology/Project_Bio
   - Nom : Plats cuisinés Bio
   - Description : Sélection de plats bio
   - Ajout de l'image par défaut du projet
   - Enrichissement avec les arguments inférés...
   - Arguments ajoutés au projet : 8
   - Nouvelles alternatives créées : 0
Sauvegarde du projet dans : Plats_cuisinés_Bio.xml
   - Fichier sauvegardé : C:\var\www\natclinn\ontologies\Plats_cuisinés_Bio.xml

Nombre total de projets traités : 2
=== Tous les projets MyChoice traités avec succès ===
```

## Avantages

✅ **Multi-projets** : Gérez autant de comparaisons que nécessaire  
✅ **Flexibilité** : Choisissez exactement quels produits comparer  
✅ **Personnalisation** : Nom, description, image pour chaque projet  
✅ **Automatique** : Les arguments sont inférés et ajoutés automatiquement  
✅ **Valeurs par défaut** : Propriétés optionnelles avec valeurs sensibles  
✅ **Fichiers distincts** : Un XML par projet, nommé intelligemment  

## Workflow complet

1. **Définir les projets** : Ajoutez des INSERT queries dans `NatclinnQueryStatistics.java`
2. **Lister les produits** : Utilisez VALUES pour spécifier les codes produits
3. **Exécuter** : `mvn exec:java "-Dexec.mainClass=inferencesAndQueries.NatclinnQueryStatistics"`
4. **Résultat** : Un fichier XML par projet dans `C:\var\www\natclinn\ontologies\`
5. **Extraction Excel** : Utilisez `ExtractMychoiceProjectToExcel` sur chaque fichier XML

## Notes techniques

- **URI du projet** : Doit être unique (ex: `mch:Project_NomUnique`)
- **URI des alternatives** : Format `mch:Alternative_{CODE_PRODUIT}`
- **Relation projet-alternative** : `mch:hasAlternative`
- **Relation alternative-produit** : `mch:relatedToProduct`
- **Traçabilité argument Natclinn** : `mch:relatedToNatclinnProductArgument` (DatatypeProperty) stocke l'URI du `ncl:ProductArgument` source
- **Règles d'inférence** : Fichier `natclinn_additives.rules` utilise `ncl:ProductArgument` et `ncl:hasProductArgument`
- **Architecture** : Les produits (`ncl:Product`) sont liés aux arguments (`ncl:ProductArgument`) via `ncl:hasProductArgument` par inférence

## Compatibilité

✅ Compatible avec `ExtractMychoiceProjectToExcel.java`  
✅ Compatible avec l'ontologie MyChoice existante  
✅ Rétrocompatible avec les projets mono-fichier  
