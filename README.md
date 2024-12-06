# Poker Equity Calculator

[![Build Status](https://github.com/procrastinatormuffin/poker-equity-calc/actions/workflows/build.yml/badge.svg)](https://github.com/procrastinatormuffin/poker-equity-calc/actions)
[![Coverage](https://codecov.io/gh/procrastinatormuffin/poker-equity-calc/branch/main/graph/badge.svg)](https://codecov.io/gh/procrastinatormuffin/poker-equity-calc)

Fast and accurate poker hand equity calculator for Texas Hold'em. Supports exact calculations and Monte Carlo simulations.

## Features
- Monte Carlo simulation for pre-/postflop analysis
- Support for up to 9 players
- Highly optimized hand evaluations
- Real-time equity updates

## Installation

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.6 or higher
- Git

### Clone repository

`git clone https://github.com/procrastinatormuffin/poker-equity-calc.git`

`cd poker-equity-calc`

### Build project

#### Build with Maven
mvn clean install

#### Run tests
mvn test

#### Create executable JAR
mvn package

### Verify Installation

# Run main application
java -jar target/EquityCalc-1.0-SNAPSHOT.jar

# Run with increased memory for large simulations
java -Xmx4g -jar target/EquityCalc-1.0-SNAPSHOT.jar

## Contributing

### Development Process
1. Fork the repository
2. Create a new branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`mvn test`)
5. Commit changes (`git commit -am 'Add amazing feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Submit a Pull Request

### Code Style
- Follow Java coding conventions
- Write unit tests for new features
- Keep methods small and focused
- Document public APIs using Javadoc

### Bug Reports
- Use the GitHub issue tracker
- Include steps to reproduce
- Specify your environment details
- Attach relevant logs if applicable

## License

### MIT License

Copyright (c) 2024 procrastinatormuffin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.