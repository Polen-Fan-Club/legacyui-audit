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

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Fallback strategy for any type without a dedicated strategy: HTML-escapes {@code toString()}.
 * Moved verbatim from the {@code else} branch of the original {@code FormatTag.printObject()}.
 */
class DefaultFormatStrategy implements FormatStrategy {
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		sb.append("" + StringEscapeUtils.escapeHtml(value.toString()));
	}
}
