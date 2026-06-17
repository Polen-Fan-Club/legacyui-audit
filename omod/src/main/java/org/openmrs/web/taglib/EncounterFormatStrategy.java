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

import org.openmrs.Encounter;

/**
 * Formats an {@link Encounter} as "type @location | datetime | ". Moved verbatim from the original
 * {@code FormatTag.printEncounter()}; it composes the metadata and date strategies for its parts
 * instead of calling private helpers.
 */
class EncounterFormatStrategy implements FormatStrategy {
	
	private final FormatStrategy metadataStrategy;
	
	private final FormatStrategy dateStrategy;
	
	EncounterFormatStrategy(FormatStrategy metadataStrategy, FormatStrategy dateStrategy) {
		this.metadataStrategy = metadataStrategy;
		this.dateStrategy = dateStrategy;
	}
	
	@Override
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		Encounter encounter = (Encounter) value;
		metadataStrategy.format(sb, encounter.getEncounterType(), ctx);
		sb.append(" @");
		metadataStrategy.format(sb, encounter.getLocation(), ctx);
		sb.append(" | ");
		dateStrategy.format(sb, encounter.getEncounterDatetime(), ctx);
		sb.append(" | ");
	}
}
