# 常联系｜Android 首期开发规格（SPEC）

> 版本：v0.1  
> 日期：2026-11-10  
> 配套 PRD：[常联系-产品设计文档-PRD-v0.1.md](./常联系-产品设计文档-PRD-v0.1.md)  
> 目标读者：Qoder 编程 Agent、Android 开发者、测试人员  
> 状态：待实施。此文件是工程约束与验收契约，不代表已有代码、APK、测试结果或微信集成。  
> **开发总原则：最小充分实现；真实可验证；不扩大功能范围；不以模拟冒充真实通话。**

---

## 0. Qoder 开发任务说明（必须先读）

### 0.1 项目目标

实现一款名为「常联系」的 Android App，主要帮助低识字老人通过家人照片与本人录音，自主寻找家人并发起联系。**首期先交付可安装、可离线演示、可测试的本地功能**。产品面向真实老人，不是单纯的高保真网页图。

首期 P0 必须交付：

1. Android App 启动后直接进入默认家人的全屏大照片；垂直滑动切换联系人。
2. 照片停稳后播放该家属事先录制的本人声音，换页时停止上一个人的声音。
3. 默认点击照片发起通话请求；可以由家属改为“独立大拨打按钮”模式。
4. 通话流程先通过可替换的**模拟适配器**展示“请求中、模拟呼叫、模拟接通、失败、结束”的状态和语音提示。
5. 首次配置、本机家属设置、联系人 CRUD（增删改查）、排序、默认联系人、照片导入、音频录制／导入、试听、数据持久化。
6. UI 极简，老人首页没有设置、聊天、搜索、导航 Tab 或无关按钮。
7. 关键业务和手势冲突有自动化测试，真实设备可安装试用。

**明确不在 P0 实现**：云端远程同步、第三方登录、RTC、微信真正拨出与微信来电接管、系统桌面替换、AI 语音交互。微信能力进入 P1 的独立 PoC，不得为了它阻断 P0 交付。

### 0.2 首期完成定义（Definition of Done）

- `assembleDebug` 成功，产出实际 APK。
- `testDebugUnitTest` 中与当前范围相关的测试通过。
- 在 Android 模拟器或真机运行关键流程；记录设备系统版本与结果。
- 真机至少验证：照片切换、照片点击、录音播放、照片导入、录音制作、持久化、后台恢复；若某项未测必须明确标注。
- 演示呼叫出现“演示模式”标识，绝不假装真正向微信拨出。
- 日志不含真实家人的照片内容、录音字节、手机号、微信账号或其他敏感数据。
- 安装前家属可以完成配置，老人首次打开不会看到未配置或空白界面。

### 0.3 工作方式（给 Qoder 的约束）

1. 开始编码前读取 PRD、本 SPEC、现有工程、README 与 Gradle 配置；若工程为空则创建最小原生 Android 工程。
2. 按 `T0 → T1 → T2 → T3 → T4 → T5 → T6` 的任务顺序执行；每个阶段先完成与验证，再继续下一阶段。
3. 先 clone 参考仓库到**主工程外的相邻目录**，只研究明确相关实现；不要直接把第三方项目复制进我们的 `app/`。
4. 不因为参考仓库包含天气、SOS、桌面替换、医疗提醒而在本产品加入这些功能。
5. 依赖优先使用 Android 官方和已有 Gradle 依赖；避免无必要的 DI 框架、网络框架、后端、微服务和复杂抽象。
6. 一切“已完成”“已通过”必须来自实际编译／测试／真机结果；如 Android SDK 缺失，报告阻塞与所需环境，不得伪造截图或测试记录。
7. **代码可随阶段推进，功能不能越级扩展。** P1、P3 内容仅按本文进行预研，未经单独指令不要把服务端和无障碍自动化混入 P0。

---

## 1. 仓库参考与本地 clone 指南

### 1.1 优先级与适用范围

