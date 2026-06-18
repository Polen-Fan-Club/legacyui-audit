# Dependabot-triage — besluit per PR

**Module:** OpenMRS Legacy UI v1.20.0 · **Repo:** `Polen-Fan-Club/legacyui-audit` · **Datum:** 2026-06-18
**Norm:** NEN-7510:2024-2 (8.8 kwetsbaarheidsbeheer) · CRA (EU 2024/2847) · **Bron:** `risk-assessment-report.md` §3b, `sbom-supply-chain.md`, `false-positive-beleid.md`

---

## 1. Aanleiding en uitgangspunt

Dependabot bood vijftien openstaande version-update-PR's aan. Deze zijn één voor één getrieerd volgens het triagebeleid (`false-positive-beleid.md`): per PR een besluit Merge / Defer / Close met onderbouwing. Het uitgangspunt is de auditregel uit `risk-assessment-report.md`: de geauditeerde baseline `baseline-legacyui-1.20.0` blijft intact vóór en tijdens de analyse. Een PR die de baseline wijzigt of de build breekt, wordt niet gemerged maar blijft open als bewijsmateriaal voor het patchadvies.

**Kernobservatie:** geen enkele openstaande PR adresseert een van de geauditeerde kwetsbaarheidsclusters uit `risk-assessment-report.md` §3b (xstream, commons-fileupload, log4j 1.x, spring-*, mysql-connector-java, xercesImpl). Die kwetsbaarheden zitten transitief via `openmrs-api` (provided scope) en worden door Dependabot niet als directe bump aangeboden. Dat log4j 1.x in geen enkele PR voorkomt bevestigt het advies: voor een EOL-component bestaat geen in-place bump; migratie blijft handwerk.

## 2. Besluit per PR

| PR | Pakket | Van→naar | Type | Besluit | Onderbouwing |
|---|---|---|---|---|---|
| #2 | github/codeql-action | 3→4 | Action | **Merged** | Pipeline-tooling, raakt de baseline niet, CI groen |
| #4 | actions/setup-java | 4→5 | Action | **Merged** | Draait op de CI-runner, niet in het module-artefact; CI groen |
| #5 | actions/checkout | 4→6 | Action | **Merged** | Pipeline-tooling, CI groen |
| #7 | actions/upload-artifact | 4→7 | Action | **Merged** | Pipeline-tooling, CI groen; SBOM/JaCoCo/SARIF-artefacten geverifieerd na merge |
| #22 | actions/setup-node | 4→6 | Action | **Merged** | Draait op de CI-runner, niet in het module-artefact; CI groen |
| #23 | jacoco-maven-plugin | 0.7.7→0.8.15 | Build-plugin | **Merged** | Coverage-tooling, niet in het artefact; build + coverage-meting lokaal geverifieerd (337 classes geanalyseerd, 399 tests groen) |
| #1 | jfree:jfreechart | 1.0.12→1.0.13 | Runtime-dep | **Deferred** | Raakt de baseline, CI niet groen, geen CVE-onderbouwing |
| #3 | maven-surefire-plugin | 2.22.1→3.5.6 | Build-plugin | **Deferred** | Buiten de baseline; mergebaar na CI re-run (de eerdere CodeQL-fail was een CI-infra-kwestie, geverifieerd groen na re-run), maar niet noodzakelijk voor de audit |
| #6 | maven-release-plugin | 2.5→3.3.1 | Build-plugin | **Deferred** | Idem #3 |
| #10 | formatter-maven-plugin | 2.10.0→2.29.0 | Build-plugin | **Deferred** | Idem #3 |
| #11 | coveralls-maven-plugin | 4.2.0→4.3.0 | Build-plugin (dev) | **Deferred** | Idem #3 |
| #13 | maven-dependency-plugin | 2.4→3.11.0 | Build-plugin | **Deferred** | Idem #3 |
| #8 | openmrsPlatformVersion | 2.0.0→2.8.6 | Platform (baseline) | **Deferred** | Wijzigt de geauditeerde baseline én breekt de build — geverifieerd bewijs voor het patchadvies (zie §3) |
| #9 | javax.servlet-api | 3.0.1→4.0.1 | Baseline-dep | **Deferred** | Breekt de build (Servlet 3→4 API-breuk), raakt de baseline |
| #14 | maven-parent-openmrs-module | 1.1.1→2.2.0 | Parent-POM | **Deferred** | Breekt de build, wijzigt de parent vóór de analyse |

Geen enkele PR is gesloten: geen ervan is aantoonbaar onjuist of overbodig.

## 3. De deferred baseline-bumps als bewijs voor het patchadvies

De drie baseline-/platform-bumps (#8, #9, #14) zijn het concrete bewijs voor een kernpunt uit `sbom-supply-chain.md`: een afhankelijkheidsupgrade op deze legacy-module is geen triviale bump maar een migratie die de build kan breken. #8 (openmrsPlatform 2.0.0→2.8.6) en #9 (javax.servlet-api 3→4, een API-breuk tussen majors) falen aantoonbaar op de build. Dit onderbouwt de aanpak uit het patchadvies — upgraden vereist een gepland migratietraject met regressietests, niet een directe merge — en de onzekerheid in de kostenraming (`risk-assessment-report.md` §4).

## 4. Onderscheid pipeline vs. baseline

De triage maakt een expliciet onderscheid dat de auditscope respecteert:

- **Pipeline-tooling** (GitHub Actions, JaCoCo-plugin) raakt de geauditeerde module-baseline niet — het draait op de CI-runner of als build-tooling en belandt niet in het gepackagede `.omod`-artefact. Deze bumps zijn veilig gemerged (pipeline-hygiëne) zonder de baseline-integriteit te schenden.
- **Baseline- en runtime-deps** raken wel de geauditeerde code en blijven deferred, conform de regel dat de baseline intact blijft tot de analyse is afgerond.

## 5. Openstaand vervolg

- **SHA-pinning (security-backlog B10).** De gemergede Action-bumps gaan van een mutabele tag naar een mutabele tag; ze pinnen de actions niet op een commit-SHA. Het pinnen op SHA is een aparte hardening-stap die in de backlog staat (B10) en losstaat van deze version-bumps.
- **Dependabot-labels.** De PR's tonen dat de in `dependabot.yml` geconfigureerde labels (`dependencies`, `github-actions`) niet in de repo bestaan, waardoor Dependabot ze niet kon toepassen. Dit is een configuratie-aandachtspunt: de labels aanmaken of uit `dependabot.yml` verwijderen zodat de configuratie klopt met de werkelijkheid.
- De deferred baseline-bumps (#1, #8, #9, #14) blijven bewust open als bewijsmateriaal.
