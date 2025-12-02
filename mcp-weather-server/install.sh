#!/bin/bash

# MCP 天气服务安装脚本

echo "正在安装 MCP 天气服务依赖..."

# 检查 Node.js 是否安装
if ! command -v node &> /dev/null; then
    echo "错误: 未找到 Node.js，请先安装 Node.js"
    echo "访问 https://nodejs.org/ 下载安装"
    exit 1
fi

# 检查 npm 是否安装
if ! command -v npm &> /dev/null; then
    echo "错误: 未找到 npm，请先安装 npm"
    exit 1
fi

# 显示版本信息
echo "Node.js 版本: $(node -v)"
echo "npm 版本: $(npm -v)"

# 安装依赖
echo "正在安装依赖包..."
npm install

if [ $? -eq 0 ]; then
    echo "✅ 依赖安装成功！"
    echo ""
    echo "下一步："
    echo "1. 确保 mcp-servers.json 中已配置天气服务"
    echo "2. 重启 Spring Boot 应用"
    echo "3. 查看日志确认工具已加载"
else
    echo "❌ 依赖安装失败，请检查错误信息"
    exit 1
fi
