package queriesForWebService;

import org.apache.jena.rdf.model.InfModel;

public final class NatclinnSingletonInfModel {

	private static InfModel infModel = null;

	public static void initModel() throws Exception
	{
		if (infModel == null){
			System.out.println("InfModel under construction");
			NatclinnQueryCreationInfModel.creationModel();
			System.out.println("InfModel builds");
		}
	}
	
	public static InfModel getModel() throws Exception
	{
		if (infModel == null){
			System.out.println("InfModel is not built!");
			NatclinnQueryCreationInfModel.creationModel();
		}
		return infModel;
	}

	public static void setModel(InfModel modelInfered) {
		infModel = modelInfered;
		System.out.println("InfModel is now built!");
	}
}
