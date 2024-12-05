# EquityCalc

## Description
EquityCalc is a Poker Real-Time Equity Calculator that helps you determine the equity of your hand in real-time during a poker game.

## Installation
1. Clone the repository:
   ```
   git clone https://github.com/ProcrastinatorMuffin/EquityCalc.git
   ```
2. Navigate to the project directory:
   ```
   cd EquityCalc
   ```
3. Install the dependencies:
   ```
   npm install
   ```
4. Set up Java:
   - Ensure you have Java Development Kit (JDK) installed. You can download it from [Oracle's website](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html).
   - Set the `JAVA_HOME` environment variable to point to your JDK installation directory.
   - Add the `bin` directory of your JDK installation to the `PATH` environment variable.
5. Set up Maven:
   - Ensure you have Apache Maven installed. You can download it from [Maven's website](https://maven.apache.org/download.cgi).
   - Set the `MAVEN_HOME` environment variable to point to your Maven installation directory.
   - Add the `bin` directory of your Maven installation to the `PATH` environment variable.

## Usage
1. Run the application:
   ```
   npm start
   ```
2. Follow the on-screen instructions to input your hand and calculate the equity.
3. To run the Java application:
   - Compile the Java code:
     ```
     javac -d bin src/main/java/com/equitycalc/*.java
     ```
   - Run the Java application:
     ```
     java -cp bin com.equitycalc.EquityCalc
     ```
4. Follow the on-screen instructions to input player hole cards and community cards, and to calculate and display win, loss, and split probabilities.
5. To run the Java application using Maven:
   - Compile the Java code:
     ```
     mvn compile
     ```
   - Run the Java application:
     ```
     mvn exec:java -Dexec.mainClass="com.equitycalc.EquityCalc"
     ```
6. Follow the on-screen instructions to input player hole cards and community cards, and to calculate and display win, loss, and split probabilities.
