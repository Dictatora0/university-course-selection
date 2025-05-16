# 在线选课系统

这是一个基于Java Servlet和MySQL的简单在线选课系统。

## 功能

- 学生登录与注册
- 浏览课程
- 选择课程
- 退选课程
- 查看已选课程
- （可能包含其他功能，根据实际项目情况补充）

## 技术栈

- **后端**: Java Servlet
- **前端**: HTML, CSS, JavaScript
- **数据库**: MySQL
- **构建/部署**: Apache Tomcat, Shell脚本
- **依赖管理**: 手动管理JAR包 (gson, mysql-connector-j, javax.servlet-api)

## 环境准备

在开始之前，请确保你已经安装并配置了以下环境：

1.  **Java Development Kit (JDK)**: 版本 8 或更高版本 (推荐 JDK 11+)。
    -   可以通过运行 `java -version` 来检查版本。
2.  **Apache Tomcat**: 版本 9.x (本项目配置路径为 `/opt/homebrew/opt/tomcat@9/libexec`)。
    -   如果你的Tomcat安装在其他路径，请修改 `deploy.sh` 脚本中的 `TOMCAT_HOME` 变量。
3.  **MySQL数据库**: 版本 5.7 或更高版本。
    -   确保MySQL服务正在运行。
4.  **MySQL JDBC驱动**: `mysql-connector-j-*.jar`。
    -   脚本会自动尝试从IntelliJ IDEA的驱动目录复制，如果找不到，请手动下载并放置到项目的 `lib/` 目录下。
5.  **命令行工具**: 如 `bash`, `curl`, `lsof` (macOS/Linux环境下通常自带)。

## 项目配置与启动

### 1. 克隆项目 (如果尚未克隆)

```bash
git clone <repository_url>
cd <project_directory>
```

### 2. 配置数据库

-   **创建数据库**: 运行 `db_setup.sql` 脚本来创建名为 `course_selection` 的数据库和必要的表结构。
    ```bash
    mysql -u your_mysql_user -p < db_setup.sql
    ```
    (根据你的MySQL用户名替换 `your_mysql_user`，然后输入密码)

-   **配置数据库连接**: 编辑 `config.properties` 文件，确保以下参数正确：
    ```properties
    jdbc.url=jdbc:mysql://localhost:3306/course_selection?useSSL=false&serverTimezone=UTC
    jdbc.username=your_mysql_user
    jdbc.password=your_mysql_password
    ```
    将 `your_mysql_user` 和 `your_mysql_password` 替换为你的MySQL凭据。
    项目中默认用户是`root`，密码是`12345678`，如果你的MySQL配置不同，请务必修改此文件。

### 3. 准备依赖

-   确保以下JAR包存在于 `lib/` 目录下：
    -   `gson-*.jar` (用于JSON处理)
    -   `javax.servlet-api-*.jar` (Servlet API)
    -   `mysql-connector-j-*.jar` (MySQL JDBC驱动)
-   `deploy.sh` 脚本会尝试从IntelliJ的默认驱动路径复制MySQL驱动。如果失败，你需要手动下载并放入`lib/`目录。

### 4. (可选) 导入课程数据

-   项目包含一个 `course.csv` 文件，其中包含示例课程数据。
-   `deploy.sh` 脚本会自动将此文件复制到部署的Web应用中。
-   登录系统后，学生可以通过特定功能（如果实现）导入这些课程，或者应用启动时自动导入（取决于 `StudentServlet` 的 `importCourses` 方法是否被调用）。

### 5. 修改Tomcat路径 (如果需要)

-   打开 `deploy.sh` 脚本。
-   找到 `TOMCAT_HOME` 变量，默认为 `/opt/homebrew/opt/tomcat@9/libexec`。
-   如果你的Tomcat安装在不同的路径，请修改此变量的值为你的Tomcat安装目录的 `libexec` (或类似) 子目录。

### 6. 运行部署脚本

-   在项目的根目录下，给予 `deploy.sh` 执行权限并运行它：
    ```bash
    chmod +x deploy.sh
    ./deploy.sh
    ```
-   此脚本会自动执行以下操作：
    -   清理旧的编译文件。
    -   创建必要的目录结构。
    -   复制资源文件 (`course.csv`, `config.properties`)。
    -   编译Java源代码。
    -   复制编译后的类文件和库文件到WEB-INF目录。
    -   **尝试停止任何正在运行的Tomcat实例并释放占用的端口** (默认为 8081/8006，如果被占用会尝试切换到 8081/8006 等)。
    -   创建WAR文件 (`course-selection.war`)。
    -   将WAR文件部署到Tomcat的 `webapps` 目录。
    -   启动Tomcat服务。
    -   检测Tomcat是否成功启动。

### 7. 访问应用程序

-   脚本执行成功后，会在控制台输出访问地址。
-   通常情况下，访问地址为：`http://localhost:8081/course-selection/`
-   如果默认端口被占用，脚本会自动切换到其他端口，并会在输出中提示新的访问地址。

## 故障排除

-   **端口占用**: `deploy.sh` 脚本会尝试自动处理。如果持续失败，请手动检查并终止占用端口 (如8080, 8081, 8005, 8006) 的进程。
    ```bash
    lsof -i tcp:<port_number>
    kill -9 <PID>
    ```
-   **编译错误**: 查看 `compile_errors.log` 文件获取详细错误信息。
-   **Tomcat启动失败**: 查看Tomcat的日志文件，通常位于 `$TOMCAT_HOME/logs/catalina.out`。
-   **数据库连接失败**: 
    -   确认MySQL服务正在运行。
    -   检查 `config.properties` 中的数据库URL、用户名和密码是否正确。
    -   确认 `mysql-connector-j-*.jar` 在 `web/WEB-INF/lib/` 目录下，并且Tomcat能够加载它。
-   **注册/登录或其他功能失败**: 
    -   打开浏览器的开发者工具 (通常按F12)，查看控制台(Console)和网络(Network)选项卡，检查是否有前端错误或API请求失败。
    -   查看Tomcat的 `catalina.out` 日志，寻找与Servlet相关的错误或异常信息，特别是 `StudentServlet`、`CourseServlet` 等。
    -   检查 `StudentDAO` 等数据访问对象的日志输出，确认SQL语句是否正确执行。

## 项目结构 (主要目录和文件)

```
├── lib/                     # 存放外部JAR依赖
├── src/
│   ├── dao/                 # 数据访问对象 (DAO)
│   ├── model/               # 数据模型 (POJO)
│   ├── servlet/             # Servlets 控制器
│   └── util/                # 工具类 (DBConnection, PasswordUtil等)
├── web/
│   ├── css/
│   ├── img/
│   ├── js/
│   ├── WEB-INF/
│   │   ├── classes/         # 编译后的Java类文件 (由脚本自动生成)
│   │   ├── lib/             # Web应用的库文件 (由脚本自动复制)
│   │   └── web.xml          # Web应用部署描述符
│   └── *.html               # HTML页面
├── .gitignore               # Git忽略文件配置
├── config.properties        # 数据库和应用配置
├── course.csv               # 示例课程数据
├── db_setup.sql             # 数据库表结构创建脚本
├── deploy.sh                # 部署和启动脚本
└── README.md                # 本文件
```

祝你使用愉快！ 