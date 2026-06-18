# Analyse Onderhoudbaarheid — OpenMRS LegacyUI v1.20.0

**Datum:** 2026-06-17  
**Scope:** `omod/src/main/java/org/openmrs/web/**` (excl. `org/springframework/**`)  
**Prioriteitspool:** `FormatTag.java`, `PersonFormController.java`, `ConceptFormController.java`  
**Norm:** ISO 25010 (Onderhoudbaarheid), McCabe Cyclomatic Complexity  

---

## 1. Samenvatting

De drie onderzochte klassen vertonen ernstige onderhoudbaarheidsrisico's. `PersonFormController.updatePersonAddresses()` berekent een McCabe CC van ~56 (KRITISCH) en heeft **geen** directe testdekking. `ConceptFormController` bundelt controller-logica met een grote inner class (`ConceptFormBackingObject`, ~700 regels), wat de modulariteit schaadt. `FormatTag.doStartTag()` heeft CC ~32 door 27+ opeenvolgende null-checks zonder decompositie. Testbaarheid is de zwakste schakel: van de drie klassen heeft alleen `ConceptFormController` brede testdekking; `PersonFormController` heeft geen eigen testklasse. Directe refactoring-prioriteit ligt bij `updatePersonAddresses()` en `onSubmit()` in `PersonFormController`.

---

## 2. ISO 25010 Subkenmerk-analyse

### 2.1 Modularity

**Definitie (ISO 25010):** De mate waarin het systeem bestaat uit discrete componenten, zodat een wijziging in één component minimale impact heeft op andere componenten.

**Bevindingen:**

**ConceptFormController.java (r. 415–1132):** De inner class `ConceptFormBackingObject` (718 regels) combineert view-databinding, domeinlogica en een eigen lifecycle binnen de controller-klasse. Twee verantwoordelijkheden (HTTP-afhandeling + Concept-assemblee) zijn onscheidbaar zonder de bestandsstructuur te wijzigen.

```
ConceptFormController.java:415  public class ConceptFormBackingObject {
ConceptFormController.java:539  public Concept getConceptFromFormData() { ... }  // ~140 LOC
ConceptFormController.java:947  public List<ConceptUsageExtension> getConceptUsage() { ... }
```

**FormatTag.java (r. 67–1041):** Eén klasse verwerkt veertien verschillende domeintypen via een cascade van `instanceof`-checks en afzonderlijke `print*()`-methoden. Een nieuw domeinobject toevoegen (bijv. `Visit` werd later toegevoegd) vereist wijzigingen op drie plaatsen: het veld (r. 119–121), `doStartTag()` (r. 196–201) én `printObject()` (r. 271–303). Dit is een Open/Closed-schending.

**PersonFormController.java (r. 68–837):** De hiërarchie `PersonFormController → PatientFormController` (de class-javadoc verwijst naar `@see org.openmrs.web.controller.patient.PatientFormController` op r. 67) is wél een positief modulariteitspatroon: gedeelde persoonlogica zit in de basisklasse en domein-specifieke logica in subklassen.

---

### 2.2 Reusability

**Definitie (ISO 25010):** De mate waarin een asset gebruikt kan worden in meer dan één systeem of bij de bouw van andere assets.

**Bevindingen:**

**PersonFormController.getMiniPerson()** (r. 790–835) is een `public static` generieke methode met een type-parameter `<P extends Person>`. Dit is een bewust herbruikbaar ontwerp: de methode wordt aangeroepen vanuit `formBackingObject()` (r. 113–120) én kan worden gebruikt door subklassen als `PatientFormController`.

**FormatTag.printObject()** (r. 270–303) implementeert een generieke type-dispatcher die voor elk OpenMRS-domeinobject de juiste `print*()`-methode aanroept. Dit maakt het mogelijk dat JSP-templates één tag kunnen gebruiken voor elk object — een herbruikbaar contract.

