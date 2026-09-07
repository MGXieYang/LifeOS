# LifeOS V3.0 架构

## 模块边界

`:domain` 是纯 Kotlin/JVM 模块，只包含 calendar、salary/worktime、retirement、balance、lifetime。所有金额使用 BigDecimal；所有依赖当前日期/时间的 Calculator 接受 Clock 或显式时间，禁止依赖 Android/Compose。

`:app` 负责 Compose UI、Navigation、ViewModel、Room、DataStore 和本地 Calendar assets。Feature 目录为 onboarding、home、balance、lifetime、settings。

## 状态流

AppViewModel 合并 WorkSettings、PersonalSettings、DisplaySettings、BalanceHistory 和 LifeNode Flow。前台有订阅时每 250ms 读取一次系统时区和系统时间并重新计算工资状态；Timer 只触发刷新，不累计事实数据。年度余额、退休和未来时间预算按日期缓存。

## 本地持久化

DataStore 保存设置并通过 V3 DataMigration 删除 FreeTime/V2 显示遗留字段。Room `life-os.db` 版本 2 保存 `balance_history` 与 `life_nodes`；1→2 migration 保留对应数据后删除 Decision 表。`legacy-v1.db` 仅作一次性心愿迁移源，不向产品暴露旧功能。

## 日历与离线边界

WorkCalendar 读取 `assets/calendar`。Calendar 文件覆盖普通周规则；缺失年份降级为周一至周五并返回估算警告。V3 不声明 INTERNET 权限，不包含联网 SDK、自动更新或后台计时服务。

## Phase 记录

- Phase 1：完成品牌、Onboarding、当下、设置、四 Tab 导航、状态流和安全数据迁移；删除 Decision、FreeTime、旧 UI 与联网入口。
- Phase 2：加入 PurchaseWeightPolicy 和独立天平 Feature。
- Phase 3：加入 UsageValueCalculator 与精简称重历史。
- Phase 4：重做时间 Feature，包含年度、人生时间轴、节点和退休。
- Phase 5：加入 FutureTimeBudgetCalculator 与按日缓存的 90/180/365 天结果。

