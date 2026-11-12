# 0.2.0-alpha 验证更新

2026-11-12：本版新增内置Controlify手柄与P2上下文路由。正式Forge客户端验证输入隔离、世界交互、背包取放及键盘回退，记录见[CONTROLIFY.md](CONTROLIFY.md)及docs/evidence/controller。真人实体手柄全流程尚未验收。以下为0.1.0历史证据，不将旧版的无手柄范围当成本版限制。

# 第一阶段验证记录

验证日期：2026-11-12，北京时间。目标为 Minecraft 1.20.1、Forge 47.3.33、Java 17；测试实例只有 Forge 与 TwoSeat，使用独立 `TwoSeat-Probe` 存档。

## 结果

| 项目 | 实际结果与证据 |
| --- | --- |
| 构建 / 正式命名映射 | `tools/build-final.log` 构建与 reobfJar 成功；JAR 含 MixinConfigs、refmap、mods.toml、MIT 许可 |
| 正式 JAR 安装运行 | Forge 官方安装器在 `tools/production` 安装 47.3.33；最终 JAR 放入 mods，以 `forgeclient`（非 userdev）启动，PID 37876，两玩家、分屏、独立移动 / 转向与正常保存退出通过；`tools/production-run.log`、production-players / production-movement JSON 为证 |
| 单进程真实双玩家 | run-03 游戏 PID 35500；两个实体均为 `net.minecraft.server.level.ServerPlayer`，服务器线程是同一 `Server thread` |
| 稳定身份 | Host UUID `10ceda3b-9a9f-3428-98d4-700a86367ee8`，Guest UUID `ddcb043a-db56-33bb-8f82-6344b6e7dfe2`；跨游戏进程及正常退出重进保持一致 |
| 独立移动 | Guest 从 (88.5,150,-28.5) 走到约 (88.5,150,-24.62)，Host 留在 (96.5,150,-24.5)；随后 Host 单独行走，Guest 维持原位，仅有正常停止惯性 |
| 独立视角 | Guest 方向键使 yaw / pitch 约为 46.8 / 46.8，Host Rotation 保持 -180 / 0；最终截图已确认场景中心对准半屏准星 |
| 独立背包 | Host 为 1 stick，Guest 为 64 dirt；Guest 建造后变 62 dirt，挖掘拾取后回到 64 dirt；Host 始终不受影响 |
| 生存交互 | P 放置、U 挖掘；服务器断言分别输出 `TWOSEAT_PLACE_OK` 和 `TWOSEAT_BREAK_OK` |
| 战斗与生命 | Guest 按 U 攻击 Host，Host 20→19 HP 并击退，Guest 保持 20 HP |
| 远距离 | Guest 到约 (597.1,165.0,600.5)，Host 留在 (88.5,150,-23.5)，约 800 格距离；可继续独立行走、加载与绘制地形 |
| 真实键盘与菜单 | Windows 输入验证 F8、O、Esc、菜单焦点和确认。中文输入法会吞字母操作，切英文后 O 成功打开 Guest InventoryScreen |
| 正常存档 | 原版暂停菜单“Save and Quit to Title”正常停止服务器、保存两人数据；同进程重新进入并真实 F8 加入，身份 / 位置 / 背包恢复；最终又正常保存并关闭测试游戏 |

交付目录 `evidence/` 保存精选 JSON，`screenshots/` 保存游戏帧缓冲原始截图。完整构建与过程日志留在本机 `tools/`，不混入源码 ZIP。

## 修复与测试边界

- 修复了 Forge 本地登录通道重复注册导致的 channelActive 竞态、Guest 缺少 blitShader、登录嵌套绘制、半屏辅助帧缓冲尺寸及原版场景偏心问题。
- F8 / O / U / P 增加按下事件处理，按住动作继续由独立输入状态驱动。
- 测试驱动最初直接 clearLevel，遗漏 vanilla 的 level.disconnect，导致等待服务器退出；已修正驱动，并通过真实原版菜单单独验收了退出流程。旧测试进程曾通过任务专用 agent 请求正常服务器保存停止，没有把强杀当作存档验收。
- 一次桌面界面检查期间的原生崩溃发生在 glfwSwapBuffers / d3d11.dll；原因未确定。后续 run-05（Gradle 运行总计 4分38秒）正常退出，未复现。此项仍是已知稳定性限制。
- 未运行完整 JUnit 测试套件（当前没有此类测试）；核心证据来自实际 Minecraft 客户端、集成服务器状态、标准交互和截图。
- 正式 JAR 已通过独立正式 Forge 运行时加载验证，测试启动参数来自官方版本 JSON；没有使用用户原启动器或修改其配置，也没有测试各第三方启动器的专有行为。
- 没有验收手柄、物理设备分离、所有容器鼠标拖拽、跨维度、死亡重生、整合包、LOD、光影和长时间运行，也不作性能提升承诺。