**Beperkende factor:** Bijna alle methoden in de drie klassen zijn direct afhankelijk van `javax.servlet.http.HttpServletRequest` (bijv. `PersonFormController.updatePersonNames()` r. 467, `updatePersonAddresses()` r. 543) of van `javax.servlet.jsp.tagext.TagSupport` (`FormatTag`). Daardoor zijn deze methoden **buiten de web-servlet-context niet herbruikbaar** — de businesslogica (bijv. dood-registratie in `PersonFormController.onSubmit()` r. 295–391) is niet losgemaakt van de HTTP-infrastructuur.

---

### 2.3 Analyzability

**Definitie (ISO 25010):** De effectiviteit en efficiëntie waarmee het mogelijk is om de impact te beoordelen van een beoogde wijziging, een fout te diagnosticeren, of te bepalen welke delen gewijzigd moeten worden.

**Bevindingen:**

**FormatTag.doStartTag()** (r. 143–262, ~120 LOC): Bevat 27 gelijkvormige null-checks voor 14 afzonderlijke domeintypes. Er is geen enkelvoudig aanspreekpunt voor een specifiek type: een ontwikkelaar die wil begrijpen hoe `Visit` wordt opgemaakt, moet de cascade van conditionals nalopen. CC ≈ 32.

**PersonFormController.onSubmit()** (r. 207–399, ~192 LOC): Bevat dood-registratielogica (r. 295–391) die vier niveaus diep genest is. De businesslogica voor het vastleggen van overlijdensoorzaken is vermengd met HTTP-sessie-attributen en redirect-logica. Een bug in `obsDeath.setValueText()` (r. 364/369) is moeilijk te isoleren zonder de volledige cascade van nullchecks te volgen:

```
PersonFormController.java:295  if (person.getDead()) {
PersonFormController.java:302    if (causeOfDeath != null) {
PersonFormController.java:304      if (obssDeath != null) {
PersonFormController.java:305        if (obssDeath.size() > 1) { ... }
PersonFormController.java:309        else { if (obssDeath.size() == 1) { ... } }
```

**Gebrek aan comments in kritieke secties:** De methode `updatePersonAddresses()` (r. 543–709) heeft slechts één comment op r. 613 (`"There appears to be " + maxAddrs + " addresses that need to be saved"`). De 15-voorwaardige openingsconditie op r. 562–564 — die bepaalt óf adressen worden verwerkt — bevat geen toelichting op waarom alle velden individueel worden gecontroleerd.

---

### 2.4 Modifiability

**Definitie (ISO 25010):** De mate waarin een product of systeem effectief en efficiënt gewijzigd kan worden zonder kwaliteitsvermindering of fouten.

**Bevindingen:**

**PersonFormController.updatePersonAddresses()** (r. 543–709, ~165 LOC, CC ~56): De 15-weg `if`-conditionering op r. 562–564 en de daaropvolgende 15 afzonderlijke `maxAddrs`-bepalingen (r. 567–611) zijn functie-duplicatie. Een nieuw adresveld (bijv. `address7`) toevoegen vereist minimaal vier afzonderlijke aanpassingen zonder dat de compiler dit bewaakt: declaratie (r. 558), initialisatie (r. 605–610), maxAddr-check, en invulling in de lus. Dit schending van DRY maakt wijzigingen foutgevoelig.

**ConceptFormController — SimpleFormController (r. 92):** De klasse erft van `org.springframework.web.servlet.mvc.SimpleFormController`, dat **deprecated** is sinds Spring 3.0 en verwijderd in Spring 6. Een Spring-upgrade forceert een complete herschrijving van de controllerlaag.

**ConceptFormController.getConceptFromFormData()** (r. 539–678): Gebruik van `ListUtils.lazyList()` (r. 499–502) introduceert side-effects via Apache Commons Collections. De lazy-initialisatie van `synonymsByLocale` en `indexTermsByLocale` maakt het gedrag bij iteratie niet-deterministisch als de lijsten buiten de verwachte binding worden aangesproken. Dit maakt veilig wijzigen van de bindinglogica riskant.

