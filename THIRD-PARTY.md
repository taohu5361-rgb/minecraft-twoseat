# 第三方代码与重新组合说明

TwoSeat 0.2.0-alpha 包含 Controlify: Forgified 2.1.9 作为内置手柄引擎，并包含它依赖的 YetAnotherConfigLib 3.6.6。安装者只需放入一个 TwoSeat JAR。Forge 模组列表仍会显示这些内置模块的原始名称与作者，以保留身份、资源空间、配置界面及许可证信息。不要另装独立 Controlify 或 YACL 副本。

## LGPL 部分

| 组件 | 来源 | 许可 | 对应源码 |
| --- | --- | --- | --- |
| Controlify: Forgified 2.1.9 | https://github.com/EchoEllet/Controlify | LGPL-3.0-or-later | `vendor/controlify-forgified-2.1.9/release-sources.jar`、`upstream-source-e97d6ec.zip` |
| Controlify 原作 | https://github.com/isXander/Controlify | LGPL-3.0-or-later | 上述 Forgified 源码保留原作包名、作者与声明 |
| YetAnotherConfigLib 3.6.6 | https://github.com/isXander/YetAnotherConfigLib | LGPL-3.0-or-later | `vendor/yacl-3.6.6/upstream-project-source.zip` |
| TwoSeat 的 Controlify 适配代码 | 本仓库 `ControllerSeat.java`、`ControllerProbe.java` 及名称以 `Controlify`、`Controller`、`VirtualMouseSeat` 开头的 Mixin | LGPL-3.0-or-later | 本仓库 Java 源文件，文件头标有 SPDX 许可 |

Controlify 作者：isXander；Forge 非官方移植作者：EchoEllet。YACL 作者：isXander。完整 LGPL 与 GPL 文本在 `licenses/LGPL-3.0.txt` 和 `licenses/GPL-3.0.txt`，同时打入 TwoSeat JAR 的 `META-INF/licenses`。TwoSeat 原有独立代码继续使用根目录 `LICENSE` 中的 MIT 许可；不能把第三方代码解释为仅受 MIT 许可约束。

YACL原始归档和Controlify的分发基底保留在mod/libs。Controlify基底已用scripts/strip-steamdeck.py排除可选steamdeck4j嵌套归档及其Jar-in-Jar条目：在其上游仓库、README和发布POM中未找到明确的再分发许可证，不将Controlify的LGPL许可自动套给该独立库。对应原始发布二进制可从dependencies.json中的上游链接取得，但没有放入本项目的源码包或公开仓库。

输入改造通过TwoSeat适配代码和Mixin实现，包括限定P2上下文、阻止全局模式切换、隔离按键和鼠标、复用移动/转向及背包。vendor/controlify-patches中保留LGPL修改源码：SteamDeckUtil和SteamDeckDriver改为禁用可选CEF增强功能的入口，InGameInputHandler移除对应截图调用；普通SDL/GLFW驱动不改。Gradle单独编译并重混淆这些类，再装入内置Controlify模块，不与TwoSeat外层形成重复包。

prepareEmbeddedModules还禁用上游含无效remappable Shadow的一个可选配方书吸附Mixin，为YACL补minVersion:0.8。普通槽位吸附和物品转移保留。全部修改可从mod/build.gradle及补丁源码复现，结果在mod/build/embedded。Steam Deck专用CEF/Decky增强功能不属于本版交付范围。

来源和校验值在 `vendor/dependencies.json`。Forgified 的发布标签为 `2.1.9-mc1.20.1-forge`，源码提交为 `e97d6ec37cdb558afd10119c120206d41afdf50b`；该标签中的 `gradle.properties` 仍写 2.1.8，发布二进制的模组元数据和发布源码归档对应 2.1.9。重新构建原项目时应显式传 `-Pmod_version=2.1.9`。

YACL 源码归档固定为其 3.6.6 发布前最后的上游提交 `ae66e4c77c13e4e153da5afb9271ed59a359589e`，包含构建配置和多版本源文件；按上游构建方法选择 `1.20.1-forge` 目标。未把更新版本的源码冒充为旧版本源码。

## 替换与调试 LGPL 组件

允许修改内置 LGPL 组件，以及为调试这些修改而逆向工程。源码交付包包含 TwoSeat 完整源码、构建文件、适配代码、依赖二进制和上游源码归档。

- 重建 TwoSeat：Java 17 下执行 `build.ps1`，或在 `mod` 下执行 `./gradlew build`。
- 使用修改后的接口兼容 Controlify：将自己的重混淆 Forge 1.20.1 JAR 替换 `mod/libs/controlify-forgified-2.1.9.jar` 后重建；无需改动驱动或映射实现。
- 也可用 `scripts/replace-embedded.py` 直接替换已构建 TwoSeat 中对应的内置 JAR，避免重新编译 TwoSeat。替换后的模块需保留兼容 API、资源与 Forge 模块身份；不存在签名或联网校验阻止替换。
- YACL 可按同样方式替换。修改版本的稳定性由修改者自行验证，不能沿用本次测试结果。

## 内置依赖

Controlify带有SDL3本地库、libsdl4j、hid4java、MixinExtras；YACL带有TwelveMonkeys ImageIO和Quilt JSON解析库。除前述排除steamdeck4j外，其嵌套归档和元数据保留。额外上游许可证副本在licenses/upstream，原归档中的声明不删除。SDL3：https://github.com/libsdl-org/SDL；libsdl4j：https://github.com/isXander/libsdl4j；MixinExtras：https://github.com/LlamaLad7/MixinExtras；hid4java：https://github.com/gary-rowe/hid4java；TwelveMonkeys：https://github.com/haraldk/TwelveMonkeys；Quilt JSON：https://github.com/QuiltMC/quilt-json5。

本项目没有重新开发手柄本地驱动或默认映射表，没有分发 Minecraft/Forge 游戏本体、JDK、测试存档或个人账号凭据。
