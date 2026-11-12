# Controlify Forgified 2.1.9 集成与验收

适用TwoSeat0.2.3-alpha：Minecraft1.20.1 / Forge47.3.33 / Java17。

保留单JVM、唯一集成服务器、两名真实玩家和两套视图。Controlify和YACL作为可替换的内置Jar-in-Jar模块分发，玩家只放一个TwoSeat JAR；复用驱动、映射、绑定、死区、曲线、配置界面和虚拟光标。

| 实现 | 处理的单玩家假设 |
| --- | --- |
| ControllerSeat / SecondInput | 将活动手柄给P2，复用ControllerPlayerMovement和转向处理，基本动作交给SplitSession |
| ControlifySeatMixin | 取消全局tick，改在P2上下文调用；禁止全局输入模式切换和按键重置 |
| ControllerSubmissionMixin / ControlifySeatMixin | 禁止设备数据提交页与提交请求；不自动排入设置向导，避免首次连接阻断P2，保留手动校准/映射 |
| ControllerSeat | 只接管标准手柄或已映射接口，避免同一设备的零输入原始接口覆盖XInput |
| ControllerMovementMixin | 禁止DualInput覆盖P1/P2输入对象，保留P1键鼠和P2独立输入 |
| SeatKeys / KeyMappingAccess | 分开保存held与clickCount，finally恢复P1，隔离手柄按键模拟 |
| ClientContext / MouseAccess | P2始终独立MouseHandler，无第二套GLFW回调；设备在tick中接入也不借用P1鼠标 |
| ClientContext / MinecraftAccess | 两套ToastComponent，成就和配方提示随接收玩家的上下文渲染 |
| VirtualMouseSeatMixin / WindowMixin | 复用光标插值/吸附/屏幕处理器，拦截全局光标移动/隐藏，逻辑坐标对应右半屏 |
| ControllerInGameMixin / ControllerGuideMixin | 转向与提示限定P2，基本动作显式路由，避免全局HUD/视角/暂停副作用 |

原版类与Controlify类使用各自remap边界。构建任务prepareEmbeddedModules禁用上游一个含无效remappable Shadow的可选配方书吸附Mixin，为YACL补minVersion；普通槽位吸附和物品操作保留。原始归档不覆盖，资源修改可从mod/build.gradle复现。

## 验证

独立正式forgeclient从mods加载重混淆JAR，mods只有一个TwoSeat文件；内置模块与SDL3正常加载。早期运行中SDL3实际识别飞智APEX5/XInput两个接口；设备未连接时也可正常启动并退回P2键盘。

17项自动断言：P2单独移动、P1单独移动、P2单独转向、攻击/使用按键隔离、仅打开P2背包、独立鼠标对象、虚拟光标不动P1光标、P2开背包时P1仍移动、手柄关闭背包、生存放置、持续挖掘、攻击另一真实玩家、背包拿起/放回物品、同时独立背包、断开后键盘回退。结果和精选状态在docs/evidence/controller。

ControllerProbe仅在-Dtwoseat.probe=true时可用。它通过原始InputComponent.pushState输入轴/按钮，再经过原绑定、死区、曲线、按键输出和屏幕处理器操作真实服务器玩家；只替代硬件状态来源，不证明USB/蓝牙/震动体验。复测先把两人分开，防止原版实体碰撞推动静止玩家而误判串输入。

测试脚本在tests/controller，需要另行准备纯净正式运行环境、以probe属性启动隔离TwoSeat测试世界，通过TWOSEAT_TEST_GAME指定游戏目录。不要向普通用户存档运行服务器测试命令。

真人实体手柄全流程、不同设备/蓝牙组合、震动、跨维度、死亡重生、复杂容器及长期稳定性未完整验收。已排除没有明确再分发许可的可选steamdeck4j库，并用LGPL补丁禁用Steam Deck CEF增强入口；普通SDL/GLFW不变。源码开发启动也已验证双玩家；其上游资源配置仍可能打印可选Mixin诊断，正式打包已作资源修补。v0.1.0一次d3d11原生崩溃仍未定因，不宣称根治。安装与按键见README，LGPL及对应源码见THIRD-PARTY.md。

## 0.2.1 回归补充

用户实体试玩暴露了0.2.0自动回归未覆盖的首次连接流程：原测试设备接入会清理向导，因而漏掉了真正设备接入时的提交页面死锁。新增 `controller_first_use.py` 走真实 CONTROLLER_CONNECTED 事件，修复前确实停在 SubmitUnknownControllerScreen，修复后无阻塞页面且P2移动不影响P1。没有把未校准设备伪标记为已校准。

APEX5实际枚举出标准XInput（10轴、21按钮）和零输入原始接口；旧版会选择后者。修复根据Controlify的标准手柄/映射能力选择接口，不按厂商名写特例。自动输入测试仍不代替实体手柄的人工作用验证。

新增 `player_progress.py` 对两个真实玩家分别设置经验、物品、装备、末影箱、重生点、配方和成就；检查客户端管理器和两个方向的成就提示隔离，再在整个JVM退出重启后比较各自个人数据及UUID文件。运行方法为：设好TWOSEAT_TEST_GAME和TWOSEAT_TEST_OUTPUT，执行seed；正常保存退出并重启同一测试世界、加入P2后执行verify。此测试只用于可丢弃的TwoSeat测试世界。


## 0.2.2 天气、生物与模拟

本版补齐P2客户端动画/粒子更新，与Controlify输入路由并行；不更改手柄驱动或映射。天气、生物、粒子隔离、8/8服务端模拟及帧率证据见[BENCHMARK-0.2.2.md](BENCHMARK-0.2.2.md)。
