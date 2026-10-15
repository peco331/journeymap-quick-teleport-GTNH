# JourneyMap 快速传送扩展 - GTNH

> GTNH (1.7.10) 地图 JourneyMap 的附加模组：为全屏大地图增加右键菜单传送功能。

## JourneyMap 1.7.10 大地图右键传送菜单

适配当前 GTNH 2.9.0-RC-1 客户端的 `journeymap-1.7.10-5.2.23-unlimited.jar`。
只安装客户端，不修改 JourneyMap 原 JAR、地图图片、导航点、服务器权限或服务器模组。
独立 Forge coremod 在加载时为大地图鼠标/键盘事件注入分支。

## 功能特性（复刻高版本 JourneyMap 右键菜单）

- **右键弹出菜单**：在全屏地图（J 键）任意位置按**鼠标右键**，在鼠标所在处弹出菜单。
- **菜单顶部标题**：
  - 若鼠标悬停在已有的导航点上：显示该**导航点名称**。
  - 若悬停在普通方块上：显示该方块精准的世界坐标 `X, Z`。
- **传送到此处**（直接传送，无二次确认）：
  - 点击后**直接执行 `/tp` 命令**并将玩家传送至目标位置，**立即自动关闭全屏地图**。
  - **绝不弹出聊天框要求按回车确认**，完全复刻高版本即点即传的流畅体验。
  - **高度判定（Y 坐标）**：
    - 目标位置有导航点：直接使用该导航点的精确 Y 坐标。
    - 当前已加载区块：读取地表高度并校验站立空间（头脚两格空气）。
    - 洞穴/地下视图、或远处未加载区块：直接使用玩家当前 Y 坐标（地表未加载保底 64），直接传送，绝不弹窗打断。
- **新建导航点**：打开 JourneyMap 官方的导航点编辑器，坐标自动填入鼠标所指方块。
- **关闭菜单**：按 Esc 键、点击菜单外区域、或再次右键，均可立即关闭菜单。
- **聊天保护**：地图聊天框激活时右键不会误开菜单；工具栏按钮上的右键不触发菜单。

## 坐标精准度修复原理

GTNH 2.9 使用了 `lwjgl3ify` 模组，将底层事件系统桥接至 LWJGL3 / SDL2，在部分 DPI 缩放或窗口模式下会导致原生底层事件坐标出现偏差。

本次 1.2.0 版本采用双重坐标解析机制：
1. **优先读取 JourneyMap 内部 `LayerDelegate` 数据流**：直接从每帧负责在全屏地图底部渲染坐标的 `BlockInfoLayer.lastCoord` 和 `WaypointLayer.selected` 提取方块坐标与悬停导航点。屏幕底部显示的是什么坐标，右键菜单中取到的就是什么坐标，分毫不差。
2. **几何视口比例数学映射（兜底）**：若在特殊情况下 `lastCoord` 未就绪，根据 GUI 坐标相对于视口中心的相对比例、结合 `centerBlockX/Z` 与当前地图缩放倍率 `zoom` 精确投影，彻底解决坐标偏移问题。

## 构建与测试

复制 `paths.example.json` 为 `paths.local.json` 并填入本机的 JDK、服务端、启动器库目录与客户端目录(也可用环境变量 `JMQTP_JDK`、`JMQTP_SERVER_ROOT`、`JMQTP_LIBRARY_ROOT`、`JMQTP_CLIENT_ROOT`)。该文件已被 Git 忽略。

```powershell
.\build.ps1
py -3 .\tests\run_tests.py
```

34 项隔离测试通过，包括真实 JourneyMap 5.2.23 字节码转换、ASM 数据流分析、JVM `-Xverify:all` 验证、直接传送指令触发校验、导航点识别、以及几何映射兜底测试。

## 安装与回滚

```powershell
.\install.ps1
```

`install.ps1` 会自动备份 `mods` 目录中旧版扩展至 `_audit-backups\jmquicktp-时间戳`，并安装新版 JAR。

回滚：正常退出客户端，删除 `mods` 中的 `journeymap-quick-teleport-GTNH-1.2.0.jar`，把备份目录中的旧版复制回 `mods` 即可。

## 许可证

LGPL-3.0，详见 [LICENSE](LICENSE)。本模组依赖 JourneyMap，不含其代码。
