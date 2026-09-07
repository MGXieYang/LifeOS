# 架构
:domain 是无 Android 依赖的 Kotlin/JVM 模块：calendar、salary、retirement、balance、lifetime、freetime、decision，并保留 V1 wish 兼容模型。可以单独运行单测。
:app 的 data 层从 Assets、联网缓存读取年度日历；DataStore 持久化用户设置；Room 分别保存 V1 迁移源与 V2 天平、节点、决策和复盘。应用首次升级会把 V1 心愿幂等迁移到天平。
AppViewModel 将配置 Flow、心愿 Flow、前台 Clock Flow 合成为界面状态，保存与删除只在用户操作时发生。计算结果内存中派生，无收入记录表。
V2 主导航为 now、balance、time、decision；work 与 settings 是从当下进入的次级页面。
UI 使用 Material 3 与统一主题、卡片、金额和时间格式化，文案集中 MotivationTextProvider。

NetworkCalendarRepository 独占日历联网与 AtomicFile 缓存；CalendarFeed 校验第三方格式并转换为 domain 模型；更新成功后重新发布界面状态。
