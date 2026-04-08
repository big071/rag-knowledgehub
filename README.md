# RAG KnowledgeHub（智能知识库问答系统）

面向 27 届 Java 后端同学的全栈 RAG 项目模板：支持 PDF/Markdown 上传、向量检索、生成式问答、来源溯源、热度统计。

## 1. 技术栈

### 后端
- Java 17
- Spring Boot 3.x + Spring MVC
- MyBatis-Plus + MySQL 8.0 + Redis 7.x
- LangChain4j（依赖保留，可扩展 Agent/RAG 流程编排）
- Elasticsearch 8.x（向量检索）
- Sentence-BERT（`all-MiniLM-L6-v2`，通过 `embedding-service`）
- ChatGPT 3.5 / 通义千问（OpenAI 兼容接口） + OkHttp
- Lombok + SLF4J/Logback + Junit5 + Swagger/Knife4j + Redisson

### 前端
- Vue 3 + Vite + Pinia + Vue Router
- Element Plus
- Axios + ECharts + PDF.js
- Tailwind CSS
- WebSocket

### 部署
- Maven / npm
- Git
- Docker Compose（MySQL / Redis / Elasticsearch / Embedding / Backend / Frontend）

## 2. 项目结构

```text
.
├── backend
├── frontend
├── embedding-service
├── docs
│   ├── sql/schema.sql
│   ├── backend-structure.md
│   └── api-list.md
├── scripts
│   ├── start-docker.ps1
│   ├── start-docker.sh
│   └── start-local.ps1
└── docker-compose.yml
```

## 3. 核心业务流程（RAG）

1. 用户登录后上传 PDF/Markdown。
2. 后端解析文档并切片。
3. 调用 Sentence-BERT 服务生成向量。
4. 文本片段+向量写入 Elasticsearch。
5. 用户提问，后端将问题向量化并在 ES 检索 Top5。
6. 后端把 Top5 片段拼接 Prompt 调用大模型。
7. 返回答案 + 来源片段，前端高亮展示并保存历史对话。

## 4. 快速启动（Docker）

### 4.1 前置要求
- Docker + Docker Compose

### 4.2 配置
```bash
cp .env.example .env
# 编辑 .env，填入 LLM_API_KEY（若使用 OpenAI/通义千问）
```

### 4.3 启动
```bash
docker compose up --build -d
```

访问：
- 前端：[http://localhost:5173](http://localhost:5173)
- 后端 API：[http://localhost:8080/doc.html](http://localhost:8080/doc.html)

## 5. 本地开发

1. 启动基础中间件（可用 docker）：MySQL、Redis、Elasticsearch、embedding-service。
2. 启动后端：`cd backend && mvn spring-boot:run`
3. 启动前端：`cd frontend && npm install && npm run dev`

## 6. 可直接开工内容

- 数据库 DDL：`docs/sql/schema.sql`
- 后端包结构说明：`docs/backend-structure.md`
- 关键接口列表：`docs/api-list.md`
- 一键启动脚本：`scripts/start-docker.ps1` / `scripts/start-docker.sh`

## 7. 简历亮点写法（示例）

- 基于 `Spring Boot 3 + Vue 3` 独立实现 RAG 知识库问答平台，支持文档上传、向量检索、来源溯源与统计分析。
- 构建 `Sentence-BERT + Elasticsearch` 向量检索链路，实现 TopK 召回与 Prompt 增强生成。
- 引入 `Redis 缓存 + Redisson 分布式锁 + 接口限流` 提升并发稳定性。
- 使用 Docker Compose 完成多服务容器化，支持一键启动与工程复现。
