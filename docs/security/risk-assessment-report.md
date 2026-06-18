# Risk Assessment Report — legacyui-audit

**Module:** OpenMRS Legacy UI v1.20.0 · **Normen:** NEN-7510:2024-2, AVG · **Vertrouwelijk** (NEN-7510 5.12)

Op basis van Snyk SCA, CodeQL, SBOM, de gap-analyse NEN-7510 en de security backlog.

## 1. Samenvatting

legacyui verwerkt patiëntdossiers en toegangsbeheer — een hoog-risico zorgcomponent. Twee soorten bevindingen: kwetsbare dependencies (128 unieke vulns, 6 critical / 60 high) en code-/configuratiezwakheden tegen NEN-7510 (A.8.3 en A.8.5 gedeeltelijk, A.8.15 niet compliant). De zwaarste risico's raken authenticatie, autorisatie en ontbrekende audit-logging.

## 2. Verwerkte gevoelige gegevens

Referenties wijzen naar de verwerkende controller. Volledige classificatie: `cia-biv-analyse.md`.

| Gegeven | Referentie | Hoogste BIV | AVG |
|---|---|---|---|
| Patiëntdossier (encounters, observaties) | `PatientDashboardController`, `EncounterFormController`, `ObsFormController` | I/V = 5 | art. 9 |
| Persoons-/identificatiegegevens | `PersonFormController`, `ShortPatientFormController` | V = 5 | art. 9 |
| Gebruikers-/toegangsbeheer | `UserFormController`, `ChangePasswordFormController` | I = 5 | — |
| Relaties & programma's | `PersonRelationshipsPortletController`, `PatientProgramFormController` | V = 4 | art. 9 |
| Audit-/loggegevens | ontbreekt grotendeels (gap 8.15) | I = 4 | — |

## 3. Bevindingen & mitigaties (NEN-7510)

### 3a. Code-/configuratiebevindingen

| Finding | Ernst | NEN | Mitigatie |
|---|---|---|---|
| 8.3-6 — sessie-IP-binding dood codepad (`getLocalAddr` i.p.v. `getRemoteAddr`) | Hoog | A.8.3 | `getRemoteAddr()`; mismatch-check herstellen |
| 8.5-5 — brute-force drempel default 100, in-memory per instance | Hoog | A.8.5 | Drempel verlagen; counter persistent/gedeeld |
| 8.15-4 — mislukte logins niet gelogd | Hoog | A.8.15 | Log-call bij `ContextAuthenticationException` |
| 8.15-5 — wachtwoordwijzigingen niet gelogd | Hoog | A.8.15 | Audit-log op `ChangePasswordFormController` |
| 8.3-4/5 — geen privilege-check op controller-niveau | Medium | A.8.3 | Vangnet onder de JSP-laag |
| 8.5-6 — tijdelijk wachtwoord zonder vervaldatum + enumeratie | Medium | A.8.5 | Vervaldatum; responses uniformeren |

### 3b. Dependency-bevindingen → update-advies (SBOM)

Gegroepeerd per pakket; per losse CVE is met 128 vulns niet werkbaar. Veel zitten transitief via `openmrs-api` (provided scope) — exploiteerbaarheid te verifiëren vóór patchen. NEN: A.8.8, A.8.29.

| Pakket | Vulns | Hoogste | Upgrade? | Advies |
|---|---|---|---|---|
| `xstream` | 36 | high (RCE) | Ja | Upgraden; deserialisatie-RCE reëel exploiteerbaar |
| `commons-fileupload` | 7 | critical | Ja | Upgraden — prioriteit |
| `log4j` 1.x | 7 | critical | Nee (EOL) | Migreren naar log4j 2 / reload4j; geen in-place upgrade |
| `spring-web`/`-webmvc`/`-core` | 25 | high (auth bypass, DoS) | Ja | Bumpen; build-impact testen (CI breekt op sommige bumps) |
| `mysql-connector-java` | 8 | high | Ja | Upgraden |
| `xercesImpl` | 7 | high (DoS) | Ja | Upgraden |

## 4. Kostenraming

Inschatting in dagdelen (≈ 4 uur), één developer, geen licentiekosten, exclusief hertest-PoC. Tarief-aanname € 60/uur.

| Cluster | Werk | Dagdelen | € |
|---|---|---|---|
| Audit-logging (8.15-4/5) | Logging-laag via framework | 4 | 960 |
| Authenticatie/autorisatie (8.3-6, 8.5-5, 8.3-4/5) | IP-binding, brute-force, controller-checks | 4 | 960 |
| Wachtwoordherstel (8.5-6) | Vervaldatum + enumeratie | 1 | 240 |
| Critical/high dependencies | Triage + upgrade + regressietest | 5 | 1.200 |
| log4j-migratie | 1.x → 2/reload4j | 3 | 720 |
| Pipeline-hardening | scan-gate, SHA-pinning, secret-scoping | 2 | 480 |
| **Totaal** | | **19 (~9,5 dag)** | **± 4.560** |

log4j-migratie en Spring-upgrade zijn de grootste onzekerheden; build-impact kan de inschatting verhogen.

## 5. Aanbevolen volgorde

Per `security-backlog.md`: eerst P1 met patiëntdata-impact (audit-logging, authenticatie/autorisatie, critical dependencies), dan P2. log4j-migratie is critical maar arbeidsintensief — inplannen, niet uitstellen.

---

*Bronnen: `snyk-sca-report.json`, `gap-analyse-NEN7510.md`, `security-backlog.md`, `cia-biv-analyse.md`. Dependency-clusters vereisen triage tegen de code vóór patchen. Issue #33.*
