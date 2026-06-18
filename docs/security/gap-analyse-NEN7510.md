# Gap-analyse — NEN-7510:2024-2
**Module:** Legacy UI Module v1.20.0 (OpenMRS 2.0.0)  
**Platform:** Java 1.8  
**Controls geanalyseerd:** A.8.3 (Toegangsbeperking), A.8.5 (Beveiligde authenticatie), A.8.15 (Logging)  
**Datum:** juni 2026 · **Versie:** v1.1 (na codeverificatie — zie changelog)

---

## Aanpak

Statische code-inspectie van de broncode. Per control: status **Aanwezig**, **Gedeeltelijk** of **Afwezig**, met bestandsnaam + regelnummer als bewijs. Prioriteit op basis van risico voor de zorgomgeving.

### Scope-beperkingen

- De **DWR-laag** (`config.xml` regels 33–240: o.a. `DWRPatientService`, `DWRUserService`, `DWRAdministrationService`, `DWRObsService`) verwerkt patiëntzoekacties, observatie-mutaties en globale-property-wijzigingen — auditrelevant, maar **niet diepgaand beoordeeld** in deze analyse. Bewust buiten scope.
- Het CSRF-mechanisme (OWASP CSRFGuard) wordt op platform-niveau in openmrs-core geconfigureerd; deze module includeert alleen het client-script. Zie 8.5-3.

---

## Control A.8.3 — Toegangsbeperking

**Norm:** Toegang tot informatie en systeemfuncties wordt beperkt op basis van het toegangsbeheerbeleid.

| # | Bevinding | Status | Bewijs | Prioriteit |
|---|-----------|--------|--------|-----------|
| 8.3-1 | Privilege-gebaseerde autorisatie via JSP-tags (`<openmrs:require>`) — bij onbevoegde toegang omleiding + WARN-log | **Aanwezig** | `RequireTag.java:82-184` | — |
| 8.3-2 | 14 dashboard-view-permissions via `@AddOnStartup` (alle `DASHBOARD_*` en `PATIENT_OVERVIEW_*`), automatisch aangemaakt bij eerste start. View-permissies voor het patient dashboard, geen system/admin-privileges (die komen uit `org.openmrs.util.PrivilegeConstants` in openmrs-core). | **Aanwezig** | `ApplicationPrivilegeConstants.java:26-66` | — |
| 8.3-3 | Gebruikersisolatie via sessie-IP-koppeling; mismatch genereert WARN-log | **Aanwezig** | `RequireTag.java:88-89, 154` | — |
| 8.3-4 | `AdminPageFilter` stuurt door zonder zelf rechten te valideren; vertrouwt volledig op downstream JSP-laag | **Gedeeltelijk** | `AdminPageFilter.java:29-31` | Medium |
| 8.3-5 | Controllers controleren alleen of gebruiker is ingelogd, niet welke privileges hij heeft | **Gedeeltelijk** | `UserFormController.java:165` | Medium |
| 8.3-6 | IP-binding uit 8.3-3 is functioneel een **dood codepad**: `RequireTag.java:88` gebruikt `request.getLocalAddr()` (server-IP) i.p.v. `getRemoteAddr()` (client-IP). `LoginServlet.java:176` zet diezelfde server-IP in de sessie. Beide waarden zijn daardoor altijd gelijk; de mismatch-check op regel 154 triggert in productie nooit. `LoginServlet.java:73` gebruikt vóór de brute-force-teller wél `getRemoteAddr()` — inconsistentie binnen hetzelfde bestand. | **Afwezig** | `RequireTag.java:88`, `LoginServlet.java:176` | **Hoog** |

**Architectuurrisico's:** (1) zowel `AdminPageFilter` als controllers vertrouwen volledig op de JSP-laag; ontbreekt een tag of wordt die omzeild, dan is er geen vangnet op controller-niveau (8.3-4, 8.3-5). (2) De sessie-IP-binding (8.3-3) is door verkeerd API-gebruik functioneel uitgeschakeld (8.3-6) — formeel aanwezig, feitelijk geen bescherming tegen sessie-hijacking via IP-wissel.

**Eindoordeel A.8.3: Gedeeltelijk compliant**

---

