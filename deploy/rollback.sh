#!/bin/bash
# rollback.sh - 回滚到上一个版本
#
# 在本地执行:
#   ./rollback.sh <服务器IP>

set -e

SERVER=$1
if [[ -z "$SERVER" ]]; then
  echo "用法: ./rollback.sh <服务器IP>"
  echo "示例: ./rollback.sh 8.138.206.227"
  exit 1
fi

CURRENT=$(ssh root@"$SERVER" "cat /opt/www/.current 2>/dev/null || echo '9000'")

if [[ "$CURRENT" == "9000" ]]; then
  TARGET_PORT=9001
  echo "==> 切换到 绿 版本 (端口 $TARGET_PORT)"
else
  TARGET_PORT=9000
  echo "==> 切换到 蓝 版本 (端口 $TARGET_PORT)"
fi

ssh root@"$SERVER" << ENDSSH
sed -i "s/proxy_pass http:\/\/127.0.0.1:[0-9]*\/api/proxy_pass http:\/\/127.0.0.1:$TARGET_PORT\/api/" /etc/nginx/nginx.conf
nginx -s reload
echo "$TARGET_PORT" > /opt/www/.current
echo "    nginx 已切换到端口 $TARGET_PORT"
ENDSSH

echo ""
echo "=========================================="
echo "  回滚完成！"
echo "  前端: http://$SERVER"
echo "=========================================="
