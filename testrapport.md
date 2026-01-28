# Testrapport Slimme Meter


Dit testrapport is om de slimme meter voor tijdvak 2 Thema Software 2526 te testen en bij te houden en om te kijken of deze (goed) functioneert


Het project bestaat uit verschillende onderdelen:

- De potmeter levert analoge data
- ATTiny85 leest de potmeter data en stuurt het in bytes door naar de ESP
- LDR levert analoge data aan ESP
- De ESP stuurt de data naar de server
- De Server ontvangt data en stuurt dit naar de Webpagina en Database

## Test 1: Potmeter uitlezen en weergeven ESP

Doel: Controleren of data correct wordt verzonden

Stappen:

- draai aan de potmeter
- kijk in de seriele monitor of je de waarde van de potmeter ziet veranderen

Verwachting:

De data wordt correct gestuurd en wordt correct getoont

| test nr. | resultaat |
| -------- | --------- |
| 1        | ❌        |
| 2        | ❌        |
| 3        | ✅        |
| 4        | ✅        |
| 5        | ✅        |
| 6        | ✅        |
| 7        | ✅        |
| 8        | ✅        |
| 9        | ✅        |
| 10       | ✅        |

Resultaat:

80% geslaagd


## Test 2: LDR uitlezen en weergeven ESP


Doel: Controleren of data correct wordt verzonden

Stappen:

- schijnlicht op de LDR
- haal het licht weg
- kijk in de seriele monitor of je de waarde van de LDR ziet veranderen

Verwachting:

De data wordt correct gestuurd en wordt correct getoont

| test nr. | resultaat |
| -------- | --------- |
| 1        | ✅        |
| 2        | ✅        |
| 3        | ✅        |
| 4        | ✅        |
| 5        | ✅        |
| 6        | ✅        |
| 7        | ✅        |
| 8        | ✅        |
| 9        | ✅        |
| 10       | ✅        |

Resultaat:

100% geslaagd

## Test 3: data versturen naar webpagina

Doel: Controleren of de data van de potmeter en LDR weergeven worden op de webpagina

Stappen:

- Verbind de esp en laptop aan hetzelfde netwerk
- controleer de webpagina op localhost:8000

Verwachting: Data wordt normaal weergeven

| test nr | resultaat |
| ------- | --------- |
| 1       | ✅        |
| 2       | ✅        |
| 3       | ✅        |
| 4       | ✅        |
| 5       | ✅        |
| 6       | ✅        |
| 7       | ✅        |
| 8       | ✅        |
| 9       | ✅        |
| 10      | ✅        |

conclusie: 

100% geslaagd


## Test 4: data versturen naar Database

Doel: Controleren of de data van de potmeter en LDR weergeven worden op de database

Stappen:

- Verbind de esp en laptop aan hetzelfde netwerk
- controleer de database voor tabellen

Verwachting: Data wordt normaal weergeven met tijdstamp

| test nr | resultaat |
| ------- | --------- |
| 1       | ✅        |
| 2       | ✅        |
| 3       | ✅        |
| 4       | ✅        |
| 5       | ✅        |
| 6       | ✅        |
| 7       | ✅        |
| 8       | ✅        |
| 9       | ✅        |
| 10      | ✅        |

conclusie:

100% geslaagd

## Conclusie:

De Slimme meter is volledig funcionerend.

Het enige wat geen 100% slaging had was de ATTiny naar ESP maar dit lag aan een fout in de code
