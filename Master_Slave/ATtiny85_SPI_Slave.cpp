#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/sleep.h>
#include <stdbool.h>

// SPI Pin Definitions
#define dataInPin PB0      
#define dataOutPin PB1     
#define serialClockPin PB2 
#define chipSelectPin PB3  

// ADC Configuration
#define ADC_CHANNEL 2 
#define VCC_VOLTAGE 3.33f // Controleer dit! Gebruik 5.0f indien nodig.
#define SERIES_RESISTOR 10000.0f 

// Functie prototypes
uint8_t transfer(uint8_t data);
float measureResistance();

// --- SPI Transfer Functie ---
uint8_t transfer(uint8_t data) {
    USIDR = data; 
    USISR |= (1<<USIOIF); 
    while (!(USISR & (1<<USIOIF))); 
    return USIDR; 
}

// --- ADC Meet Functie (onveranderd) ---
float measureResistance() {
    ADCSRA |= (1<<ADEN); 
    ADCSRA |= (1<<ADSC); 
    while (ADCSRA & (1<<ADSC)) {} 
    
    uint8_t low = ADCL; 
    uint8_t high = ADCH;
    ADCSRA &= ~(1<<ADEN); 
    
    uint16_t adc_value = (high << 8) | low;

    float voltage = (float)adc_value / 1024.0f * VCC_VOLTAGE;

    // Rx = R_serie * (V_out / (Vcc - V_out))
    float resistance = SERIES_RESISTOR * (voltage / (VCC_VOLTAGE - voltage));

    return resistance;
}

// --- GECORRIGEERDE Pin Change Interrupt Handler ---
ISR(PCINT0_vect) {
    // 1. Controleer of CS laag is (Falling Edge)
    if (PINB & (1<<chipSelectPin)) return; 

    // 2. Meet de waarde EENMAAL bij de start
    float resistance_value = measureResistance(); 
    
    // Pointer naar de bytes van de float
    uint8_t *resistance_bytes = (uint8_t *)&resistance_value;
    
    // 3. LOOP 4 KEER om alle 4 transfers af te handelen
    for (int i = 0; i < sizeof(float); i++) {
        uint8_t byte_to_send = resistance_bytes[i];
        
        // Wacht tot de Master de klok levert voor deze byte
        transfer(byte_to_send); 
    }
    
    // De ISR eindigt hier, de ATtiny gaat terug in slaap
}

// --- Main Program (onveranderd) ---
int main() {
    PORTB = 0; 
    DDRB = 1<<dataOutPin; 

    // USI (SPI Slave) Configuratie
    USICR = (1<<USIWM0) | (1<<USICS1); 

    // ADC Configuratie
    ADMUX = (0 << REFS0) | (0 << ADLAR) | (ADC_CHANNEL << MUX0); 
    ADCSRA = (1 << ADPS2) | (1 << ADPS1) | (1 << ADPS0); 

    // Pin Change Interrupt Configuratie
    GIMSK = 1<<PCIE; 
    PCMSK = 1<<chipSelectPin; 

    // Slaapmodus Configuratie
    set_sleep_mode(SLEEP_MODE_PWR_DOWN); 
    
    sei(); 
    while (true) sleep_mode(); 
}
