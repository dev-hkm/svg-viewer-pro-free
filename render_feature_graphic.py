import subprocess
from pathlib import Path

html = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @import url('https://fonts.googleapis.com/css2?family=Urbanist:wght@400;600;700;800;900&display=swap');
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    width: 1024px;
    height: 500px;
    overflow: hidden;
    background: radial-gradient(circle at 50% 40%, #151821 0%, #08090C 100%);
    font-family: 'Urbanist', -apple-system, sans-serif;
    color: #FFFFFF;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;
  }
  .glow {
    position: absolute;
    width: 380px;
    height: 380px;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0) 70%);
    border-radius: 50%;
    top: 50px;
    pointer-events: none;
  }
  .icon-wrap {
    width: 130px;
    height: 130px;
    margin-bottom: 24px;
    filter: drop-shadow(0 12px 30px rgba(0,0,0,0.6));
    z-index: 1;
  }
  .title {
    font-size: 38px;
    font-weight: 800;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #FFFFFF;
    margin-bottom: 10px;
    z-index: 1;
  }
  .subtitle {
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 0.22em;
    text-transform: uppercase;
    color: #9E9E9E;
    margin-bottom: 24px;
    z-index: 1;
  }
  .tags {
    display: flex;
    gap: 12px;
    z-index: 1;
  }
  .tag {
    padding: 6px 14px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.14);
    color: #E0E0E0;
  }
  .tag.hi {
    background: #FFFFFF;
    color: #08090C;
    border-color: #FFFFFF;
  }
</style>
</head>
<body>
  <div class="glow"></div>
  <div class="icon-wrap">
    <svg viewBox="0 0 108 108" width="130" height="130">
      <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
      <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      <g transform="rotate(45 54 54)">
        <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
        <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
        <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      </g>
      <g transform="rotate(90 54 54)">
        <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
        <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
        <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      </g>
      <g transform="rotate(135 54 54)">
        <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
        <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
        <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      </g>
      <path d="M 41.146 54 A 12.854 12.854 0 1 0 66.854 54 A 12.854 12.854 0 1 0 41.146 54 Z M 46.789 54 A 7.211 7.211 0 1 0 61.211 54 A 7.211 7.211 0 1 0 46.789 54 Z" fill="#FFFFFF" fill-rule="evenodd" />
      <circle cx="54" cy="54" r="2.822" fill="#FFFFFF" />
    </svg>
  </div>
  <div class="title">SVG Viewer Pro</div>
  <div class="subtitle">Vector Inspector • Global Recolor • Motion Player • 8K Export</div>
  <div class="tags">
    <div class="tag hi">100% Free</div>
    <div class="tag">Zero Ads</div>
    <div class="tag">Offline Privacy</div>
    <div class="tag">Jetpack Compose</div>
  </div>
</body>
</html>"""

html_path = Path(r"C:\Users\Admin\AppData\Local\Temp\render_svg_banner.html")
html_path.write_text(html, encoding="utf-8")

png_path = Path(r"C:\Users\Admin\AppData\Local\Temp\svg_feature_graphic_1024x500.png")
CHROME = Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe")

cmd = [
    str(CHROME),
    "--headless=new",
    "--disable-gpu",
    "--force-device-scale-factor=1",
    "--window-size=1024,500",
    f"--screenshot={png_path.resolve()}",
    html_path.resolve().as_uri()
]
subprocess.run(cmd, check=True)
print("Rendered 1024x500 Feature Graphic:", png_path.exists(), png_path.stat().st_size)
