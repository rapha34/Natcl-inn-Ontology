# Génération des fichiers DAMN depuis MyChoice

## Vue d'ensemble

Trois nouveaux fichiers Java ont été créés pour exporter les projets MyChoice au format DAMN (Defeasible Reasoning Tool for Multi-Agent Reasoning).

## Fichiers créés

### 1. ExportMychoiceProjectToDamn.java
**Classe principale** qui orchestrate l'export.

- **Fonctionnalité** : Charge un projet MyChoice depuis un fichier RDF/XML et l'exporte en JSON et texte DAMN
- **Point d'entrée** : `exportProjectToDamn(String aboxFile, String outputJson, String outputText)`
- **Extraction de données** :
  - Alternatives MyChoice (via `mch:hasProject`)
  - Arguments associés (via `mch:hasArgument`)
  - Parties prenantes (Stakeholder)
  - Sources
  - Toutes les propriétés associées

**Classes internes pour les données** :
- `AlternativeData` : uri, name, description, image, icon, arguments
- `ArgumentData` : uri, assertion, explanation, value, condition, isProspective, date, stakeholder, sources
- `StakeholderData` : uri, name
- `SourceData` : uri, name, date, typeSource

### 2. ExportMychoiceProjectToDamnJson.java
**Export au format JSON** compatible DAMN.

Structure du JSON :
```json
{
  "project": "...",
  "format": "DAMN_v2",
  "arguments": [ ... ],
  "rules": [ ... ],
  "constraints": [ ... ]
}
```

**Contenu** :
- **Arguments** : Liste complète des arguments avec tous leurs attributs
- **Règles** (7 règles codées en dur) :
  - `serves(ALT,AIM,PROP)` : Alternative sert un objectif
  - `harms(ALT,AIM,PROP)` : Alternative nuit à un objectif
  - `hasCriterion(AIM,CRIT)` : Objectif a un critère
  - `serves(ALT,CRIT,PROP)` : Transitivité serve-critère
  - `harms(ALT,CRIT,PROP)` : Transitivité harms-critère
  - `support(ALT)` : Soutenir une alternative
  - `avoid(ALT)` : Éviter une alternative

- **Contraintes** (3 contraintes codées en dur) :
  - Contradiction : serves ET harms au même aim
  - Contradiction : serves ET harms au même critère
  - Contradiction : support ET avoid

### 3. ExportMychoiceProjectToDamnText.java
**Export au format texte** DAMN (compatible DamnArgument.java).

Syntaxe :
- **Faits** : Prédicats binaires (`argument(a1)`, `nameAlternative(a1,value)`, etc.)
- **Règles** : Syntaxe DAMN (`head <= body`)
- **Contraintes** : Integrity constraints (`! :- body`)

**Normalisation des chaînes** :
- Minuscules
- Suppression des accents et caractères spéciaux
- Remplacement des espaces par underscores
- Suppression des underscores en début

## Intégration

La génération DAMN a été intégrée dans **CreateMychoiceProjectFromPreliminaryProject.java** dans la méthode `processAllProjects()`.

Après chaque extraction Excel d'un projet MyChoice :
```java
ExportMychoiceProjectToDamn.exportProjectToDamn(xmlFilePath, damnJsonFilePath, damnTextFilePath);
```

**Fichiers de sortie** (pour un projet nommé "Project_madeleines.xml") :
- `Project_madeleines_damn.json`
- `Project_madeleines_damn.txt`

## Utilisation

1. **Depuis la ligne de commande** :
   ```bash
   java ontologyManagement.ExportMychoiceProjectToDamn [chemin_fichier_xml]
   ```
   
   Sans argument, utilise le fichier par défaut :
   ```
   {folderForOntologies}/Project_madeleines.xml
   ```

2. **Automatique** : L'export DAMN s'exécute automatiquement lors du traitement des projets MyChoice par `CreateMychoiceProjectFromPreliminaryProject`.

## Détails techniques

### Format DAMN généré

Le format texte DAMN suit la syntaxe de l'exemple `exemple_MyChoice-Damn_v2.txt` :
- Faits : Chaque argument déclaré avec ses propriétés
- Règles : Inférence basée sur les types d'arguments (pro/con)
- Contraintes : Détection des conflits entre les assertions

### Règles codées en dur

Les 7 règles incluses sont indépendantes du contenu spécifique du projet et permettent un raisonnement défaisable générique sur les alternatives et critères.

Ces règles peuvent être personnalisées en modifiant `ExportMychoiceProjectToDamnJson.java` et `ExportMychoiceProjectToDamnText.java`.

## Notes

- Le format JSON DAMN est extensible et peut être modifié
- Les règles sont actuellement codées en dur (comme demandé)
- La normalisation des chaînes suit le modèle du programme `DamnArgument.java`
- Les fichiers générés sont situés dans `{folderForResults}/`
