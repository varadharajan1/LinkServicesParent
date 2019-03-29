package com.pfg.portal;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.portal.um.Group;
import com.ibm.portal.um.PumaController;
import com.ibm.portal.um.PumaHome;
import com.ibm.portal.um.PumaLocator;
import com.ibm.portal.um.PumaProfile;
import com.ibm.portal.um.User;

public class WCM {
	
	static boolean DEBUG = false;
	static Logger logger = null;
	
	static void setDEBUG(boolean value) {
		DEBUG = value;
	}
	
	static void setLogger(Logger l) {
		logger = l;
	}

	static boolean updateUserAttribute(String portaluid, String attributeName, String attributeValue) {

		boolean result = true;

		try {
			Context ctx = null;
			PumaHome pumaHome = null;
			PumaLocator pumaLocator = null;
			PumaController pumaController = null;

			ctx = new InitialContext();
			pumaHome = (PumaHome) ctx.lookup(PumaHome.JNDI_NAME);
			pumaController = pumaHome.getController();
			pumaLocator = pumaHome.getLocator();

			List<User> userList = new ArrayList<User>();

			userList = pumaLocator.findUsersByAttribute("uid", portaluid);
			User user = (User)userList.get(0);

			HashMap<String,String> userAttrs = new HashMap<String, String>(); 
			userAttrs.put(attributeName, attributeValue);
			pumaController.setAttributes(user, userAttrs);

			result = true;
		} catch (Exception e) {
			logger.severe("WCM.UpdateUserAttributes Exception:" + e.getMessage());
		}

		return result;

	}

	static boolean addUserToGroup(String groupName, String portaluid) {
		boolean result = false;

		// this allows the function to be run without access checks
		PrivilegedExceptionAction<Boolean> action = new PrivilegedExceptionAction<Boolean>() {

			@Override
			public Boolean run() {

				boolean result = false;
				try {
					Context ctx = null;
					PumaHome pumaHome = null;
					PumaLocator pumaLocator = null;
					PumaController pumaController = null;

					ctx = new InitialContext();
					pumaHome = (PumaHome) ctx.lookup(PumaHome.JNDI_NAME);
					pumaController = pumaHome.getController();
					pumaLocator = pumaHome.getLocator();

					List<Group> groupList = pumaLocator.findGroupsByAttribute("cn", groupName); 
					if (groupList != null) {
						Group group = (Group)groupList.get(0); 
						List<User> userList = new ArrayList<User>();

						userList = pumaLocator.findUsersByAttribute("uid", portaluid);
						if (userList != null) {
							pumaController.addToGroup(group, userList);
							result = true;
						} else {
							if (DEBUG) {
						    	logger.severe("Unable to add " + portaluid + " to " + groupName + " because user was not found");
						    }
						}
					} else {
						if (DEBUG) {
							logger.severe("Unable to add " + portaluid + " to " + groupName + " because group was not found");
						}
					}

				} catch (Exception e) {
					logger.severe("WCM.addUserToGroup Exception:" + e.getMessage());
				}

				return new Boolean(result);

			}
		};

		try {
			Context ctx = null;
			PumaHome pumaHome = null;

			ctx = new InitialContext();
			pumaHome = (PumaHome) ctx.lookup(PumaHome.JNDI_NAME);
			result = pumaHome.getEnvironment().runUnrestricted(action).booleanValue();
		} catch (PrivilegedActionException e) {
			logger.severe("WCM PrivilegedActionException:" + e.getMessage());
//			e.printStackTrace();
		} catch (NamingException e) {
			logger.severe("WCM NamingException:" + e.getMessage());
//			e.printStackTrace();
		}

		return result;
	}


	static boolean reloadUser(String portaluid) {

		boolean result = false;

		try {
			Context ctx = null;
			PumaHome pumaHome = null;
			PumaProfile pumaProfile = null;
			PumaLocator pumaLocator = null;
			PumaController pumaController = null;

			ctx = new InitialContext();
			pumaHome = (PumaHome) ctx.lookup(PumaHome.JNDI_NAME);
			pumaProfile = pumaHome.getProfile();
			pumaController = pumaHome.getController();
			pumaLocator = pumaHome.getLocator();

			List<String> myList = new ArrayList<String>();
			myList.add("uid");
			myList.add("sn");

			List<User> userList = new ArrayList<User>();

			userList = pumaLocator.findUsersByAttribute("uid", portaluid);

			if (userList.size() == 0) {
				if (DEBUG) {
					logger.fine("No results returned from search for " + portaluid + "!  Nothing to reload");
				}
			} else {
				// System.out.println("Found a number of users in LDAP  " + userList.size());

				for (int i = 0; i < userList.size(); i++) {
					if (DEBUG) {
						logger.fine("About to Reload User Attributes for user number "+ (i + 1)	+ "   " + pumaProfile.getAttributes((User) userList.get(i), myList));
					}
//					System.out.println("About to Reload User Attributes for user number "+ (i + 1)	+ "   " + pumaProfile.getAttributes((User) userList.get(i), myList));
					pumaController.reload((User) userList.get(i));
					if (DEBUG) {
						logger.fine("Done Reloading.  Updated User Attributes for user  "+ pumaProfile.getAttributes((User) userList.get(i), myList));
					}
//					System.out.println("Done Reloading.  Updated User Attributes for user  "+ pumaProfile.getAttributes((User) userList.get(i), myList));
				}
				result = true;
			}
		} catch (Exception e) {
			logger.severe("WCM.reloadUser Exception:" + e.getMessage());
		}

		return result;
	}
	
}