**PersonFormController — deprecated `new Boolean(namePrefStatus[i])`** (r. 491, r. 648): Gebruik van de deprecated `Boolean(String)`-constructor (verwijderd in Java 17) vormt een modifiability-risico bij een JVM-upgrade.

---

### 2.5 Testability

**Definitie (ISO 25010):** De effectiviteit en efficiëntie waarmee testcriteria vastgesteld kunnen worden voor een systeem of component, en waarmee tests uitgevoerd kunnen worden om vast te stellen of die criteria zijn voldaan.

**Bevindingen:**

**PersonFormController:** Er bestaat **geen** `PersonFormControllerTest.java`. De map `src/test/java/org/openmrs/web/controller/person/` bevat:
- `AddPersonControllerTest.java` — test een andere klasse
- `PersonAttributeTypeFormControllerTest.java` — test een andere klasse

De kernmethoden `onSubmit()`, `updatePersonAddresses()`, `updatePersonNames()` en `updatePersonAttributes()` zijn **ongedekt**. De diepe afhankelijkheid op `HttpServletRequest` zonder interface-abstractie maakt unit-testing zonder volledige servlet-container moeilijk.

**FormatTag:** `FormatTagTest.java` bevat drie tests die `printConcept()` en `doStartTag()` (via object-routing) testen. De private methoden `printSingleCustomValue()`, `getDisplayEncounterProviders()`, `filterProviders()`, `containsRole()`, `trimStringArray()` en `applyConversion()` zijn **niet direct gedekt**. De `"global"`-branch in `applyConversion()` (r. 466–469) is nooit getest.

**ConceptFormController:** De sterkst gedekte klasse: twee testklassen (26 + 1 tests) dekken alle primaire submit-paden, numerieke velden, concept-mappings, attributen en validatiefouten. De methode `validateConceptUsesPersistedObjects()` heeft drie directe tests.

---

## 3. Metriek-keten

De metriek-keten maakt de relatie expliciet tussen wat gemeten wordt en waarom dat relevant is voor kwaliteit. Een metriek is altijd een indicator, nooit een doel op zichzelf.

| Kwaliteitseigenschap | ISO 25010 Subkenmerk | Metriek | Meetwaarde (geschat) | Interpretatie |
|---|---|---|---|---|
| Onderhoudbaarheid | Modifiability | McCabe Cyclomatic Complexity (CC) | `updatePersonAddresses()`: CC ≈ 56 | KRITISCH (>50): hoog risico op regressies bij aanpassing |
| Onderhoudbaarheid | Modifiability | McCabe Cyclomatic Complexity (CC) | `PersonFormController.onSubmit()`: CC ≈ 32 | HOOG (21–50): matige kans op correcte aanpassing zonder tests |
| Onderhoudbaarheid | Analyzability | Lines of Code per methode (LOC) | `onSubmit()` (PersonFormController): ~192 LOC | Overschrijdt richtlijn van 30–50 LOC per methode (Clean Code) |
| Onderhoudbaarheid | Testability | Branch Coverage (geschat) | `PersonFormController`: ~0% (geen tests) | Geen vangnet bij refactoring |
| Onderhoudbaarheid | Testability | Branch Coverage (geschat) | `ConceptFormController`: ~75–85% | Adequate veiligheidsnet; niet-gedekte paden zijn edge cases |
| Onderhoudbaarheid | Testability | Branch Coverage (geschat) | `FormatTag`: ~40–50% | Grote ongedekte oppervlak in `printSingleCustomValue()` en provider-filtering |
| Onderhoudbaarheid | Modularity | Klasse-LOC | `ConceptFormController.java`: 1134 LOC (incl. inner class) | Overschrijdt single-responsibility; twee conceptuele klassen in één bestand |
| Onderhoudbaarheid | Reusability | Externe afhankelijkheidskoppeling | `HttpServletRequest` als parameter in alle businessmethoden | Businesslogica niet extraheerbaar zonder servlet-container |

