# Mini-complianceverslag — Sprint 1

**Module:** Legacy UI Module v1.20.0 (OpenMRS 2.0.0) · **Repo:** `Polen-Fan-Club/legacyui-audit` (private)
**Normenkader:** NEN-7510:2024-2 · **Scope:** repo hardenen, CI/CD-pipeline, OTAP-inrichting
**Datum:** juni 2026 · **Auteur:** Serino · **Gerelateerd:** Gap-analyse NEN-7510 v1.1 (Maurits)

---

## 1. Doel en afbakening

Dit verslag legt vast welke maatregelen in de **CI/CD-pipeline en repo-inrichting** bijdragen aan de beheersing van de NEN-7510-controls uit de gap-analyse (A.8.3, A.8.5, A.8.15), en welk bewijs daarvoor beschikbaar is. Het is een **aanvulling op, geen herhaling van** de gap-analyse: die beoordeelt de *broncode*, dit verslag de *pipeline en repo-omgeving* eromheen.

**Onderscheid om overclaiming te vermijden** — A.8.3/8.5/8.15 gaan over het gedrag van de applicatiecode; een pipeline kan dat gedrag niet zelf leveren, wel bewaken of detecteren. Daarom per maatregel:

- **Dekt af** — realiseert (een deel van) de controleis zelf.
- **Ondersteunt** — helpt de control detecteren, bewaken of beheersen, realiseert de eis niet.
- **Procesmaatregel** — raakt geen specifieke A.8.3/8.5/8.15-eis, maar borgt integriteit en herleidbaarheid van het auditproces.

---

## 2. Overzicht pipeline-maatregelen

| # | Maatregel | Type | Status | Bewijs |
|---|-----------|------|--------|--------|
| M1 | SBOM-generatie (CycloneDX) als CI-artifact | Ondersteunt | Actief | CI-run + artifact `sbom-cyclonedx` |
| M2 | CodeQL SAST-scan in CI | Ondersteunt | Actief | Run + artifact `codeql-sarif` |
| M3 | Dependabot (alerts + version-updates) | Ondersteunt | Actief | Settings + PR's #10–14 |
| M4 | Branch protection op `main` | Procesmaatregel | Actief | Ruleset + PR-flow |
| M5 | OTAP-omgevingsscheiding (docker-compose + GitHub Environments) | Ondersteunt | Ingericht | `docker-compose.yml` + 3 Environments |
| M6 | Secrets buiten versiebeheer (`.env` / `.gitignore`) | Dekt deels af | Actief | `docker-compose.yml`, `.env.example`, commit `10f9a4a` |
| M7 | MFA op repository-accounts | Dekt deels af | Actief | Accountinstelling |

---

## 3. Maatregelen gekoppeld aan controls

**M1 — SBOM-generatie (CycloneDX).** CI genereert bij elke build een SBOM in CycloneDX-formaat als artifact, met alle third-party componenten en versies. *Ondersteunt* — geen control direct; beheersinstrument voor kwetsbaarheidsmanagement, basis voor de sprint-2-analyse. **Bewijs:** artifact `sbom-cyclonedx` (CycloneDX 1.5, 100 componenten, o.a. log4j 1.2.x). *Signaleert* verouderde componenten; verhelpen is sprint-2-werk.

**M2 — CodeQL SAST-scan.** Statische analyse draait in CI op de volledige module, produceert SARIF. *Ondersteunt* A.8.3/8.5/8.15 als **detectiemaatregel**: vindt potentiële tekortkomingen, lost ze niet op, garandeert geen volledige dekking. **Bewijs:** artifact `codeql-sarif` (SARIF 2.1.0, CodeQL 2.25.5, 592 bestanden, 76 security-queries, 0 findings default-suite).
- **0 findings ≠ veilig:** draaide met *default* suite, niet `security-extended`. Handmatige inspectie (gap-analyse) vond wél tekortkomingen (ontbrekende audit-logging, dode IP-binding) die de default-suite niet markeerde. SAST en handmatige analyse zijn complementair.
- **Security-tab niet beschikbaar:** upload vereist GitHub Advanced Security. Code Scanning voor private repos is een betaalde Enterprise-feature ("Contact sales") — geverifieerd op zowel persoonlijk Pro-account als in een organisatie; een org maken lost dit niet op. Public maken zou GHAS gratis geven, maar is onwenselijk voor een vertrouwelijkheidsaudit op een zorgmodule. Resultaten daarom als CI-artifact. Analyse draait volledig; alleen de presentatielaag wijkt af. Bewuste, geverifieerde keuze.

**M3 — Dependabot.** Geactiveerd voor alerts én version-update-PR's op Maven-deps en GitHub Actions. *Ondersteunt* — geen control direct; beheersmaatregel voor technische kwetsbaarheden. **Bewijs:** alerts + security updates aan; PR's #10–14. De openstaande PR's zijn **bewust niet gemerged** — materiaal voor het sprint-2-patchadvies; nu mergen wijzigt de baseline (`baseline-legacyui-1.20.0`) vóór de analyse. Enkele PR's falen op de license-header-check — zelf een observatie voor het patchadvies.

