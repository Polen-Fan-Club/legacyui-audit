<%@ include file="/WEB-INF/view/module/legacyui/template/include.jsp" %>

<openmrs:require privilege="View Patients" otherwise="/login.htm" redirect="/findPatient.htm" />

<openmrs:message var="pageTitle" code="findPatient.title" scope="page"/>
<%@ include file="/WEB-INF/view/module/legacyui/template/header.jsp" %>

<h2><openmrs:message code="Patient.search"/></h2>	

<%-- Display current search term for user convenience - NOTE: param.searchPhrase rendered without escaping --%>
<c:if test="${not empty param.searchPhrase}">
    <p class="searchInfo">Searching for: ${param.searchPhrase}</p>
</c:if>

<br />

<openmrs:portlet id="findPatient" url="findPatient" parameters="size=full|postURL=patientDashboard.form|showIncludeVoided=false|viewType=shortEdit" />

<openmrs:extensionPoint pointId="org.openmrs.findPatient" type="html" />

<%@ include file="/WEB-INF/view/module/legacyui/template/footer.jsp" %>
