# Attack Surface Mapping — Tabel

**Module:** OpenMRS Legacy UI v1.20.0  
**Audit:** NEN-7510:2024-2 maatregel 8.25 — Attack Surface Mapping  
**Datum:** 2026-06-09  
**Methode:** statische broncode-inspectie (Java-controllers, servlets, DWR-config, JSP-formulieren)

---

## 1. HTTP-controllers & servlets

| Endpoint | Methode | Vereiste privilege | Inputvalidatie | Autorisatiecheck | Risico | Opmerking |
|---|---|---|---|---|---|---|
| `/login` | GET | Geen (publiek) | n.v.t. | Geen | 🟢 LOW | LoginController.java:36 — bewust publiek; redirect als al ingelogd |
| `forgotPassword.form` | GET/POST | Geen (publiek) | Gedeeltelijk (rate-limit 5/IP, StringUtils-check op username) | Geen | 🟡 MEDIUM | ForgotPasswordFormController.java:89 — publiek bereikbaar; response-divergentie maakt gebruikersenumeratie mogelijk (reg. 131–145); zie gap 8.5-6 |
| `remotecommunication/postHl7.form` | POST | Geen — accepteert eigen username/password in POST-body | Geen (StringUtils.hasText) | In-band: authenticatie via POST-parameters | 🔴 HIGH | PostHl7Controller.java:38-45 — plaintext credentials in HTTP-body; omzeilt sessiemodel; geen privilege-check na auth; HL7-berichten worden direct in wachtrij geplaatst zonder inhoudsvalidatie |
| `hl7InQueueProcessorServlet` | GET | `isAuthenticated()` | Geen | Alleen sessiecheck | 🟡 MEDIUM | HL7InQueueProcessorServlet.java:38-42 — start HL7-verwerkingsqueue; iedere ingelogde gebruiker, geen specifiek privilege |
| `admin/modules/module.list` (action=upload) | POST | `MANAGE_MODULES` | WebUtil.stripFilename op bestandsnaam; geen MIME/inhoud-validatie | Context.hasPrivilege (ModuleListController.java:78) | 🔴 HIGH | ModuleListController.java:99-151 — .omod-bestandsupload; gelaadde module draait met volledige server-rechten; downloadURL-parameter (reg. 112-120) is een tweede SSRF-vector |
| `admin/modules/manage/fetchModuleDetails.form` | GET | Geen expliciete privilege-check in endpoint | Geen | Authentheated-only (implicieet via admin-area filter) | 🔴 HIGH | ModuleManagementController.java:50-53 — `new java.net.URL(url).openStream()` zonder URL-validatie; **Server-Side Request Forgery (SSRF)**; kan intern netwerk bereiken; geen privilege-check in methode zelf |
| `admin/modules/manage/checkdependencies.form` | GET | Geen expliciete privilege-check | Geen | Implicieet via admin-area | 🟡 MEDIUM | ModuleManagementController.java:40-45 — moduleId-parameter direct aan ModuleFactory; geen sanitisatie |
| `admin/maintenance/globalProps.form` | GET/POST | `isAuthenticated()` — geen specifiek privilege | Geen expliciete validatie; Arrays van key/value direct opgeslagen | Alleen `Context.isAuthenticated()` (GlobalPropertyController.java:70) | 🔴 HIGH | Iedere ingelogde gebruiker kan global properties lezen én overschrijven, inclusief veiligheidsinstellingen (wachtwoordbeleid, brute-force-drempel, log-niveaus); geen `MANAGE_GLOBAL_PROPERTIES`-check op controller-niveau |
| `admin/maintenance/settings.list` | GET/POST | Geen expliciete privilege-check in controller | WebAttributeUtil.getValue — type-afhankelijk | Geen zichtbare check in SettingsController.java | 🟡 MEDIUM | SettingsController.java:58-80 — instellen van systeem-properties; privilege-check ontbreekt op controller-niveau; vertrouwt op service-laag |
| `admin/maintenance/serverLog.form` | GET | Geen expliciete privilege-check | n.v.t. (alleen lezen) | Geen zichtbare check | 🟡 MEDIUM | ServerLogController.java:66-94 — exposeert volledige server-log inclusief interne stacktraces, SQL-queries, gebruikersnamen; geen controller-niveau privilege-check; `Context.isAuthenticated()` vereist via platform |
| `admin/maintenance/systemInfo.htm` | GET | Geen expliciete privilege-check | n.v.t. | Geen zichtbare check (SystemInformationController.java:40) | 🟡 MEDIUM | Exposeert Java-runtime, DB-info, module-versies, OS-details — recon-data voor aanvaller |
| `admin/maintenance/rebuildSearchIndex.htm` | POST | `isAuthenticated()` | Geen | Alleen `Context.getUserContext().isAuthenticated()` (SearchIndexController.java:55) | 🟡 MEDIUM | Trigger voor resource-intensieve herindexering; potentieel DoS-vector; iedere ingelogde gebruiker |
| `admin/users/user.form` | GET/POST | `EDIT_USERS` / `ADD_USERS` | ValidateUtil.validate (UserFormController) | Via service-laag; controller-niveau gedeeltelijk (gap 8.3-5) | 🟡 MEDIUM | Gebruikersbeheer inclusief rollen/privileges; admin-acties niet gelogd (gap 8.15-7/8) |
| `admin/users/changePassword.form` | GET/POST | `isAuthenticated()` | OpenmrsUtil.validatePassword (ChangePasswordFormController.java:~92) | Geen expliciete privilege; ChangePasswordFormController.java:37 | 🟡 MEDIUM | Wachtwoordwijziging; alleen voor current user maar geen logging (gap 8.15-5) |
| `options.form` | GET/POST | `isAuthenticated()` | Validatie aanwezig (EmailValidator, ValidateUtil) | `Context.isAuthenticated()` | 🟢 LOW | Eigen profiel/wachtwoord; redelijke validatie; geen logging op wachtwoordwijziging |
| `admin/patients/patient.form` | GET/POST | Via service-laag (PatientService) | ShortPatientFormValidator / PatientValidator | Implicieet via service | 🟡 MEDIUM | Patiëntidentificatiegegevens (art. 9 AVG); geen expliciete controller-privilege-check; niet gelogd |
| `admin/patients/mergePatients.form` | POST | Via service-laag | Gedeeltelijk | Via service | 🟡 MEDIUM | Samenvoegen patiënten — onomkeerbare actie op gevoelige data; geen audit-log |
| `admin/observations/obs.form` | GET/POST | Via service-laag | Gedeeltelijk | Via service | 🟡 MEDIUM | Klinische observatiedata (art. 9 AVG) |
| `admin/encounters/encounter.form` | GET/POST | Via service-laag | Gedeeltelijk | Via service | 🟡 MEDIUM | Encounter-data (art. 9 AVG) |
| `patientDashboard.form` | GET | Via service-laag | n.v.t. | Via service + JSP `<openmrs:require>` tag | 🟢 LOW | Dashboard-view; JSP-laag controleert privilege; kwetsbaar als JSP wordt omzeild |
| `admin/maintenance/implementationid.form` | GET/POST | Via service-laag | Gedeeltelijk | Implicieet via admin-area | 🟡 MEDIUM | Systeem-ID configuratie; identificerende informatie |
| `forgotPassword.form` (tijdelijk wachtwoord) | POST | Geen | Rate-limit (5 pogingen/IP, 5 min) | Geen — publiek | 🟡 MEDIUM | Tijdelijk wachtwoord zonder vervaldatum; wachtwoordherstel niet gelogd (gap 8.15-6) |
| `complexObsServlet` | GET | `GET_OBS` | Geen (obsId: `Integer.valueOf()` zonder try-catch) | `Context.hasPrivilege(GET_OBS)` (ComplexObsServlet.java:55) | 🟡 MEDIUM | Content-Type uit DB zonder re-validatie; geen try-catch op Integer.valueOf — NPE/NFE mogelijk |
| `downloadDictionaryServlet` | GET | Via platform | Minimaal | Via platform | 🟢 LOW | Concept-dictionary download; geen gevoelige patiëntdata |
| `showGraphServlet` / `displayChartServlet` | GET | Via platform | Minimaal | Via platform | 🟢 LOW | Grafiekweergave; geen directe gevoelige data |
| `logout` (LogoutServlet) | GET | Geen (publiek) | n.v.t. | n.v.t. | 🟢 LOW | Sessie-invalidatie; logout niet gelogd (gap 8.15-12) |
| `admin/scheduler/scheduler.form` | GET/POST | Via service-laag | Gedeeltelijk | Via service + admin-area | 🟡 MEDIUM | Scheduler-taken beheren; uitvoerpad kan server-side acties triggeren |
| `admin/hl7/hl7Source.form` | GET/POST | Via service-laag | Gedeeltelijk | Via service | 🟡 MEDIUM | HL7-bronbeheer; integratiepunt voor externe berichten |

