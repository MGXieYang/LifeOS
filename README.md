# LifeOS

LifeOS 0.0.4 是一个 Local First / Offline First Android 应用，让工作的回报看得见、消费的重量感受得到、时间的流逝变得具体。`docs/PRODUCT.md` 是当前唯一有效产品事实源。

## 当前功能

- 当下：实时今日收入、有效工作时长、下班/午休倒计时、本月已赚、发薪倒计时。
- 天平：金额的工作小时、工作日、月薪比例、压力等级、长期使用成本和可选历史。
- 时间：年度进度、人生时间轴、自定义节点、渐进式退休、90/180/365 天未来预算。
- 设置：工作、个人、显示、主题和本地数据说明。

Decision 与 FreeTime 已从 V3 代码、导航和持久化模型删除。核心计算完全离线；系统时钟是当前时刻唯一事实来源。

## 技术栈

Kotlin、Compose Material 3、Navigation、ViewModel、Flow、Room、DataStore；`:domain` 为纯 JVM 模块。

## 构建

安装 JDK 17、Android SDK Platform 35 与 Build Tools 35.0.0，设置 JAVA_HOME 和 ANDROID_HOME 后运行：

```powershell
.\gradlew.bat :domain:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

项目私有工具可放在未提交的 `.tools/jdk`、`.tools/android-sdk`、`.tools/gradle-home`，并通过 `scripts/build-local.ps1` 运行同一组任务。

