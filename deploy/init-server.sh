#!/bin/bash
# init-server.sh - 服务器初始化脚本（首次执行一次即可）
#
# 目录结构:
#   前端: /opt/app/frontend/
#   后端: /opt/www/bill-blue/backend.jar  /opt/www/bill-green/backend.jar
#   端口: 9000=蓝, 9001=绿

set -e

echo "==> 创建目录结构..."
mkdir -p /opt/app
mkdir -p /opt/www/bill-blue
mkdir -p /opt/www/bill-green

echo "==> 初始化端口标记 (默认蓝=9000)..."
echo "9000" > /opt/www/.current

echo ""
echo "=========================================="
echo "  服务器初始化完成！"
echo "=========================================="
echo ""
echo "  目录:"
echo "  /opt/app/               ← 前端 (所有版本共用)"
echo "  /opt/www/bill-blue/backend.jar  ← 后端蓝版"
echo "  /opt/www/bill-green/backend.jar ← 后端绿版"
echo "  /opt/www/.current      ← 当前端口标记 (9000)"
echo ""
echo "  之后每次部署只需在本机执行:"
echo "  ./deploy.sh <服务器IP>"
