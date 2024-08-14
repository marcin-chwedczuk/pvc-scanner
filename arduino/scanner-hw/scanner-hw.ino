#include <Servo.h>

#define SONIC_TRIGGER_PIN 3
#define SONIC_ECHO_PIN 4

#define ANGLE_SERVO_PIN 5
#define LEFT_SERVO_PIN 6
#define RIGHT_SERVO_PIN 7

Servo angleServo;
Servo leftServo;
Servo rightServo;

// Pattern contains '$' where a number should be located e.g. 'ANGLE $'
// Number may be preceded by a singl + or - character.
// Returns true on success.
bool parseCommand(const char* text, const char* pattern, int* arg) {
  int sign = 1;
  int number = 0;

  while (*pattern) {
    if (*pattern == '$') {
      // parse number
      if (*text == '-' || *text == '+') {
        sign = (*text == '-') ? -1 : 1;
        text++;
      }
      if (!*text) {
        // ends too early
        return false;
      }

      while (*text) {
        char digit = *text;
        if (digit < '0' || digit > '9') {
          // invalid character
          return false;
        }

        number = 10*number + (digit - '0');
        text++;
      }

      pattern++;
    } else {
      if (*text != *pattern) {
        return false;
      }

      pattern++;
      text++;
    }
  }

  if (arg) {
    *arg = number * sign;
  }
  return true;
}

bool parseSimpleCommand(const char* text, const char* pattern) {
  return parseCommand(text, pattern, NULL);
}

int singlePingMM() {
  digitalWrite(SONIC_TRIGGER_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(SONIC_TRIGGER_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(SONIC_TRIGGER_PIN, LOW);

  long duration = pulseIn(SONIC_ECHO_PIN, HIGH);
  long distancemm = duration * 34 / 200;
  return (int)distancemm;
}

void insertionSort(int arr[], int n) {
    for (int i = 1; i < n; i++) {
        int key = arr[i];
        int j = i - 1;

        // Move elements of arr[0..i-1], that are greater than key,
        // to one position ahead of their current position
        while (j >= 0 && arr[j] > key) {
            arr[j + 1] = arr[j];
            j = j - 1;
        }
        arr[j + 1] = key;
    }
}

unsigned int measureDistanceMM() {
  const int MEASURMENTS_COUNT = 7;
  const int REJECTED_MEASUREMENTS_SIDE = 2;
  static int measurments[MEASURMENTS_COUNT] = { };

  for (int i = 0; i < MEASURMENTS_COUNT; i++) {
    measurments[i] = singlePingMM();
    delayMicroseconds(250);
  }

  insertionSort(measurments, MEASURMENTS_COUNT);
  // drop min & max values, avg the rest
  unsigned long sum = 0;
  for (int i = REJECTED_MEASUREMENTS_SIDE; i < MEASURMENTS_COUNT - REJECTED_MEASUREMENTS_SIDE; i++) {
    sum += measurments[i];
  }
  return (int)((sum + (MEASURMENTS_COUNT-2*REJECTED_MEASUREMENTS_SIDE - 1)) / (MEASURMENTS_COUNT - 2*REJECTED_MEASUREMENTS_SIDE));
}

void setup() {
  pinMode(SONIC_TRIGGER_PIN, OUTPUT);
  pinMode(SONIC_ECHO_PIN, INPUT);
  digitalWrite(SONIC_TRIGGER_PIN, LOW);

  angleServo.attach(ANGLE_SERVO_PIN);
  leftServo.attach(LEFT_SERVO_PIN);
  rightServo.attach(RIGHT_SERVO_PIN);

  // Not moving
  leftServo.write(92);
  rightServo.write(92);
  // Center
  angleServo.write(90);

  // Low baud rate since we have 3 servos generating electromagnetic noise...
  Serial.begin(9600);

  // Light on the LED so that we know that we are setup
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, 0);
}

#define SERIAL_BUFFER_SIZE 128
char serialBuffer[SERIAL_BUFFER_SIZE + 1] = {0};

int currentLayer = 0;
int currentAngle = 90;

void loop() {
  // Read data from Serial port
  size_t nchars = Serial.readBytesUntil('\n', serialBuffer, SERIAL_BUFFER_SIZE);
  if (nchars == 0) return;

  // Terminate with zero
  serialBuffer[nchars] = '\0';
  int arg = 0;

  if (parseSimpleCommand(serialBuffer, "SCAN")) {
    unsigned long d = measureDistanceMM();

    // convert to distance from center
    
    if (d > 120) { d = 0; }
    else if (d < 60) { d = 120 - 60; }
    else { d = 120 - d; }

    Serial.print("OK distance = ");
    Serial.print(d*4); Serial.println(".");
  } else if (parseCommand(serialBuffer, "ANGLE $", &arg)) {
    // Try to go to angle in small increments to minimalize Servo electric noise
    while (currentAngle != arg) {
      if (currentAngle < arg) currentAngle++;
      else currentAngle--;

      angleServo.write(currentAngle);
      delay(10);
    }

    Serial.print("OK angle set to ");
    Serial.print(arg);
    Serial.println("."); 
  } else if (parseCommand(serialBuffer, "LAYER $", &arg)) {
    delay(1000); // angle servo must return
    int delta = arg - currentLayer;
    if (delta > 0) {
      leftServo.write(90);
      rightServo.write(95);
      delay(delta * 1000);
    } else {
      leftServo.write(95);
      rightServo.write(89);
      delay(-delta * 1000);
    }
    leftServo.write(92);
    rightServo.write(92);
    currentLayer = arg;
    Serial.print("OK layer set to ");
    Serial.print(arg);
    Serial.println("."); 
  } else if (parseSimpleCommand(serialBuffer, "DESCRIBE")) {
    Serial.println("OK PCV Scanner 1.0");
  } else if (parseSimpleCommand(serialBuffer, "RESET")) {
    digitalWrite(LED_BUILTIN, 1);
    Serial.println("OK");
  } else {
    Serial.print("ERROR Unknown command '");
    Serial.print(serialBuffer);
    Serial.println("'");
  }

  Serial.flush();
  delay(100);
}


