package ontologyManagement;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;

/**
 * Génère une feuille Excel avec IDArgument et mots-clés extraits des colonnes Attribut, Description et Verbatim.
 * 
 * EXTRACTION DES MOTS-CLÉS :
 * ────────────────────────────
 * - Nettoie et normalise les textes (suppression ponctuation, minuscules)
 * - Filtre les stopwords français (articles, prépositions, etc.)
 * - Conserve les termes significatifs de longueur ≥ 3 caractères
 * - Déduplique et trie par ordre alphabétique
 * 
 * @author Natclinn
 */
public class GenerateKeywordsExcel2 {
    
    /**
     * Liste exhaustive des stopwords français à filtrer lors de l'extraction.
     * Inclut : articles, prépositions, pronoms, auxiliaires et mots de liaison courants.
     */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "mais", "donc", "car",
        "ni", "or", "pour", "dans", "sur", "sous", "avec", "sans", "entre", "vers", "chez",
        "par", "ce", "cet", "cette", "ces", "mon", "ton", "son", "ma", "ta", "sa", "mes", "tes",
        "ses", "notre", "votre", "leur", "nos", "vos", "leurs", "je", "tu", "il", "elle", "on",
        "nous", "vous", "ils", "elles", "me", "te", "se", "moi", "toi", "lui", "eux", "qui",
        "que", "quoi", "dont", "où", "si", "comme", "quand", "tout", "tous", "toute", "toutes",
        "être", "avoir", "faire", "dire", "aller", "voir", "savoir", "pouvoir", "vouloir",
        "est", "sont", "ai", "as", "a", "avons", "avez", "ont", "été", "était", "étaient",
        "sera", "seront", "plus", "très", "bien", "alors", "aussi", "ainsi", "peut", "peuvent",
        "va", "ça", "pas", "pas", "non", "oui", "enfin", "voilà", "même", "déjà", "encore",
        "jamais", "toujours", "quelque", "quelques", "autre", "autres", "certains", "certaines",
        "rien", "quelqu", "d", "l", "s", "c", "n", "qu", "j", "m", "t", "y", "au", "aux"
    ));

    public static void main(String[] args) {
        new NatclinnConf();
        
        try {
            // Données du tableau (IDArgument, Attribut, Description, Verbatim)
            String[][] data = {
                {"A-0000001", "Mode de culture et d'élevage\\Mode d'élevage\\Bien-être animal (sans souffrance)", "Conditions et éthique d'élevage ou d'abbatage. L'industrialisation est associée à la souffrance animale", "Parce que c'est fait en usine.  On sait très bien que toutes ces usines, déjà, ne respectent pas l'animal au niveau de l'abattage de l'animal."},
                {"A-0000002", "Mode de culture et d'élevage\\Mode d'élevage\\Elevage perçu comme naturel", "Elevage naturel : De qualité (perçue), Elevage non intensif, certifié, qui influence la qualité des produits animaux (chair, oeufs ou lait perçus de meilleur qualité)", "Par exemple de la viande si c'est des poulets élevés en plein air ou si c'est du poulet de batterie, enfin voilà, naturalité c'est tout ce qui va regrouper je pense des pratiques un peu plus vertueuses par rapport à la nature."},
                {"A-0000003", "Mode de culture et d'élevage\\Mode de culture\\Agriculture sans intrants chimiques", "Agriculture sans intrants chimiques", "Entre manger des pesticides pétrochimiques et manger des pesticides style bouillie bordelaise, enfin un peu plus naturel."},
                {"A-0000004", "Mode de culture et d'élevage\\Mode de culture\\Culture par particulier", "Culture par un particulier dans son jardin", "Ma courge butternut, c'est ma voisine qui l'a produit dans son potager.  Donc, oui, voilà, ça, c'est des produits naturels."},
                {"A-0000005", "Mode de culture et d'élevage\\Mode de culture\\Culture, pêche locale", "Notion du local (non de l'origine ou de le provenance : codées séparemment)", "Quand je prends le chou-fleur du coin, les artichauts du coin, ça c'est naturel."},
                {"A-0000006", "Production Ingrédients et compositions\\Composition AVEC\\Arômes naturels", "Arômes naturelles dans la liste d'ingrédients", "Tu n'auras pas des extraits naturels de vanille dans ton yaourt (...) Aujourd'hui, si je veux manger un yaourt à la vanille, je vais acheter un yaourt avec des extraits de vanille.  Quand je l'ouvre, je veux distinguer un soupçon de vanille dans mon yaourt."},
                {"A-0000007", "Production Ingrédients et compositions\\Composition AVEC\\Colorants naturels", "Colorants naturels dans la liste d'ingrédients", "Je sais que la betterave, c'est beaucoup utilisé comme colorant.  Quand le colorant est d'origine naturelle, où il n'y a pas d'ajout chimique dessus, pour moi, c'est du colorant naturel."},
                {"A-0000008", "Production Ingrédients et compositions\\Composition AVEC\\Conservateurs naturels", "Conservateur naturel dans la liste d'ingrédients", "Et il y a des conservateurs naturels, d'accord ?  Ça dépend de ce que tu vas faire.  Mais le sel est un conservateur.  Tu peux utiliser de l'huile comme conservateur, l'alcool, d'accord ?  Et le fait d'isoler donc le... Le produit avec l'air."},
                {"A-0000009", "Production Ingrédients et compositions\\Composition AVEC\\Ingrédients à partir de produits bruts", "Fait avec des produits bruts, simple et reconnaissables", "Que je peux assembler moi-même, quoi.  Enfin, que je peux produire sans utiliser de produits eux-mêmes transformés, en prenant que des produits bruts."},
                {"A-0000010", "Production Ingrédients et compositions\\Composition AVEC\\Variétés ou races anciennes", "Variétés pour la culture et races pour les animaux", "Farine à l'ancienne // Bon, le jambon, ça, je sais qu'il est artisanal (...) C'est fait avec des cochons noirs"},
                {"A-0000011", "Production Ingrédients et compositions\\Composition SANS\\Sans additifs", "Sans additifs", "C'est tous les additifs commençant par E. Je crois que ce n'est pas tous qui sont en cause, mais il y en a beaucoup qui sont décrits comme cancérigènes, perturbateurs endocriniens"},
                {"A-0000012", "Production Ingrédients et compositions\\Composition SANS\\Sans arômes artificiels", "Sans arômes artificiels", "Des arômes artificiels (...) C'est des arômes de synthèse.  Là, il n'y a rien de naturel, je pense (...) C'est de la chimie."},
                {"A-0000013", "Production Ingrédients et compositions\\Composition SANS\\Sans colorants artificiels", "Sans colorants artificiels", "Il y a certains colorants qui sont mauvais pour la santé, comme le blanc qu'on retrouve sur les chewing-gums, en dragée."},
                {"A-0000014", "Production Ingrédients et compositions\\Composition SANS\\Sans conservateurs", "Sans conservateurs", "La charcuterie (...) d'un point de vue santé, c'est vraiment, vraiment mauvais avec justement tous les conservateurs, les nitrates, qui sont quand même des cancérogènes avérés."},
                {"A-0000015", "Production Ingrédients et compositions\\Composition SANS\\Sans exhausteur de goût", "Sans exhausteur de goût", "Eh bien, la cuisine industrielle, c'est celle qui est justement avec des additifs et des exhausteurs de goût et toutes ces cochonneries."},
                {"A-0000016", "Production Ingrédients et compositions\\Composition SANS\\Sans huile de palme", "Sans huile de palme", "Et après s'il y a trop d'huile, des huiles de palme, des machins hydrogénés, tout ça, de temps en temps, je ne prends pas."},
                {"A-0000017", "Production Ingrédients et compositions\\Composition SANS\\Sans ingrédients chimiques", "Sans ingrédients chimiques", "Il faut faire quelque chose sans produits chimiques."},
                {"A-0000018", "Production Ingrédients et compositions\\Composition SANS\\Sans ingrédients inatendus", "Sans ingrédients inatendus", "On s'est aperçu que même dans des produits qui n'ont pas besoin de sucre, on trouve du sucre."},
                {"A-0000019", "Production Ingrédients et compositions\\Composition SANS\\Sans ingrédients méconnus", "Sans ingrédient méconnus", "Je me dis, il y a deux trucs, je ne sais pas ce que c'est.  Ça ne doit pas être bon, quoi."},
                {"A-0000020", "Production Ingrédients et compositions\\Composition SANS\\Sans ou moins de sucre ou sel", "Sans (moins) de sucre ou de sel", "Effectivement, je fais toujours attention aux produits où la teneur en sel est la plus basse possible."},
                {"A-0000021", "Production Ingrédients et compositions\\Longueur liste d'ingrédients\\Liste d'ingrédients longue", "Liste d'ingrédients longue = produit pas naturel", "Là, il y a une liste plus longue.  Déjà, ce n'est pas forcément terrible."},
                {"A-0000022", "Production process\\Degré de transformation\\Transformé", "Transformation", "Un produit naturel, c'est un produit qui n'est pas transformé"},
                {"A-0000023", "Production process\\Degré de transformation\\Très peu transformé", "Très peu de transformation", "Pour moi, le raisin sec, c'est un produit qui va être légèrement transformé.  Il va être séché au soleil.  Mais on n'a rien ajouté dedans."},
                {"A-0000024", "Production process\\Degré de transformation\\Ultra-transformé", "Ultra-transformation", "De se retrouver à manger des trucs ultra transformés qui vont nuire et à la santé et à l'environnement parce que emballage dans tous les sens, produits issus de transformations pas possibles."},
                {"A-0000025", "Production process\\Mode de conservation\\Barquette", "Viande en barquette", "Par exemple, la viande que j'achète à la boucherie directement, mais qui est mise en barquette par le boucher, pour moi, c'est presque naturel."},
                {"A-0000026", "Production process\\Mode de conservation\\Conserve", "Conserve", "Mais je prends vachement de produits non transformés.  Après, si je veux que ce soit des péremptions longues, je vais prendre des conserves ou des choses comme ça"},
                {"A-0000027", "Production process\\Mode de conservation\\Sous vide", "Sous-vide", "Parce qu'ils sont bruts, il n'y a pas d'ajout dessus.  Le produit tel qu'il est, il y a juste un moyen de conservation.  Même pas pour le poulet, par exemple, il est juste sous vide."},
                {"A-0000028", "Production process\\Mode de conservation\\Surgelation", "Surgelation : fraicheur et goût  de produits bruts (pas de référence à la composition)", "Je n'ai rien contre le congelé.  J'achète les épinards, des petits pois congelés, les haricots verts.  Oui, oui, oui.  Mais je trouve que c'est meilleur que d'acheter des légumes qu'on laisse longtemps à la lumière."},
                {"A-0000029", "Production process\\Mode de production\\Fait maison", "Fait maison", "Parce que moi quand je fais ma ratatouille maison, je mets pas de l'amidon modifié de maïs et je mets pas d'acide citrique non plus quoi // Si je le fais moi-même, c'est quand même plus naturel."},
                {"A-0000030", "Production process\\Mode de production\\Industriel", "Industriel", "C'est-à-dire que quand on dit la nourriture industrielle, tout le monde pense aux plats préparés avec plein d'additifs (...) J'accepte qu'ils soient industrialisés à condition qu'ils soient avec des produits naturels.  Exclusivement."},
                {"A-0000031", "Production process\\Mode de production\\Production à petite échelle", "Petite production", "il y a tout un côté où petit producteur, là, on sait bien que ça peut être que naturel."},
                {"A-0000032", "Production process\\Mode de production\\Production artisanale, traditionnelle", "Fait main, sans mécanisation à la chaine", "Alors, madeleines, oui, (j'achète) quand elles sont fabriquées par des gens que je connais.  Je connais beaucoup de personnes qui font des madeleines, mais c'est fait de manière artisanale."},
                {"A-0000033", "Production process\\Mode de production\\Production locale", "Production locale", "Pour moi, ce sont tous des produits d'à côté.  Pour moi, ça me plaît.  C'est marqué filière locale responsable."},
                {"A-0000034", "Production process\\Type de transformation \\Assemblage", "Assemblage Mélange", "C'est déjà transformé, parce que tu as ta vinaigrette dessus et tout (...) Après, c'est un assemblage."},
                {"A-0000035", "Production process\\Type de transformation \\Cuisson", "Cuisson perçue comme naturelle", "Pour moi, ce qui est cru va être naturel, mais ce qui est naturel n'est pas forcément cru (...) Ça peut être cuit. En fait, on peut cuire des aliments avec du citron, par exemple.  On peut cuire avec... On appelle ça cuire, c'est une sorte de cuisson, avec du sel, des choses comme ça.  Et de toute façon, avec du feu, oui, et ça reste naturel.  Pour moi, si je mets ma tranche d'aubergine au-dessus du barbec, elle est naturelle."},
                {"A-0000036", "Production process\\Type de transformation \\Cuisson", "Cuisson perçue comme une transformation", "Là par exemple, je prends, et j'aime bien, la courgette. Elle est plus naturelle. Elles ont été déjà prises dans quelque chose, elles ont été cuites.  C'est pas pour ça qu'elles ne sont pas bonnes. Mais ce n'est pas naturel."},
                {"A-0000037", "Production process\\Type de transformation \\Découpe Broyage mixage", "Découpe Broyage mixage perçue comme naturel", "Éventuellement, la farine, parce qu'en soi, c'est juste du blé broyé (...) Si vraiment c'était juste le potiron qu'ils avaient cuit et qu'ils avaient mixé comme on fait nous à la maison, ça reste très naturel."},
                {"A-0000038", "Production process\\Type de transformation \\Découpe Broyage mixage", "Techniques Perçue comme transformant le produit naturel", "De la chair broyée, par exemple, pour faire une viande hachée, ça va être un produit transformé //Parce que je pense à mes fruits et légumes que je prends congelés, qui me facilitent un peu la vie quand j'en ai vite.  Je me dis que techniquement, ils sont transformés.  Ils ne sont plus comme on les achèterait.  Enfin, notre oignon, parce que c'est surtout les oignons que je prends.  Ce n'est pas comme l'oignon que j'achète directement au primeur.  Donc, il y a le côté découpe."},
                {"A-0000039", "Produit final\\Aspects sensoriel \\Frais", "Frais", "Ils ont l'air toujours aussi assez frais (produits de la boulangerie).  C'est vraiment une pâtisserie, donc ils font vraiment leurs choses eux-mêmes.  On sent quand même que c'est fait maison et c'est pas tout mou comme on peut retrouver... dans les chaînes qui mettent juste au four."},
                {"A-0000040", "Produit final\\Aspects sensoriel \\Goût", "Goût naturel", "Et puis, quand c'est en escalope, comme moi je cuisine, du coup, je ne prends pas forcément le label rouge pour les escalopes.  Alors que si je fais un poulet rôti au four, je vais prendre plus un label rouge."},
                {"A-0000041", "Produit final\\Aspects sensoriel \\Texture", "Texture", "Le fromage à raclette par exemple, le truc en supermarché, c'est dégueulasse.  De toute façon, quand vous cuisez, C'est de l'huile, en fait.  C'est que de l'huile.  Mais vous achetez ça à la laitière.  pour le fromage à raclette, c'est différent.  Il se tient bien."},
                {"A-0000042", "Produit final\\Aspects sensoriel \\Apparence", "Apparence visuelle du produit (indépendemment du packaging)", "Le jambon que j'achète chez Biocoop, il est gris.  Il n'y a pas de colorant.  Et le jambon chez Leclerc, (Marque connue) par exemple, il est rose."},
                {"A-0000043", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Autres Allégations liées au Naturel", "Allégations hors label et certification", "Par exemple, si les œufs que j'achète de temps en temps aussi, je vais plutôt prendre des œufs en plein air.  C'est le minimum."},
                {"A-0000044", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Label Rouge", "Labels Rouge", "Vous achetez n'importe quel autre produit que label rouge, la viande va se décoller de l'os.  Tandis que là, vous vous rendez compte que la viande reste quand même collée à l'os.  Donc c'est une viande plus naturelle."},
                {"A-0000045", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Labels Biologiques", "Labels Biologiques", "Je ne me permets pas vraiment d'acheter des choses hors bio. //c'est me dire ah c'est un bio français.  je sais que le cahier des charges est un peu plus strict.  si je vois l'exemple que je prenais tout à l'heure NL néerlandais je suis pas sûr que le cahier des charges soit aussi drastique que pour l'agriculture bio française."},
                {"A-0000046", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Marque", "Marque perçue comme naturelle", "Par exemple, je prends beaucoup les produits Hénaff qui sont de qualité, sans colorant.  Ça fait longtemps que je prends ça."},
                {"A-0000047", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Nutriscore", "Nutriscore comme critère d'achat", "Je regarde le Nutri-Score en premier.  Oui.  Après, pour la ratatouille, c'est à peu près toujours Nutri-Score A"},
                {"A-0000048", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Origine", "Origine, provenance et traçabilité", "Origine France.  Ça, j'aime bien aussi quand c'est Origine France."},
                {"A-0000049", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Valeurs nutritionnelles", "Teneur en matière grasse", "J'essaye de regarder la teneur en matière grasse."},
                {"A-0000050", "Produit final\\Caractéristiques du packaging  \\Matière Packaging \\Verre", "Emballage en verre, principalement des bocaux", "Je préférais le verre, pour une histoire de santé puis de... de recyclage."},
                {"A-0000051", "Produit final\\Caractéristiques du packaging  \\Matière Packaging \\Plastique", "Perception négative du plastique", "Le fait d'utiliser du plastique,  ça m'énerve...  tu vois donc je me dis que c'est pas naturel non plus. (...) parce que tu as le transfert déjà qui se fait."},
                {"A-0000052", "Produit final\\Caractéristiques du packaging  \\Visuel du packaging \\Visuel", "Images, couleurs et autres éléments visuels associés à la naturalité", "Pour renforcer le côté bio, nature, tout ça, il y a un petit coin de ciel bleu avec des rizières pour représenter le mode de culture.  Le code couleur, ce qu'on disait tout à l'heure, le côté un petit peu beige brut d'aspect, comme si c'était un papier brut avec le vert du carrefour bio.  C'est ça qui fait l'image nature."},
                {"A-0000053", "Mode de culture et d'élevage\\Elevage perçu comme naturel", "Bien être animal comme garantie de la naturalité", "Ça va être les œufs aussi (...)  c'est juste qu'ils sont pas dans la cage (...) Oui, c'est le bien-être animal peut-être."},
                {"A-0000054", "Mode de culture et d'élevage\\Mode de culture\\Agriculture sans intrants chimiques", "Agriculture sans intrants chimiques", "(...) parce que le chou-fleur de plein champ qui a reçu 10 traitements de pesticides pendant sa croissance peut être jugé comme non naturel."},
                {"A-0000055", "Mode de culture et d'élevage\\Mode de culture\\Culture de saison", "Culture de saison", "Si on prend une tomate, une tomate bio cultivée en plein champ qui est récoltée au mois de mai sera, pour moi en tout cas, plus naturelle qu'une tomate conventionnelle sous serre chauffée...Des produits de saison aussi un peu, c'est ça."},
                {"A-0000056", "Mode de culture et d'élevage\\Mode de culture\\Pêche durable", "Pêche durable", "On va avoir les ressources pêche durable, pour raccrocher un peu la naturalité ici."},
                {"A-0000057", "Production Ingrédients et compositions\\Composition AVEC\\Colorants naturels", "Colorants naturels", "je vais mettre un petit peu d'extraits de betterave par exemple. Du coup, c'est une forme de coloration naturelle"},
                {"A-0000058", "Production Ingrédients et compositions\\Composition AVEC\\Ingrédients à partir de produits bruts", "Fait avec des produits bruts, simple et reconnaissables", "ce muffin pour moi qui devrait être naturel, il serait fait avec de la farine complète, avec du sucre muscovado, ou un sucre de canne vraiment brut (...) C'est les ingrédients du placard."},
                {"A-0000059", "Production Ingrédients et compositions\\Longueur liste d'ingrédients\\Liste d'ingrédients longue", "Liste d'ingrédients longue = produit pas naturel", "il y a aussi une notion de quantité d'ingrédients. Instinctivement, ça me paraît plus naturel de proposer un produit aux consos qui aient une liste courte plutôt que même… enfin, il y a la qualité des ingrédients en premier lieu, mais aussi la quantité en deuxième lieu,"},
                {"A-0000060", "Production Ingrédients et compositions\\Composition SANS\\Sans additifs", "Sans additifs", "Naturalité, j'entends aussi absence d'additifs, oui, et peu transformé, mais surtout l'absence d'additifs."},
                {"A-0000061", "Production Ingrédients et compositions\\Composition SANS\\Sans arômes", "Sans arômes", "En fait, à partir du moment où il y a arôme, pour moi, c'est pas naturel."},
                {"A-0000062", "Production Ingrédients et compositions\\Composition SANS\\Sans arômes artificiels", "Sans arômes artificiels", "Si je veux que ça soit très naturel, je vais y mettre une épice. Si je veux un intermédiaire, je vais mettre un arôme naturel de, et après, je peux mettre juste un arôme qui sera un artificiel."},
                {"A-0000063", "Production Ingrédients et compositions\\Composition SANS\\Sans conservateurs", "Sans conservateurs", "de la purée de tomate, auquel je vais rajouter un conservateur ou un antioxydant, je perds ce côté naturel"},
                {"A-0000064", "Production Ingrédients et compositions\\Composition SANS\\Sans ingrédients chimiques", "Sans ingrédients à connotation chimiques", "On a parlé de maltodextrine tout à l'heure. C'est un mot compliqué, mais c'est quelque chose de simple derrière. C'est pas naturel."},
                {"A-0000065", "Production Ingrédients et compositions\\Composition SANS\\Sans ingrédients inattendus", "Sans ingrédients inattendus", "il y a surtout de l'eau. Comme tu dis, c'est vraiment pas naturel (rire). Du coup, l'eau… enfin en fait, je m'attends à manger quelque chose et du coup, c'est un truc qui est dilué, enfin c'est de l'eau."},
                {"A-0000066", "Production Ingrédients et compositions\\Composition SANS\\Sans ou moins de sucre ou sel", "Sans (moins) de sucre ou de sel", "Dans la compote à partir… enfin, tu mixes ta po… enfin, tu la mixes, elle est naturelle. Tu rajoutes du sucre, elle est artificielle."},
                {"A-0000067", "Production Ingrédients et compositions\\Composition SANS\\Sans OGM", "Sans OGM", "(les produits bruts sont naturels) sauf si tu mets des OGM derrière."},
                {"A-0000068", "Production Ingrédients et compositions\\Composition SANS\\Sans émulsifiants", "Sans émulsifiants", "on sait que farine de blé, ça va faire plus naturel que matière grasse composée. Mais on sait aussi par exemple que s'il y a de l'émulsifiant, du E471 dans ma céréale, c'est parce qu'on peut pas faire autrement technologiquement,"},
                {"A-0000069", "Production Ingrédients et compositions\\Composition SANS\\Sans Sirop de glucose fructose", "Sans Sirop de glucose fructose", "dans notre charte, on interdit le sirop de glucose fructose, mais on autorise le sirop de glucose."},
                {"A-0000070", "Production process\\Degré de transformation\\Transformé", "Transformation", "(...) brut, du coup, ça veut dire pas transformé,"},
                {"A-0000071", "Production process\\Degré de transformation\\Très peu transformé", "Très peu de transformation", "il y a justement le fait de pas avoir la transformation des mains de l'homme et du coup pour moi, ça s'arrête très bas (rire)."},
                {"A-0000072", "Production process\\Degré de transformation\\Ultra-transformé", "Ultra-transformation", "C'est un produit sans ultra-transformation, c'est un produit qui se rapproche du naturel, qu'on peut faire soi-même chez soi"},
                {"A-0000073", "Production process\\Mode de conservation\\Conserve", "Conserve pas naturelle", "c'est que la conserve dénature plus le produit"},
                {"A-0000074", "Production process\\Mode de conservation\\Conserve", "Conserve naturelle", "il peut très bien acheter une conserve de légumes naturelle où effectivement, il y a rien dedans de plus que le légume et du sel. Pour moi, ça reste naturel"},
                {"A-0000075", "Production process\\Mode de conservation\\Surgelation et congélation", "Surgélation congélation  pas naturelle", "c'est quand même très artificiel parce qu'on applique des contraintes au produit et ensuite on va avoir des conditions de stockage et qu'on impose aussi au consommateur derrière pour avoir un produit."},
                {"A-0000076", "Production process\\Mode de conservation\\Surgelation et congélation", "Surgélation congélation naturelle", "voilà, qu'on découpe, qu'on blanchît, qu'on surgèle, pour moi, c'est naturel, quoi."},
                {"A-0000077", "Production process\\Mode de conservation\\Produits crus prêts à l'emploi", "4éme gamme pas naturelle", "je considère ça très antinaturel, une salade en sachet alors qu'on peut se dire, ben j'ai que de la salade et un sachet."},
                {"A-0000078", "Production process\\Mode de production\\Fait maison", "Fait maison", "qu'on peut faire soi-même chez soi, entre guillemets, avec des ingrédients naturels."},
                {"A-0000079", "Production process\\Type de transformation \\Cuisson", "Cuisson perçue comme naturelle", "À la maison, on va couper, une étape de découpe, de cuisson, elle est naturelle,"},
                {"A-0000080", "Production process\\Type de transformation \\Cuisson", "Cuisson perçue comme une transformation", "Tu as perdu ben une partie nutritionnelle, tu as fait chauffer. Et puis tu transformes complètement ton aliment. Donc il a plus rien à voir, mais tu prends un pop-corn. Tu vois, tu te dis, ben, il a complètement changé."},
                {"A-0000081", "Production process\\Type de transformation \\Découpe Broyage mixage", "Découpe Broyage mixage perçue comme naturel", "Sans mélange, tu vois, quand tu reprends les zones de ta pomme, que tu la cueilles et que tu la coupes, elle est toujours brute."},
                {"A-0000082", "Production process\\Type de transformation \\Découpe Broyage mixage", "Techniques Perçue comme transformant le produit naturel", "personnellement, je le mets pas sur le terme brut, du coup, le fait qu'elle soit coupée."},
                {"A-0000083", "Production process\\Type de transformation \\Lyophilisation", "Lyphilisation pas naturelle", "Mais l'action de lyophiliser, même si ça reste un process pas si compliqué que ça, amène quand même un certain degré de transformation."},
                {"A-0000084", "Production process\\Type de transformation \\Soufflage", "Soufflage perçu comme naturel", "maintenant, c'est juste du riz soufflé ... moi, je trouve ça naturel"},
                {"A-0000085", "Production process\\Type de transformation \\Fermentation", "Fermentation naturelle", "on fait fermenter les légumes, c'est plus naturel, on fait tout fermenter spontanément, on n'ajoute pas de bactérie."},
                {"A-0000086", "Production process\\Type de transformation \\Fermentation", "Fermentation pas naturelle", "La fermentation est une altération, par définition. Le fromage est une altération, par définition."},
                {"A-0000087", "Production process\\Type de transformation \\Extrusion", "Extrusion naturelle", "Mais pour moi, une extrusion, c'est naturel.(...) J'ai pas, moi, un sentiment en tant qu'industriel que ce soit plus ou moins naturel que quand je fais un biscuit"},
                {"A-0000088", "Production process\\Type de transformation \\Extrusion", "Extrusion pas naturelle", "De mettre des céréales extrudées, par exemple, tu vois, là où tu viens dénaturer la céréale en elle-même."},
                {"A-0000089", "Production process\\Type de transformation \\Déshydratation", "Déshydratation", ""},
                {"A-0000090", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Labels Biologiques", "Labels Biologiques perçu comme naturel", ""},
                {"A-0000091", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Labels Biologiques", "Labels Biologiques ne garantissant pas le naturel", "Un beurre végétal, c'est absolument pas naturel. Oui, c'est pas parce que c'est bio ou biologique, qu'on peut pas rajouter tout un tas d'ingrédients technologiques."},
                {"A-0000092", "Produit final\\Caractéristiques du packaging  \\Infos packaging\\Origine", "Origine, provenance et traçabilité", "l'origine France, si on prend sur la viande, alors en l'occurrence, nous, on était déjà passé en viande française quand ça s'est passé, mais de ce qu'on a eu sur la viande et les origines, aujourd'hui, la question, elle se pose presque plus."},
                {"A-0000093", "Produit final\\Caractéristiques du packaging  \\Matière Packaging \\Carton", "Carton Perception positive", "l'empreinte RSE de ce nouveau packaging s'est quand même améliorée parce que c'est du carton recyclé (...)  Donc moi, je me dis que ça va plutôt dans le bon sens de la naturalité."},
                {"A-0000094", "Produit final\\Caractéristiques du packaging  \\Matière Packaging \\Carton", "Carton Perception Complexe", "on va enlever le plastique et on met directement les produits dans un carton, mais effectivement, ton carton, il est filmé,"},
                {"A-0000095", "Produit final\\Caractéristiques du packaging  \\Matière Packaging \\Plastique", "Plastique  Perception négative", "Si on a le raisin sans emballage à côté, en moins naturel."},
                {"A-0000096", "Produit final\\Caractéristiques du packaging  \\Matière Packaging \\Plastique", "Plastique perception complexe", "Le plastique, c'est pas si pire que ça, par exemple. Enfin, c'est moins pire que d'autres choses"},
                {"A-0000097", "Produit final\\Caractéristiques du packaging  \\Matière Packaging \\Verre", "Perception positive verre", "Naturel, transparent, recyclable (...) réutilisable, enfin voilà, tout ce que… tous les bénéfices du verre."}
            };
            
            String outputExcel = NatclinnConf.folderForResults + "/MotsClefs_Arguments.xlsx";
            
            generateKeywordsExcel(data, outputExcel);
            
            System.out.println("Fichier Excel généré avec succès : " + outputExcel);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Génère un fichier Excel avec IDArgument et mots-clés extraits.
     * 
     * @param data Tableau 2D : [ligne][colonne] avec colonnes : IDArgument, Attribut, Description, Verbatim
     * @param outputFile Chemin du fichier Excel de sortie
     * @throws IOException si erreur d'écriture
     */
    private static void generateKeywordsExcel(String[][] data, String outputFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("MotsClefs");
            
            // En-tête
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("IDArgument");
            headerRow.createCell(1).setCellValue("MotClefs");
            
            // Parcourir les données
            int rowIdx = 1;
            for (String[] row : data) {
                String idArgument = row[0];
                String attribut = row[1];
                String description = row[2];
                String verbatim = row[3];
                
                // Extraire les mots-clés
                Set<String> keywords = extractKeywords(attribut, description, verbatim);
                
                // Créer la ligne Excel
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(idArgument);
                excelRow.createCell(1).setCellValue(String.join(", ", keywords));
            }
            
            // Auto-ajuster les colonnes
            sheet.autoSizeColumn(0);
            sheet.setColumnWidth(1, 20000); // Largeur fixe pour les mots-clés
            
            // Écrire le fichier
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
    
    /**
     * Extrait les mots-clés pertinents de plusieurs textes sources.
     * 
     * ALGORITHME :
     * ────────────
     * 1. Concatène tous les textes
     * 2. Normalise : minuscules, suppression ponctuation, accents
     * 3. Tokenise : découpe en mots
     * 4. Filtre : supprime stopwords, mots courts (< 3 car)
     * 5. Déduplique et trie alphabétiquement
     * 
     * @param texts Textes sources (attribut, description, verbatim, etc.)
     * @return Ensemble trié de mots-clés uniques
     */
    private static Set<String> extractKeywords(String... texts) {
        Set<String> keywords = new TreeSet<>(); // TreeSet pour tri automatique
        
        // Concaténer tous les textes
        StringBuilder fullText = new StringBuilder();
        for (String text : texts) {
            if (text != null && !text.isEmpty()) {
                fullText.append(" ").append(text);
            }
        }
        
        // Normaliser et tokeniser
        String normalized = normalizeText(fullText.toString());
        String[] tokens = normalized.split("\\s+");
        
        // Filtrer et collecter les mots-clés
        for (String token : tokens) {
            token = token.trim();
            
            // Filtrer : longueur minimale et pas dans les stopwords
            if (token.length() >= 3 && !STOPWORDS.contains(token.toLowerCase())) {
                keywords.add(token);
            }
        }
        
        return keywords;
    }
    
    /**
     * Normalise un texte pour l'extraction de mots-clés.
     * 
     * TRANSFORMATIONS :
     * ─────────────────
     * 1. Minuscules
     * 2. Suppression des accents (é→e, à→a, etc.)
     * 3. Remplacement des séparateurs spéciaux (\, /, parenthèses) par espaces
     * 4. Suppression de toute ponctuation
     * 5. Normalisation des espaces multiples
     * 
     * @param text Texte à normaliser
     * @return Texte nettoyé et normalisé
     */
    private static String normalizeText(String text) {
        if (text == null) return "";
        
        // Minuscules
        text = text.toLowerCase();
        
        // Supprimer les accents
        text = text.replaceAll("[àâä]", "a")
                   .replaceAll("[éèêë]", "e")
                   .replaceAll("[îï]", "i")
                   .replaceAll("[ôö]", "o")
                   .replaceAll("[ùûü]", "u")
                   .replaceAll("[ÿ]", "y")
                   .replaceAll("ç", "c")
                   .replaceAll("œ", "oe")
                   .replaceAll("æ", "ae");
        
        // Remplacer les backslash et slash par des espaces (pour les hiérarchies d'attributs)
        text = text.replaceAll("[/\\\\]", " ");
        
        // Remplacer les parenthèses et autres ponctuations par des espaces
        text = text.replaceAll("[()\\[\\]{}\"']", " ");
        
        // Supprimer toute ponctuation restante
        text = text.replaceAll("[^a-z0-9\\s-]", " ");
        
        // Normaliser les espaces multiples
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
}
