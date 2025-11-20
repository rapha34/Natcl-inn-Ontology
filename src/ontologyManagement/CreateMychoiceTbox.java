package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

// utiliser le modele ontologique
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import natclinn.util.NatclinnConf;


public class CreateMychoiceTbox {


	public static <ValuesFromRestriction> void main( String[] args ) {

		String jsonString = CreationTBox();
		
		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		RDFParser.fromString(jsonString,Lang.JSONLD11).parse(om);

		try {   

			//////////////////////////////
			// Sorties fichiers         //
			////////////////////////////// 

			FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/MyChoiceTbox.xml");
			// exporte le resultat dans un fichier
			om.write(outStream, "RDF/XML");

			// N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
			om.write(System.out, "N3");

			outStream.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
		catch (IOException e) {System.out.println("IO problem");}
	}

	public static String CreationTBox() {

		String jsonString = null;

		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...	
		new NatclinnConf();  

		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
	    ///////////////////////////////
	    //Définition des namespaces  //
	    ///////////////////////////////
	    
	    String mch = new String("https://w3id.org/MCH/ontology/");
	    om.setNsPrefix("mch", mch);
		String aif = new String("http://www.arg.dundee.ac.uk/aif#");
	    om.setNsPrefix("aif", aif);
	    String dcat = new String("http://www.w3.org/ns/dcat#/");
	    om.setNsPrefix("dcat", dcat);
		String prov = new String("http://www.w3.org/ns/prov#");
	    om.setNsPrefix("prov", prov);
		String dct = new String("http://purl.org/dc/terms/"); 
	    om.setNsPrefix("dct", dct);
		String skos = new String("http://www.w3.org/2004/02/skos/core#");
	    om.setNsPrefix("skos", skos); 
	    String foaf = new String("http://xmlns.com/foaf/0.1/");
	    om.setNsPrefix("foaf", foaf);
	    String rdfs = new String("http://www.w3.org/2000/01/rdf-schema#");
	    om.setNsPrefix("rdfs", rdfs);

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////
	    
	    Ontology ont = om.createOntology(mch + "MyChoiceTbox");
		om.add(ont, RDFS.label,"Model Ontology of MyChoice", "en");
		om.add(ont, DC.description,"Tbox for the Mychoice ontology");
		om.add(ont, DC.creator,"Pierre BISQUERT");	
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");	
		om.add(ont, DC.creator,"Rallou THOMOPOULOS");	
		om.add(ont, RDFS.comment,"Ontologie pour la modélisation d'un système d'aide à la décision multicritère avec parties prenantes, projets, critères et alternatives", "fr");


	    //////////////////////////////////////////////////////////////////////
	    //														            //
	    //				TBOX = terminological box				            //				
	    //	ensemble de formules relatives aux informations terminologiques	//
	    //	(i.e. notions de bases et relations entre elles)                //
	    //       													        //
	    //////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////
//Axiomisation des propriétés et des relations	//
//terminologie DL: concepts et rôles	        // 
//////////////////////////////////////////////////

	    ////////////////////////////////////////////
	    // Définition des concepts atomiques      //
	    ////////////////////////////////////////////		

	    // MCH est un ensemble de produits et d'arguments
	    OntClass MCH = om.createClass(mch + "MCH");
	    MCH.addComment("MCH is the set of product and arguments pour My Choice.", "en");
		MCH.addComment("MCH est l'ensemble des produits et des arguments pour My Choice.", "fr");
	    
		// Classe Stakeholder
        OntClass Stakeholder = om.createClass(mch + "Stakeholder");
        Stakeholder.addLabel("Partie Prenante", "fr");
        Stakeholder.addLabel("Stakeholder", "en");
        Stakeholder.addComment("Une partie prenante impliquée dans le processus de décision.", "fr");
        Stakeholder.addComment("A stakeholder involved in the decision-making process.", "en");

		// Classe Project
        OntClass Project = om.createClass(mch + "Project");
        Project.addLabel("Projet", "fr");
        Project.addLabel("Project", "en");
        Project.addComment("Un projet soumis à évaluation dans le processus de décision.", "fr");
        Project.addComment("A project subject to evaluation in the decision-making process.", "en");
        
        // Classe Criterion
        OntClass Criterion = om.createClass(mch + "Criterion");
        Criterion.addLabel("Critère", "fr");
        Criterion.addLabel("Criterion", "en");
        Criterion.addComment("Un critère d'évaluation utilisé pour juger les alternatives.", "fr");
        Criterion.addComment("An evaluation criterion used to judge alternatives.", "en");
        
        // Classe Aim
        OntClass Aim = om.createClass(mch + "Aim");
        Aim.addLabel("Objectif", "fr");
        Aim.addLabel("Aim", "en");
        Aim.addComment("Un objectif ou but associé à un critère.", "fr");
        Aim.addComment("An objective or goal associated with a criterion.", "en");
        
        // Classe Alternative
        OntClass Alternative = om.createClass(mch + "Alternative");
        Alternative.addLabel("Alternative", "fr");
        Alternative.addLabel("Alternative", "en");
        Alternative.addComment("Une alternative évaluée selon différents critères.", "fr");
        Alternative.addComment("An alternative evaluated according to different criteria.", "en");
        
        // Classe Argument
        OntClass Argument = om.createClass(mch + "Argument");
        Argument.addLabel("Argument", "fr");
        Argument.addLabel("Argument", "en");
        Argument.addComment("Un argument ou justification lié à une évaluation.", "fr");
        Argument.addComment("An argument or justification related to an evaluation.", "en");
        
        // Classe Property
        OntClass Property = om.createClass(mch + "Property");
        Property.addLabel("Propriété", "fr");
        Property.addLabel("Property", "en");
        Property.addComment("Une propriété mesurable d'une alternative.", "fr");
        Property.addComment("A measurable property of an alternative.", "en");
        
        // Classe QualValue
        OntClass QualValue = om.createClass(mch + "QualValue");
        QualValue.addLabel("Valeur Qualitative", "fr");
        QualValue.addLabel("Qualitative Value", "en");
        QualValue.addComment("Une valeur qualitative attribuée à une propriété.", "fr");
        QualValue.addComment("A qualitative value assigned to a property.", "en");
        
        // Classe Source
        OntClass Source = om.createClass(mch + "Source");
        Source.addLabel("Source", "fr");
        Source.addLabel("Source", "en");
        Source.addComment("Une source d'information ou de données.", "fr");
        Source.addComment("A source of information or data.", "en");
        
        // Classe TypeSource
        OntClass TypeSource = om.createClass(mch + "TypeSource");
        TypeSource.addLabel("Type de Source", "fr");
        TypeSource.addLabel("Source Type", "en");
        TypeSource.addComment("Le type ou la catégorie d'une source.", "fr");
        TypeSource.addComment("The type or category of a source.", "en");
        
        // Classe IncompatibleValues
        OntClass IncompatibleValues = om.createClass(mch + "IncompatibleValues");
        IncompatibleValues.addLabel("Valeurs Incompatibles", "fr");
        IncompatibleValues.addLabel("Incompatible Values", "en");
        IncompatibleValues.addComment("Ensemble de valeurs mutuellement incompatibles.", "fr");
        IncompatibleValues.addComment("Set of mutually incompatible values.", "en");
        
        // Classe HasExpertise
        OntClass HasExpertise = om.createClass(mch + "HasExpertise");
        HasExpertise.addLabel("Expertise", "fr");
        HasExpertise.addLabel("Expertise", "en");
        HasExpertise.addComment("Relation d'expertise entre une partie prenante et un domaine.", "fr");
        HasExpertise.addComment("Expertise relationship between a stakeholder and a domain.", "en");
		
		
	    ////////////////////////////////////////////
	    // Définition des disjonctions de classes //
	    ////////////////////////////////////////////

	    // add disjoint individuals axiom assertion:
	   
	    List<OntClass> classes = Arrays.asList(
			Stakeholder,
			Project,
			Criterion,
			Aim,
			Alternative,
			Argument,
			Property,
			QualValue,
			Source,
			TypeSource,
			IncompatibleValues,
			HasExpertise
			
		);

		// Création de toutes les disjonctions possibles entre chaque paire de classes
		for (int i = 0; i < classes.size(); i++) {
			for (int j = i + 1; j < classes.size(); j++) {
				classes.get(i).addDisjointWith(classes.get(j));
			}
		}


	    //////////////////////////////////////////////////////////
	    // Définition des object property                       //
	    //////////////////////////////////////////////////////////
		
	    // hasStakeholder
        ObjectProperty hasStakeholder = om.createObjectProperty(mch + "hasStakeholder");
        hasStakeholder.addLabel("a pour partie prenante", "fr");
        hasStakeholder.addLabel("has stakeholder", "en");
        hasStakeholder.addDomain(Argument);
        hasStakeholder.addRange(Stakeholder);
        
        // hasAlternative
        ObjectProperty hasAlternative = om.createObjectProperty(mch + "hasAlternative");
        hasAlternative.addLabel("a pour alternative", "fr");
        hasAlternative.addLabel("has alternative", "en");
        hasAlternative.addDomain(Argument);
        hasAlternative.addRange(Alternative);
        
        // hasProject
        ObjectProperty hasProject = om.createObjectProperty(mch + "hasProject");
        hasProject.addLabel("a pour projet", "fr");
        hasProject.addLabel("has project", "en");
        hasProject.addDomain(Alternative);
        hasProject.addRange(Project);

        // hasProperty
        ObjectProperty hasProperty = om.createObjectProperty(mch + "hasProperty");
        hasProperty.addLabel("a pour propriété", "fr");
        hasProperty.addLabel("has property", "en");
        hasProperty.addDomain(Argument);
        hasProperty.addRange(Property);
        
        // hasQualValue
        ObjectProperty hasQualValue = om.createObjectProperty(mch + "hasQualValue");
        hasQualValue.addLabel("a pour valeur qualitative", "fr");
        hasQualValue.addLabel("has qualitative value", "en");
        hasQualValue.addDomain(Argument);
        hasQualValue.addRange(QualValue);
        
        // hasSource
        ObjectProperty hasSource = om.createObjectProperty(mch + "hasSource");
        hasSource.addLabel("a pour source", "fr");
        hasSource.addLabel("has source", "en");
        hasSource.addDomain(Argument);
        hasSource.addRange(Source);
        
        // hasAim
        ObjectProperty hasAim = om.createObjectProperty(mch + "hasAim");
        hasAim.addLabel("a pour objectif", "fr");
        hasAim.addLabel("has aim", "en");
        hasAim.addDomain(Argument);
        hasAim.addRange(Aim);
        
        // belongsToProject
        ObjectProperty belongsToProject = om.createObjectProperty(mch + "belongsToProject");
        belongsToProject.addLabel("appartient au projet", "fr");
        belongsToProject.addLabel("belongs to project", "en");
        belongsToProject.addDomain(Argument);
        belongsToProject.addRange(Project);
        
        // hasCriterion
        ObjectProperty hasCriterion = om.createObjectProperty(mch + "hasCriterion");
        hasCriterion.addLabel("a pour critère", "fr");
        hasCriterion.addLabel("has criterion", "en");
        hasCriterion.addDomain(Aim);
        hasCriterion.addRange(Criterion);
        
        // hasTypeSource
        ObjectProperty hasTypeSource = om.createObjectProperty(mch + "hasTypeSource");
        hasTypeSource.addLabel("a pour type de source", "fr");
        hasTypeSource.addLabel("has source type", "en");
        hasTypeSource.addDomain(Source);
        hasTypeSource.addRange(TypeSource);
        
        // hasIncompatibleValues
        ObjectProperty hasIncompatibleValues = om.createObjectProperty(mch + "hasIncompatibleValues");
        hasIncompatibleValues.addLabel("a des valeurs incompatibles", "fr");
        hasIncompatibleValues.addLabel("has incompatible values", "en");
        hasIncompatibleValues.addDomain(QualValue);
        hasIncompatibleValues.addRange(IncompatibleValues);
        
        // expertiseIn
        ObjectProperty expertiseIn = om.createObjectProperty(mch + "expertiseIn");
        expertiseIn.addLabel("expertise dans", "fr");
        expertiseIn.addLabel("expertise in", "en");
        expertiseIn.addDomain(Stakeholder);
        expertiseIn.addRange(HasExpertise);

	    
	    //////////////////////////////////////////////////////////
	    // Définition des data property                         //
	    //////////////////////////////////////////////////////////
	    // Propriétés pour Stakeholder
        DatatypeProperty stakeholderName = om.createDatatypeProperty(mch + "stakeholderName");
        stakeholderName.addLabel("nom de la partie prenante", "fr");
        stakeholderName.addLabel("stakeholder name", "en");
        stakeholderName.addDomain(Stakeholder);
        stakeholderName.addRange(XSD.xstring);
        
        // Propriétés pour Project
        DatatypeProperty projectName = om.createDatatypeProperty(mch + "projectName");
        projectName.addLabel("nom du projet", "fr");
        projectName.addLabel("project name", "en");
        projectName.addDomain(Project);
        projectName.addRange(XSD.xstring);
        
        DatatypeProperty projectDescription = om.createDatatypeProperty(mch + "projectDescription");
        projectDescription.addLabel("description du projet", "fr");
        projectDescription.addLabel("project description", "en");
        projectDescription.addDomain(Project);
        projectDescription.addRange(XSD.xstring);
        
        DatatypeProperty projectImage = om.createDatatypeProperty(mch + "projectImage");
        projectImage.addLabel("image du projet", "fr");
        projectImage.addLabel("project image", "en");
        projectImage.addDomain(Project);
        projectImage.addRange(XSD.xstring);
        
        // Propriétés pour Criterion
        DatatypeProperty criterionName = om.createDatatypeProperty(mch + "criterionName");
        criterionName.addLabel("nom du critère", "fr");
        criterionName.addLabel("criterion name", "en");
        criterionName.addDomain(Criterion);
        criterionName.addRange(XSD.xstring);
        
        // Propriétés pour Aim
        DatatypeProperty aimDescription = om.createDatatypeProperty(mch + "aimDescription");
        aimDescription.addLabel("description de l'objectif", "fr");
        aimDescription.addLabel("aim description", "en");
        aimDescription.addDomain(Aim);
        aimDescription.addRange(XSD.xstring);
        
        // Propriétés pour Alternative
        DatatypeProperty nameAlternative = om.createDatatypeProperty(mch + "nameAlternative");
        nameAlternative.addLabel("nom de l'alternative", "fr");
        nameAlternative.addLabel("alternative name", "en");     
        nameAlternative.addDomain(Alternative);
        nameAlternative.addRange(XSD.xstring);
        
        DatatypeProperty alternativeDescription = om.createDatatypeProperty(mch + "alternativeDescription");
        alternativeDescription.addLabel("description de l'alternative", "fr");
        alternativeDescription.addLabel("alternative description", "en");
        alternativeDescription.addDomain(Alternative);
        alternativeDescription.addRange(XSD.xstring);
        
        DatatypeProperty typeProCon = om.createDatatypeProperty(mch + "typeProCon");
        typeProCon.addLabel("type pour/contre", "fr");
        typeProCon.addLabel("pro/con type", "en");
        typeProCon.addDomain(Alternative);
        typeProCon.addRange(XSD.xstring);
        
        DatatypeProperty infValue = om.createDatatypeProperty(mch + "infValue");
        infValue.addLabel("valeur inférieure", "fr");
        infValue.addLabel("lower value", "en");
        infValue.addDomain(Alternative);
        infValue.addRange(XSD.xfloat);
        
        DatatypeProperty supValue = om.createDatatypeProperty(mch + "supValue");
        supValue.addLabel("valeur supérieure", "fr");
        supValue.addLabel("upper value", "en");
        supValue.addDomain(Alternative);
        supValue.addRange(XSD.xfloat);
        
        DatatypeProperty unit = om.createDatatypeProperty(mch + "unit");
        unit.addLabel("unité", "fr");
        unit.addLabel("unit", "en");
        unit.addDomain(Alternative);
        unit.addRange(XSD.xstring);
        
        DatatypeProperty evaluationDate = om.createDatatypeProperty(mch + "evaluationDate");
        evaluationDate.addLabel("date d'évaluation", "fr");
        evaluationDate.addLabel("evaluation date", "en");
        evaluationDate.addDomain(Alternative);
        evaluationDate.addRange(XSD.date);
        
        DatatypeProperty isProspective = om.createDatatypeProperty(mch + "isProspective");
        isProspective.addLabel("est prospectif", "fr");
        isProspective.addLabel("is prospective", "en");
        isProspective.addDomain(Alternative);
        isProspective.addRange(XSD.xboolean);
        
        DatatypeProperty hasCoverage = om.createDatatypeProperty(mch + "hasCoverage");
        hasCoverage.addLabel("a une couverture", "fr");
        hasCoverage.addLabel("has coverage", "en");
        hasCoverage.addDomain(Alternative);
        hasCoverage.addRange(XSD.xint);
        
        DatatypeProperty confidenceLevel = om.createDatatypeProperty(mch + "confidenceLevel");
        confidenceLevel.addLabel("niveau de confiance", "fr");
        confidenceLevel.addLabel("confidence level", "en");
        confidenceLevel.addDomain(Alternative);
        confidenceLevel.addRange(XSD.xboolean);
        
        // Propriétés pour Argument
        DatatypeProperty assertion = om.createDatatypeProperty(mch + "assertion");
        assertion.addLabel("assertion", "fr");
        assertion.addLabel("assertion", "en");
        assertion.addDomain(Argument);
        assertion.addRange(XSD.xstring);
        
        DatatypeProperty explanation = om.createDatatypeProperty(mch + "explanation");
        explanation.addLabel("explication", "fr");
        explanation.addLabel("explanation", "en");
        explanation.addDomain(Argument);
        explanation.addRange(XSD.xstring);
        
        // Propriétés pour Property
        DatatypeProperty propertyName = om.createDatatypeProperty(mch + "propertyName");
        propertyName.addLabel("nom de la propriété", "fr");
        propertyName.addLabel("property name", "en");
        propertyName.addDomain(Property);
        propertyName.addRange(XSD.xstring);
        
        DatatypeProperty propertyUnit = om.createDatatypeProperty(mch + "propertyUnit");
        propertyUnit.addLabel("unité de la propriété", "fr");
        propertyUnit.addLabel("property unit", "en");
        propertyUnit.addDomain(Property);
        propertyUnit.addRange(XSD.xstring);
        
        // Propriétés pour QualValue
        DatatypeProperty qualitativeValue = om.createDatatypeProperty(mch + "qualitativeValue");
        qualitativeValue.addLabel("valeur qualitative", "fr");
        qualitativeValue.addLabel("qualitative value", "en");
        qualitativeValue.addDomain(QualValue);
        qualitativeValue.addRange(XSD.xstring);
        
        // Propriétés pour Source
        DatatypeProperty sourceName = om.createDatatypeProperty(mch + "sourceName");
        sourceName.addLabel("nom de la source", "fr");
        sourceName.addLabel("source name", "en");
        sourceName.addDomain(Source);
        sourceName.addRange(XSD.xstring);
        
        DatatypeProperty obtention = om.createDatatypeProperty(mch + "obtention");
        obtention.addLabel("obtention", "fr");
        obtention.addLabel("obtention", "en");
        obtention.addDomain(Source);
        obtention.addRange(XSD.xstring);
        
        DatatypeProperty sourceDate = om.createDatatypeProperty(mch + "sourceDate");
        sourceDate.addLabel("date de la source", "fr");
        sourceDate.addLabel("source date", "en");
        sourceDate.addDomain(Source);
        sourceDate.addRange(XSD.gYear);
        
        DatatypeProperty fiability = om.createDatatypeProperty(mch + "fiability");
        fiability.addLabel("fiabilité", "fr");
        fiability.addLabel("reliability", "en");
        fiability.addDomain(Source);
        fiability.addRange(XSD.xboolean);
        
        // Propriétés pour TypeSource
        DatatypeProperty typeSourceName = om.createDatatypeProperty(mch + "typeSourceName");
        typeSourceName.addLabel("nom du type de source", "fr");
        typeSourceName.addLabel("source type name", "en");
        typeSourceName.addDomain(TypeSource);
        typeSourceName.addRange(XSD.xstring);
        
        DatatypeProperty typeFiability = om.createDatatypeProperty(mch + "typeFiability");
        typeFiability.addLabel("fiabilité du type", "fr");
        typeFiability.addLabel("type reliability", "en");
        typeFiability.addDomain(TypeSource);
        typeFiability.addRange(XSD.xboolean);
        
        // Propriétés pour IncompatibleValues
        DatatypeProperty incompatibleValue1 = om.createDatatypeProperty(mch + "incompatibleValue1");
        incompatibleValue1.addLabel("valeur incompatible 1", "fr");
        incompatibleValue1.addLabel("incompatible value 1", "en");
        incompatibleValue1.addDomain(IncompatibleValues);
        incompatibleValue1.addRange(XSD.xint);
        
        DatatypeProperty incompatibleValue2 = om.createDatatypeProperty(mch + "incompatibleValue2");
        incompatibleValue2.addLabel("valeur incompatible 2", "fr");
        incompatibleValue2.addLabel("incompatible value 2", "en");
        incompatibleValue2.addDomain(IncompatibleValues);
        incompatibleValue2.addRange(XSD.xint);
        
        // Propriétés pour HasExpertise
        DatatypeProperty stakeholderNumber = om.createDatatypeProperty(mch + "stakeholderNumber");
        stakeholderNumber.addLabel("numéro de partie prenante", "fr");
        stakeholderNumber.addLabel("stakeholder number", "en");
        stakeholderNumber.addDomain(HasExpertise);
        stakeholderNumber.addRange(XSD.xint);
        
        DatatypeProperty criterionNumber = om.createDatatypeProperty(mch + "criterionNumber");
        criterionNumber.addLabel("numéro de critère", "fr");
        criterionNumber.addLabel("criterion number", "en");
        criterionNumber.addDomain(HasExpertise);
        criterionNumber.addRange(XSD.xint);
        
        DatatypeProperty projectNumber = om.createDatatypeProperty(mch + "projectNumber");
        projectNumber.addLabel("numéro de projet", "fr");
        projectNumber.addLabel("project number", "en");
        projectNumber.addDomain(HasExpertise);
        projectNumber.addRange(XSD.xint);

	
		//////////////////////////////////////////////////////////
	    // Définition des annotation property                   //
	    //////////////////////////////////////////////////////////
		om.createAnnotationProperty(skos + "prefLabel");
	    om.createAnnotationProperty(skos + "altLabel");
		om.createAnnotationProperty(skos + "definition");
		om.createAnnotationProperty(rdfs + "label");
		om.createAnnotationProperty(rdfs + "comment");
		om.createAnnotationProperty(dct + "created");
//////////////////////////////////////////////////////////
// Imchusion/Equivalence de concepts                    //
//////////////////////////////////////////////////////////


	/////////////////////////////
	// Imchusion de concepts   //
	/////////////////////////////

		// MCH est l'ensemble de tous les concepts définis
		MCH.addSubClass(Stakeholder);
		MCH.addSubClass(Project);
		MCH.addSubClass(Criterion);
		MCH.addSubClass(Aim);
		MCH.addSubClass(Alternative);
		MCH.addSubClass(Argument);
		MCH.addSubClass(Property);
		MCH.addSubClass(QualValue);
		MCH.addSubClass(Source);
		MCH.addSubClass(TypeSource);
		MCH.addSubClass(IncompatibleValues);
		MCH.addSubClass(HasExpertise);

		// Alternative doit avoir au moins un projet
        Alternative.addSuperClass(om.createMinCardinalityRestriction(null, belongsToProject, 1));
        
        // Argument doit avoir exactement une partie prenante
        Argument.addSuperClass(om.createCardinalityRestriction(null, hasStakeholder, 1));
        
        // Argument doit avoir exactement une alternative
        Argument.addSuperClass(om.createCardinalityRestriction(null, hasAlternative, 1));
	
	

		// exporte le resultat dans un fichier au format RDF/JSON
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
		try {
			jsonString = out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return jsonString;
	}
}