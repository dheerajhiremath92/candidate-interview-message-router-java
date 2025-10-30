#!/bin/bash

# Verification script for MessageRouter interview candidate
# This script helps verify the basic project structure and compilation

echo "🔍 Verifying MessageRouter project setup..."
echo

# Check if required files exist
echo "📁 Checking project structure..."
if [ ! -f "src/main/java/MessageRouter.java" ]; then
    echo "❌ MessageRouter.java not found"
    exit 1
fi

if [ ! -f "src/test/java/MessageRouterTest.java" ]; then
    echo "❌ MessageRouterTest.java not found"
    exit 1
fi

if [ ! -f "pom.xml" ]; then
    echo "❌ pom.xml not found"
    exit 1
fi

echo "✅ All required files present"
echo

# Check for basic interface structure
echo "🔍 Checking interface definitions..."
if ! grep -q "interface MessageRouter" src/main/java/MessageRouter.java; then
    echo "❌ MessageRouter interface not found"
    exit 1
fi

if ! grep -q "interface Subscription" src/main/java/MessageRouter.java; then
    echo "❌ Subscription interface not found"
    exit 1
fi

echo "✅ Interface definitions look good"
echo

# Try to compile if Maven is available
if command -v mvn >/dev/null 2>&1; then
    echo "📦 Maven found, checking compilation..."

    if mvn compile -q; then
        echo "✅ Project compiles successfully"
    else
        echo "❌ Compilation failed - check your implementation"
        exit 1
    fi
else
    echo "⚠️  Maven not found - install Maven to run tests"
fi

echo
echo "🧪 To run tests:"
echo "   mvn test                    # Run all tests"
echo "   mvn test -q                 # Run tests quietly"
echo "   mvn clean test              # Clean and run tests"
echo

echo "💡 Getting started:"
echo "   1. Examine the interfaces in MessageRouter.java"
echo "   2. Look at the test cases to understand expected behavior"
echo "   3. Implement your concrete classes"
echo "   4. Run 'mvn test' to check your progress"
echo

echo "🚀 Setup verification complete!"
