/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.taglib;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;

/**
 * Carries the {@link FormatTag} instance state that the {@link FormatStrategy} implementations need
 * to render an object: the concept-name selectors, the case conversion setting and the locale. This
 * removes the hidden coupling the old private {@code print*()} methods had to {@code FormatTag}'s
 * fields, so each strategy can be tested in isolation.
 */
public class FormatContext {
	
	private final String withConceptNameType;
	
	private final String withConceptNameTag;
	
	/**
	 * Mutable on purpose: the {@code "global"} value is resolved against the dashboard global
	 * property once and cached, exactly as the original {@code FormatTag.applyConversion()} did.
	 */
	private String caseConversion;
	
	private final Locale locale;
	
	public FormatContext(String withConceptNameType, String withConceptNameTag, String caseConversion, Locale locale) {
		this.withConceptNameType = withConceptNameType;
		this.withConceptNameTag = withConceptNameTag;
		this.caseConversion = caseConversion;
		this.locale = locale;
	}
	
	public String getWithConceptNameType() {
		return withConceptNameType;
	}
	
	public String getWithConceptNameTag() {
		return withConceptNameTag;
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	/**
	 * Applies the configured case conversion to the input string. Behaviour is identical to the
	 * original {@code FormatTag.applyConversion()}: a {@code "global"} setting is resolved against
	 * the {@link OpenmrsConstants#GP_DASHBOARD_METADATA_CASE_CONVERSION} global property and
	 * cached, after which {@code lowercase}/{@code uppercase}/{@code capitalize} are applied.
	 * 
	 * @param source the string to convert
	 * @return the converted string (unchanged if no conversion is configured)
	 */
	public String applyConversion(String source) {
		
		String result = source;
		
		// Find global property
		if ("global".equalsIgnoreCase(caseConversion)) {
			AdministrationService adminService = Context.getAdministrationService();
			caseConversion = adminService.getGlobalProperty(OpenmrsConstants.GP_DASHBOARD_METADATA_CASE_CONVERSION);
		}
		
		// Apply conversion
		if ("lowercase".equalsIgnoreCase(caseConversion)) {
			result = StringUtils.lowerCase(result);
		} else if ("uppercase".equalsIgnoreCase(caseConversion)) {
			result = StringUtils.upperCase(result);
		} else if ("capitalize".equalsIgnoreCase(caseConversion)) {
			result = WordUtils.capitalize(StringUtils.lowerCase(result));
		}
		return result;
	}
}
