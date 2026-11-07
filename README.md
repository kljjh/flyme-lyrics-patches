# Flyme Lyrics Patches

为 YouTube Music 增加**魅族状态栏歌词**的独立社区补丁源。基于固定版本的 Morphe 官方补丁构建，不是 Morphe 或魅族官方项目。

歌词完全由官方 **Third-party lyrics** 提供。状态栏使用“在锁屏上显示媒体通知卡片歌词”已经生成的相同文本，包括该功能的格式化结果、翻译和间奏文本。

## 导入与修补

1. 安装兼容当前 `.mpp` 格式的最新版 Morphe App。
2. [点此添加补丁源](https://morphe.software/add-source?github=kljjh/flyme-lyrics-patches)，或在补丁来源中添加 GitHub 仓库 `kljjh/flyme-lyrics-patches`。
3. 选择本来源和**原版、未修补的 YouTube Music APK**。建议先使用 `9.30.53`。
4. 勾选 **Flyme status bar lyrics**，它会依赖同包内的 **Third-party lyrics**。按需要选择同一来源中的 GmsCore support、后台播放等官方补丁。
5. 修补并安装。在 YT Music 的 Morphe → 歌词设置中打开：第三方歌词、在锁屏上显示媒体通知卡片歌词、魅族状态栏歌词。
6. 在 Flyme 系统设置中允许该应用的通知、媒体通知和状态栏歌词（具体名称依系统版本）。

**不要同时勾选本来源和官方来源中的同名补丁。** 本来源包含官方补丁及本项目新增补丁，直接一次修补原版 APK；不要求先用官方来源修补一次。

手动来源地址：

```text
https://raw.githubusercontent.com/kljjh/flyme-lyrics-patches/main/patches-bundle.json
```

发布后也可以从 [Releases](https://github.com/kljjh/flyme-lyrics-patches/releases/latest) 下载 `.mpp` 并在 Morphe App 中导入本地补丁文件。每次发布附带完整的对应源码归档、许可和校验值。本项目不分发 YouTube Music APK。

当前发布状态见 [STATUS.md](STATUS.md)；尚未发布成功时，上述导入入口还不能下载补丁包。

## 行为与限制

- 默认关闭，需手动开启。关闭锁屏歌词也会停止状态栏歌词。
- 复用已有媒体通知，不创建额外通知或后台服务，不需要悬浮窗或通知读取权限。
- 暂停、停止、切歌、歌词尚未匹配、关闭相关设置时清除；恢复播放后继续更新。
- Flyme 的两个专用通知标志都存在且允许反射访问时才启用。不根据品牌名称判断，也不猜测数字常量。
- 没有同步歌词时不自行搜索或伪造歌词；间奏和歌词回退文本以官方锁屏模块为准。
- 上游支持的稳定 APK：`9.30.53`、`9.20.53`、`9.15.51`；实验版本：`9.44.52`、`9.43.51`、`9.42.52`。版本列表来自固定的官方 v2.1.0，不代表本项目已逐版本真机测试。
- 构建和模拟通知测试不能替代魅族真机验证。发布说明会列明实际验证范围。

## 构建

需要 Python 3.11+、Git、JDK 21（Gradle 会按上游配置获取 JDK 17）、Android SDK 36，以及有权读取公开 Morphe GitHub Packages 的凭据。

```sh
git clone https://github.com/kljjh/flyme-lyrics-patches.git
cd flyme-lyrics-patches
git clone https://github.com/MorpheApp/morphe-patches.git work/upstream
git -C work/upstream checkout 8fa2c7a1a832498de8f179748998a3ca87e198eb
python tests/run_tests.py
python scripts/prepare.py work/upstream
# 通过环境变量提供 GITHUB_ACTOR 和 GITHUB_TOKEN，不要提交凭据。
cd work/upstream
./gradlew :patches:buildAndroid --no-daemon
```

Windows 使用 `gradlew.bat`。`prepare.py` 只接受干净、版本精确匹配的上游检出；遇到接口变动立即报错，不做模糊替换。

GitHub Actions 在提交和 PR 上运行测试、编译并检查 `.mpp` 内部的补丁类、Android DEX、音乐扩展和许可文件。推送 `v*` 标签时创建 Release 和来源描述文件。首次发布前，来源描述文件不会指向不存在的构建。

## 真机验收

参见 [测试清单](docs/DEVICE_TEST.md)。反馈请提供机型、Flyme 版本、YT Music 版本、补丁版本，以及锁屏歌词能否正常显示；不要提供账号或令牌。

## 来源与许可

[改动与上游记录](UPSTREAM.md) · [GPLv3](LICENSE) · [附加 NOTICE](NOTICE) · [魅族接口文档](https://open.flyme.cn/docs?id=239)
