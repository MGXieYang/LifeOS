# V3 离线工作日日历

中国大陆存在周末补班，不能只看星期。`assets/calendar/<year>.json` 按 year、calendarVersion、calendarLastUpdated、source、days 组织，days 值只允许 WORKDAY/HOLIDAY。

当前内置 2025、2026。年度文件覆盖默认星期规则；年度缺失时按周一至周五工作、周六周日休息并明确提示估算。禁止把算法生成的假期伪装为官方数据。

来源：

- 2025 国务院办公厅：https://www.gov.cn/zhengce/zhengceku/202411/content_6986383.htm
- 2026 国务院办公厅：https://www.gov.cn/zhengce/zhengceku/202511/content_7047091.htm
- 2026 核对页面：https://www.beijing.gov.cn/cs/gncs/zcwj/202603/t20260327_4568275.html

新增年度流程：等待官方公布，逐日录入所有假期和补班，核对跨年安排，增加假期/补班/月份结清测试，更新覆盖说明。

V3 核心运行完全离线，不声明 INTERNET 权限，不在启动、跨年或页面进入时联网。Calendar 数据变化只能由发布新版本携带，经测试后生效。

