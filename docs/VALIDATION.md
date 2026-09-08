# LifeOS V3.0 验证记录

日期：2026-09-08。

## 规定任务

- `:domain:test`：成功。2 个测试套件、22 项测试、0 失败、0 错误。
- `:app:testDebugUnitTest`：成功。1 个测试套件、3 项测试、0 失败、0 错误。
- `:app:assembleDebug`：成功。
- `:app:lintDebug`：成功，0 错误、16 个警告。警告均为 compile/target SDK 35 或固定依赖存在更新版本，不影响本次构建。

最终四项任务已在同一次 Gradle 调用中复跑并显示 `BUILD SUCCESSFUL`。构建使用项目私有 Microsoft OpenJDK 17.0.20.1、Android Platform 35 和 Build Tools 35.0.0。

## 覆盖

保留测试继续覆盖默认 09:30—19:00、8 小时有效工时、上班前/工作/午休/下班/休息日、无午休、自定义四时间点、今日/全月收入、月末精确结清、发薪日、系统 Clock/时区、普通工作日/周末/法定假期/周末补班、三类渐进式退休政策边界。

V3 新增测试覆盖金额 0/8999/超大值、工作小时/工作日/月薪比例、五级 PurchaseWeightPolicy、每天/每周/每月 UsageValue、不同周期与非法输入、年度余额、当前年龄、人生节点、自然周末、90/180/365 算法基础、显式法定假日、调休周末、完整可休周末和缺失年份估算。

Room 1→2 instrumentation migration test 源码已通过 `:app:compileDebugAndroidTestKotlin` 编译；测试会插入 V1 天平/节点/Decision 数据，验证天平和节点保留、Decision 表删除。当前没有连接设备或模拟器，因此没有把该 instrumentation test 记录为已运行。

## 2026-09-08 交互修复验证

天平重置只清空当前表单，历史记录保持不变；历史入口改为独立页面，删除仍由用户逐条触发。时间首页移除内嵌新增表单，新增按钮进入独立页面，保存成功后返回列表。动画和轻松/简洁模式字段、界面控件及持久化逻辑已移除，现有动画始终启用。主题选择点击即保存并刷新全局主题，保存期间禁止重复点击，失败时恢复原主题。

本轮修改后完整复跑 `:domain:test`、`:app:testDebugUnitTest`、`:app:assembleDebug`、`:app:lintDebug`，结果为 `BUILD SUCCESSFUL`。

## 2026-09-08 首页视觉精修验证

首页新增暖米白/深森林主题层次、工作状态胶囊、渐变 Hero 与小时/分钟/秒时间单价。单价直接由 `SalaryResult.salaryPerSecond` 高精度换算，未增加累计存储或第二套计薪逻辑。

本轮修改后完整复跑 `:domain:test`、`:app:testDebugUnitTest`、`:app:assembleDebug`、`:app:lintDebug`，结果为 `BUILD SUCCESSFUL in 53s`。`git diff --check` 无空白错误；当前环境没有连接设备或模拟器，视觉与大字体检查仍保留在设备验证清单。
## 产物

`deliverables/LifeOS-0.0.4-debug.apk`，包名 `com.lifeos`，versionCode 4，versionName 0.0.4，应用名 LifeOS。

SHA-256：`ECD5C31A79DAD5EDEF91174A0DF0AFC75E6E2B013C28A74CE7ECB9E4E8761CED`。

APK Signature Scheme v2 验证通过，签名者 1 个。最终 APK 不包含 INTERNET 权限；仅存在 Android 构建工具自动生成的应用内动态接收器权限。

## 未完成的设备验证

当前没有真机或模拟器，尚未实际检查小屏、大字体、输入法、跟随系统/浅色/深色来回切换、页面返回、锁屏/杀进程恢复和真实 V2 数据库升级。V3.0 DOCX 的只读渲染因工作区依赖中没有 bundled LibreOffice 而失败；已通过 python-docx 完整提取正文与结构进行需求审计，没有修改原 DOCX。
