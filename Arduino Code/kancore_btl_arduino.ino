#include <SoftwareSerial.h>
SoftwareSerial BTH05(11,12);
int clutch_m1 = 3;  //ENA1 from L298N
int clutch_m2 = 8; //ENA2 from L298N
int m1_1 = 5; //IN1 from L298N
int m1_2 = 4; //IN2 from L298N
int m2_1 = 7; //IN3 from L298N
int m2_2 = 6; //IN4 from L298N
/*motor_control: array used for handling the activation of the L298N:
 First and second positions [0, 1] correspond to the motor 1, and third and four [2, 3] for the motor 2
 first and third position [0, 2] will handle the direction, being value 0 for backward and value 1 for forward
 second and four position will handle to speed, being a float value between 0 and 1(Max speed)*/
 float motor_control[4] = {0, 0, 0, 0}; 
const int max_len = 4; //internal use for define the size of some arrays for store temporal values from the Serial read buffer
const int m_speed = 200; //The max PWM value that will be use for adjust the power of the motor

void setup(){
  Serial.begin(38400);
  BTH05.begin(38400);
  pinMode(clutch_m1, OUTPUT);
  pinMode(m1_1, OUTPUT);
  pinMode(m1_2, OUTPUT);
  pinMode(clutch_m2, OUTPUT);
  pinMode(m2_1, OUTPUT);
  pinMode(m2_2, OUTPUT);
  }

void loop(){
  getArrayFromSerial();
  leftWheel(motor_control[0], motor_control[1]);
  rightWheel(motor_control[2], motor_control[3]);
  Serial.print(motor_control[0]);
  Serial.print(", ");
  Serial.print(motor_control[1]);
  Serial.print(", ");
  Serial.print(motor_control[2]);
  Serial.print(", ");
  Serial.println(motor_control[3]);
}

void leftWheel(float motordirection, float power){
  if (power !=0){
    if (motordirection >0){
      go_go_go(clutch_m1, m1_1, m1_2, int (power*m_speed));
    } else {
      go_back(clutch_m1, m1_1, m1_2, int (power*m_speed));
    }
  } else {
    //power is 0, stop the motor
    chillout(clutch_m1, m1_1, m2_2);
    }
}

void rightWheel(float motordirection, float power){
  if (power !=0){
    if (motordirection >0){
      go_go_go(clutch_m2, m2_1, m2_2, int (power*m_speed));
    } else {
      go_back(clutch_m2, m2_1, m2_2, int (power*m_speed));
    }
  } else {
    //power is 0, stop the motor
    chillout(clutch_m2, m2_1, m2_2);
  }
}

void go_go_go(int clutch, int in1, int in2, int power){
        analogWrite(clutch, power);  //Pin de activacion del motor. eng_speed varía de 1 a 255, siendo 255 el voltaje total hacia el motor
        digitalWrite(in1, LOW);
        digitalWrite(in2, HIGH);
}

void go_back(int clutch, int in1, int in2, int power){
        analogWrite(clutch, power);  //Pin de activacion del motor. eng_speed varía de 1 a 255, siendo 255 el voltaje total hacia el motor
        digitalWrite(in1, HIGH);
        digitalWrite(in2, LOW);
}

void chillout(int clutch, int in1, int in2){
        analogWrite(clutch, 0);  //Pin de activacion del motor. eng_speed varía de 1 a 255, siendo 255 el voltaje total hacia el motor
        digitalWrite(in1, LOW);
        digitalWrite(in2, LOW);
}
/*
 * For reading the values for the motor_control array we will be using Serial.read()
 * the input must be in format "x.xx,x.xx,x.xx,x.xx;" where 'x' represents a number
 * '.' separates integers from decimals
 * ',' separates one entire float value from another, meaning that it is ready for be written in motor_array
 * ';' is the character that says the message is over and it should break the while cycle for let the motors functions run before read from Serial again
 */
void getArrayFromSerial(){
  boolean recvInProgress = false;
  char startMarker = '<';
  char endMarker = '>';
  char on_reading[max_len+1]; //temporal array for store the data from the Serial buffer, its 1 byte larger for store a null character for making the atof method work
  int reading_pos = 0; //counter for how much data is currently stored in on_reading
  int motor_control_pos = 0; //counter for how much times was written in the motor_control array
  while(BTH05.available()>0){
    char current_byte = BTH05.read(); //current byte being read from the buffer
    float current_value; //the final float that needs to be written on the motor_control array
    if (recvInProgress == true) {
      if (current_byte != endMarker && current_byte != startMarker){
        if (current_byte != ',' || reading_pos<max_len){
        //here, current_byte its a number or a '.', then add it to on_reading
        on_reading[reading_pos] = current_byte;
        reading_pos++;
      } else {
        //here, current_byte its a ',', then convert on_reading to float and add it to motor_control
        on_reading[max_len] = '\0'; //adds 1 null character to secure the correct working of atof
        current_value = atof(on_reading);
        motor_control[motor_control_pos] = current_value;
        reading_pos = 0;
        motor_control_pos++;
      }
    } else {
      if (current_byte == startMarker){ //this means some kind of data was lost while reading
        break; //skip the rest of the current bucle for preserving the integrity of the array
      }
      //Here it reach the last byte of the input, so add it to the last position of motor_control and breaks the cycle
      on_reading[max_len] = '\0';
      current_value = atof(on_reading);
      motor_control[motor_control_pos] = current_value;
      break;
    }
    } else {
      if (current_byte == '<');
        recvInProgress = true;
    }
    }
  
}
