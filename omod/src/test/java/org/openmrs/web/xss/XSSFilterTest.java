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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

/**
 * Verifies the routing in {@link XSSFilter#doFilter}: write requests are wrapped in the appropriate
 * sanitising wrapper (multipart vs ordinary) so downstream code only ever sees encoded parameters,
 * while safe GET requests are passed through untouched. The wrappers' sanitisation itself is
 * covered by {@link XSSRequestWrapperTest} and {@link XSSMultipartRequestWrapperTest}.
 */
public class XSSFilterTest {
	
	private ServletRequest captureForwardedRequest(HttpServletRequest request) throws Exception {
		FilterChain chain = mock(FilterChain.class);
		new XSSFilter().doFilter(request, new MockHttpServletResponse(), chain);
		
		ArgumentCaptor<ServletRequest> forwarded = ArgumentCaptor.forClass(ServletRequest.class);
		verify(chain).doFilter(forwarded.capture(), any());
		return forwarded.getValue();
	}
	
	@Test
	public void doFilter_getRequestIsNotWrapped() throws Exception {
		// Arrange: GET requests carry no body/params to sanitise and must pass straight through
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		
		// Act
		ServletRequest forwarded = captureForwardedRequest(request);
		
		// Assert: the original request instance reaches the chain unchanged
		assertSame("GET must not be wrapped", request, forwarded);
	}
	
	@Test
	public void doFilter_postIsWrappedWithXSSRequestWrapper() throws Exception {
		// Arrange: an ordinary (non-multipart) POST
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("POST");
		when(request.getContentType()).thenReturn("application/x-www-form-urlencoded");
		
		// Act
		ServletRequest forwarded = captureForwardedRequest(request);
		
		// Assert
		assertTrue("Non-multipart POST must be wrapped in XSSRequestWrapper", forwarded instanceof XSSRequestWrapper);
	}
	
	@Test
	public void doFilter_multipartPostIsWrappedWithMultipartWrapper() throws Exception {
		// Arrange: a multipart (file upload) POST
		DefaultMultipartHttpServletRequest request = mock(DefaultMultipartHttpServletRequest.class);
		when(request.getMethod()).thenReturn("POST");
		when(request.getContentType()).thenReturn("multipart/form-data; boundary=----abc");
		
		// Act
		ServletRequest forwarded = captureForwardedRequest(request);
		
		// Assert
		assertTrue("Multipart POST must be wrapped in XSSMultipartRequestWrapper",
		    forwarded instanceof XSSMultipartRequestWrapper);
	}
}
