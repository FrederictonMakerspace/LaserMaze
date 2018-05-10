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
  laserOn();
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

// New Code to handle values from PC
          switch (val) {
          case 0: //game running
            laserOn();
          break;
          
          case 1: // case 1 Laser beam broken, turn lasers off
              laserOff();
          break;
          
          case 2: // case 2 System reset
        //reset system
           systemReset();
          break;
          default:
            laserOn();
          break;
          }
   }
  else {
//    attract mode
}

    //ALARM
/*    if (val == '1') 
    {
      digitalWrite(ledPin, LOW); //Turn led off
      digitalWrite(laserPin, LOW); //Turn laser off
    } 
    else
    {
     digitalWrite(ledPin, HIGH); //Turn led on
     digitalWrite(laserPin, HIGH); //Turn laser on
     }
*/
  
    
  //Wrap for compatibility with the laser processing sketch (comma separated values)
//  String serialMessage = "0:" + (String)resistorValues[0] + ",0:" + (String)resistorValues[1] + ",0:" + (String)resistorValues[2] + ",0:"  + (String)resistorValues[3];

  //Send to PC
  //Serial.println(serialMessage);

  //delay(50); //Small delay
}


void systemReset() {
  // turn alarm off
  // turn lasers on
    //flash lasers randomly on and off
  // then let the PC know I'm reset
  // reset PC so timer is 0, and start button is available.
}

void laserOn() {
      digitalWrite(ledPin, HIGH); //Turn led on
      digitalWrite(laserPin, HIGH); //Turn laser on
      String serialMessage = "0:" + (String)resistorValues[0] + "," + (String)resistorValues[1] + "," + (String)resistorValues[2] + ","  + (String)resistorValues[3];
  //Send to PC
      Serial.println(serialMessage);
      delay(50); //Small delay
}

void laserOff() {
     digitalWrite(ledPin, LOW); //Turn led on
     digitalWrite(laserPin, LOW); //Turn laser on
     triggerAlarm();
}

void triggerAlarm() {
  // digitalWrite(alarmPin, High);

}

void attractMode() {
  //random flash lasers faset and faster
}

