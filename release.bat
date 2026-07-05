@echo off
REM 狸算(LiSuan) 生产发布候选验证脚本 (Windows)

echo ====================================================
echo       狸算(LiSuan) 生产发布候选验证开始
echo ====================================================

REM 1. 单元测试
echo [1/4] 正在运行单元测试...
call mvn clean test -DskipTests=false
if %ERRORLEVEL% NEQ 0 (
    echo 单元测试失败！
    exit /b %ERRORLEVEL%
)

REM 2. 静态分析
echo [2/4] 正在运行 SpotBugs 扫描...
call mvn spotbugs:check
if %ERRORLEVEL% NEQ 0 (
    echo 静态扫描发现高风险缺陷！
    exit /b %ERRORLEVEL%
)

REM 3. 打包
echo [3/4] 正在构建生产环境安装包...
call mvn package -DskipTests=true
if %ERRORLEVEL% NEQ 0 (
    echo 打包失败！
    exit /b %ERRORLEVEL%
)

REM 4. 验证
echo [4/4] 验证发布配置...
if not exist "target\lisuan-fx-2.5.9-jar-with-dependencies.jar" (
    echo 找不到可执行 JAR！
    exit /b 1
)

echo ====================================================
echo       狸算(LiSuan) 发布验证全部通过！
echo ====================================================
