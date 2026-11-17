# Day 1 验收清单

- [ ] `java -version` 显示 21
- [ ] `mvn -version` 可运行
- [ ] `node -v` 显示 20 或更高
- [ ] MySQL 8 服务已启动
- [ ] `database/init.sql` 执行成功
- [ ] 后端 `GET http://localhost:8080/api/health` 返回 code=0
- [ ] 后端 `GET http://localhost:8080/api/health/db` 返回数据库 UP
- [ ] 前端 `http://localhost:5173` 可访问，页面显示后端与数据库状态 UP
