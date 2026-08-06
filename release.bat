@echo off
setlocal EnableExtensions EnableDelayedExpansion
REM 狸算(LiSuan) 生产发布候选验证脚本 (Windows)

echo ====================================================
echo       狸算(LiSuan) 生产发布候选验证开始
echo ====================================================

REM 1. 完整验证门禁：单元测试 + SpotBugs + JaCoCo 覆盖率 + 打包
echo [1/3] 正在运行完整验证门禁 (mvn clean verify)...
call mvn clean verify -DskipTests=false
if %ERRORLEVEL% NEQ 0 (
    echo 完整验证失败（测试/SpotBugs/覆盖率/打包）！
    exit /b %ERRORLEVEL%
)
echo 完整验证通过（测试/SpotBugs/覆盖率/打包）

REM 2. 验证
echo [2/3] 验证发布配置...
for /f "tokens=2 delims=<>" %%V in ('findstr /B /C:"<version>" pom.xml') do (
    set "VERSION_POM=%%V"
    goto :version_found
)
:version_found
if not exist "target\lisuan-fx-%VERSION_POM%-jar-with-dependencies.jar" (
    echo 找不到可执行 JAR！
    exit /b 1
)

if not exist "config\database.properties" (
    echo 找不到数据库配置文件！
    exit /b 1
)

findstr /R /C:"useSSL=false" /C:"sslMode=DISABLED" "config\database.properties" >nul
if %ERRORLEVEL% EQU 0 (
    echo 数据库连接禁用了 SSL，请改用 sslMode=PREFERRED/REQUIRED/VERIFY_CA/VERIFY_IDENTITY
    exit /b 1
)

for /F "tokens=1,* delims==" %%A in ('findstr /B "db.password=" "config\database.properties"') do set "DB_PASSWORD_VALUE=%%B"
if defined DB_PASSWORD_VALUE (
    echo config\database.properties 不应保存数据库密码，请改用 CASHIER_DB_PASSWORD 环境变量
    exit /b 1
)

if "%CASHIER_DB_PASSWORD%"=="" (
    if "%CASHER_DB_PASSWORD%"=="" (
    echo 未设置 CASHIER_DB_PASSWORD，生产发布必须通过环境变量提供数据库密码
    exit /b 1
    )
)
echo 数据库密码与 SSL 配置通过

if exist "config\api.properties" (
    findstr /B /C:"api.enabled=true" "config\api.properties" >nul
    if !ERRORLEVEL! EQU 0 (
        if "%TOKEN_SECRET%"=="" (
            echo API 已开启，请通过 TOKEN_SECRET 设置强随机密钥
            exit /b 1
        )
        if "%CORS_ALLOWED_ORIGINS%"=="" (
            echo API 已开启，请设置 CORS_ALLOWED_ORIGINS
            exit /b 1
        )
        echo %CORS_ALLOWED_ORIGINS% | findstr "\*" >nul
        if !ERRORLEVEL! EQU 0 (
            echo API 已开启，但 CORS_ALLOWED_ORIGINS 不能包含通配符
            exit /b 1
        )
        echo API 安全配置通过
    ) else (
        echo API 未开启，跳过 API 密钥与 CORS 发布门禁
    )
) else (
    echo API 未开启，跳过 API 密钥与 CORS 发布门禁
)

findstr /R /C:"db.password=..*" "config\database.properties" >nul
if %ERRORLEVEL% EQU 0 (
    echo 发现疑似数据库密码泄露
    exit /b 1
)

REM 支付配置门禁：生产发布禁止 mock 模式；production 模式必须无占位凭据
if exist "config\payment.properties" (
    findstr /I /B /C:"payment.mode=mock" "config\payment.properties" >nul
    if !ERRORLEVEL! EQU 0 (
        echo 生产发布禁止使用 mock 支付模式！请配置真实支付通道或保持 disabled
        exit /b 1
    )
    findstr /I /B /C:"payment.mode=production" "config\payment.properties" >nul
    if !ERRORLEVEL! EQU 0 (
        findstr /I /C:"YOUR_" /C:"REPLACE_WITH" /C:"CHANGE_ME" "config\payment.properties" >nul
        if !ERRORLEVEL! EQU 0 (
            echo production 支付模式仍包含占位凭据，请填写真实商户配置
            exit /b 1
        )
        echo 支付配置使用 production 模式且未发现占位凭据
    )
)

echo ====================================================
echo       狸算(LiSuan) 发布验证全部通过！
echo ====================================================