## Control A.8.5 — Beveiligde authenticatie

**Norm:** Systemen passen veilige authenticatieprocedures toe om toegang te beheersen.

| # | Bevinding | Status | Bewijs | Prioriteit |
|---|-----------|--------|--------|-----------|
| 8.5-1 | Login via `Context.authenticate()` — wachtwoord-hashing gedelegeerd aan OpenMRS Core | **Aanwezig** | `LoginServlet.java:137` | — |
| 8.5-2 | Sessievernieuwing na login (session fixation prevention) — oude sessie geïnvalideerd, nieuwe aangemaakt | **Aanwezig** | `LoginServlet.java:267-291` | — |
| 8.5-3 | CSRFGuard-script wordt door de module **geïncludeerd** in JSP-templates, maar de CSRF-filter zelf wordt **niet door deze module geregistreerd**; configuratie ligt in openmrs-core. Binnen module-scope alleen client-side inclusie. | **Aanwezig (via core)** | `headerFull.jsp:23`, `headerMinimal.jsp:23` | — |
| 8.5-4 | Verplichte wachtwoordwijziging bij eerste login afgedwongen via filter | **Aanwezig** | `ForcePasswordChangeFilter.java:51-62` | — |
| 8.5-5 | IP-gebaseerde brute-force lockout aanwezig. Drempel **default 100** (configureerbaar via `GP_ALLOWED_LOGIN_ATTEMPTS_PER_IP`) — voor een zorgomgeving te lax als standaard. Counter in-memory `HashMap` per `LoginServlet`-instantie; reset bij restart, deelt niet over meerdere app-servers. | **Gedeeltelijk** | `LoginServlet.java:58, 82-118` | **Hoog** |
| 8.5-6 | Wachtwoordherstel via geheime vraag: rate limiting aanwezig (5 pogingen/5 min), maar tijdelijk wachtwoord heeft **geen vervaldatum** en gebruikersenumeratie is mogelijk via response-divergentie (bestaande user zonder secret question → `auth.question.empty`; niet-bestaande user → fake question; bestaande user met question → echte vraag) | **Gedeeltelijk** | `ForgotPasswordFormController.java:89-183` | Medium |

**Aandachtspunten:** (1) **Hoog** — default brute-force-drempel van 100 is voor een zorgomgeving te lax (best practice 5–10); in-memory counter per servlet-instance is een ontwerprisico bij multi-server-deployments. (2) **Medium** — wachtwoordherstel laat gebruikersenumeratie toe en stelt geen vervaldatum op het tijdelijke wachtwoord.

**Eindoordeel A.8.5: Gedeeltelijk compliant**

---

## Control A.8.15 — Logging

**Norm:** Logboeken die gebruikersactiviteiten, uitzonderingen en beveiligingsgebeurtenissen registreren, worden aangemaakt, bewaard en regelmatig beoordeeld.

