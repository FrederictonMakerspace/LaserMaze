#include <SoftwareSerial.h>
/* Read Photo resisor values and send them to a PC (procssing) to monitor lasers in a laser maze)
   More info and circuit schematic: https://github.com/FrederictonMakerspace/LaserMaze
   Dev: Rick McCaskill, Fredericton Makerspace 2018
*/
const int ledPin=13;       // Led pin at Arduino pin 13
const int laserPin=8;       // Laser (5mw) connected to pin at Arduino pin 8

const int resistorPins[] = {A0, A1, A2, A3}; //Photoresistor at Arduino analog pin A0
int resistorCount = 4;

//Variables
int resistorValues[] = {0, 0, 0, 0};  // Store value from photoresistor (0-100ish? Depends on the resitor used. Use 220)

void setup()
{
  pinMode(ledPin, OUTPUT);  // Set ledPin - 13 pin as an output
  pinMode(laserPin, OUTPUT);  // Set laserPin - 8 pin as an output
  
  //Initialize input pics
  for (int i=0; i<resistorCount; i++)
  {
    int pin = resistorPins[i];
    pinMode(pin, INPUT);// Resistor input 
  }
  
  digitalWrite(ledPin, LOW); //Turn led off
  digitalWrite(laserPin, LOW); //Turn led off

  Serial.begin(9600);
}

void loop()
{
  //Read all resistors and store the valuye
  for (int i=0; i<resistorCount; i++)
  {
    int resistorToRead = resistorPins[i];
    
    //Store the value in the array
    resistorValues[i] = analogRead(resistorToRead);
  }

  //Send the values over to the attached PC
  //Check if the PC sent an alarm value
   if (Serial.available()) 
   { 
      // If data is available to read,
     int val = Serial.read(); // read it and store it in val from the processing app on the attached PC

    //ALARM
    if (val == '1') 
    {
      digitalWrite(ledPin, LOW); //Turn led off
      digitalWrite(laserPin, LOW); //Turn laser off
    } 
    else
    {
     digitalWrite(ledPin, HIGH); //Turn led on
     digitalWrite(laserPin, HIGH); //Turn laser on
     }
  }
    
  //Wrap for compatibility with the laser processing sketch (comma separated values)
  String serialMessage = (String)resistorValues[0] + ",0,0,0";

  //Send to PC
  Serial.println(serialMessage);

  delay(50); //Small delay
}
