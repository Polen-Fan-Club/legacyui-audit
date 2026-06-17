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
import org.openmrs.Provider;

/**
 * Formats a {@link Provider} using its person name, or its own name when no person is linked. Moved
 * verbatim from the original {@code FormatTag.printProvider()} / {@code getProviderName()}.
 */
class ProviderFormatStrategy implements FormatStrategy {
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		Provider p = (Provider) value;
		if (p != null) {
			sb.append(StringEscapeUtils.escapeHtml(getProviderName(p)));
		}
	}
	
	private String getProviderName(Provider provider) {
		if (provider.getPerson() != null) {
			return provider.getPerson().getPersonName().getFullName();
		} else {
			return provider.getName();
		}
	}
}
