@echo off

REM ==================== 准备工作 ====================
cd /d "%~dp0"
if not exist "uploads" mkdir "uploads"
echo [OK] uploads 目录已就绪: %CD%\uploads

REM ==================== 打包后端 ====================
echo ========== 打包后端 ==========
cd /d "%~dp0backend"
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 后端打包失败！
    pause
    exit /b 1
)

REM ==================== 复制后端已有的上传文件 ====================
echo ========== 同步上传文件 ==========
cd /d "%~dp0"
if exist "backend\uploads\*" (
    xcopy /Y /E "backend\uploads\*" "uploads\" >nul
    echo [OK] 后端上传文件已同步到根目录 uploads
) else (
    echo [提示] 后端 uploads 目录为空，跳过
)

echo ========== 打包 WebSocket 服务 ==========
cd /d "%~dp0websocket_service"
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo WebSocket 服务打包失败！
    pause
    exit /b 1
)

echo ========== 打包前端 ==========
cd /d "%~dp0frontend_nuxt"
call npm run build
if %errorlevel% neq 0 (
    echo 前端打包失败！
    pause
    exit /b 1
)

echo ========== 打包完成 ==========
echo.
echo 提示：运行时请确保 uploads 目录与 jar 包在同一目录
pause