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

# 数据库生产配置门禁
DB_CONFIG="config/database.properties"
if [ ! -f "$DB_CONFIG" ]; then
    echo "✗ 找不到数据库配置文件: $DB_CONFIG"
    exit 1
fi

DB_URL=$(grep '^db.url=' "$DB_CONFIG" | cut -d= -f2-)
DB_PASSWORD=$(grep '^db.password=' "$DB_CONFIG" | cut -d= -f2-)

if echo "$DB_URL" | grep -E 'useSSL=false|sslMode=DISABLED' > /dev/null 2>&1; then
    echo "✗ 数据库连接禁用了 SSL，请改用 sslMode=PREFERRED/REQUIRED/VERIFY_CA/VERIFY_IDENTITY"
    exit 1
fi

if [ -n "$DB_PASSWORD" ]; then
    echo "✗ config/database.properties 不应保存数据库密码，请改用 CASHER_DB_PASSWORD 环境变量"
    exit 1
fi

if [ -z "${CASHER_DB_PASSWORD:-}" ]; then
    echo "✗ 未设置 CASHER_DB_PASSWORD，生产发布必须通过环境变量提供数据库密码"
    exit 1
fi
echo "✓ 数据库密码与 SSL 配置通过"

# API 生产配置门禁：API 未开启时允许发布；开启后必须使用强密钥和限定 CORS。
API_CONFIG="config/api.properties"
if [ -f "$API_CONFIG" ] && grep -q '^api.enabled=true' "$API_CONFIG"; then
    TOKEN_SECRET_VALUE="${TOKEN_SECRET:-$(grep '^token.secret=' "$API_CONFIG" | cut -d= -f2-)}"
    CORS_VALUE="${CORS_ALLOWED_ORIGINS:-$(grep '^cors.allowed.origins=' "$API_CONFIG" | cut -d= -f2-)}"

    if [ "${#TOKEN_SECRET_VALUE}" -lt 32 ] \
        || [ "$TOKEN_SECRET_VALUE" = "default_secret_key" ] \
        || echo "$TOKEN_SECRET_VALUE" | grep -E 'REPLACE_|change_this|your_secret' > /dev/null 2>&1; then
        echo "✗ API 已开启，但 TOKEN_SECRET 不安全"
        exit 1
    fi

    if [ -z "$CORS_VALUE" ] || echo "$CORS_VALUE" | grep '\*' > /dev/null 2>&1; then
        echo "✗ API 已开启，但 CORS_ALLOWED_ORIGINS/cors.allowed.origins 未限制为具体来源"
        exit 1
    fi
    echo "✓ API 安全配置通过"
else
    echo "✓ API 未开启，跳过 API 密钥与 CORS 发布门禁"
fi

# 支付配置提示：关闭电子支付不阻断现金收银发布，但真实线上收款必须另行配置。
if [ -f "config/payment.properties" ]; then
    PAYMENT_MODE=$(grep '^payment.mode=' config/payment.properties | cut -d= -f2-)
    if [ "$PAYMENT_MODE" = "disabled" ]; then
        echo "⚠ 电子支付当前为 disabled；如生产需要微信/支付宝收款，请先配置真实支付通道"
    fi
fi

# 检查敏感信息（二次确认）
if grep -r -E "RootPassword123!|db.password=.+[^[:space:]]" config/ src/main/resources/ \
    --exclude='database.properties.template' \
    --exclude='*.example' > /dev/null 2>&1; then
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
