import subprocess
from pathlib import Path

CHROME = Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe")

screens = [
    {
        "id": "1",
        "title": "INFINITE VECTOR ZOOM",
        "sub": "Sub-pixel crisp rendering with zero pixelation",
        "mockup": """
          <div style="background:#141419;border-radius:24px;border:1px solid #26262B;height:100%;display:flex;flex-direction:column;overflow:hidden;position:relative;">
            <!-- Top App Bar -->
            <div style="height:64px;display:flex;align-items:center;justify-content:space-between;padding:0 20px;border-bottom:1px solid #222228;background:#0D0D12;">
              <span style="font-size:18px;font-weight:700;letter-spacing:-0.01em;">aurora_vector.svg</span>
              <div style="display:flex;gap:12px;">
                <span style="font-size:13px;background:#262630;padding:4px 10px;border-radius:12px;color:#A0A0B0;">34.2x Zoom</span>
                <span style="font-size:13px;background:#262630;padding:4px 10px;border-radius:12px;color:#A0A0B0;">48 KB</span>
              </div>
            </div>
            <!-- Canvas -->
            <div style="flex:1;display:flex;align-items:center;justify-content:center;background:radial-gradient(circle, #1A1A24 0%, #0A0A0E 100%);">
              <svg viewBox="0 0 100 100" width="360" height="360">
                <defs>
                  <linearGradient id="g1" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#4F46E5"/><stop offset="100%" stop-color="#06B6D4"/></linearGradient>
                  <linearGradient id="g2" x1="0" y1="1" x2="1" y2="0"><stop offset="0%" stop-color="#EC4899"/><stop offset="100%" stop-color="#8B5CF6"/></linearGradient>
                </defs>
                <circle cx="50" cy="50" r="42" fill="url(#g1)" opacity="0.85"/>
                <polygon points="50,15 85,80 15,80" fill="url(#g2)" opacity="0.75"/>
                <circle cx="50" cy="50" r="18" fill="#FFFFFF"/>
              </svg>
            </div>
            <!-- Bottom Tool Strip -->
            <div style="height:76px;background:#0D0D12;border-top:1px solid #222228;display:flex;align-items:center;justify-content:space-around;padding:0 20px;">
              <span style="color:#FFF;font-size:14px;font-weight:600;">🔍 Zoom Fit</span>
              <span style="color:#A0A0B0;font-size:14px;">🎯 Inspect</span>
              <span style="color:#A0A0B0;font-size:14px;">🎨 Recolor</span>
              <span style="color:#A0A0B0;font-size:14px;">📝 Code</span>
              <span style="color:#4F46E5;font-size:14px;font-weight:700;">💾 Export 8K</span>
            </div>
          </div>
        """
    },
    {
        "id": "2",
        "title": "TAP-TO-INSPECT ELEMENT",
        "sub": "Inspect DOM tags, paths, stroke and fill colors",
        "mockup": """
          <div style="background:#141419;border-radius:24px;border:1px solid #26262B;height:100%;display:flex;flex-direction:column;overflow:hidden;position:relative;">
            <div style="height:64px;display:flex;align-items:center;padding:0 20px;border-bottom:1px solid #222228;background:#0D0D12;">
              <span style="font-size:18px;font-weight:700;">Element Inspector</span>
            </div>
            <div style="flex:1;padding:24px;display:flex;flex-direction:column;gap:18px;">
              <div style="background:#1A1A22;padding:18px;border-radius:16px;border:1px solid #2D2D38;">
                <div style="font-size:12px;color:#8B8B9E;text-transform:uppercase;letter-spacing:0.1em;margin-bottom:6px;">Selected Node</div>
                <div style="font-size:20px;font-weight:800;color:#60A5FA;font-family:monospace;">&lt;circle id="core-pin" /&gt;</div>
              </div>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;">
                <div style="background:#1A1A22;padding:16px;border-radius:14px;border:1px solid #282834;">
                  <div style="font-size:11px;color:#8B8B9E;">FILL COLOR</div>
                  <div style="display:flex;align-items:center;gap:8px;margin-top:6px;">
                    <div style="width:18px;height:18px;border-radius:4px;background:#EC4899;"></div>
                    <span style="font-family:monospace;font-size:15px;font-weight:700;">#EC4899</span>
                  </div>
                </div>
                <div style="background:#1A1A22;padding:16px;border-radius:14px;border:1px solid #282834;">
                  <div style="font-size:11px;color:#8B8B9E;">STROKE WIDTH</div>
                  <div style="font-family:monospace;font-size:15px;font-weight:700;margin-top:6px;">2.82 px</div>
                </div>
              </div>
              <div style="background:#1A1A22;padding:16px;border-radius:14px;border:1px solid #282834;">
                <div style="font-size:11px;color:#8B8B9E;margin-bottom:8px;">BOUNDING BOX</div>
                <div style="font-family:monospace;font-size:13px;color:#E0E0EC;">x: 41.14 | y: 54.00 | w: 25.71 | h: 25.71</div>
              </div>
              <div style="display:flex;gap:12px;margin-top:auto;">
                <button style="flex:1;height:52px;background:#2563EB;color:#FFF;border:none;border-radius:14px;font-weight:700;font-size:15px;">Copy Element XML</button>
                <button style="width:52px;height:52px;background:#242430;color:#FFF;border:none;border-radius:14px;font-size:18px;">👁️</button>
              </div>
            </div>
          </div>
        """
    },
    {
        "id": "3",
        "title": "GLOBAL PALETTE RECOLOR",
        "sub": "1-tap instant color replacement across whole artwork",
        "mockup": """
          <div style="background:#141419;border-radius:24px;border:1px solid #26262B;height:100%;display:flex;flex-direction:column;overflow:hidden;position:relative;">
            <div style="height:64px;display:flex;align-items:center;padding:0 20px;border-bottom:1px solid #222228;background:#0D0D12;">
              <span style="font-size:18px;font-weight:700;">Palette Recolor (6 detected)</span>
            </div>
            <div style="flex:1;padding:24px;display:flex;flex-direction:column;gap:14px;">
              <div style="display:flex;align-items:center;justify-content:space-between;background:#1A1A22;padding:16px;border-radius:14px;border:1px solid #2A2A38;">
                <div style="display:flex;align-items:center;gap:12px;">
                  <div style="width:32px;height:32px;border-radius:8px;background:#4F46E5;"></div>
                  <div>
                    <div style="font-weight:700;font-size:15px;">Indigo Accent</div>
                    <div style="font-size:12px;color:#8B8B9E;">4 paths &bull; #4F46E5</div>
                  </div>
                </div>
                <span style="color:#60A5FA;font-weight:600;font-size:14px;">Swap &rarr;</span>
              </div>
              <div style="display:flex;align-items:center;justify-content:space-between;background:#1A1A22;padding:16px;border-radius:14px;border:1px solid #2A2A38;">
                <div style="display:flex;align-items:center;gap:12px;">
                  <div style="width:32px;height:32px;border-radius:8px;background:#06B6D4;"></div>
                  <div>
                    <div style="font-weight:700;font-size:15px;">Cyan Highlight</div>
                    <div style="font-size:12px;color:#8B8B9E;">2 gradients &bull; #06B6D4</div>
                  </div>
                </div>
                <span style="color:#60A5FA;font-weight:600;font-size:14px;">Swap &rarr;</span>
              </div>
              <div style="display:flex;align-items:center;justify-content:space-between;background:#1A1A22;padding:16px;border-radius:14px;border:1px solid #2A2A38;">
                <div style="display:flex;align-items:center;gap:12px;">
                  <div style="width:32px;height:32px;border-radius:8px;background:#EC4899;"></div>
                  <div>
                    <div style="font-weight:700;font-size:15px;">Hot Pink Fill</div>
                    <div style="font-size:12px;color:#8B8B9E;">8 shapes &bull; #EC4899</div>
                  </div>
                </div>
                <span style="color:#60A5FA;font-weight:600;font-size:14px;">Swap &rarr;</span>
              </div>
              <div style="margin-top:auto;display:flex;gap:12px;">
                <button style="flex:1;height:52px;background:#10B981;color:#FFF;border:none;border-radius:14px;font-weight:700;font-size:15px;">Apply Color Changes</button>
                <button style="height:52px;padding:0 20px;background:#242430;color:#FFF;border:none;border-radius:14px;font-weight:600;font-size:14px;">Reset</button>
              </div>
            </div>
          </div>
        """
    },
    {
        "id": "4",
        "title": "EXPORT 1080P &bull; 4K &bull; 8K",
        "sub": "Crisp lossless rasterization with transparency or solid canvas",
        "mockup": """
          <div style="background:#141419;border-radius:24px;border:1px solid #26262B;height:100%;display:flex;flex-direction:column;overflow:hidden;position:relative;">
            <div style="height:64px;display:flex;align-items:center;padding:0 20px;border-bottom:1px solid #222228;background:#0D0D12;">
              <span style="font-size:18px;font-weight:700;">Export Settings</span>
            </div>
            <div style="flex:1;padding:24px;display:flex;flex-direction:column;gap:16px;">
              <div style="font-size:12px;color:#8B8B9E;text-transform:uppercase;letter-spacing:0.1em;">Choose Output Resolution</div>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
                <div style="background:#1E1E28;border:1px solid #333344;padding:16px;border-radius:14px;text-align:center;">
                  <div style="font-size:18px;font-weight:800;color:#FFF;">1080p FHD</div>
                  <div style="font-size:11px;color:#8B8B9E;margin-top:4px;">1920 &times; 1080</div>
                </div>
                <div style="background:#1E1E28;border:1px solid #333344;padding:16px;border-radius:14px;text-align:center;">
                  <div style="font-size:18px;font-weight:800;color:#FFF;">4K UHD</div>
                  <div style="font-size:11px;color:#8B8B9E;margin-top:4px;">3840 &times; 2160</div>
                </div>
              </div>
              <div style="background:#2563EB;border:1px solid #3B82F6;padding:18px;border-radius:14px;text-align:center;">
                <div style="font-size:22px;font-weight:900;color:#FFF;">8K ULTRA HD ★</div>
                <div style="font-size:12px;color:#DBEAFE;margin-top:4px;">7680 &times; 4320 (Maximum Precision)</div>
              </div>
              <div style="font-size:12px;color:#8B8B9E;text-transform:uppercase;letter-spacing:0.1em;margin-top:8px;">Background Style</div>
              <div style="display:flex;gap:10px;">
                <div style="flex:1;padding:10px;background:#1E1E28;border:1px solid #3B82F6;border-radius:10px;text-align:center;font-size:12px;font-weight:700;">Obsidian</div>
                <div style="flex:1;padding:10px;background:#1E1E28;border:1px solid #282834;border-radius:10px;text-align:center;font-size:12px;color:#8B8B9E;">Transparent</div>
                <div style="flex:1;padding:10px;background:#1E1E28;border:1px solid #282834;border-radius:10px;text-align:center;font-size:12px;color:#8B8B9E;">White</div>
              </div>
              <button style="margin-top:auto;height:54px;background:#10B981;color:#FFF;border:none;border-radius:14px;font-weight:800;font-size:16px;">Download 8K PNG</button>
            </div>
          </div>
        """
    }
]

