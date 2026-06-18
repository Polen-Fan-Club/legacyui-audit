# Attack Surface Samenvatting — NEN-7510:2024-2 maatregel 8.25

**Module:** OpenMRS Legacy UI v1.20.0  
**Datum:** 2026-06-09  
**Gebaseerd op:** statische broncode-inspectie (controllers, servlets, DWR-config, JSP-formulieren)

---

## 1. Totaaloverzicht

| Categorie | Aantal |
|---|---|
| Geïdentificeerde entry points (endpoints + DWR-methoden) | 34 |
| 🔴 HIGH | 5 |
| 🟡 MEDIUM | 22 |
| 🟢 LOW | 7 |
| Formulieren met externe invoer (JSP) | 11 |
| Bestandsupload-kanalen | 2 (.omod upload + downloadURL SSRF) |
| HL7-invoerkanalen | 2 (postHl7.form, HL7InQueueProcessorServlet) |
| DWR-services blootgesteld | 15 (config.xml:69-238) |

---

## 2. Top-5 meest risicovolle entry points

### 1. `admin/modules/manage/fetchModuleDetails.form` — SSRF (🔴 HIGH)
**Bestand:** `ModuleManagementController.java:50-53`  
**Onderbouwing:** De parameter `url` wordt zonder enige validatie als `new java.net.URL(url).openStream()` verwerkt. Een geauthenticeerde aanvaller kan willekeurige interne hosts bereiken (bijv. cloud-metadata-endpoints zoals `169.254.169.254`, interne API's) of externe systemen benaderen. Er is geen expliciete privilege-check in de methode zelf; de enige bescherming is de impliciete admin-area-toegang via de Spring-URL-handler. **STRIDE: Information Disclosure + Tampering.**

### 2. `remotecommunication/postHl7.form` — Plaintext credentials in HTTP-body + ongecheckte HL7-invoer (🔴 HIGH)
**Bestand:** `PostHl7Controller.java:38-45`  
**Onderbouwing:** Dit endpoint accepteert gebruikersnaam en wachtwoord als gewone POST-parameters (`username`, `password`), buiten het normale sessiemodel om. Dit maakt credentials kwetsbaar voor logging-interceptie, HTTP-proxies, en man-in-the-middle-aanvallen. Na authenticatie wordt het HL7-bericht zonder inhoudsvalidatie in de verwerkingswachtrij geplaatst. Geen privilege-check na authenticatie; iedere geldig gecrediteerde gebruiker kan HL7 injecteren. **STRIDE: Spoofing + Tampering.**

### 3. `DWRAdministrationService.setGlobalProperty` — Schrijftoegang systeemconfig zonder privilege-check (🔴 HIGH)
**Bestand:** `DWRAdministrationService.java:36-38` + `config.xml:199-203`  
**Onderbouwing:** De DWR-methode `setGlobalProperty` bevat geen privilege-check. Via DWR-verzoeken kan iedere geauthenticeerde gebruiker globale properties overschrijven, inclusief beveiligingsinstellingen zoals de brute-force-drempel (`security.numberOfAllowedFailedAttempts`), wachtwoordbeleid, en log-niveaus. Gekoppeld aan `getGlobalProperty` is ook uitlezing van gevoelige systeemconfiguratie mogelijk. **STRIDE: Elevation of Privilege + Tampering.**

### 4. `admin/maintenance/globalProps.form` — Systeemconfiguratie zonder privilege-check (🔴 HIGH)
**Bestand:** `GlobalPropertyController.java:70`  
**Onderbouwing:** De controller-methode controleert uitsluitend `Context.isAuthenticated()` — geen specifiek privilege zoals `MANAGE_GLOBAL_PROPERTIES`. Iedere ingelogde gebruiker die het formulier kan bereiken, kan alle global properties overschrijven of verwijderen (inclusief `as.purgeGlobalProperties`). Via `OpenmrsUtil.applyLogLevels()` en `OpenmrsUtil.setupLogAppenders()` (regels 118-121) kunnen logconfiguraties worden gemanipuleerd. **STRIDE: Elevation of Privilege + Tampering.**

### 5. `admin/modules/module.list` (action=upload) — Module-upload RCE-risico (🔴 HIGH)
**Bestand:** `ModuleListController.java:78-151`  
**Onderbouwing:** Het uploaden van een `.omod`-bestand resulteert in het laden en uitvoeren van de module-code met volledige serverprivileges. Er is een privilege-check (`MANAGE_MODULES`), maar de uploadfunctie bevat ook een `downloadURL`-parameter (regels 112-120) die `ModuleUtil.getURLStream(url)` aanroept — een tweede SSRF-vector. Geen MIME- of inhoudsvalidatie op het geüploade bestand. Als `MANAGE_MODULES` te breed is uitgegeven, leidt dit direct tot Remote Code Execution. **STRIDE: Elevation of Privilege + Tampering.**

---

## 3. Geïdentificeerde trust-aannames

| Aanname | Locatie | Risico |
|---|---|---|
| Authenticatie is al gedaan door openmrs-core; DwrFilter checkt dit niet zelf | `DwrFilter.java:33-51` | Sessie-doorsluizing-aanval omzeilt alle DWR-privilege-checks |
| Controllers vertrouwen op JSP-laag (`<openmrs:require>`) als enige autorisatiegrens | `AdminPageFilter.java:29-31`, `GlobalPropertyController.java:70`, `SystemInformationController.java:40` | Omzeiling van de JSP-laag (bijv. directe URL-aanroep) geeft ongeautoriseerde toegang |
| GET-parameters zijn veilig; XSSFilter filtert alleen POST | `XSSFilter.java:31` | Reflected XSS via GET-parameters niet gefilterd door de applicatielaag |
| `downloadURL` biedt een legitieme bron | `ModuleListController.java:112-120`, `ModuleManagementController.java:50-53` | SSRF; aanvaller kan interne infrastructuur bereiken |
| HL7-berichtinhoud is welgevormd voordat het de wachtrij ingaat | `PostHl7Controller.java:46-57` | Kwaadaardige HL7-payloads kunnen parsers of downstream verwerkers destabiliseren |
| Service-laag handhaaft autorisatie als controller-laag tekortschiet | Meerdere controllers | Dit is gedeeltelijk correct, maar niet voor alle operaties geverifieerd (DWR-laag) |

---

## 4. Gaps t.o.v. NEN-7510 maatregel 8.25 (Attack Surface Mapping)

NEN-7510:2024-2 maatregel 8.25 vereist dat alle externe interfaces en invoerkanalen worden geïdentificeerd, gedocumenteerd en beveiligd. De volgende gaps zijn gevonden:

| Entry point | Inputvalidatie aanwezig? | Autorisatiecheck aanwezig? | Gap |
|---|---|---|---|
| `fetchModuleDetails.form` (SSRF) | ❌ Nee | ❌ Geen privilege-check in methode | Beide lagen ontbreken |
| `DWRAdministrationService.setGlobalProperty` | ❌ Nee | ❌ Geen privilege-check in DWR-methode | Schrijftoegang zonder autorisatie |
| `globalProps.form` | ❌ Nee (geen key/value-validatie) | ⚠️ Alleen `isAuthenticated()` | Privilege-check ontbreekt |
| `serverLog.form` | n.v.t. | ❌ Geen controller-niveau check | Logs leesbaar voor alle ingelogde gebruikers |
| `systemInfo.htm` | n.v.t. | ❌ Geen controller-niveau check | Systeeminfo voor alle ingelogde gebruikers |
| `postHl7.form` | ❌ Geen HL7-inhoud-validatie | ⚠️ Credentials in POST-body | Architecturele afwijking van sessiemodel |
| `rebuildSearchIndex.htm` | n.v.t. | ⚠️ Alleen `isAuthenticated()` | DoS-vector voor elke ingelogde gebruiker |
| DWR-laag algemeen | ❌ Minimaal | ⚠️ Gedelegeerd aan service-laag | Niet geverifieerd in deze laag; expliciet buiten scope gap-analyse v1.1 |
| XSSFilter (GET) | ❌ Filtert GET niet | n.v.t. | Reflected XSS via GET-parameters onbeschermd |

---

## 5. Koppeling aan bestaand threat model (STRIDE)

De volgende threats uit het bestaande threat model worden **bevestigd of uitgebreid** door de attack surface mapping:

| STRIDE-categorie | Threat (bestaand) | Bevestigd / Uitgebreid door |
|---|---|---|
| **S** — Spoofing | Gebruikersenumeratie via wachtwoordherstel (gap 8.5-6) | Bevestigd: `ForgotPasswordFormController.java:131-145`; response-divergentie voor bestaande/niet-bestaande users |
| **S** — Spoofing | Sessie-IP-binding functioneel uitgeschakeld (gap 8.3-6) | Bevestigd: aanvaller met gestolen sessietoken kan van IP wisselen zonder detectie |
| **T** — Tampering | Manipulatie global properties (nieuw) | **Uitgebreid:** zowel via `globalProps.form` als `DWRAdministrationService.setGlobalProperty` zonder adequate privilege-check |
| **T** — Tampering | SSRF via module-download (nieuw) | **Nieuw entry point:** `fetchModuleDetails.form` + `downloadURL` in module-upload |
| **T** — Tampering | HL7-injectie via `postHl7.form` (uitgebreid) | **Uitgebreid:** credentials in POST-body, geen privilege-check na auth |
| **R** — Repudiation | Ontbrekende audit-logging (gaps 8.15-4/5/6/7/8/11/12) | Bevestigd en uitgebreid: ook DWR-schrijfacties (obs, programma's, relaties) niet gelogd |
| **I** — Information Disclosure | Serverlog blootgesteld (nieuw) | **Nieuw:** `serverLog.form` en `systemInfo.htm` geen controller-niveau privilege-check |
| **I** — Information Disclosure | DWRAdministrationService.getGlobalProperty (nieuw) | **Nieuw:** systeemconfiguratie leesbaar voor alle ingelogde gebruikers |
| **D** — Denial of Service | Brute-force drempel te hoog (gap 8.5-5) | Bevestigd: default 100 in-memory; `rebuildSearchIndex` nieuw DoS-vector |
| **E** — Elevation of Privilege | Controller-level privilege-checks ontbreken (gap 8.3-4/5) | Bevestigd en **uitgebreid**: module-upload RCE-scenario als `MANAGE_MODULES` te breed; DWR `setGlobalProperty` omzeilt controller-laag |

---

*Primaire deliverable: `attack-surface-tabel.md`. Dit document is de onderbouwing. Gebaseerd op statische broncode-inspectie; dynamische exploiteerbaarheid niet geverifieerd.*
