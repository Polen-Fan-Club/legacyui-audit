# Verantwoording AI-gebruik

**Module:** ATIx IN-B2.4 Softwarearchitectuur & -kwaliteit 2025-26 P4
**Project:** Security- en onderhoudbaarheidsaudit OpenMRS Legacy UI v1.20.0
**Auteur:** Serino Mulders (2229067) · **Team:** Groep 19

---

## Waarvoor AI is gebruikt

AI-tooling (Claude, in twee rollen) is gedurende dit project ingezet als hulpmiddel, niet als vervanging van eigen analyse. Concreet:

- **Claude Code** als uitvoerder: mechanisch werk in de repository — code genereren volgens een vooraf bepaald ontwerp, bestanden bewerken, het auditrapport vullen vanuit de bronmarkdowns, en triage-onderzoek (Dependabot-PR's, Snyk-output). Werkt stapsgewijs en stopt per stap voor review.
- **Claude (browser)** als sparringpartner: bij het structureren van documenten, het formuleren van analyses, en het tegen het licht houden van keuzes en bevindingen.

De bevindingen, de risico-inschattingen, de ontwerpkeuzes en de besluiten zijn van mij. AI heeft geholpen ze sneller op te schrijven en te controleren; het oordeel erachter is mijn eigen.

## Werkwijze: verifiëren, niet vertrouwen

Het leidende principe was dat geen enkele AI-output ongecontroleerd in een deliverable terechtkomt. Elke claim is geverifieerd tegen de werkelijke bron — broncode, CI-artefacten, workflowbestanden — voordat die is overgenomen. Dit was geen formaliteit: het ving herhaaldelijk fouten.

Voorbeelden waar verificatie een AI-fout corrigeerde:

- **Snyk-severityverdeling.** Een eerste telling gaf de verkeerde aantallen (entries met duplicaten over dependency-paden i.p.v. unieke kwetsbaarheids-IDs). Pas door het Snyk-artefact zelf te ontleden bleek de juiste verdeling: 128 unieke kwetsbaarheden, 6 critical / 60 high / 51 medium / 11 low over 304 paden.
- **SBOM-aanwezigheid.** Een conclusie dat er geen SBOM-stap in de CI zat bleek onjuist; controle van het workflowbestand toonde een werkende CycloneDX-stap.
- **Een gerapporteerde testfout** die niet bestond, en een bijna-regressie tussen twee fixes — beide gevonden door de daadwerkelijke testuitvoer en code te controleren in plaats van de samenvatting te geloven.

Deze correcties zijn de reden dat verificatie de kern van de werkwijze werd, niet een sluitpost.

## Grenzen die zijn bewaakt

- **Geen overclaiming.** Getallen die niet tegen een bron geverifieerd konden worden, zijn als "te verifiëren" gemarkeerd in plaats van hard neergezet (bijvoorbeeld het SBOM-componentenaantal).
- **Inferentie expliciet gescheiden van feit.** Waar een conclusie een aanname was en geen geverifieerd gegeven, is dat benoemd.
- **Eigen stem.** De teksten zijn waar nodig herschreven naar leesbare, niet-AI-achtige formuleringen; ronkende of herhalende passages zijn verwijderd. De inhoud moet als eigen werk verdedigbaar zijn.

## Reflectie

De grootste meerwaarde van AI in dit project zat in snelheid en in het als klankbord controleren van redeneringen — niet in het bedenken van de inhoud. De grootste valkuil was de plausibiliteit van AI-output: een samenvatting klinkt overtuigend, ook als die feitelijk onjuist is. De discipline om elke claim tegen de werkelijke bron te toetsen was daarom geen extra stap maar de voorwaarde om de tooling verantwoord te kunnen gebruiken. Wat ik niet zelf kon nalopen of verdedigen, is niet in een deliverable terechtgekomen.
