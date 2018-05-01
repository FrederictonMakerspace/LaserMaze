/*
Laser Maze - 2018
Author Rick McCaskill, Fredericton Makerspace

Inspired by bkhurt http://www.instructables.com/id/LASER-Maze-2012-Halloween-Haunted-House/

Code to control a laser maze game where breaking a laser beam causes an alarm.

--------
Demo Mode
  set demoMode = true to see the interface without needing an arduino plugged in.

--------
Input:
  Expects serial data (4 comma separated values representing 4 sensors). 
  Data format: csv list of readings from a photoresistor. Example: 84,94,32,12
  Lower values mean less light.
  If and indidual value falls below a low threshold value (See MazeData), the 'alarm' is triggered.
  
Tuning:
  MazeData contains the data for the maze. if you want to 'tune' the threashold value, edit lowThreshold0, lowThreshold1,lowThreshold2,lowThreshold3
--------
*/
import processing.serial.*; // serial communication library
import processing.sound.*;
 
boolean demoMode = true; //True to generate dummy data instead of reading from serial
int alarmsTriggered = 0;
boolean gamePaused = true;


SoundFile alarmSound;
Serial myPort; // The serial port
boolean alarm = false;  // is the alarm servo on or off

String lastSerialData = "";  // What's been read off of serial OR randomly generated if demoMode = true

int startTime = 0; //Time the current run started
int ellapsedTime = 0; //Elapsed time of the current run (between start and stop)
PFont gameFont20;
PFont gameFont50;

Button startButton; 
Button stopButton; 

