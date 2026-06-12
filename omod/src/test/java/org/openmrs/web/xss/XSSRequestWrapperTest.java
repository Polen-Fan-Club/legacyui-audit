/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.xss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * Security-control test for {@link XSSRequestWrapper}: verifies that the wrapper actually sanitizes
 * request parameters and the request body (HTML-encodes XSS payloads), while leaving legitimate
 * input untouched and preserving null semantics. This is the wrapper used for ordinary
 * (non-multipart) write requests by {@link XSSFilter}.
 */
public class XSSRequestWrapperTest {
	
	private HttpServletRequest mockRequestReturning(String parameterName, String value) {
		HttpServletRequest delegate = mock(HttpServletRequest.class);
		when(delegate.getParameter(parameterName)).thenReturn(value);
		return delegate;
	}
	
	@Test
	public void getParameter_sanitizesScriptTag() {
		// Arrange: a classic stored-XSS payload arrives as a parameter value
		String payload = "<script>alert('xss')</script>";
		XSSRequestWrapper wrapper = new XSSRequestWrapper(mockRequestReturning("comment", payload));
		
		// Act
		String sanitized = wrapper.getParameter("comment");
		
		// Assert: the active markup is neutralised, encoded entities take its place
		assertFalse("Raw < must not survive sanitisation", sanitized.contains("<"));
		assertFalse("Raw > must not survive sanitisation", sanitized.contains(">"));
		assertFalse("The <script> tag must not survive verbatim", sanitized.contains("<script>"));
		assertTrue("< must be HTML-encoded to &lt;", sanitized.contains("&lt;"));
		assertTrue("> must be HTML-encoded to &gt;", sanitized.contains("&gt;"));
	}
	
	@Test
	public void getParameter_sanitizesAttributeBreakingEventHandler() {
		// Arrange: payload that tries to break out of an attribute and inject an event handler
		String payload = "\"><img src=x onerror=alert(1)>";
		XSSRequestWrapper wrapper = new XSSRequestWrapper(mockRequestReturning("name", payload));
		
		// Act
		String sanitized = wrapper.getParameter("name");
		
		// Assert: no live tag and no attribute-breaking double quote remain
		assertFalse("Injected <img> tag must not survive", sanitized.contains("<img"));
		assertFalse("Raw > must not survive", sanitized.contains(">"));
		assertFalse("Attribute-breaking double quote must be encoded", sanitized.contains("\""));
		assertTrue("< must be HTML-encoded", sanitized.contains("&lt;"));
	}
	
	@Test
	public void getParameter_percentEncodedLiteralRemainsInert() {
		// Arrange: if a percent-encoded literal ever reaches the wrapper undecoded, it must stay inert
		// (no executable angle brackets). The container normally decodes %3C -> < before getParameter,
		// in which case the script-tag test above applies; this guards the un-decoded path.
		XSSRequestWrapper wrapper = new XSSRequestWrapper(mockRequestReturning("q", "%3Cscript%3Ealert(1)%3C/script%3E"));
		
		// Act
		String sanitized = wrapper.getParameter("q");
		
		// Assert: contains no executable angle brackets
		assertFalse(sanitized.contains("<"));
		assertFalse(sanitized.contains(">"));
	}
	
	@Test
	public void getParameterValues_sanitizesEveryValue() {
		// Arrange
		HttpServletRequest delegate = mock(HttpServletRequest.class);
		when(delegate.getParameterValues("tags")).thenReturn(new String[] { "<b>ok</b>", "<svg onload=alert(1)>" });
		XSSRequestWrapper wrapper = new XSSRequestWrapper(delegate);
		
		// Act
		String[] sanitized = wrapper.getParameterValues("tags");
		
		// Assert: each element is encoded
		assertEquals(2, sanitized.length);
		for (String value : sanitized) {
			assertFalse("No raw < may survive in any array element", value.contains("<"));
			assertFalse("No raw > may survive in any array element", value.contains(">"));
			assertTrue("Each element must be HTML-encoded", value.contains("&lt;"));
		}
	}
	
	@Test
	public void getInputStream_sanitizesRequestBody() throws IOException {
		// Arrange: a JSON/body payload carrying a script tag
		final String body = "{\"note\":\"<script>steal()</script>\"}";
		HttpServletRequest delegate = mock(HttpServletRequest.class);
		when(delegate.getInputStream()).thenReturn(servletInputStreamOf(body));
		XSSRequestWrapper wrapper = new XSSRequestWrapper(delegate);
		
		// Act: read the wrapped (sanitised) body back out
		String sanitized = IOUtils.toString(wrapper.getInputStream(), "UTF-8");
		
		// Assert
		assertFalse("Request body must be sanitised too", sanitized.contains("<script>"));
		assertFalse(sanitized.contains("<"));
		assertTrue("Body angle brackets must be HTML-encoded", sanitized.contains("&lt;script&gt;"));
	}
	
	@Test
	public void getParameter_doesNotOverSanitizeLegitimateInput() {
		// Arrange: ordinary input with no HTML metacharacters must pass through unchanged
		String legit = "Ward 5 - Bed 12";
		XSSRequestWrapper wrapper = new XSSRequestWrapper(mockRequestReturning("location", legit));
		
		// Act + Assert
		assertEquals("Legitimate input must not be altered", legit, wrapper.getParameter("location"));
	}
	
	@Test
	public void getParameter_returnsNullWhenAbsent() {
		// Arrange: a missing parameter must stay null (no NPE, no empty-string substitution)
		XSSRequestWrapper wrapper = new XSSRequestWrapper(mockRequestReturning("missing", null));
		
		// Act + Assert
		assertNull(wrapper.getParameter("missing"));
		assertNull(wrapper.getParameterValues("missing"));
	}
	
	/**
	 * Minimal {@link ServletInputStream} over a UTF-8 string. Matches the Servlet 3.0 API surface
	 * used by the production wrapper (only {@code read()} is abstract).
	 */
	private static ServletInputStream servletInputStreamOf(String content) {
		final ByteArrayInputStream source = new ByteArrayInputStream(content.getBytes());
		return new ServletInputStream() {
			
			@Override
			public int read() throws IOException {
				return source.read();
			}
		};
	}
}
