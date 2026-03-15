#!/bin/bash

# AnisugKMP Build Script
# This script handles building Android APKs and Linux distributions (DEB, RPM).

set -e

# Set Java Home to the specific JDK required for the project
export JAVA_HOME=/opt/jdk-17.0.13+11
export PATH=$JAVA_HOME/bin:$PATH

# Default values from libs.versions.toml or overrides
VERSION_NAME="${1:-0.9.9}"
BUILD_NUMBER="${2:-2}"

echo "----------------------------------------------------"
echo "🚀 Starting AnisugKMP Build Process"
echo "📦 Version: $VERSION_NAME"
echo "🔢 Build Number: $BUILD_NUMBER"
echo "----------------------------------------------------"

# Navigate to project root (in case script is run from elsewhere)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

# Ensure gradlew is executable
chmod +x gradlew

# Run the build
echo "🔨 Compiling Android and Linux packages..."
./gradlew :composeApp:assembleRelease packageDeb packageRpm packageAppImage --no-daemon \
    -PappVersion="$VERSION_NAME" \
    -PappBuildNumber="$BUILD_NUMBER"

echo "----------------------------------------------------"
echo "✅ Build Successful!"
echo "----------------------------------------------------"
echo "📂 Android APKs:"
echo "   - $SCRIPT_DIR/composeApp/build/outputs/apk/release/"
echo ""
echo "📂 Linux Packages (DEB/RPM):"
echo "   - $SCRIPT_DIR/composeApp/build/compose/binaries/main/deb/"
echo "   - $SCRIPT_DIR/composeApp/build/compose/binaries/main/rpm/"
echo ""
echo "📂 AppImage (AppDir):"
echo "   - $SCRIPT_DIR/composeApp/build/compose/binaries/main/app/AnisugKMP/"
echo ""
echo "📂 Arch Linux Build:"
echo "   - $SCRIPT_DIR/arch_build/"
echo "----------------------------------------------------"
