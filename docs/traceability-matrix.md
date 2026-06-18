# Traceability matrix

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** (NEN-7510 5.12) · Issue #54

Elke bevinding is herleidbaar van bron tot hertest. CVSS is de technische ernst; het besluit volgt de risicocriteria (`risicocriteria.md` §6: grens ≥10 bij patiëntdata, anders ≥15).

| # | Bevinding | Bron | CVSS | Besluit | Fix | Hertest | NEN |
|---|---|---|---|---|---|---|---|
| 1 | Sessie-IP-binding dood codepad (`RequireTag` checkt server-IP) | gap 8.3-6 → T1 | 7.4 | Oplossen | `7e1c705` (PR #50) | IP-mismatch blokkeert nu — `T1/hertest/` | 8.3 |
| 2 | Brute-force-drempel 100, in-memory | gap 8.5-5 → T2 | 6.5 | Accepteren¹ | — | — | 8.5 |
| 3 | Admin-acties niet ge-audit-logd | gap 8.15-4/5/7/8/11 → T3 | 5.3² | Oplossen | `c693cb2` (PR #50) | 10 events loggen `AUDIT` op WARN (doorontw. `0421a10`/PR #52) — `T3/hertest/` + `AuditLoggingTest`/`LogoutServletAuditTest` | 8.15 |
| 4 | Gebruikersenumeratie via forgot-password | gap 8.5-6 → T5 | 5.3 | Accepteren¹ | — | — | 8.5 |
| 5 | Reset-flow niet voltooibaar; tijdelijk wachtwoord zonder vervaltermijn | gap 8.5-6 → T5 | 5.3² | Oplossen | `9ab74ee` (PR #50) | Flow voltooit, forced-change grijpt in — `T5/hertest/` | 8.5 |
| 6 | Information disclosure (stacktrace, Tomcat-banner) | T4/T6 (pentest) | 5.3 | Oplossen (a); scope-grens (b)³ | `9c14af3` (PR #50) | Generieke 403, markers 0 — `T6/hertest/` | 8.9³ |

¹ Onder de grenswaarde; onderbouwing in `pentestplan.md` §3. Herbeoordelen bij volgende scan.
² CVSS onderschat deze findings (geen metriek voor auditability-verlies); besluit weegt via NEN en risicocriteria, niet via de score.
³ 8.9 zat niet in de gap-analyse-scope (8.3/8.5/8.15); toegevoegd op basis van de pentest. Deel (b) is container-configuratie, buiten de module.

---

## Pipeline- en procescontroles

Aanvullende NEN-7510:2024-2 controls die niet uit pentest-bevindingen voortkomen maar op pipeline- en procesniveau zijn geborgd:

| Control | Maatregel | Vóór (baseline) | Aanpassing | Na (bewijs-artefact) |
|---|---|---|---|---|
| **8.28** Veilig coderen | CodeQL SAST-scan op volledige module bij elke PR en wekelijks; SARIF-output als CI-artefact | Geen statische analyse actief | `.github/workflows/codeql.yml` opgezet (sprint 1) | CI-artefact `codeql-sarif` (SARIF 2.1.0, 592 bestanden, 76 security-queries) · `mini-complianceverslag.md` M2 |
| **8.8** Kwetsbaarhedenbeheer | Snyk SCA wekelijkse scan + Dependabot security-update-PR's | Geen geautomatiseerde dependency-scan | `.github/workflows/snyk-sca.yml` + Dependabot ingeschakeld (sprint 1) | CI-artefact `snyk-sca-report` (128 CVEs vastgesteld) · Dependabot PR's #10–14 · `sbom-supply-chain.md` |
| **8.25** Beveiliging in de ontwikkelcyclus | Branch protection op `main` (PR verplicht, CI moet groen zijn); SBOM + SAST + SCA in elke build | Geen geformaliseerde secure-pipeline | Pipeline ingericht sprint 1; bewust gedocumenteerd in sprint 2 | `mini-complianceverslag.md` M1–M7 + bewijsregister §5 · commit `10f9a4a` (secrets-hygiëne) · actieve ruleset `main` |
| **5.35** Naleving en beoordeling | Dit auditrapport zelf vormt de beoordelingsartefact; aanpak vastgelegd in `audit-methodologie.md` | Geen formele auditcyclus | Audit uitgevoerd conform NEN-7510:2024-2 (sprint 1–3) | `docs/bijlagen.md` (index A–W) · `docs/audit-methodologie.md` · `docs/conclusie-advies.md` |

Bewijspaden zijn relatief aan `docs/pentest-bewijs/`. Details per bevinding: `pentestplan.md` §3, resultaten §5.

**T4 — weerlegde hypothese.** Privilege-escalation via controllers bleek niet mogelijk: de service-laag-AOP weigert vóór verwerking. Geen bevinding; bewijs in `T4/`.

**Deployment-voorwaarde.** #5 vereist een actieve `ForcePasswordChangeFilter`; zonder die configuratie werkt de fix niet in productie (`pentestplan.md` §4). De eerdere #3-voorwaarde rond het legacyui-logniveau is achterhaald sinds `0421a10`: de audit-events loggen nu op WARN en zijn zichtbaar onder het default distro-logbeleid.

**NEN-dekking (pentest-bevindingen):** 8.3, 8.5, 8.9, 8.15 · **NEN-dekking (pipeline/proces):** 8.28, 8.8, 8.25, 5.35 · **Totaal: 8 controls gedekt.**
