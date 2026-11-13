@echo off
setlocal
copy /b "Aftergleam-v1.8.1-debug.apk.part001"+"Aftergleam-v1.8.1-debug.apk.part002"+"Aftergleam-v1.8.1-debug.apk.part003"+"Aftergleam-v1.8.1-debug.apk.part004"+"Aftergleam-v1.8.1-debug.apk.part005" "Aftergleam-v1.8.1-debug.apk"
if errorlevel 1 (echo Failed to join APK parts.& pause & exit /b 1)
echo Created Aftergleam-v1.8.1-debug.apk
pause
