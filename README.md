# Natcl-inn-Ontology

Présentation
-----------

Ce dépôt contient les sources du projet Natcl'inn — une ontologie centrale et des outils associés destinés à décrire des produits de l'industrie agroalimentaire (IAA) et des arguments relatifs à la "naturalité" des aliments. L'objectif est de produire, à partir de feuilles Excel d'entrée, une base instanciée et inférée qui permette d'associer des arguments à des produits, puis d'exporter un fichier utilisable par le logiciel MyChoice pour analyses ultérieures.

Contenu du dépôt (vue générale)
--------------------------------

Les répertoires principaux et leurs fonctions :

- `src/` : code source Java du projet.
	- `databaseManagement/` : classes pour initialiser et gérer la base TDB (ex. `NatclinnTDBInitialisation`).
	- `inferencesAndQueries/` : programmes qui créent des modèles inférés et exécutent des requêtes SPARQL pour produire des jeux de résultats utiles (ex. `NatclinnCreateInferedModel`, `NatclinnCreateInferredModelAndRunQueries`, `NatclinnQueryInferedModel`, `NatclinnQueryStatistics`).
	- `ontologyManagement/` : assemblage et création des TBox/ABox de l'ontologie (ex. `CreateNatclinnOntology`, utilitaires pour assembler des fichiers d'instances depuis Excel).
	- `queriesForWebService/` : requêtes préparées utilisées par le service web (ex. `NatclinnQueryProducts`).
	- `webService/` : implémentation du service REST (JAX-RS) qui expose les endpoints pour consulter les produits et consulter les exports (ex. `NatclinnService`, `FileUploadService`, `Product`).
	- `webServiceListener/` : listeners d'application (ex. `AppContextListener`) pour initialiser la configuration et le modèle au démarrage du serveur.

- `config/` : fichiers de configuration (par ex. `natclinn.properties`, `log4j2.properties`).


Programmes et tâches principales
--------------------------------

Les programmes suivants sont fournis et servent de points d'entrée pour les étapes importantes du traitement :

- `NatclinnTDBInitialisation` (dans `databaseManagement/`)
	- Rôle : initialiser une base TDB Jena à partir des sources (fichiers RDF/TTL/CSV/Excel selon configuration).
	- Utilisation typique : prépare le magasin RDF local dans lequel seront chargées les instances de produits et d'arguments.
	- Entrées : fichiers d'instances (ou feuilles Excel pré-converties), configuration `natclinn.properties`.
	- Sorties : répertoires TDB prêts à l'emploi (ex. `nullnull/Data-0001/...`).

- `NatclinnStartQueries` (dans `inferencesAndQueries/`)
	- Rôle : exécuter l'ensemble des requêtes et routines d'inférence planifiées pour construire le modèle inféré et générer des exports.
	- Fonctionnalités : création du modèle inféré (InfModel), exécution de jeux de requêtes SPARQL, écriture des résultats dans des fichiers de sortie.
	- Entrées : modèle TDB initialisé, configuration des requêtes.
	- Sorties : fichiers de résultats (CSV/Excel) et éventuels rapports statistiques.

- Autres utilitaires importants :
	- `NatclinnCreateInferedModel` / `NatclinnCreateInferredModelAndRunQueries` : construction du modèle inféré et exécution ciblée de requêtes.
	- `NatclinnQueryProducts` : requêtes SPARQL spécifiques pour lister les produits et récupérer leurs propriétés (EAN, libellés, etc.).
	- `NatclinnQueryStatistics` : extraction de statistiques sur le jeu de données.

Service Web
-----------

Un service JAX‑RS expose des endpoints pour interroger les produits :

- `GET /api_natclinn/products/list` : retourne la liste des produits (JSON).
- `GET /api_natclinn/products/{id}` : retourne un produit identifié par `{id}` ; le paramètre de chemin accepte des identifiants contenant des `/` grâce à une expression régulière. L'identifiant peut être encodé (hex ou percent-encoding) ; le service tente de décoder quand nécessaire.
- `GET /api_natclinn/products/search?id=...` : variante acceptant l'`id` en paramètre de requête. L'ID doit être correctement encodé (percent-encoding) si nécessaire ; le service effectue un décodage URL automatique côté serveur.

Notes et bonnes pratiques
------------------------

- Formats d'entrée : les produits et arguments sont instanciés à partir de feuilles Excel. Le mapping onglet/colonne attendu est défini dans le code (voir classes de `ontologyManagement/` et la documentation interne).
- Inférence : Jena est utilisé pour étendre la connaissance (InfModel). Les règles et axiomes définis dans la TBox permettent d'attacher automatiquement certains arguments à certains produits.
- Export MyChoice : l'objectif final est la production d'un fichier d'échange compatible avec MyChoice. Le format exact et les colonnes d'export sont gérés par les utilitaires d'`ontologyManagement`/`inferencesAndQueries` et peuvent nécessiter une configuration locale pour correspondre à la version de MyChoice utilisée.
- Encodage des identifiants :
	- Lors de l'appel par chemin (`/{id}`), les identifiants contenant des caractères spéciaux doivent être encadrés (par ex. `<...>`) ou correctement encodés par le client.
	- Lors de l'appel par query param (`/search?id=...`), utiliser le percent-encoding (ex. `https%3A%2F%2Fw3id.org%2FNCL%2Fontology%2FP-...`). Le service effectue un décodage côté serveur.

Exemples d'exécution
--------------------

- Construction et déploiement (exemple rapide) :

	- Build Maven et création du WAR :

		mvn clean package -DskipTests

	- Déploiement sous Tomcat (script fourni pour Windows) :

		scripts\deploy-windows.ps1 -SkipTests

	- Tâche VS Code : "Build & Deploy to Tomcat" (configurée dans `.vscode/tasks.json`).

- Lancement des étapes Java directement (exemples) :

	- Initialisation TDB : exécuter `databaseManagement.NatclinnTDBInitialisation` via l'IDE ou `mvn exec:java` si configuré.
	- Construction du modèle inféré et exécution des requêtes : exécuter `inferencesAndQueries.NatclinnStartQueries`.

Liens et références
-------------------

- Dépôt GitHub : https://github.com/rapha34/Natcl-inn-Ontology
- Références externes :
	- Natcl'inn (documentation / site du projet) — https://www.labo-lego.fr/natclinn/
	- MyChoice (site/manuel de format d'import) — https://mychoice.netlify.app/

Tests rapides recommandés
------------------------

1. Initialiser la base TDB avec `NatclinnTDBInitialisation` et vérifier la création des fichiers sous `nullnull/`.
2. Lancer `NatclinnStartQueries` et vérifier l'apparition des fichiers résultats et des logs d'inférence.
3. Démarrer l'application web (Tomcat) et appeler les endpoints :
	 - `/api_natclinn/products/list`
	 - `/api_natclinn/products/search?id=<id encodé>`
4. Vérifier que l'export MyChoice a été généré et ouvrir le fichier dans MyChoice (ou outil compatible) pour valider la correspondance des colonnes.

