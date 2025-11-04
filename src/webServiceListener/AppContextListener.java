package webServiceListener;

//import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import queriesForWebService.NatclinnQueryInit;

@WebListener
public class AppContextListener implements ServletContextListener {
// Le listener permet de charger un modèle avant le démarage du web service
	
	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {

	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		//ServletContext ctx = servletContextEvent.getServletContext();
		try {
			NatclinnQueryInit.init();
		} catch (Exception e) {
			System.out.println("InfModel not initialized for Web Service !");
			//e.printStackTrace();
		}
		System.out.println("InfModel initialized for Web Service.");
	}

}
