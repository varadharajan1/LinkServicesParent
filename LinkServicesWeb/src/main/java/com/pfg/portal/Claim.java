package com.pfg.portal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
 * Servlet implementation class AccountClaim
 */
@WebServlet(urlPatterns = { "/Claim" })

public class Claim extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String clientId = null;	
	private String clientSecret = null;
	private Properties properties = new Properties();
	private static Logger logger = Logger.getLogger(Claim.class.getName());
	private boolean DEBUG = false;
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Claim() {
		super();
		try {
			Handler fileHandler = null;
			if (new File("/opt/IBM/WebSphere/wp_profile/logs/WebSphere_Portal").exists()) {
				fileHandler = new FileHandler("/opt/IBM/WebSphere/wp_profile/logs/WebSphere_Portal/AccountClaim.log");
			} else if (new File("/opt/IBM/WebSphere/wp_profile/logs/WebSphere_Portal_Additional").exists()) {
				fileHandler = new FileHandler("/opt/IBM/WebSphere/wp_profile/logs/WebSphere_Portal_Additional/AccountClaim.log");
			} else {
				fileHandler = new FileHandler("./AccountClaim.log");
			}
			logger.addHandler(fileHandler);
			
			String propertyFile = "/WEB-INF/properties/claim_" + System.getProperty("pfg.environment") + ".properties"; 
			System.out.println("Claim logs will be using property file: " + propertyFile);
			
			InputStream propertyStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyFile);
			properties.load(propertyStream);
			propertyStream.close();

			clientId = properties.getProperty("clientId");
			clientSecret = properties.getProperty("clientSecret");

			ISAM.setLogger(logger);
			ISAM.setProperties(properties);
			
			WCM.setLogger(logger);

		} catch (FileNotFoundException e) {
			logger.severe("Exception:" + e.getMessage());
		} catch (IOException e) {
			logger.severe("Exception:" + e.getMessage());
		}
		
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Set response content type
		response.setContentType("text/html");

		// Actual logic goes here.
		PrintWriter out = response.getWriter();
		out.println("<h1>This is the AccountClaim Servlet</h1>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String type = request.getParameter("type");
		String portaluid = request.getParameter("portaluid");
		String username = request.getParameter("username");
	    String password = request.getParameter("password");
	    
	    String ldapattr = request.getParameter("ldapattr");
//	    String junction = request.getParameter("junction");
	    
	    //Phase 1C - Removal of OpCos
	    //String opco = request.getParameter("opco");
	    //String service = request.getParameter("service");
//	    String comp = request.getParameter("comp");
	    
	    String debugflag = request.getParameter("debug");    
	    
	    if (debugflag != null && debugflag.equals("true")) {
	    	DEBUG = true;
	    }
	    ISAM.setDEBUG(DEBUG);
	    WCM.setDEBUG(DEBUG);
	    
	    if (request.getUserPrincipal() != null) {
	    	portaluid = request.getUserPrincipal().getName();
	    }
	    
	    // The following three variables are for PPay validation only.
	    String profileid="";
	    String pid_result = "";
	    String res= "";
	    
	    boolean authSuccessful = false;
	    String CISAttribute = "";
	    String serviceName = "";
	    String groupName = "";
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
	    
	    if (type.equals("claim")) {
	    
		    switch(ldapattr) {
		    
		    	case "pfgWasserstrom":
			    case "wasserstrom":
			    	CISAttribute = properties.getProperty("wasserstromCISAttribute");
			    	serviceName = properties.getProperty("wasserstromServiceName"); // have to base 64 encode spaces
			    	groupName = properties.getProperty("wasserstromGroup");
			    	authSuccessful = validateWasserstrom(username, password);
			    	
			    	break;
			    case "pfgPPay":
			    case "ppay":
			    	CISAttribute = properties.getProperty("ppayCISAttribute");
			    	serviceName = properties.getProperty("ppayServiceName");
			    	groupName = properties.getProperty("ppayGroup");
			    	pid_result = validatePPay(username, password);
			    	res=pid_result.substring(pid_result.indexOf("_")+1, pid_result.length());
			    	profileid=pid_result.substring(0, pid_result.indexOf("_"));
					authSuccessful=Boolean.parseBoolean(res);
			    	break;
			    case "pfgPNet":
			    case "pnet":
			    	CISAttribute = properties.getProperty("pnetCISAttribute");
			    	groupName = properties.getProperty("pnetGroup");
			    	serviceName = properties.getProperty("pnetServiceName");;
			    	authSuccessful = validatePNet(username, password);
			    	break;
			    default:
			    	logger.severe("Bad ldapattr:" + ldapattr);
		    }
		    
		    
		    if (authSuccessful) {
		    	
		    	String accessToken = ISAM.getAccessToken(clientId, clientSecret);
		    	if (accessToken != null && !accessToken.equals("")) {
			    	
			    	JSONObject entry = ISAM.getEntry(portaluid, accessToken);
			    	
			    	if (entry != null) {
			    	
			    		if (DEBUG) {
			    			logger.severe("uuid:" + entry.get("gtwayUUID").toString());
			    		}
				    	String newValue = "";
				    	try {
				    		newValue = URLEncoder.encode(username, "UTF-8");
				    		if (!profileid.equals("")){
				    			// For PPay, the system will update the LDAP attribute with profile ID return from the API call, 
				    			// instead of user name - SC - 20171111
				    			newValue =  profileid;
				    		}

				    	} catch (Exception e) {
				    		logger.severe("Exception:" + e.getMessage());
				    	}
				    	
				    	boolean updateSuccessful = ISAM.updateCIS(entry.get("gtwayUUID").toString(), accessToken, CISAttribute, newValue);
				    	updateSuccessful = updateSuccessful && WCM.updateUserAttribute(portaluid, ldapattr, newValue);
				    	
				    	if (updateSuccessful) {
				    		
				    		ISAM.getServiceNames(accessToken);
				    		
				    		boolean updateService = ISAM.updateService(serviceName, entry.get("gtwayUUID").toString(), accessToken);
				    		updateService = updateService && WCM.addUserToGroup(groupName, portaluid);
				    		if (updateService) {
				    		
						    	if (DEBUG) {
						    		entry = ISAM.getEntry(portaluid, accessToken);
						    		logger.fine("uuid:" + entry.get("gtwayUUID").toString());
						    	}							
								out.println("{");
								out.println("    \"code\":200,");
								out.println("    \"message\":\"Account Claim Successful\"");
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
		    	
		    }
		    else if(username=="" || username.trim().length()==0){
				out.println("{");
				out.println("    \"code\": 403,");
				out.println("    \"message\":\"Userid not provided\",");
				out.println("    \"username\":\"" + username + "\"");
				out.println("}");	    	
		    }
		    else if(ldapattr.equalsIgnoreCase("pfgPNet")){
				out.println("{");
				out.println("    \"code\": 403,");
				out.println("    \"message\":\"Location not selected\",");
				out.println("    \"username\":\"" + username + "\"");
				out.println("}");	    	
		    }
		    else {
				out.println("{");
				out.println("    \"code\": 403,");
				out.println("    \"message\":\"Invalid userid or password\",");
				out.println("    \"username\":\"" + username + "\"");
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
	
	
	protected boolean validateWasserstrom(String username, String password) {

		boolean result = false;
	    String form = "";
	    String storeId="";
	    String catalogId="";
	    String reLogonURL="";
	    
	    storeId = properties.getProperty("wasserstromStoreId");
	    catalogId = properties.getProperty("wasserstromCatalogId");
	    reLogonURL = properties.getProperty("wasserstromReLogonURL");
	    
	    try {
		    form += "?storeId="+storeId;
		    form += "&catalogId="+catalogId;
		    form += "&reLogonURL="+reLogonURL;
		    form += "&myAcctMain=";
		    form += "&fromOrderId=";
		    form += "&toOrderId=.";
		    form += "&deleteIfEmpty=*";
		    form += "&continue=1";
		    form += "&createIfEmpty=1";
		    form += "&calculationUsageId=-1";
		    form += "&updatePrices=0";
		    form += "&logonId=" + URLEncoder.encode(username, "UTF-8");
		    form += "&logonPassword=" + URLEncoder.encode(password, "UTF-8");
		    form += "&rememberMe=false";
		    form += "&requesttype=ajax";
		    form += "&URL=RESTOrderCalculate?URL="+reLogonURL+"?calculationUsageId=-1&calculationUsageId=-2&deleteCartCookie=true&page=&catalogId="+catalogId+"&storeId="+storeId+"&globalLogIn=true";
	    } catch (Exception e) {
	    	logger.severe("In validateWasserstrom, setting Parameters Exception:" + e.getMessage());
	    	return result;
	    }

	    try {
		    URLConnection connection = new URL(properties.getProperty("wasserstromLoginURL")).openConnection();
		    connection.setDoOutput(true); // Triggers POST.
		    connection.setRequestProperty("Accept", "*/*");
		    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
	
		    try (OutputStream output = connection.getOutputStream()) {
		        output.write(form.getBytes());
		    }
	
		    HttpURLConnection httpsConnection = (HttpURLConnection) connection;
		    int status = httpsConnection.getResponseCode();
		    
		    if (status == 200) {
			    InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    responseString = responseString.replace("/*", "");
			    responseString = responseString.replace("*/", "");
			    JSONObject obj = JSONObject.parse(responseString);
			    
			    if (obj != null && obj.get("ibm.wc.credentialsStatus") != null && obj.get("ibm.wc.credentialsStatus").toString().equals("FULLCRED")) {
			    	result = true;
			    } else {
			    	if (DEBUG) {
			    		logger.severe("Debug inside validateWasserstrom, the responseString is" + responseString);
			    	}
			    }
		    }
		    
	    } catch (Exception e) {
	    	logger.severe("validateWasserstrom Exception:" + e.getMessage());
	    	e.printStackTrace();
	    }
	    
	    return result;
	}
	
	private String validatePPay(String username, String password) {
		
		if (DEBUG) {
			logger.fine("validatePPay starts.");
		}
		boolean result = false;
		String form = "";
		String ppayresult = "";
		Long profileidnum = 0L;
		try {
		    form += "?un=" + URLEncoder.encode(username, "UTF-8");
		    form += "&pw=" + URLEncoder.encode(password, "UTF-8");

		} catch (Exception e) {
	    	logger.severe("In validatePPay, setting Parameters Exception: " + e.getMessage());
	    	return "0_false";
	    }
		
	    try {
	    	
		    URLConnection connection = new URL(properties.getProperty("ppayLoginURL")+form).openConnection();
		   
		    connection.setRequestProperty("Accept", "*/*");
		    
		    HttpURLConnection httpsConnection = (HttpURLConnection) connection;
		    int status = httpsConnection.getResponseCode();
		    
		    InputStream response = connection.getInputStream();
		    String responseString = Utils.convertStreamToString(response); 
		    JSONObject jobj = JSONObject.parse(responseString); 
		    
		    if(jobj.containsValue("USER_VALID")){
		    	profileidnum = (Long)jobj.get("ProfileId");
		    	result = true;
		    	ppayresult = profileidnum + "_" + result;
		    }
		    else {
		    	ppayresult = profileidnum + "_" + result;
		    	logger.severe("ErrorMessage from API call in validatePPay "+ jobj.get("ErrorMessage"));
		    	String err = (String)jobj.get("ErrorMessage");
		    	throw new Exception(err);
		    }
		    
		    if (DEBUG) {
		    	logger.severe("validatePPay responseString = " + responseString); 
		    	logger.severe("Post Data status code in validatePPay is:" + status + " and profileid = " + profileidnum);
		    }
		    
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	logger.severe("validatePPay Exception: "+ e.getMessage());
	    }
	    
	    return ppayresult;
	}

	private boolean validatePNet(String username, String password) {
		boolean result = false;
		JSONObject jobj=null;
	    try {
	    	String url = "";
	    	String pnetAuth = "";

	    	if(username=="" || username.trim().length()==0) {
		    	result = false;
		    	throw new Exception("validatePNet failed: username missing.");
		    }
	    	
	    	/*
	    	 *The post Restful service has different Authorization string for dev, test and prod server, it accepts a json string and returns status code 200 as success
	    	 * In case of incorrect base Auth string or incorrect userid/password it returns 403 status code
	    	 * the response String is {"code":200,"message":"Valid"} in case the user is validated
	    	 */
	    	
	    	url = properties.getProperty("pnetLoginURL");
	    	
	    	pnetAuth = properties.getProperty("pnetAuthorization");
	    	
	    	String authString = "{\"username\":\""+username+"\",\"password\":\""+password+"\"}";
	    	
	    	if (DEBUG) {
	    		logger.fine("In validatePNet, calling URL: " + url);
	    		logger.fine("pnetAuth String =" + pnetAuth);
	    	}
	        
		    URLConnection connection = new URL(url).openConnection();
		    
		    connection.setRequestProperty("Accept", "application/json");
		    connection.setRequestProperty("Content-Type", "application/json");
		    
		    connection.setRequestProperty("Authorization", pnetAuth);
		    connection.setDoOutput(true); // Triggers POST.
		    
		    OutputStream os = connection.getOutputStream();
			os.write(authString.getBytes());
			os.flush();
			
			HttpURLConnection httpsConnection = (HttpURLConnection) connection;
		    int status = httpsConnection.getResponseCode();
		    
		    InputStream _is;
		    if (httpsConnection.getResponseCode() == 200) {
		        _is = httpsConnection.getInputStream();
		    } else {
		         /* error from server */
		        _is = httpsConnection.getErrorStream();
		    }
		    
			 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        byte[] buffer = new byte[1024];
		        int length = 0;
		        while ((length = _is.read(buffer)) != -1) {
		            baos.write(buffer, 0, length);
		        }	
		        
		        jobj = JSONObject.parse(baos.toString());
		       
		    if (status == 200) {
		    	result = true;
		    	logger.fine("In validatePNet, status code : "+status +"  and message : " + jobj.get("message"));
		    }
		    else {
		    	result = false;
//		    	logger.severe("validatePNet IO Exception : "+ jobj.get("message"));
		    	throw new Exception("In validatePNet, code : " + jobj.get("code") + " , User : " +username+ " error : " + jobj.get("message"));
		    }
		    
	    } catch
	    (IOException io){
	    	logger.severe("validatePNet IO Exception : "+ io.getMessage());
	    }
	    catch
	    (Exception e) {
	    	logger.severe("validatePNet Exception : "+ e.getMessage());
	    }
	    
	    return result;
	}	
	
	String updatePNetValue(JSONObject entry, String username, String opco, String CISAttribute) {
		
		String newvalue = "";
		
		if (opco != null && !opco.equals("")) {
			String oldvalue = null;
			if (entry.get(CISAttribute) != null) {
				oldvalue = entry.get(CISAttribute).toString();
			}
			if (DEBUG) {
				logger.fine("opco:'" + opco + "' oldvalue:'" + oldvalue + "'");
		 	}
			
			/*
			|idstest220_220|idstest210_210|  is the existing format of oldvalue
			*/
			
			if (oldvalue != null && oldvalue.contains("_" + opco)) {
			
			
				String[] oldvalueArray = oldvalue.split("\\|");// can have idstest210_210,  idstest220_220
				
				for (int i = 0 ; i < oldvalueArray.length; i++) {
					if (oldvalueArray[i].contains( "_" + opco)) {
						oldvalueArray[i] = username + "_" + opco;
						break;
					}
				}
				
				newvalue = "|";
				for (int i = 0; i < oldvalueArray.length; i++) {
					if (!oldvalueArray[i].equals("")) {
						newvalue += oldvalueArray[i] + "|";
					}
				}
				
			} else {	
				if (oldvalue == null || oldvalue.equals("")) {
					newvalue = "|" + username + "_" + opco + "|";
				} else {
					newvalue = oldvalue + username + "_" + opco + "|";
				}
			}
			if (DEBUG) {
				logger.fine("newvalue='" + newvalue + "'");
			}
		}
		
		return newvalue;
	}
	
}