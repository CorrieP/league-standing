# League Standings Calculator

## Project Overview

The League Standings Calculator is a Java application that processes match results from sports leagues and generates team standings tables. It accepts input in multiple formats and calculates points based on standard league rules: 3 points for a win, 1 point for a draw, and 0 points for a loss.

## Quick Start

```bash
# Clone the repository
git clone https://github.com/yourusername/league-standings-calculator.git
cd league-standings-calculator

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

```
## Features

- Multiple input methods supported:
    - Standard console input
    - CSV file input
- Automatic calculation of league standings
- Sorting of standings by:
    - Points (descending)
    - Team name (alphabetically when points are tied)
- Robust error handling for various input formats

## Prerequisites

- Java 21 or higher
- Maven 3.8 or higher

## Usage

### Standard Input
When prompted, enter match results in the format:
TeamA 3,TeamB 1

Type `done` when finished.

### CSV File
Provide a path to a CSV file with match results in the same format:
Lions 3,Snakes 1 Tarantulas 1,FC Awesome 0 Lions 1,FC Awesome 1


### Building

```bash
# Standard build
mvn clean install

# Skip tests
mvn clean install -DskipTests

```
### Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MatchServiceImplTest

# Generate test coverage report
mvn jacoco:report

```
