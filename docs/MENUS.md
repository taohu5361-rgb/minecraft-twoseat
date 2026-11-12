# 0.2.3 独立游戏菜单

Minecraft 1.20.1 / Forge 47.3.33 / Java17；保留一个JVM和同一集成服务器中的两名真实玩家。

## 行为

- Esc始终给P1，手柄菜单键Start给P2；P2键盘后备用F10，手柄B返回。两套菜单分别绘制、处理输入，并可同时存在。
- 菜单中的玩家停止自己的移动/转向/攻击输入，另一方与世界继续运行；与原版联机一样，角色仍处在运行的世界中。
- 子界面（例如选项）也不触发分屏全局暂停。显示/资源设置本来共用一个窗口，因此设置按钮明确标注影响双方；成就和统计使用各自连接/玩家的数据。
- P2离开只断开并保存P2；P1结束会话正常保存双方并回到标题界面。退出动作延迟到P1上下文的安全位置执行，避免在Guest的finally恢复期间销毁上下文。
- P2离开后，已经打开的分屏菜单继续非暂停以完成断开清理；重新打开原版单人Esc菜单后恢复正常单人暂停。没有开放LAN、端口或改动身份验证。

## 根因与修复

旧版只把菜单画到各自半屏，Minecraft.runTick仍按单人世界和Screen.isPauseScreen计算全局pause，Minecraft.pauseGame还会暂停共享音频。P1开菜单后，客户端世界/实体、P2输入与集成服务器都停止。

分屏期间改用SeatPauseScreen，在runTick的Screen暂停判断处限定分屏不暂停；拦截pauseGame以避免全局声音暂停。ControllerSeat显式路由Pause绑定给P2，KeyboardInputMixin阻止P1菜单内的残留移动。Esc保留给P1、F10给P2；共享鼠标菜单事件按左右位置选择拥有者，P1瞄准时保持鼠标归P1。游戏/子菜单继续复用原版和Controlify界面处理。

## 实测

新增tests/controller/menu_regression.py，通过原版KeyboardHandler入口送入Esc/F10，通过真实Controlify绑定送入Start/B与摇杆，使用原MouseHandler入口点击左右按钮。修复前P1菜单使paused=true、服务器时间不前进且P2不能移动；修复后验证P1菜单与子选项中的P2移动/转向、P2菜单中的P1移动、菜单所有者输入停止、双菜单独立返回、键盘归属、左右点击归属、P2单独离开、原版单人暂停恢复和P1正常结束世界。

证据在docs/evidence/0.2.3；普通安装probe默认关闭。旧版天气和8/8模拟、个人UUID存档、内置Controlify/YACL及许可声明继续保留。帧率报告是0.2.2固定场景测试，本版不冒充重新测过整套FPS。

最终正式JAR还通过首次手柄接入与原17项Controlify移动/转向/攻击挖掘/放置/背包/隔离回归。截图：[P1菜单与P2游戏](screenshots/0.2.3-p1-menu-p2-playing.png)、[两侧独立菜单](screenshots/0.2.3-independent-menus.png)。