---

## 2. DWR-services (via `/dwr/*`)

DWR-verzoeken worden door `DwrFilter` doorgestuurd naar de `dwr-invoker`-servlet. **Geen expliciete authenticatiecheck in DwrFilter zelf** (DwrFilter.java:33-51); authenticatiestatus wordt doorgedragen via OpenMRS-sessie.

| DWR-service / methode | Autorisatiecheck | Inputvalidatie | Risico | Opmerking |
|---|---|---|---|---|
| `DWRAdministrationService.setGlobalProperty` | **Geen** in DWR-methode (DWRAdministrationService.java:36) | Geen | 🔴 HIGH | Iedere geauthenticeerde gebruiker kan globale properties wijzigen via DWR; omzeilt controller-laag; geen privilege-check in methode |
| `DWRAdministrationService.getGlobalProperty` | **Geen** | Geen | 🟡 MEDIUM | Leest systeem-configuratie inclusief potentieel gevoelige settings |
| `DWRObsService.createObs` / `createNewObs` | Via service-laag | Minimaal | 🟡 MEDIUM | Schrijft klinische observaties; afhankelijk van `EDIT_OBS` privilege in service |
| `DWRObsService.voidObservation` | Via service-laag | Minimaal | 🟡 MEDIUM | Verwijdert observaties (soft-delete) via DWR |
| `DWRPatientService.findPatients` / `findCountAndPatients` | Via service-laag | Minimaal | 🟡 MEDIUM | Zoekt op patiëntnaam/ID — patiëntdata (art. 9 AVG); geen inhoudsvalidatie op zoekterm |
| `DWRPatientService.addIdentifier` / `exitPatientFromCare` | Via service-laag | Minimaal | 🟡 MEDIUM | Schrijfacties op patiëntdata |
| `DWRPersonService.createPerson` | Via service-laag | Minimaal | 🟡 MEDIUM | Aanmaken persoon; afhankelijk van service-privilege |
| `DWRUserService.findUsers` / `getAllUsers` / `getUser` | Via service-laag | Minimaal | 🟡 MEDIUM | Gebruikerslijst; gevoelig voor verkenning van accounts |
| `DWRRelationshipService.createRelationship` / `voidRelationship` | Via service-laag | Minimaal | 🟡 MEDIUM | Relatiemutaties; klinisch relevant |
| `DWRProgramWorkflowService.changeToState` / `deletePatientProgram` | Via service-laag | Minimaal | 🟡 MEDIUM | Programma-/workflow-mutaties patiënt; klinisch relevant |
| `DWRConceptService.createConceptReferenceTerm` | Via service-laag | Minimaal | 🟢 LOW | Aanmaken referentieterm; geen directe patiëntdata |
| `DWRHL7Service.startHl7ArchiveMigration` | Via service-laag | Minimaal | 🟡 MEDIUM | Triggert migratie-job; resource-intensief; potentieel DoS |
| `DWRMessageService.sendFeedback` / `sendMessage` | Via service-laag | Minimaal | 🟡 MEDIUM | Versturen berichten; misbruikbaar voor SMTP-relay of spam |

