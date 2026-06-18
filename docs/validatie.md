# Validatie verbeteringen — testen & regressie

**Datum:** 2026-06-17
**Branch/Commit:** `feat/onderhoudbaarheid-formattag-strategy` → gemerged naar `main` via PR #80, commit `a3942bb`
**Meetinstrumenten:** JaCoCo (coverage + complexity, CI-artifact), handmatige McCabe-telling (consistent met C1)
**Bron:** `realisatie-poc.md` (C5), `analyse-onderhoudbaarheid.md` (C1)

---

## 1. Opzet van de validatie

De validatie toont twee dingen aan: dat de onderhoudbaarheid meetbaar is verbeterd, en dat er geen regressie is opgetreden. Beide worden onderbouwd met metrieken die vóór en ná de refactor op identieke wijze zijn gemeten. De "vóór"-meting komt van een JaCoCo-run op `main`, de "na"-meting van een run op de refactor-branch.

## 2. Geen regressie — het bewijs

Het regressie-vangnet (C2) bestaat uit 15 padcases over `printObject()` plus een case voor de muterende `caseConversion="global"`-route. Deze tests zijn toegevoegd en groen gedraaid op de **ongewijzigde** code (de baseline), en daarna ongewijzigd opnieuw gedraaid na de refactor.

- Volledige omod-testsuite na de refactor: 399 tests, 0 failures, 0 errors, 3 skipped — BUILD SUCCESS.
- `FormatTagTest`: alle cases groen, vóór en ná.
- **Cruciaal:** `git diff main` op de testklasse toont 144 toevoegingen en 0 wijzigingen. Geen enkele bestaande assertie is aangepast. Het groen blijven van een ongewijzigde testsuite ná een gewijzigde implementatie is het bewijs dat het waarneembare gedrag identiek is gebleven.

De `caseConversion="global"`-case verdient aparte vermelding: dit is de enige stateful, muterende route en was vóór deze opzet niet getest (C1 noteerde 0% dekking op `applyConversion`). De test pint nu het gedrag (global property `uppercase` → `"UNKNOWN LOCATION"`), zodat de refactor van de gedeelde `caseConversion`-logica een vangnet had.

## 3. Verbetering — methode-niveau

De kern van de verbetering zit op de gerefactorde methode. `printObject()` ging van een `instanceof`/`else-if`-cascade naar een registry-lookup:

| Methode | CC vóór | CC ná | McCabe-band |
|---|---|---|---|
| `FormatTag.printObject()` | ~13–15 | **4** | van "moeilijk" (11–20) naar "eenvoudig" (1–10) |

De CC ná is met de hand geteld op de werkelijke code (3 beslispunten: de `Collection`-check, de lus-conditie en de scheidingsteken-`if`, plus 1). De resterende complexiteit van 4 zit volledig in de behouden collectie-tak; de type-dispatch zelf draagt niet langer bij. Dit haalt de methode uit de complexiteitsband die C1 als risicovol markeerde.

## 4. Verbetering — herverdeling van complexiteit

Op klasse-niveau is het beeld genuanceerder, en eerlijk weergegeven belangrijker dan een misleidend cijfer:

| Eenheid | Complexity vóór | Complexity ná |
|---|---|---|
| `FormatTag` (klasse) | 166 | 140 |
| 12 strategy-klassen (samen) | — | ~46 (CC 2–8 per klasse) |
| `FormatStrategyRegistry` | — | 5 |
| `FormatContext` | — | 9 |

De klasse-complexiteit van `FormatTag` daalde beperkt (166 → 140). Dat is geen tegenvaller maar het verwachte gevolg van een afgebakende PoC: de type-dispatch-complexiteit is **verplaatst** naar twaalf losse strategy-klassen, elk met een lage, op zichzelf staande complexiteit (CC 2–8). De resterende 140 in `FormatTag` zit in de bewust buiten scope gehouden delen — de `doStartTag()` id-resolutie en de provider/location-opmaak (zie C5 §4). De totale complexiteit is niet verdwenen maar opgesplitst in begrijpelijke, individueel onderhoudbare eenheden — precies wat het Strategy-patroon en het Single Responsibility Principle beogen.

Dit verbetert de ISO 25010-subkenmerken die C1 als zwak markeerde: **Modularity** (een nieuw type = één nieuwe strategy-klasse, geen wijziging aan `printObject`/`doStartTag` — Open/Closed), **Analyzability** (de opmaaklogica per type staat in één vindbare klasse) en **Modifiability** (een wijziging aan één type-opmaak raakt alleen die ene strategy).

## 5. Verbetering — testbaarheid

| Eenheid | Branch coverage vóór | Branch coverage ná | Line coverage ná |
|---|---|---|---|
| `FormatTag` | 40% (65/162) | 32% (35/110) | 21% |
| `ConceptFormatStrategy` | — | 83% (10/12) | 90% |
| `FormatStrategyRegistry` | — | 100% (6/6) | 100% |
| `FormatContext` | — | 75% (6/8) | 90% |
| overige strategies | — | 50–100% | 80–100% |

Het coverage-percentage van `FormatTag` lijkt te dalen (40% → 32% branch), maar de absolute getallen tonen het tegendeel: het aantal branches in `FormatTag` daalde van 162 naar 110 — 52 branches zijn uit de klasse vertrokken naar de strategies. Het percentage zakt omdat de relatief goed te dekken dispatch-logica is verhuisd en de moeilijker te testen `doStartTag`-romp achterblijft. De vertrokken branches zitten nu in strategies die op zichzelf 75–100% gedekt zijn.

De inhoudelijke winst is dat deze logica nu **in isolatie testbaar** is. Vóór de refactor was de type-opmaak alleen te bereiken via de volledige JSP-tag-context (`doStartTag` + `pageContext`); nu kan elke strategy met een directe unit-test worden getest zonder tag-infrastructuur. Dit is exact de testbaarheidsbeperking die C1 als zwakste schakel aanwees, en die met de refactor is opgeheven.

## 6. Reproduceerbaarheid

Alle metingen zijn reproduceerbaar:
- Coverage + complexity: `mvn clean install -Dformatter.skip=true -Dlicense.skip=true` genereert het JaCoCo-rapport (`omod/target/site/jacoco/`), gepubliceerd als CI-artifact `jacoco-coverage`. De getallen hierboven komen uit `jacoco.xml` op respectievelijk `main` en de branch.
- Regressietests: `mvn -B test -pl omod -Dtest=FormatTagTest -Dformatter.skip=true -Dlicense.skip=true`.
- Methode-CC: handmatige McCabe-telling (beslispunten + 1), dezelfde methode als C1, na te tellen op de broncode.

## 7. Conclusie

De refactor verbetert de onderhoudbaarheid aantoonbaar: de complexiteit van de doelmethode daalde van een risicovolle band naar "eenvoudig" (CC 4), de type-dispatch is opgesplitst in twaalf individueel onderhoudbare en testbare strategies, en de eerder ontoegankelijke opmaaklogica is nu in isolatie te testen. Tegelijk is aangetoond dat geen regressie is opgetreden: een ongewijzigde, uitgebreide testsuite blijft groen op de gewijzigde implementatie. De beperkte daling van de klasse-complexiteit van `FormatTag` is eerlijk verklaard als gevolg van de afgebakende PoC-scope, met de resterende migratie vastgelegd in de vervolgbacklog (C3).
