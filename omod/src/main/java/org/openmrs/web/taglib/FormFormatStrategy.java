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
import org.openmrs.Form;

/**
 * Formats a {@link Form} as "name (vVersion)", HTML-escaped. Moved verbatim from the original
 * {@code FormatTag.printForm()}.
 */
class FormFormatStrategy implements FormatStrategy {
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		Form form = (Form) value;
		String name = StringEscapeUtils.escapeHtml(form.getName());
		sb.append(name + StringEscapeUtils.escapeHtml(" (v" + form.getVersion() + ")"));
	}
}
