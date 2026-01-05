package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

// utiliser le modele ontologique
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.checkerframework.checker.units.qual.N;


public class CreateNatclinnClassificationAttributeOntology {
	
	public static <ValuesFromRestriction> void main( String[] args ) {

		String jsonString = CreationABox();
		
		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		RDFParser.create()
			.source(new StringReader(jsonString))
			.lang(Lang.JSONLD11)
			.parse(om);
		
		try {   

			//////////////////////////////
			// Sorties fichiers         //
			////////////////////////////// 


			FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnClassificationAttribute.xml");
			// exporte le resultat dans un fichier
			om.write(outStream, "RDF/XML");

			// N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
			//om.write(System.out, "N3");

			outStream.close();
			
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
		catch (IOException e) {System.out.println("IO problem");}
	}

	public static String CreationABox() {

		String jsonString = null;

		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...	
		new NatclinnConf();  

		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

		///////////////////////////////
	    //Définition des namespaces  //
	    ///////////////////////////////

		String ncl = new String("https://w3id.org/NCL/ontology/");
	    om.setNsPrefix("ncl", ncl);
		String skos = new String("http://www.w3.org/2004/02/skos/core#");
	    om.setNsPrefix("skos", skos); 
	    String rdfs = new String("http://www.w3.org/2000/01/rdf-schema#");
	    om.setNsPrefix("rdfs", rdfs);
		String rdf = new String("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	    om.setNsPrefix("rdf", rdf);

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////

		Ontology ont = om.createOntology(ncl + "ClassificationAttribute");
		om.add(ont, RDFS.label,"Ontology of Natclinn");
		om.add(ont, DC.description,"Abox for the Natclinn Classification Attribute");
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");	

		/////////////////////////////////////
	    // Classes RDF                     //
	    /////////////////////////////////////

        /////////////////////////////////////
	    // Propriétés RDF                  //
	    /////////////////////////////////////

		//////////////////////////////
		// Définition des individus //
		////////////////////////////// 	
		
		// NCL est un ensemble de produits et d'arguments
	    OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCLCA est un ensemble d'atributs de classification
	    OntClass NCLCA = om.createClass(ncl + "NCLCA");
		NCLCA.addComment("NCLCA is the set of classification attributes.", "en");
		NCLCA.addComment("NCLCA est l'ensemble des attributs de classification.", "fr");
	    NCLCA.addSuperClass(NCL);
	

	    

		try (FileInputStream fis = new FileInputStream(NatclinnConf.folderForData + "/Lot 1.1 Attributs Naturalité Conso LEGO NATCLINN 20250708.xlsx");
            Workbook workbook = new XSSFWorkbook(fis)) {
			// Map pour stocker les classes créées
			Map<String, OntClass> classes = new HashMap<>();
			String name = "";
			
			Sheet mainSheet = workbook.getSheet("Feuille 1");
            for (Row row : mainSheet) {
				if (row.getRowNum() == 0) continue;
				String attributsPath = getCellValue(row.getCell(0));
                String description = getCellValue(row.getCell(1));
				if (description.isEmpty()) continue;
				System.out.println("attributsPath: " + attributsPath );
				System.out.println("description: " + description );

				String[] parts = attributsPath.split("\\\\");
    			OntClass parent = null;
    			
				for (int i = 0; i < parts.length; i++) {
					name = parts[i].trim();
					String uri = NatclinnUtil.makeURI(ncl, name);

					OntClass c = classes.get(uri);
					if (c == null) {
						c = om.createClass(uri);
						classes.put(uri, c);
						if (parent != null) {
							c.addSuperClass(parent);
						}
					}
					parent = c;
				}
				
				if (description.equals("Catégorie")) {
					parent.addLabel(name, "fr");
					parent.addSuperClass(NCLCA);
				} else if (description.equals("Sous-catégorie")) {
					parent.addLabel(name, "fr");
				} else {
					parent.addLabel(description, "fr");
				}
			}

			
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + NatclinnConf.folderForData + "/Lot 1.1 Attributs Naturalité Conso LEGO NATCLINN 20250708.xlsx");
			e.printStackTrace();
	
		
		} catch (IOException e) {
            e.printStackTrace();
        }
    	
				
		
		// Exporte le resultat dans un fichier au format JSON-LD
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
		try {
			jsonString = out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    
		System.out.println(jsonString);
		 
		return jsonString;
	}

    private static String getCellValue(Cell cell) {
		if (cell == null) return "";
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				 // Pour éviter la notation scientifique :
            	return new java.math.BigDecimal(cell.getNumericCellValue()).toPlainString();
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				return cell.getCellFormula();
			default:
				return "";
		}
	}

	private static Double getNumericCellValueSafe(Cell cell) {
		if (cell == null) return null;
		switch (cell.getCellType()) {
			case NUMERIC:
				return cell.getNumericCellValue();
			case STRING:
				try {
					return Double.parseDouble(cell.getStringCellValue().trim());
				} catch (NumberFormatException e) {
					return null;
				}
			default:
				return null;
		}
	}

}