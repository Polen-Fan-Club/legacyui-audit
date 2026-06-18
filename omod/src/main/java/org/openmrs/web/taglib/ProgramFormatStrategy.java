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

import org.apache.commons.lang.StringUtils;
import org.openmrs.Program;

/**
 * Formats a {@link Program}: by its name if present, otherwise by its concept. Moved verbatim from
 * the original {@code FormatTag.printProgram()}; it composes the metadata and concept strategies.
 */
class ProgramFormatStrategy implements FormatStrategy {
	
	private final FormatStrategy metadataStrategy;
	
	private final FormatStrategy conceptStrategy;
	
	ProgramFormatStrategy(FormatStrategy metadataStrategy, FormatStrategy conceptStrategy) {
		this.metadataStrategy = metadataStrategy;
		this.conceptStrategy = conceptStrategy;
	}
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		Program program = (Program) value;
		if (StringUtils.isNotEmpty(program.getName())) {
			metadataStrategy.format(sb, program, ctx);
		} else if (program.getConcept() != null) {
			conceptStrategy.format(sb, program.getConcept(), ctx);
		}
	}
}
