#include <SoftwareSerial.h>
/* Read Photo resisor values and send them to a PC (procssing) to monitor lasers in a laser maze)
   More info and circuit schematic: https://github.com/FrederictonMakerspace/LaserMaze
   Dev: Rick McCaskill, Fredericton Makerspace 2018
   Dev: James Gaudet, Fredericton Makerspace 2018
*/
const int ledPin=13;       // Led pin at Arduino pin 13
const int laserPin0=8;       // Laser (5mw) connected to pin at Arduino pin 8
const int laserPin1=9;       // Laser (5mw) connected to pin at Arduino pin 9
const int laserPin2=10;      // Laser (5mw) connected to pin at Arduino pin 10
const int laserPin3=11;      // Laser (5mw) connected to pin at Arduino pin 11
const int pressureSwitch=7;  // Controls the 'treasure' detection. The treasure holds the switch closed. 
const int startSwitch=6;  // Controls the the detecting of a 'start' momentary switch being pressed
int SystemStatus=4;

const int resistorPins[] = {A0, A1, A2, A3}; //Photoresistor at Arduino analog pin A0
int resistorCount = 4;
int laserCount = 4;

//Variables
int resistorValues[] = {0, 0, 0, 0};  // Store value from photoresistor (0-100ish? Depends on the resitor used. Use 220)

void setup()
{
  pinMode(pressureSwitch, INPUT);
  digitalWrite(pressureSwitch, HIGH);
  pinMode(ledPin, OUTPUT);  // Set ledPin - 13 pin as an output
  pinMode(laserPin0, OUTPUT);  // Set laserPin - 8 pin as an output
  pinMode(laserPin1, OUTPUT);  // Set laserPin - 9 pin as an output
  pinMode(laserPin2, OUTPUT);  // Set laserPin - 10 pin as an output
  pinMode(laserPin3, OUTPUT);  // Set laserPin - 11 pin as an output

  pinMode(startSwitch, INPUT_PULLUP);
  
  //Initialize input pics
  for (int i=0; i<resistorCount; i++)
  {
    int pin = resistorPins[i];
    pinMode(pin, INPUT);// Resistor input 
  }

  randomSeed (analogRead (4));    // randomize
    
  digitalWrite(ledPin, LOW); //Turn led off
  digitalWrite(laserPin0, LOW); //Turn led off
  digitalWrite(laserPin1, LOW); //Turn led off
  digitalWrite(laserPin2, LOW); //Turn led off
  digitalWrite(laserPin3, LOW); //Turn led off
  
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
  
  SystemStatus=0; // Game is running
  
  //Check if 'start' was pressed.
  int startSwitchStatus=digitalRead(startSwitch);
  if (startSwitchStatus == LOW)
  {
    SystemStatus=5; // start button pressed
  }

  //Check if the treasure has been stolen
  int switchStatus=digitalRead(pressureSwitch);
  if (switchStatus == HIGH)
  {
    //Yep! Treasure stolen!
    SystemStatus=6;
  }

  String serialMessage = (String)SystemStatus + ":" + (String)resistorValues[0] + "," + (String)resistorValues[1] + "," + (String)resistorValues[2] + ","  + (String)resistorValues[3];
  Serial.println(serialMessage);

  //Send the values over to the attached PC
  //Check if the PC sent an alarm value
   if (Serial.available()) 
   { 
      // If data is available to read,
      int  val = Serial.read(); // read it and store it in val from the processing app on the attached PC
     
       switch (val)
       {
          case '0': //game running
            SystemStatus=0;
            laserOn();
          break;
          
          case '1': // case 1 Laser beam broken, turn lasers off
              SystemStatus=1;
              laserOff();
          break;
          
          case '2': // case 2 System reset
           //reset system
            SystemStatus=2;
            systemReset();
          break;

          case '3':
            SystemStatus=3;
            attractMode();
          break;
       }
   }
}

void systemReset() {
    //flash lasers randomly on and off
    laserOn();
    delay(200);
      digitalWrite(ledPin, LOW); //Turn led on
      digitalWrite(laserPin0, LOW); //Turn laser on
      digitalWrite(laserPin1, LOW); //Turn laser on
      digitalWrite(laserPin2, LOW); //Turn laser on
      digitalWrite(laserPin3, LOW); //Turn laser on
      
      digitalWrite(ledPin, HIGH); //Turn led on
      digitalWrite(laserPin0, HIGH); //Turn laser on
      delay(200);
      digitalWrite(laserPin1, HIGH); //Turn laser on
      delay(200);
      digitalWrite(laserPin2, HIGH); //Turn laser on
      delay(200);
      digitalWrite(laserPin3, HIGH); //Turn laser on
  // then let the PC know I'm reset
    String serialMessage = "1:"; //need to verify what to send to PC
}

void laserOn() {
      digitalWrite(ledPin, HIGH); //Turn led on
      digitalWrite(laserPin0, HIGH); //Turn laser on
      digitalWrite(laserPin1, HIGH); //Turn laser on
      digitalWrite(laserPin2, HIGH); //Turn laser on
      digitalWrite(laserPin3, HIGH); //Turn laser on
}

void laserOff() {
      digitalWrite(ledPin, LOW); //Turn led on
      digitalWrite(laserPin0, LOW); //Turn laser on
      digitalWrite(laserPin1, LOW); //Turn laser on
      digitalWrite(laserPin2, LOW); //Turn laser on
      digitalWrite(laserPin3, LOW); //Turn laser on
}

/*
Do something that looks cool while the gsme isn't being played
*/
void attractMode() {
    digitalWrite(laserPin0, LOW);
    digitalWrite(laserPin1, LOW);
    digitalWrite(laserPin2, LOW);
    digitalWrite(laserPin3, LOW);
    for (int x = 800; x > 0; x-=200 ) {
  //    println(x);
  digitalWrite(laserPin0, HIGH);
  delay(x);
  digitalWrite(laserPin0, LOW);
  digitalWrite(laserPin1, HIGH);
  delay(x);
  digitalWrite(laserPin1, LOW);
  digitalWrite(laserPin2, HIGH);
  delay(x);
  digitalWrite(laserPin2, LOW);
  digitalWrite(laserPin3, HIGH);
  delay(x);
  digitalWrite(laserPin3, LOW);
  digitalWrite(laserPin2, HIGH);
  delay(x);
  digitalWrite(laserPin2, LOW);
  digitalWrite(laserPin1, HIGH);
  delay(x);
  digitalWrite(laserPin1, LOW);
  }
}

