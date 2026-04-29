@echo off
title Nesthub 启动脚本
echo 正在静默启动后台服务...

echo 正在启动 Redis（静默）...
start "Redis" /min redis-server

echo 正在启动 Nesthub 后端（静默）...
start "Nesthub后端" /min cmd /c "set MYSQL_HOST=localhost&& set MYSQL_PORT=3306&& set MYSQL_DATABASE=openisle&& set MYSQL_USER=root&& set MYSQL_PASSWORD=980326&& set REDIS_HOST=localhost&& set REDIS_PASSWORD=&& set RABBITMQ_HOST=localhost&& set RABBITMQ_USERNAME=guest&& set RABBITMQ_PASSWORD=guest&& set JWT_SECRET=change-me-jwt-secret&& java -jar .\backend\target\openisle-0.0.1-SNAPSHOT.jar"

echo 正在启动 WebSocket 服务（静默）...
start "Nesthub WebSocket" /min cmd /c "java -jar .\websocket_service\target\websocket-service-0.0.1-SNAPSHOT.jar"

echo 等待后端启动...
timeout /t 10 /nobreak >nul

echo 正在启动 Nesthub 前端...
start "Nesthub前端" cmd /c "node .\frontend_nuxt\.output\server\index.mjs"

echo.
echo ================================
echo 全部启动完成！
echo 后台服务已静默运行（Redis、后端、WebSocket）
echo 前端窗口保持显示
echo 浏览器打开 http://localhost:3000
echo ================================
pause