| # | Bevinding | Status | Bewijs | Prioriteit |
|---|-----------|--------|--------|-----------|
| 8.15-1 | Onbevoegde toegangspogingen gelogd op WARN met gebruikersnaam en doelpad | **Aanwezig** | `RequireTag.java:122` | — |
| 8.15-2 | Uitzonderingen gelogd met gebruiker-ID of melding "not authenticated" | **Aanwezig** | `authorizationHandlerInclude.jsp:14-37` | — |
| 8.15-3 | Succesvolle login gelogd op **DEBUG** — niet zichtbaar in productie; gebruikersnaam wordt niet gelogd | **Gedeeltelijk** | `LoginServlet.java:169-171` | Medium |
| 8.15-4 | **Mislukte inlogpogingen worden niet gelogd** — `ContextAuthenticationException` afgevangen, alleen vertaald naar error-attribuut, geen log-call. (Geen tegenspraak met 8.5-5: pogingen worden wél in-memory geteld voor lockout, maar niet via logging-framework geregistreerd.) | **Afwezig** | `LoginServlet.java:187-191` | **Hoog** |
| 8.15-5 | Wachtwoordwijzigingen worden niet gelogd | **Afwezig** | `ChangePasswordFormController.java:129` | **Hoog** |
| 8.15-6 | Wachtwoordherstel via vergeten-wachtwoord wordt niet gelogd | **Afwezig** | `ForgotPasswordFormController.java:171-175` | **Hoog** |
| 8.15-7 | Administratieve gebruikersacties bij succes niet gelogd: **create** (`us.createUser`), **purge** (hard delete, `purgeUser`) en **retire** (soft delete, `retireUser`). Alleen het *falen* van een purge wordt gelogd (regel 181). | **Afwezig** | `UserFormController.java:175` (purge), `:191` (retire), `:285` (create) | Hoog |
| 8.15-8 | Privilege/rol-wijzigingen worden niet gelogd | **Afwezig** | `UserFormController.java:230-248` (rol-aggregatie), `:287` (persistence) | Hoog |
| 8.15-9 | XSS-filter sanitizeert input, maar logt geen verdachte patronen | **Afwezig** | `XSSFilter.java:28-39` | Medium |
| 8.15-10 | Gemengde logging-frameworks (Apache Commons Logging + SLF4J) — inconsistente log-routing mogelijk | **Gedeeltelijk** | `LoginServlet.java:29-30`, `RedirectAfterLoginFilter.java:26-27` | Laag |
| 8.15-11 | **Impersonatie (`Context.becomeUser`) wordt niet gelogd** — een admin die in de huid van een andere gebruiker stapt, wordt enkel via een UI-bericht bevestigd; geen audit-spoor van wie wie wordt en wanneer. NEN-7510-kritisch in patiëntdossier-context. | **Afwezig** | `UserFormController.java:168` | **Hoog** |
| 8.15-12 | **Logout-events worden niet gelogd** — `LogoutServlet.doGet` roept `Context.logout()` en `httpSession.invalidate()` aan zonder log-call. | **Afwezig** | `LogoutServlet.java:37-47` | Medium |

### Ontbrekende security-events

| Ontbrekend event | Locatie |
|-----------------|---------|
| Mislukte inlogpoging (gebruikersnaam + IP + timestamp) | `LoginServlet.java:187-191` |
| Wachtwoordwijziging | `ChangePasswordFormController.java:129` |
| Wachtwoordherstel (vergeten wachtwoord) | `ForgotPasswordFormController.java:171-175` |
| Gebruiker aanmaken | `UserFormController.java:285` |
| Gebruiker hard delete (purge) | `UserFormController.java:175` |
| Gebruiker soft delete (retire) | `UserFormController.java:191` |
| Privilege/rol-wijzigingen | `UserFormController.java:230-248, 287` |
| Impersonatie (`becomeUser`) | `UserFormController.java:168` |
| Logout | `LogoutServlet.java:37-47` |
| Succesvolle login (op INFO, met gebruikersnaam) | `LoginServlet.java:169` |

**Zwakste control van de drie.** Onbevoegde toegangspogingen en uitzonderingen worden gelogd, maar vrijwel alle kritische security-events die NEN-7510 voor een zorgomgeving vereist ontbreken — met name mislukte logins, wachtwoordwijzigingen, administratieve acties, impersonatie en logout. Dat maakt incident-response en forensisch onderzoek praktisch onmogelijk. Het mengen van twee logging-frameworks is een beheersrisico bij grotere deployments.

**Eindoordeel A.8.15: Niet compliant**

---

## Totaaloverzicht

| Control | Eindoordeel | Kritieke punten |
|---------|------------|-----------------|
| A.8.3 Toegangsbeperking | Gedeeltelijk compliant | Geen privilege-check op controller-niveau; IP-binding functioneel uitgeschakeld door verkeerd API-gebruik |
| A.8.5 Beveiligde authenticatie | Gedeeltelijk compliant | Brute-force drempel default te hoog en in-memory; geen vervaldatum op tijdelijk wachtwoord; gebruikersenumeratie mogelijk |
| A.8.15 Logging | Niet compliant | 8+ ontbrekende security-event categorieën (incl. mislukte logins, impersonatie, logout, admin-acties) |

---

## Aanbevolen prioriteiten (security backlog)

