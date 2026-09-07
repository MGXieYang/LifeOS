# 本地数据
UserSettings：monthlySalary 为十进制字符串；workStart/lunchStart/lunchEnd/workEnd 为 ISO LocalTime；salaryDay 整数；birthDate 可空 ISO LocalDate；retirementType 枚举 MALE/FEMALE_55/FEMALE_50；onboarded 布尔。
DataStore 单次 edit 原子保存完整设置，读取前保留加载状态，避免首次启动闪烁。写入失败反馈错误且不关闭表单。
Room WishEntity：id 自增 Long 主键；name；price 十进制字符串；remark；status WISHING/ACHIEVED；createdAt ISO LocalDateTime；achievedAt 可空。数据库版本 1，导出 schema；无 destructive migration。
不存在累计收入字段。已有心愿编辑保持 id/createdAt，实现和取消实现更新 achievedAt。

日历缓存 calendar-cache.json 保存年度原始响应及 updated 日期；只含公开日历数据，AtomicFile 事务替换，失败保留此前缓存。
# V2 数据模型补充
`balance_items` 保存天平项目、状态、单一置顶标记、使用与持有成本；`life_nodes` 保存年龄或日期节点；`decisions` 保存决策过程和复盘日期；`decision_reviews` 保存结果评价。`life-os.db` 是 V2 主库，V1 `niuma.db/wishes` 只作为一次性迁移源。DataStore 增加午休开关、自由时间参数、动效与趣味文案设置。JSON 备份格式版本为 2，导入按主键合并。
