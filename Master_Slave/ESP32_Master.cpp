#include <WiFi.h>
#include <HTTPClient.h>
#include <Arduino.h>
#include <SPI.h>
#include <string.h>


//const char* ssid = "iotroam";
//const char* password = "MijnESP32!";

const char* ssid = "Ziggo5381810";
const char* password = "x6xxkyuPzpfFspzo";

float voltage = 0.0;

const int adcPin = 36; // ADC0 on ESP32 is GPIO36 (VP)


// Replace with server IP address and port
//  const char* url = "http://145.24.223.29:3001/requests/1126211";
  const char* url = "http://192.168.178.186:8000/post_data";


void setup() {
  Serial.begin(115200);
    //MASTER code
    
    // Initialiseer de SPI-interface
    SPI.begin();
    
    // Configureer de Chip Select (SS) pin als output
    pinMode(SS, OUTPUT); 
    digitalWrite(SS, HIGH); // SS hoog maken om de slave inactief te houden
    
    // Pas de SPISettings in de setup() aan
    SPISettings settings(10000, MSBFIRST, SPI_MODE0); // Probeer 10 kHz (was 100 kHz)
    SPI.beginTransaction(settings);


  // SERVER CODE
  analogReadResolution(12); // Optional: sets resolution to 12 bits (0–4095)
  delay(1000);

  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected!");
    
}


void loop() {

// MASTER CODE
    // 1. Buffer om de 4 ontvangen bytes op te slaan
    uint8_t receivedBytes[4];
    
    // 2. De float variabele waar het resultaat naartoe gekopieerd wordt
    float resistanceValue = 0.0f; 

    // Start SPI-transactie
    digitalWrite(SS, LOW); // CS/SS laag
    
    // Lees 4 opeenvolgende bytes van de slave
    for (int i = 0; i < 4; i++) {
        // Verstuur 0x00 (dummy) en ontvang de i-de byte van de float
        receivedBytes[i] = SPI.transfer(0x00); 
    }
    
    // Einde SPI-transactie
    digitalWrite(SS, HIGH); // CS/SS hoog

    memcpy(&resistanceValue, receivedBytes, sizeof(float));
    
    // Print het gereconstrueerde float-resultaat
    Serial.print("Weerstand: ");
    Serial.print(resistanceValue, 2); // Toon de float met 2 decimalen
    Serial.println(" Ohm");

//SERVER CODE
if (WiFi.status() == WL_CONNECTED) {
    int adcValue = analogRead(adcPin);
    voltage = adcValue * (3.3 / 4095.0);
    int je_mama = 69420;
    String payload = "{\"voltage\": " + String(voltage, 3) + ", \"weerstand attiny\": " + String(resistanceValue, 2) + "}" ;
    //http.begin(url + payload); // full URL with IP and port

    //int httpCode = http.GET(); // Send GET request

    //POST REQUEST VERSTUREN
    HTTPClient http;
    http.begin(url);
    http.addHeader("Content-Type" , "application/json");
    int httpResponceCode = http.POST(payload);

    if (httpResponceCode >= 200 && httpResponceCode < 300) {
    Serial.println("Success: data sent");
    } else if (httpResponceCode >= 400 && httpResponceCode < 500) {
    Serial.print("HTTP error: ");
    Serial.println(httpResponceCode);
    // krijg meer details
    String responseBody = http.getString();
    Serial.println("Server: " + responseBody);
    }

    http.end(); // Close connection

    delay(100);
  }

  Serial.println("voltage: " + String(voltage, 3) + " V");
  Serial.println("weerstand attiny: " + String(resistanceValue, 2) + " Ohm");

}