| Prio | Actie | Control | Locatie |
|------|-------|---------|---------|
| 1 — Hoog | Logging voor mislukte inlogpogingen (gebruikersnaam + IP + timestamp) | A.8.15 | `LoginServlet.java:187-191` |
| 2 — Hoog | Logging voor wachtwoordwijzigingen en -herstel | A.8.15 | `ChangePasswordFormController.java`, `ForgotPasswordFormController.java` |
| 3 — Hoog | Logging voor administratieve gebruikersacties (create / purge / retire) | A.8.15 | `UserFormController.java:175, 191, 285` |
| 4 — Hoog | Logging voor impersonatie (`becomeUser`) | A.8.15 | `UserFormController.java:168` |
| 5 — Hoog | IP-binding repareren: `getLocalAddr()` → `getRemoteAddr()` op beide plekken (of feature verwijderen) | A.8.3 | `RequireTag.java:88`, `LoginServlet.java:176` |
| 6 — Hoog | Brute-force default verlagen van 100 naar 5–10 (en/of out-of-the-box global property setten); counter naar gedeelde store voor multi-server | A.8.5 | `LoginServlet.java:82-118` |
| 7 — Medium | Privilege-controle op controller-niveau als tweede verdedigingslinie | A.8.3 | `UserFormController.java`, `AdminPageFilter.java` |
| 8 — Medium | Vervaldatum voor tijdelijk wachtwoord + enumeratie-vector dichten (uniforme response) | A.8.5 | `ForgotPasswordFormController.java` |
| 9 — Medium | Logout-events loggen (gebruikersnaam + timestamp) | A.8.15 | `LogoutServlet.java` |
| 10 — Medium | Succesvolle login op INFO loggen inclusief gebruikersnaam | A.8.15 | `LoginServlet.java:169` |
| 11 — Laag | Logging-frameworks consolideren naar één (SLF4J) | A.8.15 | Projectbreed |

---

## Changelog

**v1.1 — 2026-06-02 (revisie na codeverificatie)**

*Gewijzigd:*
- **8.3-2**: `@AddOnStartup`-privileges gecorrigeerd 8 → 14, duiding aangescherpt (dashboard-view-permissions, geen system/admin-privileges). Geverifieerd in `ApplicationPrivilegeConstants.java:26-66`.
- **8.5-3 (CSRF)**: herschreven. Oud bewijs (`WebComponentRegistrar.java:44`) is een `excludeURL`-init-parameter op `ForcePasswordChangeFilter` — geen CSRF-filterregistratie. CSRF-config ligt in openmrs-core; module includeert alleen het script via `headerFull.jsp:23` en `headerMinimal.jsp:23`. Status Aanwezig → "Aanwezig (via core)".
- **8.5-5**: gecorrigeerd dat drempel van 100 een **default** is, configureerbaar via `GP_ALLOWED_LOGIN_ATTEMPTS_PER_IP`. Teller is in-memory (`HashMap` per servlet-instance).
- **8.15-7**: citaties gepreciseerd — onderscheid purge (175), retire (191), create (285); oorspronkelijke 2-regel-citatie miste de createUser-locatie.

*Verwijderd:*
- **8.5-7 (hardcoded credentials in docker-compose.yml)**: gescrapt. Geen baseline-bevinding — `docker-compose.yml` is een sprint-1-artefact, geen onderdeel van de te auditen legacyui-baseline. Bovendien al opgelost (secrets naar `.env`, commit `10f9a4a`). Eindoordeel A.8.5 en totaaloverzicht aangepast; backlog hernummerd.

*Toegevoegd:*
- **8.3-6**: IP-binding functioneel dood codepad door `getLocalAddr()` in `RequireTag.java:88` én `LoginServlet.java:176`. Geverifieerd via grep; `LoginServlet:73` gebruikt vóór de brute-force-teller wél `getRemoteAddr()`.
- **8.15-11**: `Context.becomeUser` (impersonatie) op `UserFormController.java:168` niet gelogd.
- **8.15-12**: `LogoutServlet.doGet` (37–47) logt geen logout-events.
- **Scope-beperkingen**: DWR-laag uit `config.xml` niet beoordeeld; expliciet opgenomen.

---

*Statische code-inspectie. Bewijs verwijst naar bronbestanden Legacy UI Module v1.20.0. v1.1 herzien tegen de werkelijke broncode op 2026-06-02.*