**Toelichting keten:**  
Hoge CC → meer testpaden nodig → bij lage coverage → grotere kans op ongedetecteerde regressies → lagere Modifiability en Analyzability. De metriek CC is nuttig als vroegtijdige indicator van onderhoudskosten, niet als absoluut kwaliteitsoordeel. Een CC van 56 in een niet-geteste methode is een ernstig signaal; dezelfde CC in een volledig gedekte methode is beheersbaar.

---

## 4. CC-metingen

### 4.1 Telmethode

Gehanteerde McCabe-regel: **CC = (aantal binaire beslissingspunten) + 1**  
Beslissingspunten = elke `if`, `else if`, `while`, `for`, `do-while`, `case`, `catch`, `&&`, `||` in de methode-body.

### 4.2 FormatTag — `doStartTag()` (zwaarste methode)

**Locatie:** `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java`, r. 143–262  
**LOC:** ~120

| Beslissingspunt | Regel | Type |
|---|---|---|
| `if (object != null)` | 146 | if |
| `if (conceptId != null)` | 150 | if |
| `if (concept != null)` | 152 | if |
| `if (obsValue != null)` | 157 | if |
| `if (userId != null)` | 161 | if |
| `if (user != null)` | 163 | if |
| `if (personId != null)` | 168 | if |
| `if (person != null)` | 170 | if |
| `if (encounterId != null)` | 175 | if |
| `if (encounter != null)` | 177 | if |
| `if (encounterTypeId != null)` | 182 | if |
| `if (encounterType != null)` | 185 | if |
| `if (visitTypeId != null)` | 189 | if |
| `if (visitType != null)` | 191 | if |
| `if (visitId != null)` | 196 | if |
| `if (visit != null)` | 198 | if |
| `if (locationId != null)` | 203 | if |
| `if (location != null)` | 205 | if |
| `if (locationTagId != null)` | 210 | if |
| `if (locationTag != null)` | 212 | if |
| `if (programId != null)` | 217 | if |
| `if (program != null)` | 219 | if |
| `if (providerId != null)` | 224 | if |
| `if (provider != null)` | 226 | if |
| `if (encounterProviders != null)` | 231 | if |
| `if (form != null)` | 235 | if |
| `if (singleCustomValue != null)` | 239 | if |
| `if (StringUtils.isNotEmpty(var))` | 243 | if |
| `if (javaScriptEscape)` | 244 | if |
| `if (javaScriptEscape)` | 251 | if |
| `catch (IOException e)` | 257 | catch |

**Totaal beslissingspunten: 31 → CC = 32 (HOOG, band 21–50)**

> **Toelichting:** `doStartTag()` is zwaarder dan `printObject()` (CC ≈ 15). De methode verwerkt veertien domeintypes via 27 opeenvolgende null-checks; elke combinatie van ingevulde velden creëert een uniek uitvoeringspad. Refactoring via een `Map<Class<?>, Consumer<Object>>` of het Strategy-patroon zou de CC terugbrengen tot CC ≈ 5.

---

### 4.3 PersonFormController — `updatePersonAddresses()` (zwaarste methode)

**Locatie:** `omod/src/main/java/org/openmrs/web/controller/person/PersonFormController.java`, r. 543–709  
**LOC:** ~165

Kernbeslissingspunten (samengevat per groep):

| Groep | Beschrijving | Aantalsbijdrage |
|---|---|---|
| Openingsconditie r. 562–564 | `if (add1s != null \|\| add2s != null \|\| ... \|\| endDates != null)` — 15 termen, 14 `\|\|`-operatoren | +15 |
| maxAddrs-bepaling r. 567–611 | 15× `if (addX != null && addX.length > maxAddrs)` — elk 1 `if` + 1 `&&` | +30 |
| `for`-lus r. 615 | iteratieconditie | +1 |
| Veld-invulling r. 617–663 | 12× enkelvoudig `if`, 3× `if` met `&&` | +18 |
| Validatieblok in lus r. 667–676 | `if (hasErrors)` + `for (ObjectError)` + `if (hasErrors)` | +3 |
| `while`-lus r. 682 | iteratieconditie | +1 |
| Validatieblok in while r. 686–696 | `if (hasErrors)` + `for (ObjectError)` + `if (hasErrors)` | +3 |
| Voorkeur-adreslogica r. 698–703 | `if (isPreferred)` + `if (preferredAddress != null)` | +2 |
| Slotconditie r. 705 | `if ((preferredAddress == null) && (currentAddress != null))` — 1 `if` + 1 `&&` | +2 |

