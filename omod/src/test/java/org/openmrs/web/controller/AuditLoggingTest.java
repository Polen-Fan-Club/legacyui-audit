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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.web.controller.user.UserFormController;
import org.openmrs.web.servlet.LoginServlet;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * NEN-7510 §8.15 audit-logging tests: verifies that security events produce WARN-level AUDIT log
 * entries with SCREAMING_SNAKE_CASE event names and outcome markers, and that no sensitive values
 * (passwords) appear in the log output.
 */
public class AuditLoggingTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	private UserFormController userFormController;

	private StringWriter loginCapture;

	private WriterAppender loginAppender;

	private StringWriter userCapture;

	private WriterAppender userAppender;

	@Before
	public void attachLogAppenders() {
		loginCapture = new StringWriter();
		loginAppender = new WriterAppender(new SimpleLayout(), loginCapture);
		Logger loginLogger = Logger.getLogger(LoginServlet.class);
		loginLogger.setLevel(Level.WARN);
		loginLogger.addAppender(loginAppender);

		userCapture = new StringWriter();
		userAppender = new WriterAppender(new SimpleLayout(), userCapture);
		Logger userLogger = Logger.getLogger(UserFormController.class);
		userLogger.setLevel(Level.WARN);
		userLogger.addAppender(userAppender);
	}

	@After
	public void detachLogAppenders() {
		Logger.getLogger(LoginServlet.class).removeAppender(loginAppender);
		Logger.getLogger(UserFormController.class).removeAppender(userAppender);
	}

	@Test
	public void loginSuccess_logsAuditEvent() throws Exception {
		Context.logout();

		LoginServlet servlet = new LoginServlet();
		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/loginServlet");
		req.setContextPath("/openmrs");
		req.setParameter("uname", "admin");
		req.setParameter("pw", "test");
		servlet.service(req, new MockHttpServletResponse());

		String captured = loginCapture.toString();
		assertTrue("Audit log must contain LOGIN_SUCCESS", captured.contains("LOGIN_SUCCESS"));
		assertTrue("Audit log must contain outcome=SUCCESS", captured.contains("outcome=SUCCESS"));
	}

	@Test
	public void loginFailure_logsAuditEvent() throws Exception {
		LoginServlet servlet = new LoginServlet();
		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/loginServlet");
		req.setContextPath("/openmrs");
		req.setParameter("uname", "admin");
		req.setParameter("pw", "WrongPassword!");
		servlet.service(req, new MockHttpServletResponse());

		String captured = loginCapture.toString();
		assertTrue("Audit log must contain LOGIN_FAILED", captured.contains("LOGIN_FAILED"));
		assertTrue("Audit log must contain outcome=FAILURE", captured.contains("outcome=FAILURE"));
	}

	@Test
	public void auditLog_doesNotContainPassword() throws Exception {
		LoginServlet servlet = new LoginServlet();
		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/loginServlet");
		req.setContextPath("/openmrs");
		req.setParameter("uname", "admin");
		req.setParameter("pw", "TestWachtwoord123!");
		servlet.service(req, new MockHttpServletResponse());

		String captured = loginCapture.toString();
		assertFalse("Audit log must not contain the plaintext password value (NEN-7510 §8.15)",
		    captured.contains("TestWachtwoord123!"));
	}

	@Test
	public void createUser_logsAuditEvent() throws Exception {
		WebRequest request = new ServletWebRequest(new MockHttpServletRequest());
		User newUser = userFormController.formBackingObject(request, null);
		newUser.setUsername("auditTest" + System.currentTimeMillis());
		newUser.addName(new PersonName("Audit", "Test", "Logger"));
		newUser.getPerson().setGender("F");

		userFormController.handleSubmission(
		    request,
		    new MockHttpSession(),
		    new ModelMap(),
		    null,        // action — null falls through to create/save branch
		    null,        // oldPassword — not needed for a new user
		    "Admin1234", // password
		    null,        // secretQuestion
		    null,        // secretAnswer
		    "Admin1234", // confirm — must match password
		    false,       // forcePassword
		    new String[0], // roles
		    null,        // createNewPerson
		    null,        // addToProviderTableOption
		    newUser,
		    new BindException(newUser, "user"),
		    new MockHttpServletResponse());

		String captured = userCapture.toString();
		assertTrue("Audit log must contain USER_CREATE", captured.contains("USER_CREATE"));
		assertTrue("Audit log must contain outcome=SUCCESS", captured.contains("outcome=SUCCESS"));
	}

}
