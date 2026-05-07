# Docker Rebuild Plan

## Context

当前 Docker 设置存在以下问题：
1. **Dockerfile 缺少 onto-evolve-graph-store 模块** — 只复制了 5 个模块的 POM，现有 6 个模块，构建失败
2. **CMD 错误** — `CMD nginx && java -jar app.jar` 中 nginx 前台运行，java 永远不会执行
3. **无 HEALTHCHECK** — 无法判断应用是否就绪
4. **docker-compose.yml 无 profile 分离** — postgres 和 neo4j 始终作为硬依赖，但默认模式是 H2+memory，不需要它们
5. **无 .env.example** — 环境变量分散在代码中

## Design

### 使用方式

```
# 快速开发（app only，H2+memory，无需外部依赖）
docker compose up

# 全栈模式（app + Neo4j + Postgres）
docker compose -f docker-compose.yml -f docker-compose.full.yml up -d
```

### 具体改动

| 文件 | 操作 | 关键改动 |
|------|------|----------|
| `Dockerfile` | 重写 | 加 graph-store、修复 CMD、加 HEALTHCHECK、nginx.conf 改为 COPY |
| `docker-compose.yml` | 重写 | 仅含 app 服务，无 postgres/neo4j 依赖 |
| `docker-compose.full.yml` | 新建 | 增加 neo4j+postgres 服务，覆写 app 环境变量切换到全栈模式 |
| `.dockerignore` | 新建 | 排除 node_modules/target/.git |
| `frontend/nginx.conf` | 新建 | 独立 nginx 配置，替代内联 echo |
| `.env.example` | 新建 | 文档化全部环境变量 |

## Verification

1. `docker build .` — 构建成功，6 个模块全部编译
2. `docker compose up` — app 启动，H2+memory 模式
3. `docker compose -f docker-compose.yml -f docker-compose.full.yml up -d` — 全栈启动
4. HEALTHCHECK 在启动后 60s 内通过
5. `curl localhost/api/education/events` 返回 200
