# Risico-evaluatie CI-CD-proces — legacyui-audit (Sprint 2, punt 5)

**Module:** OpenMRS Legacy UI v1.20.0 · **Repo:** `Polen-Fan-Club/legacyui-audit` · **Schaal:** `risicocriteria.md` (5×5, variant B)

Scope: risico's van de pipeline zelf, niet de applicatiecode (die staat in gap-analyse + RAR). Afgeleid uit `.github/workflows/` en `.github/dependabot.yml`.

Variant B (`risicocriteria.md` §6): grens ≥10 onacceptabel waar patiëntveiligheid/-data geraakt wordt, anders ≥15. Een pipeline-risico raakt patiëntdata indirect — via een compromis dat in een productie-artefact landt. Per risico is beoordeeld of die brug realistisch is; alleen dan geldt variant B.

## 1. Risico's en scoring

| # | Risico | Bron | Kans | Impact | Score | Oordeel |
|---|---|---|---|---|---|---|
| R1 | `continue-on-error` maskeert een stuk-gelopen scan | `codeql.yml` r.55, `snyk-sca.yml` r.69 | 4 | 4 | **16** | Rood |
| R2 | Actions op mutabele tag i.p.v. SHA (supply-chain) | alle `uses:` op `@v4`/`@v3` | 2 | 5 | **10** | Rood (B) |
| R3 | `SNYK_TOKEN` enkel secret, geen rotatie/scoping | `snyk-sca.yml` r.78/92 | 2 | 4 | **8** | Oranje |
| R4 | Build alleen op EOL JDK 8 | setup-java `java-version: "8"` | 4 | 3 | **12** | Oranje |
| R5 | Triagebeleid zonder `.snyk`-policyfile | `false-positive-beleid.md` verwijst ernaar; bestand ontbreekt | 3 | 3 | **9** | Oranje |
| R6 | Branch protection/secret onzeker na org-migratie | niet verifieerbaar uit repo-export | 2 | 4 | **8** | Oranje |

R1 en R2 vallen onder variant B (compromis kan in prod-code landen). R3/R4/R5 blijven binnen het CI-proces of raken patiëntdata alleen via een lange keten → generieke grens.

## 2. Risicomatrix

![Risicomatrix CI-CD-proces](docs/img/matrix.png)

## 3. Bow-tie kritiekste risico (R1)

R1 (score 16): een vulnerability-scan loopt stuk of levert geen artefact, terwijl `continue-on-error: true` de run groen houdt. Gevolg: een high-severity CVE landt ongedetecteerd in een productie-artefact.
![Bow-tie R1](docs/img/bowtie.png)


Preventief: een aparte gate-stap die faalt op een ontbrekend artefact (i.p.v. `if-no-files-found: warn`), exit-code-diagnose ook in CodeQL, en `snyk monitor` voor een server-side snapshot. Correctief: de wekelijkse scheduled run, Dependabot security updates, en handmatige verificatie van het artefact in de gemergede run.

## 4. Doorkoppeling

- R1, R2 → security backlog (punt 7), hoge prioriteit; R1 direct in de pipeline-config te fixen.
- R4 → NEN-7510 8.8 + maintainability-spoor (ISO 25010).
- R5 → `.snyk`-policyfile aanmaken bij de eerste triage-beslissing.
- R3, R6 → verifiëren in de GitHub UI (niet uit de repo-export vast te stellen).

---

*Basis: `risicocriteria.md` (variant B). Geverifieerd tegen `.github/workflows/` op 2026-06-07. Issue #29.*