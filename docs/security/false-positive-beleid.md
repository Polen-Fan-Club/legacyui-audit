# Triagebeleid SCA & SAST-findings

Scope: OpenMRS legacyui-module v1.20.0 (`Polen-Fan-Club/legacyui-audit`).
NEN-7510:2024-2 8.8 — kwetsbaarheden tijdig identificeren, beoordelen en verhelpen; doorlooptijd (detectie → patch) documenteren.

## Status per finding

Een teamlid beoordeelt elke finding; de pipeline breekt niet automatisch op findings.

- **Fix** — reëel en relevant. Patchen via dependency-update. Detectie- en patchdatum vastleggen.
- **Accept** — reëel maar niet exploiteerbaar binnen deze module, of patch onevenredig. Onderbouwing verplicht, koppelen aan de risicobereidheid (punt 1). Herbeoordelen bij volgende scan.
- **False positive** — klopt feitelijk niet (verkeerde versiematch, niet-bestaand codepad, elders gemitigeerd). Onderdrukken mét onderbouwing.
- **Defer** — relevant maar lagere prioriteit dan de risicobereidheid vereist. Naar de security backlog (punt 7) met severity en reden.

## Onderdrukken — herleidbaar

- **Snyk:** elke onderdrukte finding krijgt een onderbouwing (`reason`) en, waar van toepassing, een hertoetsmoment vastgelegd bij de triage-beslissing. De triage is momenteel handmatig; het formaliseren in een `.snyk`-policyfile in versiebeheer (zodat elke onderdrukking een commit-spoor krijgt) staat als gepland backlog-item B13 (`security-backlog.md`, CI-CD R5).
- **Dependabot:** alert dismissen mét reden; de reden blijft zichtbaar in de repo.

> **Noot — Dependency Review Action niet inzetbaar.** De `actions/dependency-review-action` vereist GitHub Advanced Security. Code Scanning voor private repositories is een betaalde Enterprise-feature ("Contact sales") — geverifieerd op zowel een persoonlijk Pro-account als in een organisatie; een org maken lost dit niet op. De repo public maken zou GHAS gratis geven, maar is onwenselijk voor een vertrouwelijkheidsaudit op een zorgmodule. De PR-gate-functie wordt daarom gedekt door Dependabot security updates (automatische patch-PR's) + Snyk-scanrapport, niet door de Dependency Review Action.

**Regel: geen onderdrukking zonder schriftelijke onderbouwing.**

## Categorieën van false positives

Een finding krijgt de status **False positive** als aan één van de volgende criteria voldaan is. Per onderdrukking (in de handmatige triage-onderbouwing of een Dependabot-dismiss) wordt de categorie expliciet benoemd.

| Categorie | Definitie | Toewijzingscriterium |
|---|---|---|
| **Unreachable code** | De kwetsbare coderegel of -methode is in de module niet aanroepbaar via enig uitvoerpad. | Statische analyse (CodeQL-call-graph, handmatige trace) toont aan dat de aanroep van buitenaf niet mogelijk is. Code-geverifieerd, niet aangenomen. |
| **Parameterized queries** | SCA/SAST rapporteert SQL-injection maar alle databaseaanroepen gebruiken aantoonbaar geparametriseerde queries of ORM-binding. | Grep op de aangemerkte klasse bevestigt uitsluitend gebruik van `PreparedStatement`, Hibernate-HQL-parameters of equivalente binding; geen directe stringconcatenatie in SQL. |
| **Sanitization buiten scope** | De input-validatie of escaping vindt plaats in een laag die buiten de auditscope valt (bijv. openmrs-core-framework of de JSP-taglib) maar wél effectief is. | Scope-grens gedocumenteerd (`scope-context.md`); de sanitization is verifieerbaar actief (code-referentie of test). |
| **Component niet in runtime** | Een kwetsbaar component staat in de SBOM maar wordt niet meegeladen in de runtime van de legacyui-module (bijv. test-scope, provided-scope zonder gebruik). | Maven-scope `test` of `provided` geverifieerd in `pom.xml`; geen runtime-aanroep aantoonbaar via dependency-trace. |
| **Vendor heeft al gepatcht** | De CVE is gerapporteerd voor een versie die de module gebruikt, maar de vendor heeft een patch uitgebracht die via de gekozen upgrade-route direct beschikbaar is én geen buildbreuk veroorzaakt. | CVE-status in NVD: "Analyzed" met fix-versie ≤ beschikbare upgrade; CI-build slaagt op de gepatchte versie. Patchdatum vastleggen. |

**Regel:** een false positive zonder gedocumenteerde categorie en code-bewijs wordt behandeld als Accept (reëel, niet exploiteerbaar) — nooit stilzwijgend weggelaten.

## Prioritering

CVSS-score × kritiekheid van het systeem (WS02). Concrete deadlines komen uit de risicobereidheid/grenswaarden van punt 1; dit document verwijst ernaar en herhaalt ze niet.