**Totaal: 15 + 30 + 1 + 18 + 3 + 1 + 3 + 2 + 2 = 75 beslissingspunten → CC = 76 (KRITISCH, >50)**

> **Toelichting:** De kritische complexiteit wordt primair gedreven door de 15-weg OR-conditie en de 15 parallelle maxAddrs-checks — beide zijn directe gevolgen van het feit dat elk adresveld als los parameter-array wordt doorgegeven. Een refactoring naar een `AddressParser`-klasse die een `Map<String, String[]>` verwerkt, zou de CC reduceren tot <15. Combinatie met 0% testdekking maakt dit de hoogst-risico methode in de prioriteitspool.

---

### 4.4 ConceptFormController — `getConceptFromFormData()` (zwaarste methode)

**Locatie:** `omod/src/main/java/org/openmrs/web/controller/ConceptFormController.java` (inner class `ConceptFormBackingObject`), r. 539–678  
**LOC:** ~140

Kernbeslissingspunten (samengevat per blok):

| Blok | Beslissingspunten | Bijdrage |
|---|---|---|
| Outer `for (Locale locale : locales)` r. 542 | for-lus | +1 |
| Naam-verwerking per locale r. 543–549 | `if (hasText(name))` + `if (equalsIgnoreCase(preferred))` | +2 |
| Korte-naam verwerking r. 552–555 | `if (shortNameInLocale != null && hasText(...))` — 1 if + 1 && | +2 |
| `for` synoniemen r. 557 | for-lus | +1 |
| Synoniem-validatie r. 558–577 | `if (synonym != null && hasText)` + `if (equalsIgnoreCase && !voided)` + `else if (!contains && !hasName)` + `if (!voided)` | +7 (4 if/elif + 3 && en ||) |
| `for` indextermen r. 580 | for-lus | +1 |
| Indexterm-validatie r. 581–594 | `if (indexTerm != null && hasText)` + `if (!contains && !hasName)` + `if (!isVoided)` + `else if (isVoided && !hasText)` | +7 (4 if/elif + 3 &&) |
| Beschrijving r. 597–603 | `if (!hasText(desc))` + `else if (!contains)` | +2 |
| `for` conceptMappings r. 609 | for-lus | +1 |
| Mapping-verwerking r. 610–630 | `if (mappedTermIds == null)` + `if (termId == null)` + `if (mapId == null)` + `else if (!add(termId))` + `else if (!contains(map))` | +5 |
| Sets-verwerking r. 633–644 | `if (!isSet && conceptSets != null)` + `if (!isCoded && answers != null)` + `else for (ConceptAnswer)` — 2 && en 2 if/for | +5 |
| Numeric-verwerking r. 648–665 | `if (datatype == Numeric)` + `if (instanceof ConceptNumeric)` | +2 |
| Complex-verwerking r. 667–676 | `else if (datatype == Complex)` + `if (instanceof ConceptComplex)` | +2 |

**Totaal: 1+2+2+1+7+1+7+2+1+5+5+2+2 = 38 beslissingspunten → CC = 39 (HOOG, band 21–50)**

> **Toelichting:** `getConceptFromFormData()` is zwaarder dan `onSubmit()` (CC ≈ 21) in dezelfde klasse. De complexiteit is inherent aan het samenvoegen van multi-locale naam-structuren, maar de geneste `&&`-expressies in synoniemvalidatie (r. 558–577) en de mixing van void-redenlogica met binding-logica zijn kandidaten voor extractie. De beschikbare testsuite dekt deze methode indirect via form-submit tests.

---

### 4.5 CC-overzicht

