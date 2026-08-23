@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
REM 狸算(LiSuan) 生产发布候选验证脚本 (Windows)
REM 与 release.sh 保持同一套发布门禁

cd /d "%~dp0"

echo ====================================================
echo       狸算(LiSuan) 生产发布候选验证开始
echo ====================================================

REM 0. 版本一致性门禁
echo [0/3] 正在校验版本一致性...
set "VERSION_POM="
for /f "tokens=3 delims=<>" %%V in ('findstr /R "<version>[0-9]" pom.xml 2^>nul ^| findstr /V "javafx maven java mysql hikaricp"') do (
    set "VERSION_POM=%%V"
    goto :version_pom_found
)
:version_pom_found

set "VERSION_JAVA="
for /f "tokens=2 delims==" %%J in ('findstr /C:"APP_VERSION =" src\main\java\com\cashier\constant\AppConstants.java') do (
    set "VERSION_JAVA=%%J"
    goto :version_java_found
)
:version_java_found
set "VERSION_JAVA=!VERSION_JAVA:"=!"
set "VERSION_JAVA=!VERSION_JAVA:;=!"
set "VERSION_JAVA=!VERSION_JAVA: =!"

if not "%VERSION_POM%"=="%VERSION_JAVA%" (
    echo 错误：版本号不一致！
    echo   pom.xml: %VERSION_POM%
    echo   AppConstants.java: %VERSION_JAVA%
    exit /b 1
)
echo 版本号一致 (%VERSION_POM%)

REM 从 .env 定向读取数据库密码（仅限密码变量，与 start.sh/release.sh 保持一致）
if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if /i "%%a"=="CASHIER_DB_PASSWORD" if not defined CASHIER_DB_PASSWORD set "CASHIER_DB_PASSWORD=%%b"
        if /i "%%a"=="CASHER_DB_PASSWORD" if not defined CASHER_DB_PASSWORD set "CASHER_DB_PASSWORD=%%b"
    )
)
if defined CASHIER_DB_PASSWORD set "CASHIER_DB_PASSWORD=!CASHIER_DB_PASSWORD:"=!"
if defined CASHER_DB_PASSWORD set "CASHER_DB_PASSWORD=!CASHER_DB_PASSWORD:"=!"

REM 1. 完整验证门禁：单元测试 + SpotBugs + JaCoCo 覆盖率 + 打包
echo [1/3] 正在运行完整验证门禁 (mvn clean verify)...
call mvn clean verify -DskipTests=false
if %ERRORLEVEL% NEQ 0 (
    echo 完整验证失败（测试/SpotBugs/覆盖率/打包）！
    exit /b %ERRORLEVEL%
)
echo 完整验证通过（测试/SpotBugs/覆盖率/打包）

REM 2. 环境与配置验证
echo [2/3] 验证发布配置...

if not exist "target\lisuan-fx-%VERSION_POM%-jar-with-dependencies.jar" (
    echo 找不到可执行 JAR！
    exit /b 1
)
echo 可执行 JAR 已生成

if not exist "config\database.properties" (
    echo 找不到数据库配置文件！
    exit /b 1
)

findstr /R /C:"useSSL=false" /C:"sslMode=DISABLED" "config\database.properties" >nul
if %ERRORLEVEL% EQU 0 (
    echo 数据库连接禁用了 SSL，请改用 sslMode=PREFERRED/REQUIRED/VERIFY_CA/VERIFY_IDENTITY
    exit /b 1
)

findstr /R /C:"db.password=..*" "config\database.properties" >nul
if %ERRORLEVEL% EQU 0 (
    echo config\database.properties 不应保存数据库密码，请改用 CASHIER_DB_PASSWORD 环境变量
    exit /b 1
)

if "%CASHIER_DB_PASSWORD%"=="" if "%CASHER_DB_PASSWORD%"=="" (
    echo 未设置 CASHIER_DB_PASSWORD，生产发布必须通过环境变量提供数据库密码
    exit /b 1
)
echo 数据库密码与 SSL 配置通过

