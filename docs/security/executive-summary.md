# Executive summary

**Module:** OpenMRS Legacy UI v1.20.0 · **Norm:** NEN-7510:2024-2 · **Vertrouwelijk** · Issue #55

**Algeheel beveiligingsoordeel: AMBER** — de module is aantoonbaar veiliger geworden maar is nog niet productierijp voor een zorgomgeving zonder de geprioriteerde vervolgstappen (zie security backlog P1).

Deze audit beoordeelt de beveiliging van de OpenMRS Legacy UI-module v1.20.0, de weblaag van een elektronisch patiëntdossier, tegen NEN-7510:2024-2. De aanleiding is dat deze module patiëntdossiers en toegangsbeheer verwerkt — data van de hoogste gevoeligheidsklasse — terwijl het een verouderd component betreft.

De audit combineerde een gap-analyse tegen de norm, een attack surface mapping, een penetratietest en een analyse van de afhankelijkheden. Het leidende principe was herleidbaarheid: elke bevinding is van bron tot hertest te volgen via een verifieerbaar artefact, en elk accepteer- of herstelbesluit is genomen tegen vooraf vastgelegde risicocriteria, met een aangescherpte grens voor alles wat patiëntveiligheid raakt.

Op de baseline scoorde de module gedeeltelijk compliant op toegangsbeveiliging (8.3) en authenticatie (8.5), en niet compliant op logging (8.15). De zwaarste bevinding was het volledig ontbreken van een auditspoor voor beveiligingsgebeurtenissen. Daarnaast legde de analyse een breed aanvalsoppervlak bloot (34 entry points, waarvan 5 hoog risico) en 128 kwetsbaarheden in de afhankelijkheden, waarvan 6 critical.

Binnen het traject zijn vier van de zes pentest-bevindingen opgelost en hertest: de herstelde sessie-IP-binding, het complete audit-logging, de geharde wachtwoordreset-flow en het mitigeren van information disclosure. Logging ging daarmee van niet-compliant naar grotendeels compliant — de grootste verbetering. Twee bevindingen zijn bewust geaccepteerd omdat ze onder de risicogrenswaarde bleven, met vastgelegde onderbouwing.

De module is aantoonbaar veiliger geworden, maar nog niet productierijp voor een zorgomgeving. De belangrijkste resterende punten zijn de kwetsbare afhankelijkheden (met de log4j 1.x-migratie als zwaarste), de ontbrekende privilege-checks op controllerniveau, en de nog niet diepgaand geanalyseerde DWR-laag. Het advies is een gefaseerde aanpak volgens de geprioriteerde backlog, met een geraamde inspanning van circa 9,5 dag voor de kern.

De waarde van deze audit ligt evenzeer in de aanpak als in de fixes: de resterende risico's zijn geïdentificeerd, onderbouwd en geprioriteerd, en wat bewust niet is gedaan is met reden vastgelegd. Daarmee zijn de risico's beheersbaar in plaats van onbekend.
