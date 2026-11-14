# 常联系（changlianxi）

面向低识字老人的极简亲情通讯 Android App。核心交互：一屏一位家人大照片、上下滑动换人、播放家人本人录音、点照片发起联系。

> 公益表达：「想你了，就见一面。」

## 文档

- [产品设计文档 PRD v0.1](docs/常联系-产品设计文档-PRD-v0.1.md)
- [开发规格 SPEC v0.1](docs/常联系-开发规格-SPEC-v0.1.md)
- [Sites 演示设计 DESIGN v0.1](docs/常联系-Sites演示设计-DESIGN-v0.1.md)

配套交付物：`../changlianxi-site/`（Qoder Sites 公开交互演示，比赛提交物）；`../changlianxi-references/`（开源参考，只读隔离，不复制代码）。

## 技术栈

Kotlin + Jetpack Compose（Material 3）+ VerticalPager；ViewModel/StateFlow；本地配置与私有媒体文件；`CallGateway` 接缝 + `DemoCallGateway`（纯本地模拟）/ `RealCallGateway`（真实蜂窝拨号，决策由纯逻辑 `RealCallPlanner` 负责）。无网络权限、无通讯录权限；真实拨号仅用 `CALL_PHONE`（家属可改用零权限的系统拨号盘）。

- `minSdk = 26`（暂定，待目标真机确认）、`compileSdk = targetSdk = 36`
- AGP 8.12.0 / Kotlin 2.2.0 / Gradle 8.14.3 / Compose BOM 2025.06.00

## 环境

- JDK 17：conda 环境 `changlianxi`（`C:\env\anaconda\envs\changlianxi`，java 在 `Library\bin`）
- Android SDK：`D:\Android\Sdk`（见 `local.properties`）
- Gradle 缓存：`D:\Android\gradle-home`（C 盘空间紧张，大件均放 D 盘）

## 构建与测试（Git Bash）

```bash
export JAVA_HOME='C:\env\anaconda\envs\changlianxi\Library'
export GRADLE_USER_HOME='D:\Android\gradle-home'
./gradlew.bat :app:assembleDebug        # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew.bat :app:testDebugUnitTest    # 单元测试
./gradlew.bat :app:lintDebug            # 静态检查
```

安装到设备：`D:\Android\Sdk\platform-tools\adb.exe install -r app/build/outputs/apk/debug/app-debug.apk`

## 当前状态

- T1：最小工程可构建，`assembleDebug` 成功。
- T2（已完成，模拟器实测通过）：本地联系人模型与家属配置。
  - 家属引导：首启欢迎页 → 创建 PIN（PBKDF2-SHA256 12 万次迭代，4–6 位，5 次错误锁 5 分钟，不落明文）。
  - 联系人编辑：系统照片选择器（免权限）、录音（RECORD_AUDIO 运行时授权、<800ms 拒绝、≤60s）、导入音频、试听、设为默认、排序、删除（默认删除后自动顺延）。
  - 存储：JSON 配置临时文件+原子改名、损坏自动备份；媒体暂存→提交两阶段，文件名绑定联系人 id，照片/语音不错位。
  - 老人首页：全屏默认联系人照片+大字号称呼；顶部隐区 4 秒长按进家属 PIN 验证；退后台自动锁定家属页。
  - 验证：JVM 单元测试 39/39 通过；模拟器全流程实测（`tools/verify_family.py`、`tools/verify_record.py`），3 位联系人持久化、强杀重启恢复、照片/音频 id 绑定、录音替换为真实 AAC 均通过；截图证据 `screenshots/t2-01`…`t2-31`。
