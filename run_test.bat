@echo off
chcp 65001 > nul
echo ===================================================
echo   BIEN DICH VA CHAY HE THONG (BORROW / RETURN APP)
echo ===================================================

if not exist "backend\bin" mkdir backend\bin

javac -encoding UTF-8 -d backend\bin backend\src\policy\*.java backend\src\model\*.java backend\src\exception\*.java backend\src\repository\*.java backend\src\service\*.java backend\src\handler\*.java backend\src\main\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] Bien dich thanh cong! Dang khoi chay he thong...
    echo.
    java -cp backend\bin main.Main
) else (
    echo.
    echo [ERROR] Bien dich that bai! Kiem tra cac file bi loi.
)

pause
