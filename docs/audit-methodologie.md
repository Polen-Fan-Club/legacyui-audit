# Audit Methodologie

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** · Issue #57

## Aanpak

De audit combineert vier elkaar aanvullende methoden, elk met eigen bewijsvorm:

1. **Gap-analyse** tegen NEN-7510:2024-2 (8.3, 8.5, 8.15) — handmatige toetsing van de modulecode tegen de normcontrols, met per gap een verwijzing naar de betrokken code (`gap-analyse-NEN7510.md`).
2. **Attack surface mapping** (NEN 8.25) — statische broncode-inspectie van alle entry points, met risicoclassificatie en STRIDE-categorisering (`attack-surface-tabel.md`).
3. **Penetratietest** — dynamische verificatie van een selectie bevindingen tegen een draaiende instance, volgens de OWASP-fasering (verkenning, mapping, exploitatie, hertest).
4. **Software composition analysis** — Snyk SCA, CodeQL en een CycloneDX-SBOM voor de afhankelijkheden, plus coveragemeting (JaCoCo) op de security-relevante code.

De gap-analyse en attack surface mapping leveren de kandidaat-bevindingen; de pentest verifieert er een deel dynamisch; SCA dekt het supply-chain-spoor. Samen geven ze zowel statisch als dynamisch bewijs.

## Bewijsketen

Het dragende principe is herleidbaarheid: elke bevinding doorloopt de keten **bron → bevinding → besluit → fix → hertest → NEN-control**, en elke schakel verwijst naar een verifieerbaar artefact (commit, PR, issue of bestand in de repo). De traceability matrix (#54) maakt die keten per bevinding expliciet. Dit is bewust gekozen zodat elke conclusie in het rapport tot op de broncode of de testoutput navolgbaar is, en niet op autoriteit hoeft te worden aangenomen.

Een tweede werkprincipe: claims worden tegen de bron geverifieerd, niet uit het geheugen overgenomen. Dat ving onder meer een discrepantie tussen de gedocumenteerde en de werkelijke staat van de audit-logging (INFO versus WARN), die daarop is rechtgezet — vastgelegd in plaats van weggewerkt.

## Risicoweging: twee instrumenten

Ernst en besluit zijn bewust gescheiden. **CVSS v3.1** (FIRST-calculator) geeft de technische ernst per kwetsbaarheid, los van context. Het **besluit** — oplossen, accepteren of escaleren — volgt de **risicocriteria** (`risicocriteria.md`): een 5×5 kans×impact-schaal met grenswaarden, waarbij voor patiëntveiligheid en patiëntdata-vertrouwelijkheid een score ≥10 al onacceptabel is (tegen ≥15 voor overige categorieën).

De twee mogen niet worden verward: een bevinding met een gematigde CVSS-score kan via de aangescherpte grenswaarde toch onacceptabel zijn, en het ontbreken van auditability (bevinding #3) wordt door CVSS structureel onderschat omdat daar geen metriek voor bestaat. De weging loopt in zulke gevallen via de norm en de risicocriteria, niet via de score.

## Reikwijdte en beperkingen van de tooling

De geautomatiseerde tooling breekt de build niet automatisch op findings; een teamlid triagëert elke finding (Fix/Accept/False positive/Defer) met verplichte, herleidbare onderbouwing (`false-positive-beleid.md`). CodeQL draaide met de default-suite, niet `security-extended` — de handmatige gap-analyse vond bevindingen die de default-suite niet markeerde, wat bevestigt dat statische analyse en handmatige inspectie complementair zijn. GitHub Advanced Security is niet beschikbaar op de private repository; de presentatie van CodeQL-resultaten loopt daarom via een CI-artifact in plaats van de Security-tab (comply-or-explain, zie #59). De analyse zelf draait volledig.
