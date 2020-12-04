#include <LiquidCrystal_I2C.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>

#define DHTPIN 4
#define DHTTYPE    DHT22

DHT_Unified dht(DHTPIN, DHTTYPE);
LiquidCrystal_I2C lcd(0x27,16,2);
uint32_t delayMS;

void setup() {
  // put your setup code here, to run once:
  dht.begin();
  lcd.init();
  lcd.backlight();
  pinMode(A0, INPUT);
  Serial.begin(9600);
  
  sensor_t sensor;
  dht.temperature().getSensor(&sensor);
  dht.humidity().getSensor(&sensor);
  delayMS = sensor.min_delay / 1000;
}

void loop() {
  // put your main code here, to run repeatedly:
  delay(delayMS);
  sensors_event_t event;
  dht.temperature().getEvent(&event);
  Serial.print(F("Temperature: "));
  Serial.print(event.temperature);
  Serial.println(F("°C"));
  lcd.setCursor(0,0);
  lcd.print("Temp: ");
  lcd.print(event.temperature);
  lcd.print((char)223);
  lcd.print("C");
  dht.humidity().getEvent(&event);
  Serial.print(F("Humidity: "));
  Serial.print(event.relative_humidity);
  Serial.println(F("% RH"));
  lcd.setCursor(0,1);
  lcd.print("Hum: ");
  lcd.print(event.relative_humidity);
  lcd.print("% RH");

}
