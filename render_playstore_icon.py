import subprocess
from pathlib import Path

# SVG equivalent of ic_launcher_background + ic_launcher_foreground
# Foreground is 108x108 with 8 nodes rotating 45 deg, center circle target
svg_content = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512">
  <defs>
    <radialGradient id="bg-glow" cx="54" cy="54" r="54" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#1c1c22" />
      <stop offset="100%" stop-color="#08090c" />
    </radialGradient>
  </defs>

  <!-- Background -->
  <rect width="108" height="108" fill="url(#bg-glow)" />

  <!-- 8-Axis Star Hub Pattern -->
  <g id="hub-pattern">
    <!-- Axis 1 (0 deg) -->
    <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
    <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
    <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />

    <!-- Axis 2 (45 deg) -->
    <g transform="rotate(45 54 54)">
      <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
      <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
    </g>

    <!-- Axis 3 (90 deg) -->
    <g transform="rotate(90 54 54)">
      <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
      <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
    </g>

    <!-- Axis 4 (135 deg) -->
    <g transform="rotate(135 54 54)">
      <line x1="54" y1="30.8" x2="54" y2="77.2" stroke="#FFFFFF" stroke-width="3.135" stroke-linecap="round" />
      <circle cx="54" cy="30.8" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
      <circle cx="54" cy="77.2" r="4.389" fill="#0A0A0A" stroke="#FFFFFF" stroke-width="2.822" />
    </g>

    <!-- Concentric Center Target -->
    <path d="M 41.146 54 A 12.854 12.854 0 1 0 66.854 54 A 12.854 12.854 0 1 0 41.146 54 Z M 46.789 54 A 7.211 7.211 0 1 0 61.211 54 A 7.211 7.211 0 1 0 46.789 54 Z" fill="#FFFFFF" fill-rule="evenodd" />
    <circle cx="54" cy="54" r="2.822" fill="#FFFFFF" />
  </g>
</svg>"""

out_svg = Path(r"C:\Users\Admin\AppData\Local\Temp\svg_icon_512x512.svg")
out_svg.write_text(svg_content, encoding="utf-8")

html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  html, body {{ margin:0; padding:0; width:512px; height:512px; overflow:hidden; background:#08090C; }}
  svg {{ display:block; width:512px; height:512px; }}
</style>
</head>
<body>{svg_content}</body>
</html>"""

html_path = Path(r"C:\Users\Admin\AppData\Local\Temp\render_svg_icon.html")
html_path.write_text(html, encoding="utf-8")

png_path = Path(r"C:\Users\Admin\AppData\Local\Temp\svg_icon_512x512.png")
CHROME = Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe")

cmd = [
    str(CHROME),
    "--headless=new",
    "--disable-gpu",
    "--force-device-scale-factor=1",
    "--window-size=512,512",
    f"--screenshot={png_path.resolve()}",
    html_path.resolve().as_uri()
]
subprocess.run(cmd, check=True)
print("Rendered 512x512 App Icon:", png_path.exists(), png_path.stat().st_size)