- T3（已完成，模拟器实测通过）：老人主页 VerticalPager 一页一人、滑动稳定检测、两种拨号模式。
  - 一页一人：`VerticalPager` + `rememberPagerState`，虚拟页取模映射联系人实现**循环衔接**（末位下滑回首位、首位上滑回末位；单人停住不循环）；`settledPage`/`isScrollInProgress` 判定停稳，停稳才允许拨号；家属改序后按 `contactId` 恢复当前目标。
  - 两种模式：点照片拨打（TAP_PHOTO，整页可点+电话图标）/ 独立大按钮拨打（LARGE_BUTTON，点照片不拨、仅绿色「打电话」大按钮拨）。
  - 模拟呼叫页：暗化照片+「模拟演示」徽标+「正在呼叫 {称呼}」+「不会真实拨出电话」+红色取消（AC-11 诚实标注）；`CallGateway` 接缝 + `DemoCallGateway` 纯本地，`requestId` 幂等、双击只产生一次呼叫、呼叫中锁定联系人。
  - 排序/删除后按 `contactId` 恢复当前目标；单联系人界面稳定。
  - 验证：JVM 单元测试 39/39 通过；模拟器全流程实测（`tools/verify_pager.py`）——一轮 20 次上下滑动循环衔接不错位/误拨、首末互wrap、快速连滑只停最终页、两种模式拨号、双击单次呼叫、排序恢复、单联系人 6 次滑动稳定均通过；截图证据 `screenshots/t3-01`…`t3-07`。
- T4（已完成，模拟器实测通过）：停稳播放本人录音、音频焦点与生命周期、状态声音。
  - 停稳播放：页面停稳延迟 300ms 播放该联系人本人录音；换页立即取消旧待播任务并停播，快速连滑只播最终停住页；单一 `VoicePlayer`（世代号 token）保证不叠音；尊重 `autoPlayVoice` 开关；仅播已提交录音（`playbackVoice`），缺失返回 null 不崩溃。
  - 音频焦点：播放持 `AUDIOFOCUS_GAIN_TRANSIENT`（USAGE_MEDIA/SPEECH），失焦即停，stop/退后台放弃焦点。
  - 状态声音：`StatusVoice` 系统中文 TTS，固定短句（`StatusPhrases`：请求/没联网/没打出去，事件一一对应，绝不把请求说成已接通）；无可用 TTS 静默降级（界面文字仍在），不崩溃。
  - 生命周期：退后台（onStop）立即停录音+停状态声+取消待播；回前台按 id 恢复不崩溃。
  - 验证：JVM 单元测试 43/43 通过（新增 `StatusPhrasesTest`、`MediaFiles.playbackVoice`）；模拟器实测（`tools/verify_audio.py`，以 logcat 音频焦点事件为证）——停稳请求焦点播放、10 次快速连滑无叠音且只播最终页、HOME 退后台立即放弃焦点不再播放、拨号状态声路径无崩溃、缺录音不崩溃均通过；截图 `screenshots/t4-01`…`t4-02`。
- T5（已完成，模拟器实测通过）：DemoCallGateway 呼叫状态机与模拟来电／失败回退。
  - 状态机：纯逻辑 `CallStateMachine`（`CallPhase`：Idle/Preparing/DemoRinging/DemoConnected/IncomingDemo/Failed/Finished），保持「请求发起 ≠ 已拨通 ≠ 已接听」语义；每次迁移须匹配 requestId 与源状态，过期／重复事件拒绝。
  - 防重复呼叫：仅 Idle 接受拨打，双击只一个 requestId（AC-09）；呼叫全程锁定 contactId（AC-10）；挂断只执行一次（AC-13）。
  - 演示分支：拨号→响铃→「模拟对方接听」→接通→挂断；「模拟无人接听」／「模拟断网失败」→失败页（正确语音+「返回」回原联系人，不声称已接通，AC-17）；家属页「模拟来电（演示）」→来电页点照片接听／红色拒接（两触控区独立，AC-12）。
  - 诚实标注：所有呼叫/来电/接通/失败页均带「模拟演示」徽标与「非真实通话」说明（AC-11）；`DemoCallGateway` 纯本地，不产生真实网络呼叫。
  - 验证：JVM 单元测试 55/55 通过（新增 `CallStateMachineTest` 12 项覆盖幂等/过期事件/各分支）；模拟器实测（`tools/verify_call.py`）——拨号接听挂断、双击单次、无人接听回退、断网回退、响铃取消、来电接听挂断、来电拒接均通过；截图 `screenshots/t5-01`…`t5-05`。