void setup () {
  // set the window size:
  size(1000, 500);
  
  frameRate(10);  // Run 10 frames per second
  
  //**********
  // set inital background layout:
  //Bue left
  background(#152935); 
  //White White
  fill(#FFFFFF);
  rect(width-350, 0, width, height);
  //**********

  // load graphics
  PImage logo = loadImage("fms_logo.png");
  logo.resize(250,0);
  image(logo, 700, 30);

  textAlign(CENTER, TOP);
  fill(#1f1f1f);

  //String[] fontList = PFont.list();
  //printArray(fontList);
  gameFont20 = createFont("Helvitica",20);
  gameFont50 = createFont("Helvitica",50);

  textFont(gameFont50);
  text("Laser Maze", 824, 100);
  textFont(gameFont20);
  
  // List all the available serial ports
  println(Serial.list());
  
  if (!demoMode)
  {
    myPort = new Serial(this, Serial.list()[2], 9600);
    // don't generate a serialEvent() unless you get a newline character:
    myPort.bufferUntil('\n');
  }
  
  // load sounds
  alarmSound = new SoundFile(this, sketchPath("Siren_Noise_short.mp3"));  // load sound effects
  
  startTime = millis();
}

void draw () {
    MazeData sensorData = getSensorData(demoMode); //Get Current Data (from serial or demo)
    if (sensorData == null) return;
    
    startButton = new Button(700, 410, "Start", #749ECA);
    startButton.draw();
    stopButton = new Button(850, 410, "Stop", #43f381);
    stopButton.draw();
    
    //TODO - could be put into an array, I suppose! 
    
    //Create a bar graph, set the data and draw it (x4)
    SensorBar bar0 = new SensorBar(30,10); //instantiate a bar (visual representation) with an X and Y for where I want this bar to draw
    bar0.setSensorValues(sensorData.getSensor0Value(), sensorData.getLowThreathold0());
    bar0.setLabel("Sensor 1");
    bar0.draw();

    SensorBar bar1 = new SensorBar(130,10);
    bar1.setSensorValues(sensorData.getSensor1Value(), sensorData.getLowThreathold1());
    bar1.setLabel("Sensor 2");
    bar1.draw();

    SensorBar bar2 = new SensorBar(230,10);
    bar2.setSensorValues(sensorData.getSensor2Value(), sensorData.getLowThreathold2());
    bar2.setLabel("Sensor 3");
    bar2.draw();

    SensorBar bar3 = new SensorBar(330,10);
    bar3.setSensorValues(sensorData.getSensor3Value(), sensorData.getLowThreathold3());
    bar3.setLabel("Sensor 4");
    bar3.draw();

    SensorBar bar4 = new SensorBar(430,10);
    //bar4.setSensorValues(sensorData.getSensor3Value(), sensorData.getLowThreathold3());
    bar4.setLabel("No 5");
    bar4.draw();

    Alarm alarm = new Alarm(750,220);
    if (!gamePaused)
      alarm.setIsAlarmTripped(sensorData.isBeamBroken());
    alarm.draw();

    if (!gamePaused)
      ellapsedTime = millis() - startTime;

    // Time to sound the alarm?
    if (!gamePaused && sensorData.isBeamBroken())
    {
      if (!demoMode)
        myPort.write('1'); //Tell the arduino that the threahold has been crossed so that it can take action.
      
      println("ALARM " + alarmsTriggered + "! Beam " + sensorData.getBrokenBeam() + " has been broken");
      alarmsTriggered+=1;
      
      soundAlarm();
      delay(2); //10 seconds
      ellapsedTime = 0;
      startTime = millis();
    }
    else
    {
      if (!demoMode)
        myPort.write('0'); 
    }
    
    
    //Draw elapsed Time
    int timeX = 730;
    int timeY = 300;    
    int timeWidth = 200;
    int timeHeight = 80;
    fill(#FFFFFF);  // white
    stroke(#FFFFFF);  // white
    rect(timeX, timeY, timeWidth, timeHeight);

    fill(#303030);  // grey
    String formattedTime = nf(float(ellapsedTime) / float(1000),0,3);
    textSize(20);
    textAlign(CENTER, TOP);
    
    String label = "Ellapsed Time";
    if (gamePaused)
      label = "PAUSED";
      
    text(label, timeX + timeWidth/2, timeY);
    text(formattedTime, timeX + timeWidth/2, timeY + 30);

  }

void soundAlarm()
{
  alarmSound.play();
}

void mousePressed() {
  if (startButton.isMouseOver())
  {
     gamePaused = !gamePaused;
  }
  
  if (stopButton.isMouseOver())
  {
  }
}

class Alarm extends GuiComponent {
  private boolean isAlarmOn;
  
  Alarm(int x, int y)
  {
    super(x, y, 170, 50);
    isAlarmOn = false;
  }
  
  void setIsAlarmTripped(boolean alarmTripped)
  {
    isAlarmOn = alarmTripped;
  }
  
  void draw()
  {
    fill(#1f1f1f);
    textSize(20);
    textAlign(CENTER, CENTER);
    text("Alarm", this.getX() + this.getWidth() / 2, this.getY() - 20);

    //Default 'off' colors.
    color fillColor = #AACA74;
    String alarmText = "OFF";
    
    if (isAlarmOn)
    {
      fillColor = #FF0000; //Change to RED
      alarmText = "ON";
    }
    
    //Draw Alarm graphic indicator
    fill(fillColor);  // RED
    stroke(#303030);  // dark grey
    rect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    fill(#303030);  // grey
    textSize(20);
    
    textAlign(CENTER, CENTER);
    text(alarmText, this.getX() + this.getWidth()/2, this.getY() + this.getHeight()/2);
  }
}

class SensorBar extends GuiComponent {
  private String sensorLabel; //Name to show at the bottom of the sensor bar

  private color borderColor = #D1D1D1;
  private color backgroundColor = #F1F1F1;
  private color thresholdColor = #FF0000;

  private int sensorValue, lowerBound;

  //Constructor. X and y are upper left corner.
  SensorBar(int x, int y) 
  {
    super(x,y, 50, 400);
    
    this.sensorLabel = "Hello";
    
    this.sensorValue = 0;
    this.lowerBound = 0;
    
  }

  void setSensorValues(int sensorValue, int lowerBound)
  {
    this.sensorValue = sensorValue;
    this.lowerBound = lowerBound;
  }
  
  void setLabel(String label)
  {
    this.sensorLabel = label;
  }

  /**
  Return a color based on how close value is to lowerBound. Lower values are closer to 'alarm' red. Range of value is between 0 - 1023
  **/
  color getFillColor(int value, int lowerBound) {
    color returnColor = #00FF00;
    
    if ( value < lowerBound )
      returnColor = #CE0000; //RED
    else if ( value < (lowerBound + 20)) 
      returnColor =  #E8922A; //Orange
    else
      returnColor = #AACA74; //Green 
    
    return returnColor;
  }

  void draw()
  {
    //Graph Border
    stroke(borderColor);  // dark grey
    strokeWeight(2);
    
    //Graph Background
    fill(backgroundColor);  // grey
    rect(this.getX(), this.getY(), this.getWidth(), this.getHeight());

    //Fill based on input
    fill(this.getFillColor(this.sensorValue, this.lowerBound));  // grey
    int scaledHeight = floor(map(this.sensorValue, 0, 100, 0, this.getHeight()));  // map the input value to fit within the graph
    rect(this.getX(), this.getY() + (this.getHeight() - scaledHeight), this.getWidth(), scaledHeight); 

    //Draw threshold indicator
    stroke(thresholdColor);  // Red
    
    //Scale threshold
    int scaledThreshold = floor(map(this.lowerBound, 0, 100, 0, this.getHeight()));  // map the input value to fit within the graph
    //line(this.barX, this.barY + (this.barHeight - scaledThreshold), this.barX + this.barWidth, this.barY + (this.barHeight - scaledThreshold));
    
    line(this.getX(), this.getY() + (this.getHeight() - scaledThreshold), this.getX() + this.getWidth(), this.getY() + (this.getHeight() - scaledThreshold));

    //Draw Label at the bottom
    textAlign(CENTER, TOP);
    fill(#DDDDDD);
    textSize(20);
    text(this.sensorLabel, this.getX() + (this.getWidth() / 2), this.getY() + this.getHeight() + 10);
  }
}

//Sensor State
class MazeData {
    private int sensor0Value;
    private int sensor1Value;
    private int sensor2Value;
    private int sensor3Value;
    
    private int lowThreshold0;
    private int lowThreshold1;
    private int lowThreshold2;
    private int lowThreshold3;
    
    MazeData()
    {
      this.sensor0Value = 0;
      this.sensor1Value = 0;
      this.sensor2Value = 0;
      this.sensor3Value = 0;

      this.lowThreshold0 = 23;
      this.lowThreshold1 = 23;
      this.lowThreshold2 = 23;
      this.lowThreshold3 = 23;
    }
    
    MazeData(String inputValues) throws Exception
    {
      this(); // Call default contructor to set defatult values
      
      if (inputValues == null)
      {
        throw new Exception("Null is a valid constructor to MazeData");
      }
      else
      {
        String values[] = inputValues.split(",");
        if (values.length < 4)
        {
          throw new Exception("MazeData needs 4 values to initialize");
        }
        
        this.sensor0Value = int(values[0]);
        this.sensor1Value = int(values[1]);
        this.sensor2Value = int(values[2]);
        this.sensor3Value = int(values[3]);
      }
    }
    
    public int getSensor0Value() {
      return this.sensor0Value;
    }
    public int getSensor1Value() {
      return this.sensor1Value;
    }
    public int getSensor2Value() {
      return this.sensor2Value;
    }
    public int getSensor3Value() {
      return this.sensor3Value;
    }
    
    public int getLowThreathold0() {
      return this.lowThreshold0;
    }
    public int getLowThreathold1() {
      return this.lowThreshold1;
    }
    public int getLowThreathold2() {
      return this.lowThreshold2;
    }
    public int getLowThreathold3() {
      return this.lowThreshold3;
    }
    
    public int getBrokenBeam()
    {
        if (this.sensor0Value < this.lowThreshold0)
        {
          println("0: " + this.sensor0Value + "<" + this.lowThreshold0 );
          return 0;
        }
        else if (this.sensor1Value < this.lowThreshold1)
        {
          println("1: " + this.sensor1Value + "<" + this.lowThreshold1 );
          return 1;
        }
        else if (this.sensor2Value < this.lowThreshold2)
        {
          println("2: " + this.sensor2Value + "<" + this.lowThreshold2 );
          return 2;
        }
        else if (this.sensor3Value < this.lowThreshold3)
        {
          println("3: " + this.sensor3Value + "<" + this.lowThreshold3 );
          return 3;
        }
        else
          return -1;        
    }
    
    boolean isBeamBroken()
    {
      return (getBrokenBeam() > -1);
    }
    
    String toString()
    {
      return sensor0Value + "," + sensor1Value + "," + sensor2Value + "," + sensor3Value;
    }
}

MazeData getSensorData(boolean mockData) {
  if (mockData)
  {
    //Valid values are between 0-100 (will vary depending ont the resistors used on the sensor. Curently using 220ohm
     //Light Sensors 0-3
    int Mock0 = int(random(70,  89));
    int Mock1 = int(random(20,  89));
    int Mock2 = int(random(20, 87));
    int Mock3 = int(random(20, 90));
    
    // Grab a random result to mock various data from the serial
    lastSerialData = Mock0 + "," + Mock1 + "," + Mock2 + "," + Mock3;
  }
  
  try
  {
    return new MazeData(lastSerialData);
  }
  catch (Exception e)
  {
    println(e);
    return null;
  }
}

class Button extends GuiComponent {
  private boolean isButtonPressed;
  
  private String buttonText = "_undefined_";
  private color buttonColor = #749ECA;
  
  Button(int x, int y, String text, color buttonColor)
  {
    super(x,y, 130,50);
    
    this.isButtonPressed = false;
    this.buttonText = text;
    this.buttonColor = buttonColor;
  }
  
  public boolean isMouseOver()
  {
    if (mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && 
        mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight() ) {
      return true;
    } else {
      return false;
    }    
  }
  
  void draw()
  {
    //Default 'off' colors.
    color fillColor = this.buttonColor;
    
    if (isMouseOver())
      fillColor = #440000;
    
    if (isButtonPressed)
    {
      fillColor = #FF0000; //Change to RED
    }

    //Draw Button
    fill(fillColor);
    stroke(#888888);  //Darker outline 

    rect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 2);
    fill(#FFFFFF);  // Text Color
    textSize(20);
    
    textAlign(CENTER, CENTER);
    text(this.buttonText, this.getX() + this.getWidth()/2, this.getY() + this.getHeight()/2);
  }
}


/*
Called when serial data is read. Then we store the results which are used in getSensorData()
*/
void serialEvent (Serial myPort) {
  //Assume the data is in a comma string "12,45,76,80"

  lastSerialData = myPort.readStringUntil('\n');    // get the ASCII string
}

void stop() {
  myPort.stop();  // stop serial com
  super.stop();  // allow normal clean up routing to run after stop()
}

/*
Base Class for drawing things
*/
class GuiComponent
{
  private int x,y; //Upper left corner
  private int w,h; //Width height of element to be painted
  
  GuiComponent()
  {
    this.x = this.y = 0;
    this.w = this.h = 0;
  }

  GuiComponent(int x, int y, int w, int h)
  {
    this();
    this.x = x;
    this.y = y;
    
    this.w = w;
    this.h = h;
  }
  
  public int getX()
  {
     return this.x;
  }
  public int getY()
  {
     return this.y;
  }
  public int getWidth()
  {
     return this.w;
  }
  public int getHeight()
  {
     return this.h;
  }
  
  void draw()
  {
    //Override to draw your control on the screen
  }
}
