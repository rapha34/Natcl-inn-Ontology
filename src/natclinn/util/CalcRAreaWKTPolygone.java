package natclinn.util;

import org.apache.jena.graph.Node;
import org.apache.jena.reasoner.rulesys.BindingEnvironment;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.Util;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

public class CalcRAreaWKTPolygone extends BaseBuiltin {

	@Override
	public String getName() {
		return "calcRAreaWKTPolygone";
	}

	@Override
	public int getArgLength() {
		return 2;
	}

	@Override
	public void headAction(Node[] args, int length, RuleContext context) {
		doUserRequiredAction(args, length, context);
	}  

	@Override
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		return doUserRequiredAction(args, length, context);
	}

	private boolean doUserRequiredAction(Node[] args, int length, RuleContext context) {

		// Check we received the correct number of parameters
		checkArgs(length, context);

		boolean success = false;

		// Retrieve the input arguments
		Node wKTPolygone = getArg(0, args, context);
		// Verify the typing of the parameters
		//System.out.println(wKTPolygone);
		if (wKTPolygone.isLiteral()) {
			Node pValue = null;
			if (wKTPolygone.getLiteralLexicalForm() instanceof String) {
				if (!wKTPolygone.getLiteralLexicalForm().equals(""))  {
					//System.out.println(wKTPolygone.getLiteralValue());
					String strWKTPolygone = (String) wKTPolygone.getLiteralLexicalForm();
					// Doing the calculation
					double nPValue = RUtil.AreaWKTPolygone(strWKTPolygone);
					// Creating a node for the output parameter
					pValue = Util.makeDoubleNode(nPValue);
					//System.out.println(variance);
					// Binding the output parameter to the node
					BindingEnvironment env = context.getEnv();
					success = env.bind(args[1], pValue);
				}
			}
		} 
		return success;
	}   
	
}
