#include <DigiMouse.h>

float mspeed = 1.1;

int leftClick = 0;
int scrLock = 1;

int state = HIGH;
int reading;
int previous = LOW;

unsigned long time = 0;
unsigned long debounce = 200UL;


void setup() {
  pinMode(leftClick, INPUT);
  digitalWrite(leftClick, HIGH);
  
  pinMode(scrLock, INPUT);
  digitalWrite(scrLock, HIGH);
  
  DigiMouse.begin();
}

void loop() {
  int x = analogRead(A1);
  int y = analogRead(A0);

  DigiMouse.moveX(-0.5*((y-512)^2)*0.015);//425
  DigiMouse.moveY(0.5*((x-512)^2)*0.015);//415
  
  reading = digitalRead(scrLock);
  if (reading == HIGH && previous == LOW && millis() - time > debounce)
  {
    if (state == HIGH)
      state = LOW;
    else
      state = HIGH;

    time = millis();
  }

  if (state==HIGH){
    int flag=1;
    while(flag == 1){
            x = analogRead(A1);
            if (x > 600){
              DigiMouse.scroll(-1);
              delay(30);
              }
            else if(x < 400){
              DigiMouse.scroll(1);              
              delay(30);
            }
            DigiMouse.update();
              if(digitalRead(scrLock) == HIGH){       
                flag=0;
          }
        }
    }  
  
  if(digitalRead(leftClick)==LOW){
    DigiMouse.setButtons(1<<0);
    DigiMouse.delay(10);
    DigiMouse.setButtons(0);
    }

  previous = reading;
  DigiMouse.update();
}
