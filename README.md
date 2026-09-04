# Web Shell

一个基于 Spring Boot 的 Web 端 SSH 远程终端工具。

> 无互联网环境可离线运行，所有前端资源已本地化。

---

## ✨ 功能特性

- 🌐 **Web SSH 终端** - 浏览器直接远程连接 Linux 服务器，xterm.js 全真终端体验
- 📁 **SFTP 文件管理** - 可视化目录树 + 文件列表，支持上传、下载、删除、重命名
- 🔐 **AES 加密传输** - 前端密码 AES 加密，端到端安全
- 📴 **离线运行** - Bootstrap / xterm.js 等资源本地化，无需外网
- ⚡ **EhCache 会话缓存** - WebSocket 和 HTTP 请求共享登录状态

---

## 🛠️ 技术栈

| 分类       | 技术                                | 版本     |
|------------|-------------------------------------|----------|
| 后端框架   | Spring Boot                         | 2.5.4    |
| Web 框架   | Spring MVC + WebSocket              | 5.3.9    |
| SSH 客户端 | JSch (com.github.mwiede)            | 0.2.18   |
| 缓存       | EhCache                             | 2.10.9.2 |
| 前端终端   | xterm.js                            | 4.x      |
| UI 框架    | Bootstrap 5                         | 5.3.3    |
| 图标       | Bootstrap Icons                     | 1.11.3   |
| 模板引擎   | Thymeleaf                           | 3.0.12   |
| JSON 处理  | Jackson                             | 2.12.4   |
| 日志       | Log4j2                              | 2.14.1   |
| 工具库     | Lombok / Commons-Lang3 / Commons-IO | -        |

---

## 📦 项目结构

```
web-shell/
├── pom.xml
├── README.md
├── docs/                           # 打赏图片
│   ├── wechat-pay.png
│   └── alipay-pay.png
└── src/main/
    ├── java/com/github/wz/web/shell/
    │   ├── WebShellApplication.java          # 启动类
    │   ├── Constants.java                    # 常量定义
    │   ├── config/                           # 配置类
    │   │   ├── CorsConfig.java               # 跨域配置
    │   │   ├── WebMvcConfig.java             # MVC 配置
    │   │   └── WebSocketConfig.java          # WebSocket 配置
    │   ├── controller/                       # 控制器
    │   │   ├── RouterController.java         # 路由 (首页/SFTP页)
    │   │   ├── SftpController.java           # SFTP 文件操作 API
    │   │   └── ElectronController.java       # Electron 支持
    │   ├── handler/                          # 处理器
    │   │   ├── WebShellWebSocketHandler.java # WebSocket 消息处理
    │   │   └── ApplicationStartup.java        # 启动钩子
    │   ├── interceptor/                      # 拦截器
    │   │   └── WebSocketInterceptor.java
    │   ├── listener/                         # 事件监听
    │   │   └── ApplicationEventListener.java
    │   ├── service/                          # 业务层
    │   │   └── WebShellService.java          # SSH 连接/命令转发
    │   ├── utils/                            # 工具类
    │   │   ├── SecretUtils.java              # AES 加解密
    │   │   ├── EhCacheUtils.java              # 会话缓存
    │   │   ├── SftpUtils.java                # SFTP 操作封装
    │   │   ├── ThreadPoolUtils.java          # 线程池
    │   │   └── ...
    │   └── vo/                               # 值对象
    │       ├── WebShellData.java             # 前端请求数据
    │       ├── ShellConnectInfo.java          # SSH 连接信息
    │       └── ...
    └── resources/
        ├── application.yml                    # 主配置 (端口9999)
        ├── ehcache.xml                       # EhCache 配置
        ├── log4j2.xml                        # 日志配置
        ├── banner.txt                        # 启动 Banner
        ├── static/                           # 静态资源 (已本地化)
        │   ├── css/bootstrap/                 # Bootstrap 5.3.3
        │   ├── css/bootstrap-icons/           # Bootstrap Icons
        │   ├── js/bootstrap/                  # Bootstrap JS
        │   ├── fonts/                         # woff2 字体文件
        │   ├── js/xterm.js                    # xterm 终端
        │   ├── js/jquery/                     # jQuery
        │   └── img/                           # 图标图片
        └── templates/
            ├── index.html                     # 登录+终端页
            └── sftp.html                      # SFTP 文件管理页
```

---

## 🚀 快速开始

### 环境要求

- JDK 8+
- Maven 3.5+
- 首次构建需要联网下载依赖

### 编译打包

```bash
# 联网环境 (首次)
mvn clean package -DskipTests

# 离线环境 (依赖已缓存)
mvn clean package -DskipTests -o
```

打包产物：`target/web-shell-1.0.0.jar`（Fat Jar，自带所有依赖）

### 启动

```bash
java -jar target/web-shell-1.0.0.jar
```

浏览器访问：`http://localhost:9999`

- 登录 → 输入主机/端口/用户名/密码 → 进入 Web SSH 终端
- 点击顶部 **SFTP 文件管理** 按钮 → 新标签页打开文件管理界面

---

## 🔐 安全机制

- **密码加密**：前端使用 AES/ECB/PKCS7 加密密码后传输，后端解密
- **SSH Host Key 检查**：已关闭（`StrictHostKeyChecking=no`），首次连接不弹窗确认
- **会话管理**：EhCache 缓存 SSH 连接信息，WebSocket 和 SFTP 页面共享登录态
- **线程池隔离**：每个 SSH 连接独立线程池处理，互不影响

---

## ⚙️ 配置说明

### application.yml 关键配置

```yaml
server:
  port: 9999                     # 服务端口
spring:
  mvc:
    static-path-pattern: /static/**  # 静态资源 URL 前缀
    view:
      prefix: classpath:/templates/
      suffix: .html
  web:
    resources:
      static-locations: classpath:/static/
```

---

## 📴 离线部署

项目已完成前端资源本地化，打包后的 Fat Jar 可在完全无互联网环境运行：

```bash
# 1. 在联网机器上打包
mvn clean package -DskipTests

# 2. 拷贝 jar 到目标机器
scp target/web-shell-1.0.0.jar user@offline-server:/opt/

# 3. 离线启动
java -jar /opt/web-shell-1.0.0.jar
```

已本地化资源清单：

- ✅ Bootstrap 5.3.3 CSS + JS
- ✅ Bootstrap Icons CSS + woff2 字体
- ✅ xterm.js 终端组件
- ✅ jQuery
- ✅ jstree 文件树

---

## ❤️ 支持项目

如果这个项目对你有帮助，欢迎打赏支持开发者持续维护！

<div align="center">

|             微信支付             |             支付宝             |
|:--------------------------------:|:------------------------------:|
| ![微信支付](docs/wechat-pay.png) | ![支付宝](docs/alipay-pay.png) |

</div>

---

## 📄 License

MIT License