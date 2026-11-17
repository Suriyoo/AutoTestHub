# AutoTest Hub

自动化测试平台项目

## 技术栈
- Backend: Java 21 / Spring Boot 3 / MyBatis-Plus / Maven
- Frontend: Vue 3 / TypeScript / Vite / Arco Design Vue
- Database: MySQL 8

## 目录
- `backend/` Spring Boot 后端
- `frontend/` Vue 管理端
- `database/` SQL 初始化脚本
- `docs/` 项目文档

## 本地启动
先安装 JDK 21、Maven、Node.js 20+、MySQL 8。

1. 创建数据库：执行 `database/init.sql`
2. 后端：进入 `backend/`，设置 `DB_USERNAME`、`DB_PASSWORD` 后运行 `mvn spring-boot:run`
3. 前端：进入 `frontend/`，运行 `npm install`，再运行 `npm run dev`
4. 浏览器打开 http://localhost:5173

## API
- `GET /api/health`
- `GET /api/health/db`
