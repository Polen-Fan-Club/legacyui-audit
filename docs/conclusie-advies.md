# Conclusie & advies

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** · Issue #60

## Eindoordeel per beheersmaatregel

De gap-analyse stelde de baseline vast; de pentest-remediatie heeft een deel daarvan opgelost. Onderstaand oordeel onderscheidt beide, omdat het verschil het resultaat van de audit is.

| Control | Baseline-oordeel | Na remediatie | Toelichting |
|---|---|---|---|
| 8.3 Toegangsbeperking | Gedeeltelijk compliant | Gedeeltelijk compliant (verbeterd) | IP-binding hersteld (#1); controller-privilege-gaten 8.3-4/5 blijven open als vervolgwerk |
| 8.5 Beveiligde authenticatie | Gedeeltelijk compliant | Gedeeltelijk compliant (verbeterd) | Reset-flow gehard (#5); brute-force (#2) en enumeratie (#4) bewust geaccepteerd onder de grenswaarde |
| 8.15 Logging | Niet compliant | Grotendeels compliant | Tien security-event-categorieën nu ge-audit-logd (#3), met regressietests; grootste verbetering van de audit |
| 8.9 Configuratiebeheer | Niet apart beoordeeld | Gedeeltelijk | Information disclosure gemitigeerd (#6a); versiebanner is containerconfig, scope-grens (#6b) |

8.15 ging van niet-compliant naar grotendeels compliant en is daarmee de belangrijkste winst. 8.3 en 8.5 zijn verbeterd maar niet volledig dichtgezet — de resterende punten zijn bewuste keuzes (acceptaties onder de grenswaarde) of geïdentificeerd vervolgwerk (controller-privilege-checks), niet over het hoofd geziene gaten.

## Belangrijkste resterende risico's

Drie zaken vragen om opvolging, in volgorde van de risicocriteria (`security-backlog.md`):

Ten eerste de afhankelijkheden: 128 kwetsbaarheden waarvan 6 critical, met `commons-fileupload` en `log4j` 1.x als de scherpste. log4j 1.x is end-of-life en vereist migratie, geen patch — arbeidsintensief en daarom in te plannen, niet uit te stellen. Ten tweede de controller-privilege-gaten (8.3-4/5): de module leunt voor autorisatie op de JSP-laag, wat een directe-URL-aanroep kan omzeilen. Ten derde het bredere aanvalsoppervlak dat de mapping blootlegde maar dat buiten de pentest-scope bleef — met name de DWR-laag-autorisatie, die als bewust openstaand is gemarkeerd.

## Advies

De module is met de uitgevoerde remediatie aantoonbaar veiliger, maar niet productierijp voor een zorgomgeving zonder de vervolgstappen. Het advies is gefaseerd, conform de geprioriteerde backlog: pak eerst de P1-items met patiëntdata-impact op (de critical dependencies en de controller-privilege-checks), plan de log4j-migratie als afzonderlijk traject, en breng vervolgens de DWR-laag binnen scope voor een gerichte autorisatie-analyse. Het volledige patchadvies met kostenraming (circa 9,5 dag) staat in het risk assessment report.

Twee deployment-voorwaarden gelden voor de reeds doorgevoerde fixes: de `ForcePasswordChangeFilter` moet actief zijn (#5). De eerdere logniveau-voorwaarde voor #3 is vervallen sinds de audit-events op WARN loggen en daarmee zichtbaar zijn onder het default distro-logbeleid.

## Slotsom

De audit toont een legacy zorgcomponent met reële, onderbouwde tekortkomingen, waarvan de meest kritische logging-laag binnen het traject is hersteld. De waarde zit niet alleen in de fixes, maar in de herleidbaarheid: elke bevinding is van bron tot hertest te volgen, elk besluit is tegen vastgelegde criteria genomen, en wat bewust niet is gedaan is met reden vastgelegd. Dat maakt de resterende risico's beheersbaar in plaats van onbekend.
