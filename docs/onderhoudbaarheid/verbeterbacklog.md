# Verbeterbacklog onderhoudbaarheid — legacyui-audit

**Module:** OpenMRS Legacy UI v1.20.0
**Prioritering:** impact × effort (zie §1) · **Bronnen:** `analyse-onderhoudbaarheid.md` (C1), `realisatie-poc.md` (C5), `validatie.md` (C6)

---

## 1. Prioriteringsmethode

Elke verbetering is gescoord op twee assen, elk 1–5:

- **Impact** = hoeveel de verbetering de onderhoudbaarheid verhoogt (gewogen naar de ISO 25010-subkenmerken uit C1 en de gemeten complexiteit/dekking).
- **Effort** = geschatte realisatie-inspanning, inclusief het benodigde test-vangnet.

Prioriteit volgt uit impact afgezet tegen effort: **P1** = hoge impact, beheersbare effort (eerst doen); **P2** = hoge impact maar hoge effort, of matige impact; **P3** = lage impact of zeer hoge effort. De score-kolom is impact − effort als grove rangschikking; de motivatie per regel is leidend, niet het getal alleen.

Elke verbetering verwijst terug naar de bevinding in C1 (de gemeten CC/coverage) die haar onderbouwt.

## 2. P1 — eerst doen

| ID | Verbetering | Bron (C1) | Impact | Effort | Motivatie |
|---|---|---|---|---|---|
| M1 | `PersonFormController.updatePersonAddresses()` refactoren (CC 76 → doelband <15) via Extract Class (`AddressParser` op een `Map<String,String[]>`) | C1 §4.3, prioriteit H | 5 | 4 | Hoogste gemeten complexiteit van de hele module + 0% dekking. **Vereist eerst karakterisatietests**, want er is geen betrouwbare gedrag-baseline en de methode bevat een latente NPE (lus dereferentieert arrays zonder null-check terwijl de openingsconditie null toelaat). Vangnet vóór refactor is niet-onderhandelbaar. |
| M2 | `PersonFormController.onSubmit()` decompositie (CC 32, dood-registratie 4 niveaus diep genest) | C1 §2.3, prioriteit H | 4 | 3 | Businesslogica (overlijdensregistratie) ontvlechten van HTTP-sessie/redirect-logica; verhoogt Analyzability en maakt de kern los testbaar. 0% dekking, dus ook hier karakterisatietests eerst. |

## 3. P2 — verhoogd

| ID | Verbetering | Bron (C1) | Impact | Effort | Motivatie |
|---|---|---|---|---|---|
| M3 | `ConceptFormBackingObject` (718 regels) uit `ConceptFormController` halen naar een eigen bestand/klasse | C1 §2.1, prioriteit H | 4 | 3 | Inner class combineert HTTP-afhandeling en Concept-assemblage; scheiding verbetert Modularity en maakt de binding-logica zelfstandig testbaar. Relatief veilig dankzij de bestaande ~75–85% dekking. |
| M4 | FormatTag-vervolg: de twee bewuste duplicaties uit de PoC opheffen (`applyConversion` en `getProviderName` nog dubbel) en de provider/location-opmaak naar strategies migreren | C5 §4, validatie C6 §4 | 3 | 3 | Maakt de in de PoC begonnen Strategy-migratie compleet; haalt de resterende klasse-complexiteit van `FormatTag` (nu nog 140) verder omlaag. Vereist migratie van de `doStartTag`-takken die nu buiten scope vielen. |
| M5 | `getConceptFromFormData()` (CC 39) ontvlechten: geneste `&&`-expressies in synoniem-/indextermvalidatie extraheren | C1 §4.4 | 3 | 3 | Verlaagt complexiteit van de zwaarste methode in `ConceptFormController`; goed gedekt, dus lager regressierisico. |
| M6 | `FormatTag.doStartTag()` id-resolutie (CC 32, 27 null-checks) aanpakken — het parameter-oppervlak van de tag | C1 §2.3, C5 §4 | 3 | 4 | Bewust buiten de PoC gehouden; een apart probleem (te grote API van de tag). Hoge effort omdat het de tag-contract raakt. |

## 4. P3 — later / lage prioriteit

| ID | Verbetering | Bron (C1) | Impact | Effort | Motivatie |
|---|---|---|---|---|---|
| M7 | Deprecated `new Boolean(String)`-constructor vervangen (`PersonFormController` r. 491/648) | C1 §2.4 | 2 | 1 | Klein, maar verwijderd in Java 17 — modifiability-risico bij JVM-upgrade. Lage effort, lage directe impact. |
| M8 | `SimpleFormController`-afhankelijkheid (deprecated sinds Spring 3.0) op termijn vervangen | C1 §2.4 | 4 | 5 | Hoge impact maar zeer hoge effort: forceert herschrijving van de controllerlaag. Hoort bij een breder framework-migratietraject, niet bij losse refactoring. |
| M9 | `SingleCustomValueFormatStrategy` testdekking verhogen (nu 25% branch / 38% line, laagste van de strategies) | C6 §5 | 2 | 1 | Laaghangend fruit: de strategy bestaat nu als losse eenheid, dus aanvullende unit-tests zijn goedkoop. |

## 5. Relatie tot de gerealiseerde PoC

De PoC (C5/C6) realiseerde de FormatTag type-dispatch-refactor — niet als losse regel in deze backlog, maar als het uitgevoerde bewijs van de aanpak. M4 en M6 zijn de directe vervolgstappen die in de PoC bewust buiten scope bleven. M1 (de hoogst gescoorde verbetering uit C1) is bewust **niet** als PoC gekozen, om de in C4 §1 en C5 §4 onderbouwde reden: 0% dekking plus een latente bug maken een betrouwbare regressie-baseline onmogelijk zonder eerst karakterisatietests te schrijven. Die afweging — de hoogst-complexe methode is niet automatisch de beste PoC-kandidaat — is zelf een onderhoudbaarheidsbeslissing en staat hier expliciet vastgelegd.
