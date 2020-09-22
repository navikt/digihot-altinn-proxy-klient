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
## Forutsetning
Release tag skal være signert. Derfor må signering av commits være aktivert per default, med f.eks `git config commit.gpgsign true`

## Prosess
Vi bruker `mvn-release-plugin` for å lage en ny release. I den prosessen skal en ny tag genereres.
 Artifact publiseres fra tag-en med GitHub actions.

Start med å rydde opp etter forrige release om det trenges ved å kjøre `mvn release:clean`

Lag en ny release med `mvn release:prepare`:
 * Skriv inn nytt release version (skal følge semantic versioning: https://semver.org/)
 * SCM release tag er preutfylt (bare trykk enter)
 * new development version er også preutfylt (trykk enter)

Kommandoen skal pushe en ny tag på GitHub. Da kan `Build and publish` action starte og release artifactene til Maven central.

## Publisere til Maven Central
Credentials som skal til for å kunne publisere til Maven Central provisjoneres av [publish-maven-central](https://github.com/navikt/publish-maven-central)

Tilgjengelige versjoner: https://repo1.maven.org/maven2/no/nav/arbeidsgiver/altinn-rettigheter-proxy-klient/

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #digihot.