- T6（自动化检查已完成，真机／老人试用待硬件）：lint 0 错误；单元测试 74/74；`assembleDebug` 产出 APK 10.7MB；合并清单 `RECORD_AUDIO` + `CALL_PHONE`（电话硬件声明可选，AC-26 见验证表）。验证表见下。
- T7（真实蜂窝拨号 · P1 回退；纯逻辑/构建已验证，模拟器交棒验证见下表，真机拨通待硬件）：把「微信视频较难」时的最基础回退落地——老人上下滑动选家人、点照片拨**真实蜂窝电话**，呼出后由家人微信视频打回来。
  - 号码按联系人录入（家属页手机号字段，留空=纯演示）；拨号方式是**全局设置** `PhoneDialMethod`：`DIRECT_CALL` 一键直拨（`ACTION_CALL`，需 `CALL_PHONE`，家属选定该项时才申请权限）/ `SYSTEM_DIALER` 系统拨号盘（`ACTION_DIAL`，零权限，需再按拨打键）。
  - `RealCallPlanner` 纯逻辑失败关闭：号码不可拨（位数/字符非法）→拒绝 `MISSING_BINDING`；`DIRECT_CALL` 未授权→静默退回 `ACTION_DIAL`，绝不报错阻断老人；`Demo` 目标仍走模拟，`WeChatVideo` 仍拒绝（未实现）。
  - `RealCallGateway` 发拨号 Intent 交棒系统电话；状态机新增 `Handoff` 相（已交系统、App 不可观测接通/挂断，**绝不假装已接通**）；诚实交棒页 `RealCallScreen`（暗化照片+「正在拨打」+「已交给系统电话」+返回，无「模拟」徽标、无假接听/挂断控件）；退后台（onStop）自动复位回老人首页。
  - 隐私：号码仅存本机私有配置、绑定 `contactId`，**绝不进日志/仓库/公开演示**；网页演示保持纯模拟（虚构号码，不真拨）。
- 微信真实视频（P1）、远程配置（P3）、自有 RTC（P4）均未启动，单独验证。

## 验证表（T6）

| 类别 | 项目 | 状态 / 证据 |
|---|---|---|
| 已验证 | 构建与静态检查 | `assembleDebug` APK 10.7MB；`lintDebug` 0 错误 |
| 已验证 | 单元测试 | 74/74（排序/默认/删除/迁移/PIN/媒体绑定/状态机/requestId 幂等/状态短句/真实拨号 planner/号码配置往返/旧配置兼容） |
| 已验证 | 权限最小化（AC-26） | 合并清单 `RECORD_AUDIO` + `CALL_PHONE`（真实拨号专用、老人点击才触发；电话硬件 `required=false`）；无网络/通讯录/相机/无障碍/位置；`CALL_PHONE` 未授权时静默退回零权限 `ACTION_DIAL` |
| 已验证 | 家属配置全流程（T2） | `verify_family.py`/`verify_record.py`，持久化+强杀恢复+id 绑定 |
| 已验证 | 老人首页滑动（T3） | `verify_pager.py`，20 次滑动循环衔接、首末互wrap、两种模式、单联系人 |
| 已验证 | 停稳录音与音频焦点（T4） | `verify_audio.py`，logcat 焦点事件为证，无叠音/退后台停播 |
| 已验证 | 呼叫状态机各分支（T5） | `verify_call.py`，幂等/锁定/接听/无人接听/断网/来电/拒接 |
| 已验证 | 真实拨号纯逻辑（T7） | `RealCallPlannerTest` 8 项：Demo 保持模拟、非法号码失败关闭、未授权退回 `ACTION_DIAL`、`SYSTEM_DIALER` 零权限、号码归一化；`CallStateMachineTest` Handoff 相 4 项 |
| 已验证 | 真实拨号模拟器交棒（T7） | 播种 `Phone("5556")`+授权后点照片：logcat `act=android.intent.action.CALL dat=tel:xxxx`（号码系统自动脱敏）、系统 `UserCallActivity` 接管、telecom 生成去电（MO）；诚实交棒页与「返回」回老人首页截图 `screenshots/t7-realcall-*.png`；App 无崩溃 |
| 尚未验证 | 真实拨号真机拨通（T7） | 需真机+真 SIM（并入任务 #7）；模拟器仅验证交棒，5556 无对端故不能接通 |
| 尚未验证 | 红米真机试用 | 待用户插 USB 开调试（任务 #7） |
| 尚未验证 | HyperOS 关怀模式布局／无障碍开启路径 | 待真机（任务 #7） |
| 尚未验证 | 老人使用测试（识别/误触/耗时） | 需真实老人参与者与知情同意 |
| 尚未验证 | Compose UI 自动化（androidTest） | 暂以 uiautomator 脚本替代，未编写 instrumented 测试 |
| 尚未验证 | 微信真实视频（P1） | 未启动，需单独确认+真机（SPEC §14.1） |
| 已知限制 | 未设号码=纯本地模拟 | 设计如此，全程「模拟演示／非真实通话」诚实标注；设号码后才真实拨号 |
| 已知限制 | 模拟器手势阈值 | 验证脚本统一 300ms 高速度滑动 + ≥1.6s 停稳等待；真机阈值待复核 |
| 已调优 | headless SystemUI ANR／滑动黑帧 | 根因＝AVD 2GB 内存压力（stall SystemUI／telecom，「System UI isn't responding」遮挡输入）＋swiftshader 软件 GPU 合成黑帧；系统进程问题、非 App 缺陷。改 `-memory 4096`＋`-gpu host` 后消除，投屏人工验证滑动／点按正常 |
| 已知限制 | `minSdk=26` 暂定 | 待目标真机确认 |
| 已知限制 | TTS 依赖系统中文语音包 | 缺失时静默降级，界面文字仍在 |
| 已知限制 | 演示素材全虚构 | 公开站点与日志不含真实家属照片/录音/手机号 |

