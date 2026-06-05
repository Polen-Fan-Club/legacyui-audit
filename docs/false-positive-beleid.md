# Triagebeleid SCA & SAST-findings

Scope: OpenMRS legacyui-module v1.20.0 (`Serino2/legacyui-audit`).
NEN-7510:2024-2 8.8 — kwetsbaarheden tijdig identificeren, beoordelen en verhelpen; doorlooptijd (detectie → patch) documenteren.

## Status per finding

Een teamlid beoordeelt elke finding; de pipeline breekt niet automatisch op findings.

- **Fix** — reëel en relevant. Patchen via dependency-update. Detectie- en patchdatum vastleggen.
- **Accept** — reëel maar niet exploiteerbaar binnen deze module, of patch onevenredig. Onderbouwing verplicht, koppelen aan de risicobereidheid (punt 1). Herbeoordelen bij volgende scan.
- **False positive** — klopt feitelijk niet (verkeerde versiematch, niet-bestaand codepad, elders gemitigeerd). Onderdrukken mét onderbouwing.
- **Defer** — relevant maar lagere prioriteit dan de risicobereidheid vereist. Naar de security backlog (punt 7) met severity en reden.

## Onderdrukken — herleidbaar

- **Snyk:** via `.snyk`-policyfile in de repo-root. Elke ignore krijgt een `reason` en `expires`. De policyfile staat in versiebeheer, dus de onderdrukking is een commit.
- **Dependabot:** alert dismissen mét reden; de reden blijft zichtbaar in de repo.

**Regel: geen onderdrukking zonder schriftelijke onderbouwing.**

## Prioritering

CVSS-score × kritiekheid van het systeem (WS02). Concrete deadlines komen uit de risicobereidheid/grenswaarden van punt 1; dit document verwijst ernaar en herhaalt ze niet.
