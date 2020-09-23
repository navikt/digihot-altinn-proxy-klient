digihot-altinn-proxy-klient
==============================

Bibliotek som tilbyr en Http klient til digihot-altinn-proxy.
Klienten vil forsøke å kontakte Altinn via proxyen, som tilbyr caching på tvers av ulike tjenester i Nav.

# Komme i gang

Biblioteket er skrevet i Kotlin. Koden kompileres med maven og produserer en jar fil `digihot-altinn-proxy-klient-{version}.jar`

`mvn clean install`

# Bruk av AltinnProxyKlient 

Biblioteket importeres i klientapplikasjon slik (eksempel med maven)
```xml
<dependency>
  <groupId>no.nav.digihot</groupId>
  <artifactId>digihot-altinn-proxy-klient</artifactId>
  <version>${digihot-altinn-proxy-klient.version}</version>
</dependency>
```

Klienten instansieres slik: 
```java
String consumerId = "navn-til-klient-applikasjon";

AltinnProxyKlient klient = new AltinnProxyKlient(consumerId, proxyUrl);
```

Da skal det være mulig å hente listen av organisasjoner `AltinnReportee` en bruker har enkeltrettigheter i: 

For en spesifikk tuple `serviceCode` og `serviceEdition` 
```java
List<AltinnReportee> organisasjoner =  
    klient.hentOrganisasjoner(
        new SelvbetjeningToken(selvbetjeningTokenAsString),
        new ServiceCode(serviceCode),
        new ServiceEdition(serviceEdition), 
        true
    );
```

Hvor `selvbetjeningTokenAsString` er String verdi av `selvbetjening-idtoken` cookie til innlogget bruker. 

Det er mulig å filtrere bort organisasjoner av type `Person` eller som ikke er aktive ved å sette siste parameteren `filterPåAktiveOrganisasjoner` til `true`

---
# Lage og publisere en ny release
Nye versjonar blir publisert til Github Package Registry, og kan hentast derifrå.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #digihot.
