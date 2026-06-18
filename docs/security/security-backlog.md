# Security backlog — legacyui-audit (Sprint 2, punt 7)

**Module:** OpenMRS Legacy UI v1.20.0 · **Prioritering:** `risicocriteria.md` (5×5, variant B) · **Bronnen:** gap-analyse v1.1, Snyk SCA-rapport, `cicd-risico-evaluatie.md`

Score = kans × impact. P1 = onacceptabel (≥15, of ≥10 bij patiëntdata), P2 = oranje (8–14), P3 = groen (≤7).

## P1 — onacceptabel

| ID | Requirement | Bron | Kans | Impact | Score |
|---|---|---|---|---|---|
| B1 | Audit-logging op auth-events: mislukte login, wachtwoordwijziging, -herstel | gap 8.15-4/5/6 | 4 | 4 | 16 |
| B2 | Audit-logging op admin-acties: create/purge/retire user, rol/privilege, impersonatie | gap 8.15-7/8/11 | 4 | 5 | 20 |
| B3 | Sessie-IP-binding herstellen: `getRemoteAddr()` i.p.v. `getLocalAddr()` | gap 8.3-6 | 3 | 4 | 12 |
| B4 | Brute-force drempel verlagen + counter persistent/gedeeld maken | gap 8.5-5 | 4 | 3 | 12 |
| B5 | Pipeline-gate die faalt op ontbrekend scan-artefact (i.p.v. `warn`) | CI-CD R1 | 4 | 4 | 16 |
| B6 | Critical/high SCA-vulns trieren + patchen: xstream, spring-*, log4j, commons-fileupload | Snyk (6 crit/60 high) | 4 | 4 | 16 |
| B7 | Privilege-check op controller-niveau (vangnet onder JSP-laag) | gap 8.3-4/5 | 3 | 4 | 12 |
| B10 | Actions pinnen op commit-SHA i.p.v. mutabele tag | CI-CD R2 | 2 | 5 | 10 |

B3, B4, B7 en B10 vallen onder variant B (patiëntdata-impact bij <15), conform de scoring van punt 5.

## P2 — verhoogd

| ID | Requirement | Bron | Kans | Impact | Score |
|---|---|---|---|---|---|
| B8 | Tijdelijk wachtwoord vervaldatum + gebruikersenumeratie wachtwoordherstel dichten | gap 8.5-6 | 3 | 3 | 9 |
| B9 | Succesvolle login op INFO loggen mét gebruikersnaam (nu DEBUG) | gap 8.15-3 | 3 | 3 | 9 |
| B11 | `SNYK_TOKEN` scopen/roteren + documenteren | CI-CD R3 | 2 | 4 | 8 |
| B12 | JDK 8 EOL: migratiepad toolchain (raakt ISO 25010-spoor) | CI-CD R4 / NEN 8.8 | 4 | 3 | 12 |
| B13 | `.snyk`-policyfile zodat triage een commit-spoor krijgt | CI-CD R5 | 3 | 3 | 9 |
| B14 | Branch protection op `main` verifiëren/herstellen na org-migratie | CI-CD R6 | 2 | 4 | 8 |
| B15 | Medium SCA-vulns trieren: mysql-connector, xercesImpl, overige | Snyk (51 medium) | 3 | 3 | 9 |

## P3 — monitoren

| ID | Requirement | Bron | Kans | Impact | Score |
|---|---|---|---|---|---|
| B16 | XSS-filter verdachte patronen laten loggen | gap 8.15-9 | 2 | 3 | 6 |
| B17 | Logout-events loggen | gap 8.15-12 | 2 | 2 | 4 |
| B18 | Logging-frameworks consolideren (Commons Logging + SLF4J) | gap 8.15-10 | 2 | 2 | 4 |
| B19 | Low SCA-vulns monitoren bij volgende scan | Snyk (11 low) | 2 | 2 | 4 |

## Noten

- B1 + B2 zijn samen één epic: een logging-laag die alle security-events via het framework registreert.
- Snyk-clusters (B6/B15/B19) vereisen triage tegen de code vóór patchen of accepteren — de meeste vulns zitten transitief via `openmrs-api` (provided scope); exploiteerbaarheid in legacyui is niet aangenomen.
- Deze backlog prioriteert; het besluit oplossen/accepteren valt in punt 8 (pentest) en punt 9 (RAR).

---

*Bronnen: `gap-analyse-NEN7510.md` v1.1, Snyk SCA-rapport (run main), `cicd-risico-evaluatie.md`. Issue #31.*