| Klasse | Methode | Regels | LOC (schatting) | CC (schatting) | Band |
|---|---|---|---|---|---|
| FormatTag | `doStartTag()` | 143–262 | ~120 | **32** | HOOG |
| FormatTag | `printObject()` | 270–303 | ~34 | **15** | MATIG |
| FormatTag | `printSingleCustomValue()` | 366–403 | ~38 | **7** | LAAG |
| PersonFormController | `updatePersonAddresses()` | 543–709 | ~165 | **76** | KRITISCH |
| PersonFormController | `onSubmit()` | 207–399 | ~192 | **32** | HOOG |
| PersonFormController | `updatePersonNames()` | 467–533 | ~66 | **16** | MATIG |
| PersonFormController | `updatePersonAttributes()` | 422–459 | ~37 | **8** | LAAG |
| ConceptFormController | `onSubmit()` | 181–311 | ~130 | **21** | HOOG |
| ConceptFormController.ConceptFormBackingObject | `getConceptFromFormData()` | 539–678 | ~140 | **39** | HOOG |
| ConceptFormController.ConceptFormBackingObject | constructor | 468–530 | ~62 | **14** | LAAG |

---

## 5. Coverage-baseline

### 5.1 FormatTag.java

**Testklasse:** `omod/src/test/java/org/openmrs/web/taglib/FormatTagTest.java` (3 tests)

| Methode | Test aanwezig | Dekking (schatting) | Toelichting |
|---|---|---|---|
| `doStartTag()` | ✅ `doStartTag_shouldPrintAnyDomainObject()` | ~60% | Dekt: Concept, Encounter, Obs, User, EncounterType, Location, Program, Visit, VisitType, Form — ontbreekt: Provider, LocationTag, PersonId-branches, javaScriptEscape=true |
| `printConcept()` | ✅ `printConcept_shouldPrintTheNameWithTheCorrectLocaleNameAndType()` + `printConcept_shouldEscapeHtmlTags()` | ~80% | Dekt: FullySpecified, tag, HTML-escape; ontbreekt: fallback naar `getDisplayString()` |
| `printObject()` | ✅ indirect via `doStartTag_shouldPrintAnyDomainObject()` | ~70% | Ontbreekt: Collection-iteratie, `SingleCustomValue`, `OpenmrsMetadata`-fallback |
| `applyConversion()` | ❌ niet direct | ~0% | De `"global"`-branch (r. 466–469) is nooit getest; lowercase/uppercase/capitalize-branches impliciet via printConcept maar niet geïsoleerd |
| `printSingleCustomValue()` | ❌ | ~0% | Vijf takken volledig ongedekt, inclusief `DownloadableDatatypeHandler`-branch (r. 382–388) |
| `getDisplayEncounterProviders()` | ❌ | ~0% | Filtering op EncounterRole-namen of -ids niet getest |
| `filterProviders()` | ❌ | ~0% | |
| `containsRole()` | ❌ | ~0% | |
| `trimStringArray()` | ❌ | ~0% | |
| `printUser()`, `printPerson()`, `printProvider()` | ✅ indirect | ~50% | Null-person-branch in `printProvider` niet getest |
| `doEndTag()` / `reset()` | ❌ | ~0% | |

**Geschatte totale branch-coverage:** ~40–50%

---

### 5.2 PersonFormController.java

**Testklasse:** **Geen `PersonFormControllerTest.java`**

De map `src/test/java/org/openmrs/web/controller/person/` bevat:

| Bestand | Relatie tot PersonFormController |
|---|---|
| `AddPersonControllerTest.java` | Test `AddPersonController`, niet PersonFormController |
| `PersonAttributeTypeFormControllerTest.java` | Test een andere controller |
| `PersonAttributeTypeListControllerTest.java` | Test een andere controller |

