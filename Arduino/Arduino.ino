// IMPORTANT
// Comment "#define HID_ENABLED" at USBDesc.h not to enrich arduino as HID device
//


#define D1 2 // Left motor
#define M1 3
#define D2 4 // Right motor
#define M2 5

// Ultrasonic
#define UltraTrig 9
#define UltraEcho 8

// Status led
#define StatusLED 13

String currentCommand;

void setup()
{
  currentCommand = "";
  
  pinMode(D1, OUTPUT);
  pinMode(D2, OUTPUT);
  
  pinMode(UltraTrig, OUTPUT);
  pinMode(UltraEcho, INPUT);
  
  pinMode(StatusLED, OUTPUT);
  
  Serial.begin(9600, SERIAL_8N1);
  digitalWrite(StatusLED, HIGH);
  while (!Serial) { ; // wait for serial port to connect. Needed for Leonardo only
  }
  delay(1000);
  digitalWrite(StatusLED, LOW);
}

void loop()
{
  checkCommand();
  
  executeCommand();
  
  delay(100);
}

void checkCommand()
{
  if (Serial.available()==0)
   return;
  
  currentCommand = "";
  while(Serial.available()>0)
  {
    char c = Serial.read();
    if (c == '\n' || c == '\r' || c == 0)
    {
//      Serial.flush();
      break;
    }
    currentCommand += c;
  }
}

void executeCommand()
{
  if (currentCommand == "")
   return;
   
  if (currentCommand[0] == 'M') // Move
  {
    executeMove();
    SendDone();
  }
  else if (currentCommand[0] == 'S') // Sensor data request
  {
    String r = currentCommand.substring(1);
    if (r == "DIST")
    {
      sendDistance();
      SendDone();
    }
    else
      SendUnknown();
  }
  else
    SendUnknown();
}

void executeMove()
{
  String c = currentCommand;
  int m1 = 0, m2 = 0;  // =1 - forward; =-1 - backward; =0 - nothing
  if (c[1] == 'F')
  {
    m1=1; m2=1;    
  }
  else if (c[1] == 'B')
  {
    m1=-1; m2=-1;
  }
  else if (c[1] == 'R')
  {
    m1=1; m2=-1;
  }
  else if (c[1] == 'L')
  {
    m1=-1; m2=1;
  }
  else
  {
    SendUnknown();
    return;
  }
  
  c=c.substring(2);
  int duration=c.toInt();  
  
  if (m1==1)
  {
    digitalWrite(D1, LOW);
    analogWrite(M1, 255);
  }
  else if (m1==-1)
  {
    digitalWrite(D1, HIGH);
    analogWrite(M1, 0);
  }
  
  if (m2==1)
  {
    digitalWrite(D2, LOW);
    analogWrite(M2, 255);
  }
  else if (m2==-1)
  {
    digitalWrite(D2, HIGH);
    analogWrite(M2, 0);
  }
  
  delay(duration);
  analogWrite(M1, 0);
  analogWrite(M2, 0);
  digitalWrite(D1, LOW);
  digitalWrite(D2, LOW);
}

void sendDistance()
{
  long duration, distance;
  digitalWrite(UltraTrig, LOW);
  delayMicroseconds(2);
  digitalWrite(UltraTrig, HIGH);
  delayMicroseconds(10);
  digitalWrite(UltraTrig, LOW);
  duration = pulseIn(UltraEcho, HIGH);
  distance = (duration/2) / 29.1;
  
  Serial.println("D" + String(distance));
}

void SendDone()
{
  Serial.println(currentCommand + " DONE");
  currentCommand = "";  
}

void SendUnknown()
{
  Serial.println("!" + currentCommand + " Unknown command");
  currentCommand = "";
}
