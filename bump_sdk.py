path = r"C:\Users\Admin\AndroidStudioProjects\svg app\app\build.gradle.kts"
with open(path, "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace("compileSdk = 35", "compileSdk = 36")
text = text.replace("targetSdk = 35", "targetSdk = 36")
text = text.replace("versionCode = 9", "versionCode = 10")
text = text.replace('versionName = "1.5.0"', 'versionName = "1.5.1"')

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("Updated build.gradle.kts successfully!")
