@echo off
setlocal EnableDelayedExpansion

if not exist "local.properties" (
    echo [ERROR] local.properties not found!
    echo Please create it in the project root.
    pause
    exit /b 1
)

for /f "tokens=1,* delims==" %%i in (local.properties) do (
    set "%%i=%%j"
)
set "BUILD_TOOLS=%sdk.dir%\build-tools\33.0.2"
set "PLATFORM=%sdk.dir%\platforms\android-33\android.jar"
set "KOTLIN_STDLIB=%kotlin.lib%\kotlin-stdlib.jar"
if not exist "%KOTLIN_STDLIB%" (
    echo [ERROR] kotlin-stdlib.jar not found at: "%KOTLIN_STDLIB%"
    pause
    exit /b 1
)

:: --- Cleanup ---
if exist classes rmdir /s /q classes
mkdir classes
for %%f in (app-unsigned.apk app-aligned.apk app-signed.apk classes.dex) do if exist %%f del /q %%f

:: --- Keystore ---
if not exist debug.keystore (
    echo Generating debug.keystore...
    call "%jdk.bin%\keytool.exe" -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
)

:: --- Build Process ---
echo [1/6] Generating Resources...
call "%BUILD_TOOLS%\aapt.exe" package -f -m -J . -M AndroidManifest.xml -S res -I "%PLATFORM%"
if %errorlevel% neq 0 goto :error

echo [2/6] Compiling Kotlin...
call kotlinc MainActivity.kt trandom.kt SpinWheelView.kt -classpath "%PLATFORM%" -d classes/
if %errorlevel% neq 0 goto :error

echo [3/6] Dexing...
call d8 --lib "%PLATFORM%" --output . classes\com\example\trandom\*.class "%KOTLIN_STDLIB%"
if %errorlevel% neq 0 goto :error

echo [4/6] Packaging APK...
call "%BUILD_TOOLS%\aapt.exe" package -f -M AndroidManifest.xml -S res -I "%PLATFORM%" -0 arsc -F app-unsigned.apk
if %errorlevel% neq 0 goto :error
call "%BUILD_TOOLS%\aapt.exe" add app-unsigned.apk classes.dex >nul

echo [5/6] Aligning...
call "%BUILD_TOOLS%\zipalign.exe" -f 4 app-unsigned.apk app-aligned.apk
if %errorlevel% neq 0 goto :error

echo [6/6] Signing...
call "%BUILD_TOOLS%\apksigner.bat" sign --ks debug.keystore --ks-pass pass:android --out app-signed.apk app-aligned.apk
if %errorlevel% neq 0 goto :error

:: --- Install ---
echo Installing...
adb uninstall com.example.trandom >nul 2>&1
adb install -r app-signed.apk
adb shell am start -n com.example.trandom/.MainActivity
echo Done!
goto :eof

:error
echo [!] Process Failed at step! Check the output above.
pause