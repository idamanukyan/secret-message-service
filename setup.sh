#!/bin/bash

# Setup script for Secret Message Service
# Run this once after cloning/extracting the project

set -e

echo "Secret Agency Message Service - Setup"
echo "=========================================="

# Check Java version
echo "Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "Error: Java not found. Please install Java 21+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java 21+ required. Found Java $JAVA_VERSION"
    exit 1
fi
echo "OK: Java $JAVA_VERSION found"

# Check Docker
echo "Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo "Error: Docker not found. Please install Docker"
    exit 1
fi
echo "OK: Docker found"

# Check Docker Compose
echo "Checking Docker Compose..."
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "Error: Docker Compose not found. Please install Docker Compose"
    exit 1
fi
echo "OK: Docker Compose found"

# Setup Gradle wrapper
echo "Setting up Gradle wrapper..."
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version 8.5
        echo "OK: Gradle wrapper created"
    else
        echo "Warning: Gradle not found. Please install Gradle 8.5+ or download the wrapper manually:"
        echo "    1. Download from: https://services.gradle.org/distributions/gradle-8.5-bin.zip"
        echo "    2. Extract and run: gradle wrapper"
        echo ""
        echo "Or use SDKMAN:"
        echo "    sdk install gradle 8.5"
        echo "    gradle wrapper"
    fi
else
    echo "OK: Gradle wrapper already exists"
fi

# Make gradlew executable
chmod +x gradlew 2>/dev/null || true

echo ""
echo "=========================================="
echo "Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Start services:     docker-compose up -d"
echo "  2. Run tests:          ./gradlew test"
echo "  3. View logs:          docker-compose logs -f"
