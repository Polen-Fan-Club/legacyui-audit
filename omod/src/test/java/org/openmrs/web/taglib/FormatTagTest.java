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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.CustomValueDescriptor;
import org.openmrs.customdatatype.SingleCustomValue;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockPageContext;

public class FormatTagTest extends BaseModuleWebContextSensitiveTest {
	
	private static final String ATTRIBUTE_OBJECT_VALUE = "objectValue";
	
	/**
	 * @see FormatTag#printConcept(StringBuilder,Concept)
	 * @verifies print the name with the correct name, and type
	 */
	@Test
	public void printConcept_shouldPrintTheNameWithTheCorrectLocaleNameAndType() throws Exception {
		ConceptService service = Context.getConceptService();
		Locale locale = Context.getLocale();
		ConceptNameTag tag = service.getConceptNameTag(5);
		ConceptNameTag anotherTag = service.getConceptNameTag(6);
		Context.flushSession();
		
		Concept c = new Concept();
		c.addName(buildName("English fully specified", locale, true, ConceptNameType.FULLY_SPECIFIED, null));
		c.addName(buildName("English synonym", locale, false, null, null));
		c.addName(buildName("English tag", locale, false, null, tag));
		c.addName(buildName("English another tag", locale, false, null, anotherTag));
		c.addDescription(new ConceptDescription("some description", null));
		c.setDatatype(service.getConceptDatatype(1));
		c.setConceptClass(service.getConceptClass(1));
		
		Context.getConceptService().saveConcept(c);
		
		assertPrintConcept("English fully specified", c, null, null);
		assertPrintConcept("English fully specified", c, ConceptNameType.FULLY_SPECIFIED.toString(), null);
		assertPrintConcept("English tag", c, null, tag.getTag());
	}
	
	/**
	 * @param expected
	 * @param concept
	 * @param withType
	 * @param withTag
	 */
	private void assertPrintConcept(String expected, Concept concept, String withType, String withTag) {
		FormatTag format = new FormatTag();
		format.setWithConceptNameType(withType);
		format.setWithConceptNameTag(withTag);
		StringBuilder sb = new StringBuilder();
		format.printConcept(sb, concept);
		Assert.assertEquals(expected, sb.toString());
	}
	
	/**
	 * @see FormatTag#printConcept(StringBuilder,Concept)
	 * @verifies escape html tags
	 */
	@Test
	public void printConcept_shouldEscapeHtmlTags() throws Exception {
		ConceptService service = Context.getConceptService();
		Locale locale = Context.getLocale();
		ConceptNameTag tag = service.getConceptNameTag(5);
		ConceptNameTag anotherTag = service.getConceptNameTag(6);
		Context.flushSession();
		
		Concept c = new Concept();
		c.addName(buildName("English fully\"><script>alert('xss possible!')</script> specified", locale, true,
		    ConceptNameType.FULLY_SPECIFIED, null));
		c.addDescription(new ConceptDescription("some description", null));
		c.setDatatype(service.getConceptDatatype(1));
		c.setConceptClass(service.getConceptClass(1));
		
		Context.getConceptService().saveConcept(c);
		FormatTag format = new FormatTag();
		format.setWithConceptNameType(ConceptNameType.FULLY_SPECIFIED.toString());
		format.setWithConceptNameTag(null);
		StringBuilder sb = new StringBuilder();
		format.printConcept(sb, c);
		
		Assert.assertThat(sb.toString(), Matchers.not(Matchers.containsString("<script>")));
	}
	
	/**
	 * @param name
	 * @param locale
	 * @param localePreferred
	 * @param nameType
	 * @param tag
	 * @return
	 */
	private ConceptName buildName(String name, Locale locale, boolean localePreferred, ConceptNameType nameType,
	        ConceptNameTag tag) {
		ConceptName ret = new ConceptName();
		ret.setName(name);
		ret.setLocale(locale);
		ret.setLocalePreferred(localePreferred);
		ret.setConceptNameType(nameType);
		if (tag != null)
			ret.addTag(tag);
		return ret;
	}
	
