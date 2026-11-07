# 当前发布状态（2026-11-07）

**v1.0.0 已发布，GitHub 来源描述文件及补丁包下载均已核实可用。**

- [一键添加到 Morphe](https://morphe.software/add-source?github=kljjh/flyme-lyrics-patches)
- [Release 与补丁包](https://github.com/kljjh/flyme-lyrics-patches/releases/tag/v1.0.0)
- [来源描述文件](https://raw.githubusercontent.com/kljjh/flyme-lyrics-patches/main/patches-bundle.json)
- [成功的构建与发布记录](https://github.com/kljjh/flyme-lyrics-patches/actions/runs/42367219095)

## 已验证

- 通知行为测试：原文保留、重复歌词去重、原生通知被替换后恢复、暂停与空状态清理、播放会话切换、三个开关分别关闭、无关通知隔离、更新失败后重试、不支持系统时跳过。
- 锁定上游版本的源码叠加、重复叠加拒绝、英文和中文 XML 资源解析。
- 完整 Android 编译以及 `.mpp` 内的补丁类、Android DEX、音乐运行扩展、许可和 NOTICE 检查。
- 正式 Release 下载后，SHA-256 与发布校验文件一致。
- 使用 **Morphe Desktop 2.1.1 / Java 21** 实际加载正式发布的 `.mpp`，成功列出 `Flyme status bar lyrics` 和官方 `Third-party lyrics`。
- 公开来源 JSON 的版本和下载地址与正式发布一致。

文件：`flyme-lyrics-patches-1.0.0.mpp`，16,487,207 字节。

```text
SHA-256: f86dd0fe384e6f520e4b40c1e693bb6957f28179ad79e86303e4e443b58f4a89
```

## 尚未验证

没有执行真实 YouTube Music APK 的修补安装，也没有魅族真机。固件是否允许这两个扩展通知标志、具体视觉效果、后台长时间运行需要按 [真机清单](docs/DEVICE_TEST.md) 验证。构建成功和补丁可加载不等于真机验收通过。

本包是独立社区构建，不使用官方 Morphe 签名；首次导入时请核对来源仓库。
