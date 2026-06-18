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

import static org.junit.Assert.assertEquals;
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
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests the OWASP A05 fix in {@link LegacyUiExceptionAdvice}: an authorization failure must yield a
 * generic 403 to the client with no stack trace or internal detail, while the real cause is still
 * logged server-side. Covers both branches of the actor resolution (authenticated and anonymous).
 */
public class LegacyUiExceptionAdviceTest extends BaseModuleWebContextSensitiveTest {
	
	private static final String SECRET_CAUSE = "Privilege required: Manage Users";
	
	private static final String REQUEST_URI = "/openmrs/admin/users/user.form";
	
	private final LegacyUiExceptionAdvice advice = new LegacyUiExceptionAdvice();
	
	private StringWriter adviceCapture;
	
	private WriterAppender adviceAppender;
	
	@Before
	public void attachLogAppender() {
		adviceCapture = new StringWriter();
		adviceAppender = new WriterAppender(new SimpleLayout(), adviceCapture);
		Logger adviceLogger = Logger.getLogger(LegacyUiExceptionAdvice.class);
		adviceLogger.setLevel(Level.WARN);
		adviceLogger.addAppender(adviceAppender);
	}
	
	@After
	public void detachLogAppender() {
		Logger.getLogger(LegacyUiExceptionAdvice.class).removeAppender(adviceAppender);
	}
	
	private MockHttpServletRequest requestForProtectedPage() {
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_URI);
		req.setRequestURI(REQUEST_URI);
		return req;
	}
	
	@Test
	public void handleAuthorizationFailure_returnsGeneric403WithoutStackTrace() {
		// Arrange
		APIAuthenticationException ex = new APIAuthenticationException(SECRET_CAUSE);
		
		// Act
		ResponseEntity<String> response = advice.handleAuthorizationFailure(ex, requestForProtectedPage());
		
		// Assert: generic 403, plain text, no internal detail leaks to the client
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
		String body = response.getBody();
		assertTrue("Client must get a generic access-denied message", body.contains("Access denied"));
		assertFalse("The underlying cause must NOT be disclosed to the client", body.contains(SECRET_CAUSE));
		assertFalse("No exception class name may leak to the client", body.contains("APIAuthenticationException"));
		assertFalse("No package/stack-frame detail may leak to the client", body.contains("org.openmrs"));
	}
	
	@Test
	public void handleAuthorizationFailure_logsCauseServerSide() {
		// Arrange: authenticated actor (the base test authenticates a user)
		assertTrue("Precondition: a user must be authenticated", Context.isAuthenticated());
		APIAuthenticationException ex = new APIAuthenticationException(SECRET_CAUSE);
		
		// Act
		advice.handleAuthorizationFailure(ex, requestForProtectedPage());
		
		// Assert: the cause IS recorded server-side (audit/incident logging stays intact)
		String logged = adviceCapture.toString();
		assertTrue("Cause must be logged server-side", logged.contains(SECRET_CAUSE));
		assertTrue("Request URI must be logged for incident triage", logged.contains(REQUEST_URI));
		assertTrue("Authenticated-actor branch must be taken (not anonymous)",
		    logged.contains("Authorization denied for user '") && !logged.contains("user 'anonymous'"));
	}
	
	@Test
	public void handleAuthorizationFailure_recordsAnonymousWhenNotAuthenticated() {
		// Arrange: no authenticated user -> the anonymous branch
		Context.logout();
		assertFalse("Precondition: no user authenticated", Context.isAuthenticated());
		APIAuthenticationException ex = new APIAuthenticationException(SECRET_CAUSE);
		
		// Act
		ResponseEntity<String> response = advice.handleAuthorizationFailure(ex, requestForProtectedPage());
		
		// Assert: still a generic 403, and the actor is logged as anonymous
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertTrue("Anonymous-actor branch must be taken", adviceCapture.toString().contains("user 'anonymous'"));
	}
}