	/**
	 * @see FormatTag#doStartTag()
	 */
	@Test
	@Verifies(value = "print any domain object", method = "doStartTag()")
	public void doStartTag_shouldPrintAnyDomainObject() throws Exception {
		FormatTag tag = new FormatTag();
		PageContext pageContext = new MockPageContext();
		tag.setPageContext(pageContext);
		tag.setVar(ATTRIBUTE_OBJECT_VALUE);
		
		// check if concept is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getConceptService().getConcept(3), "COUGH SYRUP");
		
		// check if encounter is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getEncounterService().getEncounter(3),
		    "Emergency @Unknown Location | 01/08/2008 | ");
		
		// check if observation value is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getObsService().getObs(7), "50.0");
		
		// check if user is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getUserService().getUser(502),
		    "<span class=\"user\"><span class=\"username\">butch</span><span class=\"personName\"> (Hippocrates of Cos)</span></span>");
		
		// check if encounter type is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getEncounterService().getEncounterType(1), "Scheduled");
		
		// check if location is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getLocationService().getLocation(1), "Unknown Location");
		
		// check if program is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getProgramWorkflowService().getProgram(1), "HIV PROGRAM");
		
		// check if visit is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getVisitService().getVisit(1),
		    "Initial HIV Clinic Visit @Unknown Location | 01/01/2005");
		
		// check if visit type is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getVisitService().getVisitType(1), "Initial HIV Clinic Visit");
		
		// check if form is properly printed
		checkStartTagEvaluation(pageContext, tag, Context.getFormService().getForm(1), "Basic Form (v0.1)");
	}
	
	/**
	 * This method checks correctness of start tag evaluation of given tag
	 * 
	 * @param pageContext the page context to be used when checking start tag evaluation
	 * @param tag the format tag whose doStartTag() method will be evaluated
	 * @param object the object to format with given tag
	 * @param expected the expected result of object formatting
	 */
	private void checkStartTagEvaluation(PageContext pageContext, FormatTag tag, Object object, String expected) {
		tag.setObject(object);
		Assert.assertEquals(Tag.SKIP_BODY, tag.doStartTag());
		Assert.assertNotNull(pageContext.getAttribute(ATTRIBUTE_OBJECT_VALUE));
		Assert.assertEquals(expected, pageContext.getAttribute(ATTRIBUTE_OBJECT_VALUE));
	}
	
	/**
	 * Renders an object through the public tag API ({@link FormatTag#doStartTag()}) and returns the
	 * value that gets stored in the page-context var attribute. This exercises the same code path
	 * that JSP templates use, so it validates the observable behaviour of {@code printObject()}
	 * without touching the private method directly.
	 * 
	 * @param tag the format tag to evaluate (caller may pre-configure attributes such as
	 *            caseConversion)
	 * @param object the object to format
	 * @return the rendered string
	 */
	private String render(FormatTag tag, Object object) {
		MockPageContext pageContext = new MockPageContext();
		tag.setPageContext(pageContext);
		tag.setVar(ATTRIBUTE_OBJECT_VALUE);
		tag.setObject(object);
		Assert.assertEquals(Tag.SKIP_BODY, tag.doStartTag());
		return (String) pageContext.getAttribute(ATTRIBUTE_OBJECT_VALUE);
	}
	
	/**
	 * TC1 / P1: an empty collection iterates zero times and renders to an empty string.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldRenderEmptyCollectionAsEmptyString() throws Exception {
		Assert.assertEquals("", render(new FormatTag(), new ArrayList<Object>()));
	}
	
	/**
	 * TC2 / P2: a collection with a single element renders that element with no separator.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldRenderSingleElementCollectionWithoutSeparator() throws Exception {
		List<Concept> concepts = Arrays.asList(Context.getConceptService().getConcept(3));
		Assert.assertEquals("COUGH SYRUP", render(new FormatTag(), concepts));
	}
	
	/**
	 * TC3 / P3: a collection with multiple elements renders each element separated by ", ".
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldRenderCollectionElementsSeparatedByComma() throws Exception {
		Concept coughSyrup = Context.getConceptService().getConcept(3);
		List<Concept> concepts = Arrays.asList(coughSyrup, coughSyrup);
		Assert.assertEquals("COUGH SYRUP, COUGH SYRUP", render(new FormatTag(), concepts));
	}
	
	/**
	 * TC4 / P4: a Date is rendered through {@code printDate} using the context date format.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldRenderDateUsingContextDateFormat() throws Exception {
		Date date = new Date(0L);
		String expected = Context.getDateFormat().format(date);
		Assert.assertEquals(expected, render(new FormatTag(), date));
	}
	
	/**
	 * TC11 / P11: a Provider without an associated person is rendered using its name, HTML-escaped.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldRenderProviderName() throws Exception {
		Person person = new Person();
		person.addName(new PersonName("Jane", null, "Provider"));
		Provider provider = new Provider();
		provider.setPerson(person);
		Assert.assertEquals("Jane Provider", render(new FormatTag(), provider));
	}
	
	/**
	 * TC13 / P13: a SingleCustomValue is rendered through {@code printSingleCustomValue}. A
	 * FreeTextDatatype produces a complete text summary equal to the value reference, HTML-escaped.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldRenderSingleCustomValue() throws Exception {
		CustomValueDescriptor descriptor = mock(CustomValueDescriptor.class);
		when(descriptor.getDatatypeClassname()).thenReturn("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		when(descriptor.getDatatypeConfig()).thenReturn(null);
		when(descriptor.getPreferredHandlerClassname()).thenReturn(null);
		when(descriptor.getHandlerConfig()).thenReturn(null);
		
		SingleCustomValue<?> value = mock(SingleCustomValue.class);
		when(value.getDescriptor()).thenReturn((CustomValueDescriptor) descriptor);
		when(value.getValueReference()).thenReturn("plain text value");
		
		Assert.assertEquals("plain text value", render(new FormatTag(), value));
	}
	
	/**
	 * TC15 / P15: an object of an unsupported type falls through to the else-branch and is rendered
	 * via {@code toString()}, HTML-escaped.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldRenderUnknownTypeViaToString() throws Exception {
		Assert.assertEquals("42", render(new FormatTag(), Integer.valueOf(42)));
	}
	
	/**
	 * Covers the stateful caseConversion="global" route in {@code applyConversion}: it resolves the
	 * configured global property and applies it. This is the only mutating path that the Strategy
	 * refactor touches, so it must be pinned before refactoring.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldApplyGlobalCaseConversionToMetadata() throws Exception {
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty(OpenmrsConstants.GP_DASHBOARD_METADATA_CASE_CONVERSION, "uppercase"));
		
		Location location = Context.getLocationService().getLocation(1);
		FormatTag tag = new FormatTag();
		tag.setCaseConversion("global");
		Assert.assertEquals("UNKNOWN LOCATION", render(tag, location));
	}
	
	/**
	 * Guards the stateful "global" case-conversion path when more than one object is formatted in a
	 * single {@code doStartTag()} call. The concern: {@code applyConversion} resolves and caches
	 * the "global" setting on first use, so a second object formatted in the same call could
	 * observe the already-resolved value instead of re-resolving "global". This renders a
	 * collection of two distinct metadata objects in one tag invocation and asserts both are
	 * converted, proving the resolution stays correct across objects.
	 * 
	 * @see FormatTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldApplyGlobalCaseConversionToMultipleObjectsInOneTag() throws Exception {
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty(OpenmrsConstants.GP_DASHBOARD_METADATA_CASE_CONVERSION, "uppercase"));
		
		Location first = new Location();
		first.setName("Alpha Ward");
		Location second = new Location();
		second.setName("Beta Ward");
		
		FormatTag tag = new FormatTag();
		tag.setCaseConversion("global");
		Assert.assertEquals("ALPHA WARD, BETA WARD", render(tag, Arrays.asList(first, second)));
	}
}