## 模拟器验证工具（不进 APK）

`tools/emu.py`（uiautomator 辅助）、`tools/seed.py`（run-as 播种 3 位演示联系人，PIN 2468；`--phone 5556` 可给第一位联系人预设虚构号码做真实拨号交棒验证，默认不带号码=纯演示）、`tools/verify_family.py`、`tools/verify_record.py`、`tools/verify_pager.py`、`tools/verify_audio.py`、`tools/verify_call.py`。运行：`cd tools && python verify_family.py`（需 MSYS_NO_PATHCONV=1、PYTHONIOENCODING=utf-8）。

> 模拟器输入注意：本 headless AVD（host GPU）上 `input swipe` 慢拖（≥800ms）常被分页器判为未达阈值而不翻页；验证脚本统一用 300ms 高速度滑动 + ≥1.6s 停稳等待，可靠触发单页翻页。

### 交互式投屏验证（scrcpy，不进 APK）

除 uiautomator 脚本外，另用 **scrcpy 5.0.1 原生客户端**把 headless AVD 投屏到桌面，做「像手机一样」的人工交互验证：**拖拽＝滑动换人、单击＝点照片呼叫、右键＝返回、中键＝HOME**。

```bash
# headless AVD：4GB 内存 + host 硬件 GPU（消除 SystemUI ANR 与黑帧），冷启动
emulator -avd changlianxi_test -no-window -no-audio -gpu host -memory 4096 -no-snapshot-load
# 投屏（复用现有 adb；--stay-awake 防休眠黑屏）
ADB=<sdk>/platform-tools/adb.exe scrcpy -s emulator-5554 --no-audio --stay-awake
```

- **选型**：ws-scrcpy 内置 scrcpy-server 1.19 仅支持 ≤Android 12，无法投屏 API 36，故弃用；scrcpy 5.0.1 官方支持 Android 15/16。
- **调优根因**：2GB AVD 预装 Google 应用致内存压力 stall SystemUI（ANR 弹窗遮挡全部输入）；swiftshader 软件 GPU 合成黑帧。4GB＋host GPU 后两者消除，滑动换人／点照片呼叫／返回均人工实测通过。
- 投屏仅作者本地验证用，**不纳入仓库与 APK**。