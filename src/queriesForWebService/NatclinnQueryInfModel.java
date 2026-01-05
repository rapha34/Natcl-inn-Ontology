package queriesForWebService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;

import natclinn.util.NatclinnConf;


public class NatclinnQueryInfModel {

	public static void main(String[] args) throws Exception {
		Instant start = Instant.now();
		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf();
		// String ncl = NatclinnConf.ncl;
		
		////////////////////////////////
		// Recupèration du model      //
		////////////////////////////////
		InfModel infModel = NatclinnSingletonInfModel.getModel();
		showModelSize( infModel );
		
		List<Resource> listProductIAA = new ArrayList<Resource>() ;
		listProductIAA = listProductIAA(infModel);
		System.out.println(listProductIAA);
		
		// Resource caf_13 = infModel.getResource( afv + "caf_13" );
		// listTerms.clear();
		// listTerms.add(caf_13);
		
		// List<Resource> listElements = new ArrayList<Resource>() ;
		// listElements = listElementClarifyByTerms(infModel, listTerms);
		// System.out.println(listElements);
		// System.out.println(listElements.size());
		
		Instant end2 = Instant.now();
		System.out.println("Running time : " + Duration.between(start, end2).getSeconds() + " seconds");
	}


	/**
	 * Liste des éléments clarifiés par un ou plusieurs termes
	 * @param m : le modele
	 * @param listTerm : Liste de termes
	 * @return listElements : Liste des éléments clarifiés par la liste de termes
	 * @throws Exception 
	 */
	public static List<Resource> listElementClarifyByTerms(InfModel m,List<Resource> listTerms) throws Exception {
		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf();
		String ncl = NatclinnConf.ncl;

		List<Resource> listElements = new ArrayList<Resource>();
		Resource stucturalElementClass = m.getResource( ncl + "StructuralElement" );
		// On retrouve les instances de StructuralElement 
		Iterator<Resource> stucturalElements = m.listSubjectsWithProperty(RDF.type, stucturalElementClass);
		while (stucturalElements.hasNext()) {
			Resource structuralElement = stucturalElements.next();
			StmtIterator iElType = m.listStatements(structuralElement, DC.type,(RDFNode) null);
			while (iElType.hasNext()) {	
				RDFNode TermOfElement = iElType.next().getObject();
				if (listTerms.contains(TermOfElement)) {
					listElements.add((Resource) structuralElement);
				}
			}
		
		}
		return listElements;
	}	
	
	public static List<Resource> listProductIAA(InfModel m) {
		List<Resource> listProduct = new ArrayList<Resource>() ;
		new NatclinnConf();
		String ncl = NatclinnConf.ncl;
		Property isProductIAA = m.getProperty( ncl + "isProductIAA" );
		RDFNode trueBoolean = m.createTypedLiteral(true);
		StmtIterator i = m.listStatements( null, isProductIAA, trueBoolean);
		while (i.hasNext()) {
			Resource product = i.next().getSubject();
			listProduct.add(product);
		}
		return listProduct;
	}
	
	
	/**
     * Get the value of a property as a string, allowing for missing properties
     * @param r A resource
     * @param p The property whose value is wanted
     * @return The value of the <code>p</code> property of <code>r</code> as a string
     */
	public static String getValueAsString( Resource r, Property p ) {
		Statement s = r.getProperty( p );
        if (s == null) {
            return "";
        }
        else {
            return s.getObject().isResource() ? s.getResource().getURI() : s.getString();
        }
    }
	
	protected static void showModelSize( InfModel m ) {
        System.out.println( String.format( "The model contains %d triples", m.size() ) );
    }
    
	
}