| 优先级 | 仓库 | 应研究的模块／能力 | 许可证与限制 | 我们的做法 |
|---|---|---|---|---|
| A | [`bjfwan/yinxing`](https://github.com/bjfwan/yinxing) | 微信适配、页面状态机、异常恢复、通话后返回、适老交互约束 | **仅 v2.3.0 及更早版本源码开放**，旧源码 GPL-3.0-only；新版 APK、文档不能当作新版源码 | 先读 README、DESIGN、STATUS 与 LICENSING；P1 再研究对应旧源码。禁止未经核验复制实现 |
| A | [`francescosorzini-dev/SemplicePhone`](https://github.com/francescosorzini-dev/SemplicePhone) | Kotlin/Compose 适老组件、家属配置、配置版本与可信更新 | Android GPL-3.0-or-later；`tools/remote-server` **AGPL-3.0-or-later** | 研究其页面和数据流；P3 再研究签名同步；不要直接移植整个 portal |
| B | [`UriahShaulMandel/BaldPhone`](https://github.com/UriahShaulMandel/BaldPhone) | 大触控区、首页简化、可读性、联系人快捷操作 | Apache-2.0；项目较旧，依赖和 Android SDK 可能不适合直接复用 | 只参考成熟的人机交互细节，不照搬整个 launcher |
| C | [`tobihille/SeniorLauncher`](https://github.com/tobihille/SeniorLauncher) | 照片快速联系人 | 需再次在本地确认仓库实际许可与代码位置 | 仅比较界面，非首期依赖 |
| P4 备选 | [`livekit/client-sdk-android`](https://github.com/livekit/client-sdk-android) | 自有音视频 SDK、通话控制、WebRTC | Apache-2.0；仅 SDK 不含完整业务通话信令 | P4 才研究，不作为 P0 依赖 |
| P4 备选 | [`jitsi/jitsi-meet-sdk-samples`](https://github.com/jitsi/jitsi-meet-sdk-samples) | Android 视频样例 | 需检查目标仓库内每个模块的许可证 | P4 再评估 |

### 1.2 建议克隆方式（Windows PowerShell）

**推荐目录结构**：主工程与参考仓库互为兄弟目录，避免把 GPL/AGPL 代码、巨型 `.git` 和第三方构建产物纳入自己的项目与 Qoder 上下文。

```text
workspace/
├── changlianxi/                   # 我们的独立工程
│   ├── README.md
│   ├── docs/
│   │   ├── 常联系-产品设计文档-PRD-v0.1.md
│   │   └── 常联系-开发规格-SPEC-v0.1.md
│   └── app/ ...
└── changlianxi-references/        # 研究用途，不提交到 changlianxi
    ├── yinxing/
    ├── SemplicePhone/
    └── BaldPhone/
```

在 `changlianxi` 的**父目录**执行：

```powershell
mkdir changlianxi-references
cd changlianxi-references

git clone https://github.com/bjfwan/yinxing.git
git clone https://github.com/francescosorzini-dev/SemplicePhone.git
git clone https://github.com/UriahShaulMandel/BaldPhone.git

# 记录具体 commit，避免以后参考代码自动变化
git -C ./yinxing rev-parse HEAD
git -C ./SemplicePhone rev-parse HEAD
git -C ./BaldPhone rev-parse HEAD

# 查看银杏是否提供旧源码的 tag / 分支，不要假设 main 就是 v2.3.0 源码
git -C ./yinxing tag -l
git -C ./yinxing branch -a
```

**关于银杏的必要警告**：当前默认分支可能主要是最新文档、诊断镜像与部分目录，**不等于 v2.5.0 完整源码**。需要研究 v2.3.0 旧代码时，在核实仓库确实存在相应 tag／commit 后，再检出该版本；不要猜测 tag 名称，不要从最新版 APK 中臆测源码。

**关于许可证的必要警告**：`git clone` 只是本地获取供查阅，不代表可以任意复制到主工程。若未来实际复用 GPL/AGPL 代码，必须先明确整个衍生作品的许可证义务；尤其 AGPL 远程门户对网络服务提供场景另有源码提供义务。独立重写相同功能时，仍应尊重原作者和第三方资源许可证。以上非法律意见。

### 1.3 对 Qoder 的参考范围限制

只需要定位这些内容：

- 银杏：视频呼叫的**状态辨认、联系人确认、超时和安全停止**思路；不要拷贝 UI 全套或日志／诊断上报模块。
- SemplicePhone：家属端如何编辑联系人、保存配置、验证版本；不要加入 SOS、远程定位、药物提醒。
- BaldPhone：大触控目标、颜色对比和按键反馈；不要变成完整老人桌面。

参考时输出简短笔记：`可借鉴能力 → 对应文件／类 → 当前项目是否需要 → 许可风险 → 是否采纳`。没有直接相关性就跳过。

---

## 2. 技术栈与环境约束

### 2.1 推荐技术栈

| 层级 | 选型 | 理由 |
|---|---|---|
| 平台 | Android，Kotlin | 原生设备触摸、媒体播放和系统集成更直接 |
| UI | Jetpack Compose + Material 3 基础主题 | 可做极简页面；不用堆组件 |
| 换页 | `VerticalPager`（Compose Foundation） | 与熟悉的纵向切换对应；有稳定页状态和惯性停止状态 |
| 状态 | `ViewModel` + `StateFlow` | 明确的单向事件和状态，便于测试 |
| 本地配置 | DataStore / 简单 JSON + 私有文件 | 首期联系人只有 2—5 位，不必为了数据规模强行引入 Room；结构增长后再评估迁移 |
| 图片 | Android Photo Picker / `GetContent` + 拷贝到私有目录 | 仅获取用户选中的照片 |
| 音频录制 | Android `MediaRecorder` | 首期仅录制短语音，申请录音权限 |
| 音频播放 | `MediaPlayer` 或项目已有轻量播放器 | 短语音播放足够，不能同时叠音 |
| 通话请求 | `CallGateway` 接口 + `DemoCallGateway` | 模拟与真实接入解耦，避免假成功 |
| P1 微信 | 另行探索 `AccessibilityService` | 第三方微信没有可靠公开的“给好友拨视频”通用 SDK |
| P3 同步 | HTTPS + 文件资源 + 配置版本 | 当前不实施，只保留数据格式的版本字段 |

**环境建议**：JDK 17、Android Studio 当前稳定版与匹配的 Gradle 插件／Kotlin／Compose BOM。`minSdk = 26` 作为初始建议（Android 8.0），最终应结合目标真实老人手机的 Android 版本确认。`compileSdk` 与 `targetSdk` 采用当前稳定且适用于分发渠道的版本，不要凭空固定未经验证的版本号。项目 namespace 可暂用 `org.changlianxi.app`，正式上架前确认包名冲突与签名策略。

### 2.2 依赖控制

- P0 **不需要网络权限**（除非明确为演示或后续能力引入；先不声明 `INTERNET`）。
- 不需要通讯录权限：只使用家属在应用内创建的 2—5 个联系人。
- 录音功能明确请求 `RECORD_AUDIO`，其他时间不访问麦克风。
- 图片选择优先系统 Picker，不申请全盘存储权限。
- 不声明设备管理员、无障碍、通知读取、位置、摄像头等 P0 无关权限。
- 不加入广告、跟踪 SDK、Crashlytics 或自动上传日志。

### 2.3 建议工程结构（新建工程时适用）

```text
changlianxi/
├── README.md
├── docs/
│   ├── 常联系-产品设计文档-PRD-v0.1.md
│   └── 常联系-开发规格-SPEC-v0.1.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/...
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/org/changlianxi/app/
        │   │   ├── MainActivity.kt
        │   │   ├── model/           # Contact、AppSettings、CallPhase
        │   │   ├── data/            # 本地配置与媒体文件读写
        │   │   ├── audio/           # VoicePlayer、VoiceRecorder、状态语音
        │   │   ├── call/            # CallGateway 与 DemoCallGateway
        │   │   ├── ui/elder/        # 大照片、垂直切页、呼叫／来电状态
        │   │   ├── ui/family/       # 首次配置、PIN、家属编辑
        │   │   └── ui/theme/        # 极简主题
        │   └── res/...
        ├── test/                    # 纯业务逻辑测试
        └── androidTest/             # Compose 关键交互测试
```

目录是**最小建议**，不是必须机械创建全部空文件／抽象。先查看现有工程；如果已有合理结构，应优先沿用。

---

## 3. 数据模型与存储契约

### 3.1 联系人数据

Kotlin 模型示意（字段名为建议，最终以实际实现为准）：

```kotlin
data class FamilyContact(
    val id: String,                  // 本地 UUID，稳定不复用
    val displayName: String,         // 家属填写称呼，例如“女儿”
    val photoPath: String,           // App 私有目录的文件标识／相对路径
    val voicePath: String,           // App 私有目录的文件标识／相对路径
    val order: Int,
    val isDefault: Boolean,
    val callTarget: CallTarget       // Demo=模拟；Phone=真实蜂窝拨号（P1 回退，已实现）；WeChatVideo=P1 微信（未实现）
)

sealed interface CallTarget {
    data object Demo : CallTarget
    data class WeChatVideo(val localBindingId: String) : CallTarget
    data class Phone(val number: String) : CallTarget   // 家属手动录入，仅存本机私有配置
}

enum class DialMode {
    TAP_PHOTO,                       // 默认
    LARGE_BUTTON
}

enum class PhoneDialMethod {
    DIRECT_CALL,                     // 一键直拨 ACTION_CALL，需 CALL_PHONE（默认）
    SYSTEM_DIALER                    // 系统拨号盘 ACTION_DIAL，零权限
}

data class AppSettings(
    val schemaVersion: Int = 1,
    val dialMode: DialMode = DialMode.TAP_PHOTO,
    val autoPlayVoice: Boolean = true,
    val defaultContactId: String? = null,
    val configVersion: Long = 1,
    val phoneDialMethod: PhoneDialMethod = PhoneDialMethod.DIRECT_CALL
)
```

**`WeChatVideo.localBindingId` 不是微信公开的稳定联系人 ID**；P1 需要在本地保存用户手工确认的绑定信息和验证状态，不能假定可以通过一串号码静默发起微信视频。

### 3.2 约束与一致性

- 每位联系人 `id` 唯一；`order` 连续或可稳定排序；默认联系人最多一个。
- 必须先确认照片、音频文件成功复制、可读取，才提交联系人配置。文件不存在时不得静默引用其他人的资源。
- 删除联系人先更新配置，再清理其不再使用的媒体；不能误删其他联系人的相同文件。
- 删除默认联系人时重新指定默认对象；列表清空时回到家属引导，不显示假的拨号页。
- 联系人更换顺序后，UI 当前项应按 `id` 保持，不应仅按分页 index 误将另一人当作当前呼叫对象。
- 配置持久化应使用临时文件 + 原子替换，或 DataStore 事务，避免中途崩溃写坏全部数据。
- P0 存储路径仅是本地私有文件，不写真实用户文件路径到日志。

### 3.3 配置文件示例（示意，不包含真实隐私）

```json
{
  "schemaVersion": 1,
  "configVersion": 1,
  "settings": {
    "dialMode": "TAP_PHOTO",
    "autoPlayVoice": true,
    "defaultContactId": "contact-a"
  },
  "contacts": [
    {
      "id": "contact-a",
      "displayName": "女儿",
      "photoPath": "assets/photo/contact-a.jpg",
      "voicePath": "assets/voice/contact-a.m4a",
      "order": 0,
      "callTarget": { "type": "DEMO" }
    }
  ]
}
```

这里存的是私有路径，不是上传到互联网的 URL。**家人相片与语音不得提交 Git 仓库**；自动化测试使用有授权的假数据或自己制作的占位资源。**手机号（`CallTarget.Phone.number`）同样仅存本机私有配置、绑定 `contactId`，绝不进日志、Git 仓库或公开演示**；公开演示与种子脚本只用虚构号码（如模拟器互拨 `5556`）。

### 3.4 家属 PIN

- P0 可采用家属设定的 4—6 位 PIN；对本机尝试次数进行合理限制与冷却。
- 不保存 PIN 明文；采用 Android Keystore 支持的安全设计，或使用带盐的可靠密码推导结果保存。
- 应用退到后台后锁定家属设置页面；恢复时重新验证。
- PIN 丢失的 P0 恢复方式：设备持有人在了解数据会清空的前提下，清除应用数据并重新配置。不得提供静态万能 PIN。

---

## 4. 老人端 UI 的具体实现契约

### 4.1 布局层级（只保留必要元素）

```text
ElderHomeScreen
└── Box(填满可用显示区域，尊重系统安全区)
    ├── VerticalPager(每页固定一位家人)
    │   └── ContactPage(contact)
    │       ├── FamilyPhoto(占主要区域；保留人脸)
    │       └── BottomCaption(一行大称呼 + 克制的拨号语义图形)
    └── [若 dialMode=LARGE_BUTTON] BigCallButton(底部唯一主要操作)
```

**绝不添加**底部 Tab、搜索框、横向滑块、联系人数量徽章、头像缩略图条、悬浮客服、设置齿轮、聊天室、广告和花哨背景。

建议色彩（须以真实老年人可读性验证）：

| 语义 | 色值 | 用法 |
|---|---|---|
| 页面底色 | `#F7F7F3` | 照片周边或背景 |
| 主文字 | `#1B1B1B` | 称呼、重要信息 |
| 拨打／接听 | `#22754F` | 唯一主要正向操作 |
| 取消／挂断 | `#B43F3B` | 明确的危险操作 |
| 辅助文字背景 | `#FFFFFF` 或半透明暗底 | 确保照片复杂背景上仍可读 |

**可点击目标**：默认点击照片区域，热区尽可能覆盖除系统安全区外的大部分照片。照片并非必须“圆形头像”。“独立按钮”模式则只由大按钮触发拨打，不能同时保留整图拨打以制造两种互相冲突的操作含义。

### 4.2 垂直切页

使用 `VerticalPager`，监听 `settledPage` 和 `isScrollInProgress` 之类的稳定状态。实际 API 名称以工程依赖版本为准；不要在重组函数体直接启动声音。

规定：

1. 默认按联系人 `order` 排列；推荐 2—5 人，人数可按需求调整。
2. 不做循环轮播；第一页上滑／最后一页下滑遇到边界后保持该页。
3. 动画运行、惯性未结束期间禁用拨号。
4. 稳定后延迟约 300ms 触发语音；快速连续滑动只播放最终停住的联系人录音。
5. 点击与竖向拖动必须互斥。优先使用 Compose 组件内置手势与 `clickable`，只在经测试确有冲突时增加低层指针事件处理。
6. 呼叫请求已经提交时锁住当前 `contactId` 和 UI 触摸，不能切页后复用上一次的拨号目标。
7. 家属调整联系人顺序后，以 `contactId` 恢复当前目标，不能依赖旧 index。

### 4.3 语音识别与状态播报

**联系人本人录音**：

- 存储在 App 私有目录，首期从家属设置录制或导入。
- 不把真人录音当作 TTS；它是由家属真实录制的声音。
- 约 3—8 秒为推荐值，过长给出家属提示，不强行硬截断。
- 每次页面稳定只播放一次。切页、点击拨打、来电、App 进入后台时立即停播。
- 同一时刻最多允许一个语音输出任务；使用单一 `VoicePlayer` 管理当前播放 token／世代号，旧异步任务返回后不得播放旧联系人。

**系统状态语音**：

- 使用固定短句资源，例如“正在帮你打电话”“手机没联网”“电话没有打出去”。
- 可优先使用打包的语音资源，或系统 `TextToSpeech`（需检查本机中文语音是否可用）；不能假定所有手机都有可用 TTS。
- 语音资源与状态事件一一对应，不允许把 `RequestSent` 说成 `Connected`。
- 真正接入视频后，呼叫状态提示不得抢占通话音频；遵循 Android Audio Focus 和生命周期。

**音频序列示例**：

```text
进入家人 A → 稳定 300ms → 播放 A 录音
立刻滑到 B → 停止 A，取消 A 待播任务
B 页面稳定 → 播放 B 录音
点击 B 照片 → 立即停止 B 录音 → 请求拨号 → 播放请求中提示
切到后台／屏幕关闭 → 停止录音，无后台持续播报
```

### 4.4 页面导航与返回

- 打开 App：已配置时进入 E01；未配置时进入家属初始化引导。
- E01 允许用户通过 Android 正常返回／Home 退出；不强行阻止退出。
- E02/E05 退出或取消后回原家人照片。
- E03/E04 仅为模拟或未来自有 RTC；微信交接后尊重微信和系统返回行为，不能假设能够在任何 ROM 上强制返回本 App。
- 家属设置返回时若未保存，需提示家属而不是让老人处理冲突。

### 4.5 配置入口

- 首装：显式家属引导。
- 配置后：顶部安全留白区域长按约 4 秒，弹 PIN 输入（需要屏幕提示的对象是家属，不是老人）。
- 禁止在大照片触摸区域设置长按进入配置，避免与老人按照片的习惯混淆。
- 需实测 4 秒入口在系统手势区域、全屏模式下能触发；不适配则改为固定的家属入口方式，不要藏到不可到达的位置。

### 4.6 来电、接听与挂断

P0 使用 `DemoCallGateway`，UI 仅用于测试：

- 接到模拟来电 → 显示指定家人照片 → 播放来电提示。
- 点击照片 → `DemoConnected`；点击独立红色拒接区域 → `Declined`。
- 在模拟连接页只有一个明确红色挂断区域 → `Ended` → 返回原照片。

**禁止**在 P0 中拦截系统或微信来电，或声称实现了微信原生来电界面替换。微信官方并无保证第三方任意接管其来电 UI 的开放接口。自有 RTC 的真实来电显示与系统限制应在 P4 依据 Android Telecom/通知规范设计。

---

## 5. 呼叫状态机与模拟适配器

### 5.1 事件类型（推荐）

```kotlin
sealed interface CallPhase {
    data object Idle : CallPhase
    data class Preparing(val contactId: String) : CallPhase
    data class HandoffStarted(val contactId: String) : CallPhase
    data class DialingVerified(val contactId: String) : CallPhase
    data class DemoRinging(val contactId: String) : CallPhase
    data class DemoConnected(val contactId: String) : CallPhase
    data class IncomingDemo(val contactId: String) : CallPhase
    data class Failed(val contactId: String, val reason: CallError) : CallPhase
    data class Finished(val contactId: String) : CallPhase
}

enum class CallError {
    MISSING_CONTACT,
    MISSING_BINDING,
    NETWORK_UNAVAILABLE,
    WECHAT_UNAVAILABLE,
    ACCESSIBILITY_DISABLED,
    CONTACT_NOT_VERIFIED,
    TIMEOUT_UNCONFIRMED,
    CANCELLED,
    UNKNOWN
}
```

代码名称只是拟定状态集合，不要求重复封装。P0 可以用更少的状态，但必须保持 **“请求发起 ≠ 已拨通 ≠ 已接听”** 的语义区分。

### 5.2 最小接口

```kotlin
interface CallGateway {
    suspend fun startVideoCall(contact: FamilyContact): CallStartResult
}

sealed interface CallStartResult {
    data object DemoStarted : CallStartResult
    data object HandoffStarted : CallStartResult
    data object DialingVerified : CallStartResult
    data class Failed(val error: CallError) : CallStartResult
}
```

- `DemoCallGateway`：完全本地，不访问通信系统。模拟结果由开发／演示控件触发，不从 UI 代码直接伪造成功。
- `RealCallGateway`（P1 回退，已实现）：真实蜂窝拨号。决策交给纯逻辑 `RealCallPlanner`（号码可拨性／拨号方式／权限），Android 侧只发 `ACTION_CALL`/`ACTION_DIAL` Intent 并返回 `HandoffStarted`；`SecurityException` 时静默退回零权限 `ACTION_DIAL`。交棒后 App 不可观测接通／挂断，绝不假装已接通。
- `WeChatCallGateway`：P1 才实现，默认禁用。接到用户明确请求后，负责判断权限／微信状态／联系人匹配／页面验证；任何无法验证都返回失败或不可确认。
- `RTC` 方案：P4 才评估，不提前创建空实现。

### 5.3 状态迁移约束

| 当前状态 | 事件 | 下一状态 | 必要防护 |
|---|---|---|---|
| Idle | 有效点击当前照片 | Preparing | `contactId` 锁定；幂等，防双击 |
| Preparing | 模拟通话启动 | DemoRinging | 页面显示“模拟演示” |
| Preparing | 微信适配器已交接（P1） | HandoffStarted | 不能说已打通 |
| HandoffStarted | 验证进入正确对象呼叫页（P1） | DialingVerified | 不推断对方接听 |
| Preparing/HandoffStarted | 失败／超时 | Failed | 停止自动化，不得重试猜测联系人 |
| DemoRinging | 演示对方接听 | DemoConnected | 仅演示 |
| DemoRinging | 演示无人接听 | Failed | 用演示结果提示，不声称真实通话 |
| IncomingDemo | 点击照片／拒接 | DemoConnected／Finished | 两个触控区域独立 |
| DemoConnected | 点击红色挂断 | Finished | 只执行一次结束动作 |
| Finished/Failed | 返回照片 | Idle | 回到原 `contactId` |

### 5.4 防止重复呼叫

- 只接受 Idle 状态下的一次有效点击。
- 同一事务创建 `requestId`，后续事件必须与当前 `requestId` 和 `contactId` 匹配；过期事件丢弃。
- 进入 Preparing 时立即锁定按钮；失败后用户**自己**再次点击才重试。
- 在微信适配器交接成功或失败前，不发起第二次自动化。
- 不使用固定延时 5 秒就宣称“对方没接”等推断性话术。

---

## 6. 家属本机配置的详细 SPEC

### 6.1 首次启动

```text
首次启动
→ 无联系人数据：家属引导
→ 设置 PIN
→ 添加首位家人
    → 填称呼
    → 选照片并预览裁剪
    → 录本人声音／导入本人录音／试听
→ 可继续添加其他家人
→ 选默认联系人
→ 确认“照片点击拨打”默认模式
→ 保存并进入老人首页
```

照片/录音字段必须经过存在性、格式和可读取性检查。若只完成一半，留在家属编辑页，不要把空文件配置发布到老人主页。

### 6.2 联系人操作

| 操作 | 具体约束 |
|---|---|
| 添加 | 为新联系人生成 UUID；初版最多建议显示 5 人，实际可调整上限 |
| 修改照片 | 图片复制完成后才提交新配置；旧文件在不再引用后清理 |
| 录制录音 | 开始前申请权限；录制时明确标记；支持停止、试听、重新录 |
| 导入录音 | 允许选择合适音频格式，转存私有目录；播放验证后提交 |
| 排序 | 可由家属使用上移／下移按钮，避免为家属端也引入复杂拖拽 |
| 修改默认 | 必须属于当前有效联系人列表 |
| 删除 | 有确认；若删除默认则必须选新的默认，不能静默指向无效 ID |
| 预览 | 与老人端使用同一份 ContactPage 视觉组件，避免两套设计不一致 |

### 6.3 配置保存与媒体文件

- 读取第三方文件仅通过 Android 官方选取器。立即将选择的图片/音频内容复制到 App 专属目录，并作文件存在和大小检查。
- 用稳定、与联系人的 ID 绑定的媒体文件命名，避免文件名碰撞。
- 将图片压缩到合理尺寸且保留人脸辨识；避免在滑动时加载几 MB 原图引起卡顿或 OOM。
- 保存配置前先写好文件并验证；失败则旧配置保持不变。
- 保存时写入 `configVersion`，便于未来升级迁移。失败回滚到上一合法配置。
- 音频录制完成后**释放麦克风**；录音失败不能阻止返回家属设置。
- 测试必须覆盖 App 进程被系统回收后重新打开的配置恢复。

---

## 7. P1 微信视频 PoC（在 P0 完成后单独启动）

### 7.1 技术现实

微信没有提供可依赖的第三方“按好友 ID 直接发起微信视频”的公开通用 SDK。可研究 Android `AccessibilityService` 在明确授权后执行特定 UI 辅助动作，但该方式受微信版本、设备 ROM、无障碍权限和系统后台限制影响。

**P1 目标仅限单台真实 Android 手机上的主动拨出**。不宣称全机型普适，不承诺自定义微信来电界面，不使用脚本持续控制微信。

### 7.2 PoC 预检清单

1. 记录设备品牌、型号、Android 版本、系统 ROM、微信版本、屏幕设置。
2. 已安装微信、已正常登录，允许测试的家庭联系人可在微信内手工完成视频拨打。
3. 老人与家属对辅助操作明确知情；无障碍权限由设备使用者通过系统 UI 主动启用。
4. 家属逐一确认「常联系中的照片 A → 微信目标 A」绑定。不能依赖纯昵称搜索后“找到第一个匹配就拨打”。
5. 每次呼叫都由老人端明确点击触发；**不得在后台自行发起**。
6. 若微信界面节点无法区分“语音”和“视频”、联系人身份不一致、检测到广告遮挡或未知页面，**立即安全停止**。
7. 不读取聊天内容、不截图上传、不扫描全量通讯录、不记录联系人隐私数据。

### 7.3 可观测阶段与失败判定

```text
Elder click
  -> check permission + WeChat installation + binding verified
  -> open permitted UI path
  -> verify expected contact conversation/header
  -> locate and select intended video-call action
  -> verify video calling screen / other reliable signal
  -> hand off to WeChat; call state beyond this point may be unobservable
```

严禁以下行为：

- 未确认对象身份就按固定坐标拨号。
- 遇到未知页面不断随机点“下一步”。
- 没有证据就宣称已接通、已挂断或对方未接。
- 绕过系统权限弹窗、偷偷启用无障碍服务或隐藏退出方式。
- 为提高“通过率”而删除失败日志或失败测试。

### 7.4 真机矩阵与停止阈值

| 场景 | 预期 |
|---|---|
| 正确联系人且正常页面 | 能进入正确对象的微信视频呼叫页（如实际支持） |
| 同名联系人 | 不允许未验证自动选第一人 |
| 微信里有广告/权限弹窗 | 安全停止或交给家属，不盲点 |
| 微信未登录／未安装 | 短句提示，回到照片页 |
| 无障碍被关闭 | 不尝试猜测或绕过，明确告知家属 |
| 微信版本改变 | 重新验证，不假定旧规则永久适用 |
| 退出微信通话 | 如不能可靠获知结束，则不播放虚假的“已挂断”提示 |
| 手机锁屏／息屏来电 | P1 不承诺实现，单独记录 |

**P1 不通过的条件**：出现一次拨错到非目标联系人即暂停 PoC，分析根因后才能继续。对一个需要老人信任的公益 App，宁愿不拨，也不能拨错。

### 7.5 许可证与合规

银杏旧源码是 GPL，且新版自动接听能力未公开对应完整源码；参考思路后独立设计。Google Play 要求无障碍 API 用于清晰、狭窄、获用户知情的辅助用途，并在申请上架时履行相关声明。部分自动化和平台安全绕过行为被明确禁止。正式分发前必须再次审查当时平台规定与个人信息保护义务。

---

## 8. P3 家属远程管理（只做契约设计，不在 P0 实施）

### 8.1 未来架构

```text
家属网页（登录）
    ↓ HTTPS
配置服务（认证、设备授权、草稿、版本发布）
    ↓ Manifest + 媒体文件（完整性校验）
老人端（拉取、下载、验证、原子切换）
    ↓
本地最新有效配置（离线仍可看照片听录音）
```

### 8.2 最小远程数据 API（拟议，未实现）

| API | 用途 | 权限要求 |
|---|---|---|
| `POST /v1/devices/pairing-sessions` | 老人设备现场发起限时配对 | 老人设备本地确认 |
| `POST /v1/devices/{id}/pair` | 家属完成绑定 | 有效临时 token、设备确认 |
| `GET /v1/devices/{id}/config` | 老人设备拉取配置 Manifest | 绑定设备凭据 |
| `PUT /v1/devices/{id}/draft` | 家属保存草稿 | 绑定家属身份 |
| `POST /v1/devices/{id}/publish` | 发布一个完整新版本 | 绑定家属身份、版本校验 |
| `POST /v1/devices/{id}/unpair` | 解除绑定 | 认证＋必要的本机确认 |

接口是未来版本的结构设计，**不要求 Qoder 在 P0 中创建路由、数据库或云账号**。

### 8.3 配置更新安全策略

- 采用 HTTPS、身份认证、设备配对与有效期限制；配置应有版本、资源哈希和可验证来源。
- 先拉取 Manifest，再下载所有照片／录音，校验完整性后切换版本；缺一个资源就保留旧版本。
- 上传的照片/录音默认仅授权家庭内可见，不使用永久公开 URL。
- 不允许家属远程开启无障碍、摄像头、麦克风或自动接听。
- 可撤销绑定，不得依赖固定设备 ID 当作授权。
- 远程配置不能在老人正在拨号／通话时突然替换当前目标联系人。

**可参考** SemplicePhone 的配置签名、版本与家属门户，但其 portal 使用 AGPL-3.0-or-later；直接改造为网络服务需认真评估源码提供义务。也不要照搬该项目所有安全设置，需针对我们有限业务设计最小、可信的实现。

---

## 9. Qoder Sites 比赛演示（与 Android 工程分开）

Qoder 比赛要求可打开的站点。Qoder Sites 可以创建、预览、发布网页；Free 个人计划仅支持静态站点，不提供站点数据库和存储。网页演示与 Android APK 是**两个交付形态**。

### 9.1 网站目标

做一个**浏览器内可交互的手机模拟器**，让评委 2—3 分钟体验选人、录音、拨号、来电和异常；明确说明这是一套交互演示，并展示真实 Android 试用视频／构建结果（如后来实际取得）。

### 9.2 网站页面

1. 首页：一句话介绍、面向人群、产品价值、进入互动演示按钮。
2. 老人端演示：一屏一人，滑动、点击、可选听录音；呼叫行为只转到明确标注“模拟”的页面。
3. 家属设置演示：给评委用假照片／模拟录音体验配置，不上传真实家属音视频到公共站点。
4. 设计说明：交互原则、微信真实接入边界、测试计划和隐私策略。
5. 开发记录：Qoder 参与开发过程和真实验证证据。

### 9.3 网站特别约束

- 网站能够离线或使用静态样例数据运行，不依赖登录与数据库。
- 浏览器自动播放声音受用户手势策略限制，首次声音可能需要用户点击激活，不能把这个限制误归为 Android 原生功能缺陷。
- `localStorage` 若用于演示编辑，只对当前浏览器有效，不构成真正的家属跨设备同步。
- 若没有真实 APK 实测，不要写“已在微信真实打通”。
- 不要为比赛演示临时添加各种与老人通讯无关的炫技功能。

参考：[Qoder Sites 官方文档](https://docs.qoder.com/zh/qoder/sites)。

---

## 10. 任务拆解：Qoder 按序执行

每个任务需记录变更文件、关键机制、实际验证命令和结果，避免一口气生成大量不可验证代码。

### T0｜代码库与参考调研（开始前）

**输入**：本 SPEC + PRD，Android SDK 与本地工程现状。  
**工作**：

- 读取现有代码／目录，识别是否已有 Android 项目和 Gradle 配置。
- 按第 1 节 `git clone` 参考仓库到主工程外；核对 LICENSE 与能实际看到的代码。
- 输出最小开发计划，并确认目标设备 Android 版本（未知时按 `minSdk=26` 暂定）。

**完成条件**：可以说清楚现有工程的构建入口、参考代码边界和第一步要改哪些文件。无关参考项目不必全部深入阅读。

### T1｜Android 工程可构建

- 创建最小 `MainActivity`、Compose Theme、Gradle 配置。
- 生成一个无复杂菜单的联系人空态／首装入口。
- 不加入网络、无障碍、相机、通知权限。

**完成条件**：实际执行 `assembleDebug` 成功；在模拟器打开 App。

### T2｜本地联系人与家属配置

- 实现联系人模型、配置存储与文件存储。
- 家属初次配置 PIN、照片选择、录音／导入、试听、默认联系人、排序。
- 保存及重启恢复；错误资源与删除默认联系人均有显式处理。

**完成条件**：至少 3 位假联系人可持久保存，关闭并重新打开仍正确；照片、音频没有错位。

### T3｜老人主页的极简视觉与滑动

- 全屏大照片，首启默认联系人；`VerticalPager` 一页一人。
- 默认照片点击拨号；家属可切成独立大按钮。
- 不显示常驻设置按钮、导航 Tab、菜单和多余图标。

**完成条件**：一轮 20 次上下滑动不发生越界、图片错位、误拨；1 位联系人时界面稳定。

### T4｜语音与异常反馈

- 稳定页延迟 300ms 播放本人录音，换页取消旧任务。
- 拨号请求、失败和结束播放固定状态声音；音频焦点与生命周期正确。
- 本人录音缺失与 TTS 不可用时不崩溃，家属可以重新配置。

**完成条件**：10 次快速连续滑动只听到停住页面的联系人声音，不叠音；进入后台立即停播。

### T5｜DemoCallGateway 和通话模拟

- 实现 `CallGateway` 与演示状态机。
- 支持模拟“发起→响铃→接通→挂断”、模拟来电“照片接听／红色拒接”、断网／失败回退。
- 保证不产生真实网络呼叫；演示状态明确可识别。

**完成条件**：模拟关键分支全部可操作，错误时回到原联系人，连续点击不重复呼叫。

### T6｜自动化测试与真机试用

- 补充第 11 节单元／UI 测试，运行与范围最相关的 Gradle 任务。
- 在至少一台真机上试用，与目标老人进行初步交互实验。
- 输出“验证过的功能／尚未验证的功能／已知限制”简短表格。

**完成条件**：可安装 APK、关键流程实测可用；有真实记录而不是推测。

### T7｜微信 P1 技术预研（需单独确认再启动）

- 建立隔离模块／分支，真机测试授权、正确联系人确认、状态验证、失败关闭。
- 保存设备/微信版本测试矩阵，不让实验代码污染 P0。
- 若 PoC 失败，P0 应继续可独立安装运行。

### T8｜Qoder Sites（用于参赛的独立任务）

- 完成网页交互演示和功能边界说明；先预览再发布。
- 确保公开链接无需 APK 安装即可体验核心交互。
- 真实 Android 使用视频只在完成试用后加入。

---

## 11. 验收场景（Given / When / Then）

### 11.1 首页与滑动

- AC-01：**给定**配置了 3 个联系人并设定 A 为默认，**当**冷启动 App，**则**直接看到 A 的大照片，无须菜单导航，且不自动拨号。
- AC-02：**给定**停在 A，**当**上滑到 B，**则**B 的照片与录音一致，A 的录音停止。
- AC-03：**给定**只有 1 人，**当**反复上滑／下滑，**则**页面仍显示此人，不崩溃、不误拨。
- AC-04：**给定**停在最后一位，**当**继续上滑，**则**不会循环至第一位，且不触发通话。
- AC-05：**给定**用户快速连滑 3 页，**当**最终页停稳，**则**只播最终联系人的录音。
- AC-06：**给定**正在拖动页面，**当**手指放开，**则**放开动作不被识别为有效照片点击。

### 11.2 点击与通话

- AC-07：**给定**默认 `TAP_PHOTO` 模式，**当**点击大照片，**则**只提交一次当前 `contactId` 的通话请求。
- AC-08：**给定**`LARGE_BUTTON` 模式，**当**点击照片，**则**不拨号；点击唯一大按钮才拨号。
- AC-09：**给定**处于 Preparing，**当**快速双击，**则**仍只有一个 `requestId`。
- AC-10：**给定**正在呼叫 A，**当**后台发生数据排序变化，**则**此通话目标仍为 A，不会变成当前 index 的 B。
- AC-11：**给定**Demo 模式，**当**展示呼叫中／已接通，**则**有明确“演示”标识，未实际发起微信呼叫。
- AC-12：**给定**模拟来电，**当**点击家人照片，**则**进入模拟已接通；点击红色拒接区域则返回照片页。
- AC-13：**给定**模拟已接通，**当**点红色挂断，**则**仅结束一次并返回原联系人照片。

### 11.3 声音与异常

- AC-14：**给定**A 录音播放中，**当**切到 B，**则**A 的播放取消，没有重叠声音。
- AC-15：**给定**播放中 App 进入后台，**则**立即停止本人录音，不继续后台播放。
- AC-16：**给定**系统没有可用 TTS，**当**出现错误，**则**应用不崩溃，保留可读／可配置的备用提示。
- AC-17：**给定**无网络或模拟失败，**当**请求拨号，**则**有正确语音和返回路径，不声称已接通。
- AC-18：**给定**没有有效呼叫对象绑定，**当**按拨打，**则**安全停止，不选择其他联系人替代。

### 11.4 家属配置与存储

- AC-19：**给定**家属导入照片与音频，**当**保存并杀死进程后重启，**则**照片、声音和排序不丢失。
- AC-20：**给定**删除的是默认联系人，**当**确认删除，**则**配置有新的有效默认对象或回到家属初始化，不引用失效 ID。
- AC-21：**给定**导入一个损坏音频，**当**保存，**则**不能把它作为成功配置交给老人页，需提示家属重新选择。
- AC-22：**给定**家属设置 PIN，**当**老人短按或普通滑动照片，**则**不会误打开设置。
- AC-23：**给定**App 退到后台后再回家属设置，**则**敏感配置页重新锁定。
- AC-24：**给定**替换 A 的录音失败，**则**原录音保持可用，不会意外变成 B 的录音。

### 11.5 实际验证与边界

- AC-25：Android 构建任务产出 APK，并在测试设备上完成核心任务；未执行的检查需单独标为未验证。
- AC-26：Manifest 没有**无关**危险权限、后台电话操作或隐式联网依赖。P0 仅 `RECORD_AUDIO`；P1 真实蜂窝拨号**有意**新增 `CALL_PHONE`（仅老人明确点击后拨家属预设号码；家属可改零权限 `ACTION_DIAL`，未授权时网关静默退回），并把电话硬件声明为可选（`telephony required=false`，无 SIM 设备仍可安装退回演示）；仍无网络／通讯录／相机／无障碍／位置。
- AC-27：日志与示例数据不包含未经同意的真实照片、录音、手机号和微信账号。
- AC-28：如果 P1 未通过，P0 仍可独立安装并完成全部模拟交互。
- AC-29：如果 P1 微信界面定位到同名／不确定联系人，**不得拨号**。
- AC-30：如果 Qoder Sites 已发布，页面能够清楚区分“网页演示”与“真机通话验证”，不能误导评委。

### 11.6 建议测试分层

| 层级 | 测试内容 | 方式 |
|---|---|---|
| 纯单元测试 | 联系人排序、默认选择、删除、配置迁移、状态机和 requestId 幂等 | JVM 单元测试 |
| Compose UI 测试 | 默认页、滑动、点击／拖动冲突、两种按钮模式、错误返回 | `androidTest` + Compose 测试 |
| 组件测试 | 照片／音频导入、权限拒绝、进程恢复、录音取消 | 模拟器／真机 |
| 真机手工测试 | 语音不叠加、后台／锁屏行为、小屏适配、系统返回 | 明确设备信息和复现步骤 |
| 老人使用测试 | 无文字帮助时的识别、误触、耗时、复测 | 观察记录与知情同意 |

---

## 12. 构建、调试与验证命令

从 Android 工程根目录运行，Windows PowerShell 示例：

```powershell
# 检查 JDK 与 Gradle Wrapper
java -version
.\gradlew.bat --version

# 生成 Debug APK
.\gradlew.bat :app:assembleDebug

# 与范围相关的单元测试
.\gradlew.bat :app:testDebugUnitTest

# 静态检查
.\gradlew.bat :app:lintDebug

# 有连接的模拟器／真机且配置了测试后再运行
.\gradlew.bat :app:connectedDebugAndroidTest

# 如配置有 Android Debug Bridge
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

如果实际工程使用多产品变体或不同模块名，上述任务名可能需要按 Gradle 输出调整；必须以实际运行结果为准。不要因为环境缺 SDK 就假装构建成功。

### 12.1 建议调试日志

仅记录必要的**非隐私状态**，例如：

```text
contact_page_settled: index=1 (不打印姓名/微信账号)
voice_playback: started/stopped/failed
call_request: requestId=..., phase=PREPARING
call_result: phase=FAILED, code=NETWORK_UNAVAILABLE
config_commit: version=3, outcome=SUCCESS
```

不上传图片文件、录音、微信账号、聊天文本、完整电话号或屏幕截图。线上发布时可以关掉调试日志或只保留必要事件。

---

## 13. 风险清单与优先处理顺序

| 风险 | 严重程度 | 预防／应对 |
|---|---|---|
| 错拨到其他人 | 极高 | 锁定 contactId，明确身份确认，未知就停止 |
| 真实微信视频不可用 | 高 | P0 保留模拟；P1 单机型 PoC；不把微信作为前期交付阻塞项 |
| 微信接听界面无法替换 | 高 | P0 只演示；P1 不承诺；P4 自有 RTC 才控制完整 UI |
| 滑动和点击冲突 | 高 | 使用稳定 Pager + 手势识别，UI 自动化与老人真机测试 |
| 旧录音播放到新照片 | 高 | 每次切页取消旧播放 token、基于稳定 ID 关联资源 |
| 录音音量太小 | 中 | 试听、音量检测提示、真实设备与老人听觉测试 |
| 家属设置入口过于隐蔽 | 中 | 首装明确说明、家属侧说明书、设备可恢复路径 |
| 文件复制失败导致配置损坏 | 高 | 先复制并验证，配置原子提交，旧版本保留 |
| 将第三方 GPL/AGPL 代码误复制 | 高 | 参考目录隔离、逐文件许可核验，不直接搬仓库 |
| 真机厂商 ROM 限制后台权限 | 中—高 | 记录机型／系统，P1 专项测试，不宣称普适 |
| 公益宣传夸大功能 | 高 | 列出“已验证／未验证／模拟”，提供实际结果 |

---

## 14. Qoder 初次任务提示词（可直接复制使用）

> 建议首次只做 **T0—T1**，检查构建环境后再逐步进入 T2。不要让 Agent 一次性生成 Android、微信自动化、云后端和网页演示四套系统。

```text
你是「常联系」Android 项目的开发工程师。

请先完整阅读：
1. docs/常联系-产品设计文档-PRD-v0.1.md
2. docs/常联系-开发规格-SPEC-v0.1.md

目标：开发面向低识字老人的极简亲情通讯 Android App。核心交互为一屏一个家人的大照片、上下滑动换人、播放家人本人录音、点击照片拨打。界面必须遵循“少即是多”，老人主页不要添加设置按钮、搜索框、Tab、悬浮操作或额外功能。

本轮仅执行 SPEC 的 T0—T1：
- 检查现有工程与 Android 构建环境。
- 将银杏、SemplicePhone、BaldPhone clone 到主工程外的相邻 references 目录，核对许可及可参考范围；禁止复制 GPL/AGPL 代码进入主工程。
- 如工程为空，建立最小 Android Kotlin + Jetpack Compose 应用。
- 做出启动后的首装空态／家属引导入口。
- 实际运行构建，报告结果。

请先给出最小实施计划，再执行。不要未经请求加入 AI、RTC、微信无障碍服务、后端、远程同步、桌面替换或多余页面。未验证的结果须明确标注。
```

### 14.1 下一轮提示词（T2—T3）

```text
继续「常联系」项目。请先检查已有代码与实际测试结果，按 SPEC 执行 T2—T3。
实现本机家属联系人配置、照片和真人录音存储、默认联系人、排序，以及老人端 Jetpack Compose VerticalPager 一屏一人和两种点击方式。
严格遵守 PRD 的无多余按钮原则；优先复用现有代码结构。补充针对联系人模型、默认联系人和点击/滑动行为的相关测试，执行实际构建并报告结果。不要开始微信、云端或 RTC 开发。
```

### 14.2 后续提示词（T4—T6）

```text
继续「常联系」项目，按 SPEC 执行 T4—T6。
完成家人真人录音的稳定页播放与取消、固定状态声音、DemoCallGateway 呼叫状态机、模拟来电/接听/挂断、异常恢复，以及相关单元/UI 测试。
禁止将模拟通话表述为真实微信视频。确保滑动过程中不会误拨，录音不会重叠，失败后可返回原联系人。实际运行相关构建、测试及可用的模拟器/真机验证，并区分已验证与未验证。
```

---

## 15. 官方资料与项目链接（2026-11-10 核查）

**开源仓库：**

1. [Yinxing](https://github.com/bjfwan/yinxing) — 注意 v2.3.0 与新版本公开范围的区别。
2. [SemplicePhone](https://github.com/francescosorzini-dev/SemplicePhone) — `tools/remote-server` 许可证为 AGPL-3.0-or-later。
3. [BaldPhone](https://github.com/UriahShaulMandel/BaldPhone) — Apache-2.0。
4. [SeniorLauncher](https://github.com/tobihille/SeniorLauncher) — 补充对照。
5. [LiveKit Android](https://github.com/livekit/client-sdk-android) — 未来 RTC 备选。

**开发平台与规范：**

- [Compose `VerticalPager`](https://developer.android.com/develop/ui/compose/layouts/pager)
- [Compose 手势](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)
- [Android AccessibilityService](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Google Play：Accessibility API 使用政策](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)
- [Android 自有来电 ConnectionService](https://developer.android.com/reference/android/telecom/ConnectionService)
- [Qoder Sites 使用和发布](https://docs.qoder.com/zh/qoder/sites)

**最终执行要求**：先落地能给真实老人试用的最小 App，再用用户测试决定是否增加功能。任何新增按钮、权限、框架、接口或云服务，都必须能对应 PRD 中一个已确认的必要需求。

---

## 16. 实现增补：真实蜂窝拨号（P1 回退，2026-11-14）

> 本节记录 v0.1 之后**已实现**的真实蜂窝拨号回退方案，是对第 3／5／11 节契约的增补与修订；未改动部分仍以原文为准。微信真实视频（P1）仍未启动。

### 16.1 动机与定位

老人不会在智能机上找联系人拨号（老年机有预设按键、智能机没有）。最基础的回退：**上下滑动选家人、点照片拨真实蜂窝电话**；呼出后由家人微信视频打回来，同样达成「见一面」，且比微信无障碍方案更简洁、更可控。因此把真实蜂窝拨号作为 P1 的**默认回退**先落地，微信视频仍单独确认。

### 16.2 三个已确认的设计决定

1. **拨号方式是全局设置**（`AppSettings.phoneDialMethod`），不按联系人区分：`DIRECT_CALL`（一键直拨 `ACTION_CALL`，需 `CALL_PHONE`，默认）／`SYSTEM_DIALER`（系统拨号盘 `ACTION_DIAL`，零权限、需再按拨打键）。
2. **号码按联系人录入**（`CallTarget.Phone.number`），家属手动填写；**留空 = 纯演示**（`CallTarget.Demo`），不拨真实电话。
3. **`DIRECT_CALL` 缺 `CALL_PHONE` 授权时静默退回 `SYSTEM_DIALER`**（`ACTION_DIAL`），不报错、不阻断老人；权限在家属页选定「一键直拨」时提前申请（PIN 之后），老人日常不会遇到中途权限弹窗。

### 16.3 失败关闭与诚实交棒

- `RealCallPlanner`（纯逻辑、JVM 可测）：号码字符／位数非法（数字 3—15 位之外）→ `Reject(MISSING_BINDING)`，不拨；`Demo` → 模拟；`WeChatVideo` → `Reject(WECHAT_UNAVAILABLE)`（未实现）。号码归一化保留 leading `+`、去除分隔符。
- `RealCallGateway` 发 Intent 后返回 `HandoffStarted`；状态机进入新增 `CallPhase.Handoff`。**App 不可观测真实接通／挂断**，故交棒页 `RealCallScreen` 只说「正在拨打／已交给系统电话，接通后由系统显示」+「返回」，**无「模拟」徽标、无假接听／挂断控件**，绝不声称已接通。
- 系统电话界面为透明覆盖层时 App 仅 pause 不 stop，交棒页保留；老人点「返回」→ `endCall` 回老人首页。App 真正退后台（onStop）→ 复位回老人首页。
- 每次真实拨号仍由老人明确点击触发；不后台自动拨号／自动接听。

### 16.4 隐私

号码仅存本机私有配置（与照片／录音同一原子存储）、绑定 `contactId`；**不进日志、Git 仓库、公开演示**。实测 Android 系统日志对 `tel:` 号码自动脱敏（`dat=tel:xxxx`）。网页演示保持纯模拟、虚构号码。

### 16.5 验证（诚实记录）

- JVM 单测 74/74（含 `RealCallPlannerTest` 8 项、`CallStateMachineTest` Handoff 4 项、号码配置往返与旧配置兼容）。
- `lintDebug` 0 错误；`assembleDebug` 产出 APK；合并清单 `RECORD_AUDIO` + `CALL_PHONE` + `telephony required=false`。
- 模拟器（emulator-5554）交棒验证：播种 `Phone("5556")` + 授权后点照片 → logcat `act=android.intent.action.CALL dat=tel:xxxx`、系统 `UserCallActivity` 接管、telecom 生成去电（MO）；交棒页与「返回」回首页截图 `changlianxi/screenshots/t7-realcall-*.png`；App 无崩溃。
- **未验证**：真机拨通（需真机 + 真 SIM，并入红米真机验证）。
- **已调优（系统进程问题，非 App 缺陷）**：headless 模拟器曾偶发「System UI isn't responding」ANR 与滑动黑帧。根因＝AVD 仅 2GB 内存（预装 Google 应用造成内存压力、stall SystemUI／telecom）＋ swiftshader 软件 GPU 合成黑帧；改 `-memory 4096`（`hw.ramSize=4G`）＋ `-gpu host` 后消除，人工投屏验证滑动／点按正常。
- **交互式投屏验证**：scrcpy 5.0.1 原生镜像 headless AVD（`--stay-awake`），鼠标拖拽＝滑动换人、单击＝点照片呼叫、右键＝返回；与 uiautomator 脚本互补。ws-scrcpy 因内置 scrcpy-server 1.19 仅支持 ≤Android 12 而弃用。
