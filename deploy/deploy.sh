#!/bin/bash
# deploy.sh - 前后端一键部署脚本（蓝绿部署）
#
# 目录结构:
#   前端: /opt/app/frontend/          ← 所有版本共用，部署时直接覆盖
#   后端: /opt/www/bill-blue/backend.jar  (端口9000)
#          /opt/www/bill-green/backend.jar (端口9001)
#   端口切换由 nginx proxy_pass 实现，nginx 指向当前版本端口
#
# 用法（在本地执行）:
#   ./deploy.sh <服务器IP> [版本标签]

set -e

SERVER=$1
VERSION=${2:-$(date +%Y%m%d-%H%M%S)}
if [[ -z "$SERVER" ]]; then
  echo "用法: ./deploy.sh <服务器IP> [版本标签]"
  echo "示例: ./deploy.sh 8.138.206.227"
  echo "示例: ./deploy.sh 8.138.206.227 v1.2.3"
  exit 1
fi

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$DEPLOY_DIR/.."
FRONTEND_DIR="$BACKEND_DIR/frontend"
DIST_DIR="$FRONTEND_DIR/dist"

echo "=========================================="
echo "  一键部署 - 版本: $VERSION"
echo "=========================================="

# 1. 构建前端
echo ""
echo "==> [1/5] 构建前端..."
cd "$FRONTEND_DIR"
npm install 2>/dev/null || true
npm run build
echo "    前端构建完成"

# 2. 构建后端
echo ""
echo "==> [2/5] 构建后端..."
cd "$BACKEND_DIR"
mvn clean package -DskipTests -q
JAR_FILE=$(ls target/bill-*.jar 2>/dev/null | head -1)
if [[ -z "$JAR_FILE" ]]; then
  echo "    ERROR: 打包失败"
  exit 1
fi
echo "    后端打包完成: $(basename $JAR_FILE)"

# 3. 决定蓝绿目标
CURRENT_PORT=$(ssh root@"$SERVER" "cat /opt/www/.current 2>/dev/null || echo '9000'")
if [[ "$CURRENT_PORT" == "9000" ]]; then
  TARGET_PORT=9001
  TARGET_DIR=green
else
  TARGET_PORT=9000
  TARGET_DIR=blue
fi
echo ""
echo "==> [3/5] 上传到服务器 (目标: $TARGET_DIR 端口 $TARGET_PORT)..."

# 上传前端到 /opt/app/（直接覆盖）
echo "    上传前端到 /opt/app/..."
ssh root@"$SERVER" "mkdir -p /opt/app"
rsync -avz --delete "$DIST_DIR/" root@"$SERVER":/opt/app/

# 上传后端到 /opt/www/bill-{blue,green}/
echo "    上传后端到 /opt/www/bill-$TARGET_DIR/backend.jar..."
scp -q "$JAR_FILE" root@"$SERVER":/opt/www/bill-$TARGET_DIR/backend.jar

# 4. 重启目标服务
echo ""
echo "==> [4/5] 启动新版本服务 (端口 $TARGET_PORT)..."

ssh root@"$SERVER" << ENDSSH
TARGET_PORT=$TARGET_PORT
TARGET_DIR=$TARGET_DIR

pkill -f "server.port=\$TARGET_PORT" 2>/dev/null || true
sleep 2

cd /opt/www/bill-\$TARGET_DIR
nohup java -jar backend.jar --server.port=\$TARGET_PORT > app.log 2>&1 &
NEW_PID=\$!
echo "    新进程 PID: \$NEW_PID"

sleep 5

if ps -p \$NEW_PID > /dev/null 2>&1; then
    echo "    服务启动成功"
else
    echo "    ERROR: 服务启动失败"
    cat /opt/www/bill-\$TARGET_DIR/app.log | tail -20
    exit 1
fi
ENDSSH

ssh root@"$SERVER" "echo '$TARGET_PORT' > /opt/www/.current"

# 5. 切换 nginx
echo ""
echo "==> [5/5] 切换 nginx 到端口 $TARGET_PORT..."
ssh root@"$SERVER" "sed -i 's/proxy_pass http:\/\/127.0.0.1:[0-9]*\/api/proxy_pass http:\/\/127.0.0.1:$TARGET_PORT\/api/' /etc/nginx/nginx.conf && nginx -s reload"
echo "    nginx 已切换"

echo ""
echo "=========================================="
echo "  部署完成！"
echo "  前端: http://$SERVER"
echo "  API:  http://$SERVER/api"
echo "=========================================="
