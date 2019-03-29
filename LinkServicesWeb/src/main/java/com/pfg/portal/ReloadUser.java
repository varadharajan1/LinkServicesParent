package com.pfg.portal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ReloadUser
 */
@WebServlet("/ReloadUser")
public class ReloadUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
//	private Properties properties = new Properties();
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ReloadUser() {
		super();
/*		
		try {
			String propertyFile = "/WEB-INF/properties/claim_" + System.getProperty("pfg.environment") + ".properties"; 
			System.out.println("Claim using property file: " + propertyFile);
			
			InputStream propertyStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyFile);
			properties.load(propertyStream);
			propertyStream.close();
		} catch (Exception e) {
			System.out.println("Unable to load configuration properties:" + e.getMessage());
			e.printStackTrace();
		}
*/
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String portaluid = null; // request.getParameter("portaluid");

		if (request.getUserPrincipal() != null) {
			portaluid = request.getUserPrincipal().getName();
		}
	
		boolean success = WCM.reloadUser(portaluid);
		
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		if (success) {
			out.println("{");
			out.println("    \"code\": 200,");
			out.println("    \"message\":\"Found and reloaded " + portaluid +"\",");
			out.println("    \"portaluid\":\"" + portaluid + "\"");
			out.println("}");	    	
			
		} else {
			out.println("{");
			out.println("    \"code\": 404,");
			out.println("    \"message\":\"No results returned from search!  Nothing to reload\",");
			out.println("    \"portaluid\":\"" + portaluid + "\"");
			out.println("}");	    	

		}

	}

}
