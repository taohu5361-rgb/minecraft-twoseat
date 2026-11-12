# 后续范围更新

用户已明确追加0.2.0的Controlify Forgified2.1.9集成，保留本文件中0.1.0原始阶段范围作为历史。本次新增授权及实现见[CONTROLIFY.md](CONTROLIFY.md)；LOD/光影/整合包仍不在范围。

# 第一阶段范围与长期需求入口

原始需求的 DOCX 和 HTML 原样保存于 `requirements/`，已于 2026-11-12 完整读取。文档内的 AI 交接提示是需求参考，当前用户消息优先。

本阶段固定 Minecraft 1.20.1、Forge 47.3.33、Java 17，只实现一个 JVM、一个集成服务器、两名真实玩家、左右分屏、独立视角和基本移动及交互。长期需求中的 LOD、大型整合包、光影、性能优化、双物理键鼠、跨维度均不属于本次实现范围。

## 已确认的架构决定

2026-11-12 用户明确同意：没有合适的许可开放底座时，用 Forge 官方 MDK 和原版玩家连接机制，补写最小玩家上下文切换、左右渲染及输入层。首版同一键盘两组按键，未来保留设备输入扩展入口。

## 上游核验

- Forge 官方 MDK：直接复用 1.20.1-47.3.33 构建工程、运行配置与 Gradle wrapper。
- Local Multi Player：CurseForge 页面标注 All Rights Reserved，当前支持较新 NeoForge，未获得修改分发许可，未复制其实现。
- Couch Co-op（HaplessGnome/couch-coop，MIT）：README 明确每名玩家单独窗口和完整游戏副本，不能满足单 JVM 目标，未使用其运行架构。
- Splitscreen Support（pcal43/mc-splitscreen，MIT）：窗口布局辅助，需要多个实例，不能作为本次分屏底座。
- MC-Java-Reforged（Usernah/MC-Java-Reforged）：README 明确 All rights reserved，不能作为可发布衍生底座，未复制其实现。

来源：https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.33/forge-1.20.1-47.3.33-mdk.zip

https://www.curseforge.com/minecraft/mc-mods/local-multi-player

https://github.com/HaplessGnome/couch-coop

https://github.com/pcal43/mc-splitscreen

https://github.com/Usernah/MC-Java-Reforged

## 验收原则

构建成功不等于双人成功。须在纯净隔离存档中启动客户端，核对单 JVM、服务器两名玩家、左右不同视角、独立移动与转向、独立物品栏、方块交互及退出重进。未实际验证的项目必须单独标注。
