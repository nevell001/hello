#!/bin/bash
# 狸算(LiSuan) 数据库定时备份脚本
# 建议通过 crontab 每天定时执行，例如: 0 3 * * * /path/to/backup-db.sh

# --- 配置区域 ---
# 备份文件存放目录
BACKUP_DIR="/opt/lisuan/backups/sql"
# MySQL 容器名称
CONTAINER_NAME="lisuan-mysql"
# 数据库名称
DB_NAME="lisuan_system"
# 备份保留天数
RETENTION_DAYS=7

# --- 自动处理 ---
mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/lisuan_$TIMESTAMP.sql"

# 执行备份
# 注意：密码通过环境变量传递，避免在日志中暴露
# 确保在运行此脚本的环境中已导出或设置了 MYSQL_PASSWORD
docker exec "$CONTAINER_NAME" /usr/bin/mysqldump -u lisuan -p"$MYSQL_PASSWORD" "$DB_NAME" > "$BACKUP_FILE"

# 校验备份是否成功
if [ $? -eq 0 ]; then
    echo "[$(date)] 备份成功: $BACKUP_FILE"
else
    echo "[$(date)] 备份失败!"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# 清理超过保留天数的旧备份
find "$BACKUP_DIR" -name "lisuan_*.sql" -mtime +"$RETENTION_DAYS" -exec rm {} \;
echo "[$(date)] 已清理超过 $RETENTION_DAYS 天的旧备份"
