# 架构
:domain 是无 Android 依赖的 Kotlin/JVM 模块：Models、calendar、salary、wish、retirement。可以单独运行单测。
:app 的 data 层从 Assets 读取年度日历；DataStore 持久化用户设置；Room 持久化心愿。依赖由 NiuMaApplication 手动组装，避免额外 DI SDK。
AppViewModel 将配置 Flow、心愿 Flow、前台 Clock Flow 合成为界面状态，保存与删除只在用户操作时发生。计算结果内存中派生，无收入记录表。
feature 分为 home、wish、statistics、settings；首次引导复用设置表单并分步完成。navigation 管理四个底部标签。
UI 使用 Material 3 与统一主题、卡片、金额和时间格式化，文案集中 MotivationTextProvider。

NetworkCalendarRepository 独占日历联网与 AtomicFile 缓存；CalendarFeed 校验第三方格式并转换为 domain 模型；更新成功后重新发布界面状态。
