#!/bin/bash
# ============================================
# LiSuan 数据库备份/恢复演练脚本
# ============================================
# 用途：在不影响线上库的前提下，验证 mysqldump 备份可以完整恢复。
# 流程：1) 导出当前库  2) 恢复到演练库  3) 对比关键表行数  4) 自动清理演练库
#
# 前提：
#   - Docker 可用且 lisuan-mysql 容器运行中（docker compose up -d mysql）
#   - 提供 MySQL root 密码：export MYSQL_ROOT_PASSWORD='...'
#   - 可选覆盖：MYSQL_CONTAINER_NAME / MYSQL_DATABASE / LISUAN_DUMP_DIR
#
# 示例：
#   export MYSQL_ROOT_PASSWORD='your-root-password'
#   ./scripts/db-restore-drill.sh
#
# 说明：脚本只在演练库中操作，绝不写入/删除线上库；演练库名固定为 lisuan_restore_drill。
# ============================================

set -euo pipefail

CONTAINER="${MYSQL_CONTAINER_NAME:-lisuan-mysql}"
DB_NAME="${MYSQL_DATABASE:-lisuan_system}"
SCRATCH_DB="lisuan_restore_drill"
STAMP=$(date +%Y%m%d%H%M%S)
DUMP_DIR="${LISUAN_DUMP_DIR:-backups/sql}"
DUMP_FILE="${DUMP_DIR}/drill-${STAMP}.sql"

# 预检
if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
    echo "[错误] 请先设置环境变量 MYSQL_ROOT_PASSWORD（演练用 root 密码）" >&2
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "[错误] 未找到 docker 命令" >&2
    exit 1
fi

if ! docker exec "${CONTAINER}" mysqladmin ping -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent >/dev/null 2>&1; then
    echo "[错误] 容器 ${CONTAINER} 不可达或 root 密码错误，请先 docker compose up -d mysql" >&2
    exit 1
fi

mkdir -p "${DUMP_DIR}"

cleanup() {
    docker exec "${CONTAINER}" mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
        -e "DROP DATABASE IF EXISTS ${SCRATCH_DB};" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "============================================"
echo "  LiSuan 数据库恢复演练开始"
echo "  源库: ${DB_NAME}  演练库: ${SCRATCH_DB}"
echo "============================================"

# 1. 备份当前库
echo "[1/4] 导出源库..."
docker exec "${CONTAINER}" sh -c \
    "mysqldump -uroot -p\"${MYSQL_ROOT_PASSWORD}\" --single-transaction --routines --triggers \
     --default-character-set=utf8mb4 ${DB_NAME}" > "${DUMP_FILE}"
echo "✓ 备份完成: ${DUMP_FILE}"

# 2. 恢复到演练库
echo "[2/4] 创建演练库并恢复备份..."
docker exec "${CONTAINER}" mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    -e "CREATE DATABASE IF NOT EXISTS ${SCRATCH_DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
docker exec -i "${CONTAINER}" mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    --default-character-set=utf8mb4 "${SCRATCH_DB}" < "${DUMP_FILE}"
echo "✓ 恢复完成"

# 3. 对比关键表行数
echo "[3/4] 对比关键表行数..."
FAIL=0
for table in products members users transactions transaction_items promotions suppliers purchase_orders; do
    src=$(docker exec "${CONTAINER}" mysql -N -uroot -p"${MYSQL_ROOT_PASSWORD}" \
        -e "SELECT COUNT(*) FROM ${DB_NAME}.${table};" 2>/dev/null)
    rst=$(docker exec "${CONTAINER}" mysql -N -uroot -p"${MYSQL_ROOT_PASSWORD}" \
        -e "SELECT COUNT(*) FROM ${SCRATCH_DB}.${table};" 2>/dev/null)
    if [ "${src}" = "${rst}" ]; then
        echo "✓ ${table}: 源库=${src} 演练库=${rst}"
    else
        echo "✗ ${table}: 源库=${src} 演练库=${rst} 不一致！" >&2
        FAIL=1
    fi
done

if [ "${FAIL}" != "0" ]; then
    echo "[错误] 行数对比存在不一致，演练未通过" >&2
    exit 1
fi

# 4. 清理（trap 自动删除演练库）
echo "[4/4] 清理演练库（由退出钩子自动完成）..."

echo "============================================"
echo "  ✅ 恢复演练通过：备份可完整恢复到新库"
echo "  备份文件保留在: ${DUMP_FILE}"
echo "============================================"
