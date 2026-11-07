# 2.0.0 验证状态

2.0 是搭配官方来源使用的独立魅族附加补丁，包内不包含官方补丁或官方歌词扩展。

- 已通过通知行为测试，包括正常生命周期、异常恢复及不支持魅族标志的系统。
- 已对未修改的官方 2.1.0 扩展 DEX 完成真实方法注入、重复注入拒绝、接口变化拒绝、DEX 往返读写和 Android dexdump 校验。
- 已通过完整 Android 编译及包内容校验；[构建记录](https://github.com/kljjh/flyme-lyrics-patches/actions/runs/42369119482)。
- 使用官方 Morphe Desktop 2.1.1 验证 `.mpp` 可独立加载，只有一个 `Flyme status bar lyrics`；与官方 `.mpp` 一起加载时，两种歌词补丁各出现一次。
- 两个补丁需要在同一次修补中手动勾选；Morphe 尚不支持按远程来源自动选择依赖。
- 尚未验证真实 YT Music APK 的修补安装或魅族真机表现。
