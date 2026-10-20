# 贡献指南

先谢谢你的兴趣！这个项目是一个自用工具，欢迎 bug 反馈、文案改进和小的功能 PR。

## 提 Issue 前

1. 先搜一下现有 issue，避免重复
2. Bug 请写清楚：App 版本（设置页最下方「冲了吗 vX.Y」）、手机型号、Android 版本、复现步骤、预期与实际
3. 涉及统计口径的问题，请说明你期望的算法（例如「平均一次」是「天数 ÷ 次数」还是「相邻两次间隔的平均」）

## 开发环境

```bash
git clone https://github.com/<your-name>/chonglema.git
cd chonglema
export ANDROID_HOME=/path/to/Android/Sdk   # 或 local.properties: sdk.dir=
./gradlew assembleDebug
# Windows: gradlew.bat assembleDebug
```

- JDK 21、Android SDK Platform 37 / Build-Tools 37.0.0
- Gradle 用仓库自带的 wrapper（9.8.1），不要改成本机版本
- 没有 Android Studio 也能开发：改完直接 `./gradlew assembleDebug` + `adb install -r`

## 代码约定

- Kotlin + Compose，**不引入新的第三方依赖**（尤其是网络 / 图片 / 统计 SDK）——这个 App 的卖点就是「纯本地、无权限」，新增依赖请在 issue 里先讨论
- 数据层保持手写 `SQLiteOpenHelper` + DAO，不使用 Room / KSP（AGP 9 内置 Kotlin 与 KSP 版本匹配容易踩坑）
- 状态统一走 `MainViewModel` 的 `StateFlow`，屏幕组件尽量无副作用、可预览
- 界面文案用中文，风格跟现有文案保持一致（短、直白、带一点自嘲）
- 日期口径保持统一：一天从 0 点开始，统计都基于传入的 `today`，不要在页面里直接 `LocalDate.now()`

## 提交 PR

1. Fork → 新建分支（`feat/xxx` / `fix/xxx`）
2. 一个 PR 只做一件事，改动尽量小
3. 提交信息用中文或英文都可以，建议格式：`<type>: <一句话>`，例如 `fix: 月热力图最后一行点击偏移`
4. 提交前跑 `./gradlew assembleDebug` 和 `./gradlew assembleRelease`，确保都通过
5. 涉及 UI 的改动请附截图（亮色 + 夜间各一张）
6. **不要提交** `local.properties`、`keystore.properties`、任何 `.jks` / `.keystore`、`build/` 产物、自己手机导出的备份 JSON

## 不接受的方向

- 加入联网同步 / 账号体系 / 云备份（本地优先是这个项目的底线）
- 加入广告、统计、崩溃上报 SDK
- 把 release 改成需要付费或闭源

## 行为准则

保持友善。这个 App 记录的是私密数据，讨论时不要要求别人提供真实的打卡记录或导出文件。
