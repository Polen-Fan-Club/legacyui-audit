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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.Program;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.customdatatype.SingleCustomValue;

/**
 * Type-indexed registry of {@link FormatStrategy} instances. Replaces the {@code instanceof}
 * cascade in {@code FormatTag.printObject()} with a lookup.
 * <p>
 * Resolution order is: exact class match, then assignable match (for subtypes, interfaces and
 * Hibernate proxies), then the default strategy. The registry is a {@link LinkedHashMap} whose
 * insertion order mirrors the original cascade (most specific first, {@code OpenmrsMetadata} last),
 * so assignable-matching of types that implement several registered interfaces — e.g.
 * {@code Provider}, {@code Form} and {@code Program} are all {@code OpenmrsMetadata} — resolves
 * exactly as the cascade did.
 */
class FormatStrategyRegistry {
	
	private final Map<Class<?>, FormatStrategy> strategies = new LinkedHashMap<Class<?>, FormatStrategy>();
	
	private final FormatStrategy defaultStrategy = new DefaultFormatStrategy();
	
	FormatStrategyRegistry() {
		FormatStrategy metadata = new MetadataFormatStrategy();
		FormatStrategy date = new DateFormatStrategy();
		FormatStrategy concept = new ConceptFormatStrategy();
		
		// Insertion order mirrors the original printObject() instanceof cascade.
		strategies.put(Date.class, date);
		strategies.put(Concept.class, concept);
		strategies.put(Obs.class, new ObsFormatStrategy());
		strategies.put(User.class, new UserFormatStrategy());
		strategies.put(Encounter.class, new EncounterFormatStrategy(metadata, date));
		strategies.put(Visit.class, new VisitFormatStrategy(metadata, date));
		strategies.put(Program.class, new ProgramFormatStrategy(metadata, concept));
		strategies.put(Provider.class, new ProviderFormatStrategy());
		strategies.put(Form.class, new FormFormatStrategy());
		strategies.put(SingleCustomValue.class, new SingleCustomValueFormatStrategy());
		strategies.put(OpenmrsMetadata.class, metadata);
	}
	
	/**
	 * Resolves the strategy for the given object: exact class match, then assignable match (first
	 * in cascade order), then the default fallback strategy.
	 * 
	 * @param o the object to resolve a strategy for (must not be null)
	 * @return the matching strategy, never null
	 */
	FormatStrategy resolve(Object o) {
		FormatStrategy exact = strategies.get(o.getClass());
		if (exact != null) {
			return exact;
		}
		for (Map.Entry<Class<?>, FormatStrategy> entry : strategies.entrySet()) {
			if (entry.getKey().isAssignableFrom(o.getClass())) {
				return entry.getValue();
			}
		}
		return defaultStrategy;
	}
}
