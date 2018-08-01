#include <Ala.h>
#include <AlaLedRgb.h>
#include <AlaLed.h>

#include <SoftwareSerial.h>
/* Read Photo resisor values and send them to a PC (procssing) to monitor lasers in a laser maze)
   More info and circuit schematic: https://github.com/FrederictonMakerspace/LaserMaze
   Dev: Rick McCaskill, Fredericton Makerspace 2018
   Dev: James Gaudet, Fredericton Makerspace 2018
*/
const int ledPin=13;       // Led pin at Arduino pin 13

AlaLed leds;
byte pins[] = { 6, 9, 10, 11 };
const int laserPin0=6;       // Laser (5mw) connected to pin at Arduino pin 6
const int laserPin1=9;       // Laser (5mw) connected to pin at Arduino pin 9
const int laserPin2=10;      // Laser (5mw) connected to pin at Arduino pin 10
const int laserPin3=11;      // Laser (5mw) connected to pin at Arduino pin 11

const int pressureSwitch=7;  // Controls the 'treasure' detection. The treasure holds the switch closed. 
const int startSwitch=4;  // Controls the the detecting of a 'start' momentary switch being pressed
int SystemStatus=4;

int brightness = 0;    // how bright the LED is
int fadeAmount = 5; 

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
  pinMode(laserPin0, OUTPUT);  // Set laserPin - 6 pin as an output
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
//Serial.println(serialMessage);

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

          case '3': // case 3 Attract
            SystemStatus=3;
            attractMode();
          break;
          
          case '4': // case 4 GameWon!
            SystemStatus=1;
            gameWon();
          break;
       }
   }
}


//---------------------------------------------
//          ANIMATION SEQUENCES
//---------------------------------------------
// 2: Power Up = 5 sec
AlaSeq seq_powerup[] =
{
  { ALA_SPARKLE2, 500, 1000 },
  { ALA_COMET, 500, 1000 },
  { ALA_COMET, 250, 1000 },
  { ALA_FADEIN, 3000, 3000 },
  { ALA_STOP, 1000, 1000},
  { ALA_ENDSEQ }
  
};

AlaSeq seq_powerdown[] =
{
  { ALA_BLINK, 100, 1000 },
  { ALA_BLINK, 250, 1500 },
  { ALA_BLINK, 500, 2000 },
  { ALA_FADEOUT, 2000, 2000 },
  { ALA_OFF, 1000, 1000 },
  { ALA_STOP, 1000, 1000},
  { ALA_ENDSEQ }
  
};

// 3: Attract = no set time.
AlaSeq seq_attract[] =
{
  { ALA_SPARKLE2, 400, 4000 },
  { ALA_LARSONSCANNER, 2000, 2000 },
  { ALA_LARSONSCANNER, 1000, 2000 },
  { ALA_LARSONSCANNER, 500, 1000 },
  { ALA_LARSONSCANNER, 250, 1000 },
  { ALA_FADEIN, 2000, 2000 },
  { ALA_GLOW, 3000, 3000 },
  { ALA_FADEIN, 2000, 2000 },
  { ALA_STOP, 1000, 1000},
  { ALA_ENDSEQ }
};

AlaSeq seq_on[] =
{
  { ALA_ON, 1000, 1000 },
  { ALA_STOP, 1000, 1000},
  { ALA_ENDSEQ }
};

AlaSeq seq_off[] =
{
  { ALA_OFF, 1000, 1000 },
  { ALA_STOP, 1000, 1000},
  { ALA_ENDSEQ }
};

AlaSeq seq_test[] =
{
  { ALA_LARSONSCANNER, 1000, 5000 },
  { ALA_STOP, 1000, 1000},
  { ALA_ENDSEQ }
};

AlaSeq seq_gamewon[] =
{
  { ALA_OFF, 1000, 1000 },
  { ALA_FADEIN, 2000, 2000 },
  { ALA_COMET, 1000, 2000 },
  { ALA_COMET, 2000, 2000 },
  { ALA_FADEOUT, 3000, 3000 },
  { ALA_STOP, 1000, 1000},
  { ALA_ENDSEQ }
};
//-------------------------------

void playRandom()
{
  playBlockingAnimation(seq_test);
}

void laserOn() {
  playBlockingAnimation(seq_on);
}

void laserOff() {
    playBlockingAnimation(seq_off);
}

// Mode 2 (5 seconds for sound) - System is 'booting up' or resetting
void systemReset() {
  //flash lasers randomly on and off
  playBlockingAnimation(seq_powerup);

  // then let the PC know I'm reset
  String serialMessage = "1:"; //need to verify what to send to PC
}

// Mode 3 - Do something that looks cool while the game isn't being played
void attractMode() {
  playBlockingAnimation(seq_attract);
}

// Mode 4 - Player has completed the game successfully
void gameWon() {
  playBlockingAnimation(seq_gamewon);
}



void playBlockingAnimation(AlaSeq seq_animation[])
{
  leds.reset();
  leds.initPWM(4, pins);

  leds.setAnimation(seq_animation);
  while (1)
  {
    leds.runAnimation();
    if (leds.getStoppedFlag()) break;
  }
}