---

## 3. Formuliervelden (externe inputs — JSP)

| Formulier / JSP | Inputtype | Bestandsupload? | Opmerking |
|---|---|---|---|
| `login.jsp` | username, password | Nee | HTTPS vereist; geen CSRF nodig (nog niet ingelogd) |
| `forgotPasswordForm.jsp` | uname (username), secretAnswer | Nee | Publiek; enumeratie-risico |
| `admin/modules/modules.jsp` | moduleFile (MultipartFile), downloadURL | **Ja (.omod)** | Geen MIME-check; downloadURL = SSRF |
| `admin/patients/shortPatientForm.jsp` | naam, geboortedatum, geslacht, identifiers, adres | Nee | Art. 9 AVG-data; XSSFilter actief op POST |
| `admin/users/userForm.jsp` | gebruikersnaam, wachtwoord, rollen, privileges | Nee | Gevoelig; geen audit-log op wijzigingen |
| `admin/users/changePasswordForm.jsp` | password, confirmPassword, secretQuestion/Answer | Nee | Wachtwoordvalidatie aanwezig; niet gelogd |
| `remotecommunication/postHl7Form.jsp` | username, password, hl7Message, source | Nee | Plaintext credentials in body; HL7-berichtinhoud niet gevalideerd |
| `admin/observations/obsForm.jsp` | conceptId, valueText, obsDate, patientId | Nee | Art. 9 AVG-data; directe invoer klinische waarden |
| `admin/maintenance/globalPropsForm.jsp` | property (key), value, description (arrays) | Nee | Systeemconfiguratie; geen validatie op waarden |
| `admin/observations/personObsForm.jsp` | person, concept, value | Nee | Art. 9 AVG-data |
| `optionsForm.jsp` | username, email, defaultLocale, notifications, wachtwoorden | Nee | Eigen profielbeheer |

---

## 4. Omgevingsvariabelen / externe inputs

| Input-kanaal | Locatie | Opmerking |
|---|---|---|
| `runtime.properties` | Via `Context.getAdministrationService()` | DB-credentials, HL7-configuratie; niet direct blootgesteld via UI |
| Global Properties (DB) | `DWRAdministrationService`, `GlobalPropertyController`, `SettingsController` | Beïnvloedbaar door elke ingelogde gebruiker (zie tabel) |
| HL7-berichten (queue) | `PostHl7Controller`, `HL7InQueueProcessorServlet` | Externe integratie; berichtinhoud niet gevalideerd in legacyui |
| Module-bestanden (.omod) | `ModuleListController` (upload + downloadURL) | Uitvoerbaar servercode; MIME ongevalideerd |
| HTTP Referer-header | `LoginController.java:83-89` | Gesanitiseerd (StringUtils.isNotBlank + `!contains("login.")`), geen volledige whitelist |

---

## 5. Risico-telling

| Risico | Aantal |
|---|---|
| 🔴 HIGH | 5 |
| 🟡 MEDIUM | 22 |
| 🟢 LOW | 7 |
| **Totaal endpoints** | **34** |

---

*Bronnen: statische inspectie legacyui-broncode v1.20.0. Bewijsregels per bevinding opgenomen. Geen PII in dit document.*
