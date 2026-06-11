/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.WebConstants;
import org.openmrs.web.user.UserProperties;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controls the forgotten password form Initially a form with just a username box is shown Then a
 * box for the answer to the secret question is shown
 */
public class ForgotPasswordFormController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected static final Log log = LogFactory.getLog(ForgotPasswordFormController.class);
	
	/**
	 * Not used with the forgot password form controller.
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
		return "";
	}
	
	/**
	 * The mapping from user's IP address to the number of attempts at logging in from that IP
	 */
	private Map<String, Integer> loginAttemptsByIP = new HashMap<String, Integer>();
	
	/**
	 * The mapping from user's IP address to the time that they were locked out
	 */
	private Map<String, Date> lockoutDateByIP = new HashMap<String, Date>();
	
	/**
	 * This takes in the form twice. The first time when the input their username and the second
	 * when they submit both their username and their secret answer
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj,
	        BindException errors) throws Exception {
		
		HttpSession httpSession = request.getSession();
		
		String username = request.getParameter("uname");
		
		String ipAddress = request.getRemoteAddr();
		Integer forgotPasswordAttempts = loginAttemptsByIP.get(ipAddress);
		if (forgotPasswordAttempts == null) {
			forgotPasswordAttempts = 1;
		}
		
		boolean lockedOut = false;
		
		if (forgotPasswordAttempts > 5) {
			lockedOut = true;
			
			Date lockedOutTime = lockoutDateByIP.get(ipAddress);
			if (lockedOutTime != null && System.currentTimeMillis() - lockedOutTime.getTime() > 300000) {
				lockedOut = false;
				forgotPasswordAttempts = 0;
				lockoutDateByIP.put(ipAddress, null);
			} else {
				// they haven't been locked out before, or they're trying again
				// within the time limit.  Set the locked-out date to right now
				lockoutDateByIP.put(ipAddress, new Date());
			}
			
		}
		
		if (lockedOut) {
			httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "auth.forgotPassword.tooManyAttempts");
		} else {
			// if the previous logic didn't determine that the user should be locked out,
			// then continue with the check
			
			forgotPasswordAttempts++;
			
			String secretAnswer = request.getParameter("secretAnswer");
			if (secretAnswer == null) {
				// if they are seeing this page for the first time
				
				User user = null;
				
				try {
					Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
					
					// only search if they actually put in a username
					if (username != null && username.length() > 0) {
						user = Context.getUserService().getUserByUsername(username);
					}
				}
				finally {
					Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
				}
				
				if (user == null) {
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "auth.question.fill");
					request.setAttribute("secretQuestion", getRandomFakeSecretQuestion(username));
				} else {
					String secretQuestion = Context.getUserService().getSecretQuestion(user);
					if (secretQuestion == null || secretQuestion.equals("")) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "auth.question.empty");
					} else {
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "auth.question.fill");
						request.setAttribute("secretQuestion", secretQuestion);
						
						// reset the forgotPasswordAttempts because they have a right user.
						// they will now have 5 more chances to get the question right
						forgotPasswordAttempts = 0;
					}
				}
			} else {
				// if they've filled in the username and entered their secret answer
				
				User user = null;
				
				try {
					Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
					user = Context.getUserService().getUserByUsername(username);
				}
				finally {
					Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
				}
				
				// check the secret question again in case the user got here "illegally"
				if (user == null) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "auth.answer.invalid");
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "auth.question.fill");
					request.setAttribute("secretQuestion", getRandomFakeSecretQuestion(username));
				} else {
					String secretQuestion = Context.getUserService().getSecretQuestion(user);
					if (secretQuestion == null || secretQuestion.equals("")) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "auth.question.empty");
					} else if (secretQuestion != null && Context.getUserService().isSecretAnswer(user, secretAnswer)) {
						
						String randomPassword = getRandomPassword();
						
						// Reset the password AND flag a forced change on the next login, under one
						// privilege block so saveUser can persist the changed password. Mirrors the
						// EDIT_USERS / GET_USERS / EDIT_USER_PASSWORDS proxy set in ChangePasswordFormController.
						// The forced-change flag stops the generated temporary password being permanently
						// valid (gap 8.5-6): ForcePasswordChangeFilter redirects a user carrying this
						// property to the change-password form on the next request.
						try {
							Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
							Context.addProxyPrivilege(PrivilegeConstants.EDIT_USERS);
							Context.addProxyPrivilege(PrivilegeConstants.EDIT_USER_PASSWORDS);
							// saveUser validates the user and its person/address. That validation reads the
							// address template (LocationService.getAddressTemplate -> "Get Locations", which
							// reads a global property), so the anonymous reset user needs these read grants too.
							Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
							Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
							Context.getUserService().changePassword(user, randomPassword);
							new UserProperties(user.getUserProperties()).setSupposedToChangePassword(true);
							Context.getUserService().saveUser(user);
						}
						finally {
							Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
							Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
							Context.removeProxyPrivilege(PrivilegeConstants.EDIT_USER_PASSWORDS);
							Context.removeProxyPrivilege(PrivilegeConstants.EDIT_USERS);
							Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
						}
						
						// Audit-logging (NEN 8.15): record THAT a forgot-password reset succeeded and for whom -
						// never the generated password, never the secret answer.
						log.warn("AUDIT PASSWORD_RESET user=" + username + " outcome=SUCCESS");
						
						httpSession.setAttribute("resetPassword", randomPassword);
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "auth.password.reset");
						Context.authenticate(username, randomPassword);
						// Reload the just-authenticated user so the session context reflects the
						// forced-change flag set above; ForcePasswordChangeFilter then redirects this
						// session to the change-password form on the next request (gap 8.5-6). The
						// reset user is unprivileged, so the reload needs a GET_USERS proxy.
						try {
							Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);
							Context.refreshAuthenticatedUser();
						}
						finally {
							Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
						}
						httpSession.setAttribute("loginAttempts", 0);
						
						return new ModelAndView(new RedirectView(request.getContextPath()
						        + "/options.form#Change Login Info"));
					} else {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "auth.answer.invalid");
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "auth.question.fill");
						request.setAttribute("secretQuestion", secretQuestion);
					}
				}
			}
		}
		
		loginAttemptsByIP.put(ipAddress, forgotPasswordAttempts);
		
		request.setAttribute("uname", username);
		
		return showForm(request, response, errors);
	}
	
	public String getRandomPassword() {
		//Password should be satisfy the minimum length if any is set, must have 1 upper case letter and 1 number
		Integer minLength = 8;
		// The anonymous reset user holds no privileges, so this global-property lookup needs a proxy
		// privilege - the same pattern as the GET_USERS / EDIT_USER_PASSWORDS proxy blocks in onSubmit
		// (gap 8.5-6). Without it the reset flow threw APIAuthenticationException "Privileges required:
		// Get Global Properties" before changePassword was ever reached.
		String str;
		try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			str = Context.getAdministrationService().getGlobalProperty(OpenmrsConstants.GP_PASSWORD_MINIMUM_LENGTH);
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		}
		if (StringUtils.isNotBlank(str)) {
			minLength = Integer.valueOf(str);
		}
		return RandomStringUtils.randomAlphabetic(1).toUpperCase() + RandomStringUtils.randomAlphanumeric(minLength)
		        + RandomStringUtils.randomNumeric(1);
	}
	
	public String getRandomFakeSecretQuestion(String username) {
		
		List<String> questions = new ArrayList<>();
		
		questions.add(Context.getMessageSourceService().getMessage("What is your best friend's name?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your grandfather's home town?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your mother's maiden name?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your favorite band?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your first pet's name?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your brother's middle name?"));
		questions.add(Context.getMessageSourceService().getMessage("Which city were you born in?"));
		
		int hashValueForName = username.hashCode();
		
		//Converting this value to something between 0 and 6
		if (hashValueForName < 0) {
			hashValueForName *= -1;
		}
		hashValueForName %= 7;
		
		//Return random question
		return questions.get(hashValueForName);
	}
}
