# Realisatie PoC & verantwoording — FormatTag Strategy-refactor

**Datum:** 2026-06-17
**Branch/Commit:** `feat/onderhoudbaarheid-formattag-strategy` → gemerged naar `main` via PR #80, commit `a3942bb`
**Target:** `FormatTag.printObject()` type-dispatch → Strategy-patroon
**Bron:** `aangepast-ontwerp.md` (C4), `testopzet.md` (C2)

---

## 1. Wat is gerealiseerd

De type-dispatch van `FormatTag.printObject()` — een `instanceof`/`else-if`-cascade over elf domeintypen — is vervangen door het Strategy-patroon. De realisatie bestaat uit:

| Component | Type | Rol |
|---|---|---|
| `FormatStrategy` | interface | `format(StringBuilder, Object, FormatContext)` |
| `FormatContext` | waardeobject | draagt `withConceptNameType`, `withConceptNameTag`, `caseConversion`, `locale` |
| `FormatStrategyRegistry` | registry | `LinkedHashMap` in cascade-volgorde; `resolve()` = exact → assignable → default |
| 11 concrete strategies + `DefaultFormatStrategy` | per type | de verplaatste opmaaklogica per domeintype |

`printObject()` is daarmee teruggebracht tot de collectie-tak (recursie + scheidingsteken) plus één delegatie naar `registry.resolve(o).format(...)`.

## 2. Overeenstemming met het ontwerp (C4)

De PoC volgt het ontwerp op de hoofdpunten: Strategy-interface, een registry met een `Map<Class<?>, FormatStrategy>`, en een resolutie die overerving-aware is. Het ontwerp koos Strategy boven Visitor omdat Visitor een `accept()`-methode op de openmrs-core-domeinklassen zou vereisen — buiten de modulegrens. De realisatie bevestigt die keuze: de strategies konden volledig binnen de module worden toegevoegd zonder enige wijziging aan core.

Eén bewuste afwijking van het ontwerpdocument: `aangepast-ontwerp.md` §3 noemde `javaScriptEscape` als context-veld. Tijdens de realisatie bleek via verificatie dat geen enkele `print*`-methode op de `printObject`-tak dit veld leest — het wordt uitsluitend in `doStartTag()` toegepast bij het wegschrijven van het eindresultaat, ná de opmaak. Het veld is daarom bewust niet in `FormatContext` opgenomen; opname zou een ongebruikte afhankelijkheid (dcode-dood) hebben geïntroduceerd. Dezelfde redenering geldt voor de EncounterRole-filters, die alleen via `doStartTag()` lopen. Deze afwijking is een aanscherping van het ontwerp op basis van de werkelijke code, niet een tekortkoming.

## 3. Resolutie-volgorde: het correctheidskritische detail

`Provider`, `Form` en `Program` implementeren allemaal óók `OpenmrsMetadata`. Een naïeve type-lookup zou ze naar `MetadataFormatStrategy` sturen en hun specifieke opmaak breken. De registry borgt dit met een `LinkedHashMap` in exact de oude cascade-volgorde (specifieke typen eerst, `OpenmrsMetadata` als laatste) en een `resolve()` die eerst een exacte klasse-match probeert en daarna de eerste assignable match. Dit reproduceert de `else-if`-semantiek precies. Bijkomend voordeel: Hibernate-proxies (waarvan `getClass()` niet gelijk is aan `Concept.class`) worden via de assignable-match correct afgehandeld — wat de regressietests met DB-geladen objecten groen houdt.

## 4. Bewuste beperkingen van de PoC

De PoC is afgebakend tot de type-dispatch. Het volgende is bewust níet aangepakt en hoort in de vervolgbacklog:

- **De `doStartTag()` id-resolutie** (de `Integer xId` → object-lookups) is ongemoeid gelaten. Dit is een apart onderhoudbaarheidsprobleem (het parameter-oppervlak van de tag), niet de type-dispatch.
- **Twee bewuste duplicaties.** `applyConversion()` staat nu zowel in `FormatContext` (voor de strategies) als nog in `FormatTag` (voor `printLocation`/`printLocationTag`, die op het id-pad liggen). `getProviderName()` staat in `ProviderFormatStrategy` én nog in `FormatTag` (voor `printEncounterProviders`, dat het `encounterProviders`-veld gebruikt). Beide duplicaties bestaan omdat de bijbehorende `doStartTag`-takken buiten PoC-scope vielen; volledig wegtrekken vereist dat ook die takken worden gemigreerd.
- **De provider-filtering en location-opmaak** (`getDisplayEncounterProviders`, `filterProviders`, `containsRole`, `trimStringArray`, `printLocation`, `printLocationTag`) zijn in `FormatTag` gebleven. Dit verklaart waarom de klasse-complexiteit van `FormatTag` beperkt daalde (zie validatie, C6): een PoC migreert de dispatch, niet de hele klasse.

Deze beperkingen zijn geen onaf werk maar een scope-keuze: één goed onderbouwde, gemeten en geteste refactor toont het patroon aantoonbaar aan, waar een volledige herschrijving van de tag het PoC-karakter en de meetbaarheid zou ondermijnen.

## 5. Verantwoording van AI-toolinggebruik

De refactor is uitgevoerd met Claude Code als uitvoerder onder directe aansturing, in vier gecontroleerde fasen met een reviewmoment na elke fase:

1. **Onderzoek** — per `print*`-methode geverifieerd welke tag-velden en statische context worden gelezen, vóór enige codewijziging. Dit ving de `javaScriptEscape`-afwijking (§2) en de `caseConversion`-mutatie (§3).
2. **Vangnet eerst** — de regressietests (de 15 padcases uit C2 plus een `caseConversion="global"`-case) zijn toegevoegd en groen gedraaid op de ongewijzigde code, vóór de refactor begon. Zonder dit vangnet is "geen regressie aantonen" niet hard te maken.
3. **Refactor** — de strategies, context en registry gebouwd; de logica verplaatst, niet herschreven.
4. **Isolatietests** — directe unit-tests op losse strategies om de testbaarheidswinst aan te tonen.

**Kritische reflectie op het toolinggebruik.** De waarde van het AI-gebruik zat niet in het genereren van code maar in de gecontroleerde, verifieerbare werkwijze. Drie keuzes zijn bewust níet aan de tool overgelaten maar zelf bepaald: de keuze het vangnet vóór de refactor te bouwen (niet erna), de eis dat geen enkele bestaande test gewijzigd mocht worden (geverifieerd via `git diff`: 144 toevoegingen, 0 wijzigingen), en de afbakening van de scope tot de type-dispatch. Waar de tool een schatting gaf ("CC ≈ 2") is die niet overgenomen maar tegen de werkelijke code geverifieerd (de gemeten methode-CC is 4 — zie C6). Het tegen de bron verifiëren in plaats van de tool-samenvatting vertrouwen was de doorslaggevende discipline; de tool versnelt de uitvoering, maar de correctheidsgaranties komen uit de meting en de tests, niet uit de tool.
