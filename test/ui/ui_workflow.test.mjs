import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "../..");
const screenshotDir = path.join(repoRoot, "build", "ui_screenshots");
fs.mkdirSync(screenshotDir, { recursive: true });

const pageUrl = "file://" + path.join(__dirname, "index.html");

async function run() {
  console.log("==========================================");
  console.log("Running Playwright UI & Workflow Tests");
  console.log("==========================================");

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
  });
  const page = await context.newPage();

  try {
    // Step 1: Initial Scan Screen
    await page.goto(pageUrl);
    const initialStatus = await page.textContent("#topbar-subtitle");
    assert.equal(initialStatus, "BLE: DISCONNECTED");
    await page.screenshot({ path: path.join(screenshotDir, "01_scan_initial.png") });
    console.log("  PASSED: Step 1 - Initial Scan Screen captured");

    // Step 2: Enable Simulation Mode, Scan, and Connect
    await page.check("#sim-toggle");
    await page.click("#btn-scan");
    await page.waitForSelector("#btn-connect-sim");
    await page.click("#btn-connect-sim");
    const simStatus = await page.textContent("#topbar-subtitle");
    assert.match(simStatus, /^\[SIM\] Connected/);
    await page.screenshot({ path: path.join(screenshotDir, "02_scan_simulation_connected.png") });
    console.log("  PASSED: Step 2 - Simulation Mode Scan & Connect captured");

    // Step 3: Control Screen (Pattern, Swatch, Brightness, Melody, Play)
    await page.click("#nav-control");
    await page.click("#pattern-chips button:has-text('Sparkle')");
    await page.click(".swatch[data-rgb='0,200,50']");
    await page.fill("#slider-brightness", "85");
    await page.dispatchEvent("#slider-brightness", "input");
    await page.click("#melody-chips button:has-text('Jingle Bells')");
    await page.click("#btn-play");
    const wireText = await page.textContent("#wire-frame");
    assert.match(wireText, /Last Frame \(Audio\): 10 01 01 46 64 00/);
    await page.screenshot({ path: path.join(screenshotDir, "03_control_lighting_audio.png") });
    console.log("  PASSED: Step 3 - Control Dashboard & Binary Frame verification captured");

    // Step 4: Presets Screen (Save & Apply)
    await page.click("#nav-presets");
    await page.fill("#preset-name-input", "Festive Sparkle");
    await page.click("#btn-save-preset");
    const presetCount = await page.textContent("#preset-count");
    assert.equal(presetCount, "3");
    await page.screenshot({ path: path.join(screenshotDir, "04_presets_manager.png") });
    console.log("  PASSED: Step 4 - Preset Manager workflow captured");

    console.log("==========================================");
    console.log("All Playwright UI Workflow Tests Passed!");
    console.log("Screenshots saved to build/ui_screenshots/");
    console.log("==========================================");
  } finally {
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
