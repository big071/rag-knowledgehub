# 后端包结构（可直接开工）

```text
com.rag.knowledgehub
├── KnowledgeHubApplication.java
├── common
│   ├── exception
│   └── web
├── config
├── controller
├── dto
│   ├── auth
│   ├── kb
│   └── qa
├── entity
├── enums
├── mapper
├── security
├── service
│   └── impl
├── util
└── websocket
```

## 模块职责

- `controller`：REST 接口（鉴权、知识库、文档、问答、统计、管理员）。
- `service/impl`：业务编排（文档解析、向量化、检索、LLM 调用、缓存、记录沉淀）。
- `mapper`：MyBatis-Plus 数据访问层。
- `entity`：数据库实体。
- `security`：JWT 鉴权、RBAC 权限控制、权限异常处理。
- `websocket`：问答结果实时推送。
- `common`：统一返回、分页返回、异常体系、限流拦截器。
- `config`：安全、Redis、MyBatis、OpenAPI、WebSocket、配置绑定。

## 关键配置

- `application.yml`
  - `app.jwt`：JWT 密钥与过期时间
  - `app.rag`：chunk size / overlap / topK
  - `app.embedding`：Sentence-BERT 服务地址
  - `app.llm`：大模型 API 地址、key、model
  - `app.elasticsearch`：索引和连接
