# 云端源码审查入口

0.2.4新增指定Neo Voxy版本的双视角兼容。先读[VOXY.md](VOXY.md)和[分档报告](BENCHMARK-0.2.4.md)，再审查`VoxyCallbacks`与三个可选Mixin：重点检查共享更新通知、renderer退出时的监听生命周期、在途回调及未安装Voxy时的可选性。Voxy实现/二进制受ARR限制，均未公开或嵌入。

当前版本为TwoSeat0.2.4-alpha，保留Minecraft1.20.1、Forge47.3.33、Java17单进程双人分屏，并集成Controlify Forgified2.1.9。先读[手柄集成与验收](CONTROLIFY.md)及[LGPL与源码声明](../THIRD-PARTY.md)，再检查下方基础架构。长期需求不等于已实现功能。

## 建议阅读顺序

1. 根目录 README：安装、操作、实现范围和已知限制。
2. `PHASE1.md`：用户已确认的架构决定、候选项目许可与第一阶段边界。
3. `VERIFICATION.md`：实际运行结果，区分开发客户端和正式 Forge 运行时。
4. `../mod/src/main/java/local/twoseat/`：实际实现。
5. `evidence/` 和 `screenshots/`：精选测试状态与原始游戏截图；完整本机日志没有上传。

## 核心代码位置

| 文件 | 职责 |
| --- | --- |
| `TwoSeat.java` | Forge 模组入口，仅在客户端注册会话事件 |
| `SplitSession.java` | 本地玩家连接、第二玩家数据包队列、上下文切换、双视图绘制、输入与会话生命周期 |
| `ClientContext.java` | 保存和恢复当前玩家、世界视图、相机、GUI 与交互状态 |
| `ParticleState.java` | 每视图活动粒子状态，提供器与纹理仍共用 |
| `WeatherProbe.java` / `SimulationProbe.java` | 默认关闭的天气、生物、服务端模拟与帧时间证据采集 |
| `SecondInput.java` | 玩家二基础键盘输入 |
| `mixin/` | 数据包接收、渲染、窗口尺寸、键盘鼠标与原版私有成员的必要挂钩 |
| `DevProbe.java` | 默认关闭的本地测试驱动；只有显式设置 `twoseat.probe=true` 时启用 |

## 可以重点审查的内容

- 第二连接的数据包是否始终在正确的客户端上下文处理，异常时主玩家状态是否可靠恢复。
- 加入、离开、保存退出、重新进入以及渲染资源释放是否完整。
- 两套玩家状态、交互目标、背包和 GUI 是否存在串用。
- 半屏渲染尺寸与辅助帧缓冲是否一致，窗口尺寸变化是否正确处理。
- 私有成员访问、Mixin refmap 和正式 SRG 环境是否匹配。
- 默认关闭的测试驱动是否有需要进一步收紧的边界。

Java 17 下可在 `mod` 目录执行 `./gradlew build`。云端无图形环境时可以做源码审查和构建；实际游戏交互、显示效果和原生图形稳定性仍需要真实客户端验证。一次已记录的 `d3d11.dll` 原生崩溃原因未定，不应把后续未复现解读为已经根治。

本仓库已按用户最新要求公开，源码、审查文档及 Release 附件可匿名访问。让 Chat 审查时，可提供仓库链接和本文件路径；实际读取仍取决于该聊天是否具备网页或 GitHub 读取工具。
