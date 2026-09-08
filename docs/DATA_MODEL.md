# LifeOS V3.0 数据模型

## DataStore

`UserSettings` 分为：

- WorkSettings：monthlySalary 十进制字符串；workStart、lunchStart、lunchEnd、workEnd 为 ISO LocalTime；lunchBreakEnabled；salaryDay 1～31。
- PersonalSettings：birthDate 可空 ISO LocalDate；retirementType 可空 MALE/FEMALE_55/FEMALE_50。
- DisplaySettings：仅保留 themeMode，值为 SYSTEM/LIGHT/DARK；动画固定启用，不持久化动画或文案模式。
- 内部状态：onboarded。

V3SettingsMigration 删除旧 FreeTime、货币格式、animations 和 fun_mode 键。

## Room 版本 2

`balance_history`：id 自增 Long；name 可空；price 十进制字符串；usageMonths 可空；frequencyType 可空 DAILY/WEEKLY/MONTHLY；frequencyValue 可空十进制字符串；createdAt ISO LocalDateTime。

`life_nodes`：id 自增 Long；title；type AGE/DATE；targetAge 可空；targetDate 可空 ISO LocalDate；createdAt ISO LocalDateTime。

Room 1→2 migration 新建精简天平表，从 `balance_items` 复制可继续使用的字段，原样保留 `life_nodes`，然后删除旧 `balance_items`、`decision_reviews`、`decisions`。旧 YEARLY 使用频率在读取时等价换算为每月次数，未知旧频率安全降级为空。禁止 destructive migration。

`legacy-v1.db/wishes` 只作一次性迁移源：当 V3 历史为空时，将旧 name/price/createdAt 转换为 BalanceHistory；旧状态和备注不进入 V3 产品模型。

## 非持久化数据

今日/本月累计收入、秒薪、工作状态、年度余额、退休倒计时、天平结果、长期价值和未来时间预算全部按设置与系统时间派生，不写入数据库。

## Phase 记录

- Phase 1：完成 UserSettings 收窄、DataStore migration、Room 1→2 migration。
- Phase 2：天平计算结果保持瞬时，不自动制造记录。
- Phase 3：只在用户点击保存时写入 BalanceHistory。
- Phase 4：LifeNode 保持原表兼容。
- Phase 5：FutureTimeBudget 不持久化。
