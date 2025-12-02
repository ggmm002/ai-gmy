#!/bin/bash

# MCP 天气服务启动脚本
# 用于在 mcp-servers.json 中配置使用

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 切换到脚本目录
cd "$SCRIPT_DIR"

# 启动服务
node index.js