for s in screens:
    page_html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @import url('https://fonts.googleapis.com/css2?family=Urbanist:wght@500;600;700;800;900&display=swap');
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    width: 1080px;
    height: 1920px;
    overflow: hidden;
    background: #08090C;
    font-family: 'Urbanist', -apple-system, sans-serif;
    color: #FFFFFF;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 100px 70px 0;
  }}
  .kicker {{
    font-size: 16px;
    font-weight: 800;
    letter-spacing: 0.22em;
    color: #60A5FA;
    text-transform: uppercase;
    margin-bottom: 14px;
  }}
  .title {{
    font-size: 52px;
    font-weight: 900;
    letter-spacing: -0.02em;
    text-align: center;
    line-height: 1.15;
    margin-bottom: 14px;
  }}
  .sub {{
    font-size: 24px;
    color: #9E9E9E;
    text-align: center;
    max-width: 820px;
    line-height: 1.4;
    margin-bottom: 50px;
  }}
  .frame {{
    width: 900px;
    height: 1400px;
    background: #000;
    border-radius: 40px;
    padding: 14px;
    border: 3px solid #282834;
    box-shadow: 0 30px 80px rgba(0,0,0,0.8);
    position: relative;
  }}
</style>
</head>
<body>
  <div class="kicker">SVG Viewer Pro Free</div>
  <div class="title">{s['title']}</div>
  <div class="sub">{s['sub']}</div>
  <div class="frame">
    {s['mockup']}
  </div>
</body>
</html>"""

    t_html = Path(rf"C:\Users\Admin\AppData\Local\Temp\render_screen_{s['id']}.html")
    t_html.write_text(page_html, encoding="utf-8")
    
    t_png = Path(rf"C:\Users\Admin\AppData\Local\Temp\svg_screen_{s['id']}.png")
    cmd = [
        str(CHROME),
        "--headless=new",
        "--disable-gpu",
        "--force-device-scale-factor=1",
        "--window-size=1080,1920",
        f"--screenshot={t_png.resolve()}",
        t_html.resolve().as_uri()
    ]
    subprocess.run(cmd, check=True)
    print(f"Rendered Screenshot {s['id']}:", t_png.exists(), t_png.stat().st_size)
