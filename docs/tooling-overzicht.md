# Tooling-overzicht en -verantwoording

**Module:** ATIx IN-B2.4 Softwarearchitectuur & -kwaliteit 2025-26 P4
**Project:** Audit OpenMRS Legacy UI v1.20.0 · **Team:** Groep 19

Dit overzicht beschrijft per tool de functie en de reden van de keuze, en verwijst naar het document waar de tool inhoudelijk wordt behandeld. Het is een naslag- en verantwoordingsdocument, geen herhaling van die behandeling.

---

## Security-spoor

| Tool | Functie | Waarom gekozen | Uitgebreid in |
|---|---|---|---|
| **CodeQL** | Statische analyse (SAST) op de volledige module; produceert SARIF | GitHub-native, draait in CI zonder extra infrastructuur; dekt een brede set security-queries | `audit-methodologie.md`, `mini-complianceverslag.md` M2 |
| **Snyk** | Software Composition Analysis (SCA): scant afhankelijkheden tegen de NVD/CVE-database | Levert per-CVE severity (CVSS) en een machine-leesbaar rapport; geschikt voor het update-advies | `sbom-supply-chain.md`, `false-positive-beleid.md` |
| **CycloneDX (Maven-plugin)** | Genereert bij elke build een SBOM in CycloneDX 1.5 | Open standaard, machine-leesbaar; vormt de basis voor het kwetsbaarheidsonderzoek en sluit aan op de CRA-eis van componenttransparantie | `sbom-supply-chain.md`, `mini-complianceverslag.md` M1 |
| **Dependabot** | Alerts + version-update-PR's op Maven-deps en GitHub Actions | GitHub-native; signaleert kwetsbare en verouderde afhankelijkheden automatisch | `dependabot-triage.md`, `mini-complianceverslag.md` M3 |
| **Branch protection** | PR-only merges naar `main`, CI-gate | Borgt integriteit en herleidbaarheid van de auditbaseline | `mini-complianceverslag.md` M4 |

De Dependabot-PR's zijn niet blind gemerged maar per stuk getrieerd; de afweging staat in `dependabot-triage.md`.

## Onderhoudbaarheidsspoor

| Tool | Functie | Waarom gekozen | Uitgebreid in |
|---|---|---|---|
| **JaCoCo** | Code coverage-meting in CI | Standaard voor JVM-projecten; levert de meetbare onderbouwing voor de testopzet en de validatie (vóór/na) | `testopzet.md`, `validatie.md` |
| **SonarCloud** | Quality gate / statische kwaliteitsanalyse | Gekozen als externe kwaliteitspoort bovenop de eigen metingen; "Clean as You Code"-model op nieuwe code | dit document (zie hieronder) |

### SonarCloud — verantwoording

SonarCloud is gekoppeld aan de repository als externe quality gate voor het onderhoudbaarheidsspoor. De gate (gericht op nieuwe code, "Clean as You Code") slaagde op de PoC-wijzigingen. Twee beperkingen zijn vastgesteld en bewust geaccepteerd:

1. **Coverage-koppeling.** SonarCloud las de JaCoCo-coverage niet in (de XML-koppeling kwam niet rond). Dit is niet kritiek: de coverage-onderbouwing in `validatie.md` (C6) gebruikt de JaCoCo-rapporten rechtstreeks, niet via SonarCloud. SonarCloud is gebruikt voor de kwaliteitsregels, niet als coverage-bron.
2. **LOC-grens.** Het gratis plan kent een limiet op het aantal geanalyseerde regels; bij een module van deze omvang was scoping nodig. De analyse is uiteindelijk op de relevante omvang uitgevoerd.

SonarCloud was een hulpmiddel naast de eigen metingen (JaCoCo, handmatige McCabe-telling), niet de enige bron voor het onderhoudbaarheidsoordeel.

## Build

| Tool | Functie | Opmerking |
|---|---|---|
| **Maven (JDK 8 Temurin)** | Build + test van de multi-module (api/omod) | Build-commando met formatter/license-skip; deze plugins herschrijven anders de bronbestanden |

## Bewust niet gebruikt

Voor het onderhoudbaarheidsspoor zijn enkele gangbare statische-analysetools overwogen maar **niet** ingezet:

- **Checkstyle, PMD, SpotBugs, Qodana** — niet toegevoegd. De combinatie CodeQL (security-statisch) + JaCoCo (coverage) + SonarCloud (kwaliteitsregels) + handmatige metriekanalyse dekte de onderhoudbaarheidsvragen af. Een vierde of vijfde statische tool zou overlappende bevindingen opleveren zonder nieuwe inzichten voor de gekozen PoC-scope. Dit is een afweging, geen weglating: bij een breder onderhoudbaarheidstraject zou bijvoorbeeld PMD (codecomplexiteit, duplicatie) een logische aanvulling zijn.

## Comply-or-explain: GitHub Advanced Security

GitHub Advanced Security (GHAS) — nodig voor de Security-tab, Code Scanning-presentatie en native Secret Scanning — is op een privé-repository een betaalde Enterprise-feature en was niet beschikbaar. De functies zijn vervangen door: CodeQL-resultaten als CI-artefact, Snyk voor SCA, en Dependabot voor kwetsbaarheidssignalering. Dit is een bewuste, gedocumenteerde keuze (zie `mini-complianceverslag.md` M2/M8 en `cicd-risico-evaluatie.md`), geen tekortkoming in de analyse.