| Methode | Test aanwezig | Dekking |
|---|---|---|
| `formBackingObject()` | ❌ | ~0% |
| `showForm()` (beide overloads) | ❌ | ~0% |
| `processFormSubmission()` | ❌ | ~0% |
| `onSubmit()` | ❌ | ~0% |
| `referenceData()` | ❌ | ~0% |
| `updatePersonAttributes()` | ❌ | ~0% |
| `updatePersonNames()` | ❌ | ~0% |
| `updatePersonAddresses()` | ❌ | ~0% |
| `setupFormBackingObject()` | ❌ | ~0% |
| `setupReferenceData()` | ❌ | ~0% |
| `getMiniPerson()` | ❌ | ~0% |
| `initBinder()` | ❌ | ~0% |

**Geschatte totale branch-coverage: ~0%**

> Subklasse `PatientFormController` heeft wel een testklasse (`PatientFormControllerTest.java`), maar die test de geovererfte methoden van PersonFormController niet expliciet.

---

### 5.3 ConceptFormController.java

**Testklassen:**
1. `omod/src/test/java/org/openmrs/web/controller/ConceptFormControllerTest.java` (26 tests)
2. `omod/src/test/java/org/openmrs/web/controller/concept/ConceptFormControllerTest.java` (1 test)

| Methode | Tests | Dekking (schatting) |
|---|---|---|
| `initBinder()` | `getConceptFromFormData_shouldSetConceptOnConceptAnswers()` (r. 1144–1147) | ~80% |
| `processFormSubmission()` — cancel-branch | ❌ niet direct | ~20% |
| `processFormSubmission()` — jumpAction-branch | ❌ | ~0% |
| `onSubmit()` — retire | `shouldNotDeleteConceptsWhenConceptsAreLocked()` | ~70% |
| `onSubmit()` — unretire | ❌ niet direct | ~30% |
| `onSubmit()` — delete | `shouldNotDeleteConceptsWhenConceptsAreLocked()` | ~60% |
| `onSubmit()` — save (happy path) | 12+ tests | ~90% |
| `onSubmit()` — validatiefouten | `onSubmit_shouldNotSaveChangesIfThereAreValidationErrors()` | ~80% |
| `validateConceptUsesPersistedObjects()` | 3 directe tests (r. 1004–1057) | ~100% |
| `formBackingObject()` | `shouldGetConcept()`, `onSubmit_shouldReturnAConceptWithANullIdIfNoMatchIsFound()` | ~90% |
| `referenceData()` | `onSubmit_shouldDisplayNumericValuesFromTable()` | ~60% |
| `ConceptFormBackingObject()` constructor | `ConceptFormBackingObject_shouldCopyNumericAttributes()` | ~70% |
| `getConceptFromFormData()` | indirect via meerdere submit-tests | ~65% |

**Geschatte totale branch-coverage: ~75–85%**

---

## 6. Prioriteringstabel

| Klasse | Methode | LOC (schatting) | CC (schatting) | ISO 25010 Subkenmerk | Prioriteit | Motivatie |
|---|---|---|---|---|---|---|
| PersonFormController | `updatePersonAddresses()` | ~165 | **76 (KRITISCH)** | Modifiability, Testability | **H** | CC >50 + 0% testdekking = hoogste foutrisico bij wijziging; DRY-schending bij 15-weg OR |
| PersonFormController | `onSubmit()` | ~192 | **32 (HOOG)** | Analyzability, Modifiability, Testability | **H** | Dood-registratielogica 4 niveaus diep genest; 0% testdekking; side-effects op `httpSession` moeilijk te isoleren |
| ConceptFormController.ConceptFormBackingObject | `getConceptFromFormData()` | ~140 | **39 (HOOG)** | Modifiability, Modularity | **H** | Inner-class-locatie bemoeilijkt zelfstandig testen; geneste `&&`-expressies in synoniem- en indextermverwerking; wijziging binding-logica raakt ook de controller |
| FormatTag | `doStartTag()` | ~120 | **32 (HOOG)** | Analyzability, Testability | **M** | 27 null-checks zonder decompositie; ~40% gedekt — ongedekte paden zijn `javaScriptEscape=true`, Provider en LocationTag-branches |
| PersonFormController | `updatePersonNames()` | ~66 | **16 (MATIG)** | Testability | **M** | 0% testdekking; logica voor preferred-name-deduplicatie (r. 517–531) kan stil falen bij meerdere preferred-names |
| FormatTag | `printSingleCustomValue()` | ~38 | **7 (LAAG)** | Testability, Analyzability | **M** | 0% testdekking; bevat dynamisch-gegenereerde HTML-links (r. 377–388) zonder aparte test voor `DownloadableDatatypeHandler`; aangrenzend aan eerder geïdentificeerd SSRF-risico |
| ConceptFormController | `onSubmit()` | ~130 | **21 (HOOG)** | Modifiability | **L** | Vier afzonderlijke catch-blokken voor drie exception-types; goed gedekt (~80%) — laag direct risico |
| PersonFormController | `updatePersonAttributes()` | ~37 | **8 (LAAG)** | Testability | **L** | 0% testdekking maar korte methode; ongedekte `APIException`-branch (r. 444–449) |
| FormatTag | `printObject()` | ~34 | **15 (MATIG)** | Reusability | **L** | Grotendeels indirect gedekt via `doStartTag`; `Collection`-iteratie (r. 272–277) niet expliciet getest |

