/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

/**
 * NEN-7510 §8.15 audit-logging test for {@link LogoutServlet}. Mirrors {@code AuditLoggingTest}:
 * verifies that a logout produces a WARN-level AUDIT event with the expected fields, that the actor
 * is captured before the session is cleared, that the HTTP session is invalidated, and that no
 * session token leaks into the audit line.
 */
public class LogoutServletAuditTest extends BaseModuleWebContextSensitiveTest {
	
	private StringWriter logoutCapture;
	
	private WriterAppender logoutAppender;
	
	@Before
	public void attachLogAppender() {
		logoutCapture = new StringWriter();
		logoutAppender = new WriterAppender(new SimpleLayout(), logoutCapture);
		Logger logoutLogger = Logger.getLogger(LogoutServlet.class);
		logoutLogger.setLevel(Level.WARN);
		logoutLogger.addAppender(logoutAppender);
	}
	
	@After
	public void detachLogAppender() {
		Logger.getLogger(LogoutServlet.class).removeAppender(logoutAppender);
	}
	
	@Test
	public void doGet_logsLogoutAuditEvent() throws Exception {
		// Arrange: an authenticated session (the base test authenticates as the admin user)
		assertTrue("Precondition: a user must be authenticated", Context.isAuthenticated());
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/logout");
		req.setContextPath("/openmrs");
		req.setSession(new MockHttpSession());
		
		// Act
		new LogoutServlet().service(req, new MockHttpServletResponse());
		
		// Assert: the audit line carries the event name, the actor (captured before logout) and outcome
		String captured = logoutCapture.toString();
		assertTrue("Audit log must contain AUDIT LOGOUT", captured.contains("AUDIT LOGOUT"));
		assertTrue("Audit log must contain outcome=SUCCESS", captured.contains("outcome=SUCCESS"));
		assertTrue("Audit log must record the client IP", captured.contains("ip=127.0.0.1"));
		assertTrue("Actor must be captured before logout (a real user, not null/anonymous)", captured.contains("user=")
		        && !captured.contains("user=null") && !captured.contains("user=anonymous"));
	}
	
	@Test
	public void doGet_invalidatesSession() throws Exception {
		// Arrange
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/logout");
		req.setContextPath("/openmrs");
		MockHttpSession session = new MockHttpSession();
		req.setSession(session);
		
		// Act
		new LogoutServlet().service(req, new MockHttpServletResponse());
		
		// Assert: the session is invalidated - touching it afterwards must fail per the servlet spec
		try {
			session.getAttribute("anything");
			fail("Session must be invalidated after logout");
		}
		catch (IllegalStateException expected) {
			// expected: invalidated session rejects access
		}
	}
	
	@Test
	public void doGet_auditLogDoesNotLeakSessionId() throws Exception {
		// Arrange: a distinctive session id that cannot collide with other audit fields (ip, outcome)
		String sessionId = "ZZ-SESSION-TOKEN-9q8w7e6r";
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/logout");
		req.setContextPath("/openmrs");
		MockHttpSession session = new MockHttpSession(null, sessionId);
		req.setSession(session);
		
		// Act
		new LogoutServlet().service(req, new MockHttpServletResponse());
		
		// Assert: the session identifier must not appear in the audit trail
		assertFalse("Audit log must not leak the session id", logoutCapture.toString().contains(sessionId));
	}
}