**M4 — Branch protection op `main`.** Actieve ruleset: directe pushes geblokkeerd, wijzigingen alleen via PR, CI moet groen zijn. *Procesmaatregel* — dekt geen A.8.3/8.5/8.15 af, borgt **integriteit en herleidbaarheid** van de audit: elke wijziging aan de baseline traceerbaar via PR. Aan een van de drie controls koppelen zou een overclaim zijn. **Bewijs:** actieve ruleset; alle sprint-1-wijzigingen via PR's met groene checks gemerged.

**M5 — OTAP-omgevingsscheiding.** `docker-compose.yml` met gescheiden dev/test/prod-profielen (elk OpenMRS + MySQL, eigen poort en volume) + drie GitHub Environments; prod heeft een protection rule (required reviewer). *Ondersteunt* A.8.3 op omgevingsniveau — oplopende toegangscontrole beperkt wie naar productie deployt; dekt de control niet af op applicatieniveau. **Bewijs:** `docker-compose.yml`; drie Environments; protection rule op prod.
- OpenMRS-images **niet geverifieerd** tegen Platform 2.0.0 — claim is inrichting, geen draaiende stack; validatie is sprint-3.
- Environments nog **niet gekoppeld aan een deploy-workflow** — protection rule wordt nog niet getriggerd. Inrichting staat; activering is open punt.

**M6 — Secrets buiten versiebeheer.** OTAP-database-wachtwoorden uit lokaal `.env` (in `.gitignore`) i.p.v. in `docker-compose.yml`; `.env.example` met placeholders staat wel in de repo. *Dekt deels af* — raakt A.8.5 (credential-beheer): voorkomt plaintext credentials in versiebeheer. **Bewijs:** `${VAR}`-verwijzingen; `.env.example`; `.gitignore`-regel; commit `10f9a4a`. Een eerdere versie bevatte plaintext dummy-wachtwoorden; verwijderd uit de huidige staat maar nog in de git-historie. Voor dummy-waarden (nooit echte secrets) is bewust geen history-rewrite gedaan. Dit is een sprint-1-artefact, geen baseline-bevinding — daarom geen onderdeel van de gap-analyse.

**M7 — MFA op repository-accounts.** MFA actief op het beherende account. *Dekt deels af* — raakt A.8.5 op het niveau van toegang tot de auditomgeving, niet applicatieniveau. **Bewijs:** accountinstelling. Borgt repository-toegang, niet de authenticatie *binnen* de OpenMRS-module; relevantie voor A.8.5 reëel maar beperkt tot de ontwikkelomgeving.

---

## 4. Matrix: control → ondersteunende maatregelen

| Control | Eindoordeel gap-analyse | Direct afgedekt door pipeline? | Ondersteunende maatregelen |
|---------|------------------------|-------------------------------|----------------------------|
| **A.8.3** Toegangsbeperking | Gedeeltelijk compliant | Nee — codegedrag | M5 (omgevingsscheiding), M2 (detectie) |
| **A.8.5** Beveiligde authenticatie | Gedeeltelijk compliant | Deels — M6, M7 (credential-/toegangsbeheer omgeving) | M2, M6, M7 |
| **A.8.15** Logging | Niet compliant | Nee — codegedrag | M2 (detectie ontbrekende patronen, beperkt) |

**Kernconclusie:** Geen pipeline-maatregel dekt de drie controls op *applicatieniveau* af — dat kan ook niet, want ze gaan over modulecodegedrag. De pipeline levert een **detectie- en beheerslaag** (SAST, SBOM, Dependabot), borgt **proces-integriteit** (branch protection) en **omgevings-/credential-hygiëne** (OTAP, secrets, MFA). Remediatie van de gap-analyse-bevindingen — met name de A.8.15-logging — is applicatiewerk voor latere sprints.

---

## 5. Bewijsregister

| Bewijs | Vindplaats | Verifieerbaar via |
|--------|-----------|-------------------|
| SBOM-artifact | Actions-run → `sbom-cyclonedx` | Download + open `*.cdx.json` |
| CodeQL-artifact | Actions-run → `codeql-sarif` | Download + open `java.sarif` |
| Dependabot actief | Settings → Advanced Security | + open PR's #10–14 |
| Branch protection | Settings → Rules → Rulesets | PR-historie op `main` |
| OTAP-inrichting | `docker-compose.yml` + Settings → Environments | repo-root + Environments-tab |
| Secrets-hygiëne | `docker-compose.yml`, `.env.example`, `.gitignore` | commit `10f9a4a` |
| MFA | Accountinstelling | Settings → Password and authentication |

---

## 6. Open punten

- **OTAP-activering:** Environments ingericht, nog niet aan deploy-workflow gekoppeld; prod-protection-rule niet getriggerd. Volstaat inrichting voor sprint 1?
- **CodeQL-diepte:** overweeg `security-extended` in sprint 2.
- **Remediatie A.8.15:** logging-tekortkomingen vragen applicatiewijzigingen (sprint 2+).
- **Git-historie secrets:** dummy-credentials nog in historie; bewuste keuze geen rewrite — bevestigen.
- **Verificatie gap-analyse-claims:** 8.3-6 (dode IP-binding) en 8.5-3 (CSRF via core) nog handmatig te bevestigen vóór definitieve oplevering.