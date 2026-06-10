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

import javax.servlet.http.HttpServletRequest;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Suppresses verbose exception disclosure for the legacy UI controllers (OWASP A05). An
 * authorization failure thrown while invoking a legacy-UI handler (e.g. a privileged call in a
 * {@code @ModelAttribute} method) was otherwise serialized straight to the client as a full Java
 * stack trace - the complete servlet filter chain, internal class names + line numbers and the Java
 * runtime version - with HTTP 200. This advice returns a generic 403 instead, while still logging
 * the real cause server-side so audit/incident logging stays intact.
 * <p>
 * Scoped to the legacy UI controller packages via {@code basePackages} so it does not act as a
 * global handler for other modules' controllers. Registered as an explicit bean in
 * webModuleApplicationContext.xml (the module's component scan only includes {@code @Controller}).
 */
@ControllerAdvice(basePackages = { "org.openmrs.web", "org.openmrs.hl7.web.controller", "org.openmrs.module.web.controller",
        "org.openmrs.template.web.controller" })
public class LegacyUiExceptionAdvice {
	
	private static final Logger log = LoggerFactory.getLogger(LegacyUiExceptionAdvice.class);
	
	/**
	 * Handles an authorization failure without leaking a stack trace to the client. The real cause
	 * is logged server-side (so incident/audit logging is unaffected); the client receives only a
	 * generic 403 with a short body - a body is returned deliberately so the response is committed
	 * and the container's default error page (which carries the server version banner) is not used.
	 * 
	 * @param ex the authorization exception thrown during handler invocation
	 * @param request the current request (used for the server-side log line only)
	 * @return a generic 403 response without any stack trace, filter chain or version information
	 */
	@ExceptionHandler(APIAuthenticationException.class)
	public ResponseEntity<String> handleAuthorizationFailure(APIAuthenticationException ex, HttpServletRequest request) {
		String actor = Context.isAuthenticated() ? Context.getAuthenticatedUser().getUsername() : "anonymous";
		log.warn("Authorization denied for user '" + actor + "' on " + request.getRequestURI() + ": " + ex.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN)
		        .body("Access denied: you do not have the required privilege for this page.");
	}
}
