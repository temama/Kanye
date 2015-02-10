#define D1 2 // Left motor
#define M1 3
#define D2 4 // Right motor
#define M2 5

String currentCommand;

void setup()
{
  pinMode(D1, OUTPUT);
  pinMode(D2, OUTPUT);
  
  Serial.begin(9600);
  while (!Serial) { ; // wait for serial port to connect. Needed for Leonardo only
  }
}

void loop()
{
  checkCommand();
  
  executeCommand();
  
  sendSensorsData();
  
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
    if (c == '\n')
    {
      Serial.flush();
      break;
    }
    currentCommand += c;
  }
}

void executeCommand()
{
  if (currentCommand == "")
   return;
   
  String c = currentCommand;
  int m1 = 0, m2 = 0;  // =1 - forward; =-1 - backward; =0 - nothing
  if (c[0] == 'L')
  {
    m1=1; m2=1;    
  }
  else if (c[0] == 'R')
  {
    m1=-1; m2=-1;
  }
  else if (c[0] == 'B')
  {
    m1=1; m2=-1;
  }
  else if (c[0] == 'F')
  {
    m1=-1; m2=1;
  }
  else
  {
    Serial.println(currentCommand + " Unknown command");
    currentCommand = "";
    return;
  }
  
  c=c.substring(1);
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
  
  Serial.println(currentCommand + " DONE");
  currentCommand = "";  
}

void sendSensorsData()
{

}
