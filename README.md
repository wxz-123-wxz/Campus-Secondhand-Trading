# 校园二手交易平台微服务版

这是一个基于 Spring Boot、Spring Cloud Alibaba、Nacos、Redis、RabbitMQ、MySQL 和支付宝沙箱的校园二手交易平台。项目适合用于分布式应用开发课程设计演示，包含用户、商品、订单、支付、网关等多个独立服务。

## 一、项目结构

```text
Campus secondhand trading
├─ campus-trade-common      公共 DTO、枚举、响应对象、常量
├─ campus-trade-gateway     后端网关，端口 8080
├─ campus-trade-user        用户服务，端口 8081
├─ campus-trade-product     商品服务，端口 8082
├─ campus-trade-order       订单服务，端口 8083
├─ campus-trade-payment     支付服务，端口 8084
├─ campus-trade-frontend    前端静态页面，端口 3000
├─ init-sql                 数据库初始化和示例数据脚本
├─ assets                   商品图片、首页背景图等素材
└─ docker-compose.yml       Redis、RabbitMQ、Nacos、MySQL 容器配置
```

## 二、系统架构

```text
浏览器前端: http://localhost:3000/index.html
        |
        | HTTP 请求
        v
后端网关 campus-trade-gateway: 8080
        |
        | Nacos 服务发现 + 网关路由 + JWT 鉴权
        v
+----------------+-------------------+----------------+-------------------+
| 用户服务 8081   | 商品服务 8082      | 订单服务 8083   | 支付服务 8084      |
| campus_user    | campus_product    | campus_order   | campus_payment    |
+----------------+-------------------+----------------+-------------------+

Nacos:    8848，服务注册与发现
Redis:    6379，缓存、登录黑名单、分布式锁
RabbitMQ: 5672/15672，订单和支付事件消息队列
MySQL:    3306，四个独立数据库
```

## 三、演示前准备

### 1. 必备软件

- JDK 17
- IntelliJ IDEA
- Maven
- MySQL 8.x
- Docker Desktop
- Python 3，用于启动前端静态服务器

### 2. 本机数据库配置

本项目当前演示环境使用本机 MySQL：

```text
MySQL 地址: localhost:3306
用户名: root
密码: 请使用你本机 MySQL 的 root 密码
```

如果你的 MySQL 密码不是默认值，需要在 IDEA 的环境变量里改成你自己的密码。

### 3. 创建数据库

如果数据库已经存在，可以跳过这一步。

在项目根目录打开 PowerShell，执行：

```powershell
mysql -uroot -p你的MySQL密码 < init-sql\01-init-databases.sql
```

该脚本会创建四个数据库：

```text
campus_user
campus_product
campus_order
campus_payment
```

注意：`02-sample-products.sql` 和 `03-fix-admin-user.sql` 依赖数据表已经存在。第一次运行项目时，可以先启动后端服务，让 JPA 自动创建表，再执行示例数据脚本。

## 四、启动基础设施

本项目演示时建议使用本机 MySQL，只用 Docker 启动 Redis、RabbitMQ、Nacos。

如果只是想快速演示，可以直接跳到“十四、一键启动和关闭”。一键启动脚本会自动启动 Redis、RabbitMQ、Nacos、5 个后端服务和前端页面。

在项目根目录执行：

```powershell
docker-compose up -d redis rabbitmq nacos
```

如果你的 Docker 版本推荐新命令，也可以执行：

```powershell
docker compose up -d redis rabbitmq nacos
```

启动后检查：

```text
Nacos 控制台: http://localhost:8848/nacos
RabbitMQ 控制台: http://localhost:15672
RabbitMQ 账号: guest
RabbitMQ 密码: guest
```

如果命令行出现 `version is obsolete`，只是 Docker Compose 的版本提示，不影响运行。

如果出现 `dockerDesktopLinuxEngine` 找不到，说明 Docker Desktop 没有启动，先打开 Docker Desktop，等它启动完成后再执行命令。

## 五、用 IDEA 启动后端服务

### 1. 打开项目

用 IDEA 打开项目根目录：

```text
D:\intellijIDEAworkSpace\ideaprojects\Campus secondhand trading
```

确认 IDEA 使用：

```text
JDK: 17
Maven: 根目录 pom.xml 已正常加载
```

### 2. 配置环境变量

