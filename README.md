# JourneyMap Quick Teleport - GTNH

为 GTNH 的 JourneyMap 全屏地图添加**右键菜单传送**，体验与高版本 JourneyMap 一致：在地图上点右键，选择“传送到此处”即可。

## 功能

在全屏地图（默认 `J` 键）任意位置点击鼠标右键，弹出菜单：

- **传送到此处**：直接执行 `/tp` 并自动关闭地图，不会弹出聊天框。
- **新建导航点**：打开 JourneyMap 的导航点编辑器，坐标自动填入。

菜单标题显示鼠标所指的坐标；指向导航点时显示导航点名称，并传送到该导航点的高度。

按 `Esc`、点击菜单外区域或再次右键可关闭菜单。

> 传送使用 `/tp` 指令，需要你在当前世界/服务器拥有该指令的权限。

## 安装

1. 前置：GTNH 2.9.x（Minecraft 1.7.10），已安装 `journeymap-1.7.10-5.2.23-unlimited`。
2. 从 [Releases](../../releases) 下载 `journeymap-quick-teleport-GTNH-<版本>.jar`。
3. 放入客户端 `mods` 目录，重启游戏。

仅需客户端安装，不修改 JourneyMap 本体，卸载时删除该 JAR 即可。

## 构建（开发者）

复制 `paths.example.json` 为 `paths.local.json`，填入本机的 JDK、服务端、启动器库和客户端目录（也可使用环境变量 `JMQTP_JDK`、`JMQTP_SERVER_ROOT`、`JMQTP_LIBRARY_ROOT`、`JMQTP_CLIENT_ROOT`），然后：

```powershell
.\build.ps1
py -3 .\tests\run_tests.py
```

## 许可证

LGPL-3.0，详见 [LICENSE](LICENSE)。本模组依赖 JourneyMap，不包含其代码。
