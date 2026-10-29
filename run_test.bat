@echo off
chcp 65001 > nul
echo ===================================================
echo   BIEN DICH VA CHAY THU KIEM THU BORROW_RETURN_SERVICE
echo ===================================================

if not exist "backend\bin" mkdir backend\bin

javac -encoding UTF-8 -d backend\bin backend\src\policy\*.java backend\src\model\*.java backend\src\exception\*.java backend\src\service\*.java backend\src\main\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] Bien dich thanh cong! Dang chay kiem thu...
    echo.
    java -cp backend\bin main.Main
) else (
    echo [ERROR] Bien dich that bai!
)

pause
