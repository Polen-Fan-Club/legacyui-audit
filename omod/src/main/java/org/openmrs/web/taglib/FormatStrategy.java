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

/**
 * Strategy for rendering a single object onto a {@link StringBuilder}. There is one implementation
 * per domain type formatted by {@link FormatTag}. This replaces the {@code instanceof}/{@code else
 * if} cascade that used to live in {@link FormatTag#printObject(StringBuilder, Object)}, so adding
 * a new type means adding a new strategy and one registry entry instead of editing the cascade.
 */
public interface FormatStrategy {
	
	/**
	 * Formats the given value and appends the result to the string builder.
	 * 
	 * @param sb the string builder to append to
	 * @param value the object to format
	 * @param ctx the formatting context carrying the tag state the strategy may need
	 */
	void format(StringBuilder sb, Object value, FormatContext ctx);
}
