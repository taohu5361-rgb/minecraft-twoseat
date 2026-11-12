# 0.2.3-alpha 独立菜单

用户要求一侧Esc/手柄菜单不暂停另一方。Minecraft全局pause和pauseGame声音暂停是原缺口；本版SeatPauseScreen按玩家分开，runTick的Screen暂停判断在分屏时关闭，保持单人原版行为。输入路由明确Esc给P1、F10/Start给P2，Mouse按半屏归属；P1菜单内KeyboardInput归零，P2菜单不处理角色转向。退出请求延迟回P1上下文执行，P2只离开自己，P1保存结束双方。P2离开后旧菜单非暂停，避免刚变为单人就冻结连接断开清理。说明与回归见MENUS.md；原本共享物理鼠标的键盘后备仍由P1游戏瞄准优先。

以下保留历史。

# 0.2.2-alpha 天气与模拟修复

用户反馈P1拉远后P2雨线反复上下跳动，要求8区块加载/8区块模拟及帧率测试，并追加验证生物。根因已通过真实客户端复现：P2漏掉LevelRenderer.tick，10秒内P1渲染时钟前进200刻/P2为0，P2动画时间回退200次。补齐P2渲染器刻、ClientLevel.animateTick和ParticleEngine.tick；ParticleState按上下文分开活动粒子/发射器/待加入队列/组计数，共享生成器、纹理图集与资源。音频继续P1监听。

WeatherProbe/MinecraftMixin以实际全局帧间隔记录FPS与帧时间，在两套真实视图中观测天气及固定UUID生物的动画时间和插值。普通安装不启用probe。SimulationProbe从服务器线程检查两玩家区块范围、实体刻数和熔炉状态；探针区分实体tick资格与方块tick就绪，不能把ServerChunkCache.isPositionTicking误标为实体资格。测试脚本tests/controller/weather_regression.py、simulation_regression.py、frame_benchmark.py。Windows命令文件更新增加短暂占用重试；静止判定允许小于0.001度的浮点尾差。

最新结果和测帧条件见BENCHMARK-0.2.2.md；原17项输入回归及个人进度不因本次模拟修复而取消。以下保留历史。

# 0.2.1-alpha 最新交接

首次设备信息提交页与自动设置向导已停用；标准或已映射接口分配给P2，零输入原始接口不再覆盖XInput。每个客户端上下文独立ToastComponent，修复一人成就提示出现在两屏的问题。原版个人数据继续由真实ServerPlayer按UUID持久化；本次已实测整个JVM重启后的背包/装备、末影箱、经验、位置/方向、重生点、配方、成就和统计独立恢复。精选证据见evidence/0.2.1，测试脚本见tests/controller。0.2.0实体试玩暴露了原自动测试接入时清理向导的漏测，新增首次连接回归不再清理。人工实体手柄全流程仍需用户试玩确认。

以下保留旧版交接历史。

# 0.2.0-alpha 最新交接

2026-11-12：已集成Controlify Forgified2.1.9与YACL，保持单JVM/双玩家。当前实现、构建资源修补、17项正式客户端回归及边界见[CONTROLIFY.md](CONTROLIFY.md)，LGPL对应源码和替换办法见[第三方声明](../THIRD-PARTY.md)。交付与最新状态以README及项目任务日志为准。下方保留0.1.0阶段原始历史。

# 最新开发交接

更新：2026-11-12 12:17（北京时间）。**第一阶段最小闭环已实现并验证，JAR、源码 ZIP 与运行说明均已生成校验；后续记录以 README、docs/VERIFICATION.md 和项目任务日志为准。**

- 最终重混淆 JAR：`dist/twoseat-1.20.1-forge-0.1.0-alpha.jar`，SHA-256 `a742bb6884415ee50638339b605f723036f1113d63b34b58cfdb1666b19bc54d`。
- 已修复下方历史快照中的半屏场景中心偏移，并实际截图复核；两套 LevelRenderer / GameRenderer 的辅助帧缓冲必须随半屏 resize，退出时还原全屏。
- run-05 已验证真实 F8、O、Esc、原版菜单保存退出和重进恢复，最后正常保存并关闭游戏。
- 最终 JAR 已复制进官方 Forge 安装器创建的 `tools/production/mods`，以 **forgeclient** 正式运行时加载，游戏 PID 37876。分屏、标准双 ServerPlayer、独立移动 / 转向、保存与正常退出通过；无 SRG / Mixin 映射错误。正式运行 JAR 与交付 JAR 的 SHA-256 一致。
- 正式安装过程和启动参数辅助脚本都在 tools（不进源码 ZIP）；没有打开或改动用户原 Minecraft / PCL 实例。
- 3577 个游戏资源全部 SHA-1 校验通过。正式安装客户端库也按官方 SHA-1 校验后复用 / 下载。
- 稳定性限制：run-04 在桌面界面验证期间发生一次 `glfwSwapBuffers → d3d11.dll` 原生访问异常，原因未定；后续开发复测和正式运行未复现。不能宣称已根治或长时间稳定。
- 后续只需用户安装 Alpha 试玩反馈；复杂输入、LOD、整合包、光影、跨维度、完整容器与长测仍属于以后阶段。

## 历史开发快照（以下 11:57 状态已被上述结果取代）

更新：2026-11-12 11:57（北京时间）。第一阶段尚在验证，不能宣布全部完成。

