package com.pfg.portal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;

/**
 * Servlet implementation class Create
 */
@WebServlet("/Create")
public class Create extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String clientId = null;	
	private String clientSecret = null;
	private Properties properties = new Properties();
	private static Logger logger = Logger.getLogger(Create.class.getName());
	private boolean DEBUG = false;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Create() {
        super();
		try {
			Handler fileHandler = null;
			if (new File("/opt/IBM/WebSphere/wp_profile/logs/WebSphere_Portal").exists()) {
				fileHandler = new FileHandler("/opt/IBM/WebSphere/wp_profile/logs/WebSphere_Portal/AccountCreate.log");
			} else {
				fileHandler = new FileHandler("./AccountCreate.log");
			}
			logger.addHandler(fileHandler);
			String propertyFile = "/WEB-INF/properties/claim_" + System.getProperty("pfg.environment") + ".properties"; 
			System.out.println("Create will be using property file: " + propertyFile);
			
			InputStream propertyStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyFile);
			properties.load(propertyStream);
			propertyStream.close();

			clientId = properties.getProperty("clientId");
			clientSecret = properties.getProperty("clientSecret");
			
			ISAM.setLogger(logger);
			ISAM.setProperties(properties);
			/*			
  			Enumeration<Object> enuKeys = properties.keys();
			while (enuKeys.hasMoreElements()) {
				String key = (String) enuKeys.nextElement();
				String value = properties.getProperty(key);
			}
			*/		
		} catch (FileNotFoundException e) {
			logger.severe("Exception:" + e.getMessage());
		} catch (IOException e) {
			logger.severe("Exception:" + e.getMessage());
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String type = request.getParameter("type");
		String uid = request.getParameter("uid");
		String portaluid = request.getParameter("portaluid");
		String username = request.getParameter("username");
	    String ldapattr = request.getParameter("ldapattr");
//	    String custaccount = request.getParameter("custaccount");
	    String debugflag = request.getParameter("debug");
	    if (debugflag != null && debugflag.equals("true")) {
	    	DEBUG = true;
	    }
	    ISAM.setDEBUG(DEBUG);

	    if (request.getUserPrincipal() != null) {
	    	portaluid = request.getUserPrincipal().getName();
	    }
	    
	    String CISAttribute = "";
	    String serviceName = "";
	    String groupName = "";
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		if (type.equals("create")) {
	    	
	    	if (ldapattr != null && (ldapattr.equals("wasserstrom") || ldapattr.equals("pfgWasserstrom"))) {
		    	CISAttribute = properties.getProperty("wasserstromCISAttribute");
		    	serviceName = properties.getProperty("wasserstromServiceName"); // have to base 64 encode spaces
		    	groupName = properties.getProperty("wasserstromGroup");
//		    	String CNAttribute = properties.getProperty("customerNumberAttribute");

		    	String accessToken = ISAM.getAccessToken(clientId, clientSecret);
		    	if (accessToken != null) {
		    		if (DEBUG) {
		    			logger.fine("Access Token:" + accessToken);
		    		}
			    	JSONObject entry = ISAM.getEntry(portaluid, accessToken);
			    	
			    	if (entry != null) {
			    	
			    		if (DEBUG) {
			    			logger.fine("uuid: " + entry.get("gtwayUUID").toString());
			    		}
				    	if (entry.get("sn").toString() != null && entry.get("gtwayUUID").toString() != null) {
				    		uid = entry.get("sn").toString().substring(0,2).toUpperCase() + entry.get("gtwayUUID").toString().substring(entry.get("gtwayUUID").toString().length() - 8);
				    	}
				    	if (DEBUG) {
				    		logger.fine("uid: " + uid);
				    	}
				    	boolean updateSuccessful = ISAM.updateCIS(entry.get("gtwayUUID").toString(), accessToken, CISAttribute, uid);
				    	// Comment out customer number update 
				    	// because the new PFG requirement states that the customer number field will be pre-populated 
				    	// and should be read only - 20171016 - SC				    	
				    	// updateSuccessful = updateSuccessful && ISAM.updateCIS(entry.get("gtwayUUID").toString(), accessToken, CNAttribute, custaccount);
				    	updateSuccessful = updateSuccessful && WCM.updateUserAttribute(portaluid, ldapattr, uid);
				    	// updateSuccessful = updateSuccessful && WCM.updateUserAttribute(portaluid, "pfgCustomerNumber", custaccount);
				    	
				    	if (updateSuccessful) {
				    		
				    		ISAM.getServiceNames(accessToken);
				    		
				    		boolean updateService = ISAM.updateService(serviceName, entry.get("gtwayUUID").toString(), accessToken);
				    		updateService = updateService && WCM.addUserToGroup(groupName, portaluid);
				    		
				    		if (updateService) {
				    		
						    	entry = ISAM.getEntry(portaluid, accessToken);
						    	if (DEBUG) {
						    		logger.fine("Account Creation Successful. uuid:" + entry.get("gtwayUUID").toString());
						    	}
						    	out.println("{");
								out.println("    \"code\":200,");
								out.println("    \"message\":\"Account Creation Successful\"");
								out.println("}");
								WCM.reloadUser(portaluid);
				    		} else {
								out.println("{");
								out.println("    \"code\": 400,");
								out.println("    \"message\":\"Unable to update CIS Service Record for " + serviceName + "\",");
								out.println("    \"username\":\"" + username + "\"");
								out.println("}");	    	
				    			
				    		}
				    	} else {
							out.println("{");
							out.println("    \"code\": 400,");
							out.println("    \"message\":\"Unable to update CIS Record\",");
							out.println("    \"username\":\"" + username + "\"");
							out.println("}");	    	
				    		
				    	} 
			    	} else {
						out.println("{");
						out.println("    \"code\": 400,");
						out.println("    \"message\":\"Unable to get CIS UUID\",");
						out.println("    \"username\":\"" + username + "\"");
						out.println("}");	    	
			    	}
		    	} else {
					out.println("{");
					out.println("    \"code\": 400,");
					out.println("    \"message\":\"Unable to get CIS Access token\",");
					out.println("    \"username\":\"" + username + "\"");
					out.println("}");	    	
		    	}
	    		
	    	} else {
				out.println("{");
				out.println("    \"code\": 500,");
				out.println("    \"message\":\"Invalid account for creation\",");
				out.println("    \"account\":\"" + ldapattr + "\"");
				out.println("}");	    	
	    		
	    	}
	    	
	    } else {
			out.println("{");
			out.println("    \"code\": 500,");
			out.println("    \"message\":\"Invalid transaction type\",");
			out.println("    \"type\":\"" + type + "\"");
			out.println("}");	    	
	    	
	    }
		
	}

}