REM API 生产配置门禁：API 未开启时允许发布；开启后必须使用强密钥和限定 CORS
set "API_ENABLED="
if exist "config\api.properties" (
    findstr /B /C:"api.enabled=true" "config\api.properties" >nul
    if not errorlevel 1 set "API_ENABLED=1"
)
if defined API_ENABLED (
    set "TOKEN_SECRET_VALUE=!TOKEN_SECRET!"
    set "CORS_VALUE=!CORS_ALLOWED_ORIGINS!"

    if not defined TOKEN_SECRET_VALUE (
        if exist "config\api.properties" (
            for /f "usebackq tokens=1,* delims==" %%a in ("config\api.properties") do (
                if /i "%%a"=="token.secret" set "TOKEN_SECRET_VALUE=%%b"
            )
        )
    )
    if not defined CORS_VALUE (
        if exist "config\api.properties" (
            for /f "usebackq tokens=1,* delims==" %%a in ("config\api.properties") do (
                if /i "%%a"=="cors.allowed.origins" set "CORS_VALUE=%%b"
            )
        )
    )

    if "!TOKEN_SECRET_VALUE:~31!"=="" (
        echo API 已开启，但 TOKEN_SECRET 长度不足 32 位
        exit /b 1
    )
    if /i "!TOKEN_SECRET_VALUE!"=="default_secret_key" (
        echo API 已开启，但 TOKEN_SECRET 使用了默认值
        exit /b 1
    )
    echo !TOKEN_SECRET_VALUE! | findstr /I /C:"REPLACE_" /C:"change_this" /C:"your_secret" >nul
    if not errorlevel 1 (
        echo API 已开启，但 TOKEN_SECRET 包含占位符
        exit /b 1
    )
    if "!CORS_VALUE!"=="" (
        echo API 已开启，但 CORS_ALLOWED_ORIGINS/cors.allowed.origins 未限制为具体来源
        exit /b 1
    )
    echo !CORS_VALUE! | findstr "\*" >nul
    if not errorlevel 1 (
        echo API 已开启，但 CORS_ALLOWED_ORIGINS 不能包含通配符
        exit /b 1
    )
    echo API 安全配置通过
) else (
    echo API 未开启，跳过 API 密钥与 CORS 发布门禁
)

REM 支付配置门禁：生产发布禁止 mock 模式；production 模式必须无占位凭据；disabled 给出提示
set "PAYMENT_MODE="
if exist "config\payment.properties" (
    for /f "usebackq tokens=1,* delims==" %%a in ("config\payment.properties") do (
        if /i "%%a"=="payment.mode" set "PAYMENT_MODE=%%b"
    )
)
if defined PAYMENT_MODE (
    set "PAYMENT_MODE=!PAYMENT_MODE: =!"
    if /i "!PAYMENT_MODE!"=="mock" (
        echo 生产发布禁止使用 mock 支付模式！请配置真实支付通道或保持 disabled
        exit /b 1
    )
    if /i "!PAYMENT_MODE!"=="production" (
        findstr /I /C:"YOUR_" /C:"REPLACE_WITH" /C:"CHANGE_ME" /C:"change_me" /C:"your-" "config\payment.properties" >nul
        if not errorlevel 1 (
            echo production 支付模式仍包含占位凭据（YOUR_/REPLACE_WITH/CHANGE_ME），请填写真实商户配置
            exit /b 1
        )
        echo 支付配置使用 production 模式且未发现占位凭据（真实通道回调需另行验证）
    )
    if /i "!PAYMENT_MODE!"=="disabled" (
        echo 电子支付当前为 disabled；如生产需要微信/支付宝收款，请先配置真实支付通道
    )
)

REM 检查敏感信息（二次确认）
set "LEAK_FOUND=0"
for /r "config" %%f in (*) do call :check_leak "%%f"
for /r "src\main\resources" %%f in (*) do call :check_leak "%%f"
if "%LEAK_FOUND%"=="1" (
    echo 警告：在代码或配置中发现疑似泄露的本地密码！请清理后再发布。
    exit /b 1
)
echo 未发现明显的本地泄露密码

REM 检查 .env 是否被误包含在 target 中
if exist "target\classes\.env" (
    echo 警告：.env 文件被误包含在打包资源中！
    exit /b 1
)
if exist "target\classes\.env.example" (
    echo 警告：.env.example 文件被误包含在打包资源中！
    exit /b 1
)

echo ====================================================
echo       狸算(LiSuan) 发布验证全部通过！
echo       发布版本: %VERSION_POM%
echo ====================================================
exit /b 0

:check_leak
set "LEAK_FILE=%~1"
echo !LEAK_FILE! | findstr /I /C:".example" /C:"database.properties.template" >nul
if not errorlevel 1 exit /b 0
findstr /I /C:"RootPassword123!" /R /C:"db.password=..*" "!LEAK_FILE!" >nul 2>&1
if not errorlevel 1 (
    echo 在 !LEAK_FILE! 中发现疑似泄露的本地密码！
    set "LEAK_FOUND=1"
)
exit /b 0
