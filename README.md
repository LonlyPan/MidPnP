# MidPnP

基于 **OpenPnP v2.6** 修改的贴片机控制软件，针对 LumenPnP / PhotonFeeder 调整提前送料和手动操作流程。

上游：[openpnp/openpnp](https://github.com/openpnp/openpnp)，基线提交 `5bd404cfc70f34103a3ca0fbb6b50c2b465f407c`。保留上游 GPL v3 许可证及版权声明；上游介绍见 [README.upstream.md](README.upstream.md)。

## 当前版本

- Photon 飞达提供 `Move Before Feed` 和 `Feed After Pick` 选项，参考 RealCorebb 的 faster-lumenpnp-feeder 分支移植到 v2.6。
- 开启提前送料时，任务吸取成功后发送送料命令，不等待机械送料完成才继续运动；下一次吸取前检查送料完成。
- 每次新开始任务询问是否是最后一块电路板。选“是”，各元件最后一次吸取后停止提前送料；选“否”，末颗吸取后仍送料，为下一块板备料。
- 任务开始默认使用已准备好的首颗物料，不额外送料。新任务、或上一轮选择最后一块板后再次开工，用户须自行确保首颗有效。
- 暂停、继续、单步沿用本轮选择；取消贴装或关闭询问窗口会在任务准备运动前停止。
- 元件库和飞达页面的供料并吸取保留“是 / 否 / 取消吸取”备料询问，手动吸取后不自动送料，支持回收后再吸取。
- 修复回收后直接吸取导致回收按钮变灰的状态与刷新问题。
- 原版多吸嘴规划和换吸嘴策略保持原样。

## 下载与运行

从 [Releases](https://github.com/LonlyPan/MidPnP/releases/latest) 下载完整包。Windows 解压后运行 `OpenPnP-Photon/OpenPnP-Photon.exe`，不要只复制 exe。Linux 便携版解压后执行 `sh ./openpnp.sh`，需要带桌面组件的 Java 11 或以上。

Debian / Ubuntu 安装：

```sh
sudo apt install ./openpnp-photon_2.6.0+photon.20261106.5_amd64.deb
```

安装包不会替换用户已有配置。本版通过 92 项相关测试及跨平台打包内容核对，尚未在 Linux 图形界面和实际硬件上验证。

## 构建

安装 JDK 11 或以上以及 Maven，在仓库根目录执行 `mvn -s support/release/maven-settings.xml package`。Windows 应用镜像打包另需带 `jpackage` 的 JDK（推荐 JDK 21）。

本版针对性回归检查：

```sh
mvn -s support/release/maven-settings.xml package -Dtest=PhotonPrimePromptTest,ManualFeedConfirmationTest,PhotonFeederTest,PhotonBusTest,SharedPumpConfigTest,ReferenceJobProcessorRetryTests,GcodeDriverTest
```

打包脚本见 `support/release`。`src/test/resources/config/shared-pump` 为共享气泵回归测试配置样例，含机器坐标，不能直接作为其他机器的运行配置。

## 源码和许可证

发布页提供完整源码 ZIP / tar.gz。使用、修改和分发须遵守 [LICENSE.txt](LICENSE.txt) 中的 GPL v3 条款。
