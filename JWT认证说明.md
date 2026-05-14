# JWT 认证说明

本项目已加入轻量级 JWT 登录态，不引入完整 Spring Security 登录流程，继续沿用现有 Controller、Service、Result 返回结构。

## 1. 登录流程

用户通过下面任意方式登录成功后，后端会在返回的 `UserLoginVO` 中附带 JWT：

- `POST /user/login`：邮箱 + 密码登录
- `POST /user/login/code`：邮箱 + 验证码登录

返回体中的关键字段：

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "用户昵称",
  "role": "USER",
  "token": "JWT_TOKEN",
  "expiresAt": 1770000000
}
```

`expiresAt` 是秒级时间戳，当前默认有效期为 24 小时。

## 2. 前端请求方式

前端登录后会把用户信息和 `token` 一起保存到 `localStorage` 的 `guizhou_job_user`。

之后所有 API 请求会自动携带：

```http
Authorization: Bearer JWT_TOKEN
```

原来的 `X-User-Id` 不再由前端发送，敏感接口不再信任前端传入的用户 ID。

## 3. 后端认证结构

新增后端类：

- `org.txd.guizhoujob.auth.JwtService`：签发和校验 JWT，使用 HMAC-SHA256。
- `org.txd.guizhoujob.auth.JwtAuthInterceptor`：从 `Authorization` 请求头解析 token。
- `org.txd.guizhoujob.auth.AuthContext`：在当前请求线程保存登录用户信息。
- `org.txd.guizhoujob.auth.WebMvcConfig`：注册 JWT 拦截器。

JWT 中保存的主要信息：

- `sub`：用户 ID
- `email`：用户邮箱
- `role`：用户角色
- `iat`：签发时间
- `exp`：过期时间

## 4. 已接入 JWT 的敏感接口

以下接口现在通过 JWT 获取当前用户身份：

- `POST /user/password/change`
- `GET /user/{id}`
- `PUT /user/profile`
- `POST /recommend/refresh`
- `GET /recommend/list`
- `/admin/**` 下的用户和岗位管理接口

其中 `/admin/**` 仍会在 Service 层查询数据库校验当前用户是否为 `ADMIN`，不是只相信 token 里的角色。

## 5. 配置项

配置文件位置：

`Java/GuiZhouJob/src/main/resources/application.properties`

新增配置：

```properties
app.jwt.secret=${GUIZHOU_JOB_JWT_SECRET:guizhou-job-local-jwt-secret-change-before-production-2026}
app.jwt.expire-hours=24
```

生产环境建议设置系统环境变量：

```powershell
$env:GUIZHOU_JOB_JWT_SECRET="换成至少32位的高强度随机字符串"
```

注意：`app.jwt.secret` 不能少于 32 个字符。正式部署时不要使用默认值。

## 6. 验证方式

后端验证：

```powershell
cd "D:\graduation Project\Java\GuiZhouJob"
.\mvnw.cmd test
```

前端验证：

```powershell
cd "D:\graduation Project\Vue\guizhou-job-web"
npm run build
```

手动联调时，先登录获取 `token`，再请求需要登录的接口，并确认请求头带有 `Authorization: Bearer ...`。
