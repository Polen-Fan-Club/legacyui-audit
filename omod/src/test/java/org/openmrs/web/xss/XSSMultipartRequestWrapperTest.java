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

import org.junit.Test;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

/**
 * Security-control test for {@link XSSMultipartRequestWrapper}: verifies that parameters of a
 * multipart (file upload) write request are HTML-encoded, that legitimate input is left intact and
 * that null semantics are preserved. This is the wrapper {@link XSSFilter} applies to multipart
 * requests.
 */
public class XSSMultipartRequestWrapperTest {
	
	/**
	 * Builds the wrapper over a mocked underlying multipart request. The wrapper delegates
	 * parameter access through {@code getRequest()}, which returns exactly the request passed to
	 * the constructor.
	 */
	private XSSMultipartRequestWrapper wrapperOver(DefaultMultipartHttpServletRequest delegate) {
		return new XSSMultipartRequestWrapper(delegate);
	}
	
	@Test
	public void getParameter_sanitizesScriptTag() {
		// Arrange
		DefaultMultipartHttpServletRequest delegate = mock(DefaultMultipartHttpServletRequest.class);
		when(delegate.getParameter("caption")).thenReturn("<script>alert('xss')</script>");
		XSSMultipartRequestWrapper wrapper = wrapperOver(delegate);
		
		// Act
		String sanitized = wrapper.getParameter("caption");
		
		// Assert
		assertFalse("The <script> tag must not survive in a multipart parameter", sanitized.contains("<script>"));
		assertFalse(sanitized.contains("<"));
		assertFalse(sanitized.contains(">"));
		assertTrue("Angle brackets must be HTML-encoded", sanitized.contains("&lt;") && sanitized.contains("&gt;"));
	}
	
	@Test
	public void getParameterValues_sanitizesEveryValue() {
		// Arrange
		DefaultMultipartHttpServletRequest delegate = mock(DefaultMultipartHttpServletRequest.class);
		when(delegate.getParameterValues("tags")).thenReturn(new String[] { "<img src=x onerror=alert(1)>", "ok" });
		XSSMultipartRequestWrapper wrapper = wrapperOver(delegate);
		
		// Act
		String[] sanitized = wrapper.getParameterValues("tags");
		
		// Assert
		assertEquals(2, sanitized.length);
		assertFalse("Injected tag must be neutralised", sanitized[0].contains("<img"));
		assertFalse(sanitized[0].contains("<"));
		assertTrue(sanitized[0].contains("&lt;"));
		assertEquals("Plain value must be unchanged", "ok", sanitized[1]);
	}
	
	@Test
	public void getParameter_doesNotOverSanitizeLegitimateInput() {
		// Arrange
		DefaultMultipartHttpServletRequest delegate = mock(DefaultMultipartHttpServletRequest.class);
		when(delegate.getParameter("title")).thenReturn("Patient intake form 2026");
		XSSMultipartRequestWrapper wrapper = wrapperOver(delegate);
		
		// Act + Assert
		assertEquals("Patient intake form 2026", wrapper.getParameter("title"));
	}
	
	@Test
	public void getParameter_returnsNullWhenAbsent() {
		// Arrange
		DefaultMultipartHttpServletRequest delegate = mock(DefaultMultipartHttpServletRequest.class);
		when(delegate.getParameter("missing")).thenReturn(null);
		when(delegate.getParameterValues("missing")).thenReturn(null);
		XSSMultipartRequestWrapper wrapper = wrapperOver(delegate);
		
		// Act + Assert
		assertNull(wrapper.getParameter("missing"));
		assertNull(wrapper.getParameterValues("missing"));
	}
}
