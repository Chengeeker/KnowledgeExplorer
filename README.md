# 万象探险家 · Knowledge Explorer

一款面向小学阶段的离线百科探索与答题游戏，使用 **Kotlin + Jetpack Compose** 构建，支持 Android 手机与平板。

## 功能

- **科普小课堂**：翻阅问题、插画与详细解析，可用中文语音朗读。
- **知识大闯关**：四选一随机答题，答后查看解析，结束后回顾成绩与错题。
- **知识对决**：支持平板横屏双人同屏或单人与 AI 对战，提供「速度优先」与「正确率优先」两种规则。速度优先中先答对者获得该题分数，双方能看到抢答结果提示。
- **统一题库筛选**：可选择年级、知识领域、细分主题、原始题型与每轮题数；三个玩法共用设置。
- **声音与反馈**：可在设置中开启全局汉字拼音（逐字标注声调，默认关闭）、中文 TTS、互动音效、背景音乐与可选震动；平板可以切换缓冲 PCM 朗读和系统直接朗读。
- **响应式布局**：平板横屏分栏及独立滚动题卡，双人对决时双方分坐平板两端。

## 独立粤语乐园

- **粤语词语宝典**：12 个生活主题、180 个词语，支持粤拼（Jyutping 1～6 声调数字）显示、简繁切换、词卡学习记录和家长录音示范。
- **粤语小耳朵**：听内置离线粤语音频选择对应图片；保留可选的粤语语音引擎和家长示范录音，不会用普通话冒充粤语。
- **点心小掌柜**：体验广式茶楼点单，按图文或可用的粤语录音选择点心。
- **跟我讲粤语**：42 句短语加基础词汇的自由跟读、录音回听，不进行容易误判的儿童语音自动评分。
- **更多场景游戏**：「粤普变变变」「生活寻宝」「对话小剧场」将词义、听音、生活情境与粤语表达结合；平板首页四种大玩法采用等宽 2×2 布局。
- **离线粤语发音**：安装包已内置用开源 canto-tts 模型批量制作的 232 条粤语音频（180 个词、42 句短语及 10 句茶楼点单）。无需家长逐条录音，也不依赖设备内置粤语引擎；家长音频可选，设置中还能选择优先使用设备提供的真正粤语 TTS（如果可用），无可用音色则退回内置音频。内置语音是 AI 合成素材，已处理异常长静音、统一响度并核对粤拼，不等于由母语者逐条听审。生成脚本见 `question_bank/build_cantonese_audio.py`，模型许可证及出处见资源清单。
- **独立粤语音量**：粤语乐园首页「学习小设置」提供 0～200% 发音音量滑块，默认 150%，可试听并记住选择。100% 以上对离线语音/录音使用 Android 音频增强效果，实际增强能力随设备而异；不改变普通话、音乐、答题音效或系统媒体音量。
- **隐私**：可选的录音只保存在本地应用私有存储，退出学习区后清理临时练习录音；按需申请麦克风权限。

粤语语料保存在 `question_bank/source/cantonese_beginner.psv` 和
`question_bank/source/cantonese_expanded.psv`，由
`question_bank/build_cantonese_bank.py` 合并生成离线 JSON。内容是首版自主整理的广州话启蒙素材，不能视作《广州话正音字典》的完整复制或规范读音已经逐条专家审定。粤拼与普通话拼音互相独立。

## 题库

题库内置 **1370 道四选一题目**，无需在线加载，覆盖 12 个知识领域。

| 年级 | 题数 |
| --- | ---: |
| 一年级 | 265 |
| 二年级 | 248 |
| 三年级 | 236 |
| 四年级 | 221 |
| 五、六年级 | 每年级 200 |

「语文文学」领域共有 **253 道**，一年级有「古诗启蒙」65 道，二年级有「古诗阅读」48 道。选择某个年级时只会抽取该年级的题，不会自动切换到高年级题目；选择「全部年级」则允许跨年级探索。

挂图上的 40 首古诗均有专门练习（每首新增 3 道，共新增 120 道），题源见 `question_bank/source/poster_poetry.psv`。题目来源和构建方式参见 `question_bank/`。古诗题以低年级适龄练习为定位，**不保证与每所学校的教材版本和教学进度逐篇一致**；内容仍需要持续审核。

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

调试包输出在 `app/build/outputs/apk/debug/`。如需重新生成题库（包括低年级古诗、挂图古诗及粤语词语数据），运行：

```powershell
py .\question_bank\convert_bank.py
```

`question_bank/source/` 保留原始 Excel、低年级古诗及 40 首挂图古诗的题源文件；`question_bank/merge_poetry_source.py` 和 `question_bank/merge_poster_poetry.py` 会在转换后合并新增古诗题，避免被原始 Excel 覆盖。

正式签名使用 `scripts/sign-release.ps1` 与本地 `key.properties`、原签名密钥。**签名材料不在仓库中**，新环境不能仅凭源码生成与现有安装包同签名的 Release。发布前可用 `scripts/verify_release.py` 进行 APK、题库和测试检查。

## 项目结构

- `app/src/main/`：Compose 界面、游戏逻辑、音频播放、资源
- `app/src/test/`：JVM 自动化单元测试
- `question_bank/`：原始题库、补充题、数据转换及审核脚本
- `scripts/`：构建辅助、回归检查与签名验证

## 版本更新

每次发布的更新说明与签名 APK 请查看 [GitHub Releases](https://github.com/Chengeeker/KnowledgeExplorer/releases)。较早版本的详细历史记录统一放在 [CHANGELOG.md](CHANGELOG.md)。
