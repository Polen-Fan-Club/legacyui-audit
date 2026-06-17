# Testopzet & testresultaten — FormatTag.printObject()

**Datum:** 2026-06-17
**Methode:** `FormatTag.printObject(StringBuilder, Object)` (r. 270–303)
**Doel:** het gedrag van de te refactoren methode vastleggen vóór de Strategy-refactor (C5), zodat C6 kan aantonen dat er geen regressie optreedt.
**Bron:** lesmodel path-coverage (`06_Metrieken_en_Testbaarheid.pdf`), `aangepast-ontwerp.md` (C4)

---

## 1. Aanpak

De refactor in C5 vervangt de type-dispatch van `printObject()` door een Strategy-registry. Om aantoonbaar te maken dat het uitwendige gedrag niet verandert, leggen we het gedrag eerst vast met een testopzet volgens het lesmodel: code als graaf → basispaden → padvergelijkingen → testcases met voorspelde uitkomst. Deze tests vormen het regressie-vangnet en draaien zowel vóór als na de refactor (C6).

Twee testtypen worden gecombineerd:
- **White-box path coverage** op `printObject()` zelf: elke tak van de dispatch krijgt een testpad.
- **Black-box gedragstest** via de publieke `doStartTag()`: omdat `printObject()` private is, valideren de tests het waarneembare resultaat (de gerenderde output) door de tag aan te roepen met een object van elk type. Dit dekt tevens de integratie van `printObject()` met `doStartTag()`.

`FormatTagTest` bestaat al en dekt een deel hiervan (Concept, HTML-escape, object-routing); de testopzet breidt dit uit tot volledige takdekking.

## 2. Code als graaf

`printObject()` bestaat uit één voorwaardelijke keten. De beslispunten:

| Knoop | Conditie | Regel |
|---|---|---|
| D1 | `o instanceof Collection<?>` | 271 |
| L1 | `i.hasNext()` (lus-conditie) | 272 |
| D2 | `i.hasNext()` (scheidingsteken binnen lus) | 274 |
| D3 | `else if o instanceof Date` | 278 |
| D4 | `else if o instanceof Concept` | 280 |
| D5 | `else if o instanceof Obs` | 282 |
| D6 | `else if o instanceof User` | 284 |
| D7 | `else if o instanceof Encounter` | 286 |
| D8 | `else if o instanceof Visit` | 288 |
| D9 | `else if o instanceof Program` | 290 |
| D10 | `else if o instanceof Provider` | 292 |
| D11 | `else if o instanceof Form` | 294 |
| D12 | `else if o instanceof SingleCustomValue<?>` | 296 |
| D13 | `else if o instanceof OpenmrsMetadata` | 298 |
| (else) | fallback `toString()` | 300 |

**McCabe CC = 12 beslissingspunten + 1 = 13** (band 11–20, "moeilijk"). De grafiek staat in `img/printobject-cfg.png`.

## 3. Basispaden

Elke type-tak is een onafhankelijk basispad van start naar eind. De collectie-tak is de enige samengestelde route, omdat de lus (L1) en het scheidingsteken (D2) twee extra paden introduceren.

| Pad | Route | Beschrijving |
|---|---|---|
| P1 | D1=true, L1=false | lege collectie: geen iteratie |
| P2 | D1=true, L1=true, D2=false | collectie met één element: geen scheidingsteken |
| P3 | D1=true, L1=true (2×), D2=true | collectie met meerdere elementen: scheidingsteken `, ` tussen elementen |
| P4 | D1=false, D3=true | Date |
| P5 | D4=true | Concept |
| P6 | D5=true | Obs |
| P7 | D6=true | User |
| P8 | D7=true | Encounter |
| P9 | D8=true | Visit |
| P10 | D9=true | Program |
| P11 | D10=true | Provider |
| P12 | D11=true | Form |
| P13 | D12=true | SingleCustomValue |
| P14 | D13=true | OpenmrsMetadata |
| P15 | alle false | else-fallback: `toString()` van een onbekend type |

## 4. Testcases met voorspelde uitkomst

| TC | Pad | Invoer (object) | Voorspelde uitkomst | Bestaand in FormatTagTest? |
|---|---|---|---|---|
| TC1 | P1 | lege `List` | lege string | nee |
| TC2 | P2 | `List` met 1 Concept | gerenderde concept-naam, geen `, ` | indirect |
| TC3 | P3 | `List` met 2 Concepts | beide namen, gescheiden door `, ` | nee |
| TC4 | P4 | `Date` | datum via `printDate` (locale-formaat) | nee |
| TC5 | P5 | `Concept` | naam via `printConcept` | ja |
| TC6 | P6 | `Obs` | `getValueAsString`, HTML-escaped | indirect |
| TC7 | P7 | `User` | naam via `printUser` | indirect |
| TC8 | P8 | `Encounter` | via `printEncounter` | indirect |
| TC9 | P9 | `Visit` | via `printVisit` | indirect |
| TC10 | P10 | `Program` | via `printProgram` | indirect |
| TC11 | P11 | `Provider` | via `printProvider` | nee |
| TC12 | P12 | `Form` | via `printForm` | indirect |
| TC13 | P13 | `SingleCustomValue` | via `printSingleCustomValue` | nee |
| TC14 | P14 | `OpenmrsMetadata` (bv. `Location`) | via `printMetadata` | indirect |
| TC15 | P15 | object van een niet-ondersteund type (bv. `Integer`) | `toString()`, HTML-escaped | nee |

De rij "bestaand in FormatTagTest" maakt expliciet welke paden al gedekt zijn en welke nieuw moeten worden toegevoegd om volledige takdekking te bereiken. TC1, TC3, TC4, TC11, TC13 en TC15 zijn de gaten in de huidige suite (consistent met de coverage-bevinding in C1: provider- en SingleCustomValue-takken ongedekt).

## 5. Uitvoering en resultaten

De testcases worden geïmplementeerd in `FormatTagTest` (uitbreiding van de bestaande klasse). Uitvoering: `mvn -B test -pl omod -Dtest=FormatTagTest`.

> **In te vullen na uitvoering (vóór de refactor):** uitkomst per TC (groen/rood), en de gemeten branch-coverage van `printObject()` als baseline. Deze baseline is het ijkpunt voor C6; dezelfde suite draait ná de refactor en moet identiek gedrag tonen.

## 6. Relatie tot de refactor

Na de Strategy-refactor (C5) verschuift de dispatch van de `else-if`-keten naar `FormatStrategyRegistry.resolve()`. De testcases hierboven testen het waarneembare gedrag (de output per type), niet de interne structuur — ze blijven dus geldig na de refactor en vormen het bewijs dat het gedrag ongewijzigd is. Daarnaast krijgt elke nieuwe strategy-klasse een eigen directe unit-test, wat de testbaarheid verhoogt (het ISO 25010-subkenmerk dat C1 als zwakste aanwees).
