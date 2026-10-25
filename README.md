# FolderLocker

FolderLocker 是一个使用 Java 21 和 Swing 编写的 Windows 文件夹访问控制工具。它允许把多个文件夹保存为目标，在应用内使用密码切换目标的锁定状态，并在解锁成功后用系统文件管理器打开目标目录。

> **重要：这不是文件系统加密工具。** “锁定”通过 Windows ACL 拒绝当前用户对目录及其子项的读取和执行权限；拥有管理员权限或其他账户仍可能修改权限并访问目录。

## 功能

- 添加文件夹目标：名称、路径、密码和自动上锁方式。路径支持 `D:\xxx` 和 `"D:\xxx"` 两种输入形式，程序会自动去除成对首尾引号。
- 在列表中显示每个目标的 `LOCKED` 或 `UNLOCKED` 状态。
- 输入密码后移除当前用户的拒绝 ACL，并在成功时打开目录。
- 手动锁定、删除目标。
- 保存窗口大小、目标路径、状态等设置。
- 锁定时使用 `icacls` 设置当前 Windows 用户的目录访问控制；程序启动时会重新应用已保存的锁定状态。
- 可选择“保持解锁”或“程序退出时自动上锁”。当前版本不具备监控资源管理器窗口关闭的能力，因此不能在用户关闭资源管理器窗口的瞬间自动上锁。
- 使用临时文件和原子替换保存配置，降低写入中断导致配置损坏的风险。

## 环境要求

- Java Development Kit (JDK) 21 或更高版本。
- Windows 桌面环境；打开文件夹依赖 `java.awt.Desktop`。
- 项目没有外部运行时依赖。仓库包含 Maven 配置，但也支持直接使用 `javac` 编译。

## 使用 Maven 运行

在项目根目录 `FolderLocker` 中执行：

```powershell
mvn compile
mvn exec:java
```

Maven 会根据 [pom.xml](pom.xml) 使用 `com.folderlocker.Main` 作为入口类。

## 不使用 Maven 运行

```powershell
New-Item -ItemType Directory -Force out
$sources = Get-ChildItem src\main\java -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d out $sources
java -cp out com.folderlocker.Main
```

应用启动后：

1. 点击“添加”，填写显示名称、现有文件夹路径和密码。
2. 在列表中选择目标，点击“解锁/打开”并输入密码。
3. 验证成功后，应用将目标状态改为 `UNLOCKED` 并请求系统打开文件夹。
4. 点击“锁定”会通过 Windows ACL 禁止当前用户访问目录；点击“删除”只会从应用配置中移除目标。
5. 添加目标时可选择“程序退出时自动上锁”；程序退出时会锁定所有已解锁且启用该选项的目标。

应用从当前工作目录的 `config/folder-locker.json` 读取配置。请从项目根目录启动，否则相对路径可能指向其他位置。首次运行或配置文件不存在时，程序会使用默认配置；首次添加或执行其他变更后会创建配置文件。

首次运行时，程序会让用户选择快捷方式保存目录，并在该目录生成 `FolderLocker.lnk`。快捷方式会直接启动当前 Java 类路径中的程序，之后可直接双击运行，不需要再次执行 PowerShell 编译命令。快捷方式创建位置会记录在 `config/folder-locker.shortcut`；删除该标记文件后，下次启动可以重新选择位置。快捷方式功能仅支持 Windows。

## 运行 Smoke Test

测试会在临时目录中验证配置读写、错误/正确密码、解锁和锁定状态：

```powershell
New-Item -ItemType Directory -Force out, test-out
$mainSources = Get-ChildItem src\main\java -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d out $mainSources
javac -encoding UTF-8 -cp out -d test-out src\test\java\com\folderlocker\SmokeTest.java
java -cp "out;test-out" com.folderlocker.SmokeTest
```

使用 Maven 时，当前项目将测试类作为可直接运行的 smoke test；也可以先执行 `mvn test` 检查项目是否能正常编译。

## 配置文件

默认配置文件为 [config/folder-locker.json](config/folder-locker.json)，结构如下。完整字段示例另见 [config/folder-locker.example.json](config/folder-locker.example.json)；该文件只是参考模板，程序不会自动读取它。

| 字段 | 说明 |
| --- | --- |
| `version` | 配置格式版本，目前为 `1`。 |
| `settings.windowWidth` | 密码窗口和主窗口使用的宽度，默认 `400`。 |
| `settings.windowHeight` | 密码窗口和主窗口使用的高度，默认 `300`。 |
| `targets` | 文件夹目标数组。 |
| `targets[].id` | 目标唯一标识，由程序生成，不建议手动修改。 |
| `targets[].name` | 列表中显示的目标名称。 |
| `targets[].path` | Windows 文件夹路径，例如 `C:\\Users\\Alice\\Documents`。 |
| `targets[].password` | 当前版本保存的明文密码。 |
| `targets[].state` | `LOCKED` 或 `UNLOCKED`。 |
| `targets[].autoLock` | `KEEP_UNLOCKED` 或 `LOCK_ON_APPLICATION_EXIT`。旧配置缺少此字段时默认为 `KEEP_UNLOCKED`。 |

JSON 标准本身不支持 `//` 或 `/* ... */` 注释，而项目使用轻量级正则解析器，因此运行配置中的说明使用 JSON 合法的 `_comment` 字段表示注释。程序会忽略未知字段；不要把运行配置中的示例对象补成完整目标，否则可能被识别为真实目标。程序保存配置时会重写文件并移除手工添加的说明字段。

## 项目结构

```text
FolderLocker/
├─ config/folder-locker.json       # 运行时配置
├─ src/main/java/com/folderlocker/
│  ├─ Main.java                     # Swing 窗口和用户操作
│  ├─ application/                 # 解锁、锁定业务流程
│  ├─ config/                      # 应用配置模型
│  ├─ domain/                      # 文件夹目标和锁定状态
│  ├─ persistence/                 # JSON 配置读写
│  ├─ platform/                    # Windows 文件夹打开逻辑
│  └─ ui/                          # 密码和目标编辑对话框
└─ src/test/java/.../SmokeTest.java # 基础流程测试
```

## 安全限制

当前版本会在 JSON 中保存明文密码，任何能读取配置文件的人都可以看到密码。锁定依赖当前用户拥有修改目录 ACL 的权限，不等同于加密保护；管理员、目录所有者或其他有权限的账户仍可能恢复访问。

如果要用于生产环境，应至少：

- 使用密码派生函数保存校验值，而不是保存明文密码，并增加安全的随机盐和参数配置。
- 使用 Windows 凭据管理器或其他系统安全存储保护密钥材料。
- 接入成熟的加密容器/加密卷方案，并明确处理挂载、卸载、权限和异常恢复。
- 对配置文件和目标路径增加权限、格式及错误处理校验。
