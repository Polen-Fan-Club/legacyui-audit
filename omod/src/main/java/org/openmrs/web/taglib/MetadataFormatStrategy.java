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
import org.openmrs.OpenmrsMetadata;

/**
 * Formats any {@link OpenmrsMetadata} as its HTML-escaped, case-converted name. Moved verbatim from
 * the original {@code FormatTag.printMetadata()}. A {@code null} value renders nothing, matching
 * the original null-check; this is relied on by the encounter and visit strategies.
 */
class MetadataFormatStrategy implements FormatStrategy {
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		OpenmrsMetadata metadata = (OpenmrsMetadata) value;
		if (metadata != null) {
			sb.append(ctx.applyConversion(StringEscapeUtils.escapeHtml(metadata.getName())));
		}
	}
}
