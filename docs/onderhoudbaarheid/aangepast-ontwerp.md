# Aangepast ontwerp — FormatTag type-dispatch

**Datum:** 2026-06-17
**Scope:** `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java`
**Target-methode:** `printObject(StringBuilder, Object)` (r. 270–303) en de gedeelde type-dispatch in `doStartTag()`
**Norm:** ISO 25010 (Modularity, Modifiability, Analyzability) · Strategy-patroon · SRP
**Bron-analyse:** `docs/onderhoudbaarheid/analyse-onderhoudbaarheid.md` (C1)

---

## 1. Keuze van het refactor-target (en waarom niet de zwaarste methode)

De C1-analyse wijst `PersonFormController.updatePersonAddresses()` aan als de methode met de hoogste complexiteit (CC ≈ 76) en de hoogste prioriteit. Toch is dat bewust **niet** het target van deze PoC. De motivatie is een risico-afweging op kwaliteitseisen, niet gemak:

1. **Geen betrouwbare regressie-baseline.** `updatePersonAddresses()` heeft 0% testdekking. Bovendien bevat de methode een latente fout: de lus dereferentieert `add1s.length` (r. 615 e.v.) zonder null-check, terwijl de openingsconditie (r. 562–564) toelaat dat `add1s` null is zolang één van de zeventien parameter-arrays gevuld is. Het huidige gedrag is dus deels onbepaald. De validatie-eis van dit beroepsproduct ("aantonen dat er geen regressie is opgetreden", criterium C6) vereist een vastgelegd correct uitgangsgedrag; dat ontbreekt hier. Refactoren zonder vangnet op code die zich al deels fout gedraagt, ondermijnt juist het criterium dat het meeste weegt.
2. **Het patroon past minder zuiver.** `updatePersonAddresses()` is een DRY-schending (parallelle parameter-arrays), op te lossen met Extract Class. Dat is een refactoringpatroon, geen ontwerppatroon — een minder rijk verhaal voor het ontwerpcriterium.

`FormatTag.printObject()` is daarom gekozen: het is een textbook type-dispatch (CC ≈ 15, en het voedt de CC-32 in `doStartTag()`), het mapt direct op het Strategy-patroon uit de lesstof, en er bestaat al een regressie-vangnet (`FormatTagTest`). De lagere complexiteit is hier een voordeel: de PoC kan de verbetering zuiver en reproduceerbaar aantonen in plaats van te verzanden in een hoog-risico herschrijving.

Dit is een scope-keuze, geen ontwijking: `updatePersonAddresses()` blijft als hoogste prioriteit in de verbeterbacklog (C3) staan, met de aantekening dat het eerst karakterisatietests vereist.

## 2. Huidige situatie: de smell

`printObject()` dispatcht op runtime-type via een `instanceof`/`else if`-cascade over elf domeintypen (Collection, Date, Concept, Obs, User, Encounter, Visit, Program, Provider, Form, SingleCustomValue, OpenmrsMetadata, met een toString-fallback). Daarnaast houdt `FormatTag` ~30 parallelle `Integer xId` + domeinobject-veldparen bij, die `doStartTag()` één voor één afhandelt in dezelfde stijl.

Dit schendt drie ISO 25010-subkenmerken tegelijk:

- **Modularity / Open-Closed:** een nieuw domeintype toevoegen raakt drie plekken in dezelfde klasse (veld, `doStartTag()`, `printObject()`). De Visit-uitbreiding in de git-historie is daar het bewijs van. De klasse is open voor wijziging waar ze gesloten zou moeten zijn.
- **Analyzability:** wie wil weten hoe één type wordt opgemaakt, moet de hele cascade doorlopen. Er is geen enkelvoudig aanspreekpunt per type.
- **Modifiability:** alle opmaaklogica voor veertien typen zit in één klasse van ruim 1000 regels — één verantwoordelijkheid per type, opgeteld een SRP-schending.

```
printObject(sb, o):
    if      o instanceof Collection  -> recurse per element
    else if o instanceof Date        -> printDate(...)
    else if o instanceof Concept     -> printConcept(...)
    else if o instanceof Obs         -> sb.append(escapeHtml(valueAsString))
    else if o instanceof User        -> printUser(...)
    ... (nog 6 takken) ...
    else                             -> sb.append(escapeHtml(o.toString()))
```

## 3. Voorgesteld ontwerp: Strategy met een type-geïndexeerde registry

Vervang de cascade door een Strategy-interface `FormatStrategy` met één implementatie per domeintype, geregistreerd in een `Map<Class<?>, FormatStrategy>`. `printObject()` wordt een lookup: zoek de strategy voor `o.getClass()` (met overerving-aware matching voor subtypes en interfaces), delegeer, val terug op een default-strategy.

