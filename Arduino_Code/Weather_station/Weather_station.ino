#include <TimeLib.h> // Lib for time sync

#include <Wire.h> // Libs for I2C
#include <LiquidCrystal_I2C.h> 

#include <Adafruit_Sensor.h> // Libs for DHT 22 sensor
#include <DHT.h>
#include <DHT_U.h>

#define DHTPIN 4
#define DHTTYPE    DHT22

#define TIME_HEADER     "SYNC"    // Header tag for serial time sync message
#define DATA_HEADER     "DATA"    // Header tag for ordering temperature and humidity
#define HISTORY_HEADER  "HIST"    // Header tag for ordering history

#define HOLDED_HISTORY 24         // Number of holded history cells

DHT_Unified dht(DHTPIN, DHTTYPE);
sensors_event_t event; // DHT event
LiquidCrystal_I2C lcd(0x27,16,2);
uint32_t delayMS;

struct history_cell {
  float h_temperature = 0.0f;
  float h_humidity = 0.0f;
  String h_time = " - ";
};

String receivedData = "";
float Temperature, Humidity;
struct history_cell history[HOLDED_HISTORY];
int history_num = 0;
int prev_time = 0;

int LCD_change = 0;

void setup() {
  // put your setup code here, to run once:
  Temperature = 0.0f;
  Humidity = 0.0f;
  dht.begin();
  lcd.init();
  lcd.backlight();
  pinMode(A0, INPUT);
  Serial.begin(9600);
  setTime(1609459200); // Jan 1 2021

  sensor_t sensor;
  dht.temperature().getSensor(&sensor);
  dht.humidity().getSensor(&sensor);
  delayMS = sensor.min_delay / 1000;
  delay(delayMS);
  dht.temperature().getEvent(&event);
  Temperature = event.temperature;
  dht.humidity().getEvent(&event);
  Humidity = event.relative_humidity;
  
} // end of setup

void loop() {  
  if (history_num > HOLDED_HISTORY - 1){
    history_num = 0;
  }
  if (minute() != prev_time) {
    dht.temperature().getEvent(&event);
    history[history_num].h_temperature = event.temperature;
    dht.humidity().getEvent(&event);
    history[history_num].h_humidity = event.relative_humidity;
    history[history_num].h_time = ((String)hour() + ":" +  (String)(minute()/10) + (String)(minute()%10) + " " + (String)day() + "." + (String)(month()/10) + (String)(month()%10));

    prev_time = minute();
    history_num ++;
  }
  
  if (LCD_change == 0){  // LCD print temperature and humidity
    dht.temperature().getEvent(&event);
    Temperature = event.temperature;
    dht.humidity().getEvent(&event);
    Humidity = event.relative_humidity;
    String temp = ("Temp: " + String(Temperature,2) + (char)223 + "C");
    lcd.setCursor(0,0);
    lcd.print(temp);
    temp = ("Hum: " + String(Humidity,2) + "% RH");
    lcd.setCursor(0,1);
    lcd.print(temp);
    LCD_change = 160;
  }
  if (LCD_change == 80){  // LCD print time + date
    String temp = (today() + " " + (String)hour() + ":" +  (String)(minute()/10) + (String)(minute()%10) + "          ");
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
    checkMessageForTag(receivedData);
  }
   
  receivedData.remove(0,-1); // clear receivedData string
  LCD_change--;
  delay(100);
} // end of loop

void processSyncMessage(String text) {
  String temp = text.substring(5,15);
  long tstamp = temp.toInt() + 3600; // current time UTC + 1H (GMT+1)
  const unsigned long DEFAULT_TIME = 1609459200; // Jan 1 2021
  if( tstamp >= DEFAULT_TIME) { // check the integer is a valid time (greater than Jan 1 2021)
    setTime(tstamp); // Sync Arduino clock to the time received
//    Serial.println("Time Sync succeeded");
  } else {
//    Serial.println("Time Sync failed");
  }
} // end of void processSyncMessage()

void Return_message(){
//  String temp = ((String)hour() + ":" +  (String)minute() + " " + (String)day() + "." + (String)month() + "." + (String)year());
//  Serial.println(temp); 
  String temp = ("Temp: " + String(Temperature) + "\u00B0C, Hum: " + String(Humidity) + "% RH");
  Serial.println(temp);
} // end of void Return_message()

void checkMessageForTag(String text) {
  String check_tag = text.substring(0,4);
  if (check_tag == TIME_HEADER){
    processSyncMessage(text);
  }
  else if (check_tag == DATA_HEADER){ // ordering data transfer
    if (analogRead(A0) >= 500) // data won't be send if there is no connection
      Return_message();
  }
  else if (check_tag == HISTORY_HEADER){ // ordering data transfer
    if (analogRead(A0) >= 500) // data won't be send if there is no connection
      send_history();
  }
} //end of void checkMessageForTag()

void send_history() {
  for (int i = 0; i < HOLDED_HISTORY ; i++) {
    String temp = ("Temp: " + String(history[(history_num + i) % HOLDED_HISTORY].h_temperature) + " , Hum: " + 
    String(history[(history_num + i) % HOLDED_HISTORY].h_humidity) + " Time: " +  String(history[(history_num + i) % HOLDED_HISTORY].h_time));
    Serial.println(temp);
    delay(100);
  }
} //end of void history()

String today() { 
  String week[7] = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
  return week[weekday()];
} // end of String today()
