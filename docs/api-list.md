# 关键接口清单（Swagger/Knife4j）

## 鉴权
- `POST /api/auth/register` 注册
- `POST /api/auth/login` 登录
- `GET /api/auth/me` 当前用户

## 知识库
- `POST /api/kb` 创建知识库
- `GET /api/kb` 分页查询知识库
- `DELETE /api/kb/{id}` 删除知识库

## 文档
- `POST /api/documents/upload` 上传 PDF/Markdown
- `GET /api/documents` 分页查询文档
- `DELETE /api/documents/{id}` 删除文档

## RAG 问答
- `POST /api/qa/ask` 问答（向量检索 + 大模型生成）

### `POST /api/qa/ask` 请求示例
```json
{
  "knowledgeBaseId": 1,
  "question": "请总结该文档中的系统架构"
}
```

### 响应示例
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "answer": "...",
    "cached": false,
    "latencyMs": 842,
    "sources": [
      {
        "documentId": 3,
        "documentName": "架构设计.pdf",
        "chunkIndex": 4,
        "snippet": "...",
        "score": 1.72
      }
    ]
  }
}
```

## 统计
- `GET /api/stats/hot-questions` 问答热度
- `GET /api/stats/document-usage` 文档命中频次

## 管理员
- `GET /api/admin/users` 用户列表（`ROLE_ADMIN`）

## WebSocket
- `ws://{host}/ws/qa?token={jwt}`
- 推送类型：`qa_result`
