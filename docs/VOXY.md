# Voxy 双视角实验兼容

目标环境固定Minecraft1.20.1、Forge47.3.33、Java17。0.2.4保持原有单JVM、同一集成服务器、两个真实ServerPlayer和Controlify输入隔离，新增指定版本Voxy的可选远景兼容。

## 被测组合与许可

- TwoSeat0.2.4-alpha，Controlify Forgified2.1.9/YACL3.6.6继续内置。
- Embeddium0.3.31+mc1.20.1，独立安装。
- [Neo Voxy](https://github.com/NHblock-Johnsnow/neo-voxy-multiversion)的Forge1.20.1/Java17源码，固定commit `84ac197d92627aeeea167702757f14bf84c2f26f`，本机版本标识`0.4.0beta-twoseat-local`。
- 不安装Connector或光影。Chunky1.3.146只用于独立世界的地形准备，常规游玩不需要它。

Voxy原作和上述移植分支均标明All Rights Reserved / Do not redistribute。本仓库和公开源码ZIP不包含Voxy实现、二进制或世界数据库；TwoSeat只分发自己的互操作代码。不要把本机实验JAR重新上传。其他Voxy版本/移植分支未验收，不能据此宣称普遍兼容。

本机为保持Java17/Forge47.3.33，使用既有ForgeGradle工具链构建相同渲染核心，将访问加宽声明转换为Forge AT。该工具链开发映射没有暴露Forge新增的StairBlock.getModelState，因此本地构建通过反射调用运行时Forge getter，保留baseState私有，避免破坏Forge自己的字段重定向。纯净实验排除了Create可选集成。原作作者MCRcortex和移植维护者JohnSnow的声明、名称及版权保留。本地构建只用于本机测试，不是公开Voxy发行版。

## 修复的单玩家假设

上游每套VoxyRenderSystem可独立渲染，但共享WorldEngine的地形更新通知只有一个回调，Mapper的生物群系通知也只有一个。第二套渲染器覆盖第一套后，新导入或新变化的远景只更新后注册的一侧。上游关闭任一渲染器时还会把共享通知设为null。

`VoxyCallbacks`为每个共享源保留各renderer的监听，分发更新；三个可选Mixin在构造器处注册，在资源释放前撤销自己，保留另一位玩家的通知。撤销会等待已开始的回调返回，再让上游释放队列和GPU资源。`VoxyCompat`还将距离变化同步到各自renderer，避免设置页只更新当前视角。

共享的是世界地形数据、存储和后台服务。两个视角各有Voxy渲染器、LOD几何/模型图集和可见性状态；本版没有宣称两套GPU网格已经去重。

操作回归另复现了第二个单玩家假设：两套ModelBakerySubsystem各启动一个模型处理线程，却共用MipGen的静态SCRATCH/QUEUE。烘焙透明方块时会数组越界。第四个可选Mixin将这两块临时工作区改为ThreadLocal；没有重写纹理算法或串行化全部烘焙。`lodMipRegression`探针在真实已变换上游类上比较8种透明纹理，两线程各1000次输出与串行参考的8192字节缓冲区逐字节一致；探针只在开发模式启用。

## 距离与数据

近景完整渲染8区块、服务器模拟8区块；LOD按32→64→128区块测试。Voxy内部最高层section宽512格，即32个Minecraft区块，因此这三档的`section_render_distance`分别为`1.0`、`2.0`、`4.0`。数字是每个摄像机的远景范围，不是把服务器模拟提升到128区块。

LOD需要已有地形数据。新世界的未生成区域不会因为拉高距离而自动出现。本次先在独立世界用Chunky生成覆盖两摄像机的区域，正常保存，再通过原生`voxy import current`导入，等待后台网格构建后测量。生成/导入耗时与稳定观景帧率分开报告。

## 实际回归入口

- `tests/lod/staged_distance.py`：环境变量`TWOSEAT_TEST_GAME`指向独立`tools/lod-playtest`，`TWOSEAT_TEST_OUTPUT`为结果目录。按`--stage 32`、人工检查原生截图、`--stage 64`、再`--stage 128`执行；后续档要求前档的`visualVerified`已确认。
- `tests/lod/callback_regression.py`：两个玩家各有约320格外的彩色塔，超出8区块近景；实际修改方块、保存和导入，验证两侧换色与还原。`MarkerPixels.java`用Java ImageIO检查原生游戏PNG，不伪造画面。
- `tests/lod/mip_concurrency.py`：双线程真实纹理处理与串行字节比较；`callback_lifecycle.py`验证退出、重进后的监听和远景更新。
- 状态探针核实两套960×1080 viewport、独立相机/renderer身份、同一个WorldEngine、两份dirty/biome监听及非零LOD几何。
- 每档两次15秒，记录整窗FPS、1%low、p95/p99帧时间、堆内存、服务器平均tick和焦点。不能把仅有配置或几何缓存、但未实际画出的空远景当成验收。

最终结果和图像见[0.2.4分档报告](BENCHMARK-0.2.4.md)。固定场景短测不代表跨维度、光影、整合包或长期压力验证。
