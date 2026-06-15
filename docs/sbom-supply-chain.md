# SBOM & Supply Chain Security

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 (8.8, 8.29), CRA (EU 2024/2847) · **Vertrouwelijk** · Issue #59

## SBOM

Bij elke CI-build genereert de pipeline een Software Bill of Materials in CycloneDX-formaat (1.5) als artifact `sbom-cyclonedx`, met alle third-party componenten en versies (circa 100 componenten, waaronder log4j 1.2.x). De SBOM is geen NEN-control op zichzelf, maar het beheersinstrument dat kwetsbaarheidsmanagement mogelijk maakt en de basis vormt voor de analyse hieronder (`mini-complianceverslag.md` M1).

## Kwetsbaarheden in afhankelijkheden

Snyk SCA identificeert 128 unieke kwetsbaarheden in de afhankelijkheden, waarvan 6 critical en 60 high. Per losse CVE is dat aantal niet werkbaar; de bevindingen zijn daarom per pakket geclusterd. Veel kwetsbaarheden zitten transitief via `openmrs-api` in de provided-scope, waardoor de feitelijke exploiteerbaarheid binnen deze module per cluster geverifieerd moet worden vóór patchen (`risk-assessment-report.md` §3b).

| Pakket | Vulns | Hoogste | Upgrade? | Advies |
|---|---|---|---|---|
| `xstream` | 36 | high (RCE) | Ja | Upgraden; deserialisatie-RCE reëel exploiteerbaar |
| `commons-fileupload` | 7 | critical | Ja | Upgraden — prioriteit |
| `log4j` 1.x | 7 | critical | Nee (EOL) | Migreren naar log4j 2 / reload4j; geen in-place upgrade mogelijk |
| `spring-web` / `-webmvc` / `-core` | 25 | high (auth bypass, DoS) | Ja | Bumpen; build-impact testen (CI breekt op sommige bumps) |

log4j 1.x en de Spring-upgrade zijn de grootste onzekerheden: log4j 1.x is end-of-life en vereist een migratie in plaats van een versie-bump, en sommige Spring-bumps breken de build. Beide zijn ingeschat in de security backlog, niet weggeschreven.

## Triage en onderdrukking

De kwetsbare baseline is bewust niet gepatcht: dat zou `baseline-legacyui-1.20.0` wijzigen vóór de analyse en de herleidbaarheid breken. De Dependabot-PR's (#10–14) staan daarom open als materiaal voor het patchadvies. Elke SCA-/SAST-finding krijgt een triagebesluit (Fix/Accept/False positive/Defer) met verplichte onderbouwing; onderdrukkingen lopen herleidbaar via de `.snyk`-policyfile in versiebeheer, met `reason` en `expires` per ignore (`false-positive-beleid.md`).

## GitHub Advanced Security — comply-or-explain

De `actions/dependency-review-action` en Code Scanning vereisen GitHub Advanced Security, een betaalde Enterprise-feature voor private repositories ("Contact sales") — geverifieerd op zowel een persoonlijk Pro-account als binnen een organisatie. De repository publiek maken zou GHAS gratis geven, maar is onwenselijk voor een vertrouwelijkheidsaudit op een zorgmodule. De PR-gate-functie is daarom vervangen door Dependabot security updates plus het Snyk-scanrapport; CodeQL levert resultaten als CI-artifact in plaats van via de Security-tab. Bewuste, geverifieerde keuze, geen tekortkoming in de analyse (`mini-complianceverslag.md` M2).

## CRA-relevantie

De CRA (EU 2024/2847) stelt eisen aan kwetsbaarheidsbeheer en transparantie over componenten gedurende de levenscyclus van een product met digitale elementen. De SBOM (componenttransparantie), de gestructureerde SCA-triage (kwetsbaarheidsbeheer) en het patchadvies (tijdige remediatie) sluiten daarop aan. Een expliciete, puntsgewijze CRA-mapping als appendix bestaat nog niet en is een openstaand item voor de bijlagen (#61) — hier benoemd zodat het niet als gedekt wordt aangenomen.
