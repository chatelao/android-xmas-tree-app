#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "Installing Application Development Tools"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Ensure source directories exist
mkdir -p "${REPO_ROOT}/src/android"
mkdir -p "${REPO_ROOT}/src/firmware"

echo "Directory structure verified:"
echo " - ${REPO_ROOT}/src/android"
echo " - ${REPO_ROOT}/src/firmware"

# Check JDK / Java installation
if command -v java >/dev/null 2>&1; then
    echo "Java installed: $(java -version 2>&1 | head -n 1)"
else
    echo "Warning: Java JDK is not found in PATH."
fi

# Check Python3 installation (required for ESP-IDF and build tools)
if command -v python3 >/dev/null 2>&1; then
    echo "Python installed: $(python3 --version)"
else
    echo "Warning: Python3 is not found in PATH."
fi

# Check CMake installation
if command -v cmake >/dev/null 2>&1; then
    echo "CMake installed: $(cmake --version | head -n 1)"
else
    echo "Warning: CMake is not found in PATH."
fi

echo "=========================================="
echo "Application build environment prepared."
echo "=========================================="
