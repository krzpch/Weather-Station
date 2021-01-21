# Weather-Station
This project was made for Design labolatory at AGH UST. The main idea was to make simple weather station with Android companion app. The weather station is based on Arduino Uno board and it can measure temperature and humidity using DHT22 sensor. In addition LCD shows current measurments and time. The data shown on LCD are switched automaticly after few seconds. The Android aplication connects to station via Bluetooth.

## Hardware reqired
- Arduino compatible board
- Android phone
- Bluetooth module HC-05
- LCD 16x2 with I2C module
- DHT 22 temperature and humidity sensor
- 10k resistor for DHT22
- 12 V power supply

## Station
The station measures current temperature and humidity in 8 seconds delays. It also saves measurments in history. To get this measurments it uses DHT22 sensor, whitch measures temerature in range from -40 to 80 Celsuis with accuracy of +-0.5 Celsius and humidity in range from 0 to 100 % RH with accuracy of +- 2 % RH. If your phone is connected, the station will be sending current measurements periodically with 30 second delays. HC-05 Bluetooth module communicate with board using UART. 

#### Schematics
<img src="https://github.com/krzpch/Weather-Station/blob/main/Weather_Station_Schematics.jpg" width="1000">

## Android Aplication
To properly use this aplication your device must be paired with HC-05 module. After opening the app you press the "CONNECT" button to establish communication with the station. App sends time sync message to station in order to update current time. Temperature and humidity from the station are sent automatically in 30 second periods to your phone. They are shown in a box at the top of your screen. You can order history of measurments by pressing "GET HISTORY" button.

#### Screenshot from aplication
<img src="https://github.com/krzpch/Weather-Station/blob/main/Aplication_screenshot.jpg" width="300">
