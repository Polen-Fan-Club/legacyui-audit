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

import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.springframework.web.util.HtmlUtils;

/**
 * Formats a {@link Concept}, respecting the {@code withConceptNameType} and
 * {@code withConceptNameTag} selectors if specified and a match is found. Moved verbatim from the
 * original {@code FormatTag.printConcept()}; the tag fields it used to read now come from
 * {@link FormatContext}.
 */
class ConceptFormatStrategy implements FormatStrategy {
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		Concept concept = (Concept) value;
		Locale loc = ctx.getLocale();
		
		if (ctx.getWithConceptNameType() != null || ctx.getWithConceptNameTag() != null) {
			ConceptNameType lookForNameType = null;
			
			if (ctx.getWithConceptNameType() != null) {
				lookForNameType = ConceptNameType.valueOf(ctx.getWithConceptNameType());
			}
			
			ConceptNameTag lookForNameTag = null;
			if (ctx.getWithConceptNameTag() != null) {
				lookForNameTag = Context.getConceptService().getConceptNameTagByName(ctx.getWithConceptNameTag());
			}
			
			ConceptName name = concept.getName(loc, lookForNameType, lookForNameTag);
			if (name != null) {
				sb.append(ctx.applyConversion(HtmlUtils.htmlEscape(name.getName())));
				return;
			}
		}
		
		ConceptName name = concept.getPreferredName(loc);
		if (name != null) {
			sb.append(ctx.applyConversion(HtmlUtils.htmlEscape(name.getName())));
			return;
		}
		sb.append(ctx.applyConversion(HtmlUtils.htmlEscape(concept.getDisplayString())));
	}
}
