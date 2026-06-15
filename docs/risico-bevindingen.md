# Risico-analyse & bevindingen

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** · Issue #58

## Aanpak van de risicoweging

Per bevinding zijn twee instrumenten gebruikt, met een verschillende rol. CVSS v3.1 geeft de technische ernst van de kwetsbaarheid los van context. Het besluit — oplossen, accepteren of escaleren — volgt de risicocriteria in `risicocriteria.md`: een bevinding die patiëntveiligheid of de vertrouwelijkheid van patiëntdata raakt is onacceptabel vanaf score ≥10, tegen ≥15 voor overige categorieën. De zwaarst geraakte categorie bepaalt welke grens geldt.

De besluiten zijn rechtstreeks tegen deze grenswaarden genomen, niet via een afzonderlijke kans×impact-plot per bevinding. De risicomatrix uit het threat model (Sprint 2, bijlage) dekt het risiconiveau op systeemniveau; de pentest-bevindingen zijn daarvan de concrete, geverifieerde instanties. Een aparte 5×5-score per finding zou een rekenstap suggereren die in de werkelijke besluitvorming niet is gezet en is daarom bewust achterwege gelaten.

## Bevindingen

De volledige bewijsketen per bevinding (bron, CVSS, fix-commit, hertest, NEN-control) staat in de traceability matrix; deze sectie geeft de risico-onderbouwing.

**#1 — Sessie-IP-binding via een dood codepad (opgelost).** `RequireTag` controleerde het server-IP in plaats van het client-IP, waardoor de sessie-IP-binding feitelijk niets afdwong. CVSS 7.4 (hoog). De bevinding raakt de toegangsbeveiliging tot een zorgsysteem en valt daarmee onder de aangescherpte grens; oplossen was de enige aanvaardbare uitkomst. Gefixt en hertest (NEN 8.3).

**#2 — Brute-force-drempel van 100, in-memory (geaccepteerd).** Het inlogmechanisme blokkeert pas na 100 pogingen en de teller staat in geheugen, dus reset bij herstart. CVSS 6.5. Er ís rate-limiting aanwezig en het resterende risico blijft onder de grenswaarde voor de getroffen categorie; de bevinding is geaccepteerd met de afspraak om te herbeoordelen bij de volgende scan (NEN 8.5). De onderbouwing is een bewuste acceptatie, geen nalatigheid — zie de sectie hieronder.

**#3 — Beveiligingsgebeurtenissen niet ge-audit-logd (opgelost en doorontwikkeld).** Tien beveiligingsrelevante acties (aanmaken/wijzigen/verwijderen van gebruikers, rolwijziging, wachtwoordwijziging, login/logout) lieten geen auditspoor na, terwijl het mechanisme aantoonbaar bestond — een mislukte verwijdering produceerde 139 regels, een succesvolle nul. CVSS 5.3, maar de score onderschat de ernst: CVSS kent geen metriek voor verlies van auditability, en het ontbreken van een auditspoor raakt NEN 8.15 direct. De weging liep daarom via de norm en de risicocriteria, niet via de score. Gefixt in de pentest-remediatie en daarna doorontwikkeld (logging op WARN met gestructureerde eventnamen plus geautomatiseerde regressietests); de doc-staat is daarop gelijkgetrokken (zie traceability matrix en `pentestplan.md` §4). Deze evolutie is via verificatie tegen de codebase vastgesteld, niet aangenomen.

**#4 — Gebruikersenumeratie via forgot-password (geaccepteerd).** De wachtwoordvergeten-flow gaf deels onderscheidbare responses voor bestaande en niet-bestaande gebruikers. CVSS 5.3. Het risico blijft onder de grenswaarde voor de getroffen categorie; geaccepteerd met herbeoordeling bij de volgende scan (NEN 8.5).

**#5 — Reset-flow niet voltooibaar en tijdelijk wachtwoord zonder vervaltermijn (opgelost).** De reset-flow was niet af te ronden en het tijdelijke wachtwoord kende geen vervaltermijn. CVSS 5.3 voor het bereikbare deel. Gefixt: de flow voltooit en een geforceerde wachtwoordwijziging grijpt in (NEN 8.5). Eén deployment-voorwaarde resteert: de `ForcePasswordChangeFilter` moet actief zijn.

**#6 — Information disclosure via stacktraces en versiebanner (opgelost / scope-grens).** Autorisatiefouten leverden een stacktrace op en de foutpagina lekte de Tomcat-versie. CVSS 5.3. Het applicatiedeel is opgelost — autorisatiefouten geven nu een generieke 403 zonder stacktrace, met behoud van server-side logging. De versiebanner is containerconfiguratie en valt buiten de modulescope; dat deel is als scope-grens vastgelegd. Deze bevinding kwam uit de pentest en niet uit de gap-analyse (die toetste 8.3/8.5/8.15); ze is gekoppeld aan NEN 8.9 (configuratiebeheer), zonder te claimen dat 8.9 systematisch is geanalyseerd.

**T4 — weerlegde hypothese (geen bevinding).** De aanname dat controllers geen privilege-check zouden afdwingen bleek onjuist: de service-laag-AOP weigert ongeprivilegieerde toegang vóór gevoelige verwerking. Een audit hoort ook vast te leggen wat aantoonbaar werkt; dit is opgenomen als positief resultaat.

## Wat bewust niet is gedaan, met onderbouwing

De opdracht vereist dat geaccepteerde risico's en niet-uitgevoerde maatregelen expliciet worden verantwoord. Dit zijn bewuste keuzes met een vastgelegde reden, geen openstaande gaten.

**Geaccepteerde bevindingen #2 en #4.** Beide vallen onder de grenswaarde voor de getroffen categorie en zijn geaccepteerd met herbeoordeling bij de volgende scan, conform het triagebeleid (`false-positive-beleid.md`, status *Accept*). Voor #2 weegt mee dat rate-limiting aanwezig is; voor #4 dat de informatielekkage beperkt en niet zelfstandig exploiteerbaar is.

**Scope-grens bij #6.** Het hardenen van de Tomcat-versiebanner is containerconfiguratie buiten de geauditeerde module. Vastgelegd als grens, niet als opgeloste of genegeerde bevinding.

**Snyk-/SCA-triage zonder automatische pipeline-breuk.** De scans breken de build niet automatisch; een teamlid beoordeelt elke finding (Fix/Accept/False positive/Defer) met verplichte onderbouwing, en onderdrukkingen lopen herleidbaar via de `.snyk`-policyfile in versiebeheer (`false-positive-beleid.md`). De kwetsbare baseline is bewust niet gepatcht om `baseline-legacyui-1.20.0` intact te houden vóór de analyse; de openstaande Dependabot-PR's zijn het materiaal voor het patchadvies.

**GitHub Advanced Security niet ingezet (comply-or-explain).** Code Scanning en de Dependency Review Action vereisen GHAS, een betaalde Enterprise-feature voor private repositories — geverifieerd op zowel een persoonlijk Pro-account als in een organisatie. De repo publiek maken zou GHAS gratis geven, maar is onwenselijk voor een vertrouwelijkheidsaudit op een zorgmodule. De functie is daarom vervangen door Dependabot security updates plus een Snyk-scanrapport; CodeQL draait volledig en levert resultaten als CI-artifact in plaats van via de Security-tab. De analyse is dus uitgevoerd; alleen de presentatielaag wijkt af (`mini-complianceverslag.md` M2).

## NEN-dekking

Vier controls geraakt door de bevindingen: 8.3 (#1), 8.5 (#2, #4, #5), 8.15 (#3), 8.9 (#6). De compliance-oordelen per control horen in de conclusie (#60).
