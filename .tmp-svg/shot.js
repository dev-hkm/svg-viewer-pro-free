const { chromium } = require("C:/Users/Admin/.workbuddy-ai/binaries/node/workspace/node_modules/playwright-core");
(async () => {
  const b = await chromium.launch({ executablePath: "C:/Users/Admin/AppData/Local/ms-playwright/chromium-1248/chrome-win64/chrome.exe" });
  const p = await b.newPage({ viewport: { width: 760, height: 1200 }, deviceScaleFactor: 1.3 });
  await p.goto("file:///C:/Users/Admin/WorkBuddy%20AI/svg%20app/changelog-1.4.1.html");
  await p.waitForTimeout(350);
  console.log("overflow:", await p.evaluate(() => document.documentElement.scrollWidth > window.innerWidth));
  await p.screenshot({ path: "cl141.png", fullPage: true });
  await b.close();
})();
