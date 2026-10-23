#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "Installing Test Tools and Verification Suite"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Ensure test directory exists
mkdir -p "${REPO_ROOT}/test"

echo "Directory structure verified:"
echo " - ${REPO_ROOT}/test"

# Check Node.js and NPM installation for Playwright and test utilities
if command -v node >/dev/null 2>&1; then
    echo "Node.js installed: $(node -v)"
else
    echo "Warning: Node.js is not found in PATH."
fi

if command -v npm >/dev/null 2>&1; then
    echo "NPM installed: $(npm -v)"
else
    echo "Warning: NPM is not found in PATH."
fi

# Install npm dependencies if package.json exists in test directory
if [ -f "${REPO_ROOT}/test/package.json" ]; then
    echo "Installing test dependencies from ${REPO_ROOT}/test/package.json..."
    (cd "${REPO_ROOT}/test" && npm install)
fi

echo "=========================================="
echo "Test environment preparation completed."
echo "=========================================="
