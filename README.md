# ATM Processor

This is a simple ATM (Automated Teller Machine) CLI application built using Kotlin and Gradle. It provides basic ATM
operations via the command line.

## Prerequisites

- **Java 21+** (Ensure `JAVA_HOME` is set correctly)
- **Gradle Wrapper** (Run `./gradlew` to avoid manual Gradle installation)
- **IDE (Optional)**: IntelliJ IDEA (Recommended)

## How to Build and Run

### 1. Using Gradle Commands

- **Build the project and create JAR file:**
  ```sh
  ./gradlew clean build
  ```

- **Run unit tests:**
  ```sh
  ./gradlew clean test
  ```

- **Run the application:**
  ```sh
  ./gradlew clean bootRun --console=plain
  ```

- **Run the JAR manually:**
  ```sh
  java -jar build/libs/atm-simulator-{version}.jar
  ```

### 2. Using `start.sh`

A shell script is provided to automate the build, test, and run process.

- **Make the script executable:**
  ```sh
  chmod +x start.sh
  ```

- **Run the script:**
  ```sh
  ./start.sh
  ```

## Git Setup

To push the project to GitHub:

```sh
git init
git add .
git commit -m "ATM simulator initial commit"
git remote add origin <your-repo-url>
git branch -M main
git push -u origin main
```

## ATM Simulator Commands

* `login [name]` - Logs in as this customer and creates the customer if not exist

* `deposit [amount]` - Deposits this amount to the logged in customer

* `withdraw [amount]` - Withdraws this amount from the logged in customer

* `transfer [target] [amount]` - Transfers this amount from the logged in customer to the target customer

* `logout` - Logs out of the current customer

## Documentation

[User Manual](UserManual.md)
