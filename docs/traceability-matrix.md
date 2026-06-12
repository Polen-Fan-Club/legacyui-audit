# Traceability matrix

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** (NEN-7510 5.12) · Issue #54

Elke bevinding is herleidbaar van bron tot hertest. CVSS is de technische ernst; het besluit volgt de risicocriteria (`risicocriteria.md` §6: grens ≥10 bij patiëntdata, anders ≥15).

| # | Bevinding | Bron | CVSS | Besluit | Fix | Hertest | NEN |
|---|---|---|---|---|---|---|---|
| 1 | Sessie-IP-binding dood codepad (`RequireTag` checkt server-IP) | gap 8.3-6 → T1 | 7.4 | Oplossen | `7e1c705` (PR #50) | IP-mismatch blokkeert nu — `T1/hertest/` | 8.3 |
| 2 | Brute-force-drempel 100, in-memory | gap 8.5-5 → T2 | 6.5 | Accepteren¹ | — | — | 8.5 |
| 3 | Admin-acties niet ge-audit-logd | gap 8.15-4/5/7/8/11 → T3 | 5.3² | Oplossen | `c693cb2` (PR #50) | 10 events loggen AUDIT-INFO — `T3/hertest/` | 8.15 |
| 4 | Gebruikersenumeratie via forgot-password | gap 8.5-6 → T5 | 5.3 | Accepteren¹ | — | — | 8.5 |
| 5 | Reset-flow niet voltooibaar; tijdelijk wachtwoord zonder vervaltermijn | gap 8.5-6 → T5 | 5.3² | Oplossen | `9ab74ee` (PR #50) | Flow voltooit, forced-change grijpt in — `T5/hertest/` | 8.5 |
| 6 | Information disclosure (stacktrace, Tomcat-banner) | T4/T6 (pentest) | 5.3 | Oplossen (a); scope-grens (b)³ | `9c14af3` (PR #50) | Generieke 403, markers 0 — `T6/hertest/` | 8.9³ |

¹ Onder de grenswaarde; onderbouwing in `pentestplan.md` §3. Herbeoordelen bij volgende scan.
² CVSS onderschat deze findings (geen metriek voor auditability-verlies); besluit weegt via NEN en risicocriteria, niet via de score.
³ 8.9 zat niet in de gap-analyse-scope (8.3/8.5/8.15); toegevoegd op basis van de pentest. Deel (b) is container-configuratie, buiten de module.

Bewijspaden zijn relatief aan `docs/pentest-bewijs/`. Details per bevinding: `pentestplan.md` §3, resultaten §5.

**T4 — weerlegde hypothese.** Privilege-escalation via controllers bleek niet mogelijk: de service-laag-AOP weigert vóór verwerking. Geen bevinding; bewijs in `T4/`.

**Deployment-voorwaarden.** #3 vereist `org.openmrs.web` op INFO in het logbeleid; #5 vereist een actieve `ForcePasswordChangeFilter`. Zonder die configuratie werken de fixes niet in productie (`pentestplan.md` §4).

**NEN-dekking:** 8.3, 8.5, 8.9, 8.15 (eis: ≥3 controls).
