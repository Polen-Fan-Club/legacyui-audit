# Scope & Context

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** · Issue #56

## Wat is geauditeerd

Het object van de audit is de OpenMRS Legacy UI-module versie 1.20.0 — de webgebaseerde beheer- en formulierlaag van een OpenMRS-installatie. De module levert de administratieve interface (gebruikersbeheer, systeemconfiguratie, patiënt- en encounterformulieren) en de bijbehorende servlets, Spring-controllers en DWR-services. De broncode is vastgezet op de tag `baseline-legacyui-1.20.0`, zodat de gehele analyse herleidbaar is naar één onveranderlijke uitgangsstaat.

De module verwerkt patiëntdossiers en regelt toegangsbeheer en is daarmee een hoog-risico component in een zorgsysteem. Dat bepaalt het risicokader: bevindingen die patiëntveiligheid of de vertrouwelijkheid van patiëntdata raken wegen zwaarder (zie `risicocriteria.md`).

## Afbakening

Binnen scope: de modulecode van legacyui 1.20.0 — controllers, servlets, taglibs, DWR-configuratie en JSP-formulieren — plus de afhankelijkheden zoals die in de SBOM van deze versie voorkomen.

Buiten scope, met motivatie:

- **De OpenMRS-core en het platform.** De module draait op platform ≥2.0.0; kwetsbaarheden in de core zelf zijn niet beoordeeld, behalve waar de module er aantoonbaar op leunt (bijvoorbeeld de service-laag-autorisatie die bij T4 is geverifieerd).
- **De referenceapplication-SPA (O3).** De moderne single-page front-end is een aparte module en valt buiten deze audit. De pentest is daarom uitgevoerd tegen de directe backend (`http://localhost:8080/openmrs`), niet via de O3-gateway.
- **Containerconfiguratie en distro-logbeleid.** De Tomcat-versiebanner (bevinding #6b) en het distro-logniveau zijn deployment-eigenschappen buiten de module; ze zijn als scope-grens respectievelijk deployment-voorwaarde vastgelegd, niet als modulebevinding.

## Aanvalsoppervlak

Het aanvalsoppervlak is via statische broncode-inspectie in kaart gebracht (`attack-surface-tabel.md`, `attack-surface-samenvatting.md`, NEN-7510 8.25). Geïdentificeerd zijn 34 entry points (HTTP-endpoints plus DWR-methoden), waarvan 5 als hoog risico geclassificeerd, naast 11 JSP-formulieren met externe invoer, 2 bestandsupload-/SSRF-kanalen en 2 HL7-invoerkanalen. De vijf hoog-risico entry points betreffen onder meer SSRF via `fetchModuleDetails.form`, plaintext-credentials op `postHl7.form`, en systeemconfiguratie-endpoints zonder privilege-check op controller-niveau (`globalProps.form`, `DWRAdministrationService.setGlobalProperty`).

Dit aanvalsoppervlak vormt de context waarbinnen de pentest is uitgevoerd: de geteste bevindingen zijn concrete instanties uit deze bredere kaart, niet de volledige kaart zelf. Het overzicht is daarmee zowel scope-afbakening als verantwoording van wat wél en niet diepgaand is getest.

## Normkader

De audit toetst primair tegen **NEN-7510:2024-2** (de geanalyseerde controls: 8.3 toegangsbeveiliging, 8.5 authenticatie, 8.15 logging, 8.9 configuratiebeheer en 8.25 attack surface). Aanvullend zijn **ISO 25010** (voor het onderhoudbaarheidsspoor, apart beoordeeld), **OWASP** (pentest-methodologie, zie #57) en de **CRA (EU 2024/2847)** als kader voor supply-chain- en kwetsbaarheidsbeheer (zie #59) van toepassing.
