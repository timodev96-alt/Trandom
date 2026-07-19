@echo off

if exist classes rmdir /s /q classes
mkdir classes
if exist app-unsigned.apk del /q app-unsigned.apk
if exist app-aligned.apk del /q app-aligned.apk
if exist app-signed.apk del /q app-signed.apk
if exist classes.dex del /q classes.dex
if not exist debug.keystore (
    echo debug.keystore not found. Generating a new one...
    call "B:\Java JDK 21\bin\keytool.exe" -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
    if %errorlevel% neq 0 (echo Keystore Generation Failed! && exit /b)
)

call B:\Hsoub\Kotlin\AndroidSDK\build-tools\33.0.2\aapt.exe package -f -m -J . -M AndroidManifest.xml -S res -I B:\Hsoub\Kotlin\AndroidSDK\platforms\android-33\android.jar
if %errorlevel% neq 0 (echo Resource Generation Failed! && exit /b)

call kotlinc MainActivity.kt trandom.kt SpinWheelView.kt -classpath B:\Hsoub\Kotlin\AndroidSDK\platforms\android-33\android.jar -d classes/
if %errorlevel% neq 0 (echo Kotlin Compilation Failed! && exit /b)

call d8 --lib B:\Hsoub\Kotlin\AndroidSDK\platforms\android-33\android.jar --output . classes\com\example\trandom\*.class B:\Hsoub\Kotlin\kotlinc\lib\kotlin-stdlib.jar
if %errorlevel% neq 0 (echo D8 Dexing Failed! && exit /b)

call B:\Hsoub\Kotlin\AndroidSDK\build-tools\33.0.2\aapt.exe package -f -M AndroidManifest.xml -S res -I B:\Hsoub\Kotlin\AndroidSDK\platforms\android-33\android.jar -0 arsc -F app-unsigned.apk
if %errorlevel% neq 0 (echo APK Packaging Failed! && exit /b)

call B:\Hsoub\Kotlin\AndroidSDK\build-tools\33.0.2\aapt.exe add app-unsigned.apk classes.dex >nul

call B:\Hsoub\Kotlin\AndroidSDK\build-tools\33.0.2\zipalign.exe -f 4 app-unsigned.apk app-aligned.apk
if %errorlevel% neq 0 (echo APK Alignment Failed! && exit /b)
call B:\Hsoub\Kotlin\AndroidSDK\build-tools\33.0.2\apksigner.bat sign --ks debug.keystore --ks-pass pass:android --out app-signed.apk app-aligned.apk
if %errorlevel% neq 0 (echo APK Signing Failed! && exit /b)

adb uninstall com.example.trandom >nul 2>&1
adb install -r app-signed.apk
if %errorlevel% neq 0 (echo ADB Install Failed! && exit /b)

adb shell am start -n com.example.trandom/.MainActivity