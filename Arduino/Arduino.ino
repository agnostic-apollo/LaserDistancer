#include <Servo.h>

//orange signal 9
//red power
//brown ground

int servoAngle = 0;
int servoPin = 9; 
int leftLaser = 10; 
int rightLaser = 11; 
bool laserTurret = false;
int index;
 //unsigned byte servoAngleByte;
Servo servo;

void setup()  
 {  
  Serial.begin(19200);  
  servo.attach(servoPin);
  
  pinMode(leftLaser,OUTPUT);
  digitalWrite(leftLaser,LOW);
  pinMode(rightLaser,OUTPUT);
  digitalWrite(rightLaser,LOW);
  servo.write(servoAngle);
  turnOnBothLasers();
  /*
  int pos = 0;
  for(pos = 0; pos < 180; pos += 1) // goes from 0 degrees to 180 degrees 
  { // in steps of 1 degree 
    servo.write(pos); // tell servo to go to position in variable 'pos' 
    delay(15); // waits 15ms for the servo to reach the position 
  } 
  for(pos = 180; pos>=1; pos-=1) // goes from 180 degrees to 0 degrees 
  { 
    servo.write(pos); // tell servo to go to position in variable 'pos' 
    delay(15); // waits 15ms for the servo to reach the position 
  } 
   

 */
     
 }  
 void loop()  
 {  
    
     if(laserTurret)
       laserTurretStart();
    
    String command;
    //char c;
    if(Serial.available() > 0)
    {
      command = Serial.readStringUntil('\n');
      //Serial.println(command);
    
     
      if(command.equals("getAngle"))
      {
         Serial.println("angle:" +  String(servoAngle));
      }
      else if(command.equals("turnOnBothLasers"))
      {
          turnOnBothLasers();
          Serial.println("turning on lasers");
      }
      else if(command.equals("turnOffBothLasers"))
      {
          turnOffBothLasers();
      }
      else if(command.equals("laserTurretStart"))
      {
         laserTurret=true;
      }
      else if(command.equals("laserTurretStop"))
      {
         laserTurret=false;
         servo.write(0);
      }
      else if(command.equals("reset"))
      {
         laserTurret=false;
         servo.write(0);
         turnOffBothLasers();
      }
      
      else if(command.indexOf("setAngle:")>=0)
      {
          index = String("setAngle:").length();
          if(index<command.length())
          {
            int angle = command.substring(index).toInt();
            if(angle>=0 && angle<=180)
            {
              servoAngle=angle;
              servo.write(servoAngle);
            }
          } 
        
      }
      
   }

 }  

 void turnOnBothLasers()
{
 digitalWrite(leftLaser,HIGH);
 digitalWrite(rightLaser,HIGH);
}
 
void turnOnLeftLaser()
{
 digitalWrite(leftLaser,HIGH);
}

void turnOnRightLaser()
{

 digitalWrite(rightLaser,HIGH);
}

 void turnOffBothLasers()
{
 digitalWrite(leftLaser,LOW);
 digitalWrite(rightLaser,LOW);
}

void turnOffLeftLaser()
{
 digitalWrite(leftLaser,LOW);
}

void turnOffRightLaser()
{

 digitalWrite(rightLaser,LOW);
}

void laserTurretStart()
{
   int timeBetweenBursts = random(200,300);
   int timeBetweenShots = random(50,200);
   int vertStart = random(1,180);
   int vertEnd = random(1,180);
   int horiStart = random(1,180);
   int horiEnd = random(1,180);
   int numShots = random(5,10);

   int vertChange = (vertEnd - vertStart) / numShots; //how much to move vertical axis by each shot
   int horiChange = (horiEnd - horiStart) / numShots;

   //vert.write(vertStart);//let it get to start position first, wait a little
   //hori.write(horiStart);
   
   servo.write(horiStart);
   delay(100);

   for(int shot = 0; shot<numShots; shot++){
        //vert.write(vertStart);
        //hori.write(horiStart);
        servo.write(horiStart);

        vertStart += vertChange;//increment the vert value for next time
        horiStart += horiChange;

        fire();
        delay(timeBetweenShots); //add a bit of variety to the speed of shots
   }
   delay(timeBetweenBursts);
}

void fire(){
     //digitalWrite(laser,HIGH);
     turnOnRightLaser();
     //analogWrite(buzzer,100);
     delay(random(20,40));//adjust this to change length of turret shot
     //digitalWrite(laser,LOW);
     turnOffRightLaser();
     //analogWrite(buzzer, 0); 
} 


