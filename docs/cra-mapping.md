# CRA-mapping (EU 2024/2847)

**Module:** OpenMRS Legacy UI v1.20.0 · **Vertrouwelijk** · Bijlage bij issue #61 / #59

De Cyber Resilience Act stelt eisen aan producten met digitale elementen gedurende hun levenscyclus. Deze mapping koppelt de relevante CRA-eisen aan het bewijs uit deze audit. Het is een mapping op auditniveau, geen formele conformiteitsverklaring: legacyui is een open-source component, niet een door dit team in de handel gebracht product. De mapping laat zien in hoeverre de uitgevoerde maatregelen aansluiten op de CRA-systematiek, en waar de grenzen liggen.

| CRA-eis (Annex I) | Verwachting | Status in deze audit | Bewijs |
|---|---|---|---|
| Beveiliging by design / risicogebaseerd | Beveiliging afgestemd op het risico | Gedeeltelijk — risicogebaseerde aanpak via CIA/BIV + risicocriteria; legacy-module niet by design veilig | `cia-biv-analyse.md`, `risicocriteria.md` |
| Geen bekende exploiteerbare kwetsbaarheden bij levering | Producten zonder bekende exploiteerbare gaten | Niet voldaan op baseline (bewust) — kwetsbare baseline behouden voor de analyse; patchadvies opgesteld | `sbom-supply-chain.md`, `risk-assessment-report.md` §3b |
| Beschermde toegang (authenticatie/autorisatie) | Ongeautoriseerde toegang voorkomen | Gedeeltelijk — IP-binding hersteld (#1), reset-flow gehard (#5); controller-privilege-gaten 8.3-4/5 open | `traceability-matrix.md`, `gap-analyse-NEN7510.md` |
| Vertrouwelijkheid en integriteit van data | Bescherming van verwerkte gegevens | Gedeeltelijk — information disclosure (#6) gemitigeerd; patiëntdata als kroonjuweel geclassificeerd | `traceability-matrix.md`, `cia-biv-analyse.md` |
| Logging van security-relevante gebeurtenissen | Activiteit registreerbaar en monitorbaar | Voldaan na remediatie — 10 event-types ge-audit-logd (#3), met regressietests | `traceability-matrix.md`, `pentestplan.md` §4 |
| Minimaliseren van het aanvalsoppervlak | Beperkt en gedocumenteerd aanvalsoppervlak | Gedeeltelijk — aanvalsoppervlak volledig in kaart (34 entry points); reductie is vervolgwerk | `attack-surface-samenvatting.md` |
| Kwetsbaarhedenbeheer | Identificeren, documenteren, verhelpen | Voldaan op procesniveau — SCA + getrieerde triage met onderbouwing, nu handmatig herleidbaar; `.snyk`-formalisering gepland (B13) | `false-positive-beleid.md`, `sbom-supply-chain.md` |
| SBOM | Actueel componentenoverzicht | Voldaan — CycloneDX-SBOM uit CI bij elke build | `sbom-supply-chain.md`, `ci.yml` |
| Beveiligde updates | Mechanisme voor tijdige updates | Gedeeltelijk — Dependabot version-update-PR's actief; productieve update-uitrol buiten scope | `mini-complianceverslag.md` M3 |

## Grenzen van deze mapping

Drie CRA-aspecten vallen buiten wat in deze audit aantoonbaar is, en worden eerlijk als zodanig benoemd in plaats van als gedekt aangenomen: de meldplicht voor actief uitgebuite kwetsbaarheden (procesmatig, vereist een organisatie), het onderhoud gedurende de gehele ondersteuningsperiode (legacyui is een EOL-gevoelig component — JDK 8, log4j 1.x), en formele conformiteitsbeoordeling. De CRA-systematiek is hier gebruikt als toetsingskader, niet als certificeringstraject.