- 主工程 `E:\AI\Minecraft-SingleProcess-Splitscreen\mod`，入口 `local.twoseat.TwoSeat`，分屏会话 `SplitSession`。
- 固定 MC 1.20.1、Forge 47.3.33、Java 17；ForgeGradle 6.0.54、MixinGradle 0.7.38、Gradle 8.13。
- 本机 JDK：`E:\AI\Phone-MCP-zh\tools\jdk\jdk-17.0.21.0.2+1`；Gradle：`E:\AI\Phone-MCP-zh\tools\gradle\gradle-8.13\bin\gradle.bat`。
- 构建缓存、日志均在本项目 `tools/`。`GRADLE_USER_HOME` 必须指向 `tools/gradle-cache`，其中有任务专用网络配置，不写到用户全局配置。
- `tools/build-05.log` 首次完整构建成功，生成 30KB 左右重混淆 JAR；其后的代码仍须重新构建，不能交付旧 JAR。
- 3577 个官方游戏资源已全部下载并逐项 SHA-1 验证，`tools/assets-fetch.log` 最终 failed 为空。

## 已实测

运行记录 `tools/run-03.log`，游戏 PID 35500（已正常关闭），一个集成服务器、Host/Guest 两名标准 ServerPlayer，各有独立 UUID、背包、生命、坐标；左右两画面和 HUD 实际显示。默认纯 Forge，无其他玩法/优化模组。

隔离存档：`mod/run/saves/TwoSeat-Probe`；测试过程没有修改用户原 MC 目录或存档。

- 玩家二单独移动约 3.88 格时 Host 原位；Host 单独移动时 Guest 原位（停止瞬间有正常惯性）。
- 玩家二方向键改变 yaw/pitch 至约 46.8；Host Rotation 保持 -180/0。
- Host 背包为 1 stick，Guest 为 64 dirt；Guest 按 P 放置两块后余 62，U 挖掘并拾取后回到 64；服务器输出 TWOSEAT_PLACE_OK / TWOSEAT_BREAK_OK。
- O 打开 Guest 个人背包，已截图，Host HUD/背包仍为木棍。
- U 攻击 Host，Host 20→19 HP，Guest 保持 20 HP，击退由服务器处理。
- Guest 到约 (597,165,600) 后仍能独立行走；Host 留在 (88.5,150,-23.5)，相距约 800 格。
- 证据 JSON：`mod/run/twoseat-evidence/`；截图：`mod/run/screenshots/`。应挑选最终有效证据复制到交付目录，不把旧失败测试当成功。

## 已修问题

- Forge 本地 Connection 自带 activationHandler 注册登录通道，不可再次手工 registerClientLoginChannel，否则与 channelActive 发生竞态。
- 第二 GameRenderer 需共享已加载 shader 表及 blitShader。
- Guest 的 updateScreenAndTick 只 setScreen，避免登录阶段原版嵌套 runTick。
- mainRenderTarget 的 Mixin accessor 需要 @Mutable。

## 当前工作与下一步

1. 刚补上半屏创建时分别 resize 两个 LevelRenderer / GameRenderer，退出时还原全屏，修复先前 3D 画面中心偏到右侧的现象。正在 `tools/run-04.log` 启动复核，必须看新截图确认，不能仅假定 resize 已解决。
2. 新运行开始会按 `mod/run/twoseat-command.json` 的 world 操作载入已有 TwoSeat-Probe。待 Host 登录后发送 keys [297]（F8），然后 keys []；验证 UUID/背包/坐标恢复并截图。若中心偏移仍存在，查 LevelRenderer 的辅助 RenderTarget / GL viewport。
3. `DevProbe` 只有 `-Pprobe` -> `-Dtwoseat.probe=true` 时启用。控制文件 op 支持 world、keys（GLFW 数值键）、hostForward、server、inspect、screenshot、saveQuit、quit、join、leave；每条需唯一 id。证据文件名为 id。可用同目录临时文件+原子替换避免读到半写 JSON。
4. saveQuit 必须先 `mc.level.disconnect()` 再 `mc.clearLevel()`；旧测试驱动漏了 disconnect 而在 clearLevel 等待服务器退出。已修测试驱动。旧进程通过任务专用 agent 请求 TwoSeat 测试服务器正常 halt/save 后关闭，没有强杀；`tools/server-normal-stop.txt`、`run-03.log` 可追溯。这个 agent 不属于交付模组。
5. 修正后再测一次正常 saveQuit→world→F8，检查真正退出重进无挂起；测试菜单基本鼠标操作、玩家一鼠标/玩家二方向键，必要时通过 computer-use Skill 做桌面验证。
6. 最后 build、检查 JAR manifest/refmap/reobf，输出 dist 的 JAR、源码 ZIP、简短中文说明与最终验证记录；源码包排除 tools、references、mod/build、mod/run、.git，绝不打包 MC/Forge 游戏文件或保留全部权利的参考仓库。
7. 完成后追加项目/任务日期日志，更新任务 README 和 `D:\Codex日志\我的电脑\交接手册.md` 索引。

## 实现边界

一个 Minecraft 单例/一个 JVM/唯一集成服务器；两套轻量客户端世界视图与区块缓存、LevelRenderer、GameRenderer、Gui，纹理/模型/资源管理器共享。首版尚未去重客户端区块网格，不宣称已经完成长期方案的资源/性能目标。粒子管理器暂共用，声音统一混音；复杂设备隔离、LOD、光影、整合包均不做。
