# Risicocriteria — legacyui-audit (Deel 2, punt 1)

**Module:** OpenMRS Legacy UI v1.20.0 · **Normenkader:** NEN-7510:2024-2 · **Methode:** ISO/IEC 27005 (WS03)

Schaal en grenswaarden volgen WS03/ISO 27005; de niveaudefinities per score zijn onze invulling voor de legacyui-context.

## 1. Risicoberekening

**Risico = Kans × Impact** (1–5 × 1–5 → 1–25).
- **Kans = Blootstelling × Waarschijnlijkheid** — is de kwetsbaarheid aanwezig/bereikbaar, en hoe groot is de kans dat iemand die benut?
- **Impact:** ernst voor de CIA-triad (vertrouwelijkheid, integriteit, beschikbaarheid).

## 2. Kans-schaal (1–5)

| Score | Niveau | Betekenis in legacyui-context |
|---|---|---|
| 1 | Zeer onwaarschijnlijk | Theoretisch; niet bereikbaar zonder al gecompromitteerde admin-toegang |
| 2 | Onwaarschijnlijk | Bereikbaar, maar vereist specifieke randvoorwaarden of interne toegang |
| 3 | Mogelijk | Bereikbaar voor een geauthenticeerde gebruiker; benutting vergt enige moeite |
| 4 | Waarschijnlijk | Bekend patroon, geautomatiseerd te misbruiken |
| 5 | Zeer waarschijnlijk | Triviaal te misbruiken, bekende exploit, of structureel ontbrekende controle |

## 3. Impact-schaal (1–5)

Categorieën (WS03): financiële schade, schade aan personen/gezondheid, reputatieschade, schade aan omgeving. In zorgcontext weegt patiëntdata/-veiligheid het zwaarst — alles wat dat raakt zit op 3+.

| Score | Niveau | Betekenis in legacyui-context |
|---|---|---|
| 1 | Onbelangrijk | Geen patiëntdata of beschikbaarheid geraakt; cosmetisch of intern |
| 2 | Minder ernstig | Beperkte interne impact; geen patiëntdata, makkelijk herstelbaar |
| 3 | Serieus | Beperkte blootstelling patiëntdata óf tijdelijke functie-uitval |
| 4 | Zeer serieus | Substantiële blootstelling/manipulatie patiëntdata, of verlies audit-spoor |
| 5 | Catastrofaal | Grootschalige blootstelling patiëntdata, integriteitsverlies dossier, of patiëntveiligheid in gevaar |

## 4. Risicoscore-matrix

|        | Impact 1 | Impact 2 | Impact 3 | Impact 4 | Impact 5 |
|--------|---|---|---|---|---|
| **Kans 5** | 5 | 10 | 15 | 20 | 25 |
| **Kans 4** | 4 | 8 | 12 | 16 | 20 |
| **Kans 3** | 3 | 6 | 9 | 12 | 15 |
| **Kans 2** | 2 | 4 | 6 | 8 | 10 |
| **Kans 1** | 1 | 2 | 3 | 4 | 5 |

## 5. Grenswaarden (WS03)

- **Rood (≥ 15):** onacceptabel — onmiddellijk aanpakken.
- **Oranje (8–14):** verhoogd — mitigatieplan met deadline.
- **Groen (≤ 7):** acceptabel — monitoren en periodiek herbeoordelen.

## 6. Risicobereidheid

WS03 staat toe dat de grens per categorie verschilt ("de lat ligt hoger voor patiëntveiligheid dan voor reputatie"). Wij hanteren een aangescherpte grens voor patiëntveiligheid:

- **Schade aan personen/gezondheid of patiëntdata-vertrouwelijkheid:** score **≥ 10 al onacceptabel** (i.p.v. 15).
- **Overige categorieën** (financieel, reputatie, omgeving): generieke grens, ≥ 15 onacceptabel.

Onderbouwing: dit sluit aan op de NEN-7510-kern dat patiëntveiligheid primair is. Bij het scoren van een risico bepaalt de zwaarst geraakte categorie welke grens geldt.

## 7. Acceptance

- **Groen (≤7):** team accepteert zelf, vastgelegd in het risicoregister.
- **Oranje (8–14):** acceptatie alleen met onderbouwing + mitigatieplan met deadline; in het RAR (punt 9).
- **Onacceptabel** (≥15 generiek, of ≥10 voor patiëntveiligheid): mitigeren of escaleren. In een organisatie ligt acceptatie hier bij management/CISO; in projectcontext motiveren in het RAR.

---

*Basis voor punt 3 (projectmatrix), 4 (bow-tie) en 5 (CI-CD-evaluatie) — één bron van waarheid.*
