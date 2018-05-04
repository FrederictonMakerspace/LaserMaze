/* Use a photoresistor (or photocell) to turn on an LED in the dark
   More info and circuit schematic: http://www.ardumotive.com/how-to-use-a-photoresistor-en.html
   Dev: Michalis Vasilakis // Date: 8/6/2015 // www.ardumotive.com */
   

//Constants
const int pResistor = A0; // Photoresistor at Arduino analog pin A0
const int ledPin=13;       // Led pin at Arduino pin 13
const int laserPin=8;       // Led pin at Arduino pin 13


//Variables
int value;          // Store value from photoresistor (0-1023)

void setup(){
 pinMode(ledPin, OUTPUT);  // Set lepPin - 13 pin as an output
 pinMode(laserPin, OUTPUT);  // Set lepPin - 13 pin as an output

 pinMode(pResistor, INPUT);// Set pResistor - A0 pin as an input (10k resistor)

 digitalWrite(ledPin, LOW); //Turn led off
 digitalWrite(laserPin, LOW); //Turn led off

 Serial.begin(9600);
}

void loop(){
  value = analogRead(pResistor);

    //Serial.println(value);
  //Check if the PC sent an alarm value
   if (Serial.available()) 
   { // If data is available to read,
     int val = Serial.read(); // read it and store it in val

/* New Code to handle values from PC
switch (val) {
  case 0:
     digitalWrite(ledPin, HIGH); //Turn led on
     digitalWrite(laserPin, HIGH); //Turn laser on
  break;
  // case 1 Laser beam broken, turn lasers off
  case 1:
      digitalWrite(ledPin, LOW); //Turn led off
      digitalWrite(laserPin, LOW); //Turn laser off
  break;
  // case 2 System reset
  case 2:
//reset system
   systemReset();
  break;
}
*/

  

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
    
  //You can change value "25"
  //if (value > 60){
  //  digitalWrite(ledPin, LOW);  //Turn led off
 // }
  //else{
  //  digitalWrite(ledPin, HIGH); //Turn led on
  //}

  //Wrap for compatibility with the laser processing sketch
  String serialMessage = (String)value;
  serialMessage.concat("0");
  Serial.println(serialMessage);

  delay(50); //Small delay
}



void systemReset() {
  // turn alarm off
  // turn lasers on
  // reset PC so timer is 0, and start button is available.
}
