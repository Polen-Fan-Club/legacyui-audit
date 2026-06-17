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
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.CustomDatatype;
import org.openmrs.customdatatype.CustomDatatype.Summary;
import org.openmrs.customdatatype.CustomDatatypeHandler;
import org.openmrs.customdatatype.CustomDatatypeUtil;
import org.openmrs.customdatatype.CustomValueDescriptor;
import org.openmrs.customdatatype.DownloadableDatatypeHandler;
import org.openmrs.customdatatype.SingleCustomValue;
import org.openmrs.web.attribute.handler.HtmlDisplayableDatatypeHandler;

/**
 * Formats a {@link SingleCustomValue}, delegating to the configured datatype handler or datatype
 * for its (HTML or text) summary. Moved verbatim from the original
 * {@code FormatTag.printSingleCustomValue()}.
 */
class SingleCustomValueFormatStrategy implements FormatStrategy {
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void format(StringBuilder sb, Object value, FormatContext ctx) {
		SingleCustomValue<?> val = (SingleCustomValue<?>) value;
		CustomValueDescriptor descriptor = val.getDescriptor();
		CustomDatatype<?> datatype = CustomDatatypeUtil.getDatatype(descriptor);
		CustomDatatypeHandler handler = CustomDatatypeUtil.getHandler(descriptor);
		if (handler != null && handler instanceof HtmlDisplayableDatatypeHandler) {
			Summary summary = ((HtmlDisplayableDatatypeHandler) handler).toHtmlSummary(datatype, val.getValueReference());
			if (summary.isComplete()) {
				sb.append(StringEscapeUtils.escapeHtml(summary.toString()));
			} else {
				sb.append(StringEscapeUtils.escapeHtml(summary.toString()));
				sb.append("...");
				String link = "viewCustomValue.form?handler=" + handler.getClass().getName() + "&datatype="
				        + datatype.getClass().getName() + "&value=" + StringEscapeUtils.escapeHtml(val.getValueReference());
				sb.append(" (<a target=\"_blank\" href=\"" + link + "\">"
				        + Context.getMessageSourceService().getMessage("general.view") + "</a>)");
				
				if (handler instanceof DownloadableDatatypeHandler) {
					link = "downloadCustomValue.form?handler=" + handler.getClass().getName() + "&datatype="
					        + datatype.getClass().getName() + "&value="
					        + StringEscapeUtils.escapeHtml(val.getValueReference());
					sb.append(" (<a href=\"" + link + "\">"
					        + Context.getMessageSourceService().getMessage("general.download") + "</a>)");
				}
			}
		} else if (datatype != null) {
			Summary summary = datatype.getTextSummary(val.getValueReference());
			if (summary.isComplete()) {
				sb.append(StringEscapeUtils.escapeHtml(summary.toString()));
			} else {
				sb.append(StringEscapeUtils.escapeHtml(summary.toString()));
				sb.append("...");
			}
		} else {
			sb.append(StringEscapeUtils.escapeHtml(Context.getMessageSourceService().getMessage(
			    "CustomDatatype.error.missingDatatype", new Object[] { descriptor.getDatatypeClassname() }, ctx.getLocale())));
			sb.append(StringEscapeUtils.escapeHtml(val.getValueReference()));
		}
	}
}