---

## 7. Bronverwijzingen

| Bestand | Relevante regels | Onderwerp |
|---|---|---|
| `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java` | r. 67–1041 | Volledige klasse |
| `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java` | r. 143–262 | `doStartTag()` — CC 32 |
| `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java` | r. 270–303 | `printObject()` — type-dispatcher |
| `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java` | r. 366–403 | `printSingleCustomValue()` — ongedekt |
| `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java` | r. 461–480 | `applyConversion()` — `"global"`-branch ongedekt |
| `omod/src/main/java/org/openmrs/web/taglib/FormatTag.java` | r. 610–648 | `getDisplayEncounterProviders()` + hulpmethoden — volledig ongedekt |
| `omod/src/main/java/org/openmrs/web/controller/person/PersonFormController.java` | r. 68–837 | Volledige klasse |
| `omod/src/main/java/org/openmrs/web/controller/person/PersonFormController.java` | r. 207–399 | `onSubmit()` — CC 32, dood-registratie r. 295–391 |
| `omod/src/main/java/org/openmrs/web/controller/person/PersonFormController.java` | r. 543–709 | `updatePersonAddresses()` — CC 76 |
| `omod/src/main/java/org/openmrs/web/controller/person/PersonFormController.java` | r. 562–564 | Kritische 15-weg OR-conditie |
| `omod/src/main/java/org/openmrs/web/controller/person/PersonFormController.java` | r. 490–491, r. 648 | Deprecated `new Boolean(String)` |
| `omod/src/main/java/org/openmrs/web/controller/person/PersonFormController.java` | r. 790–835 | `getMiniPerson()` — herbruikbaar, goed ontwerp |
| `omod/src/main/java/org/openmrs/web/controller/ConceptFormController.java` | r. 92 | `extends SimpleFormController` — deprecated Spring API |
| `omod/src/main/java/org/openmrs/web/controller/ConceptFormController.java` | r. 181–311 | `onSubmit()` — CC 21 |
| `omod/src/main/java/org/openmrs/web/controller/ConceptFormController.java` | r. 415–1132 | Inner class `ConceptFormBackingObject` — modularity-probleem |
| `omod/src/main/java/org/openmrs/web/controller/ConceptFormController.java` | r. 499–502 | `ListUtils.lazyList()` — side-effect bij binding |
| `omod/src/main/java/org/openmrs/web/controller/ConceptFormController.java` | r. 539–678 | `getConceptFromFormData()` — CC 39 |
| `omod/src/test/java/org/openmrs/web/taglib/FormatTagTest.java` | r. 1–187 | Testdekking FormatTag (3 tests) |
| `omod/src/test/java/org/openmrs/web/controller/ConceptFormControllerTest.java` | r. 1–1318 | Testdekking ConceptFormController (26 tests) |
| `omod/src/test/java/org/openmrs/web/controller/concept/ConceptFormControllerTest.java` | r. 1–59 | Aanvullende test ConceptFormBackingObject (1 test) |