每个 Spring Boot 启动配置都建议填写下面这组环境变量：

```text
MYSQL_HOST=localhost;MYSQL_PORT=3306;MYSQL_USER=root;MYSQL_PASSWORD=你的MySQL密码;REDIS_HOST=localhost;RABBITMQ_HOST=localhost;NACOS_HOST=localhost
```

IDEA 配置位置：

```text
Run/Debug Configurations -> Environment variables
```

如果不配置 `MYSQL_PASSWORD`，程序会使用默认密码 `123456`，如果你的本机密码不同会导致数据库连接失败。

### 3. 启动顺序

`campus-trade-common` 是公共模块，不需要单独启动。

在 IDEA 中依次启动下面 5 个 Spring Boot 启动类：

| 启动顺序 | 模块 | 启动类 | 端口 | 作用 |
|---|---|---|---|---|
| 1 | campus-trade-user | UserApplication | 8081 | 登录、注册、用户信息 |
| 2 | campus-trade-product | ProductApplication | 8082 | 商品、收藏、留言 |
| 3 | campus-trade-order | OrderApplication | 8083 | 下单、取消、订单查询 |
| 4 | campus-trade-payment | PaymentApplication | 8084 | 支付宝沙箱支付 |
| 5 | campus-trade-gateway | GatewayApplication | 8080 | 网关路由、JWT 鉴权 |

建议最后启动网关 `GatewayApplication`，这样网关启动后可以通过 Nacos 发现前面的业务服务。

### 4. 确认服务注册

打开 Nacos：

```text
http://localhost:8848/nacos
```

正常情况下可以看到这些服务：

```text
campus-trade-user
campus-trade-product
campus-trade-order
campus-trade-payment
campus-trade-gateway
```

## 六、导入示例数据

如果首页没有商品，可以执行示例数据脚本。

第一次全新数据库建议顺序如下：

1. 执行 `01-init-databases.sql` 创建数据库。
2. 启动后端服务一次，让 JPA 自动创建表。
3. 执行示例商品和管理员信息脚本。

命令如下：

```powershell
mysql -uroot -p你的MySQL密码 < init-sql\02-sample-products.sql
mysql -uroot -p你的MySQL密码 < init-sql\03-fix-admin-user.sql
```

示例商品图片位于：

```text
campus-trade-frontend\assets\products\real
```

## 七、启动前端页面

前端是静态页面，使用 Python 启动一个本地静态服务器即可。

在 IDEA 底部 Terminal 或 PowerShell 执行：

```powershell
cd "D:\intellijIDEAworkSpace\ideaprojects\Campus secondhand trading\campus-trade-frontend"
python -m http.server 3000
```

浏览器打开：

```text
http://localhost:3000/index.html
```

注意：`http://localhost:8080` 是后端网关地址，不是网站首页。直接打开 `8080` 出现白标 404 页面是正常现象，因为网关只负责接口转发。

## 八、常用账号

演示时可以使用以下账号：

```text
管理员账号: admin
密码: 123456

普通买家账号: zhangsan
密码: 123456
```

建议演示方式：

1. 用 `zhangsan` 登录，浏览商品并购买商品。
2. 支付完成后查看“我的订单”，订单状态应变为“已支付”。
3. 用 `admin` 登录，查看“我卖的订单”和“我的商品”。

## 九、支付宝沙箱说明

支付宝沙箱配置在：

```text
campus-trade-payment\src\main\resources\application.yml
```

本地演示时，支付宝异步通知通常无法直接访问 `localhost`。项目已经做了适配：支付完成跳回订单页后，前端会调用支付服务主动查询支付宝交易状态，并同步本地订单状态。

支付成功后的链路：

```text
支付宝沙箱支付成功
-> 跳回 http://localhost:8080/api/payment/return
-> 网关放行同步回调
-> 跳转到 http://localhost:3000/my-orders.html
-> 前端主动调用 /api/payment/sync
-> 支付服务查询支付宝交易状态
-> RabbitMQ 发送 payment.done
-> 订单服务将订单改为 PAID
-> 商品服务将商品改为 SOLD
```

## 十、演示检查清单

演示前可以按下面顺序检查：

