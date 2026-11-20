# 万象探险家 · Knowledge Explorer

一款面向小学阶段的离线百科探索与答题游戏，使用 **Kotlin + Jetpack Compose** 构建，支持 Android 手机与平板。

## 功能

- **科普小课堂**：翻阅问题、插画与详细解析，可用中文语音朗读。
- **知识大闯关**：四选一随机答题，答后查看解析，结束后回顾成绩与错题。
- **知识对决**：支持平板横屏双人同屏或单人与 AI 对战，提供「速度优先」与「正确率优先」两种规则。速度优先中先答对者获得该题分数，双方能看到抢答结果提示。
- **统一题库筛选**：可选择年级、知识领域、细分主题、原始题型与每轮题数；三个玩法共用设置。
- **声音与反馈**：中文 TTS、互动音效、背景音乐与可选震动；平板可以切换缓冲 PCM 朗读和系统直接朗读。
- **响应式布局**：平板横屏分栏及独立滚动题卡，双人对决时双方分坐平板两端。

## 题库

题库内置 **1250 道四选一题目**，无需在线加载，覆盖 12 个知识领域。

| 年级 | 题数 |
| --- | ---: |
| 一年级 | 226 |
| 二年级 | 224 |
| 三至六年级 | 每年级 200 |

「语文文学」领域共有 **133 道**，一年级有「古诗启蒙」26 道，二年级有「古诗阅读」24 道。选择某个年级时只会抽取该年级的题，不会自动切换到高年级题目；选择「全部年级」则允许跨年级探索。

题目来源和构建方式参见 `question_bank/`。古诗题以低年级适龄练习为定位，**不保证与每所学校的教材版本和教学进度逐篇一致**；内容仍需要持续审核。

## 安装与使用

1. 打开 [GitHub Releases](https://github.com/Chengeeker/KnowledgeExplorer/releases)，下载对应版本的 `KnowledgeExplorer-v*-signed.apk`。
2. 在 Android 设备上安装或升级。项目包名为 `com.game.knowledge`；同签名版本可覆盖安装。
3. 从首页进入三种玩法，或在「设置」中调整年级、题目范围、朗读和对决规则。

**设备兼容性**：需要 Android 8.0（API 26）或更新版本。中文朗读依赖设备可用的中文 TTS 引擎；特定机型的音频表现、双人同屏触控和厂商系统兼容性仍需实机验证。

## 开发与构建

项目使用 Android Gradle Plugin、Kotlin 和 Jetpack Compose。请配置 Android SDK 与可用 JDK，然后在工程根目录执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

调试包输出在 `app/build/outputs/apk/debug/`。如需重新生成题库（包括一、二年级追加的古诗题），运行：

```powershell
py .\question_bank\convert_bank.py
```

`question_bank/source/` 保留原始 Excel 及低年级古诗题源文件；`question_bank/merge_poetry_source.py` 会在转换后合并新增古诗题，避免被原始 Excel 覆盖。

正式签名使用 `scripts/sign-release.ps1` 与本地 `key.properties`、原签名密钥。**签名材料不在仓库中**，新环境不能仅凭源码生成与现有安装包同签名的 Release。发布前可用 `scripts/verify_release.py` 进行 APK、题库和测试检查。

## 项目结构

- `app/src/main/`：Compose 界面、游戏逻辑、音频播放、资源
- `app/src/test/`：JVM 自动化单元测试
- `question_bank/`：原始题库、补充题、数据转换及审核脚本
- `scripts/`：构建辅助、回归检查与签名验证

## 版本更新

每次发布的更新说明与签名 APK 请查看 [GitHub Releases](https://github.com/Chengeeker/KnowledgeExplorer/releases)。较早版本的详细历史记录统一放在 [CHANGELOG.md](CHANGELOG.md)。
