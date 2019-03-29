package com.pfg.portal;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.logging.Logger;

import com.ibm.json.java.JSONObject;

public class ISAM {
	
	static private Properties properties = null;
	static boolean DEBUG = false;
	static Logger logger = null;
	
	static void setProperties(Properties props) {
		properties = props;
	}
	
	static void setDEBUG(boolean value) {
		DEBUG = value;
	}
	
	static void setLogger(Logger l) {
		logger = l;
	}

	static String getAccessToken(String clientId, String clientSecret) {
		
		String result = "";
		String data = "client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials";
		
		try {
		    URLConnection connection = new URL("https://" + properties.get("CISAPIHostname") + "/GmaApi/oauth/token").openConnection();
		    connection.setDoOutput(true); // Triggers POST.
		    connection.setRequestProperty("Accept", "*/*");
		    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
		    connection.setRequestProperty("User-Agent", "LinkServices/1.0");
	
		    try (OutputStream output = connection.getOutputStream()) {
		        output.write(data.getBytes());
		    }
		    if (DEBUG) {
		    	logger.fine("wrote: '" + data + "'");
		    	logger.fine("to:" + connection.getURL());
		    }
		    
		    HttpURLConnection httpsConnection = (HttpURLConnection) connection;
		    int status = httpsConnection.getResponseCode();
		    
		    if (status == 200) {
			    InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    if (DEBUG) {
			    	logger.fine(responseString);
			    }
			    JSONObject obj = JSONObject.parse(responseString);
			    result = obj.get("token_type").toString() + " " + obj.get("access_token").toString();
		    } 
		
		} catch (Exception e) {
			logger.severe("Exception:" + e.getMessage());
		}
		
		// logger.fine("getAccessToken result:" + result);
		
		return result;

	}

	static JSONObject getEntry(String portaluid, String accessToken) {
		
		JSONObject result = null;
		
		try {
		    URLConnection connection = new URL("https://" + properties.get("CISAPIHostname") + "/GmaApi/users/" + URLEncoder.encode(portaluid, "UTF-8") + "?gma_allAttrs=true").openConnection();
		    connection.setRequestProperty("Accept", "*/*");
		    connection.setRequestProperty("User-Agent", "LinkServices/1.0");
		    connection.setRequestProperty("Authorization", accessToken);
		    if (DEBUG) {
		    	logger.fine("getting from:" + connection.getURL());
		    }

		    connection.connect();
		    
		    HttpURLConnection httpsConnection = (HttpURLConnection) connection;
		    int status = httpsConnection.getResponseCode();
		    if (DEBUG) {
		    	logger.fine("Result code is:" + status);
		    }
		    
		    if (status == 200) {
			    InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    if (DEBUG) {
			    	logger.fine(responseString);
			    }
			    JSONObject obj = JSONObject.parse(responseString);
			    if (obj.get("status").toString().equals("success")) {
			    	result = (JSONObject) obj.get("entry");
			    }
		    }
			
		} catch (Exception e) {
			logger.fine("getEntry Exception:" + e.getMessage());
		}

		return result;
		
	}
	
	static boolean updateCIS(String uuid, String accessToken, String CISAttribute, String newvalue) {
		
		boolean result = false;
		
		String data = "";
		try {
			data = CISAttribute + "=" + newvalue;	
		} catch (Exception e) {
	    	logger.severe("Setting Parameters Exception:" + e.getMessage());
	    	return result;
	    }
		
		try {
			URLConnection connection = new URL("https://" + properties.get("CISAPIHostname") + "/GmaApi/users/" + uuid).openConnection();
			connection.setDoOutput(true); 
			HttpURLConnection httpsConnection = (HttpURLConnection) connection;
			httpsConnection.setRequestMethod("PUT");
		    connection.setRequestProperty("Accept", "*/*");
		    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
		    connection.setRequestProperty("User-Agent", "LinkServices/1.0");
		    connection.setRequestProperty("Authorization", accessToken);
			
		    try (OutputStream output = connection.getOutputStream()) {
		        output.write(data.getBytes());
		    }
		
		    int status = httpsConnection.getResponseCode();
		    if (DEBUG) {
		    	logger.fine("updateCIS Result code is:" + status);
		    }
		    
		    if (status == 200) {
			    InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    // logger.fine(responseString);
			    JSONObject obj = JSONObject.parse(responseString);
			    if (obj.get("status").toString().equals("success")) {
			    	result = true;
			    }
		    } else {
		    	if (DEBUG) {
		    		logger.severe("Error");
		    		logger.severe("Data sent was: '" + data + "'");
		    	}
		    	InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    if (DEBUG) {
			    	logger.severe(responseString);
			    }
			    InputStream errorResponse = ((HttpURLConnection) connection).getErrorStream();
			    responseString = Utils.convertStreamToString(errorResponse);
			    if (DEBUG) {
			    	logger.severe(responseString);
			    }
		    }
		} catch (Exception e) {
			logger.severe("Exception:" + e.getMessage());
		}
		
		return result;
	}
	
