import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import processing.sound.*; 
import controlP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Laser_Maze_2018_Processing_FMS extends PApplet {

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
  Expects serial data (4 comma separated values representing 4 sensors). Prepended with the 'state' of the arduino
  Data format: csv list of readings from a photoresistor. Example: 3:84,94,32,12
  Lower values mean less light.
  If and indidual value falls below a low threshold value (See MazeData), the 'alarm' is triggered.

Output / Communcations (Messages sent to attached Arduino)
  "0" - Everything is fine. The game is running. No beams broken.
  "1" - Beam has been broken. We've sounded an alarm.
  "2" - The game is to be reset back to a clean state. I.E. either booting UP or being reset AFTER the game has been won / lost and you want to play again.
        Envisioning something like lights blink, lasers flicker, etc. to show 'booting up' visually.
  "3" - Attract Mode
  
Tuning:
  MazeData contains the data for the maze. if you want to 'tune' the threashold value, edit lowThreshold0, lowThreshold1,lowThreshold2,lowThreshold3
--------

Sounds used under Creative Commons by: 
https://freesound.org/people/halomaniac/sounds/57364/
https://freesound.org/people/Timbre/sounds/138002/
--------
*/
 // serial communication library



ControlP5 cp5;

boolean demoMode = true; //True to generate dummy data instead of reading from serial

MazeData mazeData = new MazeData(); // This object holds the state of the game 

//Sounds Effects
SoundFile alarmSound;
SoundFile powerUpSound;
SoundFile countDownSound;
SoundFile gameWonSound;
SoundFile powerDownSound;
SoundFile themeSongSound;

Serial myPort; // The serial port
boolean alarm = false;  // is the alarm servo on or off

String lastSerialData = "";  // What's been read off of serial OR randomly generated if demoMode = true

PFont gameFont20;
PFont gameFont50;

Button startButton; 
Button stopButton; 
Button resetButton;
Button attractButton;
Toggle themeMusicButton;

Slider slider1;

PImage logo;

public void setup () {
  // set the window size:
  

  frameRate(5);  // Run 5 frames per second

  // load graphics
  logo = loadImage("Logo_type_white.png");
  logo.resize(250,0);
  cp5 = new ControlP5(this);

  themeMusicButton = cp5.addToggle("Theme Music")
    .setPosition(740,465)
    .setValue(true)
    .setColorLabel(0xffFFFFFF);

slider1 = cp5.addSlider("v1")
       .setPosition(15, 20)
       .setSize(10, 400)
       .setRange(0, 200)
       .setValue(40)
       .setColorCaptionLabel(color(20,20,20));

  //String[] fontList = PFont.list();
  //printArray(fontList);
  gameFont20 = createFont("Helvitica",20);
  gameFont50 = createFont("Helvitica",50);

  
  // List all the available serial ports
  println(Serial.list());

  if (!demoMode)
  {
    myPort = new Serial(this, Serial.list()[2], 9600);
    // don't generate a serialEvent() unless you get a newline character:
    myPort.bufferUntil('\n');
    
    sendStateToArduino('2'); // Send 'reset'
  }
  
  // load sounds
  alarmSound = new SoundFile(this, sketchPath("Siren_Noise_short.mp3"));
  powerUpSound = new SoundFile(this, sketchPath("Power_Up2.mp3"));
  countDownSound = new SoundFile(this, sketchPath("Countdown.mp3"));
  gameWonSound = new SoundFile(this, sketchPath("57364__halomaniac__mission-complete-2-0.mp3"));
  powerDownSound = new SoundFile(this, sketchPath("Power_Down.mp3"));
  themeSongSound = new SoundFile(this, sketchPath("Mission Impossible Theme.mp3"));
  
  startButton = new Button(700, 410, "Start", 0xff003B70);
  stopButton = new Button(700, 410, "Stop", 0xff0071E4);

  attractButton = new Button(850, 470, "Attract", 0xff0d0d0d);
  attractButton.setHeight(25);
}

public void draw () {
  mazeData.update(); //Update game state for ellapsed time, etc.

  // Check if we should play the theme song, or mute it
  if (themeMusicButton.getBooleanValue() == true)
  {
    themeSongSound.amp(1);
  }
  else
  {
    themeSongSound.amp(0);
  }

  textAlign(CENTER, TOP);

  //**********
  //Set inital background layout:
  // Blue left
  background(0xff1D364A); // 152935

  //meters background
  fill(0xff162836);
  noStroke();
  rect(10, 10, 500, height - 20);
  
  logo.resize(250,0);
  image(logo, 700, 30);
  //***
  fill(0xffffffff);
  textFont(gameFont50);
  text("Laser Maze", 824, 100);
  textFont(gameFont20);
  //**********

    textAlign(LEFT, TOP);
    stroke(0xffFFFFFF);
    fill(0xffFFFFFF);
    
    if (mazeData.getGameState() == MazeData.STATE_RUNNING)
    {
      stopButton.setVisible(true);
      startButton.setVisible(false);
    }
    else
    {
      stopButton.setVisible(false);
      startButton.setVisible(true);
    }
    stopButton.draw();
    startButton.draw();

    resetButton = new Button(850, 410, "Reset", 0xff0071E4);
    resetButton.draw();
    
    attractButton.draw();
    
    //Create a bar graph, set the data and draw it (x4)
    SensorBar bar0 = new SensorBar(30,20); //instantiate a bar (visual representation) with an X and Y for where I want this bar to draw
    bar0.setSensorValues(mazeData.getSensor0Value(), mazeData.getLowThreathold0());
    bar0.setLabel("Sensor 1");
    bar0.draw();

    SensorBar bar1 = new SensorBar(130,20);
    bar1.setSensorValues(mazeData.getSensor1Value(), mazeData.getLowThreathold1());
    bar1.setLabel("Sensor 2");
    bar1.draw();

    SensorBar bar2 = new SensorBar(230,20);
    bar2.setSensorValues(mazeData.getSensor2Value(), mazeData.getLowThreathold2());
    bar2.setLabel("Sensor 3");
    bar2.draw();

    SensorBar bar3 = new SensorBar(330,20);
    bar3.setSensorValues(mazeData.getSensor3Value(), mazeData.getLowThreathold3());
    bar3.setLabel("Sensor 4");
    bar3.draw();

    SensorBar bar4 = new SensorBar(430,20);
    //bar4.setSensorValues(sensorData.getSensor3Value(), sensorData.getLowThreathold3());
    bar4.setLabel("No 5");
    bar4.draw();

    Alarm alarm = new Alarm(750,220);
    if (!mazeData.isGamePaused())
      alarm.setIsAlarmTripped(mazeData.isBeamBroken());
    alarm.draw();

    // Time to sound the alarm?
    if (!mazeData.isGamePaused() && (mazeData.getGameState() == MazeData.STATE_RUNNING) && mazeData.isBeamBroken())
    {
      themeSongSound.stop();

      sendStateToArduino('1'); //Tell the arduino that the threashold has been crossed so that it can take action.
      
      println("ALARM " + mazeData.getBeamsBroken() + "! Beam " + mazeData.getBrokenBeam() + " has been broken");

      //Game done. Keep lasers off (need to manual set back on)
      mazeData.setGameState(MazeData.STATE_LOST);

      soundAlarm();
      delay(1000); //1 seconds
      
    }

    //Check if the treasure has been grabbed
    if (mazeData.isTreasureStolen())
    {
      mazeData.setGameState( MazeData.STATE_COMPLETE );
    }


    //Draw elapsed Time
    int timeX = 730;
    int timeY = 300;    
    int timeWidth = 200;
    int timeHeight = 80;
    fill(0xffFFFFFF);  // white
    stroke(0xffFFFFFF);  // white
    rect(timeX, timeY, timeWidth, timeHeight);

    fill(0xff303030);  // grey
    String formattedTime = nf(PApplet.parseFloat(mazeData.getEllapsedTime()) / PApplet.parseFloat(1000),0,3);
    textSize(20);
    textAlign(CENTER, TOP);
    
    String label = "Ellapsed Time";
    if (mazeData.isGamePaused())
      label = "PAUSED";
      
    text(label, timeX + timeWidth/2, timeY);
    text(formattedTime, timeX + timeWidth/2, timeY + 30);
    
    
    if (mazeData.getGameState() == MazeData.STATE_RESETTING)  
    {
      themeSongSound.stop();

      sendStateToArduino('2'); //Tell the arduino that the get needs to be 'reset'

      powerUpSound.play();
      delay(100);
      mazeData.setGameState(MazeData.STATE_ATTRACT);
    }
    
    if (mazeData.getGameState() == MazeData.STATE_GET_READY)  
    {
      sendStateToArduino('0'); // let the arduino know it's go time!
      countDownSound.play();
      delay(PApplet.parseInt(countDownSound.duration()) * 1050);
      
      themeSongSound.play();
      mazeData.setGameState(MazeData.STATE_RUNNING);
    }
    
    if (mazeData.getGameState() == MazeData.STATE_COMPLETE)
    {
      themeSongSound.stop();
      
      gameWonSound.amp(1);
      gameWonSound.play();
      delay(PApplet.parseInt(gameWonSound.duration()) * 1000);

      delay(1000);

      powerDownSound.amp(0.1f);
      powerDownSound.play();
      delay(PApplet.parseInt(powerDownSound.duration()) * 1000);
      
      mazeData.setGameState(MazeData.STATE_ATTRACT);
    }
    
    
  textAlign(LEFT, TOP);
  text("State:  " + mazeData.getGameStateLabel() + " (" + mazeData.getGameState() + ")", 13, 460);

  }

public void soundAlarm()
{
  alarmSound.play();
}

public void mousePressed() {
  if (startButton.isVisible() && startButton.isMouseOver())
  {
     mazeData.setGameState( MazeData.STATE_GET_READY );
  }
  
  if (stopButton.isVisible() && stopButton.isMouseOver())
  {
    mazeData.setGameState( MazeData.STATE_COMPLETE );
  }

  if (resetButton.isVisible() && resetButton.isMouseOver())
  {
    mazeData.resetGame();
  }

  if (attractButton.isVisible() && attractButton.isMouseOver())
  {
    mazeData.setGameState( MazeData.STATE_ATTRACT );
    sendStateToArduino('3');
  }
}

class Alarm extends GuiComponent {
  private boolean isAlarmOn;
  
  Alarm(int x, int y)
  {
    super(x, y, 170, 50);
    isAlarmOn = false;
  }
  
  public void setIsAlarmTripped(boolean alarmTripped)
  {
    isAlarmOn = alarmTripped;
  }
  
  public void draw()
  {
    fill(0xffffffff);
    textSize(20);
    textAlign(CENTER, CENTER);
    text("Alarm", this.getX() + this.getWidth() / 2, this.getY() - 20);

    //Default 'off' colors.
    int fillColor = 0xffAACA74;
    String alarmText = "OFF";
    
    if (isAlarmOn)
    {
      fillColor = 0xffFF0000; //Change to RED
      alarmText = "ON";
    }
    
    //Draw Alarm graphic indicator
    fill(fillColor);  // RED
    noStroke();
    rect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    fill(0xffFFFFFF);  // grey
    textSize(20);
    
    textAlign(CENTER, CENTER);
    text(alarmText, this.getX() + this.getWidth()/2, this.getY() + this.getHeight()/2);
  }
}

class SensorBar extends GuiComponent {
  private String sensorLabel; //Name to show at the bottom of the sensor bar

  private int backgroundColor = 0xff00505E;
  private int thresholdColor = 0xffFF0000;

  private int sensorValue, lowerBound;

  //Constructor. X and y are upper left corner.
  SensorBar(int x, int y) 
  {
    super(x,y, 50, 400);
    
    this.sensorLabel = "Hello";
    
    this.sensorValue = 0;
    this.lowerBound = 0;
    
  }

  public void setSensorValues(int sensorValue, int lowerBound)
  {
    this.sensorValue = sensorValue;
    this.lowerBound = lowerBound;
  }
  
  public void setLabel(String label)
  {
    this.sensorLabel = label;
  }

  /**
  Return a color based on how close value is to lowerBound. Lower values are closer to 'alarm' red. Range of value is between 0 - 1023
  **/
  public int getFillColor(int value, int lowerBound) {
    int returnColor = 0xff00FF00;
    
    if ( value < lowerBound )
      returnColor = 0xff8B270A; //#CE0000; //RED
    else if ( value < (lowerBound + 20)) 
      returnColor =  0xffEBA202; //#E8922A; //Orange
    else
      returnColor = 0xff00BBF6; //#AACA74; //Green 
    
    return returnColor;
  }

  public void draw()
  {
    
    //Graph Border
    noStroke();
    //stroke(borderColor);  // dark grey
    //strokeWeight(2);
    
    //Graph Background
    fill(backgroundColor);  // grey
    rect(this.getX(), this.getY(), this.getWidth(), this.getHeight());

    //Fill based on input
    fill(this.getFillColor(this.sensorValue, this.lowerBound));
    int scaledHeight = floor(map(this.sensorValue, 0, 100, 0, this.getHeight()));  // map the input value to fit within the graph
    if (scaledHeight > this.getHeight()) scaledHeight = this.getHeight(); // stop over draw when values are too high from the input
    rect(this.getX(), this.getY() + (this.getHeight() - scaledHeight), this.getWidth(), scaledHeight); 

    //Draw threshold indicator
    stroke(thresholdColor);  // Red
    
    //Scale threshold
    int scaledThreshold = floor(map(this.lowerBound, 0, 100, 0, this.getHeight()));  // map the input value to fit within the graph
    
    line(this.getX(), this.getY() + (this.getHeight() - scaledThreshold), this.getX() + this.getWidth(), this.getY() + (this.getHeight() - scaledThreshold));

    //Draw Label at the bottom
    textAlign(CENTER, TOP);
    fill(0xffDDDDDD);
    textSize(20);
    text(this.sensorLabel, this.getX() + (this.getWidth() / 2), this.getY() + this.getHeight() + 10);
  }
}

public void sendStateToArduino(char state)
{
  try
  {
    if (myPort != null)
      myPort.write(state);
  }
  catch (Exception e)
  {
    println("Error sending state to arduino: " + e);
  }
}

//Game State
class MazeData {
  
  final static public int STATE_RESETTING = 0;
  final static public int STATE_GET_READY = 5; //User has pressed start - count down!
  final static public int STATE_RUNNING = 1;
  final static public  int STATE_BEAMBROKEN = 2;
  final static public  int STATE_ATTRACT = 3;
  final static public  int STATE_PAUSED = 9;
  final static public  int STATE_COMPLETE = 6; //Player has won he game
  final static public  int STATE_LOST = 7; //Player has won he game
  
  private boolean gamePaused;
  
  private int gameState;  
  private int beamsBroken;
  
  private int sensor0Value;
  private int sensor1Value;
  private int sensor2Value;
  private int sensor3Value;
  
  private int sensorAverageCount = 3;
  private IntList sensor0Values = new IntList();
  private IntList sensor1Values = new IntList();
  private IntList sensor2Values = new IntList();
  private IntList sensor3Values = new IntList();
  
  
  private int lowThreshold0;
  private int lowThreshold1;
  private int lowThreshold2;
  private int lowThreshold3;
  
  private boolean treasureSwitchValue;
  
  //Timing data
  private int startTime;
  private int ellapsedTime;
    
    MazeData()
    {
      this.sensor0Value = 0;
      this.sensor1Value = 0;
      this.sensor2Value = 0;
      this.sensor3Value = 0;

      this.lowThreshold0 = 10;
      this.lowThreshold1 = 0;
      this.lowThreshold2 = 0;
      this.lowThreshold3 = 0;
      
      resetGame();
      
      //Initialize the rolling average
      for (int i=0; i<sensor0Values.size(); i++)
      {
        sensor0Values.set(i,0);
      }
    }

    MazeData(int lowThreshold0, int lowThreshold1, int lowThreshold2, int lowThreshold3)
    {
      super();

      this.lowThreshold0 = lowThreshold0;
      this.lowThreshold1 = lowThreshold1;
      this.lowThreshold2 = lowThreshold2;
      this.lowThreshold3 = lowThreshold3;
    }
    
    public void update()
    {
      if (this.gameState == STATE_RUNNING)
        ellapsedTime = millis() - startTime;
    
      try
      {
        mazeData.setSensorData(getSensorData(demoMode));
      }
      catch (Exception e)
      {
        println(e);
      }
    }
    
    public int getEllapsedTime()
    {
        return ellapsedTime;
    }
    
    public void resetGame()
    {
      this.gameState = STATE_RESETTING;
      
      this.beamsBroken = 0;

      this.ellapsedTime = 0;
      this.startTime = 0;
    }
    
    public boolean isGamePaused()
    {
      return this.gamePaused;
    }
    public void setGamePaused(boolean paused)
    {
       this.gamePaused = paused;
    }
    
    private int counter0, counter1, counter2, counter3 = 0; 
    public void setSensorData(String inputValues) throws Exception
    {
      if (inputValues == null)
      {
        throw new Exception("Null is NOT a valid constructor to MazeData");
      }
      else
      {
        String values[] = inputValues.split(",");
        if (values.length < 4)
        {
          throw new Exception("MazeData needs 4 values to initialize");
        }
      
        //0 - Store 'sensorAverageCount' (3, for example) values in a circual fashion.  
        //   This is so we can average out the readings over a few intervals to protect against random 0 readings.
        int value0 = PApplet.parseInt(trim(values[0]));
        sensor0Values.set(counter0, value0);
        counter0++;
        if (counter0 > this.sensorAverageCount) counter0 = 0; //loop back to the first.
        this.sensor0Value = value0;
        
        //1
        int value1 = PApplet.parseInt(trim(values[1]));
        sensor1Values.set(counter1, value1);
        counter1++;
        if (counter1 > this.sensorAverageCount) counter1 = 0; //loop back to the first.
        this.sensor1Value = value1;

        //2
        int value2 = PApplet.parseInt(trim(values[2]));
        sensor2Values.set(counter2, value2);
        counter2++;
        if (counter2 > this.sensorAverageCount) counter2 = 0; //loop back to the first.
        this.sensor2Value = value2;

        //3
        int value3 = PApplet.parseInt(trim(values[3]));
        sensor3Values.set(counter3, value3);
        counter3++;
        if (counter3 > this.sensorAverageCount) counter3 = 0; //loop back to the first.
        this.sensor3Value = value3;
/*      
        println("v0 " + values[0]);
        println("v1 " + values[1]);
        println("v2 " + values[2]);
        println("v3 " + values[3]);

        println("check0: " + this.sensor0Value);
        println("check1: " + this.sensor1Value);
        println("check2: " + this.sensor2Value);
        println("check3: " + this.sensor3Value);
*/
      }
    }
    
    /**
    Used to compute the average sensor value - 'smoothing' it out.
    **/
    private int average(IntList values)
    {
      int sum = 0;
      
      print("size (" + values.size() + "): " );
      for (int i=0; i<values.size(); i++)
      {
        print("i=" + i + ": ");
        print(values.get(i) + ",");
        sum += values.get(i);
      }

     println(" average: " + sum / values.size());
     println("");
      return sum / values.size();
    }
    
    public int getSensor0Value() {
      //Return the average Trying to avoid random '0s' from the photosensors
      return average(this.sensor0Values);
      
      //return this.sensor0Value;
    }
    public int getSensor1Value() {
      //Return the average Trying to avoid random '0s' from the photosensors
      return average(this.sensor1Values);
      
      //return this.sensor1Value;
    }
    public int getSensor2Value() {
      //Return the average Trying to avoid random '0s' from the photosensors
      return average(this.sensor2Values);
      
      //return this.sensor2Value;
    }
    public int getSensor3Value() {
      //Return the average Trying to avoid random '0s' from the photosensors
      return average(this.sensor3Values);
      
      //return this.sensor3Value;
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
    
    private String getSensorData(boolean mockData)
    {
      if (mockData)
      {
        //Valid values are between 0-100 (will vary depending ont the resistors used on the sensor. Curently using 220ohm
         //Light Sensors 0-3
        int Mock0 = PApplet.parseInt(random(70,  89));
        int Mock1 = PApplet.parseInt(random(30,  100));
        int Mock2 = PApplet.parseInt(random(200, 255));
        int Mock3 = PApplet.parseInt(random(30, 90));
        
        // Grab a random result to mock various data from the serial
        lastSerialData = Mock0 + "," + Mock1 + "," + Mock2 + "," + Mock3;
      }
      
      return lastSerialData;
    }
    
    /**
      Returns the integer of the beam that is broken or -1 if non is broken
    **/
    private int getBrokenBeam()
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
    
    public boolean isTreasureStolen()
    {
      return treasureSwitchValue;
    }
    
    public boolean isBeamBroken()
    {
      int brokenBeam = getBrokenBeam();
      if ((brokenBeam > -1) || keyPressed)
      {
        beamsBroken++;
        return true;
      }
      
      return false;
    }
    
    public int getBeamsBroken()
    {
      return this.beamsBroken; 
    }
    
    public String toString()
    {
      return sensor0Value + "," + sensor1Value + "," + sensor2Value + "," + sensor3Value;
    }
    public String getGameStateLabel()
    {
      String stateLabel = "_bad_";
      switch(this.gameState) {
        case 0: 
          stateLabel = "STATE_RESETTING";
          break;
        case 1: 
          stateLabel = "STATE_RUNNING";
          break;
        case 2: 
          stateLabel = "STATE_BEAMBROKEN";
          break;
        case 3: 
          stateLabel = "STATE_ATTRACT";
          break;
        case 6: 
          stateLabel = "STATE_COMPLETE";
          break;
        case 7: 
          stateLabel = "STATE_LOST";
          break;
       case 9: 
          stateLabel = "STATE_PAUSED";
          break;
        default:
          stateLabel = "Unknown";
          break;
      }
      
      return stateLabel;
    }
    public int getGameState()
    {
      return this.gameState;
    }
    public void setGameState(int newState)
    {
      gameState = newState;
      
      if (newState == MazeData.STATE_RUNNING)
      {
        startTime = millis();
      }
    }
}

class Slider extends GuiComponent {
  
  public void draw() {
  
  }
}

class Button extends GuiComponent {
  private boolean isButtonPressed;
  
  private String buttonText = "_undefined_";
  private int buttonColor = 0xff749ECA;
  
  private boolean isVisible = true;
  
  Button(int x, int y, String text, int buttonColor)
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
  
  public void setVisible(boolean canYouSeeMe)
  {
    this.isVisible = canYouSeeMe;
  }
  
  public boolean isVisible()
  {
    return this.isVisible;
  }
  
  public void draw()
  {
    if (!this.isVisible())
      return;
      
    //Default 'off' colors.
    int fillColor = this.buttonColor;
    
    if (isMouseOver())
      fillColor = 0xff440000;
    
    if (isButtonPressed)
    {
      fillColor = 0xffFF0000; //Change to RED
    }

    //Draw Button
    fill(fillColor);
    
    noStroke();
    //stroke(#888888);  //Darker outline 

    rect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 2);
    fill(0xffFFFFFF);  // Text Color
    textSize(20);
    
    textAlign(CENTER, CENTER);
    text(this.buttonText, this.getX() + this.getWidth()/2, this.getY() + this.getHeight()/2 - 3);
  }
}


/*
Called when serial data is read. Then we store the results which are used in getSensorData()
*/
public void serialEvent (Serial myPort) {
  //Assume the data is in a comma string "n:12,45,76,80"

  lastSerialData = myPort.readStringUntil('\n');    // get the ASCII string
  if (lastSerialData == null) return;
  
  lastSerialData = lastSerialData.replace("\n","");
  
  //strip off state (n:)
  lastSerialData = lastSerialData.substring(2, lastSerialData.length());
  println(lastSerialData);
}

public void stop() {
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
  public void setWidth(int w)
  {
    this.w = w;
  }  
  public int getHeight()
  {
     return this.h;
  }
  public void setHeight(int h)
  {
    this.h = h;
  }
  
  public void draw()
  {
    //Override to draw your control on the screen
  }
}
  public void settings() {  size(1000, 500); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Laser_Maze_2018_Processing_FMS" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
