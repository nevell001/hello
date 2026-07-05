#!/bin/bash
# 狸算(LiSuan) 生产发布候选验证脚本
# 该脚本执行完整的测试、静态扫描、打包和验证流程

set -e

echo "===================================================="
echo "      狸算(LiSuan) 生产发布候选验证开始"
echo "===================================================="

# 0. 版本一致性门禁
echo "[0/4] 正在校验版本一致性..."
VERSION_POM=$(grep -m 1 "<version>" pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
VERSION_JAVA=$(grep "APP_VERSION =" src/main/java/com/cashier/constant/AppConstants.java | sed 's/.*"\(.*\)".*/\1/')

if [ "$VERSION_POM" != "$VERSION_JAVA" ]; then
    echo "✗ 错误：版本号不一致！"
    echo "  pom.xml: $VERSION_POM"
    echo "  AppConstants.java: $VERSION_JAVA"
    exit 1
fi
echo "✓ 版本号一致 ($VERSION_POM)"

# 1. 单元测试与覆盖率
mvn clean test -DskipTests=false
echo "✓ 单元测试通过"

# 2. 静态代码分析
echo "[2/4] 正在运行 SpotBugs 静态安全扫描..."
mvn spotbugs:check
echo "✓ 静态扫描通过"

# 3. 构建可执行包
echo "[3/4] 正在构建生产环境安装包..."
mvn package -DskipTests=true
echo "✓ 打包成功"

# 4. 环境与配置验证
echo "[4/4] 验证发布配置..."

# 检查 JAR 文件
if [ -f "target/lisuan-fx-2.5.9-jar-with-dependencies.jar" ]; then
    echo "✓ 可执行 JAR 已生成"
else
    echo "✗ 找不到可执行 JAR"
    exit 1
fi

# 检查敏感信息（二次确认）
if grep -r "RootPassword123!" config/ src/main/resources/ > /dev/null 2>&1; then
    echo "✗ 警告：在代码或配置中发现疑似泄露的本地密码！请清理后再发布。"
    exit 1
else
    echo "✓ 未发现明显的本地泄露密码"
fi

# 检查 .env 是否被误包含在 target 中（如果有）
if [ -f "target/classes/.env" ] || [ -f "target/classes/.env.example" ]; then
    echo "✗ 警告：.env 文件被误包含在打包资源中！"
    exit 1
fi

echo "===================================================="
echo "      狸算(LiSuan) 发布验证全部通过！"
echo "      发布版本: 2.5.9"
echo "===================================================="
