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

float light = 0;
String receivedData = "";
String test = "TEST321";
int n = 0;
float Temperature, Humidity;
int i = 0;

sensors_event_t event; // DHT event

void setup() {
  // put your setup code here, to run once:
  Temperature = 0.0f;
  Humidity = 0.0f;
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
  
  dht.temperature().getEvent(&event);
  Temperature = event.temperature;
  dht.humidity().getEvent(&event);
  Humidity = event.relative_humidity;
  if (i == 0){  
    lcd.setCursor(0,0);
    lcd.print("Temp: ");
    lcd.print(Temperature);
    lcd.print((char)223);
    lcd.print("C");
    lcd.setCursor(0,1);
    lcd.print("Hum: ");
    lcd.print(Humidity);
    lcd.print("% RH");
    i = 20;
  }
  
  
  if(Serial.available()>0)
  {
    
    while((n = (char)Serial.read()) >= 0){
      receivedData += (char)n;
      delay(10);
    }
    if(receivedData == "1") {
      String temp = ("Temperature: " + String(Temperature) + "C");
      Serial.println(temp);
      delay(10);
      temp = ("Humidity: " + String(Humidity) + "% RH");
      Serial.println(temp);      
    }

    receivedData.remove(0,-1);
    i--;
    delay(100);
  }
}
