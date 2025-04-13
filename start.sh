#!/bin/bash

set -e

echo "Building the application..."
./gradlew clean build

echo "Running unit tests..."
./gradlew test

echo "Running the application..."
./gradlew bootRun --console=plain