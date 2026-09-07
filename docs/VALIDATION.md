# LifeOS V3.0 验证记录

日期：2026-09-08。

## 规定任务

- `:domain:test`：成功。2 个测试套件、22 项测试、0 失败、0 错误。
- `:app:testDebugUnitTest`：成功。1 个测试套件、3 项测试、0 失败、0 错误。
- `:app:assembleDebug`：成功。
- `:app:lintDebug`：成功，0 错误、4 个警告。警告为 compile/target SDK 35 和两个固定依赖存在更新版本，不影响本次构建。

最终四项任务已在同一次 Gradle 调用中复跑并显示 `BUILD SUCCESSFUL`。构建使用项目私有 Microsoft OpenJDK 17.0.20.1、Android Platform 35 和 Build Tools 35.0.0。

## 覆盖

保留测试继续覆盖默认 09:30—19:00、8 小时有效工时、上班前/工作/午休/下班/休息日、无午休、自定义四时间点、今日/全月收入、月末精确结清、发薪日、系统 Clock/时区、普通工作日/周末/法定假期/周末补班、三类渐进式退休政策边界。

V3 新增测试覆盖金额 0/8999/超大值、工作小时/工作日/月薪比例、五级 PurchaseWeightPolicy、每天/每周/每月 UsageValue、不同周期与非法输入、年度余额、当前年龄、人生节点、自然周末、90/180/365 算法基础、显式法定假日、调休周末、完整可休周末和缺失年份估算。

Room 1→2 instrumentation migration test 源码已通过 `:app:compileDebugAndroidTestKotlin` 编译；测试会插入 V1 天平/节点/Decision 数据，验证天平和节点保留、Decision 表删除。当前没有连接设备或模拟器，因此没有把该 instrumentation test 记录为已运行。

## 产物

`deliverables/LifeOS-0.0.4-debug.apk`，包名 `com.lifeos`，versionCode 4，versionName 0.0.4，应用名 LifeOS。

SHA-256：`3F6F0398C47967B7D86CBD4B1ACD2ED9F2DBD0721D58BBD2FA506A5C9B769F5E`。

APK Signature Scheme v2 验证通过，签名者 1 个。最终 APK 不包含 INTERNET 权限；仅存在 Android 构建工具自动生成的应用内动态接收器权限。

## 未完成的设备验证

当前没有真机或模拟器，尚未实际检查小屏、大字体、输入法、深色模式、动画关闭、锁屏/杀进程恢复和真实 V2 数据库升级。V3.0 DOCX 的只读渲染因工作区依赖中没有 bundled LibreOffice 而失败；已通过 python-docx 完整提取正文与结构进行需求审计，没有修改原 DOCX。
