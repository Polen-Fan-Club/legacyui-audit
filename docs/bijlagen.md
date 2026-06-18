# Bijlagen

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** · Issue #61

Index van het bewijsmateriaal. Elke bijlage is een artefact in de repository; deze lijst koppelt het auditrapport eraan. Paden zijn relatief aan `openmrs-module-legacyui/`.

## Kerndocumenten audit

| Bijlage | Inhoud | Locatie |
|---|---|---|
| A. Traceability matrix | Bewijsketen per bevinding (bron→fix→hertest→NEN) | `docs/traceability-matrix.md` |
| B. Risico-analyse & bevindingen | Risico-onderbouwing per bevinding + niet-gedaan met reden | `docs/risico-bevindingen.md` |
| C. Gap-analyse NEN-7510 | Per control: aanwezig/gedeeltelijk/afwezig, met eindoordelen | `docs/gap-analyse-NEN7510.md` |
| D. Risicocriteria | Schaal, grenswaarden en risicobereidheid (§6) | `docs/risicocriteria.md` |
| E. CIA/BIV-analyse | Kroonjuwelen en BIV-classificatie | `docs/cia-biv-analyse.md` |

## Aanvalsoppervlak & threat model

| Bijlage | Inhoud | Locatie |
|---|---|---|
| F. Attack surface samenvatting | 34 entry points, top-5 risico's, trust-aannames, STRIDE-koppeling | `docs/attack-surface-samenvatting.md` |
| G. Attack surface tabel | Volledige entry-point-tabel (primaire deliverable) | `docs/attack-surface-tabel.md` |

## Pentest

| Bijlage | Inhoud | Locatie |
|---|---|---|
| H. Pentestplan | Bevindingen, besluitkader, resultaten (incl. weerlegde T4) | `docs/pentestplan.md` |
| I. Pentest-bewijs | Bewijsdumps per test, incl. hertest na fix | `docs/pentest-bewijs/T*/` |
| J. Pentest-omgeving | Dockerfile + compose van de testopstelling | `docs/pentest-omgeving/` |

## Supply chain & CI/CD

| Bijlage | Inhoud | Locatie |
|---|---|---|
| K. SBOM & supply chain | Kwetsbaarheidsbeeld, update-advies, GHAS comply-or-explain | `docs/sbom-supply-chain.md` |
| L. SBOM-artefact (CycloneDX) | Machineleesbaar componentenoverzicht (1.5) | CI-artifact `sbom-cyclonedx` (`.github/workflows/ci.yml`) |
| M. Snyk SCA-rapport | SCA-scanresultaat | CI-artifact (`.github/workflows/snyk-sca.yml`) |
| N. CodeQL-resultaat | SAST (SARIF, als CI-artifact i.p.v. Security-tab) | CI-artifact (`.github/workflows/codeql.yml`) |
| O. JaCoCo coverage-rapport | Codedekking security-relevante klassen | CI-artifact `jacoco-coverage` |
| P. Risk assessment report | Geïntegreerd risico- en patchadvies met kostenraming | `docs/risk-assessment-report.md` |
| Q. Triagebeleid | Fix/Accept/False positive/Defer + onderdrukkingsregels | `docs/false-positive-beleid.md` |
| R. Security backlog | Geprioriteerde vervolgmaatregelen (P1/P2/P3) | `docs/security-backlog.md` |
| S. CI/CD-risico-evaluatie | Risico's in het pipelineproces | `docs/cicd-risico-evaluatie.md` |
| T. Mini-complianceverslag | Beheersmaatregelen M1–M8 (SBOM, CodeQL, Dependabot, branch protection, OTAP) | `docs/mini-complianceverslag.md` |

## Compliance-kaders

| Bijlage | Inhoud | Locatie |
|---|---|---|
| U. CRA-mapping | Koppeling CRA-eisen (EU 2024/2847) aan auditbewijs | `docs/cra-mapping.md` |

## Methodologie

| Bijlage | Inhoud | Locatie |
|---|---|---|
| V. Scope & context | Auditobject, afbakening, normkader | `docs/scope-context.md` |
| W. Audit-methodologie | Vier onderzoekssporen, herleidbaarheidsprincipe | `docs/audit-methodologie.md` |

---

*Artefacten gemarkeerd als "CI-artifact" worden bij elke pipeline-run geproduceerd en zijn via de GitHub Actions-run downloadbaar; ze staan niet als bestand in de repo omdat ze build-output zijn. Alle overige bijlagen staan onder versiebeheer in `docs/`.*
