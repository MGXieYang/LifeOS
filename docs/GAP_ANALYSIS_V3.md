# LifeOS V3.0 Gap Analysis

## 结论

当前代码已经具备可靠的工作日、工时、工资和渐进式退休计算基础，不需要重写项目。V3.0 的主要工作是保留这些算法，替换 V2 导航和页面，收窄数据模型，并补齐新版天平、人生时间轴和未来时间预算。

## KEEP

- `domain/calendar/WorkCalendar.kt`：法定调休覆盖普通周规则、缺失年份估算和警告逻辑正确；补充查询显式法定假日的能力。
- `domain/salary/SalaryCalculator.kt`：按系统时间重算、BigDecimal 工资计算、月末精确结清、发薪日和午休暂停逻辑正确；仅适配 V3 状态枚举。
- `domain/salary/WorkTimeCalculator.kt`：有效工时和分段截断算法正确；仅统一对外工作状态。
- `domain/retirement/RetirementCalculator.kt` 与 `RetirementPolicy`：渐进式延迟退休实现和既有回归测试保留。
- `app/data/Repositories.kt` 中 DataStore、Room、Assets 日历的基础设施。
- 2025/2026 本地日历资产及其校验测试。
- 系统 Clock 驱动、前台订阅时刷新且不保存累计金额的设计。

## REFACTOR

- `UserSettings`：保留已验证的默认 19:00 下班与 8 小时有效工时；退休类别改为可空；显示设置增加主题；删除 V2 自由时间与未要求的货币显示字段。
- `AppViewModel`：只组合设置、天平历史和人生节点；删除心愿、Decision、FreeTime；缓存按日计算的年度/未来预算结果。
- `MainActivity`：导航改为当下、天平、时间、设置；设置成为一级 Tab；Onboarding 独立实现。
- 首页：从 V2 综合 Dashboard 改为今日牛马 Hero、本月已赚、距离发薪三层结构。
- 天平：保留金额到工作代价的正确换算能力，删除购物清单状态、置顶、分类、持有成本和自由夜晚，重做真实天平视觉与交互。
- 时间：保留年度余额、人生节点和退休能力，重做年度进度环、人生时间轴与节点详情。
- Room：`life-os.db` 从版本 1 升到版本 2；保留并转换天平记录和人生节点，删除 Decision 表。
- DataStore：通过 V3 migration 删除 FreeTime 与旧显示字段，保留工作、个人、Onboarding 和通用显示设置。
- 备份：V2 JSON 格式含 Decision/FreeTime，不能继续作为 V3 数据格式；Phase 1-5 不提供不兼容入口，后续 Phase 6 另行设计。

## DELETE

- `domain/decision/` 及 Decision/Review/Statistics 全部类型和算法。
- `domain/freetime/`、FreeTimeCalculator 和全部睡眠、通勤、必要生活配置。
- Decision 页面、导航、ViewModel 操作、Repository/DAO 和 Room 表。
- V2 旧首页、旧天平 UI、旧时间 UI。
- 心愿和统计旧页面及其当前产品入口；V1 心愿只保留一次性本地迁移读取能力。
- Balance 的考虑中/购入/放弃状态、分类、置顶、附加/持有成本和自由夜晚模型。
- 启动自动联网日历、手动联网更新 UI 和 INTERNET 权限。

## NEW

- LifeOS V3 Onboarding：品牌欢迎、必要工作设置、可跳过个人信息。
- `PurchaseWeightPolicy`：集中输出 LIGHT、SLIGHTLY_LIGHT、NORMAL、HEAVY、VERY_HEAVY。
- 新版天平：可选商品名、金额、Slider、动态天平/砝码、工作小时/工作日/月薪比例与压力结论。
- `UsageValueCalculator`：使用周期、频率、预计次数、单次/每天/每月成本与规则总结。
- `BalanceHistory` 的精简 Room 模型与只算不保存流程。
- 新版时间页：年度进度环、法定假日余额、人生时间轴、节点详情。
- `FutureTimeBudgetCalculator`：90/180/365 天窗口、工作日、非工作日、显式法定假日和完整可休周末。
- V3 Domain 单测和 Room 1→2 migration 测试源文件。

## 数据迁移计划

1. Room `LifeDatabase` 1→2 新建 `balance_history_new`，从 `balance_items` 复制 `id/name/price/usageMonths/frequencyType/frequencyValue/createdAt`，删除旧表并重命名。
2. 保留 `life_nodes` 原表及全部记录。
3. 删除 `decision_reviews` 与 `decisions`；不清空数据库文件，也不使用 destructive migration。
4. DataStore migration 删除 `free_enabled`、`sleep_minutes`、`commute_minutes`、`necessary_enabled`、`necessary_minutes`、`currency`、`money_decimals`，保留工资、工时、发薪、个人信息、显示设置和 Onboarding 状态。
5. V1 `legacy-v1.db` 仅作一次性迁移源；未迁移的心愿转换为精简天平历史，不再暴露心愿功能。

## 阶段文件计划

- Phase 1：重构 `Models`、Settings/DataStore、Application/ViewModel、Navigation、Onboarding、Home、Settings；加入 Room/DataStore migration；删除 Decision/FreeTime 与旧入口。
- Phase 2：重构 Balance Domain，新增 `PurchaseWeightPolicy`，实现基础天平 UI 与测试。
- Phase 3：新增 `UsageValueCalculator`、长期价值、保存历史与测试。
- Phase 4：扩展 LifeTime Domain，重做年度进度、人生时间轴、自定义节点和退休 UI。
- Phase 5：新增 `FutureTimeBudgetCalculator`、90/180/365 天 UI 与测试。
