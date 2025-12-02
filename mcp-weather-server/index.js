#!/usr/bin/env node

/**
 * MCP 天气查询服务
 * 提供城市天气查询功能
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

// 创建 MCP 服务器
const server = new Server(
  {
    name: "mcp-weather-server",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// 定义天气查询工具
const weatherTool = {
  name: "get_weather",
  description: "查询指定城市的天气情况",
  inputSchema: {
    type: "object",
    properties: {
      city: {
        type: "string",
        description: "城市名称，例如：北京、上海、广州",
      },
    },
    required: ["city"],
  },
};

// 处理工具列表请求
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [weatherTool],
  };
});

// 处理工具调用请求
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  if (name === "get_weather") {
    // 获取城市名称
    const city = args?.city;
    
    if (!city || typeof city !== "string") {
      return {
        content: [
          {
            type: "text",
            text: "错误：请提供有效的城市名称",
          },
        ],
        isError: true,
      };
    }

    // 固定返回：XX市是晴天
    const result = `${city}市是晴天`;

    return {
      content: [
        {
          type: "text",
          text: result,
        },
      ],
    };
  }

  // 未知工具
  return {
    content: [
      {
        type: "text",
        text: `错误：未知的工具 "${name}"`,
      },
    ],
    isError: true,
  };
});

// 启动服务器
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("MCP 天气服务已启动");
}

main().catch((error) => {
  console.error("服务器启动失败:", error);
  process.exit(1);
});