1. Docker Desktop 已启动。
2. Redis、RabbitMQ、Nacos 已启动。
3. MySQL 本机服务已启动，密码已在环境变量中正确配置。
4. IDEA 中 5 个 Spring Boot 服务全部启动成功。
5. Nacos 中能看到 5 个服务实例。
6. 前端静态服务器 `3000` 已启动。
7. 浏览器打开 `http://localhost:3000/index.html`。
8. 商品列表能正常显示。
9. 登录、发布商品、购买、订单状态都能正常操作。

## 十一、常见问题

### 1. 打开 `localhost:8080` 显示白标错误页面

这是正常的。`8080` 是网关接口地址，不是前端首页。

网站首页请打开：

```text
http://localhost:3000/index.html
```

### 2. 登录显示请求失败

通常是后端服务没启动完整，或者网关没有在 Nacos 中发现用户服务。

检查：

```text
campus-trade-user 是否启动
campus-trade-gateway 是否启动
Nacos 是否能看到 campus-trade-user
MYSQL_PASSWORD 是否配置正确
```

### 3. 发布商品提示缺少 `X-User-Id`

通常是没有通过网关访问，或者没有登录。

正确方式：

```text
前端页面访问 3000
前端接口请求 8080 网关
不要直接访问 8081、8082、8083、8084 的业务接口
```

### 4. 支付后订单短时间仍显示待支付

先刷新“我的订单”页面。项目已经增加支付状态主动同步逻辑，正常情况下几秒内会变成“已支付”。

如果仍不变化，检查：

```text
campus-trade-payment 是否启动
campus-trade-order 是否启动
RabbitMQ 是否启动
支付宝沙箱密钥是否匹配
```

### 5. Docker 报 `dockerDesktopLinuxEngine` 找不到

说明 Docker Desktop 没有启动，先打开 Docker Desktop，等待状态变为 Running 后再执行 `docker-compose` 命令。

### 6. 端口被占用

本项目默认端口：

```text
前端: 3000
网关: 8080
用户服务: 8081
商品服务: 8082
订单服务: 8083
支付服务: 8084
MySQL: 3306
Redis: 6379
Nacos: 8848
RabbitMQ: 5672, 15672
```

如果端口被占用，需要先关闭占用端口的程序，或者修改对应服务的 `application.yml`。

## 十二、接口快速验证

商品列表接口，无需登录：

```text
http://localhost:8080/api/products
```

如果浏览器打开该地址能看到 JSON 数据，说明网关和商品服务正常。

登录接口通过前端页面使用即可：

```text
http://localhost:3000/login.html
```

## 十三、课程设计可讲的分布式点

- Spring Cloud Gateway 统一网关和 JWT 鉴权
- Nacos 服务注册与发现
- OpenFeign 服务间调用
- Redis 缓存、登录黑名单、订单分布式锁
- RabbitMQ 异步消息处理订单和支付状态
- MySQL 多数据库拆分，每个服务独立持久化
- 支付宝沙箱支付集成
- 前后端分离，前端通过网关调用后端接口

## 十四、一键启动和关闭

为了课堂演示更快，项目根目录提供了一键脚本。

### 1. 平时快速启动

双击：

```text
start-all.bat
```

或者在项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-all.ps1
```

该脚本会自动执行：

```text
1. 关闭旧的 3000、8080、8081、8082、8083、8084 端口进程
2. 启动 Docker 中的 Redis、RabbitMQ、Nacos
3. 设置 MySQL、Redis、RabbitMQ、Nacos 环境变量
4. 依次启动 user、product、order、payment、gateway 五个后端服务
5. 启动前端静态服务器 http://localhost:3000/index.html
```

### 2. 修改后端代码后启动

如果刚改过 Java 后端代码，需要重新打包后再启动，双击：

```text
start-all-build.bat
```

或者执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Build
```

这个方式会先执行：

```powershell
mvn -q -DskipTests package
```

然后再启动所有服务。

### 3. 一键关闭

双击：

```text
stop-all.bat
```

或者执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-all.ps1
```

该脚本会关闭：

```text
前端 3000
后端 8080、8081、8082、8083、8084
Docker 中的 Redis、RabbitMQ、Nacos
```

注意：脚本不会关闭本机 MySQL，因为重新启动项目还需要使用 MySQL。

### 4. 查看运行状态

执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\status.ps1
```

可以查看前端、后端端口和 Docker 容器是否已经启动。