	static void getServiceNames(String accessToken) {
		try {
		    URLConnection connection = new URL("https://" + properties.get("CISAPIHostname") + "/GmaApi/services/names").openConnection();
		    connection.setRequestProperty("Accept", "*/*");
		    connection.setRequestProperty("User-Agent", "LinkServices/1.0");
		    connection.setRequestProperty("Authorization", accessToken);
		    
		    if (DEBUG) {
		    	logger.fine("getting from:" + connection.getURL());
		    }
		    
		    connection.connect();
		    
		    HttpURLConnection httpsConnection = (HttpURLConnection) connection;
		    int status = httpsConnection.getResponseCode();
		    if (DEBUG) {
		    	logger.fine("getServicNames Result code is:" + status);
		    }
		    
		    if (status == 200) {
			    InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    if (DEBUG) {
			    	logger.fine(responseString);
			    }
		    }
			
		} catch (Exception e) {
			logger.severe("Exception:" + e.getMessage());
		}

	}
	
	
	static boolean updateService(String serviceName, String uuid, String accessToken) {
		
		boolean result = false;
		
		String data = "";
		try {
			data = "member" + "=" + uuid;	
		} catch (Exception e) {
	    	logger.severe("Setting Parameters Exception:" + e.getMessage());
	    	return result;
	    }
		
		try {
			
			URLConnection connection = new URL("https://" + properties.get("CISAPIHostname") + "/GmaApi/services/" + serviceName.replaceAll(" ", "%20") + "/members").openConnection();
			connection.setDoOutput(true); 
			HttpURLConnection httpsConnection = (HttpURLConnection) connection;
			httpsConnection.setRequestMethod("PUT");
		    connection.setRequestProperty("Accept", "*/*");
		    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;");
		    connection.setRequestProperty("User-Agent", "LinkServices/1.0");
		    connection.setRequestProperty("Authorization", accessToken);
			
		    if (DEBUG) {
		    	logger.fine("wrote:" + data);
		    	logger.fine("to:" + connection.getURL());
		    }
		    
		    try (OutputStream output = connection.getOutputStream()) {
		        output.write(data.getBytes());
		    }
		
		    int status = httpsConnection.getResponseCode();
		    if (DEBUG) {
		    	logger.fine("updateService Result code is:" + status);
		    }
		    
		    if (status == 200) {
			    InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    if (DEBUG) {
			    	logger.fine(responseString);
			    }
			    JSONObject obj = JSONObject.parse(responseString);
			    if (obj.get("status").toString().equals("success")) {
			    	result = true;
			    }
		    } else {
		    	if (DEBUG) {
		    		logger.severe("Error");
		    		logger.severe("Data sent was: '" + data + "'");
		    	}
		    	InputStream response = connection.getInputStream();
			    String responseString = Utils.convertStreamToString(response);
			    if (DEBUG) {
			    	logger.fine(responseString);
			    }
			    InputStream errorResponse = ((HttpURLConnection) connection).getErrorStream();
			    responseString = Utils.convertStreamToString(errorResponse);
			    if (DEBUG) {
			    	logger.fine(responseString);
			    }
		    }
		} catch (Exception e) {
			logger.severe("Exception:" + e.getMessage());
		}
		
		return result;
	}
}