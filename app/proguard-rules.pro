# Keep JavaScript bridge methods (called reflectively by WebView)
-keepclassmembers class com.khanhminh.svgviewer.render.SvgBridge {
    public *;
}
