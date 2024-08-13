

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

void sendOK(const char* msg) {
  Serial.print("OK ");
  Serial.println(msg);
}

void sendError(const char* msg) {
  Serial.print("ERROR ");
  Serial.print(msg);
}

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  Serial.println("Initialized");
}

#define SERIAL_BUFFER_SIZE 128
char serialBuffer[SERIAL_BUFFER_SIZE + 1] = {0};

void loop() {
  // Read data from Serial port
  size_t nchars = Serial.readBytesUntil('\n', serialBuffer, SERIAL_BUFFER_SIZE);
  if (nchars == 0) return;

  // Terminate with zero
  serialBuffer[nchars] = '\0';
  int arg = 0;

  if (parseSimpleCommand(serialBuffer, "SCAN")) {
    int fake = 300 + (int)(millis() % 100);
    Serial.print("OK distance = ");
    Serial.print(fake);
    Serial.println("."); 
  } else if (parseCommand(serialBuffer, "ANGLE $", &arg)) {
    Serial.print("OK angle set to ");
    Serial.print(arg);
    Serial.println("."); 
  } else if (parseCommand(serialBuffer, "LAYER $", &arg)) {
    Serial.print("OK layer set to ");
    Serial.print(arg);
    Serial.println("."); 
  } else if (parseSimpleCommand(serialBuffer, "DESCRIBE")) {
    sendOK("PCV Scanner 1.0");
  } else if (parseSimpleCommand(serialBuffer, "RESET")) {
    sendOK("");
  } else {
    Serial.print("ERROR Unknown command '");
    Serial.print(serialBuffer);
    Serial.println("'");
  }
}


