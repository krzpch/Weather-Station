#include <TimeLib.h> // Lib for time sync

#include <Wire.h>
#include <LiquidCrystal_I2C.h> 

#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>

#define DHTPIN 4
#define DHTTYPE    DHT22

#define TIME_HEADER  'T'   // Header tag for serial time sync message
#define TIME_REQUEST  7    // ASCII bell character requests a time sync message 

DHT_Unified dht(DHTPIN, DHTTYPE);
LiquidCrystal_I2C lcd(0x27,16,2);
uint32_t delayMS;

String receivedData = "";

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
  setTime(1609455600); // Jan 1 2021 GMT+1

  sensor_t sensor;
  dht.temperature().getSensor(&sensor);
  dht.humidity().getSensor(&sensor);
  delayMS = sensor.min_delay / 1000;
  delay(delayMS);
  dht.temperature().getEvent(&event);
  Temperature = event.temperature;
  dht.humidity().getEvent(&event);
  Humidity = event.relative_humidity;
  
}

void loop() {
  
  
  if (i == 0){  
    dht.temperature().getEvent(&event);
    Temperature = event.temperature;
    dht.humidity().getEvent(&event);
    Humidity = event.relative_humidity;
    lcd.setCursor(0,0);
    lcd.print("Temp: ");
    lcd.print(Temperature);
    lcd.print((char)223);
    lcd.print("C   ");
    lcd.setCursor(0,1);
    lcd.print("Hum: ");
    lcd.print(Humidity);
    lcd.print("% RH  ");
    i = 80;
  }
  if (i == 40){
    String temp = ((String)hour() + ":" +  (String)(minute()/10) + (String)(minute()%10) + "            ");
    lcd.setCursor(0,0);
    lcd.print(temp);
    lcd.setCursor(0,1);
    temp = ((String)day() + "." + (String)(month()/10) + (String)(month()%10) + "." + (String)year() + "          ");
    lcd.print(temp);
  } 
  
  if(Serial.available()>0)
  {
    int n = 0;
    while((n = (char)Serial.read()) >= 0){
      receivedData += (char)n;
      delay(10);
    }
    String temp = receivedData.substring(1,11);
    long tstamp = temp.toInt() + 3600; // current time UTC + 1H (GMT+1)
    processSyncMessage(tstamp); 
    Return_message();
  }
    receivedData.remove(0,-1);
    i--;
    delay(100);
  
}


void processSyncMessage(unsigned long tstamp) {
  const unsigned long DEFAULT_TIME = 1609455600; // Jan 1 2021 GMT+1
     if( tstamp >= DEFAULT_TIME) { // check the integer is a valid time (greater than Jan 1 2021)
       setTime(tstamp); // Sync Arduino clock to the time received on the serial port
  }
}

void Return_message(){
  // digital clock display of the time
//  String temp = ((String)hour() + ":" +  (String)minute() + " " + (String)day() + "." + (String)month() + "." + (String)year());
//  Serial.println(temp); 
  String temp = ("Temp: " + String(Temperature) + "C, Hum: " + String(Humidity) + "% RH");
  Serial.println(temp);
}
