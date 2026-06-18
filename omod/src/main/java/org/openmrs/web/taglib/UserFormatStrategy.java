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
import org.openmrs.User;

/**
 * Formats a {@link User} as a span with username and (optionally) person name. Moved verbatim from
 * the original {@code FormatTag.printUser()}.
 */
class UserFormatStrategy implements FormatStrategy {
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		User u = (User) value;
		sb.append("<span class=\"user\">");
		sb.append("<span class=\"username\">");
		sb.append(StringEscapeUtils.escapeHtml(u.getUsername()));
		sb.append("</span>");
		if (u.getPerson() != null) {
			sb.append("<span class=\"personName\">");
			sb.append(" (").append(StringEscapeUtils.escapeHtml(u.getPersonName().getFullName())).append(")");
			sb.append("</span>");
		}
		sb.append("</span>");
	}
}