```
interface FormatStrategy {
    void format(StringBuilder sb, Object value, FormatContext ctx);
}

class ConceptFormatStrategy implements FormatStrategy { ... }
class UserFormatStrategy    implements FormatStrategy { ... }
class DefaultFormatStrategy implements FormatStrategy { ...escapeHtml(toString())... }   // fallback

class FormatStrategyRegistry {
    private final Map<Class<?>, FormatStrategy> strategies;   // Concept.class -> ConceptFormatStrategy, ...
    FormatStrategy resolve(Object o);   // exacte match, dan assignable-match, dan default
}
```

`printObject()` reduceert tot:

```
private void printObject(StringBuilder sb, Object o) {
    if (o instanceof Collection<?>) { ...recurse...; return; }   // collectie blijft structureel apart
    registry.resolve(o).format(sb, o, context);
}
```

`FormatContext` draagt de waarden die de huidige `print*()`-methoden uit de tag-velden lezen (locale, javaScriptEscape, withConceptNameType/-Tag, EncounterRole-filters), zodat de strategies geen verborgen koppeling aan `FormatTag` houden.

> **Realisatienoot — `javaScriptEscape` bewust niet in `FormatContext`.** Bij de realisatie bleek dat geen enkele `print*()`-methode op de `printObject()`-tak `javaScriptEscape` leest; de escaping gebeurt pas in `doStartTag()` op het eindresultaat. Dit veld is daarom bewust uit `FormatContext` gehouden (zie `realisatie-poc.md`, C5). De opsomming hierboven beschrijft het oorspronkelijke ontwerp; de twee documenten spreken elkaar op dit punt dus niet tegen.

**Effect op de kwaliteitskenmerken:**
- **Open-Closed / Modularity:** een nieuw type = één nieuwe strategy-klasse + één registry-regel. `printObject()` en `doStartTag()` wijzigen niet meer.
- **Analyzability:** één klasse per type; de opmaaklogica van Concept staat in `ConceptFormatStrategy`, niet verspreid.
- **Testability:** elke strategy is een losse unit, zonder JSP-tag-context testbaar — precies het gebrek dat C1 als zwakste schakel aanwees.
- **Complexiteit:** de CC van `printObject()` daalt van ~15 naar ~2 (alleen nog de collectie-tak + delegatie); de cascade-complexiteit verschuift naar losse, elk triviale strategies.

## 4. Afgewogen alternatieven

**Alternatief A — Visitor-patroon.** Een `FormatVisitor` met `visit(Concept)`, `visit(User)`, etc., en `accept()` op de domeinobjecten. Theoretisch de klassieke oplossing voor type-dispatch. **Verworpen:** Visitor vereist dat de domeinklassen (Concept, User, …) een `accept()`-methode krijgen. Die klassen zitten in openmrs-core — buiten de scope van deze module en niet door ons te wijzigen. Visitor is hier dus onuitvoerbaar zonder de geauditeerde grens te overschrijden. Dit is precies waarom Strategy + registry past: het houdt de wijziging binnen de module.

**Alternatief B — kale `Map<Class<?>, ...>` met lambdas, zonder Strategy-interface.** Lichter qua code. **Verworpen als hoofdontwerp, deels overgenomen:** een interface geeft elke handler een naam, een vindbare klasse en een eigen testklasse (testability-winst), terwijl losse lambdas weer één groot configuratieblok vormen — de smell in een nieuw jasje. De registry-lookup zelf leen ik wél uit dit alternatief; de combinatie (Strategy-interface + Map-registry) is het ontwerp.

**Alternatief C — niets doen / alleen `printObject()` opsplitsen in kleinere methoden.** Verlaagt LOC maar niet de structurele Open-Closed-schending: een nieuw type raakt nog steeds de cascade. Lost het kernprobleem niet op.

De keuze voor Strategy + registry is gemotiveerd op kwaliteitseisen: het is het enige alternatief dat Open-Closed binnen de modulegrens realiseert (A kan niet), de testbaarheid per type oplost (C niet), en de dispatch niet terugbrengt tot één configuratieblok (B alleen).

## 5. Grenzen van de PoC

De PoC migreert `printObject()` en de daarop leunende takken; de `doStartTag()`-id-resolutie (de `Integer xId` → object-lookups) blijft grotendeels intact, omdat die een apart probleem is (parameter-oppervlak van de tag) dat los van de type-dispatch staat. Het volledig wegwerken daarvan staat als vervolg in de verbeterbacklog. De PoC toont het patroon aantoonbaar op de type-dispatch; volledige uitrol over de hele tag is expliciet buiten PoC-scope.
