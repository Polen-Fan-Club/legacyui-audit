# CIA/BIV-analyse & kroonjuwelen

**Module:** OpenMRS Legacy UI v1.20.0 · **Normen:** NEN-7510:2024-2, AVG · **Schaal:** BIV 1–5, conform de impactschaal in `risicocriteria.md`

Scope: de datatypes die legacyui verwerkt of ontsluit. Voedt het Risk Assessment Report. Dreigingen/risico's staan in het threat model en de risicomatrix.

## 1. Kroonjuwelen (met referenties)

| Kroonjuweel | Verwerkt door (referentie) | Aard |
|---|---|---|
| Patiëntdossier (encounters, observaties) | `PatientDashboardController`, `EncounterFormController`, `ObsFormController` | Gezondheidsdata, AVG art. 9 |
| Persoons- & identificatiegegevens | `PersonFormController`, `ShortPatientFormController`, `PatientIdentifierTypeFormController` | Direct identificerend |
| Relaties & programma's | `PersonRelationshipsPortletController`, `PatientProgramFormController` | Afgeleide gezondheidscontext |
| Gebruikers- & toegangsbeheer | `UserFormController`, `UserListController`, `ChangePasswordFormController` | Credentials, rollen, privileges |
| Audit-/loggegevens | ontbreekt grotendeels (gap 8.15) | Bewijs wie-wat-wanneer |

## 2. BIV-classificatie

B/I/V op 1–5 (1 = onbelangrijk, 5 = catastrofaal).

| Kroonjuweel | B | I | V | Onderbouwing |
|---|---|---|---|---|
| Patiëntdossier | 4 | 5 | 5 | Foutief dossier raakt behandelbeslissingen; lek = art. 9-datalek. B=4: zorg kan kort op papier door. |
| Persoons-/identificatiegegevens | 3 | 4 | 5 | Direct identificerend (V=5); foutieve identiteit → dossierverwisseling. |
| Relaties & programma's | 2 | 3 | 4 | Onthult gevoelige context; minder kritiek voor directe behandeling. |
| Gebruikers-/toegangsbeheer | 3 | 5 | 4 | I=5: een gemanipuleerd privilege opent álle patiëntdata. Sleutel tot de andere kroonjuwelen. |
| Audit-/loggegevens | 3 | 4 | 2 | Bewijswaarde (NEN 8.15); feitelijke bescherming laag doordat logging ontbreekt. |

## 3. Koppeling NEN-7510 & doorwerking

- Patiëntdossier + persoonsgegevens → 5.12, 8.3, 8.5 (gap 8.3-4/5/6).
- Toegangsbeheer (I=5) → verklaart waarom backlog B7 en B3 P1 zijn.
- Audit-/loggegevens → 8.15; hoge classificatie, ontbrekende uitvoering = gap 8.15 / backlog B1/B2.

De zwaarst geclassificeerde data (patiëntdossier I/V=5, toegangsbeheer I=5) onderbouwt waarom variant B (≥10 onacceptabel) op deze module van toepassing is.

---

*Referenties geverifieerd tegen de broncode (omod/.../controller). Schaal: `risicocriteria.md`. Dreigingen: zie het threat model. Issue #25.*
